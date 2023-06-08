package com.banuba.quickstart_c_api;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.banuba.quickstart_c_api.rendering.GL420Renderer;

import com.banuba.quickstart_c_api.rendering.GLYUVNVRenderer;
import com.banuba.quickstart_c_api.rendering.GLRGBARenderer;
import com.banuba.quickstart_c_api.rendering.GLRenderer;
import com.banuba.sdk.utils.ContextProvider;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static int CAMERA_PERMISSION_REQUEST = 12345;

    private Size requestSize = new Size(1280, 720);
    private Size size = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture = null;
    private OffscreenEffectPlayer oep = null;
    private GLSurfaceView glView = null;
    private GLRenderer renderer = null;
    private OffscreenEffectPlayerImage mImage = null;

    // Changing mImageOutputFormat will cause the format's changing (input and output image of OEP)
    private ImageFormat mImageFormat = ImageFormat.NV12;

    void createRenderer() {
        switch (mImageFormat) {
            case NV12:
                renderer = new GLYUVNVRenderer();
                break;
            case RGB:
                renderer = new GLRGBARenderer();
                break;
            case i420:
                renderer = new GL420Renderer();
                break;
        }
    }

    void createOEP() {
        oep = new OffscreenEffectPlayer(size.getWidth(), size.getHeight());
        oep.loadEffect("effects/<!!! PLACE YOUR EFFECT NAME HERE !!!>")
        oep.setDataReadyCallback(
                (image0,image1, image2, width, height) -> {
                    List<byte[]> planes = Arrays.asList(image0, image1, image2);
                    renderer.drawImage(planes, width, height);
                    glView.requestRender();
                });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        ContextProvider.setContext(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateSize();

        /* initialize OpenGL renderer */
        createRenderer();
        glView = findViewById(R.id.glSurfaceView);
        glView.setEGLContextClientVersion(3);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        /* initialize Banuba SDK */
        OffscreenEffectPlayer.init("", BanubaClientToken.KEY);
        /* Create offscreen effect player */
        createOEP();

        mImage = new OffscreenEffectPlayerImage();
        requestCameraPermissionAndStart();
    }

    private void updateSize() {
        final int rotation = getRotation(this);
        switch (rotation) {
            case Surface.ROTATION_0: /* portrait */
                size = new Size(requestSize.getHeight(), requestSize.getWidth());
                break;
            case Surface.ROTATION_90: /* landscape */
                size = new Size(requestSize.getWidth(), requestSize.getHeight());
                break;
            case Surface.ROTATION_180: /* reverse portrait */
                size = new Size(requestSize.getHeight(), requestSize.getWidth());
                break;
            case Surface.ROTATION_270: /* reverse landscape */
                size = new Size(requestSize.getWidth(), requestSize.getHeight());
                break;
        }
    }

    @Override
    protected void onDestroy() {
        oep.destroy();
        OffscreenEffectPlayer.deinit();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionAndStart();
        }
    }

    private void requestCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(MainActivity.this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(size)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(MainActivity.this),
                        proxy -> {
                            updateImage(proxy);
                            oep.processImageAsync(mImage);
                            proxy.close();
                        });
                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis);
            } catch (Exception e) {
                Log.d("Exception in camera:", e.toString());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /* Returns one of: Surface.ROTATION_0; Surface.ROTATION_90; Surface.ROTATION_180; Surface.ROTATION_270 */
    public int getRotation(Context context) {
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        return rotation;
    }

    private int getInputOrientation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0: /* portrait */
                return 270;
            case Surface.ROTATION_90: /* landscape */
                return 180;
            case Surface.ROTATION_180: /* reverse portrait */
                return 90;
            case Surface.ROTATION_270: /* reverse landscape */
                return 0;
        }
        return 0;
    }

    private int getOutputOrientation(int rotation) {
        if(rotation == Surface.ROTATION_0 || rotation ==  Surface.ROTATION_180) {
            return 0;
        }
        return 180;
    }

    private void updateImage(ImageProxy imageProxy) {
        int rotation = getRotation(MainActivity.this);

        mImage.mImageInfo.width = imageProxy.getWidth();
        mImage.mImageInfo.height = imageProxy.getHeight();
        mImage.mImageInfo.inputOrientation = getInputOrientation(rotation);
        mImage.mImageInfo.outputOrientation = getOutputOrientation(rotation);
        mImage.mImageInfo.pixelFormat = imageProxy.getImage().getFormat();
        mImage.mImageInfo.requireMirroring = true;
        mImage.mImageInfo.imageFormat = mImageFormat.ordinal();

        mImage.mImageInfo.rowStride0 = imageProxy.getPlanes()[0].getRowStride();
        mImage.mImageZero = imageProxy.getPlanes()[0].getBuffer();
        mImage.mImageInfo.pixelStride0 = imageProxy.getPlanes()[0].getPixelStride();

        int planesNumber = imageProxy.getPlanes().length;
        if (planesNumber > 1) {
            mImage.mImageInfo.rowStride1 = imageProxy.getPlanes()[1].getRowStride();
            mImage.mImageFirst = imageProxy.getPlanes()[1].getBuffer();
            mImage.mImageInfo.pixelStride1 = imageProxy.getPlanes()[1].getPixelStride();
            if (planesNumber > 2) {
                mImage.mImageInfo.rowStride2 = imageProxy.getPlanes()[2].getRowStride();
                mImage.mImageSecond = imageProxy.getPlanes()[2].getBuffer();
                mImage.mImageInfo.pixelStride2 = imageProxy.getPlanes()[2].getPixelStride();
            }
        }
    }
}

enum ImageFormat {
    RGB,
    NV12,
    i420
}