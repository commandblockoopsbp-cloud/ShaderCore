package net.toancb.shader.shaders.target;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AuxConfig extends AuxBuffer {
    public final String name;

    private AuxConfig(String name, boolean copyDepth, boolean preserveHistory) {
        super(copyDepth, preserveHistory);
        this.name = name;
    }

    public static AuxConfig of(String name, boolean copyDepth) {
        return new AuxConfig(name, copyDepth, false);
    }

    public static AuxConfig history(String name, boolean preserveHistory) {
        return new AuxConfig(name, false, preserveHistory);
    }

    public static AuxConfig create(String name, boolean copyDepth, boolean preserveHistory) {
        return new AuxConfig(name, copyDepth, preserveHistory);
    }
}
