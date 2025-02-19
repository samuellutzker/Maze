package com.sam.maze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Model {
    public static class Vertex {
        public Vec3 position, normal;
        public Vec2 texCoord;
        public Vertex(Vec3 position, Vec3 normal, Vec2 texCoord) {
            this.position = position;
            this.normal = normal;
            this.texCoord = texCoord;
        }
    }

    FloatBuffer buffer;
    int vao, vbo;
    float shininess;
    int numVertices;
    final int bytesPerFloat = 4;
    final int floatsPerVertex = 3 + 3 + 2;
    final int vertexStride = floatsPerVertex * bytesPerFloat; // pos,normal,texcoord * 4bytes
    private int diffuseTex, specularTex;

    private void setupVertexArray() {
        final int POSITION = 0, NORMAL = 1, TEXCOORD = 2;

        int [] objId = new int[1];
        // IntBuffer buffer = ByteBuffer.allocateDirect(8).asIntBuffer();
        GLES32.glGenVertexArrays(1, objId, 0);
        vao = objId[0];
        GLES32.glBindVertexArray(vao);
        GLES32.glGenBuffers(1, objId, 0);
        vbo = objId[0];
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, numVertices * vertexStride, buffer, GLES32.GL_STATIC_DRAW);

        // attributes
        GLES32.glVertexAttribPointer(POSITION, 3, GLES32.GL_FLOAT, false, vertexStride, 0);
        GLES32.glEnableVertexAttribArray(POSITION);
        GLES32.glVertexAttribPointer(NORMAL, 3, GLES32.GL_FLOAT, false, vertexStride, 3*bytesPerFloat);
        GLES32.glEnableVertexAttribArray(NORMAL);
        GLES32.glVertexAttribPointer(TEXCOORD, 2, GLES32.GL_FLOAT, false, vertexStride, 6*bytesPerFloat);
        GLES32.glEnableVertexAttribArray(TEXCOORD);

        GLES32.glBindVertexArray(0);

    }

    private void setupBuffer(float [] array) {
        buffer = ByteBuffer.allocateDirect(array.length * bytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(array).position(0);
    }

    int loadTexture(final Context context, final int resourceId) {
        final int [] tex = new int[1];
        GLES32.glGenTextures(1, tex, 0);
        if (tex[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tex[0]);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);

        bitmap.recycle();

        return tex[0];
    }

    // Same model, other texture
    public Model(Context context, Model other, int diffuseMapResId, int specularMapResId, float shininess) {
        this.numVertices = other.numVertices;
        this.vao = other.vao;
        this.diffuseTex = loadTexture(context, diffuseMapResId);
        this.specularTex = loadTexture(context, specularMapResId);
        this.shininess = shininess;
    }

    public Model(Context context, Vertex[] vertices) {
        numVertices = vertices.length;
        float [] bufData = new float[numVertices * floatsPerVertex];
        for (int i=0; i < vertices.length; ++i) {
            bufData[floatsPerVertex*i]   = vertices[i].position.x;
            bufData[floatsPerVertex*i+1] = vertices[i].position.y;
            bufData[floatsPerVertex*i+2] = vertices[i].position.z;
            bufData[floatsPerVertex*i+3] = vertices[i].normal.x;
            bufData[floatsPerVertex*i+4] = vertices[i].normal.y;
            bufData[floatsPerVertex*i+5] = vertices[i].normal.z;
            bufData[floatsPerVertex*i+6] = vertices[i].texCoord.x;
            bufData[floatsPerVertex*i+7] = vertices[i].texCoord.y;
        }
        setupBuffer(bufData);
        setupVertexArray();
    }

    public Model(Context context, float [][] vPos, float [][] vNorm, float [][] vTex) {
        if (vPos.length != vNorm.length || vPos.length != vTex.length) {
            throw new IllegalArgumentException("Model: Wrong matrix dimensions.");
        }
        numVertices = vPos.length;
        float [] bufData = new float[numVertices * floatsPerVertex];

        for (int i=0; i < vPos.length; ++i) {
            if (vPos[i].length != 3 || vNorm[i].length != 3 || vTex[i].length != 2)
                throw new IllegalArgumentException("Model: Wrong matrix inner dimensions.");

            bufData[floatsPerVertex*i]   = vPos[i][0];
            bufData[floatsPerVertex*i+1] = vPos[i][1];
            bufData[floatsPerVertex*i+2] = vPos[i][2];
            bufData[floatsPerVertex*i+3] = vNorm[i][0];
            bufData[floatsPerVertex*i+4] = vNorm[i][1];
            bufData[floatsPerVertex*i+5] = vNorm[i][2];
            bufData[floatsPerVertex*i+6] = vTex[i][0];
            bufData[floatsPerVertex*i+7] = vTex[i][1];
        }
        setupBuffer(bufData);
        setupVertexArray();
    }

    public Model(Context context, Vertex[] vertices, int diffuseMapResId, int specularMapResId, float shininess) {
        this(context, vertices);

        diffuseTex = loadTexture(context, diffuseMapResId);
        specularTex = loadTexture(context, specularMapResId);
        this.shininess = shininess;
    }

    public Model(Context context, float [][] vPos, float [][] vNorm, float [][] vTex, int diffuseMapResId, int specularMapResId, float shininess) {
        this(context, vPos, vNorm, vTex);
        diffuseTex = loadTexture(context, diffuseMapResId);
        specularTex = loadTexture(context, specularMapResId);
        this.shininess = shininess;
    }

    public void setTextures(int diff, int spec, float shine) {
        diffuseTex = diff;
        specularTex = spec;
        shininess = shine;
    }

    public void draw(Shader shader) {
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0+diffuseTex);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, diffuseTex);
        shader.uniform("material.diffuse", diffuseTex);
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0+specularTex);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, specularTex);
        shader.uniform("material.specular", specularTex);
        shader.uniform("material.shininess", shininess);

        GLES32.glBindVertexArray(vao);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, numVertices);
        GLES32.glBindVertexArray(0);
    }
}
