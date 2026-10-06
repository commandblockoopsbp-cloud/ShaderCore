#version 120

#include "shader:shaders/misc/noise.glsl"

uniform float BaseScale;

varying vec2 texCoord;

void main() {
    vec2 texScaled = texCoord * BaseScale;
    float perlin0 = fastPerlin3D(vec3(texScaled, 1.0), BaseScale);
    float perlin1 = fastPerlin3D(2.0 * vec3(texScaled, 1.0), BaseScale);
    float perlin2 = fastPerlin3D(3.0 * vec3(texScaled, 1.0), BaseScale);
    float perlin3 = fastPerlin3D(4.0 * vec3(texScaled, 1.0), BaseScale);
    gl_FragColor = vec4(perlin0, perlin1, perlin2, perlin3);
}