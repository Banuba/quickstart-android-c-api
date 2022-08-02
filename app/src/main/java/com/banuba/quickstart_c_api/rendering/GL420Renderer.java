package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class GL420Renderer extends GLRenderer {
    public GL420Renderer() {
        super(3);
        VERTEX_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "layout (location = 0) in vec3 aPosition;\n" +
            "layout (location = 1) in vec2 aTextureCoord;\n" +
            "uniform mat4 uMatrix;\n" +
            "out vec2 vTexCoord;\n" +
            "void main() {\n" +
                "gl_Position = vec4(aPosition, 1.0f) * uMatrix;\n" +
                "vTexCoord = aTextureCoord;\n" +
            "}\n";

        FRAGMENT_SHADER_PROGRAM =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform sampler2D uTextureY;\n" +
            "uniform sampler2D uTextureU;\n" +
            "uniform sampler2D uTextureV;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 outFragColor;\n" +
            "void main() {\n" +
                "float y = texture(uTextureY, vTexCoord).x;\n" +
                "float u = texture(uTextureU, vTexCoord).x - 0.5;\n" +
                "float v = texture(uTextureV, vTexCoord).x - 0.5;\n" +
                "float r = y + 1.402 * v;\n" +
                " float g = y - 0.344 * u - 0.714 * v;\n" +
                " float b = y + 1.772 * u;\n" +
                "outFragColor = vec4(r, g, b, 1.0f);\n" +
            "}\n";
    }

    public void initUniforms() throws Exception {
        mUniformTextures = new int[mTexturesCount];
        mUniformTextures[0] = mShaderProgram.getUniformLocation("uTextureY");
        mUniformTextures[1] = mShaderProgram.getUniformLocation("uTextureU");
        mUniformTextures[2] = mShaderProgram.getUniformLocation("uTextureV");
        mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
    }

    void updateTextures() {
        for(int i = 0; i < mTexturesCount; ++i) {
            mBuffers[i] = ByteBuffer.wrap(mImageDataPlanes.get(i));
            int width = (i == 0) ? mImageWidth : mImageWidth / 2;
            int height = (i == 0) ? mImageHeight : mImageHeight / 2;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mBuffers[i]);
        }
    }
}
