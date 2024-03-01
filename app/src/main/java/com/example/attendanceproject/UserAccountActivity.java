package com.example.attendanceproject;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UserAccountActivity extends AppCompatActivity {
    private ImageView userImageView;
    private Button buttonSavePhoto;
    private Button buttonChoosePhoto;

    private Uri imageUri;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Define an ActivityResultLauncher
    private ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    if (uri != null) {
                        imageUri = uri;
                        userImageView.setImageURI(imageUri);
                        buttonChoosePhoto.setVisibility(View.GONE);
                        buttonSavePhoto.setVisibility(View.VISIBLE);
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

        buttonChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContent.launch("image/*");
            }
        });

        buttonSavePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageToFirebaseStorage();
            }
        });
    }

    private void uploadImageToFirebaseStorage() {
        if (imageUri != null) {
            StorageReference storageReference = storage.getReference();
            StorageReference fileReference = storageReference.child("profileImages/" + System.currentTimeMillis() + "." + getFileExtension(imageUri));

            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                saveImageUrlToFirestore(downloadUrl);
            })).addOnFailureListener(e -> {
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

    private void saveImageUrlToFirestore(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DocumentReference userDocRef = firestore.collection("users").document(user.getUid());
            userDocRef.update("photoURL", imageUrl).addOnSuccessListener(aVoid -> {
                Toast.makeText(UserAccountActivity.this, "Image saved in Firestore", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(UserAccountActivity.this, "Error saving image in Firestore", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
