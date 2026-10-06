#ifndef NOISE_GLSL
#define NOISE_GLSL

vec3 hash33(vec3 p) {
    p = vec3(
            dot(p, vec3(127.1, 311.7, 742.9)),
            dot(p, vec3(269.5, 183.3, 246.1)),
            dot(p, vec3(113.5, 271.9, 124.6))
    );
    return fract(sin(p) * 43758.5453123);
}

vec3 hashHelpPerlin(vec3 offset, vec3 period, vec3 i) {
    return hash33(mod(i + offset, period));
}

float fastPerlin3D(vec3 p, float baseScale) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    vec3 period = vec3(baseScale);
    float g000 = dot(hashHelpPerlin(vec3(0.0, 0.0, 0.0), period, i) * 2.0 - 1.0, f - vec3(0.0, 0.0, 0.0));
    float g100 = dot(hashHelpPerlin(vec3(1.0, 0.0, 0.0), period, i) * 2.0 - 1.0, f - vec3(1.0, 0.0, 0.0));
    float g010 = dot(hashHelpPerlin(vec3(0.0, 1.0, 0.0), period, i) * 2.0 - 1.0, f - vec3(0.0, 1.0, 0.0));
    float g110 = dot(hashHelpPerlin(vec3(1.0, 1.0, 0.0), period, i) * 2.0 - 1.0, f - vec3(1.0, 1.0, 0.0));

    float g001 = dot(hashHelpPerlin(vec3(0.0, 0.0, 1.0), period, i) * 2.0 - 1.0, f - vec3(0.0, 0.0, 1.0));
    float g101 = dot(hashHelpPerlin(vec3(1.0, 0.0, 1.0), period, i) * 2.0 - 1.0, f - vec3(1.0, 0.0, 1.0));
    float g011 = dot(hashHelpPerlin(vec3(0.0, 1.0, 1.0), period, i) * 2.0 - 1.0, f - vec3(0.0, 1.0, 1.0));
    float g111 = dot(hashHelpPerlin(vec3(1.0, 1.0, 1.0), period, i) * 2.0 - 1.0, f - vec3(1.0, 1.0, 1.0));

    float x00 = mix(g000, g100, u.x);
    float x10 = mix(g010, g110, u.x);
    float x01 = mix(g001, g101, u.x);
    float x11 = mix(g011, g111, u.x);

    float y0 = mix(x00, x10, u.y);
    float y1 = mix(x01, x11, u.y);

    return mix(y0, y1, u.z) * 0.5 + 0.5;
}

float fastVoronoi3D(vec3 p, float baseScale) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    float minDist = 1.0;
    vec3 period = vec3(baseScale);

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 neighbor = vec3(float(x), float(y), float(z));

                vec3 wrappedCell = mod(i + neighbor, period);
                vec3 point = hash33(wrappedCell);

                vec3 diff = neighbor + point - f;
                float dist = length(diff);
                minDist = min(minDist, dist);
            }
        }
    }
    return minDist;
}

#endif