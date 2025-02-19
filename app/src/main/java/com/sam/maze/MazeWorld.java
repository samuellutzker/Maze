package com.sam.maze;

import static java.lang.Math.abs;
import static java.lang.Math.round;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.Matrix;

public class MazeWorld extends Maze {
    private final static float WALL_WIDTH = 0.05f, WALL_REPEATS = 3.2f;

    private final Model wall, floor, tower, sky, earth; // ball
    private final Shader shader, bg_shader;
    private final Light backlight, flashlight, lamp;
    private float rot_xy, rot_z;
    private Vec3 pos, dir;
    private boolean isLookingBack = false;
    private boolean isAlive;

    private Vec3 deathPos, deathDir, deathUp;
    private long deathTime;

    private static float radians(float deg) {
        return deg * (float)Math.PI / 180.0f;
    }

    private void setView() {
        final double deathDuration = 3000.0; // death takes 3s
        float [] view = new float[16];
        Vec3 viewDir = new Vec3(dir.x * (isLookingBack ? -1 : 1), dir.y * (isLookingBack ? -1 : 1), dir.z);
        float a = (float) Math.sqrt(1.0f - viewDir.z * viewDir.z);
        Vec3 viewUp = new Vec3(-viewDir.z * viewDir.x / a, -viewDir.z * viewDir.y / a, a);
        Vec3 viewPos = new Vec3(pos);

        if (!isAlive) {
            long time = System.currentTimeMillis();
            if (time - deathTime < deathDuration) {
                float phase = (float) (0.5 * Math.sin(Math.min((double) (time - deathTime) / deathDuration * Math.PI, Math.PI) - Math.PI / 2.0) + 0.5);
                viewPos = viewPos.mul(1.0f - phase).add(deathPos.mul(phase));
                viewDir = viewDir.mul(1.0f - phase).add(deathDir.mul(phase));
                viewUp = viewUp.mul(1.0f - phase).add(deathUp.mul(phase));
            } else {
                pos = viewPos = deathPos;
                dir = viewDir = deathDir;
                viewUp = deathUp;
            }
        }

        final Vec3 viewCenter = viewPos.add(viewDir);
        Matrix.setLookAtM(view, 0, viewPos.x, viewPos.y, viewPos.z, viewCenter.x, viewCenter.y, viewCenter.z, viewUp.x, viewUp.y, viewUp.z);

        shader.uniform("view", view);
        bg_shader.uniform("view", view);
        shader.uniform("viewPos", viewPos);
    }

    private void setProjection(int w, int h) {
        float [] mat = new float[16];
        if (h == 0) return;
        float ratio = (float)w / h;
        // Matrix.frustumM(mat, 0, -ratio,ratio,-1,1,0.1f, 150.0f);
        Matrix.perspectiveM(mat, 0, 45.0f, ratio, 0.02f, 100.0f);
        shader.uniform("proj", mat);
        bg_shader.uniform("proj", mat);
    }

    public void resize(int scr_w, int scr_h) {
        setProjection(scr_w, scr_h);
    }

    public void walk(float step, float strafe) {
        if (!isAlive) {
            deathPos.z = Math.max(2.0f, Math.min(deathPos.z - step, 10.0f));
            return;
        }

        step = Math.max(-0.1f, Math.min(0.1f, step));

        Vec3 way = new Vec3(step * dir.x, step * dir.y, 0.0f);
        if (strafe != 0.0f) {
            strafe = Math.max(-0.1f, Math.min(0.1f, strafe));
            way = way.add(new Vec3(strafe * dir.y, -strafe * dir.x, 0.0f));
        }

        final float w = 0.3f;

        // simple collision detection
        float xDiff = pos.x-round(pos.x);
        float yDiff = pos.y-round(pos.y);

        if (abs(xDiff) <= w)
            if (left(round(pos.x),(int)pos.y) && (pos.x-round(pos.x))*way.x < 0) way.x = 0;
        if (abs(yDiff) <= w)
            if (top((int)pos.x,round(pos.y)) && (pos.y-round(pos.y))*way.y < 0) way.y = 0;
        if (abs(yDiff) <= w*0.7f && abs(xDiff) <= w*0.7f) {
            // "slice" into wall
            if (left(round(pos.x),round(pos.y)-(yDiff>0?1:0)) && (pos.y-round(pos.y))*way.y < 0) way.y = 0;
            if (top(round(pos.x)-(xDiff>0?1:0),round(pos.y)) && (pos.x-round(pos.x))*way.x < 0) way.x = 0;
        }

        pos = pos.add(way);
    }

