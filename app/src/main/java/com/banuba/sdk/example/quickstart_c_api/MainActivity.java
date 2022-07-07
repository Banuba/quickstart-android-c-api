package com.banuba.sdk.example.quickstart_c_api;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
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


import com.banuba.sdk.example.common.BanubaClientTokenKt;
import com.banuba.sdk.utils.ContextProvider;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity  {
    private static int CAMERA_PERMISSION_REQUEST = 12345;

    private Size requestSize =  new Size(1280, 720);
    private Size size = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture = null;
    private OffscreenEffectPlayer oep = null;
    private GLSurfaceView glView = null;
    private GLRenderer renderer = null;

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
        renderer = new GLRenderer();
        glView = findViewById(R.id.glSurfaceView);
        glView.setEGLContextClientVersion(3);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        /* initialize Banuba SDK */
        OffscreenEffectPlayer.init(pathToResources, BanubaClientTokenKt.BANUBA_CLIENT_TOKEN);
        /* Create offscreen effect player */
        oep = new OffscreenEffectPlayer(size.getWidth(), size.getHeight());
        oep.loadEffect(<#Place the effect name here, e.g. effects/test_BG#>);
        oep.setDataReadyCallback((image, width, height) -> {
            renderer.drawRGBAImage(image, width, height);
            glView.requestRender();
        });

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
                            Image image = proxy.getImage();
                            ByteBuffer imageBuffer = imageToByteBuffer(image);

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
                            oep.processImageAsync(imageBuffer, image.getWidth(), image.getHeight(), inputOrientation, false, outputOrientation);
                            proxy.close();
                        });
                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis);
            } catch (Exception e) {
                Log.d("Exception in camera:", e.toString());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private ByteBuffer imageToByteBuffer(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uvBuffer = image.getPlanes()[1].getBuffer();
        int ySize = yBuffer.remaining();
        int uvSize = uvBuffer.remaining();
        ByteBuffer output = ByteBuffer.allocateDirect(ySize + uvSize);

        output.put(yBuffer);
        output.put(uvBuffer);
        return output;
    }

    /* Returns one of: Surface.ROTATION_0; Surface.ROTATION_90; Surface.ROTATION_180; Surface.ROTATION_270 */
    public int getRotation(Context context) {
        final int rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        return rotation;
    }
}
