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
import net.minecraft.client.shader.ShaderUniform;
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

@OnlyIn(Dist.CLIENT)
public class ShaderCoreGroup implements AutoCloseable {
    private final Framebuffer screenTarget;
    private final IResourceManager resourceManager;
    private final String name;
    private final List<ShaderCore> passes = Lists.newArrayList();
    private final Map<String, Framebuffer> customRenderTargets = Maps.newHashMap();
    private final List<Framebuffer> fullSizedTargets = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;
    private int screenWidth;
    private int screenHeight;
    private float time;
    private float lastStamp;

    public ShaderCoreGroup(TextureManager textureManager, IResourceManager resourceManager, Framebuffer screenTarget, ResourceLocation shaderLocation) throws IOException, JsonSyntaxException {
        this.resourceManager = resourceManager;
        this.screenTarget = screenTarget;
        this.time = 0.0F;
        this.lastStamp = 0.0F;
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
            this.addTempTarget(targetElement.getAsString(), this.screenWidth, this.screenHeight);
        } else {
            JsonObject targetObj = JSONUtils.convertToJsonObject(targetElement, "target");
            String targetName = JSONUtils.getAsString(targetObj, "name");
            int width = JSONUtils.getAsInt(targetObj, "width", this.screenWidth);
            int height = JSONUtils.getAsInt(targetObj, "height", this.screenHeight);

            if (this.customRenderTargets.containsKey(targetName)) {
                throw new JSONException(targetName + " is already defined");
            }

            this.addTempTarget(targetName, width, height);
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
                            cleanTargetId = targetId.substring(0, targetId.lastIndexOf(58)); // 58 là mã ASCII của dấu ':'
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

            JsonArray uniformsArray = JSONUtils.getAsJsonArray(passObj, "uniforms", (JsonArray) null);
            if (uniformsArray != null) {
                int index = 0;

                for (JsonElement uniformElement : uniformsArray) {
                    try {
                        this.parseUniformNode(uniformElement);
                    } catch (Exception e) {
                        JSONException jsonException = JSONException.forException(e);
                        jsonException.prependJsonKey("uniforms[" + index + "]");
                        throw jsonException;
                    }
                    ++index;
                }
            }
        }
    }

    private void parseUniformNode(JsonElement uniformElement) throws JSONException {
        JsonObject uniformObj = JSONUtils.convertToJsonObject(uniformElement, "uniform");
        String uniformName = JSONUtils.getAsString(uniformObj, "name");
        ShaderUniform shaderUniform = this.passes.get(this.passes.size() - 1).getEffect().getUniform(uniformName);

        if (shaderUniform == null) {
            throw new JSONException("Uniform '" + uniformName + "' does not exist");
        } else {
            float[] values = new float[4];
            int valueCount = 0;

            for (JsonElement valueElement : JSONUtils.getAsJsonArray(uniformObj, "values")) {
                try {
                    values[valueCount] = JSONUtils.convertToFloat(valueElement, "value");
                } catch (Exception e) {
                    JSONException jsonException = JSONException.forException(e);
                    jsonException.prependJsonKey("values[" + valueCount + "]");
                    throw jsonException;
                }
                ++valueCount;
            }

            switch (valueCount) {
                case 1:
                    shaderUniform.set(values[0]);
                    break;
                case 2:
                    shaderUniform.set(values[0], values[1]);
                    break;
                case 3:
                    shaderUniform.set(values[0], values[1], values[2]);
                    break;
                case 4:
                    shaderUniform.set(values[0], values[1], values[2], values[3]);
                    break;
                default:
                    break;
            }
        }
    }

    public Framebuffer getTempTarget(String targetName) {
        return this.customRenderTargets.get(targetName);
    }

    public void addTempTarget(String targetName, int width, int height) {
        Framebuffer framebuffer = new Framebuffer(width, height, true, Minecraft.ON_OSX);
        framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        if (screenTarget.isStencilEnabled()) {
            framebuffer.enableStencil();
        }

        this.customRenderTargets.put(targetName, framebuffer);
        if (width == this.screenWidth && height == this.screenHeight) {
            this.fullSizedTargets.add(framebuffer);
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

        for (Framebuffer framebuffer : this.fullSizedTargets) {
            framebuffer.resize(width, height, Minecraft.ON_OSX);
        }
    }

    public void process(float partialTicks, Uniform... uniforms) {
        if (partialTicks < this.lastStamp) {
            this.time += 1.0F - this.lastStamp;
            this.time += partialTicks;
        } else {
            this.time += partialTicks - this.lastStamp;
        }

        this.lastStamp = partialTicks;
        while (this.time > 20.0F) {
            this.time -= 20.0F;
        }

        for (ShaderCore shader : this.passes) {
            shader.process(this.time / 20.0F, uniforms);
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
