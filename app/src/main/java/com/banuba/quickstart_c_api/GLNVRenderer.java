package com.banuba.quickstart_c_api;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLNVRenderer implements GLSurfaceView.Renderer {
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
                    "uniform sampler2D yTexture;\n" +
                    "uniform sampler2D uvTexture;\n" +
                    "in vec2 vTexCoord;\n" +
                    "out vec4 FragColor; \n" +
                    "void main() \n" +
                    "{ \n" +
                        "float r, g, b, y, u, v; \n" +
                        "y = texture(yTexture, vTexCoord).r; \n" +
                        "u = texture(uvTexture, vTexCoord).r - 0.5; \n" +
                        "v = texture(uvTexture, vTexCoord).a - 0.5; \n" +
                        "float Umax = 0.436; \n" +
                        "float Vmax = 0.615; \n" +
                        "float Wr = 0.299; \n" +
                        "float Wb = 0.114; \n" +
                        "float Wg = 1. - Wr - Wb; \n" +
                        "r = y + 1.13983*v;\n" +
                        "g = y - 0.39465*u - 0.58060*v;\n" +
                        "b = y + 2.03211*u;\n" +
                        "FragColor = vec4(r, g, b, 1.0); \n" +
                    "} \n";

    /* input RGBA image to draw */
    private byte[] mImageData0 = null;
    private byte[] mImageData1 = null;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    /* variables for working with OpenGL */
    private boolean mIsCreated = false;
    private GLShaderProgram mShaderProgram = null;
    private int mViewportWidth;
    private int mViewportHeight;
    private int mUniformTexture0;
    private int mUniformTexture1;
    private int mUniformMatrix;
    private int[] mVBO;
    private int[] mVAO;
    private final int[] mTextures = new int[3];
    private final float[] mMat4 = {
            0, 0.0f, 0.0f, 0.0f,
            0.0f, 0, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private ByteBuffer mBuffer0 = null;
    private ByteBuffer mBuffer1 = null;
    final int vertLen = 4; /* Number of vertices */

    private static final float[] RECTANGLE_VERTEX = new float[] {
            -1f, -1f, 0.0f, /* 0 bottom left */
            1f, -1f, 0.0f, /* 1 bottom right */
            -1f,  1f, 0.0f, /* 2 top left */
            1f,  1f, 0.0f, /* 3 top right */
    };
    public static final int FLOAT_SIZE = 4;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int COORDS_UV_PER_TEXTURE = 2;

    private static final float[] RECTANGLE_TEXTURE_UV = {
            0.0f, 0.0f, /* 0 bottom left */
            1.0f, 0.0f, /* 1 bottom right */
            0.0f, 1.0f, /* 2 top left */
            1.0f, 1.0f  /* 3 top right */
    };

    private static final float[] RECTANGLE_TEXTURE_UV_SWAP = {
            0.0f, 1.0f, /* 0 bottom left */
            1.0f, 1.0f, /* 1 bottom right */
            0.0f, 0.0f, /* 2 top left */
            1.0f, 0.0f  /* 3 top right */
    };

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
                1.0f, -1.0f, 0.0f, 1.0f, 1.0f, /* vertex 1 bottom right */
                -1.0f, 1.0f, 0.0f, 0.0f, 0.0f, /* vertex 2 top left */
                1.0f, 1.0f, 0.0f, 1.0f, 0.0f  /* vertex 3 top right */
        };

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        mVAO = new int[1];
        GLES30.glGenVertexArrays(mVAO.length, mVAO, 0);
        GLES30.glBindVertexArray(mVAO[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVAO[0]);

        mVBO = new int[3];
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        loadBufferData(mVBO[0], RECTANGLE_VERTEX);
        loadBufferData(mVBO[1], RECTANGLE_TEXTURE_UV);
        loadBufferData(mVBO[2], RECTANGLE_TEXTURE_UV_SWAP);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, FloatBuffer.wrap(drawingPlaneCoords), GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(0, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(1, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);

        makeTextures(mTextures);

        try {
            mShaderProgram = new GLShaderProgram(VERTEX_SHADER_PROGRAM, FRAGMENT_SHADER_PROGRAM);
            mUniformTexture0 = mShaderProgram.getUniformLocation("yTexture");
            mUniformTexture1 = mShaderProgram.getUniformLocation("uvTexture");
            mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsCreated = true;

    }

    public static void makeTextures(int[] textures) {

        GLES20.glGenTextures(textures.length, textures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /* destructor */
    private void destroy() {
        if (mIsCreated) {
            mIsCreated = false;
            GLES20.glDeleteBuffers(1, mVBO, 0);
            GLES30.glDeleteVertexArrays(1, mVAO, 0);
            GLES20.glDeleteTextures(1, mTextures, 0);
            mShaderProgram = null;
        }
    }

    protected void finalize() {
        /* Potential issue. The destructor must be called from the thread where there is a render context. */
        destroy();
    }

    /* push image to draw */
    public void drawImage(byte[] imageData0, byte[] imageData1, int width, int height) {
        mImageData0 = imageData0;
        mImageData1 = imageData1;
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
        final int imageWidth = mImageWidth;
        final int imageHeight = mImageHeight;
        if (!mIsCreated || mImageData0 == null || mImageData1 == null) {
            return;
        }

        /* scaling matrix */
        float viewportRatio = ((float) mViewportWidth) / ((float) mViewportHeight);
        float imageRatio = ((float) imageWidth) / ((float) imageHeight);
        float xScale = imageRatio < viewportRatio ? imageRatio / viewportRatio : 1.0f;
        float yScale = viewportRatio < imageRatio ? viewportRatio / imageRatio : 1.0f;
        mMat4[0] = xScale;
        mMat4[5] = yScale;

        /* clear background */
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /* bind vertex array */
        mShaderProgram.use();
        GLES30.glBindVertexArray(mVAO[0]);

        /* update texture */
        loadProcessResultYUV2Textures();

        /* set uniforms */
        mShaderProgram.setUniformTexture(mUniformTexture0, 0);
        mShaderProgram.setUniformTexture(mUniformTexture1, 1);
        mShaderProgram.setUniformMat4(mUniformMatrix, mMat4);

        /* draw */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        /* clear */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 1);
        GLES30.glBindVertexArray(0);
        mShaderProgram.unuse();
    }

    private void loadProcessResultYUV2Textures() {

        mBuffer0 = ByteBuffer.wrap(mImageData0);
        mBuffer1 = ByteBuffer.wrap(mImageData1);
        final int imageWidth = mImageWidth;
        final int imageHeight = mImageHeight;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                imageWidth, imageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mBuffer0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                imageWidth/2, imageHeight/2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, mBuffer1);
    }
}
