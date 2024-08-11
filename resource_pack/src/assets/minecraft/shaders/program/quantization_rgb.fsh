#version 330 core

uniform sampler2D DiffuseSampler;
uniform float Gamma;
uniform int Size;

in vec2 texCoord;

out vec4 fragColor;

uniform vec4 colorPalette[24] = vec4[24](
    vec4(0.76, 0.61, 0.76, 1.0), // #c09dc3
    vec4(0.92, 0.69, 0.74, 1.0), // #ebaebc
    vec4(1.0, 0.79, 0.73, 1.0),  // #ffcabb
    vec4(1.0, 0.89, 0.73, 1.0),  // #ffe5ba
    vec4(0.78, 0.86, 0.74, 1.0), // #c8dcbe
    vec4(0.63, 0.79, 0.76, 1.0), // #a0cac2
    vec4(0.53, 0.69, 0.73, 1.0), // #87afbb
    vec4(0.61, 0.80, 0.85, 1.0), // #9dccd7
    vec4(0.76, 0.87, 0.92, 1.0), // #c3deeb
    vec4(0.65, 0.78, 0.91, 1.0), // #a6c7e7
    vec4(0.59, 0.67, 0.83, 1.0), // #97abd4
    vec4(0.55, 0.56, 0.69, 1.0), // #8b8fb0
    vec4(0.71, 0.65, 0.83, 1.0), // #b5a5d3
    vec4(0.88, 0.71, 0.86, 1.0), // #e1b6db
    vec4(1.0, 0.79, 0.84, 1.0),  // #ffcbd7
    vec4(1.0, 0.91, 0.86, 1.0),  // #ffe8dc
    vec4(0.96, 0.82, 0.80, 1.0), // #f4d2cd
    vec4(0.88, 0.71, 0.74, 1.0), // #e2b7be
    vec4(0.76, 0.61, 0.69, 1.0), // #c19caf
    vec4(0.49, 0.45, 0.56, 1.0), // #7d7390
    vec4(0.67, 0.64, 0.73, 1.0), // #ada3b9
    vec4(0.80, 0.75, 0.81, 1.0), // #cbbfcf
    vec4(0.89, 0.85, 0.88, 1.0), // #e5d9e1
    vec4(1.0, 1.0, 0.94, 1.0)    // #fff7f0
);

vec4 gammaCorrect(vec4 color) {
    return pow(color, vec4(1.0 / Gamma));
}

vec4 inverseGammaCorrect(vec4 color) {
    return pow(color, vec4(Gamma));
}

vec4 findClosestColor(vec4 color) {
    vec4 closestColor = colorPalette[0];
    float minDistance = length(color - closestColor);

    for (int i = 1; i < Size; i++) { 
        float distance = length(color - colorPalette[i]);
        if (distance < minDistance) {
            minDistance = distance;
            closestColor = colorPalette[i];
        }
    }
    return closestColor;
}

void main() {
    vec4 texColor = texture(DiffuseSampler, texCoord);
    vec4 correctedColor = gammaCorrect(texColor);

    correctedColor = findClosestColor(correctedColor);

    fragColor = inverseGammaCorrect(correctedColor);
}
