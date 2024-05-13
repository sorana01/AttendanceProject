package com.example.attendanceproject.account.teacher;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.adapters.FaceAdapter;
import com.example.attendanceproject.account.adapters.FaceItem;
import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.Timestamp;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecognizeActivity extends AppCompatActivity implements FaceAdapter.OnEditTextChanged {
    private PhotoView groupPhotoImageView;
    private Button buttonRecognize, buttonSave;
    private Canvas canvas;
    private Uri imageUri;
    private Bitmap input;
    private RecyclerView recyclerViewRecognizedFaces;
    private FaceAdapter faceAdapter;
    private List<FaceItem> faceItemList;

    private String courseName, courseDetail, courseId;
    private int courseWeek;
    private AttendanceManager attendanceManager;
    private List<String> originalNames;


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
                        buttonRecognize.setVisibility(View.GONE); // Hide the Recognize/Add photo button
                        buttonSave.setVisibility(View.VISIBLE); // Show the Save button
                    }
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        groupPhotoImageView = findViewById(R.id.groupPhotoImageView);
        buttonRecognize = findViewById(R.id.buttonRecognize);
        buttonSave = findViewById(R.id.buttonSave); // Initialize the Save button
        recyclerViewRecognizedFaces = findViewById(R.id.recyclerViewRecognizedFaces);

        faceItemList = new ArrayList<>();
        faceAdapter = new FaceAdapter(this, faceItemList, this);
        recyclerViewRecognizedFaces.setAdapter(faceAdapter);
        recyclerViewRecognizedFaces.setLayoutManager(new LinearLayoutManager(this));

        user = FirebaseAuth.getInstance().getCurrentUser();
        detector = FaceDetection.getClient(highAccuracyOpts);

        // Get data passed from fragment
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");
        courseDetail = getIntent().getStringExtra("courseDetail");
        courseWeek = getIntent().getIntExtra("courseWeek", 1);
        attendanceManager = new AttendanceManager(this);
        originalNames = new ArrayList<>();

        // Set up onBackPressed handling with callback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if the current focus is on an EditText
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {
                    // Clear focus and potentially hide the keyboard
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);

                    // Make the Save button visible again
                    buttonSave.setVisibility(View.VISIBLE);
                } else {
                    // If no EditText was focused, continue with the default back action
                    setEnabled(false);
                    onBackPressed();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        try {
            // CHANGE MODEL
            faceClassifier = TFLiteFaceRecognition.createDb(getAssets(), "facenet.tflite", 160, false, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        buttonRecognize.setOnClickListener(view -> {
            getContent.launch("image/*");
        });


        buttonSave.setOnClickListener(view -> showSaveConfirmationDialog());
    }

    private void showSaveConfirmationDialog() {
        StringBuilder namesBuilder = new StringBuilder();
        for (FaceItem item : faceItemList) {
            namesBuilder.append(item.getName()).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Confirm Save")
                .setMessage("Do you want to save these names?\n" + namesBuilder.toString())
                .setPositiveButton("Save", (dialog, which) -> {
                    saveAttendanceToFirestore();
                    Log.d("NAMES", "Saved names: " + namesBuilder.toString());
                    buttonSave.setVisibility(View.GONE);
                    buttonRecognize.setVisibility(View.VISIBLE);
                    faceItemList.clear();
                    originalNames.clear();
                    faceAdapter.notifyDataSetChanged();
                    groupPhotoImageView.setImageResource(R.drawable.poza_grup);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Restore original names if user cancels the operation
                    restoreOriginalNames();
                });


        builder.show();

    }

    private void saveAttendanceToFirestore() {
        List<String> attendeeNames = faceItemList.stream()
                .map(FaceItem::getName)
                .collect(Collectors.toList());

        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("courseID", FirebaseFirestore.getInstance().document("Courses/" + courseId));
        attendanceRecord.put("week", courseWeek);
        attendanceRecord.put("date", new Timestamp(new Date()));
        attendanceRecord.put("attendees", attendeeNames);

        firestore.collection("AttendanceRecords")
                .add(attendanceRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore Attendance", "DocumentSnapshot written with ID: " + documentReference.getId());
                    Log.d("Firestore Attendance", "Course id passed " + courseId);
                })
                .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));
    }

    private void restoreOriginalNames() {
        for (int i = 0; i < faceItemList.size(); i++) {
            faceItemList.get(i).setName(originalNames.get(i));
        }
        faceAdapter.notifyDataSetChanged();
    }

    private void detectSingleFace(Bitmap bitmap) {
        // to be able to draw on the image
        Bitmap mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutableBmp);
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
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

                        for (FaceItem face : faceItemList) {
                            originalNames.add(face.getName());
                        }
                    }
                    else {
                        groupPhotoImageView.setImageBitmap(mutableBmp);
                        Toast.makeText(this, "Picture contains no faces", Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RecognizeActivity.this, "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // TODO get the names
        // TODO excel attendance export
        if (recognition != null) {
            Log.d("FaceRecognition", recognition.getTitle() + "   " + recognition.getDistance());
            Paint p1 = new Paint();
            p1.setColor(Color.WHITE);
            p1.setTextSize(35);
            String recognizedName = recognition.getDistance() < 1 ? recognition.getTitle() : "Unknown";
            canvas.drawText(recognizedName, bound.left, bound.top, p1);
            // Update RecyclerView with new recognized face
            faceItemList.add(new FaceItem(croppedFace, recognizedName));
            faceAdapter.notifyDataSetChanged();
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


    @Override
    public void onEditTextVisibilityChange(boolean shouldShowSave) {
        if (shouldShowSave) {
            buttonSave.setVisibility(View.VISIBLE);
        } else {
            buttonSave.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // Check if any EditText has focus
        if (getCurrentFocus() instanceof EditText) {
            super.onBackPressed();
            buttonSave.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

}
