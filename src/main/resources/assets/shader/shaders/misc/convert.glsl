#ifndef CONVERT_GLSL
#define CONVERT_GLSL

uniform mat4 ModelViewMatrix;
uniform mat4 InverseModelViewMatrix;
uniform mat4 ProjectionMatrix;
uniform mat4 InverseProjectionMatrix;
uniform vec3 CameraPos;

//convert
vec3 projectAndDivide(mat4 projectionMatrix, vec3 position) {
    vec4 homogenegousPos = projectionMatrix * vec4(position, 1.0);
    return homogenegousPos.xyz / homogenegousPos.w;
}

vec3 screenToView(vec3 screenPos) {
    vec3 ndcPos = screenPos * 2.0 - 1.0;
    return projectAndDivide(InverseProjectionMatrix, ndcPos);
}

vec2 viewToScreen(vec3 viewPos) {
    vec3 ndcPos = projectAndDivide(ProjectionMatrix, viewPos);
    return ndcPos.xy * 0.5 + 0.5;
}

vec3 screenToFeet(vec3 screenPos) {
    vec3 viewPos = screenToView(screenPos, InverseProjectionMatrix);
    return (InverseModelViewMatrix * vec4(viewPos, 1.0)).xyz;
}

vec2 feetToScreen(vec3 feetPos) {
    vec3 viewPos = (ModelViewMatrix * vec4(feetPos, 1.0)).xyz;
    return viewToScreen(viewPos, ProjectionMatrix);
}

vec3 screenToWorld(vec3 screenPos) {
    vec3 feetPos = screenToFeet(screenPos, InverseProjectionMatrix, InverseModelViewMatrix);
    return feetPos + CameraPos;
}

vec2 worldToScreen(vec3 worldPos) {
    vec3 feetPos = worldPos - CameraPos;
    return feetToScreen(feetPos, ProjectionMatrix, ModelViewMatrix);
}

#endif