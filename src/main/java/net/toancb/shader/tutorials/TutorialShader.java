package net.toancb.shader.tutorials;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.helper.NoiseGenManager;
import net.toancb.shader.helper.NoiseGenShader;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.ShaderCoreApply;
import net.toancb.shader.shaders.UType;
import net.toancb.shader.shaders.pipeline.AuxConfig;

@OnlyIn(Dist.CLIENT)
public class TutorialShader extends ShaderCoreApply {
    private static final TutorialShader INSTANCE = new TutorialShader();
    private static final NoiseGenShader[] NOISE_GEN = {
            NoiseGenManager.PERLIN3D,
            NoiseGenManager.WORLEY3D,
            NoiseGenManager.CURL3D,
            NoiseGenManager.WORLEY2D,
            NoiseGenManager.PERLIN2D,
    };

    public void init() {
        for (NoiseGenShader shader : NOISE_GEN) {
            shader.init();
        }
        super.initApply(AuxConfig.depth("tuto", true));
    }

    protected ResourceLocation getShaderLocation() {
        return new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/tutorials.json");
    }

    @Override
    protected boolean requiresCustomState() {
        return true;
    }

    @Override
    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {
        float time = (System.nanoTime() * 1e-9f) % 20000f;
        shader.setUniform("Time", UType.FLOAT).writeFloat(time);
    }

    public static TutorialShader getInstance() {
        return INSTANCE;
    }
}
