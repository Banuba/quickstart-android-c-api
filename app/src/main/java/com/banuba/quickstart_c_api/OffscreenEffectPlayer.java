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

    /* image must be NV12 or i420 format */
    public void processImageAsync(Image image,
                                  int inputOrientation,
                                  boolean isRequiredMirroring,
                                  int outputOrientation,
                                  int imageFormat,
                                  boolean isProcessedImage) {
        externalProcessImageAsync(mOep, image, inputOrientation, isRequiredMirroring,
                outputOrientation, imageFormat, isProcessedImage);
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
        void onDataReady(Image image);
    }

    public void setDataReadyCallback(DataReadyCallback callback) {
        mDataReadyCallback = callback;
    }

    private void onDataReady(Image image) {
        if (mDataReadyCallback != null) {
            mDataReadyCallback.onDataReady(image);
        }
    }

    /* The functions below are implemented in c++ */
    private static native void externalInit(String pathToResources, String clientToken);
    private static native void externalDeinit();
    private native long externalCreate(int width, int height);
    private native void externalDestroy(long oep);
    private native void externalProcessImageAsync(long oep, Image image,
                                                  int inputOrientation,
                                                  boolean isRequiredMirroring,
                                                  int outputOrientation,
                                                  int imageFormat,
                                                  boolean isProcessedImage);
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

