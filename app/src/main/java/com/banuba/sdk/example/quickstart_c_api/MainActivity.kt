package com.banuba.sdk.example.quickstart_c_api


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture


import java.nio.ByteBuffer
import android.opengl.GLSurfaceView
import com.banuba.sdk.example.common.BANUBA_CLIENT_TOKEN
import com.banuba.sdk.utils.ContextProvider
import kotlinx.android.synthetic.main.activity_main.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class MainActivity : AppCompatActivity() {

    var oep = OffscreenEffectPlayer()
    lateinit var glView: GLSurfaceView
    lateinit var render: OffscreenEffectPlayerRenderer

    private var preview1: ImageView? = null
    private val PERMISSION_REQUEST_CAMERA = 10
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var translator = YUVtoRGB()

    override fun onCreate(savedInstanceState: Bundle?) {
        ContextProvider.setContext(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preview1 = findViewById(R.id.preview)

//        val pathToResources = application.filesDir.absolutePath + "/bnb-resources"
//        ResourcesExtractor.prepare(application.assets, pathToResources)
//        oep.create(pathToResources, BANUBA_CLIENT_TOKEN)
//        oep.loadEffect("effects/virtual-background")
//        var contentViewIsInit = false
//        oep.setDataReadyCallback{ image: ByteArray, width: Int, height: Int ->
//            runOnUiThread {
//                if (!contentViewIsInit) {
//                    setContentView(R.layout.activity_main)
//                    contentViewIsInit = true
//                }
//                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(image))
//                // show processing result
//                preview.setImageBitmap(bmp)
//            }
//        }

//        render = OffscreenEffectPlayerRenderer(oep)
//        glView = GLSurfaceView(this)
//        glView.setEGLContextClientVersion(3)
//        glView.setRenderer(render)
//        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//        setContentView(glView)
//        val bitmap = BitmapFactory.decodeStream(application.assets.open("img/photo.jpg"))
//        render.processImage(bitmap)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CAMERA
            )
        } else {
            startCamera()
        }
    }

    override fun onDestroy() {
        oep.destroy()
        super.onDestroy()
    }

    @SuppressLint("UnsafeOptInUsageError", "UnsafeExperimentalUsageError")
    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener( Runnable {
            try {
                val cameraProvider = cameraProviderFuture!!.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1024, 768))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@MainActivity),
                    ImageAnalysis.Analyzer { imageProxy ->
                        imageProxy.imageInfo.rotationDegrees
                        val img: Image? = imageProxy.image
                        val bitmap: Bitmap? = img?.let { translator.translateYUV(it, this@MainActivity) }

//                        if (bitmap != null) {
//                            render.processImage(bitmap)
//                        }
                        preview1!!.rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                        preview1!!.setImageBitmap(bitmap)
                        //  after done, release the ImageProxy object
                        imageProxy.close()
                    }
                )
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, imageAnalysis)
            } catch (exc: Exception) {
                //
            }
        }, ContextCompat.getMainExecutor(this)
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
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
