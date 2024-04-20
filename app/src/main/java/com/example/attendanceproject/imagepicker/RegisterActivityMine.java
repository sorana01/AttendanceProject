package com.example.attendanceproject.imagepicker;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import android.Manifest;
import android.widget.Toast;

import com.example.attendanceproject.R;
import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class RegisterActivityMine extends AppCompatActivity {
    private ImageView imageView;
    private Button galleryBtn;
    private Button cameraBtn;
    private Uri image_uri;

    private List<Bitmap> bitmaps_individual;
    private List<float[][]> embeddingsList;
    public static final int PERMISSION_CODE = 100;

    // High-accuracy landmark detection and face classification
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    FaceDetector detector;
    FaceClassifier faceClassifier;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imageView = findViewById(R.id.imageView);
        galleryBtn = findViewById(R.id.btnGallery);
        cameraBtn = findViewById(R.id.btnCamera);

        //TODO ask for permission of camera upon first launch of application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, 112);
            }
        }

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryActivityResultLauncher.launch(galleryIntent);
            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, 112);
                    }
                    else {
                        openCamera();
                    }
                }
                else {
                    openCamera();
                }
            }
        });

        embeddingsList = new ArrayList<>();
        detector = FaceDetection.getClient(highAccuracyOpts);
        try {
            // CHANGE MODEL
            faceClassifier = TFLiteFaceRecognition.create(getAssets(), "facenet.tflite", 160, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bitmaps_individual = loadBitmapsFromAssets();
        Log.d("BITMAPS", "Bitmaps_individual has size of " + bitmaps_individual.size());

    }

    //TODO get the image from gallery and display it
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
//                    for (Bitmap bitmap : bitmaps_individual) {
//                        performFaceDetection(bitmap);
//                    }
                }
            });

    //TODO opens camera so that user can capture image
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }

    //TODO capture the image using camera and display it
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap input = uriToBitmap(image_uri);
                        input = rotateBitmap(input);
                        imageView.setImageBitmap(input);
//                        performFaceDetection(input);
                    }
                }
            });

    //TODO takes URI of the image and returns bitmap
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    //TODO rotate image if image captured on samsung devices
    //TODO Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }

    public void performFaceDetection(Bitmap input, String name, CountDownLatch latch) {
        Bitmap mutableBmp = input.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBmp);
        InputImage image = InputImage.fromBitmap(input, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    for (Face face : faces) {
                        Rect bounds = face.getBoundingBox();
                        Paint p1 = new Paint();
                        p1.setColor(Color.RED);
                        p1.setStyle(Paint.Style.STROKE);
                        p1.setStrokeWidth(15);
                        performFaceRecognition(bounds, input, name);
                        canvas.drawRect(bounds, p1);
                    }
                    imageView.setImageBitmap(mutableBmp);
                    latch.countDown(); // Decrement the latch count
                })
                .addOnFailureListener(e -> {
                    // Even on failure, you must decrement the latch
                    latch.countDown();
                });
    }


    public void performFaceRecognition(Rect bound, Bitmap input, String name) {
        if (bound.top < 0) {
            bound.top = 0;
        }
        if (bound.left < 0) {
            bound.left = 0;
        }
        if (bound.right > input.getWidth()) {
            bound.right = input.getWidth() - 1;
        }
        if (bound.bottom > input.getHeight()) {
            bound.bottom = input.getHeight() - 1;
        }

        Bitmap croppedFace = Bitmap.createBitmap(input, bound.left, bound.top, bound.width(), bound.height());
//        imageView.setImageBitmap(croppedFace);
        // CHANGE MODEL
        croppedFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, false);
        FaceClassifier.Recognition recognition = faceClassifier.recognizeImageRec(croppedFace, true);
//        Log.d("RegisterActivity", recognition.toString());
//        showRegisterDialogue(croppedFace, recognition);
        Log.d("INSIDE REGISTER", "Recognition object value " + recognition);
//        embeddingsList.add((float[][]) (recognition.getEmbedding()));
//        Log.d("EMBEDDINGS", "Embedding number " + embeddingsList.size() + " with content " + recognition.getEmbedding() + " added");
        faceClassifier.registerMul(name, recognition);
    }

    public void showRegisterDialogue(Bitmap face, FaceClassifier.Recognition recognition) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.register_face_dialogue);
        ImageView regImageView = dialog.findViewById(R.id.regImageView);
        EditText nameEditText = dialog.findViewById(R.id.nameEditText);
        Button regBtn = dialog.findViewById(R.id.regBtn);
        regImageView.setImageBitmap(face);

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nameEditText.getText().toString().equals("")) {
                    nameEditText.setError("Enter Name");
                } else {
                    faceClassifier.register(nameEditText.getText().toString(), recognition);
                    Toast.makeText(RegisterActivityMine.this, "Face is registered", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    public List<Bitmap> loadBitmapsFromAssets() {
        List<Bitmap> bitmaps = new ArrayList<>();
        AssetManager assetManager = getAssets();
        String[] imageFiles;

        try {
            imageFiles = assetManager.list("one_photo");
            if (imageFiles == null) return bitmaps;

            // Initialize the latch with the number of image files
            final CountDownLatch latch = new CountDownLatch(imageFiles.length);

            for (String imageFileName : imageFiles) {
                String imagePath = "one_photo/" + imageFileName;
                InputStream is = assetManager.open(imagePath);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                bitmap = rotateBitmap(bitmap);
                bitmaps.add(bitmap);

                // Normalize the name to ensure consistency in identification
                String normalizedFileName = normalizeName(imageFileName);
                performFaceDetection(bitmap, normalizedFileName, latch); // Pass the normalized name
                is.close();
            }

            // Wait for all tasks to complete
            new Thread(() -> {
                try {
                    latch.await();
                    runOnUiThread(() -> faceClassifier.finalizeEmbeddings());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmaps;
    }

    private String normalizeName(String fileName) {
        // Remove the numerical suffix and the file extension from the filename
        return fileName.replaceAll("(_\\d+)?(\\.[^.]+)$", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
