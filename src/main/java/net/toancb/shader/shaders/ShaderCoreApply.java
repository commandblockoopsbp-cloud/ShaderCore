package net.toancb.shader.shaders;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.shaders.target.AuxConfig;
import net.toancb.shader.shaders.target.AuxTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.LongStream;

@OnlyIn(Dist.CLIENT)
public abstract class ShaderCoreApply implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Framebuffer mainTarget = mc.getMainRenderTarget();
    private ShaderCoreGroup shaderGroup;
    private final Map<String, AuxTarget> framebuffers = new HashMap<>();
    private boolean active = true;
    private static final long[] pastTime = LongStream.generate(System::currentTimeMillis).limit(2).toArray();
    private boolean pendingResize = false;
    private int lastCheckedWidth = -1;
    private int lastCheckedHeight = -1;

    protected ShaderCoreApply() {}

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
     * Executed right before the shader group and temporary framebuffers are built,
     * allowing you to prepare any pre-requisite states or clean up existing resources.
     */
    protected void onPreInitApply() {}

    /**
     * Executed immediately after the shader group finishes initialization and
     * framebuffers are successfully mapped, used for post-setup configurations.
     */
    protected void onPostInitApply() {}

    /**
     * Initializes the shader system and automatically maps the auxiliary Framebuffers defined in the JSON.
     *
     * @param framebufferName Variable arguments of pairs containing [Auxiliary FBO Name, Copy Depth Flag]
     */
    protected final void initApply(AuxConfig... framebufferName) {
        if (this.shaderGroup != null) return;
        if (System.currentTimeMillis() - pastTime[0] > 1000) {
            pastTime[0] = System.currentTimeMillis();
        } else return;
        try {
            this.shaderGroup = new ShaderCoreGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), this.getShaderLocation());
            this.onPreInitApply();
            this.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            this.framebuffers.clear();
            for (AuxConfig buffer : framebufferName) {
                this.framebuffers.put(buffer.name, new AuxTarget(shaderGroup.getTempTarget(buffer.name), buffer.copyDepth, buffer.preserveHistory));
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Failed to load shader: {}", this.getShaderLocation(), e);
            this.shaderGroup = null;
            this.framebuffers.clear();
        }
        this.onPostInitApply();
    }

    /**
     * Starts the shader application cycle (typically called at the beginning of a Render Event).
     * This method clears old frame data from auxiliary buffers and copies depth data (distance values)
     * from the main screen if requested, prepping them to capture new graphics.
     */
    public final void applyShader() {
        if (!this.isActive() || this.framebuffers.isEmpty()) return;
        for (AuxTarget auxTarget : this.framebuffers.values()) {
            if (!auxTarget.preserveHistory) {
                auxTarget.framebuffer.clear(Minecraft.ON_OSX);
            }

            if (auxTarget.copyDepth) {
                auxTarget.framebuffer.copyDepthFrom(mainTarget);
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
    public final void endShader(@Nullable Consumer<GraphicsCoreInstance> uniform) {
        if (!this.isActive()) return;
        Consumer<GraphicsCoreInstance> consumer = (shader) -> {
            this.onApplyCustomUniform(shader);
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
    protected void onApplyCustomUniform(GraphicsCoreInstance shader) {}

    /**
     * Redirects the render engine output. Any graphics drawn immediately after this call
     * will be captured and written directly into the specified auxiliary Framebuffer instead of the main screen.
     *
     * @param bufferName The target identifier string of the auxiliary Framebuffer.
     */
    public final void writeFramebuffer(String bufferName) {
        AuxTarget first = this.framebuffers.get(bufferName);
        if (first == null) return;
        Framebuffer framebuffer = first.framebuffer;
        if (framebuffer == null) return;
        framebuffer.bindWrite(false);
    }

    /**
     * Resizes the entire shader group and its associated framebuffers
     * according to the new window dimensions.
     *
     * @param width  The new width of the window in pixels.
     * @param height The new height of the window in pixels.
     */
    public void resize(int width, int height) {
        this.shaderGroup.resize(width, height);
    }

    /**
     * Automatically scales and adjusts the dimensions of all auxiliary Framebuffers
     * whenever the player resizes the game window, switches to fullscreen (F11), or modifies GUI scaling.
     * This prevents stretching, distortion, or pixel artifacting.
     */
    public final void autoResize() {
        if (!this.isActive() || this.framebuffers.isEmpty()) return;
        int currentWindowWidth = mc.getWindow().getWidth();
        int currentWindowHeight = mc.getWindow().getHeight();
        if (currentWindowWidth != lastCheckedWidth || currentWindowHeight != lastCheckedHeight) {
            lastCheckedWidth = currentWindowWidth;
            lastCheckedHeight = currentWindowHeight;
            pastTime[1]  = System.currentTimeMillis();
            pendingResize = true;
            return;
        }
        if (pendingResize && System.currentTimeMillis() - pastTime[1] > 200) {
            pastTime[1]  = System.currentTimeMillis();
            pendingResize = false;
            this.resize(currentWindowWidth, currentWindowHeight);
        }
    }

    /**
     * Retrieves the auxiliary Framebuffer associated with the specified buffer name
     * from the underlying shader group.
     *
     * @param bufferName The unique identifier/name of the target framebuffer.
     * @return The corresponding {@link Framebuffer} instance, or null if the target does not exist.
     */
    public Framebuffer getFramebuffer(String bufferName) {
        return this.shaderGroup.getTempTarget(bufferName);
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
        this.init();
    }
}
