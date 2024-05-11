package com.example.attendanceproject.account.teacher;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.R;
import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;

public class TeacherActivity extends AppCompatActivity {
    private ImageView groupPhotoImageView;
    private Button buttonRecognize;
    private Canvas canvas;
    private Uri imageUri;
    private Bitmap input;

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    private FaceDetector detector;
    private FaceClassifier faceClassifier;
    private FirebaseUser user;
    private FaceClassifier.Recognition recognition;


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
                        groupPhotoImageView.setImageBitmap(input);

                        detectSingleFace(input);
                    }
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

        groupPhotoImageView = findViewById(R.id.groupPhotoImageView);
        buttonRecognize = findViewById(R.id.buttonRecognize);

        user = FirebaseAuth.getInstance().getCurrentUser();
        detector = FaceDetection.getClient(highAccuracyOpts);
        try {
            // CHANGE MODEL
            faceClassifier = TFLiteFaceRecognition.createDb(getAssets(), "facenet.tflite", 160, false, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        buttonRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContent.launch("image/*");
            }
        });
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

                        performFaceRecognition(bounds, bitmap);
                        canvas.drawRect(bounds, p1);
                    }
                    groupPhotoImageView.setImageBitmap(mutableBmp);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TeacherActivity.this, "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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


}
