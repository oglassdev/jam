#version 330 core

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform float Threshold;
uniform float Weight;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

float near = 5;
float far = 10000.0;
float LinearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (near * far) / (far + near - z * (far - near));
}

vec3 edgeDetection(vec2 uv) {
    float depth = LinearizeDepth(texture(DiffuseDepthSampler, uv).r);

    float dx[9] = float[9](
    1.0, 0.0, -1.0,
    2.0, 0.0, -2.0,
    1.0, 0.0, -1.0
    );

    float dy[9] = float[9](
    1.0, 2.0, 1.0,
    0.0, 0.0, 0.0,
    -1.0, -2.0, -1.0
    );

    float edgeX = 0.0;
    float edgeY = 0.0;

    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            float sampleDepth = LinearizeDepth(texture(DiffuseDepthSampler, uv + vec2(i, j) / InSize).r);

            edgeX += sampleDepth * dx[(i + 1) * 3 + (j + 1)];
            edgeY += sampleDepth * dy[(i + 1) * 3 + (j + 1)];
        }
    }

    float edge = length(vec2(edgeX, edgeY));

    return vec3(1.0 - smoothstep(Threshold, Threshold + (3 * Threshold), edge));
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    fragColor = vec4(color.rgb * edgeDetection(texCoord), color.a);
}
