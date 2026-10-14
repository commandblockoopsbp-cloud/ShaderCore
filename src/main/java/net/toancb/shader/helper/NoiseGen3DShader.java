package net.toancb.shader.helper;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.UType;

@OnlyIn(Dist.CLIENT)
public class NoiseGen3DShader extends NoiseGenShader {
    private final Matrix4f inforMatrix;
    private static final int pad = 1;

    public NoiseGen3DShader(String samplerName, String bufferName, ResourceLocation shaderLocation, float baseScale, Vector2i noiseSize, Vector2i texSize) {
        super(samplerName, bufferName, shaderLocation, baseScale, noiseSize);
        Vector2i gridSize = noiseSize.divide(texSize);

        int texZ = (gridSize.x() * gridSize.y());

        this.inforMatrix = new Matrix4f(new float[]{
                gridSize.x(), texSize.x(), pad,   baseScale,
                gridSize.y(), texSize.y(), texZ,  0.0f,
                0.0f,         0.0f,        0.0f,  0.0f,
                0.0f,         0.0f,        0.0f,  0.0f
        });
    }

    protected void onShareUniform(GraphicsCoreInstance shader) {
        shader.setUniform(this.samplerName + "Information", UType.MAT4).writeMat(this.inforMatrix);
    }
}