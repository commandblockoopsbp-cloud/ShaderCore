package net.toancb.shader.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.UType;

@OnlyIn(Dist.CLIENT)
public class NoiseGen2DShader extends NoiseGenShader {
    private final Vector2i noiseSize;

    public NoiseGen2DShader(String samplerName, String bufferName, ResourceLocation shaderLocation, float baseScale, Vector2i noiseSize) {
        super(samplerName, bufferName, shaderLocation, baseScale);
        this.noiseSize = noiseSize;
    }

    public void shareUniform(GraphicsCoreInstance shader) {
        super.shareUniform(shader);
        shader.setUniform("NoiseSize", UType.VEC2).writeFloat(this.noiseSize.x(), this.noiseSize.y());
    }

    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        this.shareUniform(shader);
    }

    protected void onGroupBuilt() {
        this.getFramebuffer(this.bufferName).resize(this.noiseSize.x(), this.noiseSize.y(), Minecraft.ON_OSX);
        super.onGroupBuilt();
    }
}