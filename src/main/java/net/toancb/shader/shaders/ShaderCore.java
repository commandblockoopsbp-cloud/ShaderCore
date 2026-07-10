package net.toancb.shader.shaders;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderInstance;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.io.IOException;
import java.util.List;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class ShaderCore implements AutoCloseable {
    private final ShaderInstance effect;
    public final Framebuffer inTarget;
    public final Framebuffer outTarget;
    private final List<IntSupplier> auxAssets = Lists.newArrayList();
    private final List<String> auxNames = Lists.newArrayList();
    private final List<Integer> auxWidths = Lists.newArrayList();
    private final List<Integer> auxHeights = Lists.newArrayList();
    private Matrix4f shaderOrthoMatrix;

    public ShaderCore(IResourceManager resourceManager, String shaderName, Framebuffer inputTarget, Framebuffer outputTarget) throws IOException {
        this.effect = new ShaderInstance(resourceManager, shaderName);
        this.inTarget = inputTarget;
        this.outTarget = outputTarget;
    }

    public void close() {
        this.effect.close();
    }

    public void addAuxAsset(String assetName, IntSupplier textureIdSupplier, int width, int height) {
        this.auxNames.add(this.auxNames.size(), assetName);
        this.auxAssets.add(this.auxAssets.size(), textureIdSupplier);
        this.auxWidths.add(this.auxWidths.size(), width);
        this.auxHeights.add(this.auxHeights.size(), height);
    }

    public void setOrthoMatrix(Matrix4f orthoMatrix) {
        this.shaderOrthoMatrix = orthoMatrix;
    }

    public void process(float partialTicks, Uniform... uniforms) {
        this.inTarget.unbindWrite();

        float outWidth = (float) this.outTarget.width;
        float outHeight = (float) this.outTarget.height;

        RenderSystem.viewport(0, 0, (int) outWidth, (int) outHeight);
        this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);

        for (int i = 0; i < this.auxAssets.size(); ++i) {
            this.effect.setSampler(this.auxNames.get(i), this.auxAssets.get(i));
            this.effect.safeGetUniform("AuxSize" + i).set((float) this.auxWidths.get(i).intValue(), (float) this.auxHeights.get(i).intValue());
        }

        this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
        this.effect.safeGetUniform("InSize").set((float) this.inTarget.width, (float) this.inTarget.height);
        this.effect.safeGetUniform("OutSize").set(outWidth, outHeight);
        this.effect.safeGetUniform("Time").set(partialTicks);

        for (Uniform uniform : uniforms) {
            uniform.apply(this.effect);
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.effect.safeGetUniform("ScreenSize").set((float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight());
        this.effect.apply();

        this.outTarget.clear(Minecraft.ON_OSX);
        this.outTarget.bindWrite(false);
        RenderSystem.depthFunc(519);

        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(0.0D, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex((double) outWidth, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex((double) outWidth, (double) outHeight, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.vertex(0.0D, (double) outHeight, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.end();

        WorldVertexBufferUploader.end(bufferbuilder);
        RenderSystem.depthFunc(515);
        this.effect.clear();
        this.outTarget.unbindWrite();
        this.inTarget.unbindRead();

        for (Object object : this.auxAssets) {
            if (object instanceof Framebuffer) {
                ((Framebuffer) object).unbindRead();
            }
        }
    }

    public ShaderInstance getEffect() {
        return this.effect;
    }
}