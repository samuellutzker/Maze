package com.sam.maze;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES32;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Shader {
    private String vertexShader, fragmentShader;
    private int programHandle;

    private void setup() {
        int vertexShaderHandle = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        String message = "";
        if (vertexShaderHandle != 0)
        {
            GLES32.glShaderSource(vertexShaderHandle, vertexShader);
            GLES32.glCompileShader(vertexShaderHandle);

            final int[] compileStatus = new int[1];
            GLES32.glGetShaderiv(vertexShaderHandle, GLES32.GL_COMPILE_STATUS, compileStatus, 0);

            if (compileStatus[0] == 0)
            {
                message = GLES32.glGetShaderInfoLog(vertexShaderHandle);
                GLES32.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }
        int fragmentShaderHandle = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        if (vertexShaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader:\n"+message);
        }
        if (fragmentShaderHandle != 0)
        {
            GLES32.glShaderSource(fragmentShaderHandle, fragmentShader);
            GLES32.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES32.glGetShaderiv(fragmentShaderHandle, GLES32.GL_COMPILE_STATUS, compileStatus, 0);

            if (compileStatus[0] == 0)
            {
                message = GLES32.glGetShaderInfoLog(fragmentShaderHandle);
                GLES32.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }
        if (fragmentShaderHandle == 0)
        {
            throw new RuntimeException("Error creating fragment shader:\n"+message);
        }
        // Program
        programHandle = GLES32.glCreateProgram();
        if (programHandle != 0)
        {
            GLES32.glAttachShader(programHandle, vertexShaderHandle);
            GLES32.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES32.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES32.glBindAttribLocation(programHandle, 1, "a_Color");

            // Link the two shaders together into a program.
            GLES32.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES32.glGetProgramiv(programHandle, GLES32.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                message = GLES32.glGetProgramInfoLog(programHandle);
                GLES32.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.\n"+message);
        }
        use();
    }

    public Shader(Context context, String vertex, String fragment) {
        try {
            Resources resources = context.getResources();
            InputStream inputStream = resources.getAssets().open(vertex);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            vertexShader = reader.lines().collect(Collectors.joining("\n"));
            inputStream = resources.getAssets().open(fragment);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            fragmentShader = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException exception) {
            throw new RuntimeException("Error: Couldn't open shader file:\n"+exception.getMessage());
        }
        setup();
    }

    private int location(String name) {
        int location = GLES32.glGetUniformLocation(programHandle, name);
//        if (location == -1)
//            throw new RuntimeException("Couldn't retrieve location of " + name);
        return location;

    }

    public void use() {
        GLES32.glUseProgram(programHandle);
    }

    public void uniform(String name, boolean value) {
        use();
        GLES32.glUniform1i(location(name), value ? 1 : 0);
        checkErrors();
    }

    public void uniform(String name, int value) {
        use();
        GLES32.glUniform1i(location(name), value);
        checkErrors();
    }
    public void uniform(String name, float value) {
        use();
        GLES32.glUniform1f(location(name), value);
        checkErrors();
    }
    public void uniform(String name, float[] matrix) {
        use();
        if (matrix.length == 16)
            GLES32.glUniformMatrix4fv(location(name), 1, false, matrix, 0);
        else if (matrix.length == 9)
            GLES32.glUniformMatrix3fv(location(name), 1, false, matrix, 0);
        checkErrors();
    }
    public void uniform(String name, Vec3 v) {
        use();
        GLES32.glUniform3f(location(name), v.x, v.y, v.z);
        checkErrors();
    }
    public void uniform(String name, Vec2 v) {
        use();
        GLES32.glUniform2f(location(name), v.x, v.y);
        checkErrors();
    }
    private static void checkErrors() {
        int error = GLES32.glGetError();
        if (error != GLES32.GL_NO_ERROR) {
            throw new RuntimeException("OpenGL Error number " + error);
        }
    }
}
