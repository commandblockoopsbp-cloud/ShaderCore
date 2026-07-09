package net.toancb.shader.shaders;

import com.google.gson.JsonSyntaxException;
import com.ibm.icu.impl.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public abstract class ShaderApply {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Framebuffer mainTarget = mc.getMainRenderTarget();
    private ShaderGroup shaderGroup;
    private final Map<String, Pair<Framebuffer, Boolean>> framebuffers = new HashMap<>();

    public abstract void init();

    protected abstract ResourceLocation getShaderLocation();

    @SafeVarargs
    protected final void initApply(ResourceLocation shaderLocation, Pair<String, Boolean>... framebufferName) {
        if (shaderGroup == null) {
            try {
                shaderGroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), shaderLocation);
                shaderGroup.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                for (Pair<String, Boolean> buffer : framebufferName) {
                    framebuffers.put(buffer.first, Pair.of(shaderGroup.getTempTarget(buffer.first), buffer.second));
                }
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.warn("Failed to load shader: {}", shaderLocation, e);
                shaderGroup = null;
            }
        }
    }

    public void applyShader() {
        if (this.framebuffers.isEmpty()) return;
        for (Pair<Framebuffer, Boolean> framebuffer : framebuffers.values()) {
            framebuffer.first.clear(Minecraft.ON_OSX);
            if (framebuffer.second) {
                framebuffer.first.copyDepthFrom(mainTarget);
            }
        }
        mainTarget.bindWrite(false);
    }

    public void endShader(float partialTick) {
        if (this.shaderGroup == null) return;
        this.shaderGroup.process(partialTick);
        mainTarget.bindWrite(true);
    }

    public void writeFramebuffer(String bufferName) {
        Pair<Framebuffer, Boolean> first = framebuffers.get(bufferName);
        if (first == null) return;
        Framebuffer framebuffer = first.first;
        if (framebuffer == null) return;
        framebuffer.bindWrite(false);
    }

    public void resize() {
        if (shaderGroup == null || framebuffers.isEmpty()) return;

        Pair<Framebuffer, Boolean> firstPair = framebuffers.values().iterator().next();
        Framebuffer sampleFbo = firstPair.first;

        if (sampleFbo != null) {
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();
            if (width != sampleFbo.width || height != sampleFbo.height) {
                this.shaderGroup.resize(width, height);
            }
        }
    }
}
