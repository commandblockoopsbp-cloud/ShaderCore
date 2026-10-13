#ifndef NOISE_GEN_GLSL
#define NOISE_GEN_GLSL

uniform float BaseScale;
uniform vec2 TexSize;
uniform vec2 NoiseSize;
uniform vec2 GridMax;
uniform float SafePadding;

#define TEX_Z (GridMax.x * GridMax.y)

vec3 convert3D(vec2 texCoord) {
    vec2 grid = texCoord * GridMax;
    vec2 cell = floor(grid);

    vec2 uv = (fract(grid) - 0.5) * ((TexSize.xy + 2.0 * SafePadding) / TexSize.xy) + 0.5;

    float depthBase = cell.x + GridMax.x * cell.y;

    return fract(vec3(uv, depthBase / TEX_Z) + 1.0) * BaseScale;
}

vec2 yToTex(vec2 ndcPos, float texYPos) {
    texYPos = mod(texYPos, TEX_Z);
    vec2 gridPos = floor(vec2(mod(texYPos, GridMax.x), texYPos / GridMax.x));
    return (ndcPos + gridPos) / GridMax;
}

vec4 simple3D(sampler2D sampler, vec3 p) {
    vec2 safePad = TexSize.xy + 2.0 * SafePadding;
    vec3 pos = fract(p / BaseScale);
    pos = vec3(pos.x, pos.z, pos.y);
    vec2 ndcPos = (pos.xy * TexSize.xy + SafePadding) / safePad;

    float texYPos = pos.z * TEX_Z;

    vec2 texPos = yToTex(ndcPos, texYPos);
    vec2 nextTexPos = yToTex(ndcPos, texYPos + 1.0);

    vec4 color = texture2D(sampler, texPos);
    vec4 nextColor = texture2D(sampler, nextTexPos);

    return mix(color, nextColor, fract(texYPos));
}

#endif