package com.example.attendanceproject.account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.attendanceproject.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserActivity extends AppCompatActivity {
    private EditText fullNameEditText, emailEditText, passwordEditText, phoneEditText;
    private Button registerButton, goToLoginButton;
    private CheckBox teacherCheckBox, studentCheckBox;
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
        phoneEditText = findViewById(R.id.registerPhone);
        registerButton = findViewById(R.id.registerBtn);
        goToLoginButton = findViewById(R.id.gotoLogin);
        teacherCheckBox = findViewById(R.id.isTeacher);
        studentCheckBox = findViewById(R.id.isStudent);
        isApproved = "false";

        // Listener to ensure mutual exclusivity
        teacherCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                studentCheckBox.setChecked(false);
            }
        });

        studentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                teacherCheckBox.setChecked(false);
            }
        });


        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                valid = checkField(fullNameEditText) && checkField(emailEditText) && checkField(passwordEditText) && checkField(phoneEditText);

                if (valid) {
                    fAuth.createUserWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseUser user = fAuth.getCurrentUser();
                            Toast.makeText(RegisterUserActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                            DocumentReference df = fStore.collection("Users").document(user.getUid());
                            Map<String, Object> userInfo = new HashMap<>();
                            userInfo.put("FullName", fullNameEditText.getText().toString());
                            userInfo.put("UserEmail", emailEditText.getText().toString());
                            userInfo.put("PhoneNumber", phoneEditText.getText().toString());

                            // Correct casting to CheckBox and checking if they are checked
                            CheckBox isTeacherCheckbox = findViewById(R.id.isTeacher);
                            CheckBox isStudentCheckbox = findViewById(R.id.isStudent);
                            boolean isTeacher = isTeacherCheckbox.isChecked();
                            boolean isStudent = isStudentCheckbox.isChecked();
                            userInfo.put("isTeacher", isTeacher);
                            userInfo.put("isStudent", isStudent);
                            userInfo.put("isApproved", isApproved);

                            df.set(userInfo);

                            startActivity(new Intent(getApplicationContext(), LoginUserActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(RegisterUserActivity.this, "Failed to create account", Toast.LENGTH_SHORT).show();
                        }
                    });

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

    public boolean checkField(EditText textField){
        if(textField.getText().toString().isEmpty()){
            textField.setError("This field cannot be empty");
            return false;
        }else {
            return true;
        }
    }

}
