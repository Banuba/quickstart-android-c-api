package com.banuba.quickstart_c_api;

import java.nio.ByteBuffer;

public class Image {

    Image() {}

    Image(byte[] image0, byte[] image1, byte[] image2, int w, int h) {
        if(image0 != null) {
            mImageZero =  ByteBuffer.wrap(image0);
        }
        if(image1 != null) {
            mImageFirst =  ByteBuffer.wrap(image1);
        }
        if(image2 != null) {
            mImageSecond =  ByteBuffer.wrap(image2);
        }

        mWidth = w;
        mHeight = h;
    }

    public ByteBuffer mImageZero;
    public ByteBuffer mImageFirst;
    public ByteBuffer mImageSecond;

    public int mWidth;
    public int mHeight;
    public int rowStrideZero;
    public int rowStrideFirst;
    public int rowStrideSecond;
    public int pixelStrideZero;
    public int pixelStrideFirst;
    public int pixelStrideSecond;
    public int pixelFormat;
}
