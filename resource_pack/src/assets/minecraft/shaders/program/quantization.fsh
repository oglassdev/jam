#version 330 

uniform sampler2D DiffuseSampler;
uniform int Size;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 colour = texture(DiffuseSampler, texCoord);

    fragColor = floor(colour * (Size - 1) + 0.5) / (Size - 1);
}
