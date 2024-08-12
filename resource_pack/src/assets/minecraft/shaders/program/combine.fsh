#version 330

uniform sampler2D DiffuseSampler;
uniform sampler2D SecondSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 diffuse_colour = texture(DiffuseSampler, texCoord);
    vec4 second_colour  = texture(SecondSampler,  texCoord);

    fragColor = diffuse_colour * second_colour;
}
