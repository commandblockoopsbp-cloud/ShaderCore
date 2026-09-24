#version 110

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

void main() {
    vec3 mainColor = texture2D(DiffuseSampler, texCoord).rgb;
    float mainDepth = texture2D(DiffuseDepthSampler, texCoord).r;

    vec3 finalColor = vec3(mainDepth);

    gl_FragColor = vec4(finalColor, 1.0);
}