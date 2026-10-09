package net.toancb.shader.tutorials;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.helper.NoiseGenShader;
import net.toancb.shader.shaders.GraphicsCoreInstance;
import net.toancb.shader.shaders.ShaderCoreApply;
import net.toancb.shader.shaders.pipeline.AuxConfig;

@OnlyIn(Dist.CLIENT)
public class TutorialShader extends ShaderCoreApply {
    private static final TutorialShader INSTANCE = new TutorialShader();
    private static final NoiseGenShader NOISE_GEN = NoiseGenShader.getInstance();

    public void init() {
        NOISE_GEN.init();
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
        NOISE_GEN.shareUniform(shader);
        NOISE_GEN.addNoiseSampler(shader);
    }

    public static TutorialShader getInstance() {
        return INSTANCE;
    }
}
