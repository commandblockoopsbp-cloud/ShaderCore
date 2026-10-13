#version 120

#include "shader:shaders/misc/hash_function.glsl"

uniform float BaseScale;

varying vec2 texCoord;

vec2 sampleGradientVector2D(vec2 cellOffset, vec2 period, vec2 gridIndex) {
    vec2 localCoord = mod(gridIndex + cellOffset, period);
    vec3 h = hash23(localCoord);
    return h.xy * 2.0 - 1.0;
}

float perlinNoise2D(vec2 coord, float baseScale) {
    vec2 i = floor(coord);
    vec2 f = fract(coord);

    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    vec2 period = vec2(baseScale);

    float n00 = dot(sampleGradientVector2D(vec2(0.0, 0.0), period, i), f - vec2(0.0, 0.0));
    float n10 = dot(sampleGradientVector2D(vec2(1.0, 0.0), period, i), f - vec2(1.0, 0.0));
    float n01 = dot(sampleGradientVector2D(vec2(0.0, 1.0), period, i), f - vec2(0.0, 1.0));
    float n11 = dot(sampleGradientVector2D(vec2(1.0, 1.0), period, i), f - vec2(1.0, 1.0));

    float nx0 = mix(n00, n10, u.x);
    float nx1 = mix(n01, n11, u.x);
    float noiseValue = mix(nx0, nx1, u.y);

    return clamp(noiseValue * 1.414 * 0.5 + 0.5, 0.0, 1.0);
}

void main() {
    vec2 pos = texCoord * BaseScale;

    float r = perlinNoise2D(pos, BaseScale);
    float g = perlinNoise2D(pos * 2.0, BaseScale * 2.0);
    float b = perlinNoise2D(pos * 4.0, BaseScale * 4.0);
    float a = perlinNoise2D(pos + vec2(12.34, 56.78), BaseScale);

    gl_FragColor = vec4(r, g, b, a);
}