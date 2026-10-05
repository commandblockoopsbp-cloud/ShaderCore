#version 120

attribute vec4 Position;
uniform vec2 OutSize;

varying vec2 texCoord;

void main() {
    texCoord = Position.xy / OutSize;
    vec2 ndcPos = texCoord * 2.0 - 1.0;

    gl_Position = vec4(ndcPos, 0.2, 1.0);
}