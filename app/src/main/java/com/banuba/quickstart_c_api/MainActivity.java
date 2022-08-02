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
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.banuba.quickstart_c_api.rendering.GL420Renderer;

import com.banuba.quickstart_c_api.rendering.GLNV12Renderer;
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
    private ImageInfo imageInfo = null;

    // Changing mImageOutputFormat will cause the renderer's changing
    private ImageOutputFormat mImageOutputFormat = ImageOutputFormat.i420;

    void createRenderer() {
        switch (mImageOutputFormat) {
            case NV12:
                renderer = new GLNV12Renderer();
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
        oep.loadEffect("effects/Makeup");
        oep.evalJs("Eyes.color('0 1 0 0.64')");
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

        String pathToResources = new File(getFilesDir().getAbsoluteFile(), "bnb-resources").getAbsolutePath();
        ResourcesExtractor.Companion.prepare(getAssets(), pathToResources);

        /* initialize OpenGL renderer */
        createRenderer();
        glView = findViewById(R.id.glSurfaceView);
        glView.setEGLContextClientVersion(3);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        /* initialize Banuba SDK */
        OffscreenEffectPlayer.init(pathToResources, BanubaClientToken.KEY);
        /* Create offscreen effect player */
        createOEP();

        imageInfo = new ImageInfo();
        requestCameraPermissionAndStart();
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
                            int rotation = getRotation(MainActivity.this);
                            int inputOrientation = 0;
                            int outputOrientation = 0;
                            switch (rotation) {
                                case Surface.ROTATION_0: /* portrait */
                                    inputOrientation = 270;
                                    outputOrientation = 180;
                                    break;
                                case Surface.ROTATION_90: /* landscape */
                                    inputOrientation = 180;
                                    outputOrientation = 0;
                                    break;
                                case Surface.ROTATION_180: /* reverse portrait */
                                    inputOrientation = 90;
                                    outputOrientation = 180;
                                    break;
                                case Surface.ROTATION_270: /* reverse landscape */
                                    inputOrientation = 0;
                                    outputOrientation = 0;
                                    break;
                            }

                            imageInfo.width = proxy.getWidth();
                            imageInfo.height = proxy.getHeight();
                            imageInfo.inputOrientation = inputOrientation;
                            imageInfo.outputOrientation = outputOrientation;
                            imageInfo.rowStride0 = proxy.getPlanes()[0].getRowStride();
                            imageInfo.rowStride1 = proxy.getPlanes()[1].getRowStride();
                            imageInfo.rowStride2 = proxy.getPlanes()[2].getRowStride();
                            imageInfo.pixelStride0 = proxy.getPlanes()[0].getPixelStride();
                            imageInfo.pixelFormat = proxy.getImage().getFormat();
                            imageInfo.requireMirroring = false;
                            imageInfo.outputImageFormat = mImageOutputFormat.ordinal();

                            oep.processImageAsync(
                                    proxy.getPlanes()[0].getBuffer(),
                                    proxy.getPlanes()[1].getBuffer(),
                                    proxy.getPlanes()[2].getBuffer(),
                                    imageInfo);
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
}

enum ImageOutputFormat {
    RGB,
    NV12,
    i420
}