#version 330 core

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform float Threshold;
uniform float Weight;

in vec2 texCoord;

out vec4 fragColor;

vec3 edgeDetection(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;

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
            float sampleDepth = texture(DepthSampler, uv + vec2(i, j) / vec2(textureSize(DepthSampler, 0))).r;

            edgeX += sampleDepth * dx[(i + 1) * 3 + (j + 1)];
            edgeY += sampleDepth * dy[(i + 1) * 3 + (j + 1)];
        }
    }

    float edge = length(vec2(edgeX, edgeY));

    return vec3(1.0 - smoothstep(Threshold, Threshold + (3 * Threshold), edge));
}

void main() {
    vec4 texColor = texture(DiffuseSampler, texCoord);
    vec3 edgeColor = edgeDetection(texCoord);

    fragColor = vec4(texColor.rgb * edgeColor, texColor.a);
}
