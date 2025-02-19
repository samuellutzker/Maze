package com.sam.maze;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Game implements GLSurfaceView.Renderer {
    private final float TOUCH_DRAG_FACTOR = 0.004f;
    private final float TOUCH_SCALE_FACTOR = 0.004f;
    private final int FRAMEBUFFER_WIDTH = 600;
    private final int FRAMEBUFFER_HEIGHT = 400;

    private Model rearMirror;
    private boolean isShowing;
    private boolean isAlive;
    private Shader shader;
    private int scrWidth, scrHeight;
    private int worldSize;
    private long startTime;
    private String level;
    private final Context context;
    private MazeWorld mazeWorld;

    public void rotateView(float dx, float dy) {
        if (isShowing)
            mazeWorld.turn(dx * TOUCH_DRAG_FACTOR, dy * TOUCH_DRAG_FACTOR);
    }

    public boolean isAlive() { return isAlive; }

    public void resign() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
        dlgAlert.setMessage("Give up?");
        dlgAlert.setTitle("Oh no!");
        dlgAlert.setPositiveButton("Yes", (dialogInterface, i) -> { isAlive = false; mazeWorld.die(); } );
        dlgAlert.setNegativeButton("No", (dialogInterface, i) -> {} );
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    public void moveView(float d) {
        if (!isShowing) return;
        mazeWorld.walk(d * TOUCH_SCALE_FACTOR, 0.0f);
        if (!mazeWorld.inside() && isAlive) {
            isShowing = false;
            long endTime = System.currentTimeMillis();
            long elapsedSeconds = (endTime - startTime) / 1000;
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
            dlgAlert.setMessage(String.format("You've made it, kudos!\n\nLevel: %s\nTime: %d:%02d", level, elapsedSeconds / 60, elapsedSeconds % 60));
            dlgAlert.setTitle("Well done.");
            dlgAlert.setPositiveButton("OK", (dialogInterface, i) -> ((Activity) context).finish());
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
        }
    }

    private boolean isDrawing = false;
    @Override
    public void onDrawFrame(GL10 glUnused) {
        if (!isShowing) {
            GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT | GLES32.GL_COLOR_BUFFER_BIT);
            return;
        }

        if (isDrawing) return;
        isDrawing = true;

        if (mazeWorld != null) {
            if (isAlive) {
                // Draw on the rear mirror
                GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fbo);
                GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT | GLES32.GL_COLOR_BUFFER_BIT);
                GLES32.glViewport(0, 0, FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT);
                mazeWorld.rearView();
                mazeWorld.draw();
                mazeWorld.rearView();
            }

            GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
            GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT | GLES32.GL_COLOR_BUFFER_BIT);
            GLES32.glViewport(0, 0, scrWidth, scrHeight);
            mazeWorld.draw();

            if (isAlive) {
                // Display rear mirror
                GLES32.glDepthFunc(GLES32.GL_ALWAYS);
                rearMirror.draw(shader);
                GLES32.glDepthFunc(GLES32.GL_LESS);
            }

        } else {
            mazeWorld = new MazeWorld(context, worldSize, worldSize, scrWidth, scrHeight);
        }

        isDrawing = false;
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES32.glViewport(0, 0, width, height);
        if (isShowing) mazeWorld.resize(width, height);
        this.scrWidth = width;
        this.scrHeight = height;

        // framebuffers
        setupFramebuffer();
        final Model.Vertex [] mirror = {
                new Model.Vertex(new Vec3(0.6f, 0.6f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(0.0f, 0.0f)),
                new Model.Vertex(new Vec3(0.9f, 0.9f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(1.0f, 1.0f)),
                new Model.Vertex(new Vec3(0.6f, 0.9f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(0.0f, 1.0f)),

                new Model.Vertex(new Vec3(0.6f, 0.6f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(0.0f, 0.0f)),
                new Model.Vertex(new Vec3(0.9f, 0.6f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(1.0f, 0.0f)),
                new Model.Vertex(new Vec3(0.9f, 0.9f, 1.0f), new Vec3(0.0f, 0.0f, 1.0f), new Vec2(1.0f, 1.0f)),
        };
        rearMirror = new Model(context, mirror);
        rearMirror.setTextures(texColorBuf, texColorBuf, 0.0f);
    }

    private void setupFramebuffer() {

        int [] handles = { fbo };
        if (fbo != 0) GLES32.glDeleteFramebuffers(1, handles, 0);
        GLES32.glGenFramebuffers(1, handles, 0);
        fbo = handles[0];

        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fbo);
        // texture for colors
        GLES32.glGenTextures(1, handles, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, handles[0]);
        texColorBuf = handles[0];
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGB, FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, 0, GLES32.GL_RGB, GLES32.GL_UNSIGNED_BYTE, null);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, handles[0], 0); // attach
        // render buffer for depth and stencil
        GLES32.glGenRenderbuffers(1, handles, 0);
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, handles[0]);
        GLES32.glRenderbufferStorage(GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH24_STENCIL8, FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT);
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, 0);
        GLES32.glFramebufferRenderbuffer(GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_STENCIL_ATTACHMENT, GLES32.GL_RENDERBUFFER, handles[0]); // attach

        if (GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER) != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer could not be setup.");
        }
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
    }

    private int fbo, texColorBuf;

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to gray.
        GLES32.glClearColor(0.01f, 0.01f, 0.01f, 1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glEnable(GLES32.GL_CULL_FACE);
        GLES32.glFrontFace(GLES32.GL_CCW);

        // framebuffer
        shader = new Shader(context, "bg_vertex.glsl", "bg_frag.glsl");
        float [] m = new float[16];
        Matrix.setIdentityM(m, 0);
        shader.uniform("model", m);
        shader.uniform("view", m);
        shader.uniform("proj", m);
        shader.uniform("ambient", new Vec3(1.0f));
    }

    public Game(Context context, int scrWidth, int scrHeight) {
        this.context = context;
        this.scrWidth = scrWidth;
        this.scrHeight = scrHeight;
        this.worldSize = 0;
        this.fbo = 0;
        this.shader = null;
        this.isShowing = false;

        CharSequence [] options = { " Trivial [5x5] ", " Easy [10x10] ", " Medium [13x13] ", " Hard [20x20] ", " Insane [100x100] " };
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);
        dlgAlert.setTitle("Select difficulty");
        dlgAlert.setSingleChoiceItems(options, 0, null);
        dlgAlert.setPositiveButton("Go", (dialogInterface, i) -> {
            int selected = ((AlertDialog)dialogInterface).getListView().getCheckedItemPosition();
            switch (selected) {
                case 0 : worldSize = 5; level = "Beginner"; break;
                case 1 : worldSize = 10; level = "Easy"; break;
                case 2 : worldSize = 13; level = "Medium"; break;
                case 3 : worldSize = 20; level = "Hard"; break;
                case 4 : worldSize = 100; level = "Insane"; break;
            }
            startTime = System.currentTimeMillis();
            isShowing = true;
            isAlive = true;
        });
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();

    }
}
