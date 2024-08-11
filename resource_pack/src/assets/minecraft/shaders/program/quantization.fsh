#version 330 core

uniform sampler2D DiffuseSampler;
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


vec3 rgbToOklab(vec3 rgb) {
    vec3 linRgb = pow(rgb, vec3(2.2));
    vec3 xyz = mat3(0.4124564, 0.3575761, 0.1804375,
                    0.2126729, 0.7151522, 0.0721750,
                    0.0193339, 0.1191920, 0.9503041) * linRgb;
    
    vec3 lab = xyz / vec3(0.95047, 1.0, 1.08883);
    vec3 lms = sqrt(lab);
    return vec3(lms.x, lms.y - 0.5 * (lms.z + lms.x), lms.z);
}

vec3 rgbToOkLab(vec3 rgb) {
    rgb = pow(rgb, vec3(2.2));
    vec3 xyz = vec3(
        rgb.r * 0.4124564 + rgb.g * 0.3575761 + rgb.b * 0.1804375,
        rgb.r * 0.2126729 + rgb.g * 0.7151522 + rgb.b * 0.0721750,
        rgb.r * 0.0193339 + rgb.g * 0.1191920 + rgb.b * 0.9503041
    );

    xyz /= vec3(0.95047, 1.0, 1.08883);

    vec3 lms = pow(xyz, vec3(1.0/3.0));
    return vec3(
        0.210454 * lms.x + 0.793617 * lms.y - 0.004072 * lms.z,
        1.977998 * lms.x - 0.500596 * lms.y - 0.031886 * lms.z,
        0.025300 * lms.x + 0.050175 * lms.y + 0.844295 * lms.z
    );
}

float deltaE2000(vec3 lab1, vec3 lab2) {
    float C1 = sqrt(lab1.y * lab1.y + lab1.z * lab1.z);
    float C2 = sqrt(lab2.y * lab2.y + lab2.z * lab2.z);
    float C = (C1 + C2) / 2.0;

    float G = 0.5 * (1.0 - sqrt(pow(C, 7.0) / (pow(C, 7.0) + pow(25.0, 7.0))));
    float a1p = lab1.y * (1.0 + G);
    float a2p = lab2.y * (1.0 + G);
    float C1p = sqrt(a1p * a1p + lab1.z * lab1.z);
    float C2p = sqrt(a2p * a2p + lab2.z * lab2.z);
    float h1p = atan(lab1.z, a1p);
    float h2p = atan(lab2.z, a2p);

    float dL = lab2.x - lab1.x;
    float dC = C2p - C1p;
    float dhp = h2p - h1p;

    if (dhp > 3.141592653589793) dhp -= 6.283185307179586;
    if (dhp < -3.141592653589793) dhp += 6.283185307179586;

    float dHp = 2.0 * sqrt(C1p * C2p) * sin(dhp / 2.0);
    
    float L = dL;
    C = dC;
    float H = dHp;

    float S_L = 1.0;
    float S_C = 1.0 + 0.03 * C1;
    float S_H = 1.0 + 0.045 * C1;

    float R_T = -sin(2.0 * 3.141592653589793 * (C1 / 2.0));

    return sqrt((L / S_L) * (L / S_L) + (C / S_C) * (C / S_C) + (H / S_H) * (H / S_H) + R_T * (C / S_C) * (H / S_H));
}

vec4 gammaCorrect(vec4 color) {
    return pow(color, vec4(1.0 / 3.8));
}

vec4 inverseGammaCorrect(vec4 color) {
    return pow(color, vec4(3.8));
}

vec4 findClosestColor(vec4 color) {
    vec3 inputColorOkLab = rgbToOkLab(color.rgb);
    vec4 closestColor = colorPalette[0];
    float minDistance = deltaE2000(inputColorOkLab, rgbToOkLab(closestColor.rgb));

    for (int i = 1; i < Size; i++) {
        float distance = deltaE2000(inputColorOkLab, rgbToOkLab(colorPalette[i].rgb));
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
