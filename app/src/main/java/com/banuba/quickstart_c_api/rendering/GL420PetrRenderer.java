package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GL420PetrRenderer implements GLSurfaceView.Renderer {
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
                    "uniform sampler2D uTextureY;\n" +
                    "uniform sampler2D uTextureU;\n" +
                    "uniform sampler2D uTextureV;\n" +
                    "in vec2 vTexCoord;\n" +
                    "out vec4 outFragColor;\n" +
                    "void main() {\n" +
                        "float y = texture(uTextureY, vTexCoord).x;\n" +
                        "float u = texture(uTextureV, vTexCoord).x - 0.5;\n" +
                        "float v = texture(uTextureU, vTexCoord).x - 0.5;\n" +
                    "   float r = y + 1.402 * v;\n" +
                    "   float g = y - 0.344 * u - 0.714 * v;\n" +
                    "   float b = y + 1.772 * u;\n" +
                    "  outFragColor = vec4(r, g, b, 1.0f);\n" +
                    "}\n";

    /* input RGBA image to draw */
    private List<byte[]> mImageData = null;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    /* variables for working with OpenGL */
    private boolean mIsCreated = false;
    private GLShaderProgram mShaderProgram = null;
    private int mViewportWidth;
    private int mViewportHeight;
    private int mUniformTextureY;
    private int mUniformTextureU;
    private int mUniformTextureV;

    private int mUniformMatrix;
    private int[] mVBO;
    private int[] mVAO;
    private int[] mTexture;

    final int vertLen = 4; /* Number of vertices */

    /* initialize of OpenGL drawing */
    private void create() {
        if (mIsCreated) {
            return;
        }
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

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        mVAO = new int[1];
        GLES30.glGenVertexArrays(mVAO.length, mVAO, 0);
        GLES30.glBindVertexArray(mVAO[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVAO[0]);
        mVBO = new int[1];
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, FloatBuffer.wrap(drawingPlaneCoords), GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(0, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(1, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);

        mTexture = new int[3];
        GLES20.glGenTextures(3, mTexture, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[1]);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[2]);
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);



        try {
            mShaderProgram = new GLShaderProgram(VERTEX_SHADER_PROGRAM, FRAGMENT_SHADER_PROGRAM);
            mUniformTextureY = mShaderProgram.getUniformLocation("uTextureY");
            mUniformTextureU = mShaderProgram.getUniformLocation("uTextureU");
            mUniformTextureV = mShaderProgram.getUniformLocation("uTextureV");
            mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsCreated = true;
    }

    /* destructor */
    private void destroy() {
        if (mIsCreated) {
            mIsCreated = false;
            GLES20.glDeleteBuffers(1, mVBO, 0);
            GLES30.glDeleteVertexArrays(1, mVAO, 0);
            GLES20.glDeleteTextures(1, mTexture, 0);
            mShaderProgram = null;
        }
    }

    protected void finalize() {
        /* Potential issue. The destructor must be called from the thread where there is a render context. */
        destroy();
    }

    /* push mage to draw */
    public void drawRGBAImage(List<byte[]> planes, int width, int height) {
        mImageData = planes;
        mImageWidth = width;
        mImageHeight = height;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        create();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewportWidth = width;
        mViewportHeight = height;
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        final List<byte[]> imageData = mImageData;
        final int imageWidth = mImageWidth;
        final int imageHeight = mImageHeight;
        if (!mIsCreated || imageData == null) {
            return;
        }

        /* scaling matrix */
        float viewportRatio = ((float)mViewportWidth) / ((float)mViewportHeight);
        float imageRatio = ((float)imageWidth) / ((float)imageHeight);
        float xScale = imageRatio < viewportRatio ? imageRatio / viewportRatio : 1.0f;
        float yScale = viewportRatio < imageRatio ? viewportRatio / imageRatio : 1.0f;
        final float[] mat4 = {
                xScale, 0.0f, 0.0f, 0.0f,
                0.0f, yScale, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        };

        /* clear background */
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /* bind vertex array */
        mShaderProgram.use();
        GLES30.glBindVertexArray(mVAO[0]);

        /* update texture */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                imageWidth, imageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imageData.get(0)));

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[1]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                imageWidth/2, imageHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imageData.get(1)));

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[2]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                imageWidth/2, imageHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imageData.get(2)));


        /* set uniforms */
        mShaderProgram.setUniformTexture(mUniformTextureY, 0);
        mShaderProgram.setUniformTexture(mUniformTextureU, 1);
        mShaderProgram.setUniformTexture(mUniformTextureV, 2);
        mShaderProgram.setUniformMat4(mUniformMatrix, mat4);

        /* draw */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        /* clear */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES30.glBindVertexArray(0);
        mShaderProgram.unuse();
    }
}
