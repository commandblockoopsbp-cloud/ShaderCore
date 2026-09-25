package net.toancb.shader.shaders;

import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreDefault {
    public static final ShaderCoreDefault DUMMY_UNIFORM = new ShaderCoreDefault();

    protected ShaderCoreDefault() {}

    public void writeFloat(float... value) {
    }

    public void writeInt(int... value) {
    }

    public void writeMat(Matrix4f matrix4f) {
    }
}