    public void turn(float horizontal, float vertical) {
        if (isAlive) {
            rot_z = Math.max(radians(-89.5f), Math.min(radians(89.5f), rot_z + vertical));
            rot_xy += horizontal;
            dir = new Vec3((float)(Math.cos(rot_xy) * Math.cos(rot_z)), (float)(Math.sin(rot_xy) * Math.cos(rot_z)), (float)Math.sin(rot_z));
        } else {
            deathPos.x = Math.max(Math.min((float)width, deathPos.x - horizontal * deathUp.y + vertical * deathUp.x), 0.0f);
            deathPos.y = Math.max(Math.min((float)height, deathPos.y + vertical * deathUp.y + horizontal * deathUp.x), 0.0f);
        }
    }

    public void die() {
        isAlive = false;
        deathPos = pos.add(new Vec3(0.0f, 0.0f, 5.0f));
        // deathUp = new Vec3(dir);
        deathUp = (new Vec3(dir.x, dir.y, 0.0f)).normalize();
        deathDir = new Vec3(0.1f*deathUp.x, 0.1f*deathUp.y, -1.0f);
        deathTime = System.currentTimeMillis();
        solve();
    }

    public void rearView() {
        isLookingBack = !isLookingBack;
    }

    public boolean inside() {
        return pos.x >= 0.0f && pos.x < (float)width && pos.y >= 0.0f && pos.y < (float)height;
    }

    public void draw() {
        if (isAlive) {
            flashlight.move(pos.add(new Vec3(0.0f, 0.0f, 0.1f)), shader);
            flashlight.turn(dir, shader);
        } else {
            flashlight.turn(new Vec3(0.0f, 0.0f, -1.0f), shader);
        }
        backlight.apply(shader);
        lamp.apply(shader);
        setView();

        float [] id = new float[16];
        float [] di = new float[16];
        Matrix.setIdentityM(id, 0);
        Matrix.rotateM(di, 0, id, 0, 90.0f, 0.0f, 0.0f, 1.0f);

        float [] model = new float[16];
        float [] normal = new float[16];

        // sky
        Matrix.scaleM(model, 0, id, 0, 30.0f, 30.0f, 30.0f);
        bg_shader.uniform("model", model);
        bg_shader.uniform("ambient", new Vec3(0.4f));
        bg_shader.uniform("dirLightColor", new Vec3(0.0f));
        GLES32.glFrontFace(GLES32.GL_CW); // change orientation: view from inside of the ball
        sky.draw(bg_shader);
        GLES32.glFrontFace(GLES32.GL_CCW);

        // earth
        Matrix.translateM(model, 0, id, 0, -1.1f, -2.5f, 3.0f);
        Matrix.rotateM(model, 0, 130.0f, 1.0f, 1.0f, 1.0f);
        bg_shader.uniform("model", model);
        bg_shader.uniform("ambient", new Vec3(0.1f, 0.07f, 0.072f));
        bg_shader.uniform("dirLightColor", new Vec3(3.0f));
        bg_shader.uniform("dirLightDirection", new Vec3(-1.0f, -1.0f, -2.0f));
        earth.draw(bg_shader);

        final int view_limit = isAlive ? 15 : 20;

        for (int y=0; y <= height; ++y) {
            for (int x=0; x <= width; ++x) {
                if (abs(pos.x-x)+abs(pos.y-y) > view_limit) continue;
                if (y < height && x < width && visit[y*width+x]) {
                    Matrix.translateM(model, 0, id, 0, (float)x, (float)y, -0.8f * WALL_WIDTH);
                    Matrix.rotateM(model, 0, -90.0f, 1.0f, 0.0f, 0.0f);
                    Matrix.rotateM(normal, 0, id, 0, -90.0f, 1.0f, 0.0f, 0.0f);
                    shader.uniform("model", model);
                    shader.uniform("normal", normal);
                    wall.draw(shader);
                }

                if (top(x,y)) {
                    Matrix.translateM(model,0, id, 0, x, y, 0.0f);
                    // Matrix.scaleM(model, 0, 1.01f, 1.01f, 1.0f);
                    shader.uniform("normal", id);
                    shader.uniform("model", model);
                    wall.draw(shader);
                }
                if (left(x,y)) {
                    Matrix.translateM(model,0, di, 0, y, -x, 0.0f);
                    // Matrix.scaleM(model, 0, 1.01f, 1.01f, 1.0f);
                    shader.uniform("normal", di);
                    shader.uniform("model", model);
                    wall.draw(shader);
                }
                if ((top(x-1,y) || (top(x,y))) && (left(x,y) || left(x,y-1))) {
                    Matrix.translateM(model,0, id, 0, x, y, 0.0f);
                    shader.uniform("normal", id);
                    shader.uniform("model", model);
                    tower.draw(shader);
                }
            }
        }

        shader.uniform("model", id);
        shader.uniform("normal", id);
        floor.draw(shader);
    }

