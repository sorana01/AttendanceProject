package com.example.attendanceproject.account.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.adapters.EntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ViewAttendanceStudentActivity extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter<EntityItem> entityAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> entityItems = new ArrayList<>();
    private String courseName, courseId;
    private FirebaseUser user;
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance_student);

        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once
        fAuth = FirebaseAuth.getInstance();
        user = fAuth.getCurrentUser();

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        entityAdapter = new EntityAdapter(this, entityItems);
        recyclerView.setAdapter(entityAdapter);


        // Retrieve the data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getStringExtra("courseId");
            courseName = intent.getStringExtra("courseName");
            if (courseName != null) {
                getSupportActionBar().setTitle(courseName);
            }
        }

        loadAttendanceFromFirestore();
    }

    private void loadAttendanceFromFirestore() {
        // Reference to the user's enrolled courses collection
        DocumentReference userDocRef = fStore.collection("Users").document(user.getUid());
        Log.d("Firestore", "User document path: " + userDocRef.getPath());

        userDocRef.collection("CoursesEnrolled")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Log.d("Firestore", "No courses enrolled found for user.");
                            Toast.makeText(this, "No enrolled courses found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean courseFound = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DocumentReference courseRefPath = document.getDocumentReference("courseReference");
                            if (courseRefPath != null && courseRefPath.getPath().equals("Courses/" + courseId)) {
                                courseFound = true;
                                Log.d("Firestore", "Course found: " + courseRefPath.getPath());

                                Log.d("Firestore", "Attempting to fetch attendance data...");
                                document.getReference().collection("Attendance")
                                        .get()
                                        .addOnCompleteListener(attendanceTask -> {
                                            if (attendanceTask.isSuccessful()) {
                                                Log.d("Firestore", "Attendance data fetch successful.");
                                                entityItems.clear();
                                                for (QueryDocumentSnapshot attendanceDoc : attendanceTask.getResult()) {
                                                    String status = attendanceDoc.getString("status");
                                                    Long weekNumber = attendanceDoc.getLong("week");
                                                    if (weekNumber != null) {
                                                        entityItems.add(new EntityItem("Week " + weekNumber, status));
                                                        Log.d("Firestore", "Attendance loaded: Week " + weekNumber + ", Status: " + status);
                                                    }
                                                }
                                                entityAdapter.notifyDataSetChanged();
                                            } else {
                                                Log.e("Firestore", "Error loading attendance: ", attendanceTask.getException());
                                                Toast.makeText(this, "Error loading attendance", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "Failed to fetch attendance data: ", e);
                                        });
                                break;
                            }
                        }

                        if (!courseFound) {
                            Log.d("Firestore", "Course with ID " + courseId + " not found in user's enrolled courses.");
                            Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Firestore", "Error getting courses enrolled: ", task.getException());
                        Toast.makeText(this, "Error loading enrolled courses", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch courses enrolled: ", e);
                });
    }

}
