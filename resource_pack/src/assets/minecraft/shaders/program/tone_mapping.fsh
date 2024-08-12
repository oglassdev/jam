#version 330

uniform sampler2D DiffuseSampler;
uniform float Exposure;
uniform float Gamma;

in vec2 texCoord;

out vec4 fragColor;

vec3 tone_mapping(vec3 color) {
    return color * Exposure;
}

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;

    color = tone_mapping(color);

    color = pow(color, vec3(1.0 / Gamma));


    fragColor = vec4(color, 1.0);
}
