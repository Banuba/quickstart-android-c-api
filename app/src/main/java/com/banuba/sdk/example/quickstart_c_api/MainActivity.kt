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
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var oep = OffscreenEffectPlayer()
    var translator = YUVtoRGB()
    private val default_height = 720
    private val default_width = 1280
    private val permission_request_camera = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        ContextProvider.setContext(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pathToResources = application.filesDir.absolutePath + "/bnb-resources"
        ResourcesExtractor.prepare(application.assets, pathToResources)
        oep.create(pathToResources, BANUBA_CLIENT_TOKEN)
        oep.surfaceChanged(default_width, default_height)
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
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                permission_request_camera)
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
                    .setTargetResolution(Size(default_width, default_height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@MainActivity),
                    ImageAnalysis.Analyzer { imageProxy ->
                        val img: Image? = imageProxy.image
                        val bitmap: Bitmap? = img?.let { translator.translateYUV(it, this@MainActivity) }

                        val width = bitmap?.rowBytes ?: 0
                        val height = bitmap?.height!!
                        val byte_buffer: ByteBuffer? = ByteBuffer.allocateDirect( width * height)
                        bitmap.copyPixelsToBuffer(byte_buffer)
                        if (byte_buffer != null) {
                            oep.processImageAsync(byte_buffer, default_width, default_height)
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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String?>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permission_request_camera && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}
