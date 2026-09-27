package net.toancb.shader.shaders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IShaderCoreManager {
    int getId();

    void markDirty();

    ShaderCoreLoader getVertexProgram();

    ShaderCoreLoader getFragmentProgram();
}
