package com.banuba.quickstart_c_api;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLNV12RendererConsts implements GLSurfaceView.Renderer {
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

    private static final String SHADER_FRAG_CONVERSION_CODE =
            "  vec3 rgb = vec3(\n" +
                    "    dot(yuv, v_cvtR.xyz) + v_cvtR.w,\n" +
                    "    dot(yuv, v_cvtG.xyz) + v_cvtG.w,\n" +
                    "    dot(yuv, v_cvtB.xyz) + v_cvtB.w);\n" +
                    "  gl_FragColor = vec4(rgb, 1.0);\n" +
                    "}\n";

    private static final String SHADER_VERTEX =
            "uniform mat4 uTextureMatrix;\n" +
                    "uniform mat4 uVertexMatrix;\n" +
                    "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texCoord;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uVertexMatrix * a_position;\n" +
                    "  vec4 texCoord = vec4(a_texCoord, 0.0, 1.0);\n" +
                    "  v_texCoord = (uTextureMatrix * texCoord).xy;\n" +
                    "}\n";

    // clang-format off
    private static final String SHADER_FRAG_START_3_TEXTURES =
            "precision highp float;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "uniform sampler2D s_baseMapY;\n" +
                    "uniform sampler2D s_baseMapCh1;\n" +
                    "uniform sampler2D s_baseMapCh2;\n" +
                    "uniform vec4 v_cvtR;\n" +
                    "uniform vec4 v_cvtG;\n" +
                    "uniform vec4 v_cvtB;\n" +
                    "void main() {\n" +
                    "  float y = texture2D(s_baseMapY, v_texCoord).r;\n" +
                    "  float tu = texture2D(s_baseMapCh1, v_texCoord).r;\n" +
                    "  float tv = texture2D(s_baseMapCh2, v_texCoord).r;\n";
    // clang-format on

    /* input image to draw */
    private byte[] mImageData0 = null;
    private byte[] mImageData1 = null;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    /* variables for working with OpenGL */
    private boolean mIsCreated = false;
    private GLShaderProgram mShaderProgram = null;
    private int mViewportWidth;
    private int mViewportHeight;
    private int mUniformTexture;
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
    private static final float[] MAT_CVT_FROM_BT601_VIDEO_RANGE_TO_RGB = {
            1.164383562f,  0.0000000000f,  1.5960267860f, -0.8742022179f, /* RED coeffs */
            1.164383562f, -0.3917622901f, -0.8129676472f,  0.5316678235f, /* GREEN coeffs */
            1.164383562f,  2.0172321430f,  0.0000000000f, -1.0856307890f  /* BLUE coeffs */
    };
    private int mUniformSamplerY;
    private int mUniformSamplerCh1;
    private int mUniformSamplerCh2;
    private int mUniformCvtR;
    private int mUniformCvtG;
    private int mUniformCvtB;

    public static final int GL_TEXTURE0 = 0;
    public static final int GL_TEXTURE1 = 1;
    public static final int GL_TEXTURE2 = 2;
    public static final int GL_TEXTURE3 = 3;
    public static final int GL_TEXTURE4 = 4;

    private static final int offsetToRedColorCoeffs = 0;
    private static final int offsetToGreenColorCoeffs = 4;
    private static final int offsetToBlueColorCoeffs = 8;

    private ByteBuffer mBuffer0 = null;
    private ByteBuffer mBuffer1 = null;
    final int vertLen = 4; /* Number of vertices */
    protected int mProgramHandle;
    protected final float[] mIdentity = new float[16];
    private final float[] mMatrixScreen = new float[16];
    private int mAttributePosition;
    private int mAttributeTextureCoord;
    private int mUniformVertexMatrix;
    private int mUniformTextureMatrix;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int COORDS_UV_PER_TEXTURE = 2;
    private static final float[] RECTANGLE_VERTEX = new float[] {
            -1f, -1f, 0.0f, /* 0 bottom left */
            1f, -1f, 0.0f, /* 1 bottom right */
            -1f,  1f, 0.0f, /* 2 top left */
            1f,  1f, 0.0f, /* 3 top right */
    };
    public static final int FLOAT_SIZE = 4;
    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;
    public static final int TEXTURE_STRIDE = COORDS_UV_PER_TEXTURE * FLOAT_SIZE;

    private final int mVertexCount = RECTANGLE_VERTEX.length / COORDS_PER_VERTEX;
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


    public static void makeTextures(int[] textures) {
        GLES20.glGenTextures(textures.length, textures, 0);

        for (int texture : textures) {
            if (texture == 0) {
                throw new RuntimeException("Error loading texture.");
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static void checkGlErrorNoException(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error) + " (" + GLU.gluErrorString(error) + ")";
//            Logger.e(msg);
        }
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorNoException("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
//            Logger.e(
//                    "Could not compile shader %1$d: %2$s",
//                    shaderType,
//                    GLES20.glGetShaderInfoLog(shader)
//            );
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int loadProgram(@NonNull String vertShaderSrc, @NonNull String fragShaderSrc) {
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertexShader == 0) {
            return 0;
        }

        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0) {
            return 0;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
//            Logger.e("Error linking program: %s", GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programObject;
    }

    protected String getShaderStart(boolean swapColors) {
        final String swapCode = swapColors ? "  vec3 yuv = vec3(y, tv, tu);\n" : "  vec3 yuv = vec3(y, tu, tv);\n";

        return SHADER_FRAG_START_3_TEXTURES + swapCode;
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

        //start
        final float[] drawingPlaneCoords = {
                /* X      Y     Z     U     V */
                -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, /* vertex 0 bottom left */
                1.0f,  -1.0f, 0.0f, 1.0f, 1.0f, /* vertex 1 bottom right */
                -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, /* vertex 2 top left */
                1.0f,   1.0f, 0.0f, 1.0f, 0.0f  /* vertex 3 top right */
        };
        boolean swapColors = false;
        mProgramHandle = loadProgram(SHADER_VERTEX, getShaderStart(swapColors) + SHADER_FRAG_CONVERSION_CODE);

        // Fragment Shader
        mUniformSamplerY = GLES20.glGetUniformLocation(mProgramHandle, "s_baseMapY");
        mUniformSamplerCh1 = GLES20.glGetUniformLocation(mProgramHandle, "s_baseMapCh1");
        mUniformSamplerCh2 = GLES20.glGetUniformLocation(mProgramHandle, "s_baseMapCh2");
        mUniformCvtR = GLES20.glGetUniformLocation(mProgramHandle, "v_cvtR");
        mUniformCvtG = GLES20.glGetUniformLocation(mProgramHandle, "v_cvtG");
        mUniformCvtB = GLES20.glGetUniformLocation(mProgramHandle, "v_cvtB");
        Matrix.setIdentityM(mIdentity, 0);
        // Vertex shader
        mAttributePosition = GLES20.glGetAttribLocation(mProgramHandle, "a_position");
        mAttributeTextureCoord = GLES20.glGetAttribLocation(mProgramHandle, "a_texCoord");

        mUniformVertexMatrix = GLES20.glGetUniformLocation(mProgramHandle, "uVertexMatrix");
        mUniformTextureMatrix = GLES20.glGetUniformLocation(mProgramHandle, "uTextureMatrix");


        // finish
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
            mUniformTexture = mShaderProgram.getUniformLocation("uTexture");
            mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsCreated = true;

    }

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

    /* destructor */
    private void destroy() {
        if (mIsCreated) {
            mIsCreated = false;
            GLES20.glDeleteBuffers(1, mVBO, 0);
            GLES30.glDeleteVertexArrays(1, mVAO, 0);
            GLES20.glDeleteTextures(mTextures.length, mTextures, 0);
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

    private void loadProcessResultYUV2Textures() {
        int plane_count = 2;
        int size0 = 1280;
        int size1 = 1280;
        final int imageWidth = mImageWidth;
        final int imageHeight = mImageHeight;
        for (int i = 0; i < plane_count; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
            if(i == 0) {
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, size0);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                        imageWidth, imageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mBuffer0);
            } else {
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, size1);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                        imageWidth, imageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mBuffer1);
            }
        }

        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    public void draw(boolean flipVertical, @NonNull final int[] textures, @NonNull float[] vertexMatrix,
                     @NonNull float[] textureMatrix) {
        setupDraw(flipVertical, vertexMatrix, textureMatrix);

        // Fragment Shader - Texture
        setupSampler(0, mUniformSamplerY, textures[0], false);
        setupSampler(1, mUniformSamplerCh1, textures[1], false);
        setupSampler(2, mUniformSamplerCh2, textures[2], false);

        final float[] conversionMatrix = MAT_CVT_FROM_BT601_VIDEO_RANGE_TO_RGB;
        assert conversionMatrix != null;
        GLES20.glUniform4fv(mUniformCvtR, 1, conversionMatrix, offsetToRedColorCoeffs);
        GLES20.glUniform4fv(mUniformCvtG, 1, conversionMatrix, offsetToGreenColorCoeffs);
        GLES20.glUniform4fv(mUniformCvtB, 1, conversionMatrix, offsetToBlueColorCoeffs);

        drawAndClear();
    }

    void drawAndClear() {
        // Drawing
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);

        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoord);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glUseProgram(0);
    }

    void setupDraw(boolean swap, @NonNull float[] vertexMatrix, @NonNull float[] textureMatrix) {
        GLES20.glUseProgram(mProgramHandle);

        // Vertex Shader Buffers
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO[0]);
        GLES20.glVertexAttribPointer(mAttributePosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 0);
        GLES20.glEnableVertexAttribArray(mAttributePosition);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, swap ? mVBO[2] : mVBO[1]);
        GLES20.glVertexAttribPointer(mAttributeTextureCoord, COORDS_UV_PER_TEXTURE, GLES20.GL_FLOAT, false, TEXTURE_STRIDE, 0);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoord);

        // Vertex Shader - Uniforms
        GLES20.glUniformMatrix4fv(mUniformVertexMatrix, 1, false, vertexMatrix, 0);
        GLES20.glUniformMatrix4fv(mUniformTextureMatrix, 1, false, textureMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsCreated || mImageData0 == null || mImageData1 == null) {
            return;
        }

        mBuffer0 = ByteBuffer.wrap(mImageData0);
        mBuffer1 = ByteBuffer.wrap(mImageData1);
        loadProcessResultYUV2Textures();
        draw(false, mTextures, mIdentity, mMatrixScreen);
    }

    public static void setupSampler(int samplerIndex, int location, int texture, boolean external) {
        int glTexture = GLES20.GL_TEXTURE0;
        int glUniformX = GL_TEXTURE0;

        switch (samplerIndex) {
            case 1:
                glTexture = GLES20.GL_TEXTURE1;
                glUniformX = GL_TEXTURE1;
                break;
            case 2:
                glTexture = GLES20.GL_TEXTURE2;
                glUniformX = GL_TEXTURE2;
                break;
            case 3:
                glTexture = GLES20.GL_TEXTURE3;
                glUniformX = GL_TEXTURE3;
                break;
            case 4:
                glTexture = GLES20.GL_TEXTURE4;
                glUniformX = GL_TEXTURE4;
                break;
        }

        final int target = external ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;

        GLES20.glActiveTexture(glTexture);
        GLES20.glBindTexture(target, texture);
        GLES20.glUniform1i(location, glUniformX);
    }


}
