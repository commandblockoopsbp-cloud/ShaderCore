#ifndef NOISE_GEN_GLSL
#define NOISE_GEN_GLSL

uniform sampler2D WorleyNoise3DTexture;
uniform sampler2D WorleyNoise2DTexture;
uniform sampler2D PerlinNoise3DTexture;
uniform sampler2D PerlinNoise2DTexture;

uniform float BaseScale;
uniform float Channel;
uniform vec3 TexSize;
uniform float ZSize;
uniform vec2 Size3D;
uniform vec2 Size2D;
uniform vec2 GridMax;
uniform float SafePadding;

vec3 simple3D(sampler2D sampler, vec3 p) {
    vec2 safePad = TexSize.xy + 2.0 * SafePadding;
    vec3 pos = fract(p);
    vec2 ndcPos = (pos.xz * TexSize.xy + SafePadding) / safePad;

    float texYPos = pos.y * TexSize.z;
    float realYPos = texYPos / Channel;

    vec2 gridPos = floor(vec2(mod(realYPos, GridMax.x), realYPos / GridMax.x));

    vec2 texPos = (ndcPos + gridPos) / GridMax;
    vec4 color = texture2D(sampler, texPos);

    float testCase = mod(texYPos, Channel);
    float fY = fract(testCase);
    int index = int(floor(testCase));

    vec2 resXy = vec2(color[index], color[index+1]);

    return vec3(resXy, fY);
}

vec4 samplerPerlin2D(vec2 p) {
    vec2 uv = fract(p / BaseScale);
    return texture2D(PerlinNoise2DTexture, uv);
}

vec4 samplerVoronoi2D(vec2 p) {
    vec2 uv = fract(p / BaseScale);
    return 1.0 - texture2D(WorleyNoise2DTexture, uv);
}

float samplerVoronoi3D(vec3 p) {
    vec3 noiseTex = simple3D(WorleyNoise3DTexture, p);
    return 1.0 - mix(noiseTex.x, noiseTex.y, noiseTex.z);
}

float samplerPerlin3D(vec3 p) {
    vec3 noiseTex = simple3D(PerlinNoise3DTexture, p);
    return mix(noiseTex.x, noiseTex.y, noiseTex.z);
}

#endif