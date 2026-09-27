#ifndef AABB_GLSL
#define AABB_GLSL

//ray_tracing
vec2 rayBox(vec3 centerPos, vec3 b, vec3 rayOri, vec3 rayDir) {
    vec3 t0 = (centerPos - b - rayOri) / rayDir;
    vec3 t1 = (centerPos + b - rayOri) / rayDir;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);

    float dstA = max(max(tmin.x, tmin.y), tmin.z);
    float dstB = min(min(tmax.x, tmax.y), tmax.z);

    float dstToBox = max(0.0, dstA);
    float dstInsideBox = max(0.0, dstB - dstToBox);
    return vec2(dstToBox, dstInsideBox);
}

#endif
