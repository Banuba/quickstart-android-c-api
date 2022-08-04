package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class GLRGBARenderer extends GLRenderer {
    public GLRGBARenderer() {
        super(1);
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
            "uniform sampler2D uTexture;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 outFragColor;\n" +
            "void main() {\n" +
                "outFragColor = vec4(texture(uTexture, vTexCoord).xyz, 1.0f);\n" +
            "}\n";
    }

    public void initUniforms() throws Exception {
        mUniformTextures = new int[mTexturesCount];
        mUniformTextures[0] = mShaderProgram.getUniformLocation("uTexture");
        mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
    }

    void updateTextures() {
        mBuffers[0] = ByteBuffer.wrap(mImageDataPlanes.get(0));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mImageWidth, mImageHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBuffers[0]);
    }
}
