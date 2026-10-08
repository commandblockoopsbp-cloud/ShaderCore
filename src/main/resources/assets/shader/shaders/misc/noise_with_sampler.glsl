#ifndef NOISE_GEN_GLSL
#define NOISE_GEN_GLSL

uniform sampler2D WorleyNoise3DTexture;
uniform sampler2D WorleyNoise2DTexture;
uniform sampler2D PerlinNoise3DTexture;
uniform sampler2D PerlinNoise2DTexture;

uniform float BaseScale;
uniform float Channel;
uniform vec2 TexSize;
uniform float ZSize;
uniform vec2 Size3D;
uniform vec2 Size2D;

vec4 sampleTileHardware(sampler2D sampler, vec3 pos, vec2 tilePos) {
    vec2 tilePixel = pos.xz * TexSize;
    vec2 clampedPixel = clamp(tilePixel, vec2(0.5), TexSize - vec2(0.5));
    vec2 uv = (clampedPixel + tilePos * TexSize) / Size3D;
    return texture2D(sampler, uv);
}

vec4 simpleNoise3D(sampler2D sampler, vec3 p, float offsetY, out float y) {
    vec3 pos = fract(p / BaseScale);
    y = pos.y * ZSize / Channel + offsetY;
    float tileIdx = floor(y);

    vec2 gridTiles = Size3D / TexSize;

    vec2 tilePos = vec2(mod(tileIdx, gridTiles.x), floor(tileIdx / gridTiles.x));

    return sampleTileHardware(sampler, pos, tilePos);
}

vec2 layerNoise3D(sampler2D sampler, vec3 p, out float f) {
    float y1 = 0.0;
    vec4 noiseTex1 = simpleNoise3D(sampler, p, 0.0, y1);

    float channelVal = y1 * Channel;
    int testCase = int(mod(floor(channelVal), Channel));
    f = fract(channelVal);

    if (testCase == 0) return noiseTex1.xy;
    if (testCase == 1) return noiseTex1.yz;
    if (testCase == 2) return noiseTex1.zw;

    float y2 = 0.0;
    vec4 noiseTex2 = simpleNoise3D(sampler, p, 1.0, y2);
    return vec2(noiseTex1.w, noiseTex2.x);
}

vec4 samplerPerlin2D(vec2 p) {
    vec2 uv = fract(p / BaseScale);
    return texture2D(PerlinNoise2DTexture, uv);
}

vec4 samplerVoronoi2D(vec2 p) {
    vec2 uv = fract(p / BaseScale);
    return texture2D(WorleyNoise2DTexture, uv);
}

float samplerVoronoi3D(vec3 p) {
    float f = 0.0;
    vec2 noiseTex = layerNoise3D(WorleyNoise3DTexture, p, f);
    return 1.0 - mix(noiseTex.x, noiseTex.y, f);
}

float samplerPerlin3D(vec3 p) {
    float f = 0.0;
    vec2 noiseTex = layerNoise3D(PerlinNoise3DTexture, p, f);
    return mix(noiseTex.x, noiseTex.y, f);
}

#endif