package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.adapters.EntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AdminCourseActivity extends AppCompatActivity {
    private FloatingActionButton addBtn;
    private RecyclerView recyclerView;
    private EntityAdapter entityAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> entityItems = new ArrayList<>();
    private EditText class_edt;
    private EditText subject_edt;
    private Button cancel_btn;
    private Button add_btn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_course);

        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(v -> showDialog());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        entityAdapter = new EntityAdapter(this, entityItems);
        recyclerView.setAdapter(entityAdapter);
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.entity_dialog, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        class_edt = view.findViewById(R.id.entity_name_edt);
        subject_edt = view.findViewById(R.id.entity_detail_edt);
        cancel_btn = view.findViewById(R.id.cancel_btn);
        add_btn = view.findViewById(R.id.add_btn);

        cancel_btn.setOnClickListener(v -> dialog.dismiss());
        add_btn.setOnClickListener(v -> {
            addClass();
            dialog.dismiss();
        });
    }

    private void addClass() {
        String className = class_edt.getText().toString();
        String subjectName = subject_edt.getText().toString();
        entityItems.add(new EntityItem(className, subjectName));
        entityAdapter.notifyDataSetChanged();
    }
}
