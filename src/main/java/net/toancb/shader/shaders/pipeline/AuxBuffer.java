package net.toancb.shader.shaders.pipeline;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AuxBuffer {
    public final boolean copyDepth;
    public final boolean preserveHistory;

    protected AuxBuffer(boolean copyDepth, boolean preserveHistory) {
        this.copyDepth = copyDepth;
        this.preserveHistory = preserveHistory;
    }
}
