package net.toancb.shader.shaders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum UType {
    INT(0, 1),
    IVEC2(1, 2),
    IVEC3(2, 3),
    IVEC4(3, 4),
    FLOAT(4, 1),
    VEC2(5, 2),
    VEC3(6, 3),
    VEC4(7, 4),
    MAT2(8, 4),
    MAT3(9, 9),
    MAT4(10, 16);

    private final int type, count;

    UType(int type, int count) {
        this.type = type;
        this.count = count;
    }

    public int type() {
        return type;
    }

    public int count() {
        return count;
    }
}
