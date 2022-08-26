package com.banuba.quickstart_c_api;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class OffscreenEffectPlayerImage {

    OffscreenEffectPlayerImage(ImageProxy imageProxy) {
        mWidth = imageProxy.getWidth();
        mHeight = imageProxy.getHeight();
        mPixelFormat = imageProxy.getImage().getFormat();

        ImageProxy.PlaneProxy planeProxy = imageProxy.getPlanes()[0];
        mRowStride0 = planeProxy.getRowStride();
        mPlane0 = planeProxy.getBuffer();
        mPixelStride0 = planeProxy.getPixelStride();
        int planesNumber = imageProxy.getPlanes().length;
        if (planesNumber > 1) {
            planeProxy = imageProxy.getPlanes()[1];
            mRowStride1 = planeProxy.getRowStride();
            mPlane1 = planeProxy.getBuffer();
            mPixelStride1 = planeProxy.getPixelStride();
            if (planesNumber > 2) {
                planeProxy = imageProxy.getPlanes()[2];
                mRowStride2 = planeProxy.getRowStride();
                mPlane2 = planeProxy.getBuffer();
                mPixelStride2 = planeProxy.getPixelStride();
            } else {
                mRowStride2 = 0;
                mPlane2 = null;
                mPixelStride2 = 0;
            }
        } else {
            mRowStride1 = 0;
            mPlane1 = null;
            mPixelStride1 = 0;
            mRowStride2 = 0;
            mPlane2 = null;
            mPixelStride2 = 0;
        }
    }

    OffscreenEffectPlayerImage(byte[] rgbPlane, int width, int height) {
        assert rgbPlane != null;
        mPlane0 =  ByteBuffer.wrap(rgbPlane);
        mPlane1 =  null;
        mPlane2 =  null;
        mWidth = width;
        mHeight = height;

        mRowStride0 = 0;
        mRowStride1 = 0;
        mRowStride2 = 0;
        mPixelStride0 = 0;
        mPixelStride1 = 0;
        mPixelStride2 = 0;
        mPixelFormat = 0;
    }

    OffscreenEffectPlayerImage(byte[] yPlane, byte[] uvPlane, int width, int height) {
        assert yPlane != null && uvPlane != null;
        mPlane0 =  ByteBuffer.wrap(yPlane);
        mPlane1 =  ByteBuffer.wrap(uvPlane);
        mPlane2 =  null;
        mWidth = width;
        mHeight = height;

        mRowStride0 = 0;
        mRowStride1 = 0;
        mRowStride2 = 0;
        mPixelStride0 = 0;
        mPixelStride1 = 0;
        mPixelStride2 = 0;
        mPixelFormat = 0;
    }

    OffscreenEffectPlayerImage(byte[] yPlane, byte[] uPlane, byte[] vPlane, int width, int height) {
        assert yPlane != null && uPlane != null && vPlane != null;
        mPlane0 =  ByteBuffer.wrap(yPlane);
        mPlane1 =  ByteBuffer.wrap(uPlane);
        mPlane2 =  ByteBuffer.wrap(vPlane);
        mWidth = width;
        mHeight = height;

        mRowStride0 = 0;
        mRowStride1 = 0;
        mRowStride2 = 0;
        mPixelStride0 = 0;
        mPixelStride1 = 0;
        mPixelStride2 = 0;
        mPixelFormat = 0;
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

    final private ByteBuffer mPlane0;
    final private ByteBuffer mPlane1;
    final private ByteBuffer mPlane2;

    final private int mWidth;
    final private int mHeight;
    final private int mRowStride0;
    final private int mRowStride1;
    final private int mRowStride2;
    final private int mPixelStride0;
    final private int mPixelStride1;
    final private int mPixelStride2;
    final private int mPixelFormat;
}
