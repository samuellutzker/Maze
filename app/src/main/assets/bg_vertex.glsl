#version 300 es

layout(location = 0) in vec3 vPos;
layout(location = 1) in vec3 vNorm;
layout(location = 2) in vec2 vTex;

out vec3 fNorm;
out vec2 fTex;

uniform mat4 model, view, proj;

void main() {
    fTex = vTex;
    fNorm = vNorm;
    gl_Position = proj * mat4(mat3(view)) * model * vec4(vPos, 1.0);
}