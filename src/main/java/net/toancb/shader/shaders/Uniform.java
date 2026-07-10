package net.toancb.shader.shaders;

import net.minecraft.client.shader.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class Uniform {
    private final Consumer<ShaderInstance> newUniform;

    private Uniform(Consumer<ShaderInstance> newUniform) {
        this.newUniform = newUniform;
    }

    public static Uniform of(Consumer<ShaderInstance> newUniform) {
        return new Uniform(newUniform);
    }

    public void apply(ShaderInstance shader) {
        this.newUniform.accept(shader);
    }
}
