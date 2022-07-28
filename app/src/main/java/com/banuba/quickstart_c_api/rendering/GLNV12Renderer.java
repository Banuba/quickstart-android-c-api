package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLNV12Renderer extends GLRenderer {
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
                        "r = y + 1.13983*v;\n" +
                        "g = y - 0.39465*u - 0.58060*v;\n" +
                        "b = y + 2.03211*u;\n" +
                        "FragColor = vec4(r, g, b, 1.0); \n" +
                    "} \n";

    public static final int FLOAT_SIZE = 4;

    private int mUniformTexture0;
    private int mUniformTexture1;
    private int mUniformMatrix;

    private ByteBuffer mBuffer0 = null;
    private ByteBuffer mBuffer1 = null;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (mIsCreated) {
            return;
        }

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        makeVO();
        makeTextures();

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

    public void makeVO() {
        mVAO = new int[1];
        GLES30.glGenVertexArrays(mVAO.length, mVAO, 0);
        GLES30.glBindVertexArray(mVAO[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVAO[0]);

        makeVBO();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);
    }

    public void makeVBO() {
        final float[] rectangleVertex = new float[] {
                -1f, -1f, 0.0f, /* 0 bottom left */
                1f, -1f, 0.0f, /* 1 bottom right */
                -1f,  1f, 0.0f, /* 2 top left */
                1f,  1f, 0.0f, /* 3 top right */
        };

        final float[] rectangleTextureUv = {
                0.0f, 0.0f, /* 0 bottom left */
                1.0f, 0.0f, /* 1 bottom right */
                0.0f, 1.0f, /* 2 top left */
                1.0f, 1.0f  /* 3 top right */
        };

        final float[] rectangleTextureUvSwap = {
                0.0f, 1.0f, /* 0 bottom left */
                1.0f, 1.0f, /* 1 bottom right */
                0.0f, 0.0f, /* 2 top left */
                1.0f, 0.0f  /* 3 top right */
        };

        mVBO = new int[3];
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        loadBufferData(mVBO[0], rectangleVertex);
        loadBufferData(mVBO[1], rectangleTextureUv);
        loadBufferData(mVBO[2], rectangleTextureUvSwap);

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
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, FloatBuffer.wrap(drawingPlaneCoords), GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(0, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(1, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
    }

    public void makeTextures() {
        mTextures = new int[2];
        GLES20.glGenTextures(mTextures.length, mTextures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
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

    private void updateTextures() {
        mBuffer0 = ByteBuffer.wrap(mImageDataPlanes.get(0));
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                mImageWidth, mImageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mBuffer0);

        mBuffer1 = ByteBuffer.wrap(mImageDataPlanes.get(1));
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                mImageWidth/2, mImageHeight/2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, mBuffer1);
    }

    private static FloatBuffer createFloatBuffer(@NonNull float[] coords) {
        final ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * FLOAT_SIZE);
        bb.order(ByteOrder.nativeOrder());
        final FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.rewind();
        return fb;
    }

    private static void loadBufferData(int bufferId, @NonNull float[] array) {
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
