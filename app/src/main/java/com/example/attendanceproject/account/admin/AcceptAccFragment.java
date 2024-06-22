package com.example.attendanceproject.account.admin;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.attendanceproject.account.adapters.AcceptAccEntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.example.attendanceproject.account.adapters.StudentItem;
import com.example.attendanceproject.account.adapters.TeacherItem;
import com.example.attendanceproject.databinding.FragmentAcceptAccBinding;
import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AcceptAccFragment extends Fragment {
    private FirebaseFirestore fStore;
    private AcceptAccEntityAdapter adapter;
    private ArrayList<EntityItem> entityItems;
    private FragmentAcceptAccBinding binding;
    private Map<String, String> userMap = new HashMap<>();
    private FaceClassifier faceClassifier;
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    FaceDetector detector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once
        loadUsers(); // Load users initially here, only once when the fragment is first created
        entityItems = new ArrayList<>();
        detector = FaceDetection.getClient(highAccuracyOpts);


        try {
            // CHANGE MODEL
            faceClassifier = TFLiteFaceRecognition.create(getContext().getAssets(), "facenet.tflite", 160, false, getContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAcceptAccBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AcceptAccEntityAdapter(getContext(), entityItems);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            String userId = item.getEntityId();
            String fullName = item.getEntityName();
            String userDetail = item.getEntityName();
            String imageUrl = null;
            Uri imageUri = null;
            boolean isStudent = item instanceof StudentItem;

            if (isStudent) {
                imageUrl = ((StudentItem) item).getImageUrl();
                imageUri = Uri.parse(imageUrl);
                if (imageUri != null && imageUrl != null) {
                    showApprovalDialog(userId, fullName, userDetail, isStudent, imageUri);
                }
                else {
                    Toast.makeText(getContext(), "Image doesn't exist", Toast.LENGTH_SHORT).show();
                }
            } else {
                showApprovalDialog(userId, fullName, userDetail, isStudent, imageUri);
            }

        });
    }

    private void loadUsers() {
        fStore.collection("Users").whereEqualTo("isApproved", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userDetail = document.getString("FullName") + " - ";
                            userDetail += Boolean.TRUE.equals(document.getBoolean("isTeacher")) ? "Teacher" : "Student";
                            boolean isTeacher = Boolean.TRUE.equals(document.getBoolean("isTeacher"));
                            String userId = document.getId();
                            String fullName = document.getString("FullName");
                            String phoneNumber = document.getString("PhoneNumber");

                            userMap.put(userDetail, userId);

                            if (isTeacher) {
                                entityItems.add(new TeacherItem(fullName, phoneNumber, userId));
                            } else {
                                String ssn = document.getString("SSN");
                                String studentId = document.getString("StudentId");
                                String imageUrl = document.getString("imageUrl");
                                entityItems.add(new StudentItem(fullName, phoneNumber, ssn, studentId, imageUrl, userId));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showApprovalDialog(String userId, String fullName, String userDetail, boolean isStudent, Uri imageUri) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Approve User");
        builder.setMessage("Do you want to approve " + userDetail + "?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (userId != null) {
                if (isStudent) {
                    if (imageUri != null) {
                        generateAndStoreEmb(imageUri, fullName);
                    }
                }
                updateUserStatus(userId, "true");
            } else {
                Toast.makeText(getContext(), "User ID is null, cannot update status", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            if (userId != null) {
                updateUserStatus(userId, "false");
            } else {
                Toast.makeText(getContext(), "User ID is null, cannot update status", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void updateUserStatus(String userId, String status) {
        fStore.collection("Users").document(userId)
                .update("isApproved", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "User status updated", Toast.LENGTH_SHORT).show();
                    reloadUserList();  // Refresh list after update
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating user status", Toast.LENGTH_SHORT).show());
    }

    private void generateAndStoreEmb(Uri imageUri, String fullName) {
        Glide.with(getContext())
                .asBitmap()
                .load(imageUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap input = resource;
                        input = rotateBitmap(input, imageUri);  // Assuming you handle rotation correctly
                        detectFace(input, fullName);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e("BitmapError", "Failed to decode bitmap from URI");
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void detectFace(Bitmap bitmap, String fullName) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Detect faces in the image
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 1) {
                        Face face = faces.get(0);
                        Rect bounds = face.getBoundingBox();

                        performFaceRecognition(bounds, bitmap, fullName);
                    }
                    else {
                        Toast.makeText(getContext(), "Not a single face detected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performFaceRecognition(Rect bound, Bitmap input, String fullName) {
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
        croppedFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, false);
        FaceClassifier.Recognition recognition = faceClassifier.recognizeImage(getContext(), croppedFace, true);
        Log.d("INSIDE REGISTER", "Recognition object value " + recognition);
        faceClassifier.registerDb(fullName, recognition, getContext());
    }

    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input, Uri imageUri){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = null;
        int orientation = -1;

        if (getContext() != null) {
            cur = requireContext().getContentResolver().query(imageUri, orientationColumn, null, null, null);
        }

        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
            cur.close();
        }
        Log.d("tryOrientation",orientation+"");

        Matrix rotationMatrix = new Matrix();
        if (orientation != -1) {
            rotationMatrix.setRotate(orientation);
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return rotatedBitmap;
    }


    private void reloadUserList() {
        entityItems.clear();
        userMap.clear();
        loadUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
