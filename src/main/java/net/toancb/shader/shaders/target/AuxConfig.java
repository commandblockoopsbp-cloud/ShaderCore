package net.toancb.shader.shaders.target;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class AuxConfig extends AuxBuffer {
    public final String name;
    private static final Map<String, AuxConfig> configs = new WeakHashMap<>();

    private AuxConfig(String name, boolean copyDepth, boolean preserveHistory) {
        super(copyDepth, preserveHistory);
        this.name = name;
    }

    public static AuxConfig nothing(String name) {
        return configs.computeIfAbsent(name, key -> new AuxConfig(key, false, false));
    }

    public static AuxConfig depth(String name, boolean copyDepth) {
        return configs.computeIfAbsent(name, key -> new AuxConfig(key, copyDepth, false));
    }

    public static AuxConfig history(String name, boolean preserveHistory) {
        return configs.computeIfAbsent(name, key -> new AuxConfig(key, false, preserveHistory));
    }

    public static AuxConfig of(String name, boolean copyDepth, boolean preserveHistory) {
        return configs.computeIfAbsent(name, key -> new AuxConfig(key, copyDepth, preserveHistory));
    }
}
