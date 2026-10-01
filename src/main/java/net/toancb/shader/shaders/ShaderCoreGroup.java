package net.toancb.shader.shaders;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.util.JSONException;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreGroup implements AutoCloseable {
    private final Framebuffer screenTarget;
    private final IResourceManager resourceManager;
    private final String name;
    private final List<ShaderCore> passes = Lists.newArrayList();
    private final Map<String, Framebuffer> customRenderTargets = Maps.newHashMap();
    private final List<TargetScaleData> resizedTargets = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;
    private int screenWidth;
    private int screenHeight;

    static final class TargetScaleData {
        public final Framebuffer framebuffer;
        public final float scaleWidth, scaleHeight;

        private TargetScaleData(Framebuffer framebuffer, float scaleWidth, float scaleHeight) {
            this.framebuffer = framebuffer;
            this.scaleWidth = scaleWidth;
            this.scaleHeight = scaleHeight;
        }
    }

    public ShaderCoreGroup(TextureManager textureManager, IResourceManager resourceManager, Framebuffer screenTarget, ResourceLocation shaderLocation) throws IOException, JsonSyntaxException {
        this.resourceManager = resourceManager;
        this.screenTarget = screenTarget;
        this.screenWidth = screenTarget.viewWidth;
        this.screenHeight = screenTarget.viewHeight;
        this.name = shaderLocation.toString();
        this.updateOrthoMatrix();
        this.load(textureManager, shaderLocation);
    }

    private void load(TextureManager textureManager, ResourceLocation shaderLocation) throws IOException, JsonSyntaxException {
        IResource resource = null;

        try {
            resource = this.resourceManager.getResource(shaderLocation);
            JsonObject jsonObject = JSONUtils.parse(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            if (JSONUtils.isArrayNode(jsonObject, "targets")) {
                JsonArray targetsArray = jsonObject.getAsJsonArray("targets");
                int index = 0;

                for (JsonElement targetElement : targetsArray) {
                    try {
                        this.parseTargetNode(targetElement);
                    } catch (Exception e) {
                        JSONException jsonException = JSONException.forException(e);
                        jsonException.prependJsonKey("targets[" + index + "]");
                        throw jsonException;
                    }
                    ++index;
                }
            }

            if (JSONUtils.isArrayNode(jsonObject, "passes")) {
                JsonArray passesArray = jsonObject.getAsJsonArray("passes");
                int index = 0;

                for (JsonElement passElement : passesArray) {
                    try {
                        this.parsePassNode(textureManager, passElement);
                    } catch (Exception e) {
                        JSONException jsonException = JSONException.forException(e);
                        jsonException.prependJsonKey("passes[" + index + "]");
                        throw jsonException;
                    }
                    ++index;
                }
            }
        } catch (Exception e) {
            String sourceName = (resource != null) ? " (" + resource.getSourceName() + ")" : "";
            JSONException jsonException = JSONException.forException(e);
            jsonException.setFilenameAndFlush(shaderLocation.getPath() + sourceName);
            throw jsonException;
        } finally {
            IOUtils.closeQuietly((Closeable) resource);
        }
    }

    private void parseTargetNode(JsonElement targetElement) throws JSONException {
        if (JSONUtils.isStringValue(targetElement)) {
            this.addTempTarget(targetElement.getAsString(), 1.0f, 1.0f, true);
        } else {
            JsonObject targetObj = JSONUtils.convertToJsonObject(targetElement, "target");
            String targetName = JSONUtils.getAsString(targetObj, "name");
            float scaleWidth = JSONUtils.getAsFloat(targetObj, "scale_width", 1.0f);
            float scaleHeight = JSONUtils.getAsFloat(targetObj, "scale_height", 1.0f);
            boolean isResize = JSONUtils.getAsBoolean(targetObj, "is_resize", true);

            if (this.customRenderTargets.containsKey(targetName)) {
                throw new JSONException(targetName + " is already defined");
            }

            this.addTempTarget(targetName, scaleWidth, scaleHeight, isResize);
        }
    }

    private void parsePassNode(TextureManager textureManager, JsonElement passElement) throws IOException {
        JsonObject passObj = JSONUtils.convertToJsonObject(passElement, "pass");
        String passName = JSONUtils.getAsString(passObj, "name");
        String inputTargetName = JSONUtils.getAsString(passObj, "intarget");
        String outputTargetName = JSONUtils.getAsString(passObj, "outtarget");

        Framebuffer inputTarget = this.getRenderTarget(inputTargetName);
        Framebuffer outputTarget = this.getRenderTarget(outputTargetName);

        if (inputTarget == null) {
            throw new JSONException("Input target '" + inputTargetName + "' does not exist");
        } else if (outputTarget == null) {
            throw new JSONException("Output target '" + outputTargetName + "' does not exist");
        } else {
            ShaderCore shader = this.addPass(passName, inputTarget, outputTarget);
            JsonArray auxTargetsArray = JSONUtils.getAsJsonArray(passObj, "auxtargets", (JsonArray) null);

            if (auxTargetsArray != null) {
                int index = 0;

                for (JsonElement auxElement : auxTargetsArray) {
                    try {
                        JsonObject auxObj = JSONUtils.convertToJsonObject(auxElement, "auxtarget");
                        String samplerName = JSONUtils.getAsString(auxObj, "name");
                        String targetId = JSONUtils.getAsString(auxObj, "id");

                        boolean isDepthBuffer;
                        String cleanTargetId;
                        if (targetId.endsWith(":depth")) {
                            isDepthBuffer = true;
                            cleanTargetId = targetId.substring(0, targetId.lastIndexOf(58));
                        } else {
                            isDepthBuffer = false;
                            cleanTargetId = targetId;
                        }

                        Framebuffer auxTarget = this.getRenderTarget(cleanTargetId);
                        if (auxTarget == null) {
                            if (isDepthBuffer) {
                                throw new JSONException("Render target '" + cleanTargetId + "' can't be used as depth buffer");
                            }

                            ResourceLocation parsedRl = ResourceLocation.tryParse(cleanTargetId);
                            ResourceLocation textureLocation = new ResourceLocation(parsedRl.getNamespace(), "textures/effect/" + parsedRl.getPath() + ".png");
                            IResource textureResource = null;

                            try {
                                textureResource = this.resourceManager.getResource(textureLocation);
                            } catch (FileNotFoundException e) {
                                throw new JSONException("Render target or texture '" + cleanTargetId + "' does not exist");
                            } finally {
                                IOUtils.closeQuietly((Closeable) textureResource);
                            }

                            textureManager.bind(textureLocation);
                            Texture texture = textureManager.getTexture(textureLocation);
                            int auxWidth = JSONUtils.getAsInt(auxObj, "width");
                            int auxHeight = JSONUtils.getAsInt(auxObj, "height");
                            boolean isBilinear = JSONUtils.getAsBoolean(auxObj, "bilinear");

                            if (isBilinear) {
                                RenderSystem.texParameter(3553, 10241, 9729);
                                RenderSystem.texParameter(3553, 10240, 9729);
                            } else {
                                RenderSystem.texParameter(3553, 10241, 9728);
                                RenderSystem.texParameter(3553, 10240, 9728);
                            }

                            shader.addAuxAsset(samplerName, texture::getId, auxWidth, auxHeight);
                        } else if (isDepthBuffer) {
                            shader.addAuxAsset(samplerName, auxTarget::getDepthTextureId, auxTarget.width, auxTarget.height);
                        } else {
                            shader.addAuxAsset(samplerName, auxTarget::getColorTextureId, auxTarget.width, auxTarget.height);
                        }
                    } catch (Exception e) {
                        JSONException jsonException = JSONException.forException(e);
                        jsonException.prependJsonKey("auxtargets[" + index + "]");
                        throw jsonException;
                    }
                    ++index;
                }
            }
        }
    }

    public Framebuffer getTempTarget(String targetName) {
        return this.customRenderTargets.get(targetName);
    }

    public void addTempTarget(String targetName, float widthScale, float heightScale, boolean isResize) {
        Framebuffer framebuffer =
                new Framebuffer((int) (this.screenWidth * widthScale), (int) (this.screenHeight * heightScale), true, Minecraft.ON_OSX);
        framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        if (screenTarget.isStencilEnabled()) {
            framebuffer.enableStencil();
        }

        this.customRenderTargets.put(targetName, framebuffer);
        if (isResize) {
            this.resizedTargets.add(new TargetScaleData(framebuffer, widthScale, heightScale));
        }
    }

    public void close() {
        for (Framebuffer framebuffer : this.customRenderTargets.values()) {
            framebuffer.destroyBuffers();
        }

        for (ShaderCore shader : this.passes) {
            shader.close();
        }

        this.passes.clear();
    }

    public ShaderCore addPass(String passName, Framebuffer inputTarget, Framebuffer outputTarget) throws IOException {
        ShaderCore shader = new ShaderCore(this.resourceManager, passName, inputTarget, outputTarget);
        this.passes.add(this.passes.size(), shader);
        return shader;
    }

    private void updateOrthoMatrix() {
        this.shaderOrthoMatrix = Matrix4f.orthographic((float) this.screenTarget.width, (float) this.screenTarget.height, 0.1F, 1000.0F);
    }

    public void resize(int width, int height) {
        this.screenWidth = this.screenTarget.width;
        this.screenHeight = this.screenTarget.height;
        this.updateOrthoMatrix();

        for (ShaderCore shader : this.passes) {
            shader.setOrthoMatrix(this.shaderOrthoMatrix);
        }

        for (TargetScaleData scaleData : this.resizedTargets) {
            scaleData.framebuffer.resize((int) (width * scaleData.scaleWidth), (int) (height * scaleData.scaleHeight), Minecraft.ON_OSX);
        }
    }

    public void process(Consumer<ShaderCoreInstance> uniform) {
        for (ShaderCore shader : this.passes) {
            shader.process(uniform);
        }
    }

    public final String getName() {
        return this.name;
    }

    private Framebuffer getRenderTarget(String targetName) {
        if (targetName == null) {
            return null;
        } else {
            return targetName.equals("minecraft:main") ? this.screenTarget : this.customRenderTargets.get(targetName);
        }
    }
}
