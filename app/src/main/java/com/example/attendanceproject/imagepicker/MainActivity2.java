package com.example.attendanceproject.imagepicker;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.R;
import com.example.attendanceproject.face_rec.FaceClassifier;

import java.util.HashMap;


public class MainActivity2 extends AppCompatActivity {
    private Button registerBtn;
    private Button recognizeBtn;

    public static HashMap<String, FaceClassifier.Recognition> registered = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        registerBtn = findViewById(R.id.btnRegister);
        recognizeBtn = findViewById(R.id.btnRecognize);

        registerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity2.this, RegisterActivity.class));
            }
        });

        recognizeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity2.this, RecognitionActivity.class));
            }
        });
    }
}
