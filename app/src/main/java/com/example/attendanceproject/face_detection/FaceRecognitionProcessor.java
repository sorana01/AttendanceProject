package com.example.attendanceproject.face_detection;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProcessor;
import androidx.camera.core.ImageProxy;

import com.example.attendanceproject.draw.FaceGraphic;
import com.example.attendanceproject.draw.GraphicOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaceRecognitionProcessor {

    class Person {
        public String name;
        public float[] faceVector;

        public Person(String name, float[] faceVector) {
            this.name = name;
            this.faceVector = faceVector;
        }
    }

    public interface FaceRecognitionCallback {
        void onFaceRecognised(Face face, float probability, String name);
        void onFaceDetected(Face face, Bitmap faceBitmap, float[] vector);

    }

    private static final String TAG = "FaceRecognitionProcessor";

    // Input image size for our facenet model
    private static final int FACENET_INPUT_IMAGE_SIZE = 112;

    private final FaceDetector detector;
    //    private final Interpreter faceNetModelInterpreter;
//    private final ImageProcessor faceNetImageProcessor;
    private final GraphicOverlay graphicOverlay;
    private final FaceRecognitionCallback callback;

    List<Person> recognisedFaceList = new ArrayList();

    public FaceRecognitionProcessor(GraphicOverlay graphicOverlay,
                                    FaceRecognitionCallback callback) {
        this.callback = callback;
        this.graphicOverlay = graphicOverlay;

//        try {
//            this.faceNetModelInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        // initialize processors
//        this.faceNetModelInterpreter = faceNetModelInterpreter;
        // preprocess image to work with tensorflow model = mobilefacenet.tflite
//        faceNetImageProcessor = new ImageProcessor.Builder()
//                .add(new ResizeOp(FACENET_INPUT_IMAGE_SIZE, FACENET_INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
//                .add(new NormalizeOp(0f, 255f))
//                .build();

        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                // to ensure we don't count and analyse same person again
                .enableTracking()
                .build();
        this.detector = FaceDetection.getClient(faceDetectorOptions);
    }


    @OptIn(markerClass = ExperimentalGetImage.class)
    public Task<List<Face>> detectInImage(ImageProxy imageProxy, Bitmap bitmap, int rotationDegrees) {
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), rotationDegrees);
        int rotation = rotationDegrees;

        // In order to correctly display the face bounds, the orientation of the analyzed
        // image and that of the viewfinder have to match. Which is why the dimensions of
        // the analyzed image are reversed if its rotation information is 90 or 270.
        boolean reverseDimens = rotation == 90 || rotation == 270;
        int width;
        int height;
        if (reverseDimens) {
            width = imageProxy.getHeight();
            height = imageProxy.getWidth();
        } else {
            width = imageProxy.getWidth();
            height = imageProxy.getHeight();
        }
        return detector.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        graphicOverlay.clear();
                        if (faces.isEmpty()) {
                            // No faces were detected
                            Log.d(TAG, "No faces found in the image.");
                            // You can also call a method or perform an action here to signal that no faces were found
                            if (callback != null) {
//                                callback.onNoFacesDetected(); // Assuming you have such a method in your callback
                            }
                        } else {
                            for (Face face : faces) {
                                FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, width, height);
                                Log.d(TAG, "Face found, id: " + face.getTrackingId());
//                                Bitmap faceBitmap = cropToBBox(bitmap, face.getBoundingBox(), rotation);
//
//                                if (faceBitmap == null) {
//                                    Log.d("GraphicOverlay", "Face bitmap null");
//                                    return;
//                                }
//
//                                TensorImage tensorImage = TensorImage.fromBitmap(faceBitmap);
//                                ByteBuffer faceNetByteBuffer = faceNetImageProcessor.process(tensorImage).getBuffer();
//                                float[][] faceOutputArray = new float[1][192];
//
//                                // ! create embedding
//                                faceNetModelInterpreter.run(faceNetByteBuffer, faceOutputArray);
//
//                                Log.d(TAG, "Output array: " + Arrays.deepToString(faceOutputArray));
//
//                                if (callback != null) {
//                                    callback.onFaceDetected(face, faceBitmap, faceOutputArray[0]);
//                                    if (!recognisedFaceList.isEmpty()) {
//                                        Pair<String, Float> result = findNearestFace(faceOutputArray[0]);
//                                        if (result.second < 1.0f) {
//                                            faceGraphic.name = result.first;
//                                            callback.onFaceRecognised(face, result.second, result.first);
//                                        }
//                                    }
//                                }

                                graphicOverlay.add(faceGraphic);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Intentionally left empty
                        // this means the face in frame has not yet been registered
//                        Log.d(TAG, "detectInImage: error processing image");
                    }
                });
    }

}
