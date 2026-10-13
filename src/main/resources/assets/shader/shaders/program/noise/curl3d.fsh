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

float perlinScalarField(vec3 coord, float baseScale, vec3 seedOffset) {
    return perlinNoise3D(coord + seedOffset, baseScale);
}

float curlNoise3D_directional(vec3 coord, float baseScale) {
    float e = 0.001;

    vec3 seedX = vec3(12.34, 1.23, 5.67);
    vec3 seedY = vec3(7.89, 23.45, 3.45);
    vec3 seedZ = vec3(9.10, 11.12, 13.14);

    float n1 = perlinScalarField(coord + vec3(0.0, e, 0.0), baseScale, seedZ);
    float n2 = perlinScalarField(coord - vec3(0.0, e, 0.0), baseScale, seedZ);
    float aX = (n1 - n2) / (2.0 * e);

    n1 = perlinScalarField(coord + vec3(0.0, 0.0, e), baseScale, seedY);
    n2 = perlinScalarField(coord - vec3(0.0, 0.0, e), baseScale, seedY);
    float aY = (n1 - n2) / (2.0 * e);

    n1 = perlinScalarField(coord + vec3(0.0, 0.0, e), baseScale, seedX);
    n2 = perlinScalarField(coord - vec3(0.0, 0.0, e), baseScale, seedX);
    float bX = (n1 - n2) / (2.0 * e);

    n1 = perlinScalarField(coord + vec3(e, 0.0, 0.0), baseScale, seedZ);
    n2 = perlinScalarField(coord - vec3(e, 0.0, 0.0), baseScale, seedZ);
    float bY = (n1 - n2) / (2.0 * e);

    n1 = perlinScalarField(coord + vec3(e, 0.0, 0.0), baseScale, seedY);
    n2 = perlinScalarField(coord - vec3(e, 0.0, 0.0), baseScale, seedY);
    float cX = (n1 - n2) / (2.0 * e);

    n1 = perlinScalarField(coord + vec3(0.0, e, 0.0), baseScale, seedX);
    n2 = perlinScalarField(coord - vec3(0.0, e, 0.0), baseScale, seedX);
    float cY = (n1 - n2) / (2.0 * e);

    vec3 curlVector = vec3(
            aX - aY,
            bX - bY,
            cX - cY
    );

    float scalarCurl = dot(curlVector, vec3(0.577, 0.577, 0.577));

    return clamp(scalarCurl * 0.5 + 0.5, 0.0, 1.0);
}

void main() {
    vec3 pos = convert3D(texCoord);

    float r = curlNoise3D_directional(pos, BaseScale);
    float g = curlNoise3D_directional(pos * 2.0, BaseScale * 2.0);
    float b = curlNoise3D_directional(pos * 4.0, BaseScale * 4.0);
    float a = curlNoise3D_directional(pos + vec3(12.34, 56.78, 90.12), BaseScale);

    gl_FragColor = vec4(r, g, b, a);
}