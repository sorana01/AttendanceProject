package com.example.attendanceproject.account.teacher;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private List<Uri> imageUris = new ArrayList<>();
    private Bitmap input;
    private RecyclerView recyclerViewRecognizedFaces;
    private FaceAdapter faceAdapter;
    private List<FaceItem> faceItemList;

    private String courseName, courseDetail, courseId;
    private int courseWeek;
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
    private ProgressBar progressBar;


    private ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            new ActivityResultCallback<List<Uri>>() {
                @Override
                public void onActivityResult(List<Uri> uris) {
                    // Handle the returned URIs
                    if (uris != null && !uris.isEmpty()) {
                        imageUris.clear();
                        imageUris.addAll(uris);
                        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar
                        processNextImage();
                    }
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        buttonSave = findViewById(R.id.buttonSave); // Initialize the Save button
        recyclerViewRecognizedFaces = findViewById(R.id.recyclerViewRecognizedFaces);
        progressBar = findViewById(R.id.progressBar);

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
        originalNames = new ArrayList<>();

        if (courseName != null) {
            getSupportActionBar().setTitle(courseName);
        }

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

        // Directly launch the gallery for selecting photos
        getContent.launch("image/*");


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
                    progressBar.setVisibility(View.VISIBLE); // Show ProgressBar
                    saveAttendanceToFirestore();
                    Log.d("NAMES", "Saved names: " + namesBuilder.toString());
                    faceItemList.clear();
                    originalNames.clear();
                    faceAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    restoreOriginalNames();
                });

        builder.show();
    }


    private void saveAttendanceToFirestore() {
        List<String> attendeeNames = faceItemList.stream()
                .map(FaceItem::getName)
                .collect(Collectors.toList());

        firestore.collection("AttendanceRecords")
                .whereEqualTo("courseID", FirebaseFirestore.getInstance().document("Courses/" + courseId))
                .whereEqualTo("week", courseWeek)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Document exists, append to the attendees list
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            List<String> existingAttendees = (List<String>) document.get("attendees");
                            if (existingAttendees == null) {
                                existingAttendees = new ArrayList<>();
                            }
                            existingAttendees.addAll(attendeeNames);
                            document.getReference().update("attendees", existingAttendees)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore Attendance", "DocumentSnapshot successfully updated with new attendees!");
                                        updateUsersAttendance(attendeeNames, FirebaseFirestore.getInstance().document("Courses/" + courseId));
                                        progressBar.setVisibility(View.GONE);
                                    })
                                    .addOnFailureListener(e -> Log.w("Firestore", "Error updating document", e));
                        }
                    } else {
                        // No document exists, create a new one
                        Map<String, Object> attendanceRecord = new HashMap<>();
                        attendanceRecord.put("courseID", FirebaseFirestore.getInstance().document("Courses/" + courseId));
                        attendanceRecord.put("courseName", courseName);
                        attendanceRecord.put("week", courseWeek);
                        attendanceRecord.put("date", new Timestamp(new Date()));
                        attendanceRecord.put("attendees", attendeeNames);

                        firestore.collection("AttendanceRecords")
                                .add(attendanceRecord)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("Firestore Attendance", "DocumentSnapshot written with ID: " + documentReference.getId());
                                    Log.d("Firestore Attendance", "Course id passed " + courseId);
                                    updateUsersAttendance(attendeeNames, FirebaseFirestore.getInstance().document("Courses/" + courseId));
                                    progressBar.setVisibility(View.GONE);
                                    launchViewAttendanceActivity(); // Open ViewAttendanceActivity after saving
                                })
                                .addOnFailureListener(e -> Log.w("Firestore", "Error adding document", e));
                    }
                })
                .addOnFailureListener(e -> Log.w("Firestore", "Error checking for existing document", e));
    }

    private void updateUsersAttendance(List<String> attendeeNames, DocumentReference courseDocRef) {
        Log.d("Firestore User Attendance", "Attendee names: " + attendeeNames);
        Log.d("Firestore User Attendance", "Course Path: " + courseDocRef.getPath());

        for (String attendeeName : attendeeNames) {
            firestore.collection("Users")
                    .whereEqualTo("FullName", attendeeName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.w("Firestore User Attendance", "No user found with name: " + attendeeName);
                            return;
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            DocumentReference userDocRef = document.getReference();
                            Log.d("Firestore User Attendance", "User found: " + userDocRef.getId());

                            userDocRef.collection("CoursesEnrolled")
                                    .whereEqualTo("courseReference", courseDocRef) // Ensure the path matches exactly
                                    .get()
                                    .addOnSuccessListener(coursesSnapshot -> {
                                        if (coursesSnapshot.isEmpty()) {
                                            Log.w("Firestore User Attendance", "No enrolled course found for user: " + userDocRef.getId());
                                            return;
                                        }
                                        for (QueryDocumentSnapshot courseDoc : coursesSnapshot) {
                                            DocumentReference userCourseDocRef = courseDoc.getReference();
                                            Log.d("Firestore User Attendance", "Course found for user: " + userCourseDocRef.getId());

                                            // Check if attendance for the week already exists
                                            userCourseDocRef.collection("Attendance")
                                                    .whereEqualTo("week", courseWeek)
                                                    .get()
                                                    .addOnSuccessListener(attendanceSnapshot -> {
                                                        if (attendanceSnapshot.isEmpty()) {
                                                            // Attendance does not exist, add new entry
                                                            Map<String, Object> attendanceData = new HashMap<>();
                                                            attendanceData.put("week", courseWeek);
                                                            attendanceData.put("status", "Present");

                                                            userCourseDocRef.collection("Attendance")
                                                                    .add(attendanceData)
                                                                    .addOnSuccessListener(aVoid -> Log.d("Firestore User Attendance", "User attendance successfully updated!"))
                                                                    .addOnFailureListener(e -> Log.w("Firestore User Attendance", "Error updating user attendance", e));
                                                        } else {
                                                            Log.d("Firestore User Attendance", "Attendance already exists for week: " + courseWeek + " for user: " + userDocRef.getId());
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Log.w("Firestore User Attendance", "Error checking attendance for user", e));
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.w("Firestore User Attendance", "Error finding enrolled course", e));
                        }
                    })
                    .addOnFailureListener(e -> Log.w("Firestore User Attendance", "Error finding user", e));
        }
    }

    private void restoreOriginalNames() {
        for (int i = 0; i < faceItemList.size(); i++) {
            faceItemList.get(i).setName(originalNames.get(i));
        }
        faceAdapter.notifyDataSetChanged();
    }

    private void launchViewAttendanceActivity() {
        Intent intent = new Intent(this, ViewAttendanceTeacherActivity.class);
        intent.putExtra("courseId", courseId);
        intent.putExtra("courseName", courseName);
        intent.putExtra("courseDetail", courseDetail);
        intent.putExtra("courseWeek", courseWeek);
        startActivity(intent);
        finish();
    }


    private void processNextImage() {
        if (!imageUris.isEmpty()) {
            Uri nextUri = imageUris.remove(0);
            input = uriToBitmap(nextUri);
            input = rotateBitmap(input, nextUri);
            detectSingleFace(input);
        } else {
            // All images processed, show the RecyclerView
            recyclerViewRecognizedFaces.setVisibility(View.VISIBLE);
            buttonSave.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
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
                    }
                    processNextImage();  // Automatically process the next image
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RecognizeActivity.this, "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    processNextImage();  // Automatically process the next image
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
            FaceItem faceItem = new FaceItem(croppedFace, recognizedName);
            faceItemList.add(faceItem);
            originalNames.add(recognizedName); // Add the original name to the list
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
    public Bitmap rotateBitmap(Bitmap input, Uri imageUri){
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
