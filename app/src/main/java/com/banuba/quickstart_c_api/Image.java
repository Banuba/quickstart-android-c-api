package com.banuba.quickstart_c_api;

public class Image {
    //    List<byte[]> imageDataPlanes;

    Image(byte[] image0, byte[] image1, byte[] image2, int w, int h) {
        mImage0 = image0;
        mImage1 = image1;
        mImage2 = image2;
        mWidth = w;
        mHeight = h;

    }
    public byte[] mImage0;
    public byte[] mImage1;
    public byte[] mImage2;
    public int mWidth;
    public int mHeight;
}
