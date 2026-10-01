#ifndef UPSCALE_GLSL
#define UPSCALE_GLSL

vec4 textureUpscale(sampler2D texSampler, vec2 coord, vec2 texelSize, int radius) {
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    for (int i = -radius; i <= radius; i++) {
        for (int j = -radius; j <= radius; j++) {
            vec2 offset = vec2(float(i), float(j)) * texelSize;
            vec2 offsetCoord = coord + offset;
            if (clamp(offsetCoord, 0.0, 1.0) != offsetCoord) continue;
            color += texture2D(texSampler, offsetCoord);
            totalWeight += 1.0;
        }
    }

    return color / totalWeight;
}

#endif