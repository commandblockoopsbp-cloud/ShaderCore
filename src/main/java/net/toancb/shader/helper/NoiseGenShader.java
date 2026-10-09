package net.toancb.shader.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.ShaderCoreApply;
import net.toancb.shader.shaders.UType;
import net.toancb.shader.shaders.target.AuxConfig;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class NoiseGenShader extends ShaderCoreApply {
    private static final NoiseGenShader INSTANCE = new NoiseGenShader();
    public static final Vector2i SIZE_3D = new Vector2i(512, 1024);
    public static final Vector2i SIZE_2D = new Vector2i(512, 512);
    private float baseScale = 16.0f;

    private NoiseGenShader() {}

    private void setupNoiseTextureFilter(String targetName) {
        Framebuffer fb = this.getFramebuffer(targetName);
        if (fb != null) {
            RenderSystem.bindTexture(fb.getColorTextureId());

            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
    }

    public void addNoiseSampler(GraphicsCoreInstance shader) {
        shader.setSampler("WorleyNoise3DTexture", this.getFramebuffer("3d_worley_noise")::getColorTextureId);
        shader.setSampler("WorleyNoise2DTexture", this.getFramebuffer("2d_worley_noise")::getColorTextureId);
        shader.setSampler("PerlinNoise3DTexture", this.getFramebuffer("3d_perlin_noise")::getColorTextureId);
        shader.setSampler("PerlinNoise2DTexture", this.getFramebuffer("2d_perlin_noise")::getColorTextureId);
    }

    public void shareUniform(GraphicsCoreInstance shader) {
        shader.setUniform("BaseScale", UType.FLOAT).writeFloat(baseScale);
        shader.setUniform("Channel", UType.FLOAT).writeFloat(4.0f);
        shader.setUniform("TexSize", UType.VEC2).writeFloat(128.0f, 128.0f);
        shader.setUniform("ZSize", UType.FLOAT).writeFloat(128.0f);
        shader.setUniform("Size3D", UType.VEC2).writeFloat(SIZE_3D.x(), SIZE_3D.y());
        shader.setUniform("Size2D", UType.VEC2).writeFloat(SIZE_2D.x(), SIZE_2D.y());
    }

    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        this.shareUniform(shader);
    }

    protected void onPreInitApply() {
        this.getFramebuffer("3d_worley_noise").resize(SIZE_3D.x(), SIZE_3D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("2d_worley_noise").resize(SIZE_2D.x(), SIZE_2D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("3d_perlin_noise").resize(SIZE_3D.x(), SIZE_3D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("2d_perlin_noise").resize(SIZE_2D.x(), SIZE_2D.y(), Minecraft.ON_OSX);

        this.setupNoiseTextureFilter("3d_worley_noise");
        this.setupNoiseTextureFilter("2d_worley_noise");
        this.setupNoiseTextureFilter("3d_perlin_noise");
        this.setupNoiseTextureFilter("2d_perlin_noise");
    }

    protected void onPostInitApply() {
        this.applyShader();
        this.endShader(null);
    }

    public void init() {
        super.initApply(
                AuxConfig.nothing("3d_worley_noise"),
                AuxConfig.nothing("2d_worley_noise"),
                AuxConfig.nothing("3d_perlin_noise"),
                AuxConfig.nothing("2d_perlin_noise")
        );
    }

    protected ResourceLocation getShaderLocation() {
        return new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise.json");
    }

    public static NoiseGenShader getInstance() {
        return INSTANCE;
    }

    public float getBaseScale() {
        return baseScale;
    }

    public void setBaseScale(float baseScale) {
        this.baseScale = baseScale;
    }
}