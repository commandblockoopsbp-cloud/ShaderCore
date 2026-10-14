#ifndef NOISE_GEN_GLSL
#define NOISE_GEN_GLSL

vec3 convert3D(mat4 info, vec2 texCoord) {
    vec2 gridMax     = info[0].xy;
    vec2 texSize     = info[1].xy;
    float safePadding = info[2].x;
    float texZ       = info[2].y;
    float baseScale  = info[3].x;

    vec2 grid = texCoord * gridMax;
    vec2 cell = floor(grid);

    vec2 uv = (fract(grid) - 0.5) * ((texSize + 2.0 * safePadding) / texSize) + 0.5;

    float depthBase = cell.x + gridMax.x * cell.y;

    return fract(vec3(uv, depthBase / texZ) + 1.0) * baseScale;
}

vec2 yToTex(mat4 info, vec2 ndcPos, float texYPos) {
    vec2 gridMax = info[0].xy;
    float texZ   = info[2].y;

    texYPos = mod(texYPos, texZ);
    vec2 gridPos = floor(vec2(mod(texYPos, gridMax.x), texYPos / gridMax.x));
    return (ndcPos + gridPos) / gridMax;
}

vec4 simple3D(mat4 info, sampler2D sampler, vec3 p) {
    vec2 texSize     = info[1].xy;
    float safePadding = info[2].x;
    float texZ       = info[2].y;
    float baseScale  = info[3].x;

    vec2 safePad = texSize + 2.0 * safePadding;
    vec3 pos = fract(p / baseScale);
    pos = vec3(pos.x, pos.z, pos.y);
    vec2 ndcPos = (pos.xy * texSize + safePadding) / safePad;

    float texYPos = pos.z * texZ;

    vec2 texPos = yToTex(info, ndcPos, texYPos);
    vec2 nextTexPos = yToTex(info, ndcPos, texYPos + 1.0);

    vec4 color = texture2D(sampler, texPos);
    vec4 nextColor = texture2D(sampler, nextTexPos);

    return mix(color, nextColor, fract(texYPos));
}

#endif