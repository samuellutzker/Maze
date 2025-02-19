#version 300 es
precision highp float;

#define NUM_POINT_LIGHTS 2
#define NUM_DIR_LIGHTS 1
#define CUTOFF_SMOOTH 0.01

in vec3 fPos;
in vec3 fNorm;
in vec2 fTex;

out vec4 outColor;

struct Material {
    sampler2D diffuse, specular;
    float shininess;
};

struct Light {
    vec3 pos, dir;
    vec3 ambient, diffuse, specular;
    float constant, linear, quadratic;
    float cutoff; // point-light: cutoff = -1.0f
};

uniform Material material;
uniform vec3 viewPos;
uniform Light pointLight[NUM_POINT_LIGHTS];
uniform Light dirLight[NUM_DIR_LIGHTS];

vec3 calcDirLight(Light light, vec3 viewDir);
vec3 calcPointLight(Light light, vec3 viewDir);

void main() {
    vec3 result = vec3(0.0);

    vec3 viewDir = normalize(viewPos-fPos);

    for (int i=0; i < NUM_POINT_LIGHTS; ++i)
        result += calcPointLight(pointLight[i], viewDir);
    for (int i=0; i < NUM_DIR_LIGHTS; ++i)
        result += calcDirLight(dirLight[i], viewDir);

    outColor = vec4(result, 0.0);
}

vec3 calcDirLight(Light light, vec3 viewDir) {
    // diffuse
    viewDir = normalize(viewDir);
    vec3 norm = normalize(fNorm);
    vec3 lightDir = normalize(-light.dir);
    float diff = max(dot(norm, lightDir), 0.0);

    // specular
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(reflectDir, viewDir), 0.0), material.shininess * 128.0);

    vec3 texDiff = vec3(texture(material.diffuse, fTex));
    vec3 texSpec = vec3(texture(material.specular, fTex));

    vec3 ambient =  light.ambient * texDiff;
    vec3 diffuse =  light.diffuse * diff * texDiff;
    vec3 specular = light.specular * spec * texSpec;

    return ambient + diffuse + specular;
}

vec3 calcPointLight(Light light, vec3 viewDir) {
    vec3 lightDir = normalize(fPos-light.pos);
    float angle = dot(lightDir, normalize(light.dir));

    if (light.cutoff < 0.0 || angle > light.cutoff) {
        float smoother = light.cutoff >= 0.0 ? min(1.0, (angle - light.cutoff) / CUTOFF_SMOOTH) : 1.0; // this is actually dependant on the cutoff angle, better way: extra uniform
        float d = length(light.pos - fPos);
        light.dir = lightDir;

        return smoother * calcDirLight(light, viewDir) / (light.constant + light.linear * d + light.quadratic * d * d);
    } else {
        // ambient
        vec3 texDiff = vec3(texture(material.diffuse, fTex));
        return light.ambient * texDiff;
    }
}
