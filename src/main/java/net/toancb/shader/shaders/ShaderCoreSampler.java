package net.toancb.shader.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.pipeline.SamplerBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;

import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreSampler extends SamplerBase implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private int location;
    private final String name;
    private final int samplerId;
    private IntSupplier textureIdSupplier;
    private final IShaderCoreManager parent;

    public ShaderCoreSampler(String name, int samplerId, IntSupplier textureIdSupplier, IShaderCoreManager parent) {
        this.name = name;
        this.samplerId = samplerId;
        this.textureIdSupplier = textureIdSupplier;
        this.parent = parent;

        this.location = -1;
        this.markDirty();
    }

    private void markDirty() {
        if (this.parent != null) {
            this.parent.markDirty();
        }
    }

    public void upload() {
        int j = this.textureIdSupplier.getAsInt();
        if (this.location == -1) {
            LOGGER.info("Sampler '{}' skipped: Uniform location is -1 (not found in shader program).", this.name);
            return;
        }

        if (j == -1) {
            LOGGER.info("Sampler '{}' skipped: Texture ID is -1 (texture not ready or unassigned).", this.name);
            return;
        }

        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + this.samplerId);
        RenderSystem.enableTexture();
        RenderSystem.bindTexture(j);
        RenderSystem.glUniform1i(this.location, this.samplerId);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void close() {
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + this.samplerId);
        GlStateManager._disableTexture();
        GlStateManager._bindTexture(0);
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public void setTextureId(IntSupplier textureIdSupplier) {
        this.textureIdSupplier = textureIdSupplier;
    }
}
