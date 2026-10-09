package net.toancb.shader.shaders.pipeline;

import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UniformBase {
    public static final UniformBase DUMMY_UNIFORM = new UniformBase();

    protected UniformBase() {}

    public void writeFloat(float... value) {
    }

    public void writeInt(int... value) {
    }

    public void writeMat(Matrix4f matrix4f) {
    }

    public void upload() {}
}
