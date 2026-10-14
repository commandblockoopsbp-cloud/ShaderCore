package net.toancb.shader.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.ShaderCoreApply;
import net.toancb.shader.shaders.UType;
import net.toancb.shader.shaders.pipeline.AuxConfig;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public abstract class NoiseGenShader extends ShaderCoreApply {
    protected final String samplerName;
    protected final String bufferName;
    protected final ResourceLocation shaderLocation;
    protected final float baseScale;
    protected final Vector2i noiseSize;

    public NoiseGenShader(String samplerName, String bufferName, ResourceLocation shaderLocation, float baseScale, Vector2i noiseSize) {
        if (samplerName == null || samplerName.isEmpty()) {
            throw new IllegalArgumentException("Sampler name cannot be null or empty!");
        }
        if (bufferName == null || bufferName.isEmpty()) {
            throw new IllegalArgumentException("Buffer name cannot be null or empty!");
        }
        if (baseScale <= 0) {
            throw new IllegalArgumentException("BaseScale must be greater than 0!");
        }
        if (noiseSize.x() <= 0 || noiseSize.y() <= 0) {
            throw new IllegalArgumentException("Noise size dimensions (x and y) must be greater than 0!");
        }

        this.bufferName = bufferName;
        this.samplerName = samplerName;
        this.shaderLocation = shaderLocation;
        this.baseScale = baseScale;
        this.noiseSize = noiseSize;
    }

    protected void setupNoiseTextureFilter(String targetName) {
        Framebuffer fb = this.getFramebuffer(targetName);
        RenderSystem.bindTexture(fb.getColorTextureId());
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        RenderSystem.bindTexture(0);
    }

    public void shareUniform(GraphicsCoreInstance shader) {
        shader.setSampler(this.samplerName, this.getFramebuffer(this.bufferName)::getColorTextureId);
        this.onShareUniform(shader);
    }

    protected void onShareUniform(GraphicsCoreInstance shader) {}

    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        this.onShareUniform(shader);
    }

    protected void onGroupBuilt() {
        this.getFramebuffer(this.bufferName).resize(this.noiseSize.x(), this.noiseSize.y(), Minecraft.ON_OSX);
        this.setupNoiseTextureFilter(this.bufferName);
    }

    protected void onPostInit() {
        this.applyShader();
        this.endShader(null);
    }

    public void init() {
        super.initApply(AuxConfig.nothing(this.bufferName));
    }

    protected ResourceLocation getShaderLocation() {
        return this.shaderLocation;
    }

    @Override
    protected boolean requiresCustomState() {
        return false;
    }
}