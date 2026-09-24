package net.toancb.shader.shaders;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.shader.*;
import net.minecraft.client.util.JSONBlendingMode;
import net.minecraft.client.util.JSONException;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreInstance implements IShaderManager, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static ShaderCoreInstance lastAppliedEffect;
    private static int lastProgramId = -1;
    private final Map<String, IntSupplier> samplerMap = Maps.newHashMap();
    private final List<String> samplerNames = Lists.newArrayList();
    private final List<Integer> samplerLocations = Lists.newArrayList();
    private final Map<String, ShaderCoreUniform> uniforms = Maps.newHashMap();
    private final int programId;
    private final String name;
    private boolean dirty;
    private final JSONBlendingMode blend;
    private final List<Integer> attributes;
    private final List<String> attributeNames;
    private final ShaderLoader vertexProgram;
    private final ShaderLoader fragmentProgram;

    public ShaderCoreInstance(IResourceManager resourceManager, String shaderPath) throws IOException {
        ResourceLocation rl = ResourceLocation.tryParse(shaderPath);
        ResourceLocation resourcelocation = new ResourceLocation(rl.getNamespace(), "shaders/program/" + rl.getPath() + ".json");
        this.name = shaderPath;
        IResource iresource = null;

        try {
            iresource = resourceManager.getResource(resourcelocation);
            JsonObject jsonobject = JSONUtils.parse(new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));
            String s = JSONUtils.getAsString(jsonobject, "vertex");
            String s2 = JSONUtils.getAsString(jsonobject, "fragment");
            JsonArray jsonarray = JSONUtils.getAsJsonArray(jsonobject, "samplers", null);
            if (jsonarray != null) {
                int i = 0;

                for(JsonElement jsonelement : jsonarray) {
                    try {
                        this.parseSamplerNode(jsonelement);
                    } catch (Exception exception2) {
                        JSONException jsonexception1 = JSONException.forException(exception2);
                        jsonexception1.prependJsonKey("samplers[" + i + "]");
                        throw jsonexception1;
                    }

                    ++i;
                }
            }

            JsonArray jsonarray1 = JSONUtils.getAsJsonArray(jsonobject, "attributes", null);
            if (jsonarray1 != null) {
                int j = 0;
                this.attributes = Lists.newArrayListWithCapacity(jsonarray1.size());
                this.attributeNames = Lists.newArrayListWithCapacity(jsonarray1.size());

                for(JsonElement jsonelement1 : jsonarray1) {
                    try {
                        this.attributeNames.add(JSONUtils.convertToString(jsonelement1, "attribute"));
                    } catch (Exception exception1) {
                        JSONException jsonexception2 = JSONException.forException(exception1);
                        jsonexception2.prependJsonKey("attributes[" + j + "]");
                        throw jsonexception2;
                    }

                    ++j;
                }
            } else {
                this.attributes = null;
                this.attributeNames = null;
            }

            this.blend = parseBlendNode(JSONUtils.getAsJsonObject(jsonobject, "blend", null));
            this.vertexProgram = getOrCreate(resourceManager, ShaderLoader.ShaderType.VERTEX, s);
            this.fragmentProgram = getOrCreate(resourceManager, ShaderLoader.ShaderType.FRAGMENT, s2);
            this.programId = ShaderLinkHelper.createProgram();
            ShaderLinkHelper.linkProgram(this);
            this.updateLocations();
            if (this.attributeNames != null) {
                for(String s3 : this.attributeNames) {
                    int l = ShaderCoreUniform.glGetAttribLocation(this.programId, s3);
                    this.attributes.add(l);
                }
            }
        } catch (Exception exception3) {
            String s1;
            if (iresource != null) {
                s1 = " (" + iresource.getSourceName() + ")";
            } else {
                s1 = "";
            }

            JSONException jsonexception = JSONException.forException(exception3);
            jsonexception.setFilenameAndFlush(resourcelocation.getPath() + s1);
            throw jsonexception;
        } finally {
            IOUtils.closeQuietly(iresource);
        }

        this.markDirty();
    }

    public static ShaderLoader getOrCreate(IResourceManager resourceManager, ShaderLoader.ShaderType shaderType, String shaderName) throws IOException {
        ShaderLoader shaderloader = shaderType.getPrograms().get(shaderName);
        if (shaderloader == null) {
            ResourceLocation rl = ResourceLocation.tryParse(shaderName);
            ResourceLocation resourcelocation = new ResourceLocation(rl.getNamespace(), "shaders/program/" + rl.getPath() + shaderType.getExtension());
            IResource iresource = resourceManager.getResource(resourcelocation);

            try {
                shaderloader = ShaderLoader.compileShader(shaderType, shaderName, iresource.getInputStream(), iresource.getSourceName());
            } finally {
                IOUtils.closeQuietly(iresource);
            }
        }

        return shaderloader;
    }

    public static JSONBlendingMode parseBlendNode(JsonObject jsonObject) {
        if (jsonObject == null) {
            return new JSONBlendingMode();
        } else {
            int i = 32774;
            int j = 1;
            int k = 0;
            int l = 1;
            int i1 = 0;
            boolean flag = true;
            boolean flag1 = false;
            if (JSONUtils.isStringValue(jsonObject, "func")) {
                i = JSONBlendingMode.stringToBlendFunc(jsonObject.get("func").getAsString());
                if (i != 32774) {
                    flag = false;
                }
            }

            if (JSONUtils.isStringValue(jsonObject, "srcrgb")) {
                j = JSONBlendingMode.stringToBlendFactor(jsonObject.get("srcrgb").getAsString());
                if (j != 1) {
                    flag = false;
                }
            }

            if (JSONUtils.isStringValue(jsonObject, "dstrgb")) {
                k = JSONBlendingMode.stringToBlendFactor(jsonObject.get("dstrgb").getAsString());
                if (k != 0) {
                    flag = false;
                }
            }

            if (JSONUtils.isStringValue(jsonObject, "srcalpha")) {
                l = JSONBlendingMode.stringToBlendFactor(jsonObject.get("srcalpha").getAsString());
                if (l != 1) {
                    flag = false;
                }

                flag1 = true;
            }

            if (JSONUtils.isStringValue(jsonObject, "dstalpha")) {
                i1 = JSONBlendingMode.stringToBlendFactor(jsonObject.get("dstalpha").getAsString());
                if (i1 != 0) {
                    flag = false;
                }

                flag1 = true;
            }

            if (flag) {
                return new JSONBlendingMode();
            } else {
                return flag1 ? new JSONBlendingMode(j, k, l, i1, i) : new JSONBlendingMode(j, k, i);
            }
        }
    }

    public void close() {
        for(ShaderCoreUniform shaderCoreUniform : this.uniforms.values()) {
            shaderCoreUniform.close();
        }

        ShaderLinkHelper.releaseProgram(this);
    }

    public void clear() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        ShaderLinkHelper.glUseProgram(0);
        lastProgramId = -1;
        lastAppliedEffect = null;

        for(int i = 0; i < this.samplerLocations.size(); ++i) {
            if (this.samplerMap.get(this.samplerNames.get(i)) != null) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                GlStateManager._disableTexture();
                GlStateManager._bindTexture(0);
            }
        }

    }

    public void apply() {
        RenderSystem.assertThread(RenderSystem::isOnGameThread);
        this.dirty = false;
        lastAppliedEffect = this;
        this.blend.apply();
        if (this.programId != lastProgramId) {
            ShaderLinkHelper.glUseProgram(this.programId);
            lastProgramId = this.programId;
        }

        for(int i = 0; i < this.samplerLocations.size(); ++i) {
            String s = this.samplerNames.get(i);
            IntSupplier intsupplier = this.samplerMap.get(s);
            if (intsupplier != null) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                RenderSystem.enableTexture();
                int j = intsupplier.getAsInt();
                if (j != -1) {
                    RenderSystem.bindTexture(j);
                    ShaderCoreUniform.uploadInteger(this.samplerLocations.get(i), i);
                }
            }
        }

        for(ShaderCoreUniform shaderCoreUniform : this.uniforms.values()) {
            shaderCoreUniform.upload();
        }
    }

    public ShaderCoreUniform getOrCreateUniform(String name, UType type, int count) {
        if (this.uniforms.containsKey(name)) {
            return this.uniforms.get(name);
        }

        int location = ShaderCoreUniform.glGetUniformLocation(this.programId, name);
        if (location == -1) {
            this.uniforms.put(name, ShaderCoreUniform.DUMMY);
            return ShaderCoreUniform.DUMMY;
        }

        ShaderCoreUniform shaderCoreUniform = ShaderCoreUniform.create(name, type, count, this);
        shaderCoreUniform.setLocation(location);
        this.uniforms.put(name, shaderCoreUniform);

        return shaderCoreUniform;
    }

    public ShaderCoreUniform getOrCreateUniform(String name, UType type) {
        return this.getOrCreateUniform(name, type, 1);
    }

    public void markDirty() {
        this.dirty = true;
    }

    private void updateLocations() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        IntList intlist = new IntArrayList();

        for(int i = 0; i < this.samplerNames.size(); ++i) {
            String s = this.samplerNames.get(i);
            int j = ShaderUniform.glGetUniformLocation(this.programId, s);
            if (j == -1) {
                LOGGER.warn("Shader {} could not find sampler named {} in the specified shader program.", this.name, s);
                this.samplerMap.remove(s);
                intlist.add(i);
            } else {
                this.samplerLocations.add(j);
            }
        }

        for(int l = intlist.size() - 1; l >= 0; --l) {
            this.samplerNames.remove(intlist.getInt(l));
        }
    }

    private void parseSamplerNode(JsonElement jsonElement) {
        JsonObject jsonobject = JSONUtils.convertToJsonObject(jsonElement, "sampler");
        String s = JSONUtils.getAsString(jsonobject, "name");
        if (!JSONUtils.isStringValue(jsonobject, "file")) {
            this.samplerMap.put(s, null);
            this.samplerNames.add(s);
        } else {
            this.samplerNames.add(s);
        }
    }

    public void setSampler(String samplerName, IntSupplier textureIdSupplier) {
        this.samplerMap.remove(samplerName);

        this.samplerMap.put(samplerName, textureIdSupplier);
        this.markDirty();
    }

    @MethodsReturnNonnullByDefault
    public ShaderLoader getVertexProgram() {
        return this.vertexProgram;
    }

    @MethodsReturnNonnullByDefault
    public ShaderLoader getFragmentProgram() {
        return this.fragmentProgram;
    }

    public int getId() {
        return this.programId;
    }
}
