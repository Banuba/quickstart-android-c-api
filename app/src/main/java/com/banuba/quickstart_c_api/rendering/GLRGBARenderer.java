package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRGBARenderer extends GLRenderer {
    private static final String VERTEX_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "layout (location = 0) in vec3 aPosition;\n" +
            "layout (location = 1) in vec2 aTextureCoord;\n" +
            "uniform mat4 uMatrix;\n" +
            "out vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(aPosition, 1.0f) * uMatrix;\n" +
            "  vTexCoord = aTextureCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 outFragColor;\n" +
            "void main() {\n" +
            "  outFragColor = vec4(texture(uTexture, vTexCoord).xyz, 1.0f);\n" +
            "}\n";

    private int mUniformTexture;
    private int mUniformMatrix;

    private ByteBuffer mBuffer = null;


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (mIsCreated) {
            return;
        }
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        makeVO();
        makeVBO();
        makeTextures();

        try {
            mShaderProgram = new GLShaderProgram(VERTEX_SHADER_PROGRAM, FRAGMENT_SHADER_PROGRAM);
            mUniformTexture = mShaderProgram.getUniformLocation("uTexture");
            mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsCreated = true;
    }
    public  void makeVO() {
        mVAO = new int[1];
        GLES30.glGenVertexArrays(mVAO.length, mVAO, 0);
        GLES30.glBindVertexArray(mVAO[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVAO[0]);

        makeVBO();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);
    }

    public void makeVBO() {
        mVBO = new int[1];
        final int floatSize = Float.SIZE / 8; /* Size of Float in bytes */
        final int xyzLen = 3; /* Number of components */
        final int xyzOffset = 0; /* Size in bytes */
        final int uvLen = 2;  /* Number of components */
        final int uvOffset = xyzLen * floatSize; /* Size in bytes */
        final int coordPerVert = xyzLen + uvLen; /* Number of components */
        final int vertStride = coordPerVert * floatSize; /* Vertex size in bytes */
        final int drawingPlaneCoordsBufferSize = vertStride * vertLen;
        final float[] drawingPlaneCoords = {
                /* X      Y     Z     U     V */
                -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, /* vertex 0 bottom left */
                1.0f,  -1.0f, 0.0f, 1.0f, 1.0f, /* vertex 1 bottom right */
                -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, /* vertex 2 top left */
                1.0f,   1.0f, 0.0f, 1.0f, 0.0f  /* vertex 3 top right */
        };
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, FloatBuffer.wrap(drawingPlaneCoords), GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(0, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(1, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
    }

    public void makeTextures() {
        mTextures = new int[1];
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsCreated || mImageDataPlanes.isEmpty()) {
            return;
        }

        /* scaling matrix */
        scaleMatrix();

        /* clear background */
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /* bind vertex array */
        mShaderProgram.use();
        GLES30.glBindVertexArray(mVAO[0]);

        /* update texture */
        updateTextures();

        /* set uniforms */
        mShaderProgram.setUniformTexture(mUniformTexture, 0);
        mShaderProgram.setUniformMat4(mUniformMatrix, mMat4);

        /* draw */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        /* clear */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES30.glBindVertexArray(0);
        mShaderProgram.unuse();
    }

    private void updateTextures() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        mBuffer = ByteBuffer.wrap(mImageDataPlanes.get(0));
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mImageWidth, mImageHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBuffer);
    }

}
