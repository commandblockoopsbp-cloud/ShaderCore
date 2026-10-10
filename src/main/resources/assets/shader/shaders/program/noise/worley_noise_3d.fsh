#version 120

#include "shader:shaders/misc/noise.glsl"

uniform float BaseScale;
uniform float Channel;
uniform vec3 TexSize;
uniform vec2 Size3D;

varying vec2 texCoord;

void main() {
    vec2 texPos = texCoord * Size3D;
    vec2 gridPos = texPos / TexSize.xy;

    vec2 floorGrid = floor(gridPos);

    float depthPos = Channel * (floorGrid.x + (Size3D.x / TexSize.x) * floorGrid.y);

    vec2 maxPos = Size3D / TexSize.xy;
    float m = maxPos.x * maxPos.y;

    vec4 result;
    for (int i = 0; i < 4; i++) {
        vec3 pos = vec3(gridPos.x, (depthPos + 1.0 + float(i)) / TexSize.z, gridPos.y);
        pos = fract(pos) * BaseScale;
        result[i] = fastVoronoi3D(pos, BaseScale);
    }

    gl_FragColor = result;
}