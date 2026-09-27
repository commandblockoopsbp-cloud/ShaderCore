package net.toancb.shader.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreLinkHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void glUseProgram(int programId) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        GlStateManager._glUseProgram(programId);
    }

    public static void releaseProgram(IShaderCoreManager manager) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        manager.getFragmentProgram().close();
        manager.getVertexProgram().close();
        GlStateManager.glDeleteProgram(manager.getId());
    }

    public static int createProgram() throws IOException {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        int programId = GlStateManager.glCreateProgram();
        if (programId <= 0) {
            throw new IOException("Could not create shader program (returned program ID " + programId + ")");
        } else {
            return programId;
        }
    }

    public static void linkProgram(IShaderCoreManager manager) throws IOException {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        manager.getFragmentProgram().attachToEffect(manager);
        manager.getVertexProgram().attachToEffect(manager);
        GlStateManager.glLinkProgram(manager.getId());
        int linkStatus = GlStateManager.glGetProgrami(manager.getId(), 35714);
        if (linkStatus == 0) {
            LOGGER.warn("Error encountered when linking program containing VS {} and FS {}. Log output:", manager.getVertexProgram().getName(), manager.getFragmentProgram().getName());
            LOGGER.warn(GlStateManager.glGetProgramInfoLog(manager.getId(), 32768));
        }
    }
}