    private static Model.Vertex [] makeColumn(float r, float res) {
        final float top = 1.02f;
        final float pi = (float)Math.PI;
        final int [] zigzag = {0,0,1,0,0,1,0,1,1,0,1,1};

        int l = (int)(1.0/res);

        Model.Vertex [] data = new Model.Vertex[9*l];

        for (int i=0; i < l; ++i) {
            for (int k=0; k < 6; ++k) {
                float u = (i+zigzag[2*k])  *res;
                float phi = u*2.0f*pi;
                float x = r * (float)Math.cos(phi);
                float y = r * (float)Math.sin(phi);
                float z = zigzag[2*k+1]*top;
                data[6*i+k] = new Model.Vertex(new Vec3(x,y,z), new Vec3(x,y,0), new Vec2(WALL_REPEATS*u, WALL_REPEATS*z));
            }
        }
        data[6*l] = new Model.Vertex(new Vec3(1.0f,0.0f,top), new Vec3(0.0f,0.0f,top), new Vec2(1.0f,0.0f));

        for (int i=0; i < l; ++i) {
            float phi = -2.0f*i*res*pi;
            float x = r * (float)Math.cos(phi);
            float y = r * (float)Math.sin(phi);
            data[6*l+3*i+1] = new Model.Vertex(new Vec3(0.0f,0.0f,top), new Vec3(0.0f,0.0f,top), new Vec2(0.0f,0.0f));
            data[6*l+3*i+2] = new Model.Vertex(new Vec3(x,y,top), new Vec3(0.0f,0.0f,top), new Vec2(WALL_REPEATS*x,WALL_REPEATS*y));
            if (i < l-1)
                data[6*l+3*i+3] = new Model.Vertex(new Vec3(x,y,top), new Vec3(0.0f,0.0f,top), new Vec2(WALL_REPEATS*x,WALL_REPEATS*y));
        }

        return data;
    }

    private static Model.Vertex [] makeBall(float r, float res) {
        final float pi = (float)Math.PI;
        final int [] zigzag = {0,0,1,0,0,1,0,1,1,0,1,1};

        int l = (int)(1.0/res);

        Model.Vertex [] data = new Model.Vertex[6*l*l];

        for (int i=0; i < l*l; ++i) {
            for (int k=0; k < 6; ++k) {
                float u = ((i/l)+zigzag[2*k])  *res;
                float v = ((i%l)+zigzag[2*k+1])*res;
                float phi = u*2*pi;
                float psi = v*pi-pi/2;
                float x = (float)(r * Math.cos(phi) * Math.cos(psi));
                float y = (float)(r * Math.sin(phi) * Math.cos(psi));
                float z = (float)(r * Math.sin(psi));
                data[6*i+k] = new Model.Vertex(new Vec3(x,y,z), new Vec3(x,y,z), new Vec2(u, -v));
            }
        }
        return data;
    }

