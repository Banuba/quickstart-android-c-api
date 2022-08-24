package com.banuba.quickstart_c_api;

import java.nio.ByteBuffer;

public class OffscreenEffectPlayerImage {

    OffscreenEffectPlayerImage() {}

    OffscreenEffectPlayerImage(byte[] yPlane, byte[] uPlane, byte[] vPlane, int width, int height) {
        if(yPlane != null) {
            mPlane0 =  ByteBuffer.wrap(yPlane);
        }
        if(uPlane != null) {
            mPlane1 =  ByteBuffer.wrap(uPlane);
        }
        if(vPlane != null) {
            mPlane2 =  ByteBuffer.wrap(vPlane);
        }

        mWidth = width;
        mHeight = height;
    }

    void setWidth(int width) { mWidth = width; }

    void setHeight(int height) { mHeight = height; }

    void setPixelFormat(int pixelFormat) {
        mPixelFormat = pixelFormat;
    }

    void setPlane(int planeNumber, int planeWidth, ByteBuffer plane, int pixelStride) {
        switch (planeNumber) {
            case 1:
                mRowStride1 = planeWidth;
                mPlane1 = plane;
                mPixelStride1 = pixelStride;
                break;
            case 2:
                mRowStride2 = planeWidth;
                mPlane2 = plane;
                mPixelStride2 = pixelStride;
                break;
            default:
                mRowStride0 = planeWidth;
                mPlane0 = plane;
                mPixelStride0 = pixelStride;
                break;
        }
    }

    public ByteBuffer getPlane(int planeNumber) {
        switch (planeNumber) {
            case 1:
                return mPlane1;
            case 2:
                return mPlane2;
            default:
                return mPlane0;
        }
    }

    public int getWidth() { return mWidth; }

    public int getHeight() {
        return mHeight;
    }

    private ByteBuffer mPlane0;
    private ByteBuffer mPlane1;
    private ByteBuffer mPlane2;

    private int mWidth;
    private int mHeight;
    private int mRowStride0;
    private int mRowStride1;
    private int mRowStride2;
    private int mPixelStride0;
    private int mPixelStride1;
    private int mPixelStride2;
    private int mPixelFormat;
}
