package com.banuba.quickstart_c_api;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.banuba.sdk.effect_player_c_api.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private int mWidth = 0;
    private int mHeight = 0;
    private byte[] mImageData = null;

    private boolean isInit = false;

    private GLShaderProgram mProgram = null;

    private static final String vs =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "layout (location = 0) in vec3 aPosition;\n" +
            "layout (location = 1) in vec2 aTextureCoord;\n" +
            "uniform mat3 uMatrix;\n" +
            "out vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(aPosition * uMatrix, 1.0f);\n" +
            "  vTexCoord = aTextureCoord;\n" +
            "}\n";

    private static final String fs =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 outFragColor;\n" +
            "void main() {\n" +
            "  outFragColor = vec4(texture(uTexture, vTexCoord).xyz, 1.0f);\n" +
            "}\n";

    private int mViewportWidth;
    private int mViewportHeight;
    private int mUniformTexture;
    private int mUniformMatrix;
    private int mAttributePosition;
    private int mAttributeTextureCoord;
    private int[] mVBO;
    private int[] mTexture;

    final int floatSize = Float.SIZE / 8; /* Size in bytes */
    final int xyzLen = 3; /* Number of components */
    final int xyzOffset = 0; /* Size in bytes */
    final int uvLen = 2;  /* Number of components */
    final int uvOffset = xyzLen * floatSize; /* Size in bytes */
    final int vertLen = 4; /* Number of vertices */
    final int coordPerVert = xyzLen + uvLen; /* Number of components */
    final int vertStride = coordPerVert * floatSize; /* Vertex size in bytes */
    final float[] drawingPlaneCoords = {
            /* X      Y     Z     U     V */
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, /* vertex 0 bottom left */
            1.0f,  -1.0f, 0.0f, 1.0f, 1.0f, /* vertex 1 bottom right */
            -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, /* vertex 2 top left */
            1.0f,   1.0f, 0.0f, 1.0f, 0.0f  /* vertex 3 top right */
    };

    private void init() {
        final int drawingPlaneCoordsBufferSize = vertStride * vertLen;
        final FloatBuffer drawingPlaneCoordsBuffer = ByteBuffer.allocateDirect(drawingPlaneCoordsBufferSize)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(drawingPlaneCoords);
        drawingPlaneCoordsBuffer.position(0);

        mVBO = new int[1];
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, drawingPlaneCoordsBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mTexture = new int[1];
        GLES20.glGenTextures(1, mTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        try {
            mProgram = new GLShaderProgram(vs, fs);
            mUniformTexture = mProgram.getUniformLocation("uTexture");
            mUniformMatrix = mProgram.getUniformLocation("uMatrix");
            mAttributePosition = mProgram.getAttributeLocation("aPosition");
            mAttributeTextureCoord = mProgram.getAttributeLocation("aTextureCoord");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drawRGBAImage(byte[] imageData, int width, int height) {
        mImageData = imageData;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewportWidth = width;
        mViewportHeight = height;
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!isInit) {
            init();
            isInit = true;
        }
        if (mImageData == null) {
            return;
        }
        final byte[] imageData = mImageData;
        final int width = mWidth;
        final int height = mHeight;

        float viewportRatioY = ((float)mViewportWidth) / ((float)mViewportHeight);
        float imageRatioY = ((float)width) / ((float)height);

        float yScale = viewportRatioY / imageRatioY;
        float xScale = 1.0f;
        if (yScale > 1.0f) {
            xScale = 1.0f / yScale;
            yScale = 1.0f;
        }
        final float[] mat3 = {
            xScale, 0.0f, 0.0f,
            0.0f, yScale, 0.0f,
            0.0f, 0.0f, 1.0f
        };

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mProgram.use();

        // Vertex Shader Buffers
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
        GLES20.glVertexAttribPointer(mAttributePosition, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(mAttributeTextureCoord, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(mAttributePosition);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoord);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imageData));

        // Uniforms
        mProgram.setUniformTexture(mUniformTexture, 0);
        mProgram.setUniformMat3(mUniformMatrix, mat3);

        // Drawing
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        // Clearing
        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoord);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mProgram.unuse();
    }
}
