package com.example.attendanceproject.account.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.R;
import com.example.attendanceproject.UserAccountActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class StudentActivity extends AppCompatActivity {

    private TextView welcomeMessageTextView;
    private TextView profileCompletionTextView;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser user;

    // Define an ActivityResultLauncher for launching UserAccountActivity
    private ActivityResultLauncher<Intent> userAccountLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Perform your check when returning from UserAccountActivity
                    checkPictureAdded(user.getUid());
                }
            });


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = fAuth.getCurrentUser();

        welcomeMessageTextView = findViewById(R.id.welcomeMessage);
        profileCompletionTextView = findViewById(R.id.profileCompletionNotification);

        // Set the welcome message with user's display name
        if (user != null && user.getDisplayName() != null) {
            welcomeMessageTextView.setText("Welcome, " + user.getDisplayName() + "!");
        } else {
            welcomeMessageTextView.setText("Welcome, user!");
        }

        checkPictureAdded(user.getUid());

        profileCompletionTextView.setOnClickListener(view -> {
            Intent intent = new Intent(this, UserAccountActivity.class);
            userAccountLauncher.launch(intent);
        });

    }

    private void checkPictureAdded(String uid) {
        DocumentReference df = fStore.collection("Users").document(uid);
        df.get().addOnSuccessListener(documentSnapshot -> {
            // Check if the imageUrl exists and is not null
            if (documentSnapshot.contains("imageUrl") && documentSnapshot.getString("imageUrl") != null) {
                // imageUrl exists and is not null
                String imageUrl = documentSnapshot.getString("imageUrl");
                profileCompletionTextView.setVisibility(View.GONE);
            } else {
                // imageUrl does not exist or is null
                profileCompletionTextView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            // Handle any errors here
            Log.e("FirestoreError", "Error checking for image URL", e);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if the picture has been added every time the activity comes to the foreground
        checkPictureAdded(user.getUid());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkPictureAdded(user.getUid());
    }

}