    public MazeWorld(Context context, int width, int height, int scr_w, int scr_h) {
        super(width, height);

        isAlive = true;

        final int FLOOR_REPEATS = 4;
        final float WALL_CORNER_F = 0.015f;
        final float WALL_CORNER_S = 0.006f;
        final float WALL_CORNER_D = (float) Math.sqrt(WALL_CORNER_F * WALL_CORNER_F + WALL_CORNER_S * WALL_CORNER_S);
        final float WALL_CORNER_SN = WALL_CORNER_S / WALL_CORNER_D;
        final float WALL_CORNER_FN = WALL_CORNER_F / WALL_CORNER_D;

        // Vertices, normals and texture coords for a wall
        final float [][] wallPos = {
                // front face
                {0.0f+WALL_CORNER_F, WALL_WIDTH, 1.0f}, {1.0f-WALL_CORNER_F, WALL_WIDTH, 1.0f}, {0.0f+WALL_CORNER_F, WALL_WIDTH, 0.0f},
                {0.0f+WALL_CORNER_F, WALL_WIDTH, 0.0f}, {1.0f-WALL_CORNER_F, WALL_WIDTH, 1.0f}, {1.0f-WALL_CORNER_F, WALL_WIDTH, 0.0f},
                // sides
                {0.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f}, {0.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f},
                {0.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 0.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f},

                {1.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f}, {1.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f},
                {1.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 0.0f},
                // corners
                {0.0f+WALL_CORNER_F, WALL_WIDTH, 1.0f}, {0.0f+WALL_CORNER_F, WALL_WIDTH, 0.0f}, {0.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f},
                {0.0f+WALL_CORNER_F, WALL_WIDTH, 0.0f}, {0.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f}, {0.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f},

                {0.0f+WALL_CORNER_F, -WALL_WIDTH, 1.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f}, {0.0f+WALL_CORNER_F, -WALL_WIDTH, 0.0f},
                {0.0f+WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f}, {0.0f, -WALL_WIDTH+WALL_CORNER_S, 0.0f},

                {1.0f-WALL_CORNER_F, WALL_WIDTH, 1.0f}, {1.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f}, {1.0f-WALL_CORNER_F, WALL_WIDTH, 0.0f},
                {1.0f-WALL_CORNER_F, WALL_WIDTH, 0.0f}, {1.0f, WALL_WIDTH-WALL_CORNER_S, 1.0f}, {1.0f, WALL_WIDTH-WALL_CORNER_S, 0.0f},

                {1.0f-WALL_CORNER_F, -WALL_WIDTH, 1.0f}, {1.0f-WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f},
                {1.0f-WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 0.0f}, {1.0f, -WALL_WIDTH+WALL_CORNER_S, 1.0f},
                // back face
                {0.0f+WALL_CORNER_F, -WALL_WIDTH, 1.0f}, {0.0f+WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {1.0f-WALL_CORNER_F, -WALL_WIDTH, 1.0f},
                {0.0f+WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {1.0f-WALL_CORNER_F, -WALL_WIDTH, 0.0f}, {1.0f-WALL_CORNER_F, -WALL_WIDTH, 1.0f},
                // top
                {1.0f, WALL_WIDTH, 1.0f}, {0.0f, WALL_WIDTH, 1.0f}, {1.0f, -WALL_WIDTH, 1.0f},
                {0.0f, -WALL_WIDTH, 1.0f}, {1.0f, -WALL_WIDTH, 1.0f}, {0.0f, WALL_WIDTH, 1.0f},

        };

        final float [][] wallNorm = {
                {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f},
                {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f},

                {-1.0f, 0.0f, 0.0f}, {-1.0f, 0.0f, 0.0f}, {-1.0f, 0.0f, 0.0f},
                {-1.0f, 0.0f, 0.0f}, {-1.0f, 0.0f, 0.0f}, {-1.0f, 0.0f, 0.0f},

                {1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f},
                // corners
                {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f},
                {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, WALL_CORNER_FN, 0.0f},

                {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f},
                {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {-WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f},

                {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f},
                {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, WALL_CORNER_FN, 0.0f},

                {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f},
                {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f}, {WALL_CORNER_SN, -WALL_CORNER_FN, 0.0f},

                {0.0f, -1.0f, 0.0f}, {0.0f, -1.0f, 0.0f}, {0.0f, -1.0f, 0.0f},
                {0.0f, -1.0f, 0.0f}, {0.0f, -1.0f, 0.0f}, {0.0f, -1.0f, 0.0f},
                // top
                {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f},
                {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f},
        };

        final float [][] wallTex = {
                {0.0f, WALL_REPEATS}, {WALL_REPEATS, WALL_REPEATS}, {0.0f, 0.0f},
                {0.0f, 0.0f}, {WALL_REPEATS, WALL_REPEATS}, {WALL_REPEATS, 0.0f},

                {0.0f, WALL_REPEATS}, {0.0f, 0.0f}, {2* WALL_WIDTH *WALL_REPEATS, WALL_REPEATS},
                {0.0f, 0.0f}, {2* WALL_WIDTH *WALL_REPEATS, 0.0f}, {2* WALL_WIDTH *WALL_REPEATS, WALL_REPEATS},

                {0.0f, WALL_REPEATS}, {2* WALL_WIDTH *WALL_REPEATS, WALL_REPEATS}, {0.0f, 0.0f},
                {0.0f, 0.0f}, {2* WALL_WIDTH *WALL_REPEATS, WALL_REPEATS}, {2* WALL_WIDTH *WALL_REPEATS, 0.0f},
                // corners
                {0.0f, WALL_REPEATS}, {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS},
                {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS},

                {0.0f, WALL_REPEATS}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS}, {0.0f, 0.0f},
                {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS}, {WALL_CORNER_D * WALL_REPEATS, 0.0f},

                {0.0f, WALL_REPEATS}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS}, {0.0f, 0.0f},
                {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS}, {WALL_CORNER_D * WALL_REPEATS, 0.0f},

                {0.0f, WALL_REPEATS}, {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS},
                {0.0f, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, 0.0f}, {WALL_CORNER_D * WALL_REPEATS, WALL_REPEATS},

                {0.0f, WALL_REPEATS}, {0.0f, 0.0f}, {WALL_REPEATS, WALL_REPEATS},
                {0.0f, 0.0f}, {WALL_REPEATS, 0.0f}, {WALL_REPEATS, WALL_REPEATS},
                // top
                {WALL_REPEATS, 0.0f}, {0.0f, 0.0f}, {WALL_REPEATS, 2*WALL_WIDTH * WALL_REPEATS},
                {0.0f, 2*WALL_WIDTH * WALL_REPEATS}, {WALL_REPEATS, 2*WALL_WIDTH * WALL_REPEATS}, {0.0f, 0.0f}

        };

        // Floor data
        final float [][] floorPos = {
                {0.0f, 0.0f, 0.0f}, {width*1.0f, 0.0f, 0.0f}, {width*1.0f, height*1.0f, 0.0f},
                {0.0f, 0.0f, 0.0f}, {width*1.0f, height*1.0f, 0.0f}, {0.0f, height*1.0f, 0.0f}
        };

        final float [][] floorNorm = {
                {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f},
                {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 0.0f, 1.0f}
        };

        final float [][] floorTex = {
                {0.0f, 0.0f}, {FLOOR_REPEATS*width*1.0f, 0.0f}, {FLOOR_REPEATS*width*1.0f, FLOOR_REPEATS*height*1.0f},
                {0.0f, 0.0f}, {FLOOR_REPEATS*width*1.0f, FLOOR_REPEATS*height*1.0f}, {0.0f, FLOOR_REPEATS*height*1.0f}
        };

        // Initial position and direction
        pos = new Vec3((float)width-0.5f, (float)height-0.5f, 0.5f);
        rot_xy = radians(-135.0f);
        rot_z = 0.0f;

        // Load shader with and w/o lighting
        shader = new Shader(context, "vertex.glsl", "frag.glsl");
        bg_shader = new Shader(context, "bg_vertex.glsl", "bg_frag.glsl");

        // Load all models
        wall = new Model(context, wallPos, wallNorm, wallTex, R.drawable.wall, R.drawable.wall_spec, 0.3f);
        floor = new Model(context, floorPos, floorNorm, floorTex, R.drawable.floor, R.drawable.floor_spec, 0.4f);
        sky = new Model(context, makeBall(1.0f, 0.01f), R.drawable.sky, R.drawable.sky, 0.0f);
        tower = new Model(context, makeColumn(0.10f, 0.01f), R.drawable.wall, R.drawable.wall_spec, 0.55f);
        earth = new Model(context, sky, R.drawable.earth, R.drawable.earth, 0.0f);

        // lights
        Light.reset();
        backlight = new Light(new Vec3(0.25f), new Vec3(0.35f), new Vec3(0.15f));
        backlight.turn(new Vec3(0.4f, 0.2f, -1.0f), shader);
        lamp = new Light(new Vec3(0.05f), new Vec3(0.7f), new Vec3(0.35f), 1.0f, 0.09f, 0.032f, -1.0f);
        lamp.move(new Vec3((float)width-0.5f, (float)height-0.5f, 1.0f), shader);
        flashlight = new Light(new Vec3(0.0f), new Vec3(0.8f), new Vec3(0.4f), 1.0f, 0.18f, 0.096f, (float) Math.cos(radians(12.5f)));

        resize(scr_w, scr_h);
        turn(0.0f, 0.0f);

        // mark the entrance
        visit[(height-1)*width+width-1] = true;
    }
}
