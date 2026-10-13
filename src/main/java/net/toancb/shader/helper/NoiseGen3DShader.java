package net.toancb.shader.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.UType;

@OnlyIn(Dist.CLIENT)
public class NoiseGen3DShader extends NoiseGenShader {
    private final Vector2i noiseSize;
    private final Vector2i texSize;
    private final Vector2i gridSize;
    private static final int pad = 1;

    public NoiseGen3DShader(String samplerName, String bufferName, ResourceLocation shaderLocation, float baseScale, Vector2i noiseSize, Vector2i texSize) {
        super(samplerName, bufferName, shaderLocation, baseScale);
        this.noiseSize = noiseSize;
        this.texSize = texSize;
        this.gridSize = noiseSize.divide(texSize);
    }

    public void shareUniform(GraphicsCoreInstance shader) {
        super.shareUniform(shader);
        shader.setUniform("TexSize", UType.VEC2).writeFloat(texSize.x(), texSize.y());
        shader.setUniform("NoiseSize", UType.VEC2).writeFloat(noiseSize.x(), noiseSize.y());
        shader.setUniform("GridMax", UType.VEC2).writeFloat(gridSize.x(), gridSize.y());
        shader.setUniform("SafePadding", UType.FLOAT).writeFloat(pad);
    }

    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        this.shareUniform(shader);
    }

    protected void onGroupBuilt() {
        this.getFramebuffer(this.bufferName).resize(this.noiseSize.x(), this.noiseSize.y(), Minecraft.ON_OSX);
        super.onGroupBuilt();
    }
}