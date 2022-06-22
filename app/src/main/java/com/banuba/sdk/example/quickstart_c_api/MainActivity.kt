package com.banuba.sdk.example.quickstart_c_api


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import android.util.Log
import com.banuba.sdk.example.common.BANUBA_CLIENT_TOKEN
import com.banuba.sdk.utils.ContextProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {
    var oep = OffscreenEffectPlayer()
    private val WIDTH = 1280
    private val HEIGHT = 720
    private val PERMISSION_REQUEST_CAMERA = 10
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var translator = YUVtoRGB()

    override fun onCreate(savedInstanceState: Bundle?) {
        ContextProvider.setContext(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pathToResources = application.filesDir.absolutePath + "/bnb-resources"
        ResourcesExtractor.prepare(application.assets, pathToResources)
        oep.create(pathToResources, BANUBA_CLIENT_TOKEN)
        oep.surfaceChanged(WIDTH, HEIGHT)
        oep.loadEffect("effects/virtual-background")
        oep.setDataReadyCallback{ image: ByteArray, width: Int, height: Int ->
            runOnUiThread {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(image))
                // show processing result
                imageView.setImageBitmap(bmp)
            }
        }

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
                    .setTargetResolution(Size(WIDTH, HEIGHT))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@MainActivity),
                    ImageAnalysis.Analyzer { imageProxy ->
                        val img: Image? = imageProxy.image
                        val bitmap: Bitmap? = img?.let { translator.translateYUV(it, this@MainActivity) }

                        val width = (bitmap?.rowBytes ?: 0)
                        val height = (bitmap?.height!!)
                        val byteBuffer: ByteBuffer? = ByteBuffer.allocateDirect( width * height)
                        bitmap.copyPixelsToBuffer(byteBuffer)
                        if (byteBuffer != null) {
                            oep.processImageAsync(byteBuffer, WIDTH, HEIGHT)
                        }

                        //  after done, release the ImageProxy object
                        imageProxy.close()
                    }
                )
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, imageAnalysis)
            } catch (exc: Exception) {
                Log.d("Exception in camera:", exc.toString())
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
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}
