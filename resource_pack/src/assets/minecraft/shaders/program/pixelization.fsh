#version 330 core

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D TranslucentDepthSampler;
uniform sampler2D ItemEntityDepthSampler;
uniform sampler2D ParticlesDepthSampler;
uniform sampler2D CloudsDepthSampler;
uniform sampler2D WeatherDepthSampler;

#define DIFFUSE
#define TRANSLUCENT
#define ITEM_ENTITY
#define PARTICLES
#define CLOUDS
#define WEATHER

precision highp float;

uniform float Threshold;
uniform float Size;
uniform int Weight;


in vec2 texCoord;
out vec4 fragColor;

#define KERNEL_SIZE 4

vec2 offsetsK4[4] = vec2[](
    vec2( 0.0,  1.0),
    vec2( 1.0,  0.0),
    vec2( 0.0, -1.0),
    vec2(-1.0,  0.0)
);

vec2 offsetsK8[8] = vec2[](
    vec2( 0.0,  1.0),
    vec2( 1.0,  1.0),
    vec2( 1.0,  0.0),
    vec2( 1.0, -1.0),
    vec2( 0.0, -1.0),
    vec2(-1.0, -1.0),
    vec2(-1.0,  0.0),
    vec2(-1.0,  1.0)
);

float sample(vec2 uv) {
    float depth = 1.0;

    #ifdef DIFFUSE
        depth = min(depth, texture(DiffuseDepthSampler,     uv).r);
    #endif
    #ifdef TRANSLUCENT
        depth = min(depth, texture(TranslucentDepthSampler, uv).r);
    #endif
    #ifdef ITEM_ENTITY
        depth = min(depth, texture(ItemEntityDepthSampler,  uv).r);
    #endif
    #ifdef PARTICLES
        depth = min(depth, texture(ParticlesDepthSampler,   uv).r);
    #endif
    #ifdef CLOUDS
        depth = min(depth, texture(CloudsDepthSampler,      uv).r);
    #endif
    #ifdef WEATHER
        depth = min(depth, texture(WeatherDepthSampler,     uv).r);
    #endif
    
    return depth;
}

void main() {
    vec2 viewport = textureSize(DiffuseDepthSampler, 0);

    float summation = 0.0;
    float count     = 0.0;

    for (int i = 1; i <= Weight; ++i) {
        for (int j = 0; j < KERNEL_SIZE; ++j) {
            vec2 offset;

            if (KERNEL_SIZE == 4) {
                offset = (offsetsK4[j] * i) / viewport;
            } else {
                offset = (offsetsK8[j] * i) / viewport;
            }

            float depth = sample(texCoord + offset);

            summation += depth;
            count     += 1.0;
        }
    }

    float average  = summation / count;
    float fragment = sample(texCoord);

    bool isFlat    = abs(fragment - average) < (Threshold * float(Weight));
    fragColor      = isFlat ? texture(DiffuseSampler, texCoord) : vec4(0);
}