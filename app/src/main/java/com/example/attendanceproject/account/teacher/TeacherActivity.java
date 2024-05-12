package com.example.attendanceproject.account.teacher;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.adapters.EntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.example.attendanceproject.account.admin.CourseAdminBottomSheetFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class TeacherActivity extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter courseAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> courseItems = new ArrayList<>();
    private FirebaseUser user;
    private FirebaseAuth fAuth;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);

        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore
        fAuth = FirebaseAuth.getInstance();
        user = fAuth.getCurrentUser();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        courseAdapter = new EntityAdapter(this, courseItems);
        courseAdapter.setOnItemClickListener(this::onItemClicked);
        recyclerView.setAdapter(courseAdapter);

        loadCoursesForUser();
    }

    private void onItemClicked(EntityItem item) {
        showCourseDetailsBottomSheet(item);
    }

    private void showCourseDetailsBottomSheet(EntityItem item) {
        // You can pass data to your bottom sheet fragment via arguments if needed
        Bundle bundle = new Bundle();
        bundle.putString("courseName", item.getEntityName());
        bundle.putString("courseDetail", item.getEntityDetail());

        CourseTeacherBottomSheetFragment bottomSheet = CourseTeacherBottomSheetFragment.newInstance();
        bottomSheet.setArguments(bundle); // Pass data
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }


    private void loadCoursesForUser() {
        DocumentReference userDocRef = fStore.collection("Users").document(user.getUid());
        userDocRef.collection("CoursesEnrolled")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        courseItems.clear(); // Clear the existing items
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DocumentReference courseRefPath = document.getDocumentReference("courseReference");
                            if (courseRefPath != null) {
                                courseRefPath.get()
                                        .addOnSuccessListener(courseDoc -> {
                                            String courseName = courseDoc.getString("courseName");
                                            String courseDetail = courseDoc.getString("courseDetail");
                                            courseItems.add(new EntityItem(courseName, courseDetail));
                                            courseAdapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Error loading course", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
