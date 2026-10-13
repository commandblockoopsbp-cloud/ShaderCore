package net.toancb.shader.helper;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.toancb.shader.ShaderCoreMod;

@OnlyIn(Dist.CLIENT)
public class NoiseGenManager {
    public static final NoiseGen3DShader WORLEY3D = new NoiseGen3DShader("Worley3DTexture", "worley3d",
            new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise/worley3d.json"),
            16.0f, new Vector2i(2048, 1024), new Vector2i(128, 128));
    public static final NoiseGen3DShader PERLIN3D = new NoiseGen3DShader("Perlin3DTexture", "perlin3d",
            new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise/perlin3d.json"),
            16.0f, new Vector2i(2048, 1024), new Vector2i(128, 128));
    public static final NoiseGen3DShader CURL3D = new NoiseGen3DShader("Curl3DTexture", "curl3d",
            new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise/curl3d.json"),
            16.0f, new Vector2i(2048, 1024), new Vector2i(128, 128));

    public static final NoiseGen2DShader WORLEY2D = new NoiseGen2DShader("Worley2DTexture", "worley2d",
            new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise/worley2d.json"),
            16.0f, new Vector2i(512, 512));
    public static final NoiseGen2DShader PERLIN2D = new NoiseGen2DShader("Perlin2DTexture", "perlin2d",
            new ResourceLocation(ShaderCoreMod.MODID, "shaders/post/noise/perlin2d.json"),
            16.0f, new Vector2i(512, 512));

    private NoiseGenManager() {}
}
