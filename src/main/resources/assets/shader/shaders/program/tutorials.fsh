#version 120

#include "shader:shaders/misc/noise_with_sampler.glsl"

uniform sampler2D DiffuseSampler;

uniform float Time;
uniform vec2 OutSize;

varying vec2 texCoord;

void main() {
    vec2 newTex = texCoord - 0.5;
    float mainColor = samplerVoronoi3D(vec3(newTex.x, Time / 60.0, texCoord.y) * 2.0);


    vec3 finalColor = vec3(mainColor);

    gl_FragColor = vec4(finalColor, 1.0);
}