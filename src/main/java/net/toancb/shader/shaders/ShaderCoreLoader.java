package net.toancb.shader.shaders;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ShaderCoreLoader {
    private static final Minecraft mc = Minecraft.getInstance();
    private final ShaderCoreLoader.ShaderCoreType type;
    private final String name;
    private final int id;
    private int references;

    private ShaderCoreLoader(ShaderCoreLoader.ShaderCoreType shaderType, int shaderId, String shaderName) {
        this.type = shaderType;
        this.id = shaderId;
        this.name = shaderName;
    }

    public void attachToEffect(IShaderCoreManager manager) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        ++this.references;
        GlStateManager.glAttachShader(manager.getId(), this.id);
    }

    public void close() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        --this.references;
        if (this.references <= 0) {
            GlStateManager.glDeleteShader(this.id);
            this.type.getPrograms().remove(this.name);
        }
    }

    public String getName() {
        return this.name;
    }

    public static ShaderCoreLoader compileShader(ShaderCoreLoader.ShaderCoreType shaderType, String shaderName, InputStream inputStream, String domain) throws IOException {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        String s = TextureUtil.readResourceAsString(inputStream);
        if (s == null) {
            throw new IOException("Could not load program " + shaderType.getName());
        } else {
            int i = GlStateManager.glCreateShader(shaderType.getGlType());
            GlStateManager.glShaderSource(i, processShaderIncludes(s));
            GlStateManager.glCompileShader(i);

            if (GlStateManager.glGetShaderi(i, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String s1 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
                throw new IOException("Couldn't compile " + shaderType.getName() + " program (" + domain + ", " + shaderName + ") : " + s1);
            } else {
                ShaderCoreLoader shaderloader = new ShaderCoreLoader(shaderType, i, shaderName);
                shaderType.getPrograms().put(shaderName, shaderloader);
                return shaderloader;
            }
        }
    }

    private static String processShaderIncludes(String shaderSource) throws IOException {
        int startIndex = shaderSource.indexOf("#include");
        while (startIndex != -1) {
            int midIndex = shaderSource.indexOf("\"", startIndex + 1);
            if (midIndex == -1) {
                throw new IOException("Malformed #include directive: missing opening quotation mark!");
            }
            int endIndex = shaderSource.indexOf("\"", midIndex + 1);
            if (endIndex == -1) {
                throw new IOException("Malformed #include directive: missing closing quotation mark!");
            }
            String includePath = shaderSource.substring(midIndex + 1, endIndex);

            ResourceLocation rl = ResourceLocation.tryParse(includePath);
            ResourceLocation resourcelocation = new ResourceLocation(rl.getNamespace(), rl.getPath());
            IResource iresource = mc.getResourceManager().getResource(resourcelocation);

            shaderSource = shaderSource.replaceFirst(shaderSource.substring(startIndex, endIndex + 1), TextureUtil.readResourceAsString(iresource.getInputStream()).trim());

            startIndex = shaderSource.indexOf("#include", startIndex + 1);
        }

        return shaderSource;
    }

    @OnlyIn(Dist.CLIENT)
    public enum ShaderCoreType {
        VERTEX("vertex", ".vsh", GL43.GL_VERTEX_SHADER),
        FRAGMENT("fragment", ".fsh", GL43.GL_FRAGMENT_SHADER);

        private final String name;
        private final String extension;
        private final int glType;
        private final Map<String, ShaderCoreLoader> programs = Maps.newHashMap();

        private ShaderCoreType(String name, String extension, int glType) {
            this.name = name;
            this.extension = extension;
            this.glType = glType;
        }

        public String getName() {
            return this.name;
        }

        public String getExtension() {
            return this.extension;
        }

        private int getGlType() {
            return this.glType;
        }

        public Map<String, ShaderCoreLoader> getPrograms() {
            return this.programs;
        }
    }
}
