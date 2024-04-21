package com.example.attendanceproject;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UserAccountActivity extends AppCompatActivity {
    private ImageView userImageView;
    private Button buttonSavePhoto;
    private Button buttonChoosePhoto;
    private Button buttonRecognize;
    private Uri imageUri;
    private Bitmap input;
    private Canvas canvas;
    private boolean registerFace;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // High-accuracy landmark detection and face classification
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    FaceDetector detector;
    FaceClassifier faceClassifier;
    FirebaseUser user;
    FaceClassifier.Recognition recognition;

    // Define an ActivityResultLauncher
    private ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    if (uri != null) {
                        imageUri = uri;
                        input = uriToBitmap(imageUri);
                        input = rotateBitmap(input);
                        userImageView.setImageBitmap(input);

                        detectSingleFace(input);
                    }
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);
        userImageView = findViewById(R.id.userImage);
        buttonSavePhoto = findViewById(R.id.buttonSavePhoto);
        buttonChoosePhoto = findViewById(R.id.buttonChoosePhoto);
        buttonRecognize = findViewById(R.id.buttonRecognize);

        buttonRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContent.launch("image/*");
            }
        });

        buttonChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContent.launch("image/*");
                registerFace = true;
            }
        });

        buttonSavePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Disable the Save button immediately to prevent multiple uploads
                buttonSavePhoto.setEnabled(false);
                faceClassifier.registerDb(user.getDisplayName(), recognition, UserAccountActivity.this);


                // Clear the imageUri and update the ImageView
                imageUri = null;
                input = null;
                userImageView.setImageBitmap(input);
                buttonChoosePhoto.setVisibility(View.VISIBLE);
                buttonSavePhoto.setVisibility(View.GONE);
                registerFace = false;
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        detector = FaceDetection.getClient(highAccuracyOpts);
        try {
            // CHANGE MODEL
            faceClassifier = TFLiteFaceRecognition.createDb(getAssets(), "facenet.tflite", 160, false, UserAccountActivity.this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void detectSingleFace(Bitmap bitmap) {
        // to be able to draw on the image
        Bitmap mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutableBmp);
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    for (Face face : faces) {
                        Rect bounds = face.getBoundingBox();
                        Paint p1 = new Paint();
                        p1.setColor(Color.RED);
                        p1.setStyle(Paint.Style.STROKE);
                        p1.setStrokeWidth(15);

                        if (registerFace) {
                            if (faces.size() == 1) {
//                                Face face = faces.get(0);
                                // Compute the embeddings
                                performFaceRecognition(face.getBoundingBox(), bitmap);
                                // Exactly one face detected, allow user to save the image
                                buttonChoosePhoto.setVisibility(View.GONE);
                                buttonSavePhoto.setVisibility(View.VISIBLE);
                                buttonSavePhoto.setEnabled(true);
                            } else {
                                // Not exactly one face, show error or handle otherwise
                                Toast.makeText(UserAccountActivity.this, "Please select an image with exactly one face.", Toast.LENGTH_LONG).show();
                                // Optionally, you might want to reset to the initial state
                                buttonChoosePhoto.setVisibility(View.VISIBLE);
                                buttonSavePhoto.setVisibility(View.GONE);
                                buttonSavePhoto.setEnabled(false);
                                //                        userImageView.setImageBitmap(null);
                            }
                        }
                        else {
                            performFaceRecognition(bounds, bitmap);
                        }
                        canvas.drawRect(bounds, p1);
                    }
                    userImageView.setImageBitmap(mutableBmp);


                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserAccountActivity.this, "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageToFirebaseStorage(FaceClassifier.Recognition recognition) {
        if (user != null && imageUri != null) {
            String fileName = "profileImages/" + System.currentTimeMillis() + "." + getFileExtension(imageUri);
            StorageReference storageReference = storage.getReference();
            StorageReference fileReference = storageReference.child(fileName);

            // Set custom metadata
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("fullName", user.getDisplayName())
                    .build();

            fileReference.putFile(imageUri, metadata)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        saveImageDetailsToFirestore(downloadUrl, fileName, recognition);  // Save other details to Firestore as needed

                        // Clear the imageUri and update the ImageView
                        imageUri = null;
                        input = null;
                        userImageView.setImageBitmap(input);
                        buttonChoosePhoto.setVisibility(View.VISIBLE);
                        buttonSavePhoto.setVisibility(View.GONE);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserAccountActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }



    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void saveImageDetailsToFirestore(String imageUrl, String fileName, FaceClassifier.Recognition recognition) {
        if (user != null) {
            DocumentReference userDocRef = firestore.collection("users").document(user.getUid());
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("imageUrl", imageUrl);
            imageData.put("fileName", fileName);

            // Use the helper method to convert embeddings to a string
            float[][] embeddings = (float[][]) recognition.getEmbedding();
            imageData.put("embeddings", convertEmbeddingsToString(embeddings));

            // Add to an array of images
            userDocRef.update("images", FieldValue.arrayUnion(imageData))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserAccountActivity.this, "Image details saved in Firestore", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserAccountActivity.this, "Error saving image details", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    public void performFaceRecognition(Rect bound, Bitmap input) {
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
        // CHANGE MODEL
        croppedFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, false);
        recognition = faceClassifier.recognizeImageRec(croppedFace, true);
        Log.d("INSIDE REGISTER", "Recognition object value " + recognition);
        // Call upload method here with embedding data
//        uploadImageToFirebaseStorage(recognition);

        // excel attendance export
        if (!registerFace) {
            if (recognition != null) {
                Log.d("FaceRecognition", recognition.getTitle() + "   " + recognition.getDistance());
                Paint p1 = new Paint();
                p1.setColor(Color.WHITE);
                p1.setTextSize(35);
                if (recognition.getDistance() < 1) {
                    canvas.drawText(recognition.getTitle(), bound.left, bound.top, p1);
                } else {
                    canvas.drawText("Unknown", bound.left, bound.top, p1);
                }
            }
        }
    }

    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(imageUri, orientationColumn, null, null, null);
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

    private String convertEmbeddingsToString(float[][] embeddings) {
        if (embeddings == null || embeddings.length == 0) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embeddings.length; i++) {
            sb.append(Arrays.toString(embeddings[i]));
            if (i < embeddings.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
