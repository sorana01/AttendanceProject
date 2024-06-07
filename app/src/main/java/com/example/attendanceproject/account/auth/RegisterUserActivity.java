package com.example.attendanceproject.account.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class RegisterUserActivity extends AppCompatActivity {
    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText;
    private EditText studentIdEditText, cnpEditText;
    private Button registerButton, goToLoginButton;
    private RadioGroup roleRadioGroup;
    private RadioButton teacherRadioButton, studentRadioButton;

    private boolean isTeacher, isStudent;
    private boolean valid;
    private String isApproved;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

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
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        teacherRadioButton = findViewById(R.id.isTeacher);
        studentRadioButton = findViewById(R.id.isStudent);
        isApproved = "pending";

        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.isTeacher) {
                isTeacher = true;
                isStudent = false;
                cnpEditText.setVisibility(View.GONE);
                studentIdEditText.setVisibility(View.GONE);
            } else if (checkedId == R.id.isStudent) {
                isTeacher = false;
                isStudent = true;
                cnpEditText.setVisibility(View.VISIBLE);
                studentIdEditText.setVisibility(View.VISIBLE);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                valid = emptyField(fullNameEditText) && emptyField(emailEditText) && emptyField(passwordEditText) && emptyField(phoneEditText);
                valid = valid && checkField();

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
                                    userInfo.put("isApproved", isApproved);

                                    if (isTeacher) {
                                        userInfo.put("isTeacher", true);
                                    } else {
                                        userInfo.put("isStudent", true);
                                        userInfo.put("SSN", cnpEditText.getText().toString());
                                        userInfo.put("StudentId", studentIdEditText.getText().toString());
                                    }

                                    df.set(userInfo).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterUserActivity.this, "User information saved in Firestore", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(RegisterUserActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    public boolean checkField() {
        isTeacher = teacherRadioButton.isChecked();
        isStudent = studentRadioButton.isChecked();

        if (!isTeacher && !isStudent) {
            Toast.makeText(RegisterUserActivity.this, "One of the roles must be checked!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isStudent && (!emptyField(cnpEditText) || !emptyField(studentIdEditText))) {
            return false;
        }

        return true;
    }
}
