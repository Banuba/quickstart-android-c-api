package com.banuba.sdk.example.quickstart_cpp

import java.nio.ByteBuffer

class BanubaSdk {
    external fun createEffectPlayer(pathToResources: String, clientToken: String): Long
    external fun destroyEffectPlayer(effectPlayer: Long)

    external fun surfaceChanged(effectPlayer: Long, width: Int, height: Int)

    external fun loadEffect(effectPlayer: Long, name: String)
    external fun processPhoto(effectPlayer: Long, rgba: ByteBuffer, width: Int, height: Int): ByteArray


    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}