package com.example.attendanceproject.face_detection;

import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;
import android.Manifest;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendanceproject.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class FaceDetectionActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private PreviewView previewView;
    private FrameLayout overlayContainer;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private FaceDetector faceDetector;

    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);
        previewView = findViewById(R.id.previewView);
        overlayContainer = findViewById(R.id.overlayContainer);

        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            initSource();
        }

    }

    private void initSource() {

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getApplicationContext()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                // Permission was denied. Handle the feature's unavailability or close the activity
                Log.e("CameraPermission", "Camera permission was not granted.");
            }
        }
    }

    private void setupCamera() {
        // Ensure that cameraProviderFuture is completed and get the cameraProvider.
        if (cameraProviderFuture.isDone()) {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXApp", "Error binding camera use cases", e);
            }
        } else {
            // In case cameraProviderFuture is not done, add a listener to call bindPreview when it's ready.
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("CameraXApp", "Error binding camera use cases", e);
                }
            }, ContextCompat.getMainExecutor(this));
        }
    }


    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        int lensFacing = CameraSelector.LENS_FACING_BACK;
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

//        setFaceDetector(lensFacing);
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    private void detectFaces(InputImage image, Runnable onComplete) {
        faceDetector.process(image)
                .addOnSuccessListener(
                        faces -> {
                            drawFaceBoundingBoxes(faces);
                            onComplete.run();
                        })
                .addOnFailureListener(
                        e -> {
                            Log.e("FaceDetection", "Face detection failed", e);
                            onComplete.run();
                        });
    }

    private void drawFaceBoundingBoxes(List<Face> faces) {
        View overlay = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);

                for (Face face : faces) {
                    canvas.drawRect(face.getBoundingBox(), paint);
                }
            }
        };

        overlayContainer.removeAllViews();
        overlayContainer.addView(overlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }


}
