package net.toancb.shader.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.ShaderCoreApply;
import net.toancb.shader.shaders.UType;
import net.toancb.shader.shaders.pipeline.AuxConfig;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class NoiseGenShader extends ShaderCoreApply {
    private static final NoiseGenShader INSTANCE = new NoiseGenShader();
    private static final Vector2i TEX_SIZE = new Vector2i(128, 128);
    private static final Vector2i SIZE_3D = new Vector2i(1024, 1024);
    private static final Vector2i SIZE_2D = new Vector2i(512, 512);
    private static final int CHANNEL = 3;
    private float baseScale = 16.0f;

    private NoiseGenShader() {}

    private void setupNoiseTextureFilter(String targetName) {
        Framebuffer fb = this.getFramebuffer(targetName);
        fb.setFilterMode(GL11.GL_LINEAR);
    }

    public void addNoiseSampler(GraphicsCoreInstance shader) {
        shader.setSampler("WorleyNoise3DTexture", this.getFramebuffer("3d_worley_noise")::getColorTextureId);
        shader.setSampler("WorleyNoise2DTexture", this.getFramebuffer("2d_worley_noise")::getColorTextureId);
        shader.setSampler("PerlinNoise3DTexture", this.getFramebuffer("3d_perlin_noise")::getColorTextureId);
        shader.setSampler("PerlinNoise2DTexture", this.getFramebuffer("2d_perlin_noise")::getColorTextureId);
    }

    public void shareUniform(GraphicsCoreInstance shader) {
        shader.setUniform("BaseScale", UType.FLOAT).writeFloat(this.baseScale);
        shader.setUniform("Channel", UType.FLOAT).writeFloat(CHANNEL);
        int texZ = (SIZE_3D.x() / TEX_SIZE.x()) * (SIZE_3D.y() / TEX_SIZE.y()) * CHANNEL;
        shader.setUniform("TexSize", UType.VEC3).writeFloat(TEX_SIZE.x(), TEX_SIZE.y(), texZ);
        shader.setUniform("Size3D", UType.VEC2).writeFloat(SIZE_3D.x(), SIZE_3D.y());
        shader.setUniform("Size2D", UType.VEC2).writeFloat(SIZE_2D.x(), SIZE_2D.y());
    }

    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        this.shareUniform(shader);
    }

    protected void onGroupBuilt() {
        this.getFramebuffer("3d_worley_noise").resize(SIZE_3D.x(), SIZE_3D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("2d_worley_noise").resize(SIZE_2D.x(), SIZE_2D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("3d_perlin_noise").resize(SIZE_3D.x(), SIZE_3D.y(), Minecraft.ON_OSX);
        this.getFramebuffer("2d_perlin_noise").resize(SIZE_2D.x(), SIZE_2D.y(), Minecraft.ON_OSX);

        this.setupNoiseTextureFilter("3d_worley_noise");
        this.setupNoiseTextureFilter("2d_worley_noise");
        this.setupNoiseTextureFilter("3d_perlin_noise");
        this.setupNoiseTextureFilter("2d_perlin_noise");
    }

    protected void onPostInit() {
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

    @Override
    protected boolean requiresCustomState() {
        return false;
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