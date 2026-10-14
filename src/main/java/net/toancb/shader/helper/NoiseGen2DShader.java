package net.toancb.shader.helper;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.UType;

@OnlyIn(Dist.CLIENT)
public class NoiseGen2DShader extends NoiseGenShader {
    public NoiseGen2DShader(String samplerName, String bufferName, ResourceLocation shaderLocation, float baseScale, Vector2i noiseSize) {
        super(samplerName, bufferName, shaderLocation, baseScale, noiseSize);
    }

    protected void onShareUniform(GraphicsCoreInstance shader) {
        shader.setUniform("BaseScale", UType.FLOAT).writeFloat(this.baseScale);
    }
}