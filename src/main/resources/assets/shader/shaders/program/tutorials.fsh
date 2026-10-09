#version 120

#include "shader:shaders/misc/noise_with_sampler.glsl"

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec3 mainColor = texture2D(PerlinNoise3DTexture, texCoord).rrr;

    vec3 finalColor = vec3(mainColor);

    gl_FragColor = vec4(finalColor, 1.0);
}