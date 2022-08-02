package com.banuba.quickstart_c_api.rendering;

import static javax.microedition.khronos.opengles.GL10.GL_BLEND;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GL420Renderer extends GLRenderer {
//    private static final String VERTEX_SHADER_PROGRAM =
//            "precision mediump float;\n" +
//            "uniform mat4 uTextureMatrix;\n" +
//            "uniform mat4 uVertexMatrix;\n" +
//            "attribute vec4 a_position;\n" +
//            "attribute vec2 a_texCoord;\n" +
//            "varying vec2 v_texCoord;\n" +
//            "void main() {\n" +
//            "  gl_Position = uVertexMatrix * a_position;\n" +
//            "  vec4 texCoord = vec4(a_texCoord, 0.0, 1.0);\n" +
//            "  v_texCoord = (uTextureMatrix * texCoord).xy;\n" +
//            "}\n";
//
//    private static final String FRAGMENT_SHADER_PROGRAM =
//            "precision mediump float;\n" +
//            "uniform sampler2D s_baseMapY;\n" +
//            "uniform sampler2D s_baseMapCh1;\n" +
//            "uniform sampler2D s_baseMapCh2;\n" +
//            "uniform vec4 v_cvtR;\n" +
//            "uniform vec4 v_cvtG;\n" +
//            "uniform vec4 v_cvtB;\n" +
//            "varying vec2 v_texCoord;\n" +
//            "void main() {\n" +
//            "  float y = texture2D(s_baseMapY, v_texCoord).r;\n" +
//            "  float tu = texture2D(s_baseMapCh1, v_texCoord).r;\n" +
//            "  float tv = texture2D(s_baseMapCh2, v_texCoord).r;\n" +
//            "  vec3 yuv = vec3(y, tu, tv);\n" +
//            "  vec3 rgb = vec3(\n" +
//            "    dot(yuv, v_cvtR.xyz) + v_cvtR.w,\n" +
//            "    dot(yuv, v_cvtG.xyz) + v_cvtG.w,\n" +
//            "    dot(yuv, v_cvtB.xyz) + v_cvtB.w);\n" +
//            "  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
//            "}\n";

 private static final String VERTEX_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "layout (location = 0) in vec3 a_position;\n" +
            "layout (location = 1) in vec2 a_texCoord;\n" +
            "uniform mat4 uMatrix;\n" +
            "out vec2 v_texCoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(a_position, 1.0f) * uMatrix;\n" +
            "  v_texCoord = a_texCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_texCoord;\n" +
            "uniform sampler2D s_baseMapY;\n" +
            "uniform sampler2D s_baseMapCh1;\n" +
            "uniform sampler2D s_baseMapCh2;\n" +
            "uniform vec4 v_cvtR;\n" +
            "uniform vec4 v_cvtG;\n" +
            "uniform vec4 v_cvtB;\n" +
            "out vec4 FragColor; \n" +
            "void main() {\n" +
            "  float y = texture(s_baseMapY, v_texCoord).r;\n" +
            "  float tu = texture(s_baseMapCh1, v_texCoord).r - 0.5;\n" +
            "  float tv = texture(s_baseMapCh2, v_texCoord).r - 0.5;\n" +
             "   float r = y + 1.402 * tv;\n" +
             "   float g = y - 0.344 * tu - 0.714 * tv;\n" +
             "   float b = y + 1.772 * tu;\n" +
            "  FragColor = vec4(r, g, b, 1.0);\n" +
            "}\n";


    /* variables for working with OpenGL */

    private int mUniformTexture0;
    private int mUniformTexture1;
    private int mUniformTexture2;
    private int mUniformMatrix;
    private ByteBuffer mBuffer0 = null;
    private ByteBuffer mBuffer1 = null;
    private ByteBuffer mBuffer2 = null;
    final int vertLen = 4; /* Number of vertices */
    private boolean mIsDebug = true;

    private static final float[] RECTANGLE_VERTEX = new float[] {
            -1f, -1f, 0.0f, /* 0 bottom left */
            1f, -1f, 0.0f, /* 1 bottom right */
            -1f,  1f, 0.0f, /* 2 top left */
            1f,  1f, 0.0f, /* 3 top right */
    };


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

    private  int mUniformSamplerY;
    private  int mUniformSamplerCh1;
    private  int mUniformSamplerCh2;
    private  int mUniformCvtR;
    private  int mUniformCvtG;
    private  int mUniformCvtB;

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
        mVBO = new int[3];
        GLES20.glGenBuffers(mVBO.length, mVBO, 0);
        loadBufferData(mVBO[0], RECTANGLE_VERTEX);
        loadBufferData(mVBO[1], RECTANGLE_TEXTURE_UV);
        loadBufferData(mVBO[2], RECTANGLE_TEXTURE_UV_SWAP);

        final int floatSize = Float.SIZE / 8; /* Size of Float in bytes */
        final int xyzLen = 3; /* Number of components */
        final int xyzOffset = 0; /* Size in bytes */
        final int uvLen = 2;  /* Number of components */
        final int uvOffset = xyzLen * floatSize; /* Size in bytes */
        final int uvOffset1 = (xyzLen + uvLen) * floatSize; /* Size in bytes */
        final int coordPerVert = xyzLen + uvLen; /* Number of components */
        final int vertStride = coordPerVert * floatSize; /* Vertex size in bytes */
        final int drawingPlaneCoordsBufferSize = vertStride * vertLen;
        final float[] drawingPlaneCoords = {
                /* X      Y     Z     U     V */
                -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, /* vertex 0 bottom left */
                1.0f, -1.0f, 0.0f, 1.0f, 0.0f, /* vertex 1 bottom right */
                -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, /* vertex 2 top left */
                1.0f, 1.0f, 0.0f, 1.0f, 1.0f  /* vertex 3 top right */
        };
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, drawingPlaneCoordsBufferSize, FloatBuffer.wrap(drawingPlaneCoords), GLES20.GL_STATIC_DRAW);
        GLES20.glVertexAttribPointer(0, xyzLen, GLES20.GL_FLOAT, false, vertStride, xyzOffset);
        GLES20.glVertexAttribPointer(1, uvLen, GLES20.GL_FLOAT, false, vertStride, uvOffset);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (mIsCreated) {
            return;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        makeVO();


        makeTextures();

        try {
            String s = VERTEX_SHADER_PROGRAM;
            String t = FRAGMENT_SHADER_PROGRAM;
            mShaderProgram = new GLShaderProgram(s, t);

            mUniformSamplerY = mShaderProgram.getUniformLocation("s_baseMapY");
            mUniformSamplerCh1 = mShaderProgram.getUniformLocation("s_baseMapCh1");
            mUniformSamplerCh2 = mShaderProgram.getUniformLocation( "s_baseMapCh2");
            mUniformCvtR = mShaderProgram.getUniformLocation("v_cvtR");
            mUniformCvtG = mShaderProgram.getUniformLocation( "v_cvtG");
            mUniformCvtB = mShaderProgram.getUniformLocation( "v_cvtB");

            mAttributePosition = mShaderProgram.getUniformLocation("a_position");
            mAttributeTextureCoord = mShaderProgram.getUniformLocation("a_texCoord");
//            mUniformVertexMatrix = mShaderProgram.getUniformLocation("uVertexMatrix");
//            mUniformTextureMatrix = mShaderProgram.getUniformLocation("uTextureMatrix");

            mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");

        } catch (Exception e) {
            e.printStackTrace();
        }

        mIsCreated = true;
    }

    public void makeTextures() {
        mTextures = new int[3];
        GLES20.glGenTextures(mTextures.length, mTextures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[2]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDrawFrame(GL10 gl) {

        if (!mIsCreated || mImageDataPlanes.isEmpty()) {
            return;
        }

        /* scaling matrix */
        scaleMatrix();

        /* clear background */
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mShaderProgram.use();
        GLES30.glBindVertexArray(mVAO[0]);

        updateTextures();

//        setup();

        /* set uniforms */
        mShaderProgram.setUniformTexture(mUniformSamplerY, 0);
        mShaderProgram.setUniformTexture(mUniformSamplerCh1, 1);
        mShaderProgram.setUniformTexture(mUniformSamplerCh2, 2);
        mShaderProgram.setUniformMat4(mUniformMatrix, mMat4);

        /* draw */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertLen);

        /* clear */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES30.glBindVertexArray(0);
        mShaderProgram.unuse();

    }
    protected final float[] mIdentity = new float[16];

    private static final int offsetToRedColorCoeffs = 0;
    private static final int offsetToGreenColorCoeffs = 4;
    private static final int offsetToBlueColorCoeffs = 8;
    public static final int COORDS_PER_VERTEX = 3;
    private final int mVertexCount = RECTANGLE_VERTEX.length / COORDS_PER_VERTEX;
    private  int mAttributePosition;
    private  int mAttributeTextureCoord;
    private int mUniformVertexMatrix;
    private  int mUniformTextureMatrix;

    public static final int COORDS_UV_PER_TEXTURE = 1;

    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;
    public static final int TEXTURE_STRIDE = COORDS_UV_PER_TEXTURE * FLOAT_SIZE;
    private final float[] mMatrixScreen = new float[16];


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateTextures() {

        mBuffer0 = ByteBuffer.wrap(mImageDataPlanes.get(0));
        mBuffer1 = ByteBuffer.wrap(mImageDataPlanes.get(1));
        mBuffer2 = ByteBuffer.wrap(mImageDataPlanes.get(2));
        if(mIsDebug) {
            ByteBuffer a0 = clone(mBuffer0);
            ByteBuffer a1 = clone(mBuffer2);
            ByteBuffer a2 = clone(mBuffer1);
            if(mBuffer0 != null) {
                saveImageDetailed(a0, a1, a2);
            } else {
                Log.e("123", "mBuffer0 == null");
            }
        }


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                mImageWidth, mImageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                mBuffer0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[1]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                mImageWidth/2, mImageHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                mBuffer1);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[2]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                mImageWidth/2, mImageHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                mBuffer2);
    }

    int i = 0;
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveImageDetailed(ByteBuffer buffer0, ByteBuffer buffer1, ByteBuffer buffer2) {
        int w = mImageWidth;
        int h = mImageHeight;
        int rowStride = mImageWidth;
        final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        final IntBuffer pixels =
                ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        for (int height = 0; height < h; height++) {
            buffer0.position(rowStride * height);
            for (int x = 0; x < w; x++) {
                int position = (height / 2) * rowStride + x / 2;
                buffer1.position(position);
                buffer2.position(position);
                int yy = buffer0.get()& 0xff;
                int uu = buffer2.get()& 0xff;
                int vv = buffer1.get()& 0xff;
                final float y = (float) (yy/255.);
                final float u = (float) ((uu - 128)/255.);
                final float v = (float) ((vv - 128)/255.);

                float r = (float) (y + 1.402 * v);
                float g = (float) (y - 0.344 * u - 0.714 * v);
                float b = (float) (y + 1.772 * u);

                pixels.position(height * w + x);
                pixels.put(Color.argb(1, r, g, b));
            }

            pixels.rewind();
            bitmap.copyPixelsFromBuffer(pixels);
        }

        final File file = new File("/storage/emulated/0/Download/123",  i + ".png");
        ++i;

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("123", " Save E = " + e.getMessage());
        }
        bitmap.recycle();
    }

    public static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();//copy from the beginning
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }


    private static void calculateCameraMatrixFlip(float[] matrix, int angle, int flip) {
        final float[] rotate = new float[16];
        final float[] transPos = new float[16];
        final float[] transNeg = new float[16];
        final float[] temp = new float[16];
        final float[] temp2 = new float[16];
        final float[] scale = new float[16];

        final int FLIP_NONE = 0;
        final int FLIP_Y = 1;
        final int FLIP_X = 2;
        final int FLIP_XY = 3;

        Matrix.setIdentityM(scale, 0);
        if (flip == FLIP_X) {
            Matrix.scaleM(scale, 0, -1.0f, 1.0f, 1.0f);
        } else if (flip == FLIP_Y) {
            Matrix.scaleM(scale, 0, 1.0f, -1.0f, 1.0f);
        } else if (flip == FLIP_XY) {
            Matrix.scaleM(scale, 0, -1.0f, -1.0f, 1.0f);
        }

        Matrix.setIdentityM(transPos, 0);
        Matrix.setIdentityM(transNeg, 0);
        Matrix.setIdentityM(rotate, 0);

        Matrix.translateM(transPos, 0, 0.5f, 0.5f, 0);
        Matrix.translateM(transNeg, 0, -0.5f, -0.5f, 0);

        Matrix.setRotateM(rotate, 0, angle, 0, 0, 1);

        Matrix.multiplyMM(temp, 0, transPos, 0, rotate, 0);
        Matrix.multiplyMM(temp2, 0, temp, 0, scale, 0);
        Matrix.multiplyMM(matrix, 0, temp2, 0, transNeg, 0);
    }


    private void setup() {
        // Vertex Shader Buffers

//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
//        GLES20.glVertexAttribPointer(mAttributePosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 0);
//        GLES20.glEnableVertexAttribArray(mAttributePosition);
//
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[1]);
//        GLES20.glVertexAttribPointer(mAttributeTextureCoord, COORDS_UV_PER_TEXTURE, GLES20.GL_FLOAT, false, TEXTURE_STRIDE, 0);
//        GLES20.glEnableVertexAttribArray(mAttributeTextureCoord);

        // Vertex Shader - Uniforms
//        mIdentity, matrix = vertexMatrix, textureMatrix

        // check
//        Matrix.setIdentityM(mIdentity, 0);
//        GLES20.glUniformMatrix4fv(mUniformVertexMatrix, 1, false, mIdentity, 0);
//        calculateCameraMatrixFlip(mMatrixScreen, 0, 0);
//        GLES20.glUniformMatrix4fv(mUniformTextureMatrix, 1, false, mMatrixScreen, 0);
    }
}
