#version 330 core

uniform sampler2D DiffuseSampler;
uniform int Size;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(DiffuseSampler, texCoord);

    fragColor = floor(texColor * Size) / Size;
}