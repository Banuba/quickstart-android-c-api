package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    public boolean mIsCreated = false;
    List<byte[]>  mImageDataPlanes = new ArrayList<>();
    public int mImageWidth = 0;
    public int mImageHeight = 0;
    public int[] mVBO;
    public int[] mVAO;
    public int[] mTextures;
    public GLShaderProgram mShaderProgram = null;
    public int mViewportWidth;
    public int mViewportHeight;
    public final float[] mMat4 = {
            0, 0.0f, 0.0f, 0.0f,
            0.0f, 0, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    final int vertLen = 4; /* Number of vertices */

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

    }


    public void scaleMatrix() {

        float viewportRatio = ((float)mViewportWidth) / ((float)mViewportHeight);
        float imageRatio = ((float)mImageWidth) / ((float)mImageHeight);
        float xScale = imageRatio < viewportRatio ? imageRatio / viewportRatio : 1.0f;
        float yScale = viewportRatio < imageRatio ? viewportRatio / imageRatio : 1.0f;
        mMat4[0] = xScale;
        mMat4[5] = yScale;
    }

    public void drawImage(List<byte[]> imageDataPlanes, int width, int height) {
        mImageDataPlanes = imageDataPlanes;
        mImageWidth = width;
        mImageHeight = height;
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

    public static final int FLOAT_SIZE = 4;

    private static FloatBuffer createFloatBuffer(@NonNull float[] coords) {
        final ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * FLOAT_SIZE);
        bb.order(ByteOrder.nativeOrder());
        final FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.rewind();
        return fb;
    }

    public static void loadBufferData(int bufferId, @NonNull float[] array) {
        final FloatBuffer floatBuffer = createFloatBuffer(array);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                array.length * FLOAT_SIZE,
                floatBuffer,
                GLES20.GL_STATIC_DRAW
        );
    }

}
