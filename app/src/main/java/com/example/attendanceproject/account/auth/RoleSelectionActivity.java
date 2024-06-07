package com.example.attendanceproject.account.auth;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.attendanceproject.R;

public class RoleSelectionActivity extends AppCompatActivity {
    private Button teacherButton, studentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        teacherButton = findViewById(R.id.teacherButton);
        studentButton = findViewById(R.id.studentButton);

        teacherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, RegisterTeacherActivity.class);
                startActivity(intent);
            }
        });

        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, RegisterStudentActivity.class);
                startActivity(intent);
            }
        });
    }
}
