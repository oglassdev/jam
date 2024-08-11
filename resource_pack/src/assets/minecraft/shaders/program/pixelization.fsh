#version 330 core

uniform sampler2D DiffuseSampler;
uniform float Quality;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float blockSize = Quality;

    vec2 pixelatedCoord = (floor(texCoord / blockSize) + 0.5) * blockSize;

    fragColor = texture(DiffuseSampler, pixelatedCoord);
}
