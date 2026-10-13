#version 120

#include "shader:shaders/misc/hash_function.glsl"
#include "shader:shaders/misc/noise_for_3d.glsl"

varying vec2 texCoord;

vec3 sampleVoronoiFeaturePoint(vec3 neighborOffset, vec3 period, vec3 gridIndex) {
    vec3 wrappedCell = mod(gridIndex + neighborOffset, period);
    return hash33(wrappedCell);
}

float voronoiNoise3D(vec3 coord, float baseScale) {
    vec3 gridIndex = floor(coord);
    vec3 localPos  = fract(coord);
    vec3 period = vec3(baseScale);

    float minimumDistance = 1.0;

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 neighborOffset = vec3(float(x), float(y), float(z));

                vec3 featurePoint = sampleVoronoiFeaturePoint(neighborOffset, period, gridIndex);
                vec3 distanceVector = neighborOffset + featurePoint - localPos;

                float currentDistance = length(distanceVector);
                minimumDistance = min(minimumDistance, currentDistance);
            }
        }
    }

    return clamp(minimumDistance, 0.0, 1.0);
}

void main() {
    vec3 pos = convert3D(texCoord);
    float r = voronoiNoise3D(pos, BaseScale);
    float g = voronoiNoise3D(pos * 2.0, BaseScale * 2.0);
    float b = voronoiNoise3D(pos * 0.5, BaseScale * 0.5);
    float a = voronoiNoise3D(pos + vec3(12.34, 56.78, 90.12), BaseScale);
    gl_FragColor = vec4(r, g, b, a);
}