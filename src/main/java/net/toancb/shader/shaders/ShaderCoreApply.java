package net.toancb.shader.shaders;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public abstract class ShaderCoreApply implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Framebuffer mainTarget = mc.getMainRenderTarget();
    private ShaderCoreGroup shaderGroup;
    private final Map<String, AuxTarget> framebuffers = new HashMap<>();
    private boolean active = true;
    private boolean hasError = false;
    private static long pastTime = System.currentTimeMillis();

    protected static class AuxConfig {
        public final String name;
        public final boolean copyDepth;

        public AuxConfig(String name, boolean copyDepth) {
            this.name = name;
            this.copyDepth = copyDepth;
        }

        public static AuxConfig of(String name, boolean copyDepth) {
            return new AuxConfig(name, copyDepth);
        }
    }

    private static class AuxTarget {
        public final Framebuffer framebuffer;
        public final boolean copyDepth;

        public AuxTarget(Framebuffer framebuffer, boolean copyDepth) {
            this.framebuffer = framebuffer;
            this.copyDepth = copyDepth;
        }
    }

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
     * @param framebufferName Variable arguments of pairs containing [Auxiliary FBO Name, Copy Depth Flag]
     */
    protected final void initApply(AuxConfig... framebufferName) {
        if (this.shaderGroup != null) return;
        if (System.currentTimeMillis() - pastTime > 1000) {
            pastTime = System.currentTimeMillis();
            this.hasError = false;
        }
        if (!hasError) {
            try {
                shaderGroup = new ShaderCoreGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), this.getShaderLocation());
                shaderGroup.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                this.framebuffers.clear();
                for (AuxConfig buffer : framebufferName) {
                    framebuffers.put(buffer.name, new AuxTarget(shaderGroup.getTempTarget(buffer.name), buffer.copyDepth));
                }
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.warn("Failed to load shader: {}", this.getShaderLocation(), e);
                shaderGroup = null;
                this.framebuffers.clear();
                this.hasError = true;
            }
        }
    }

    /**
     * Starts the shader application cycle (typically called at the beginning of a Render Event).
     * This method clears old frame data from auxiliary buffers and copies depth data (distance values)
     * from the main screen if requested, prepping them to capture new graphics.
     */
    public void applyShader() {
        if (!this.isActive() || this.framebuffers.isEmpty()) return;
        for (AuxTarget framebuffer : framebuffers.values()) {
            framebuffer.framebuffer.clear(Minecraft.ON_OSX);
            if (framebuffer.copyDepth) {
                framebuffer.framebuffer.copyDepthFrom(mainTarget);
            }
        }
        mainTarget.bindWrite(false);
    }

    /**
     * Concludes the shader application cycle (typically called at the end of a Render Event).
     * Triggers the ShaderGroup to execute the post-processing pipeline using the captured FBO data,
     * applies any dynamic uniforms provided, and renders the final composited image directly back onto the player's screen.
     *
     * @param uniform    Optional dynamic uniforms to apply to the shader passes during processing.
     */
    public void endShader(Consumer<ShaderCoreInstance> uniform) {
        if (!this.isActive()) return;
        Consumer<ShaderCoreInstance> consumer = shader -> {
            onApplyCustomUniform(shader);
            if (uniform != null) uniform.accept(shader);
        };
        this.shaderGroup.process(consumer);
        mainTarget.bindWrite(true);
    }

    /**
     * Hook method called when applying custom uniforms to the shader instance.
     * Subclasses can override this to bind their own custom uniform values.
     *
     * @param shader the current shader core instance
     */
    protected void onApplyCustomUniform(ShaderCoreInstance shader) {}

    /**
     * Redirects the render engine output. Any graphics drawn immediately after this call
     * will be captured and written directly into the specified auxiliary Framebuffer instead of the main screen.
     *
     * @param bufferName The target identifier string of the auxiliary Framebuffer.
     */
    public void writeFramebuffer(String bufferName) {
        AuxTarget first = framebuffers.get(bufferName);
        if (first == null) return;
        Framebuffer framebuffer = first.framebuffer;
        if (framebuffer == null) return;
        framebuffer.bindWrite(false);
    }

    /**
     * Automatically scales and adjusts the dimensions of all auxiliary Framebuffers
     * whenever the player resizes the game window, switches to fullscreen (F11), or modifies GUI scaling.
     * This prevents stretching, distortion, or pixel artifacting.
     */
    public void resize() {
        if (!this.isActive() || framebuffers.isEmpty()) return;

        AuxTarget firstPair = framebuffers.values().iterator().next();
        Framebuffer sampleFbo = firstPair.framebuffer;

        if (sampleFbo != null) {
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();
            if (width != sampleFbo.width || height != sampleFbo.height) {
                this.shaderGroup.resize(width, height);
            }
        }
    }

    /**
     * Sets the active state of this shader core.
     *
     * @param active true to set as active, false otherwise
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks whether this shader core is currently active and ready.
     *
     * @return true if the shader group is initialized and active, false otherwise
     */
    public boolean isActive() {
        return this.shaderGroup != null && this.active;
    }

    /**
     * Closes and releases resources associated with this shader core,
     * including the shader group and framebuffers.
     */
    public void close() {
        if (this.shaderGroup != null) {
            this.shaderGroup.close();
            this.shaderGroup = null;
        }
        this.framebuffers.clear();
    }
}
