#ifndef CONVERT_GLSL
#define CONVERT_GLSL

//convert
vec3 projectAndDivide(mat4 projectionMatrix, vec3 position) {
    vec4 homogenegousPos = projectionMatrix * vec4(position, 1.0);
    return homogenegousPos.xyz / homogenegousPos.w;
}

vec3 screenToView(vec3 screenPos, mat4 projectionInverse) {
    vec3 ndcPos = screenPos * 2.0 - 1.0;
    return projectAndDivide(projectionInverse, ndcPos);
}

vec2 viewToScreen(vec3 viewPos, mat4 projection) {
    vec3 ndcPos = projectAndDivide(projection, viewPos);
    return ndcPos.xy * 0.5 + 0.5;
}

vec3 screenToFeet(vec3 screenPos, mat4 projectionInverse, mat4 modelViewInverse) {
    vec3 viewPos = screenToView(screenPos, projectionInverse);
    return (modelViewInverse * vec4(viewPos, 1.0)).xyz;
}

vec2 feetToScreen(vec3 feetPos, mat4 projection, mat4 modelView) {
    vec3 viewPos = (modelView * vec4(feetPos, 1.0)).xyz;
    return viewToScreen(viewPos, projection);
}

vec3 screenToWorld(vec3 screenPos, mat4 projectionInverse, mat4 modelViewInverse, vec3 camPos) {
    vec3 feetPos = screenToFeet(screenPos, projectionInverse, modelViewInverse);
    return feetPos + camPos;
}

vec2 worldToScreen(vec3 worldPos, mat4 projection, mat4 modelView, vec3 camPos) {
    vec3 feetPos = worldPos - camPos;
    return feetToScreen(feetPos, projection, modelView);
}

#endif