package net.toancb.shader.shaders;

import com.ibm.icu.impl.Row;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShaderHelper {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Row.R2<Integer, Integer> convertWorldToTexel(MatrixStack matrixStack, Vector3f pos3D, ActiveRenderInfo cam, float partialTick) {
        Matrix4f projMatrix = mc.gameRenderer.getProjectionMatrix(cam, partialTick, true);
        Matrix4f modelViewMatrix = matrixStack.last().pose();
        int screenWidth = mc.getWindow().getWidth();
        int screenHeight = mc.getWindow().getHeight();
        float relX = (float) (pos3D.x() - cam.getPosition().x);
        float relY = (float) (pos3D.y() - cam.getPosition().y);
        float relZ = (float) (pos3D.z() - cam.getPosition().z);

        int texelX = -999;
        int texelY = -999;

        Vector4f viewPos = new Vector4f(relX, relY, relZ, 1.0F);
        viewPos.transform(modelViewMatrix);
        viewPos.transform(projMatrix);
        Vector3f screenPos = new Vector3f(viewPos.x(), viewPos.y(), viewPos.z());
        if (viewPos.w() > 0) {
            screenPos.mul(0.5f / viewPos.w());
            screenPos.add(0.5f, 0.5f, 0.5f);

            texelX = (int) Math.floor(screenPos.x() * screenWidth + 0.5d);
            texelY = (int) Math.floor(screenPos.y() * screenHeight + 0.5d);
        }

        return Row.of(texelX, texelY);
    }
}
