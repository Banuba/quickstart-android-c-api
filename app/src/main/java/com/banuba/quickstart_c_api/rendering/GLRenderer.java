package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.banuba.quickstart_c_api.OffscreenEffectPlayerImage;

public class GLRenderer implements GLSurfaceView.Renderer {

    /* shaders */
    String VERTEX_SHADER_PROGRAM;
    String FRAGMENT_SHADER_PROGRAM;
    public GLShaderProgram mShaderProgram = null;

    /* textures */
    public int mTexturesCount;
    public int[] mTextures;

    /* vertex objects */
    final int vertLen = 4; /* Number of vertices */
    public int[] mVBO;
    public int[] mVAO;

    /* uniforms */
    public int mUniformTextures[];
    public int mUniformMatrix;
    public final float[] mMat4 = {
            0, 0.0f, 0.0f, 0.0f,
            0.0f, 0, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    /* for image data */
    List<byte[]>  mImageDataPlanes = new ArrayList<>();
    public int mImageWidth = 0;
    public int mImageHeight = 0;
    public ByteBuffer [] mBuffers;
    public int mViewportWidth;
    public int mViewportHeight;

    public boolean mIsCreated = false;

    GLRenderer(int texturesCount) {
        mTexturesCount = texturesCount;
    }

    /* must be redefine in a child class */
    void updateTextures() {}

    /* must be redefine in a child class */
    public void initUniforms() throws Exception {}

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (mIsCreated) {
            return;
        }
        mBuffers = new ByteBuffer[mTexturesCount];
        generateVertexObjects();
        generateTextures();

        try {
            mShaderProgram = new GLShaderProgram(VERTEX_SHADER_PROGRAM, FRAGMENT_SHADER_PROGRAM);
            initUniforms();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsCreated = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewportWidth = width;
        mViewportHeight = height;
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
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
        setUniforms();

        /* draw */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        /* clear */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES30.glBindVertexArray(0);
        mShaderProgram.unuse();
    }

    private void setUniforms() {
        for(int i = 0; i < mTexturesCount; ++i) {
            mShaderProgram.setUniformTexture(mUniformTextures[i], i);
        }
        mShaderProgram.setUniformMat4(mUniformMatrix, mMat4);
    }

    void generateTextures() {
        mTextures = new int[mTexturesCount];
        GLES20.glGenTextures(mTextures.length, mTextures, 0);

        for(int i = 0; i < mTexturesCount; ++i) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void generateVertexObjects() {
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
    }

    public void scaleMatrix() {
        float viewportRatio = ((float)mViewportWidth) / ((float)mViewportHeight);
        float imageRatio = ((float)mImageWidth) / ((float)mImageHeight);
        float xScale = imageRatio < viewportRatio ? imageRatio / viewportRatio : 1.0f;
        float yScale = viewportRatio < imageRatio ? viewportRatio / imageRatio : 1.0f;
        mMat4[0] = xScale;
        mMat4[5] = yScale;
    }

    public void drawImage(OffscreenEffectPlayerImage image) {
        mImageDataPlanes.clear();
        for(int i = 0; i < mTexturesCount; ++i) {
            ByteBuffer plane = image.getPlane(i);
            if(plane != null) {
                mImageDataPlanes.add(plane.array());
            }
        }
        mImageWidth = image.getWidth();
        mImageHeight = image.getHeight();
    }

    /* destructor */
    private void destroy() {
        if (mIsCreated) {
            mIsCreated = false;
            GLES20.glDeleteBuffers(mVBO.length, mVBO, 0);
            GLES30.glDeleteVertexArrays(mVAO.length, mVAO, 0);
            GLES20.glDeleteTextures(mTextures.length, mTextures, 0);
            mShaderProgram = null;
        }
    }

    protected void finalize() {
        /* Potential issue. The destructor must be called from the thread where there is a render context. */
        destroy();
    }
}
