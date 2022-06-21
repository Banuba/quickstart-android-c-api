package com.banuba.sdk.example.quickstart_c_api


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

class MainActivity : AppCompatActivity() {

    private var preview: ImageView? = null
    private val PERMISSION_REQUEST_CAMERA = 10
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var translator = YUVtoRGB()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.preview)

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
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@MainActivity),
                    ImageAnalysis.Analyzer { imageProxy ->
                    imageProxy.imageInfo.rotationDegrees
                    val img: Image? = imageProxy.image
                    val bitmap: Bitmap? =
                        img?.let { translator.translateYUV(it, this@MainActivity) }

                    val size = bitmap!!.width * bitmap.height
                    val pixels = IntArray(size)
                    bitmap.getPixels(
                        pixels, 0, bitmap.width, 0, 0,
                        bitmap.width, bitmap.height
                    )

                    for (i in 0 until size) {
                        val color = pixels[i]
                        val r = color shr 16 and 0xff
                        val g = color shr 8 and 0xff
                        val b = color and 0xff
                        val gray = (r + g + b) / 3
                        pixels[i] = -0x1000000 or (gray shl 16) or (gray shl 8) or gray
                    }
                    bitmap.setPixels(
                        pixels, 0, bitmap.width, 0, 0,
                        bitmap.width, bitmap.height
                    )

                    preview!!.rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                    preview!!.setImageBitmap(bitmap)
                    // after done, release the ImageProxy object
                    imageProxy.close()
                })
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
