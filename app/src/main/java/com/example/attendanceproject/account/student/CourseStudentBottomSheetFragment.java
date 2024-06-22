package com.example.attendanceproject.account.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.adapters.CheckableEntityAdapter;
import com.example.attendanceproject.account.adapters.CheckableEntityItem;
import com.example.attendanceproject.account.adapters.EntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.example.attendanceproject.account.teacher.ViewAttendanceTeacherActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourseStudentBottomSheetFragment  extends BottomSheetDialogFragment {
    private String courseName, courseDetail, courseId;
    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private CollectionReference attendanceRecordsRef;
    private EntityAdapter entityAdapter;

    private ArrayList<EntityItem> userList = new ArrayList<>();
    private Task<?> currentLoadTask = null;  // Reference to track the latest async load task
    private LinearLayout optionsLayout;


    public static CourseStudentBottomSheetFragment newInstance() {
        return new CourseStudentBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        attendanceRecordsRef = firestore.collection("AttendanceRecords");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_course_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            courseId = getArguments().getString("courseId");
            courseName = getArguments().getString("courseName");
            courseDetail = getArguments().getString("courseDetail");
        }
        Log.d("CourseStudentBottomSheet", "Course id " + courseId);

        recyclerView = view.findViewById(R.id.recyclerView);
        entityAdapter = new EntityAdapter(getContext(), userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(entityAdapter);
        optionsLayout = view.findViewById(R.id.optionsLayout);


        view.findViewById(R.id.contact_teacher_TV).setOnClickListener(v -> {
//            contactTeacher();
            loadCourseUsers("isTeacher");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
        });

        view.findViewById(R.id.view_att_TV).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ViewAttendanceStudentActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });
    }

    private void loadCourseUsers(String role) {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath = "Courses/" + courseId + "/AssignedTeachers";

        // Clear user list and notify adapter first
        userList.clear();
        entityAdapter.notifyDataSetChanged();

        // Cancel any previous loading task
        if (currentLoadTask != null && !currentLoadTask.isComplete()) {
            currentLoadTask = null;
        }

        // Fetch user references asynchronously
        currentLoadTask = firestore.collection(collectionPath)
                .get()
                .addOnCompleteListener(assignedTask -> {
                    if (assignedTask.isSuccessful()) {
                        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : assignedTask.getResult()) {
                            DocumentReference userRef = document.getDocumentReference("userReference");
                            if (userRef != null) {
                                userTasks.add(userRef.get());
                            }
                        }

                        Tasks.whenAllSuccess(userTasks).addOnSuccessListener(results -> {
                            for (Object result : results) {
                                DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                if (userSnapshot.exists()) {
                                    String userName = userSnapshot.getString("FullName");
                                    String userEmail = userSnapshot.getString("UserEmail");
                                    EntityItem item = new EntityItem(userName, userEmail);
                                    userList.add(item);
                                }
                            }
                            entityAdapter.notifyDataSetChanged();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
