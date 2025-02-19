package com.sam.maze;

public class Light {
    private static int point_count = 0, dir_count = 0;
    final private boolean point;
    final private int index;
    final private Vec3 ambient, diffuse, specular;
    private Vec3 pos, dir;
    private float constant, linear, quadratic, cutoff;

    public static void reset() { point_count = dir_count = 0; }

    public Light(Vec3 ambient, Vec3 diffuse, Vec3 specular) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        pos = new Vec3(0.0f);
        dir = new Vec3(0.0f);
        point = false;
        index = dir_count++;
    }


    public Light(Vec3 ambient, Vec3 diffuse, Vec3 specular, float constant, float linear, float quadratic, float cutoff) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
        this.cutoff = cutoff;
        pos = new Vec3(0.0f);
        dir = new Vec3(0.0f);
        point = true;
        index = point_count++;
    }

    void apply(Shader shader) {
        String type = point ? "point" : "dir";

        shader.uniform(type + "Light[" + index + "].ambient", ambient);
        shader.uniform(type + "Light[" + index + "].diffuse", diffuse);
        shader.uniform(type + "Light[" + index + "].specular", specular);
        shader.uniform(type + "Light[" + index + "].dir", dir);

        if (point) {
            shader.uniform("pointLight[" + index + "].pos", pos);
            shader.uniform("pointLight[" + index + "].constant", constant);
            shader.uniform("pointLight[" + index + "].linear", linear);
            shader.uniform("pointLight[" + index + "].quadratic", quadratic);
            shader.uniform("pointLight[" + index + "].cutoff", cutoff);
        }
    }

    void move(Vec3 pos, Shader shader) {
        this.pos = pos;
        apply(shader);
    }

    void turn(Vec3 dir, Shader shader) {
        this.dir = dir;
        apply(shader);
    }
}
