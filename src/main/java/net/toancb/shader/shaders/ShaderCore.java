package net.toancb.shader.shaders;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class ShaderCore implements AutoCloseable {
    private final GraphicsCoreInstance effect;
    public final Framebuffer inTarget;
    public final Framebuffer outTarget;
    private final static Minecraft mc = Minecraft.getInstance();
    private final List<IntSupplier> auxAssets = Lists.newArrayList();
    private final List<String> auxNames = Lists.newArrayList();
    private final List<Integer> auxWidths = Lists.newArrayList();
    private final List<Integer> auxHeights = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;

    public ShaderCore(IResourceManager resourceManager, String shaderName, Framebuffer inputTarget, Framebuffer outputTarget) throws IOException {
        this.effect = new GraphicsCoreInstance(resourceManager, shaderName);
        this.inTarget = inputTarget;
        this.outTarget = outputTarget;
    }

    public void close() {
        this.effect.close();
    }

    public void addAuxAsset(String assetName, IntSupplier textureIdSupplier, int width, int height) {
        this.auxNames.add(assetName);
        this.auxAssets.add(textureIdSupplier);
        this.auxWidths.add(width);
        this.auxHeights.add(height);
    }

    public void setOrthoMatrix(Matrix4f orthoMatrix) {
        this.shaderOrthoMatrix = orthoMatrix;
    }

    public void addUniform(Consumer<GraphicsCoreInstance> uniform) {
        int outWidth = this.outTarget.width;
        int outHeight = this.outTarget.height;

        this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);
        this.effect.setSampler("DiffuseDepthSampler", this.inTarget::getDepthTextureId);

        for (int i = 0; i < this.auxAssets.size(); ++i) {
            this.effect.setSampler(this.auxNames.get(i), this.auxAssets.get(i));
            this.effect.setUniform("AuxSize" + i, UType.VEC2).writeFloat(this.auxWidths.get(i), this.auxHeights.get(i));
        }

        this.effect.setUniform("ProjMat", UType.MAT4).writeMat(this.shaderOrthoMatrix);
        this.effect.setUniform("InSize", UType.VEC2).writeFloat(this.inTarget.width, this.inTarget.height);
        this.effect.setUniform("OutSize", UType.VEC2).writeFloat(outWidth, outHeight);

        if (uniform != null) uniform.accept(this.effect);

        this.effect.setUniform("ScreenSize", UType.VEC2).writeFloat(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    public void process(Consumer<GraphicsCoreInstance> uniform) {
        this.inTarget.unbindWrite();
        int outWidth = this.outTarget.width;
        int outHeight = this.outTarget.height;
        RenderSystem.viewport(0, 0, outWidth, outHeight);
        this.addUniform(uniform);

        this.effect.apply();

        this.outTarget.clear(Minecraft.ON_OSX);
        this.outTarget.bindWrite(false);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.alphaFunc(GL11.GL_ALWAYS, 0.0F);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(0.0D, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex(outWidth, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex(outWidth, outHeight, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex(0.0D, outHeight, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.end();
        WorldVertexBufferUploader.end(bufferbuilder);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.defaultAlphaFunc();
        this.effect.clear();
        this.outTarget.unbindWrite();
        this.inTarget.unbindRead();

        for (Object object : this.auxAssets) {
            if (object instanceof Framebuffer) {
                ((Framebuffer) object).unbindRead();
            }
        }
    }

    public GraphicsCoreInstance getEffect() {
        return this.effect;
    }
}