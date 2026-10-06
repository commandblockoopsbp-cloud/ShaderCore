#version 120

#include "shader:shaders/misc/noise.glsl"
uniform float BaseScale;
uniform float Channel;
uniform vec2 TexSize;
uniform float ZSize;
uniform vec2 Size3D;

varying vec2 texCoord;

void main() {
    vec2 texPos = texCoord * Size3D;
    vec2 uv = mod(texPos.xy, TexSize) / TexSize;

    float tileX = floor(texPos.x / TexSize.x);
    float tileY = floor(texPos.y / TexSize.y);
    float u_LayerZ = Channel * (tileX + tileY * Size3D.x / TexSize.x);

    vec3 p0 = vec3(uv.x, u_LayerZ / ZSize, uv.y) * BaseScale;
    vec3 p1 = vec3(uv.x, (u_LayerZ + 1.0) / ZSize, uv.y) * BaseScale;
    vec3 p2 = vec3(uv.x, (u_LayerZ + 2.0) / ZSize, uv.y) * BaseScale;
    vec3 p3 = vec3(uv.x, (u_LayerZ + 3.0) / ZSize, uv.y) * BaseScale;

    float worley0 = fastVoronoi3D(p0, BaseScale);
    float worley1 = fastVoronoi3D(p1, BaseScale);
    float worley2 = fastVoronoi3D(p2, BaseScale);
    float worley3 = fastVoronoi3D(p3, BaseScale);
    gl_FragColor = vec4(worley0, worley1, worley2, worley3);
}