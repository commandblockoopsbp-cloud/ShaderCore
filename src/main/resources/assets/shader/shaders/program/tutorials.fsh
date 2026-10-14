#version 120

#include "shader:shaders/misc/noise_for_3d.glsl"

uniform sampler2D DiffuseSampler;

uniform sampler2D Worley3DTexture;
uniform mat4 Worley3DTextureInformation;
uniform sampler2D Perlin3DTexture;
uniform mat4 Perlin3DTextureInformation;

uniform sampler2D Perlin2DTexture;
uniform sampler2D Worley2DTexture;

uniform float Time;
uniform vec2 OutSize;

varying vec2 texCoord;

void main() {
    vec4 noise = simple3D(Perlin3DTextureInformation, Perlin3DTexture, vec3(texCoord.x, Time / 60.0, texCoord.y) * 16.0 * 2.0);
    noise = texture2D(Worley2DTexture, texCoord + Time / 10.0);
    float mainColor = (noise.r * 0.5) + (noise.g * 0.25) + (noise.b * 0.15) + (noise.a * 0.1);

    vec3 finalColor = vec3(mainColor);

    gl_FragColor = vec4(finalColor, 1.0);
}