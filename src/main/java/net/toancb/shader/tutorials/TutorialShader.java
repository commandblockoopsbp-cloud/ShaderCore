package net.toancb.shader.tutorials;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;
import net.toancb.shader.shaders.ShaderCoreApply;

@OnlyIn(Dist.CLIENT)
public class TutorialShader extends ShaderCoreApply {
    private static final TutorialShader INSTANCE = new TutorialShader();

    public void init() {
        super.initApply(AuxConfig.of("tuto", true));
    }

    protected ResourceLocation getShaderLocation() {
        return new  ResourceLocation(ShaderCoreMod.MODID, "shaders/post/tutorials.json");
    }

    public static TutorialShader getInstance() {
        return INSTANCE;
    }
}
