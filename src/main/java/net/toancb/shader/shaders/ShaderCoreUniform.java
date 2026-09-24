package net.toancb.shader.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.shader.IShaderManager;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreUniform implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private int location;
    private final int count;
    private final UType type;
    private final IntBuffer intValues;
    private final FloatBuffer floatValues;
    private final String name;
    private boolean dirty;
    private final IShaderManager parent;

    public static final ShaderCoreUniform DUMMY = new ShaderCoreUniform("dummy", UType.FLOAT, 1, null);

    private ShaderCoreUniform(String name, UType type, int count, IShaderManager shaderManager) {
        this.name = name;
        this.count = count * type.count();
        this.type = type;
        this.parent = shaderManager;
        if (type.type() <= UType.IVEC4.type()) {
            this.intValues = MemoryUtil.memAllocInt(this.count);
            this.floatValues = null;
        } else {
            this.intValues = null;
            this.floatValues = MemoryUtil.memAllocFloat(this.count);
        }

        this.location = -1;
        this.markDirty();
    }

    public static ShaderCoreUniform create(String name, UType type, int count, IShaderManager shaderManager) {
        return new ShaderCoreUniform(name, type, count, shaderManager);
    }

    public static int glGetUniformLocation(int programId, CharSequence name) {
        return GlStateManager._glGetUniformLocation(programId, name);
    }

    public static void uploadInteger(int location, int value) {
        RenderSystem.glUniform1i(location, value);
    }

    public static int glGetAttribLocation(int programId, CharSequence attributeName) {
        return GlStateManager._glGetAttribLocation(programId, attributeName);
    }

    public void close() {
        if (this.intValues != null) {
            MemoryUtil.memFree(this.intValues);
        }

        if (this.floatValues != null) {
            MemoryUtil.memFree(this.floatValues);
        }

    }

    private void markDirty() {
        this.dirty = true;
        if (this.parent != null) {
            this.parent.markDirty();
        }

    }

    public void setLocation(int location) {
        this.location = location;
    }

    public String getName() {
        return this.name;
    }

    public void writeFloat(float... value) {
        if (this.floatValues == null) {
            LOGGER.warn("Attempted to set float values on non-float uniform '{}' (type: {}). Skipping.", this.name, this.type);
            return;
        }

        if (this.count > value.length) {
            LOGGER.warn("Uniform.set called with a too-small value array (expected {}, got {}). Ignoring.", this.count, value.length);
            return;
        }
        ((Buffer) this.floatValues).position(0);
        this.floatValues.put(value);
        ((Buffer) this.floatValues).position(0);
        this.markDirty();
    }

    public void writeInt(int... value) {
        if (this.intValues == null) {
            LOGGER.warn("Attempted to set int values on non-int uniform '{}' (type: {}). Skipping.", this.name, this.type);
            return;
        }

        if (this.count > value.length) {
            LOGGER.warn("Uniform.set called with a too-small value array (expected {}, got {}). Ignoring.", this.count, value.length);
            return;
        }

        ((Buffer) this.intValues).position(0);
        this.intValues.put(value);
        ((Buffer) this.intValues).position(0);
        this.markDirty();
    }

    public void writeMat(Matrix4f matrix4f) {
        ((Buffer) this.floatValues).position(0);
        matrix4f.store(this.floatValues);
        this.markDirty();
    }

    public void upload() {
        if (!this.dirty) {
            return;
        }

        this.dirty = false;
        if (this.type.type() <= UType.IVEC4.type()) {
            this.uploadAsInteger();
        } else if (this.type.type() <= UType.VEC4.type()) {
            this.uploadAsFloat();
        } else {
            if (this.type.type() > UType.MAT4.type()) {
                LOGGER.warn("Uniform.upload called, but type value ({}) is not a valid type. Ignoring.", this.type);
                return;
            }

            this.uploadAsMatrix();
        }

    }

    private void uploadAsInteger() {
        ((Buffer) this.intValues).clear();
        switch(this.type) {
            case INT:
                RenderSystem.glUniform1(this.location, this.intValues);
                break;
            case IVEC2:
                RenderSystem.glUniform2(this.location, this.intValues);
                break;
            case IVEC3:
                RenderSystem.glUniform3(this.location, this.intValues);
                break;
            case IVEC4:
                RenderSystem.glUniform4(this.location, this.intValues);
                break;
            default:
                LOGGER.warn("Uniform.upload called, but count value ({}) is  not in the range of 1 to 4. Ignoring.", this.count);
        }

    }

    private void uploadAsFloat() {
        ((Buffer) this.floatValues).clear();
        switch(this.type) {
            case FLOAT:
                RenderSystem.glUniform1(this.location, this.floatValues);
                break;
            case VEC2:
                RenderSystem.glUniform2(this.location, this.floatValues);
                break;
            case VEC3:
                RenderSystem.glUniform3(this.location, this.floatValues);
                break;
            case VEC4:
                RenderSystem.glUniform4(this.location, this.floatValues);
                break;
            default:
                LOGGER.warn("Uniform.upload called, but count value ({}) is not in the range of 1 to 4. Ignoring.", this.count);
        }

    }

    private void uploadAsMatrix() {
        ((Buffer) this.floatValues).clear();
        switch(this.type) {
            case MAT2:
                RenderSystem.glUniformMatrix2(this.location, false, this.floatValues);
                break;
            case MAT3:
                RenderSystem.glUniformMatrix3(this.location, false, this.floatValues);
                break;
            case MAT4:
                RenderSystem.glUniformMatrix4(this.location, false, this.floatValues);
        }
    }
}
