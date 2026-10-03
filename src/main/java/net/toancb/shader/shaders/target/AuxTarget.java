package net.toancb.shader.shaders.target;

import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AuxTarget extends AuxBuffer {
    public final Framebuffer framebuffer;

    public AuxTarget(Framebuffer framebuffer, boolean copyDepth, boolean preserveHistory) {
        super(copyDepth,  preserveHistory);
        this.framebuffer = framebuffer;
    }
}
