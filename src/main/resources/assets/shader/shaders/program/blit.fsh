#version 110

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

varying vec2 texCoord;

void main(){
    gl_FragColor = texture2D(DiffuseSampler, texCoord);
    gl_FragDepth = texture2D(DiffuseDepthSampler, texCoord).r;
}
