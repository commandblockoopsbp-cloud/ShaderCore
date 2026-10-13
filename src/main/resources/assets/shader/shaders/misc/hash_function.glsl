#ifndef HASH_GLSL
#define HASH_GLSL

vec3 hash33(vec3 p) {
    p = vec3(
            dot(p, vec3(127.1, 311.7, 742.9)),
            dot(p, vec3(269.5, 183.3, 246.1)),
            dot(p, vec3(113.5, 271.9, 124.6))
    );
    return fract(sin(p) * 43758.5453123);
}

vec3 hash33_alt(vec3 p) {
    p = vec3(
            dot(p, vec3(419.2, 371.6, 111.3)),
            dot(p, vec3(105.7, 483.2, 297.8)),
            dot(p, vec3(273.1, 191.5, 439.4))
    );
    return fract(sin(p) * 23758.83179);
}

float hash31(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

vec3 hash23(vec2 p) {
    p = vec2(
            dot(p, vec2(127.1, 311.7)),
            dot(p, vec2(269.5, 183.3))
    );
    return fract(sin(p.xyx) * 43758.5453123);
}

#endif