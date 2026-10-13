#version 120

#include "shader:shaders/misc/hash_function.glsl"
#include "shader:shaders/misc/noise_for_3d.glsl"

varying vec2 texCoord;

vec3 sampleGradientVector(vec3 cellOffset, vec3 period, vec3 gridIndex) {
    vec3 localCoord = mod(gridIndex + cellOffset, period);
    vec3 randomHash = hash33(localCoord);
    return randomHash * 2.0 - 1.0;
}

float perlinNoise3D(vec3 coord, float baseScale) {
    vec3 i = floor(coord);
    vec3 f = fract(coord);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    vec3 period = vec3(baseScale);

    float lowZ  = mix(mix(dot(sampleGradientVector(vec3(0.0, 0.0, 0.0), period, i), f - vec3(0.0, 0.0, 0.0)),
            dot(sampleGradientVector(vec3(1.0, 0.0, 0.0), period, i), f - vec3(1.0, 0.0, 0.0)), u.x),
            mix(dot(sampleGradientVector(vec3(0.0, 1.0, 0.0), period, i), f - vec3(0.0, 1.0, 0.0)),
                    dot(sampleGradientVector(vec3(1.0, 1.0, 0.0), period, i), f - vec3(1.0, 1.0, 0.0)), u.x), u.y);

    float highZ = mix(mix(dot(sampleGradientVector(vec3(0.0, 0.0, 1.0), period, i), f - vec3(0.0, 0.0, 1.0)),
            dot(sampleGradientVector(vec3(1.0, 0.0, 1.0), period, i), f - vec3(1.0, 0.0, 1.0)), u.x),
            mix(dot(sampleGradientVector(vec3(0.0, 1.0, 1.0), period, i), f - vec3(0.0, 1.0, 1.0)),
                    dot(sampleGradientVector(vec3(1.0, 1.0, 1.0), period, i), f - vec3(1.0, 1.0, 1.0)), u.x), u.y);

    return mix(lowZ, highZ, u.z) * 0.5 + 0.5;
}

void main() {
    vec3 pos = convert3D(texCoord);
    float r = perlinNoise3D(pos, BaseScale);
    float g = perlinNoise3D(pos * 2.0, BaseScale * 2.0);
    float b = perlinNoise3D(pos * 4.0, BaseScale * 4.0);
    float a = perlinNoise3D(pos + vec3(12.34, 56.78, 90.12), BaseScale);
    gl_FragColor = vec4(r, g, b, a);
}