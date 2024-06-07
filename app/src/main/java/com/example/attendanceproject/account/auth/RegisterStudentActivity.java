package com.example.attendanceproject.account.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.attendanceproject.R;
import com.example.attendanceproject.face_rec.FaceClassifier;
import com.example.attendanceproject.face_rec.TFLiteFaceRecognition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RegisterStudentActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_MEDIA_IMAGES = 101;  // Arbitrary number, unique to this request
    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText;
    private EditText studentIdEditText, cnpEditText;
    private Button registerButton, goToLoginButton, uploadPhotoButton, chooseAnotherPhotoButton;
    private ImageView chosenPhotoImageView;
    private Bitmap input;

    private Uri imageUri;
    private String recognitionJson;
    private boolean oneFace;

    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseAuth fAuth;
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    FaceDetector detector;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        detector = FaceDetection.getClient(highAccuracyOpts);


        fullNameEditText = findViewById(R.id.registerName);
        emailEditText = findViewById(R.id.registerEmail);
        passwordEditText = findViewById(R.id.registerPassword);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        phoneEditText = findViewById(R.id.registerPhone);
        studentIdEditText = findViewById(R.id.registerStudentId);
        cnpEditText = findViewById(R.id.registerCnp);
        registerButton = findViewById(R.id.registerBtn);
        goToLoginButton = findViewById(R.id.gotoLogin);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        chooseAnotherPhotoButton = findViewById(R.id.chooseAnotherPhotoButton);
        chosenPhotoImageView = findViewById(R.id.chosenPhotoImageView);


        // Initialize the launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            imageUri = selectedImageUri;
                            input = uriToBitmap(imageUri);
                            input = rotateBitmap(input);
                            chosenPhotoImageView.setImageBitmap(input);
                            uploadPhotoButton.setVisibility(View.GONE);
                            chosenPhotoImageView.setVisibility(View.VISIBLE);
                            chooseAnotherPhotoButton.setVisibility(View.VISIBLE);

                            updateConstraints();
                            detectSingleFace(input);
                        }
                    }
                }
        );

        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePhotoUpload();
            }
        });

        chooseAnotherPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePhotoUpload();
            }
        });

        registerButton.setOnClickListener(view -> {
            boolean valid = !(emptyField(fullNameEditText) && emptyField(emailEditText) && emptyField(phoneEditText) && emptyField(cnpEditText) && emptyField(studentIdEditText) && emptyField(passwordEditText) && emptyField(confirmPasswordEditText));

            if (valid && passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
                if (imageUri != null) {
                    if (oneFace) {
                        checkPhoneNumberAndRegister();
                    }
                    else {
                        Toast.makeText(RegisterStudentActivity.this, "You must upload a picture alone!", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(RegisterStudentActivity.this, "You must upload a picture!", Toast.LENGTH_SHORT).show();
                }
            } else {
                confirmPasswordEditText.setError("Passwords have to match");
            }
        });

        goToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
            }
        });
    }

    // Override onRequestPermissionsResult to handle the callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Ensure permission flags are correctly set
                pickImageLauncher.launch(intent);
            } else {
                Toast.makeText(RegisterStudentActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handlePhotoUpload() {
        if (ContextCompat.checkSelfPermission(RegisterStudentActivity.this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RegisterStudentActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Ensure permission flags are correctly set
            pickImageLauncher.launch(intent);
        }
    }

    private void detectSingleFace(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Detect faces in the image
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 1) {
                        oneFace = true;
                    } else {
                        oneFace = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterStudentActivity.this, "Failed to detect faces: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    private void checkPhoneNumberAndRegister() {
        String phoneNumber = phoneEditText.getText().toString();
        fStore.collection("Users")
                .whereEqualTo("PhoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Toast.makeText(RegisterStudentActivity.this, "Phone number already registered.", Toast.LENGTH_SHORT).show();
                    } else {
                        registerNewUser();
                    }
                });
    }

    private void registerNewUser() {
        fAuth.createUserWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                user = fAuth.getCurrentUser();
                if (user != null) {
                    saveUserData();
                }

                startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterStudentActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Firestore Save Error", "Failed to save user data", e);
            }
        });
    }

    // Method to update constraints dynamically
    private void updateConstraints() {
        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        if (uploadPhotoButton.getVisibility() == View.GONE) {
            constraintSet.connect(R.id.registerBtn, ConstraintSet.TOP, R.id.chooseAnotherPhotoButton, ConstraintSet.BOTTOM, 32);
        } else {
            constraintSet.connect(R.id.registerBtn, ConstraintSet.TOP, R.id.uploadPhotoButton, ConstraintSet.BOTTOM, 32);
        }

        constraintSet.applyTo(constraintLayout);
    }

    private void saveUserData() {
        if (user == null) {
            Toast.makeText(RegisterStudentActivity.this, "User is null", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullNameEditText.getText().toString())
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Profile Update", "User profile updated with display name.");
                    }
                });

        // Create user information map
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("FullName", fullNameEditText.getText().toString());
        userInfo.put("UserEmail", emailEditText.getText().toString());
        userInfo.put("PhoneNumber", phoneEditText.getText().toString());
        userInfo.put("SSN", cnpEditText.getText().toString());
        userInfo.put("StudentId", studentIdEditText.getText().toString());
        userInfo.put("isStudent", true);
        userInfo.put("isApproved", "pending");


        // Upload image to Firebase Storage if imageUri is not null
        if (imageUri != null) {
            String fileName = "profileImages/" + System.currentTimeMillis() + "." + getFileExtension(imageUri);
            StorageReference storageReference = storage.getReference();
            StorageReference fileReference = storageReference.child(fileName);

            Log.d("RegisterStudentActivity", "User logged in " + user.getEmail());

            // Set custom metadata
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("fullName", fullNameEditText.getText().toString())
                    .build();

            fileReference.putFile(imageUri, metadata)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        userInfo.put("imageUrl", downloadUrl);
                        userInfo.put("fileName", fileName);

                        // Save user information to Firestore including imageUrl and fileName
                        saveUserInfoToFirestore(userInfo);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(RegisterStudentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(RegisterStudentActivity.this, "Image not selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserInfoToFirestore(Map<String, Object> userInfo) {
        DocumentReference df = fStore.collection("Users").document(user.getUid());
        df.set(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterStudentActivity.this, "User information saved in Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore Save Error", "Failed to save user data", e);
                });
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
        Bitmap rotatedBitmap = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return rotatedBitmap;
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    public boolean emptyField(EditText textField) {
        if (textField.getText().toString().isEmpty()) {
            textField.setError("This field cannot be empty");
            return true;
        } else {
            return false;
        }
    }
}
