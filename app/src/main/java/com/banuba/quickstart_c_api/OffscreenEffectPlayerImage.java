package com.banuba.quickstart_c_api;

import java.nio.ByteBuffer;

public class OffscreenEffectPlayerImage {
    OffscreenEffectPlayerImage() {
        mImageInfo = new ImageInfo();
    }

    public ImageInfo mImageInfo;
    public ByteBuffer mImageZero = null;
    public ByteBuffer mImageFirst = null;
    public ByteBuffer mImageSecond = null;
}

class ImageInfo {
    ImageInfo() {
        width = 0;
        height = 0;
        inputOrientation = 0;
        outputOrientation = 0;
        requireMirroring = false;
        rowStride0 = 0;
        rowStride1 = 0;
        rowStride2 = 0;
        pixelStride0 = 0;
        pixelStride1 = 0;
        pixelStride2 = 0;
        pixelFormat = 0;
        outputImageFormat = 0;
    }
    public int width;
    public int height;
    public int inputOrientation;
    public int outputOrientation;
    public int rowStride0;
    public int rowStride1;
    public int rowStride2;
    public int pixelStride0;
    public int pixelStride1;
    public int pixelStride2;
    public int pixelFormat;
    public int outputImageFormat;
    public boolean requireMirroring;
}