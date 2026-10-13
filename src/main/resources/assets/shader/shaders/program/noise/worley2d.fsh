#version 120

#include "shader:shaders/misc/hash_function.glsl"

uniform float BaseScale;

varying vec2 texCoord;

vec2 sampleVoronoiFeaturePoint2D(vec2 neighborOffset, vec2 period, vec2 gridIndex) {
    vec2 wrappedCell = mod(gridIndex + neighborOffset, period);
    return hash23(wrappedCell).xy;
}

float voronoiNoise2D(vec2 coord, float baseScale) {
    vec2 gridIndex = floor(coord);
    vec2 localPos  = fract(coord);
    vec2 period = vec2(baseScale);

    float minimumDistance = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighborOffset = vec2(float(x), float(y));

            vec2 featurePoint = sampleVoronoiFeaturePoint2D(neighborOffset, period, gridIndex);
            vec2 distanceVector = neighborOffset + featurePoint - localPos;

            float currentDistance = length(distanceVector);
            minimumDistance = min(minimumDistance, currentDistance);
        }
    }

    return clamp(minimumDistance, 0.0, 1.0);
}

void main() {
    vec2 pos = texCoord * BaseScale;

    float r = voronoiNoise2D(pos, BaseScale);
    float g = voronoiNoise2D(pos * 2.0, BaseScale * 2.0);
    float b = voronoiNoise2D(pos * 0.5, BaseScale * 0.5);
    float a = voronoiNoise2D(pos + vec2(12.34, 56.78), BaseScale);

    gl_FragColor = vec4(r, g, b, a);
}