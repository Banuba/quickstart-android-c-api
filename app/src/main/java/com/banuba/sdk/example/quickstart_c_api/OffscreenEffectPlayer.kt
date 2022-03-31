package com.banuba.sdk.example.quickstart_c_api

import java.nio.ByteBuffer

class OffscreenEffectPlayer {
    private var callbackReady: ((ByteArray, Int, Int) -> Unit)? = null
    private var oep: Long = 0

    fun create(pathToResources: String, clientToken: String) {
        this.oep = externalCreate(pathToResources, clientToken)
    }

    fun destroy() {
        externalDestroy(this.oep)
        this.oep = 0
    }

    fun processImageAsync(image: ByteBuffer, width: Int, height: Int) {
        externalProcessImageAsync(this.oep, image, width, height)
    }

    fun surfaceChanged(width: Int, height: Int) {
        externalSurfaceChanged(this.oep, width, height)
    }

    fun loadEffect(name: String) {
        externalLoadEffect(this.oep, name)
    }

    fun unloadEffect() {
        externalUnloadEffect(this.oep)
    }

    fun pause() {
        externalPause(this.oep);
    }

    fun resume() {
        externalResume(this.oep)
    }

    fun callJsMethod(method: String, param: String) {
        externalCallJsMethod(this.oep, method, param)
    }

    fun setDataReadyCallback(callback: (image: ByteArray, width: Int, height: Int) -> Unit) {
        callbackReady = callback
    }

    fun dataReady(image: ByteArray, width: Int, height: Int) {
        callbackReady?.let { it(image, width, height) }
    }


    /* The functions below are implemented in c++ */
    private external fun externalCreate(pathToResources: String, clientToken: String): Long
    private external fun externalDestroy(oep: Long)
    private external fun externalProcessImageAsync(oep: Long, image: ByteBuffer, width: Int, height: Int)
    private external fun externalSurfaceChanged(oep: Long, width: Int, height: Int)
    private external fun externalLoadEffect(oep: Long, name: String)
    private external fun externalUnloadEffect(oep: Long)
    private external fun externalPause(oep: Long)
    private external fun externalResume(oep: Long)
    private external fun externalCallJsMethod(oep: Long, method: String, param: String)

    companion object {
        /* Used to load the 'native-lib' library on application startup. */
        init {
            System.loadLibrary("native-lib")
        }
    }
}
