package com.banuba.sdk.example.quickstart_c_api

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.banuba.sdk.example.common.BANUBA_CLIENT_TOKEN
import com.banuba.sdk.utils.ContextProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var oep = OffscreenEffectPlayer()
    private val permissionRequestCamera = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        ContextProvider.setContext(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pathToResources = application.filesDir.absolutePath + "/bnb-resources"
        ResourcesExtractor.prepare(application.assets, pathToResources)
        oep.create(pathToResources, BANUBA_CLIENT_TOKEN)

        oep.loadEffect("effects/Afro")
        oep.setDataReadyCallback{ image: ByteArray, width: Int, height: Int ->
            runOnUiThread {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(image))
                imageView.setImageBitmap(bmp)
            }
        }
        startCamera()
    }

    override fun onDestroy() {
        oep.destroy()
        super.onDestroy()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                permissionRequestCamera)
        } else {
            makePicture()
        }
    }

    @SuppressLint("UnsafeOptInUsageError", "UnsafeExperimentalUsageError")
    private fun makePicture() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener( Runnable {
            try {
                val cameraProvider = cameraProviderFuture!!.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@MainActivity),
                    ImageAnalysis.Analyzer { imageProxy ->  // YUV_420_888
                        val image = imageProxy.image
                        val image_buffer = image?.let { imageToByteBuffer(it) }
                        val width =  image?.width
                        val height =  image?.height
                        if (image_buffer != null && width != null && height != null) {
                            oep.surfaceChanged(width, height)
                            oep.processImageAsync(image_buffer, width, height)
                        }

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
        if (requestCode == permissionRequestCamera && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}

fun imageToByteBuffer(image: Image): ByteBuffer {
    val yBuffer: ByteBuffer = image.planes[0].buffer
    val uBuffer: ByteBuffer = image.planes[1].buffer
    val vBuffer: ByteBuffer = image.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val output = ByteBuffer.allocateDirect(ySize + uSize + vSize)

    yBuffer[output.array(), 0, ySize]
    uBuffer[output.array(), ySize, uSize]
    vBuffer[output.array(), ySize + vSize, vSize]
    return output
}

