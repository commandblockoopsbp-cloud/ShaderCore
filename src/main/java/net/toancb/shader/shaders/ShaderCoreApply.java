package net.toancb.shader.shaders;

import com.google.gson.JsonSyntaxException;
import com.ibm.icu.impl.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public abstract class ShaderCoreApply {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Framebuffer mainTarget = mc.getMainRenderTarget();
    private ShaderCoreGroup shaderGroup;
    private final Map<String, Pair<Framebuffer, Boolean>> framebuffers = new HashMap<>();

    /**
     * Initialization hook that subclasses must implement.
     * Typically used to invoke {@link #initApply} to load the shader configuration into memory.
     */
    public abstract void init();

    /**
     * Abstract method specifying the ResourceLocation path pointing to the shader's JSON file.
     *
     * @return The ResourceLocation of the shader configuration.
     */
    protected abstract ResourceLocation getShaderLocation();

    /**
     * Initializes the shader system and automatically maps the auxiliary Framebuffers defined in the JSON.
     *
     * @param shaderLocation   The path to the shader configuration file (.json)
     * @param framebufferName Variable arguments of pairs containing [Auxiliary FBO Name, Copy Depth Flag]
     */
    @SafeVarargs
    protected final void initApply(ResourceLocation shaderLocation, Pair<String, Boolean>... framebufferName) {
        if (shaderGroup == null) {
            try {
                shaderGroup = new ShaderCoreGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), shaderLocation);
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

    /**
     * Starts the shader application cycle (typically called at the beginning of a Render Event).
     * This method clears old frame data from auxiliary buffers and copies depth data (distance values)
     * from the main screen if requested, prepping them to capture new graphics.
     */
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

    /**
     * Concludes the shader application cycle (typically called at the end of a Render Event).
     * Triggers the ShaderGroup to execute the post-processing pipeline using the captured FBO data,
     * applies any dynamic uniforms provided, and renders the final composited image directly back onto the player's screen.
     *
     * @param partialTick The frame delta time used for smooth motion interpolation.
     * @param uniforms    Optional dynamic uniforms to apply to the shader passes during processing.
     */
    public void endShader(float partialTick, Uniform... uniforms) {
        if (this.shaderGroup == null) return;
        this.shaderGroup.process(partialTick, uniforms);
        mainTarget.bindWrite(true);
    }

    /**
     * Redirects the render engine output. Any graphics drawn immediately after this call
     * will be captured and written directly into the specified auxiliary Framebuffer instead of the main screen.
     *
     * @param bufferName The target identifier string of the auxiliary Framebuffer.
     */
    public void writeFramebuffer(String bufferName) {
        Pair<Framebuffer, Boolean> first = framebuffers.get(bufferName);
        if (first == null) return;
        Framebuffer framebuffer = first.first;
        if (framebuffer == null) return;
        framebuffer.bindWrite(false);
    }

    /**
     * Automatically scales and adjusts the dimensions of all auxiliary Framebuffers
     * whenever the player resizes the game window, switches to fullscreen (F11), or modifies GUI scaling.
     * This prevents stretching, distortion, or pixel artifacting.
     */
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
