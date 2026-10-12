#version 120

#include "shader:shaders/misc/noise.glsl"

uniform float BaseScale;
uniform float Channel;
uniform vec3 TexSize;
uniform vec2 Size3D;
uniform vec2 GridMax;
uniform float SafePadding;

varying vec2 texCoord;

void main() {
    vec2 grid = texCoord * GridMax;
    vec2 cell = floor(grid);

    vec2 uv = clamp((fract(grid) - 0.5) * ((TexSize.xy + 2.0 * SafePadding) / TexSize.xy), -0.5, 0.5) + 0.5;

    float depthBase = Channel * (cell.x + GridMax.x * cell.y);

    vec4 result;
    for (int i = 0; i < 4; i++) {
        vec3 pos = fract(vec3(uv.x, (depthBase + float(i)) / TexSize.z, uv.y)) * BaseScale;
        result[i] = fastPerlin3D(pos, BaseScale);
    }

    gl_FragColor = result;
}