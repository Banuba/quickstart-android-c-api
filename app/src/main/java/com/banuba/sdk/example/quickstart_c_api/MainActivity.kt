package com.banuba.sdk.example.quickstart_c_api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.banuba.sdk.example.common.BANUBA_CLIENT_TOKEN
import com.banuba.sdk.utils.ContextProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.nio.ByteBuffer
import java.util.zip.ZipFile
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    var oep = OffscreenEffectPlayer()
    lateinit var glView: GLSurfaceView
    lateinit var render: OffscreenEffectPlayerRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        ContextProvider.setContext(applicationContext)
        super.onCreate(savedInstanceState)

        val pathToResources = application.filesDir.absolutePath + "/bnb-resources"
        ResourcesExtractor.prepare(application.assets, pathToResources)

        oep.create(pathToResources, BANUBA_CLIENT_TOKEN)
        oep.loadEffect("effects/blur_bg")

        var contentViewIsInit = false
        oep.setDataReadyCallback{ image: ByteArray, width: Int, height: Int ->
            runOnUiThread {
                if (!contentViewIsInit) {
                    setContentView(R.layout.activity_main)
                    contentViewIsInit = true
                }

                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(image))
                // show processing result
                imageView.setImageBitmap(bmp)
            }
        }

        render = OffscreenEffectPlayerRenderer(oep)

        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(3)
        glView.setRenderer(render)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        setContentView(glView)

        val bitmap = BitmapFactory.decodeStream(application.assets.open("img/photo.jpg"))

        // process image async in RenderThread with GL context
        render.processImage(bitmap)
    }

    override fun onDestroy() {
        oep.destroy()
        super.onDestroy()
    }
}

class OffscreenEffectPlayerRenderer(offscreenEffectPlayer: OffscreenEffectPlayer) : GLSurfaceView.Renderer {
    private val oep = offscreenEffectPlayer
    private var byteBuffer: ByteBuffer? = null
    private var width: Int = 0
    private var height: Int = 0

    fun processImage(bitmap: Bitmap) {
        this.byteBuffer = ByteBuffer.allocateDirect(bitmap.rowBytes * bitmap.height)
        this.width = bitmap.width
        this.height = bitmap.height
        bitmap.copyPixelsToBuffer(byteBuffer)
        oep.surfaceChanged(this.width, this.height);
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10, w: Int, h: Int) {
    }

    override fun onDrawFrame(gl: GL10) {
        oep.processImageAsync(this.byteBuffer!!, this.width, this.height)
    }
}
