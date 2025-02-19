#version 300 es
precision mediump float;

in vec3 fNorm;
in vec2 fTex;

out vec4 outColor;

struct Material {
    sampler2D diffuse, specular;
    float shininess;
};

uniform Material material;
uniform vec3 ambient;
uniform vec3 dirLightColor;
uniform vec3 dirLightDirection;

void main() {
    vec3 diffuse = max(dot(normalize(dirLightDirection), normalize(fNorm)), 0.0) * dirLightColor;
    vec3 result = vec3(texture(material.diffuse, fTex)) * (ambient + diffuse);
    outColor = vec4(result, 1.0);
}