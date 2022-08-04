package com.banuba.quickstart_c_api.rendering;

import android.opengl.GLES20;
import java.nio.ByteBuffer;

public class GLYUVNVRenderer extends GLRenderer {

    public GLYUVNVRenderer() {
        super(2);
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
            "uniform sampler2D yTexture;\n" +
            "uniform sampler2D uvTexture;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 FragColor; \n" +
            "void main() {\n" +
                "float y = texture(yTexture, vTexCoord).r;\n" +
                "float u = texture(uvTexture, vTexCoord).r - 0.5;\n" +
                "float v = texture(uvTexture, vTexCoord).a - 0.5;\n" +
                "float r = y + 1.13983*v;\n" +
                "float g = y - 0.39465*u - 0.58060*v;\n" +
                "float b = y + 2.03211*u;\n" +
                "FragColor = vec4(r, g, b, 1.0); \n" +
            "}\n";
    }

    public void initUniforms() throws Exception {
        mUniformTextures = new int[mTexturesCount];
        mUniformTextures[0] = mShaderProgram.getUniformLocation("yTexture");
        mUniformTextures[1] = mShaderProgram.getUniformLocation("uvTexture");
        mUniformMatrix = mShaderProgram.getUniformLocation("uMatrix");
    }

    void updateTextures() {
        for(int i = 0; i < mTexturesCount; ++i) {
            mBuffers[i] = ByteBuffer.wrap(mImageDataPlanes.get(i));
            int width = (i == 0) ? mImageWidth : mImageWidth / 2;
            int height = (i == 0) ? mImageHeight : mImageHeight / 2;
            int format = (i == 0) ?GLES20.GL_LUMINANCE: GLES20.GL_LUMINANCE_ALPHA;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format,
                    width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, mBuffers[i]);
        }
    }
}
