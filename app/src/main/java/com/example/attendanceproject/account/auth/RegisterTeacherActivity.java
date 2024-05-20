package com.example.attendanceproject.account.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.attendanceproject.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterTeacherActivity extends AppCompatActivity {
    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText;
    private Button registerButton, goToLoginButton;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_teacher);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        fullNameEditText = findViewById(R.id.registerName);
        emailEditText = findViewById(R.id.registerEmail);
        passwordEditText = findViewById(R.id.registerPassword);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        phoneEditText = findViewById(R.id.registerPhone);
        registerButton = findViewById(R.id.registerBtn);
        goToLoginButton = findViewById(R.id.gotoLogin);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean valid = emptyField(fullNameEditText) && emptyField(emailEditText) && emptyField(passwordEditText) && emptyField(phoneEditText);

                if (valid) {
                    if (passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
                        fAuth.createUserWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                FirebaseUser user = fAuth.getCurrentUser();
                                if (user != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(fullNameEditText.getText().toString())
                                            .build();

                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Log.d("Profile Update", "User profile updated with display name.");
                                                }
                                            });

                                    DocumentReference df = fStore.collection("Users").document(user.getUid());
                                    Map<String, Object> userInfo = new HashMap<>();
                                    userInfo.put("FullName", fullNameEditText.getText().toString());
                                    userInfo.put("UserEmail", emailEditText.getText().toString());
                                    userInfo.put("PhoneNumber", phoneEditText.getText().toString());
                                    userInfo.put("isTeacher", true);
                                    userInfo.put("isApproved", "pending");

                                    df.set(userInfo).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterTeacherActivity.this, "User information saved in Firestore", Toast.LENGTH_SHORT).show();
                                    }).addOnFailureListener(e -> {
                                        Log.e("Firestore Save Error", "Failed to save user data", e);
                                    });
                                }

                                startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(RegisterTeacherActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Firestore Save Error", "Failed to save user data", e);
                            }
                        });
                    } else {
                        confirmPasswordEditText.setError("Passwords have to match");
                    }
                }
            }
        });

        goToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
            }
        });
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
