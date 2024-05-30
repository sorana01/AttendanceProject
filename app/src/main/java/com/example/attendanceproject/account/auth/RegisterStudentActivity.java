package com.example.attendanceproject.account.auth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class RegisterStudentActivity extends AppCompatActivity {
    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText;
    private EditText studentIdEditText, cnpEditText;
    private Button registerButton, goToLoginButton, uploadPhotoButton, chooseAnotherPhotoButton;
    private ImageView chosenPhotoImageView;

    private Uri imageUri;
    private String recognitionJson;

    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseAuth fAuth;
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private FaceClassifier faceClassifier;
    private FaceClassifier.Recognition recognition;
    private ActivityResultLauncher<Intent> userAccountActivityResultLauncher;
    private static final int PICK_IMAGE_REQUEST = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

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

//        try {
//            // CHANGE MODEL
//            faceClassifier = TFLiteFaceRecognition.createDb(getAssets(), "facenet.tflite", 160, false, this);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        userAccountActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String imageUriString = data.getStringExtra("imageUri");
                            recognitionJson = data.getStringExtra("recognition");

                            if (imageUriString != null) {
                                imageUri = Uri.parse(imageUriString);
                                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                // Load image into ImageView (use a library like Glide or Picasso)
                                chosenPhotoImageView.setImageURI(imageUri);
                                uploadPhotoButton.setVisibility(View.GONE);
                                chooseAnotherPhotoButton.setVisibility(View.VISIBLE);
                                chosenPhotoImageView.setVisibility(View.VISIBLE);

                                updateConstraints();
                            }

                            if (recognitionJson != null) {
                                // Deserialize JSON back into Recognition object
                                Gson gson = new Gson();
                                Type type = new TypeToken<FaceClassifier.Recognition>(){}.getType();
                                recognition = gson.fromJson(recognitionJson, type);
                            }

                            Log.d("RECEIVED", "Data from UserAccountActivity: recognition" + recognition +" imageuri " + imageUri);
                        }
                    }
                }
        );

        uploadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterStudentActivity.this, UserAccountActivity.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                userAccountActivityResultLauncher.launch(intent);
            }
        });

        chooseAnotherPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterStudentActivity.this, UserAccountActivity.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                userAccountActivityResultLauncher.launch(intent);            }
        });

        registerButton.setOnClickListener(view -> {
            boolean valid = imageUri != null && emptyField(fullNameEditText) && emptyField(emailEditText) && emptyField(passwordEditText) && emptyField(phoneEditText) && emptyField(cnpEditText) && emptyField(studentIdEditText);

            if (valid) {
                if (passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
                    checkPhoneNumberAndRegister();
                } else {
                    confirmPasswordEditText.setError("Passwords have to match");
                }
            } else if (imageUri == null) {
                Toast.makeText(RegisterStudentActivity.this, "You must upload a photo of yourself!", Toast.LENGTH_SHORT).show();
            }
        });

        goToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
            }
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

        // Directly use the received recognition JSON
        if (recognitionJson != null) {
            userInfo.put("Recognition", recognitionJson);
        }

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
//        faceClassifier.registerDb(fullNameEditText.getText().toString(), recognition, RegisterStudentActivity.this);

        df.set(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterStudentActivity.this, "User information saved in Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore Save Error", "Failed to save user data", e);
                });
    }


    private void uploadImageToFirebaseStorage() {
        if (recognition != null && imageUri != null) {
            faceClassifier.registerDb(fullNameEditText.getText().toString(), recognition, RegisterStudentActivity.this);

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
                        saveImageDetailsToFirestore(downloadUrl, fileName);  // Save other details to Firestore as needed

                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(RegisterStudentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.d("Recognition save error", "Recognition " + recognition + " image uri " + imageUri);
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageDetailsToFirestore(String imageUrl, String fileName) {
        if (user != null) {
            DocumentReference df = fStore.collection("Users").document(user.getUid());
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("imageUrl", imageUrl);
            imageData.put("fileName", fileName);

            // Use update instead of set to keep other fields unchanged
            df.update(imageData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "Document successfully updated!");
                            setResult(Activity.RESULT_OK);  // Set the result as OK
                            finish();  // Finish this activity only after successful Firestore update
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Firestore", "Error updating document", e);
                            Toast.makeText(RegisterStudentActivity.this, "Failed to update Firestore. Please try again!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }


    public boolean emptyField(EditText textField) {
        if (textField.getText().toString().isEmpty()) {
            textField.setError("This field cannot be empty");
            return false;
        } else {
            return true;
        }
    }
}
