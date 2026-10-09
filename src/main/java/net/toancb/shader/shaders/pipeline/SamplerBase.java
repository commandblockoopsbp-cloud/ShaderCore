package net.toancb.shader.shaders.pipeline;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class SamplerBase {
    public static final SamplerBase DUMMY_SAMPLER = new SamplerBase();

    public void close() {}

    public void upload() {}

    public void setTextureId(IntSupplier textureIdSupplier) {}
}
