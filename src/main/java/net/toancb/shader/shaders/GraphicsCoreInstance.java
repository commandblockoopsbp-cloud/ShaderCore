package net.toancb.shader.shaders;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.JSONBlendingMode;
import net.minecraft.client.util.JSONException;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.pipeline.SamplerBase;
import net.toancb.shader.shaders.pipeline.UniformBase;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class GraphicsCoreInstance implements IShaderCoreManager, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static GraphicsCoreInstance lastAppliedEffect;
    private static int lastProgramId = -1;
    private final Map<String, SamplerBase> sampler = Maps.newHashMap();
    private final Map<String, UniformBase> uniforms = Maps.newHashMap();
    private final int programId;
    private final String name;
    private boolean dirty;
    private final JSONBlendingMode blend;
    private final ShaderCoreLoader vertexProgram;
    private final ShaderCoreLoader fragmentProgram;

    public GraphicsCoreInstance(IResourceManager resourceManager, String shaderPath) throws IOException {
        ResourceLocation rl = ResourceLocation.tryParse(shaderPath);
        ResourceLocation resourcelocation = new ResourceLocation(rl.getNamespace(), "shaders/program/" + rl.getPath() + ".json");
        this.name = shaderPath;
        IResource iresource = null;

        try {
            iresource = resourceManager.getResource(resourcelocation);
            JsonObject jsonobject = JSONUtils.parse(new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));
            String s = JSONUtils.getAsString(jsonobject, "vertex");
            String s2 = JSONUtils.getAsString(jsonobject, "fragment");

            JsonArray jsonarray1 = JSONUtils.getAsJsonArray(jsonobject, "attributes", null);
            List<Integer> attributes;
            List<String> attributeNames;
            if (jsonarray1 != null) {
                int j = 0;
                attributes = Lists.newArrayListWithCapacity(jsonarray1.size());
                attributeNames = Lists.newArrayListWithCapacity(jsonarray1.size());

                for(JsonElement jsonelement1 : jsonarray1) {
                    try {
                        attributeNames.add(JSONUtils.convertToString(jsonelement1, "attribute"));
                    } catch (Exception exception1) {
                        JSONException jsonexception2 = JSONException.forException(exception1);
                        jsonexception2.prependJsonKey("attributes[" + j + "]");
                        throw jsonexception2;
                    }

                    ++j;
                }
            } else {
                attributes = null;
                attributeNames = null;
            }

            this.blend = parseBlendNode(JSONUtils.getAsJsonObject(jsonobject, "blend", null));
            this.vertexProgram = getOrCreate(resourceManager, ShaderCoreLoader.ShaderCoreType.VERTEX, s);
            this.fragmentProgram = getOrCreate(resourceManager, ShaderCoreLoader.ShaderCoreType.FRAGMENT, s2);
            this.programId = ShaderCoreLinkHelper.createProgram();
            ShaderCoreLinkHelper.linkProgram(this);
//            this.updateLocations();
            if (attributeNames != null) {
                for (String s3 : attributeNames) {
                    int l = ShaderCoreUniform.glGetAttribLocation(this.programId, s3);
                    attributes.add(l);
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

    public static ShaderCoreLoader getOrCreate(IResourceManager resourceManager, ShaderCoreLoader.ShaderCoreType shaderType, String shaderName) throws IOException {
        ShaderCoreLoader shaderloader = shaderType.getPrograms().get(shaderName);
        if (shaderloader == null) {
            ResourceLocation rl = ResourceLocation.tryParse(shaderName);
            ResourceLocation resourcelocation = new ResourceLocation(rl.getNamespace(), "shaders/program/" + rl.getPath() + shaderType.getExtension());
            IResource iresource = resourceManager.getResource(resourcelocation);

            try {
                shaderloader = ShaderCoreLoader.compileShader(shaderType, shaderName, iresource.getInputStream(), iresource.getSourceName());
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
        for (UniformBase uniform : this.uniforms.values()) {
            if (uniform instanceof ShaderCoreUniform) {
                ((ShaderCoreUniform) uniform).close();
            }
        }
        this.uniforms.clear();

        ShaderCoreLinkHelper.releaseProgram(this);
    }

    public void clear() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        ShaderCoreLinkHelper.glUseProgram(0);
        lastProgramId = -1;
        lastAppliedEffect = null;

        for (SamplerBase sampler : this.sampler.values()) {
            sampler.close();
        }
    }

    public void apply() {
        RenderSystem.assertThread(RenderSystem::isOnGameThread);
        this.dirty = false;
        lastAppliedEffect = this;
        this.blend.apply();
        if (this.programId != lastProgramId) {
            ShaderCoreLinkHelper.glUseProgram(this.programId);
            lastProgramId = this.programId;
        }

        for (SamplerBase sampler : this.sampler.values()) {
            sampler.upload();
        }

        for (UniformBase uniform : this.uniforms.values()) {
            uniform.upload();
        }
    }

    public UniformBase setUniform(String name, UType type, int count) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (this.uniforms.containsKey(name)) {
            return this.uniforms.get(name);
        }

        int location = ShaderCoreUniform.glGetUniformLocation(this.programId, name);
        if (location == -1) {
            this.uniforms.put(name, UniformBase.DUMMY_UNIFORM);
            return UniformBase.DUMMY_UNIFORM;
        }

        ShaderCoreUniform shaderCoreUniform = new ShaderCoreUniform(name, type, count, this);
        shaderCoreUniform.setLocation(location);
        this.uniforms.put(name, shaderCoreUniform);

        return (UniformBase) shaderCoreUniform;
    }

    public UniformBase setUniform(String name, UType type) {
        return this.setUniform(name, type, 1);
    }

    public void setSampler(String name, IntSupplier textureIdSupplier) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (this.sampler.containsKey(name)) {
            this.sampler.get(name).setTextureId(textureIdSupplier);
            return;
        }

        int location = ShaderCoreUniform.glGetUniformLocation(this.programId, name);
        if (location == -1) {
            this.sampler.put(name, SamplerBase.DUMMY_SAMPLER);
            return;
        }

        ShaderCoreSampler shaderCoreSampler = new ShaderCoreSampler(name, this.sampler.size(), textureIdSupplier, this);
        shaderCoreSampler.setLocation(location);
        this.sampler.put(name, shaderCoreSampler);
    }

    public void markDirty() {
        this.dirty = true;
    }

    @MethodsReturnNonnullByDefault
    public ShaderCoreLoader getVertexProgram() {
        return this.vertexProgram;
    }

    @MethodsReturnNonnullByDefault
    public ShaderCoreLoader getFragmentProgram() {
        return this.fragmentProgram;
    }

    public int getId() {
        return this.programId;
    }
}
