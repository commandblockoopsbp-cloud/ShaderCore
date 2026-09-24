package net.toancb.shader.shaders;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class ShaderHelper {
    private static final Minecraft mc = Minecraft.getInstance();
    private static boolean wasF9Pressed = false;

    public static Vector3f convertWorldToTexel(MatrixStack matrixStack, Vector3f pos3D, ActiveRenderInfo cam, float partialTick) {
        Matrix4f projMatrix = mc.gameRenderer.getProjectionMatrix(cam, partialTick, true);
        Matrix4f modelViewMatrix = matrixStack.last().pose();
        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        float relX = (float) (pos3D.x() - cam.getPosition().x);
        float relY = (float) (pos3D.y() - cam.getPosition().y);
        float relZ = (float) (pos3D.z() - cam.getPosition().z);

        Vector4f viewPos = new Vector4f(relX, relY, relZ, 1.0F);
        viewPos.transform(modelViewMatrix);
        viewPos.transform(projMatrix);
        Vector3f texelPos = new Vector3f(viewPos.x(), viewPos.y(), viewPos.z());
        texelPos.mul(0.5f / viewPos.w());
        texelPos.add(0.5f, 0.5f, 0.5f);
        texelPos.mul(screenWidth, screenHeight, 1);

        return texelPos;
    }

    public static Consumer<ShaderCoreInstance> addDefaultUniform(Matrix4f model, Matrix4f projection) {
        ActiveRenderInfo cam = mc.gameRenderer.getMainCamera();

        Matrix4f inverseModelMatrix = model.copy();
        inverseModelMatrix.invert();
        Matrix4f inverseProjectionMatrix = projection.copy();
        inverseProjectionMatrix.invert();

        Vector3f camPos = new Vector3f(cam.getPosition());

        return shader -> {
            shader.getOrCreateUniform("ModelViewMatrix", UType.MAT4).writeMat(model);
            shader.getOrCreateUniform("InverseModelViewMatrix", UType.MAT4).writeMat(inverseModelMatrix);
            shader.getOrCreateUniform("ProjectionMatrix", UType.MAT4).writeMat(projection);
            shader.getOrCreateUniform("InverseProjectionMatrix", UType.MAT4).writeMat(inverseProjectionMatrix);
            shader.getOrCreateUniform("CameraPos", UType.VEC3).writeFloat(camPos.x(), camPos.y(), camPos.z());
        };
    }

    public static void fastReloadShaders(ShaderCoreApply shader) {
        if (shader == null) return;

        boolean isF9Down = InputMappings.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_F9);

        if (isF9Down && !wasF9Pressed) {
            shader.close();
        }

        wasF9Pressed = isF9Down;
    }
}
