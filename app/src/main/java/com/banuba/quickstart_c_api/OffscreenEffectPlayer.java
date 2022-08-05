package com.banuba.quickstart_c_api;

import java.nio.ByteBuffer;

class OffscreenEffectPlayer {
    private long mOep = 0;
    private DataReadyCallback mDataReadyCallback = null;
    private static boolean isInit = false;

    public static void init(String pathToResources, String clientToken) {
        assert !isInit;
        if (!isInit) {
            externalInit(pathToResources, clientToken);
            isInit = true;
        }
    }

    public static void deinit() {
        assert isInit;
        if (isInit) {
            isInit = false;
            externalDeinit();
        }
    }

    public OffscreenEffectPlayer(int width, int hegiht) {
        assert isInit;
        mOep = externalCreate(width, hegiht);
        surfaceChanged(width, hegiht);
    }

    @Override
    protected void finalize() {
        destroy();
    }

    public void destroy() {
        if (mOep != 0) {
            externalDestroy(mOep);
            mOep = 0;
        }
    }

    /* image must be NV12 format */
    public void processImageAsync(OffscreenEffectPlayerImage image, boolean isProcessImage) {
        externalProcessImageAsync(mOep, image.mImageZero, image.mImageFirst, image.mImageSecond, image.mImageInfo, isProcessImage);
    }

    public void surfaceChanged(int width, int height) {
        externalSurfaceChanged(mOep, width, height);
    }

    public void loadEffect(String effectPath) {
        externalLoadEffect(mOep, effectPath);
    }

    public void unloadEffect() {
        externalUnloadEffect(mOep);
    }

    public void pause() {
        externalPause(mOep);
    }

    public void resume() {
        externalResume(mOep);
    }

    public void stop() {
        externalStop(mOep);
    }

    public void callJsMethod(String method, String param) {
        externalCallJsMethod(mOep, method, param);
    }

    public void evalJs(String script) {
        externalEvalJs(mOep, script);
    }

    public interface DataReadyCallback {
        void onDataReady(byte[] image0, byte[] image1, byte[] image2, int width, int height);
    }

    public void setDataReadyCallback(DataReadyCallback callback) {
        mDataReadyCallback = callback;
    }

    private void onDataReady(byte[] image0, byte[] image1, byte[] image2, int width, int height) {
        if (mDataReadyCallback != null) {
            mDataReadyCallback.onDataReady(image0, image1, image2, width, height);
        }
    }

    /* The functions below are implemented in c++ */
    private static native void externalInit(String pathToResources, String clientToken);
    private static native void externalDeinit();
    private native long externalCreate(int width, int height);
    private native void externalDestroy(long oep);
    private native void externalProcessImageAsync(long oep, ByteBuffer imageY, ByteBuffer imageU, ByteBuffer imageV, ImageInfo info, boolean isProcessImage);
    private native void externalSurfaceChanged(long oep, int width, int height);
    private native void externalLoadEffect(long oep, String effectPath);
    private native void externalUnloadEffect(long oep);
    private native void externalPause(long oep);
    private native void externalResume(long oep);
    private native void externalStop(long oep);
    private native void externalCallJsMethod(long oep, String method, String param);
    private native void externalEvalJs(long oep, String script);

    static {
        System.loadLibrary("native-lib");
    }
}
