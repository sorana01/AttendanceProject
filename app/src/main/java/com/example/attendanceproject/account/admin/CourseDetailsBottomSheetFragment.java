package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore fStore;
    private CheckableEntityAdapter checkableEntityAdapter;
    // will be sent to the adapter
    private ArrayList<CheckableEntityItem> userList = new ArrayList<>();
    private LinearLayout toolbarLayout;
    private LinearLayout optionsLayout;
    private String currentRole = ""; // To track the current role being assigned
    private String courseId = null; // Store the course ID for easier reference



    public static CourseDetailsBottomSheetFragment newInstance() {
        return new CourseDetailsBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog); // Apply the custom style here
        fStore = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_course_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String courseName = getArguments().getString("courseName");
            String courseDetail = getArguments().getString("courseDetail");
            findCourseId(courseName);
            // Use courseName and courseDetail as needed
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        toolbarLayout = view.findViewById(R.id.toolbarLayout);
        optionsLayout = view.findViewById(R.id.optionsLayout);
        checkableEntityAdapter = new CheckableEntityAdapter(getContext(), userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(checkableEntityAdapter);

        view.findViewById(R.id.assignStudentsTV).setOnClickListener(v -> {
            // TODO: Implement selection logic or show another dialog to select teachers
            currentRole = "student";
            loadUnassignedUsers("isStudent");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.assignTeachersTV).setOnClickListener(v -> {
            // TODO: Implement selection logic or show another dialog to select students
            currentRole = "teacher";
            loadUnassignedUsers("isTeacher");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        Button backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            toolbarLayout.setVisibility(View.GONE);
            optionsLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        });

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            saveData(); // Implement saving logic
            recyclerView.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.GONE);
            optionsLayout.setVisibility(View.VISIBLE);
        });

    }

    private void findCourseId(String courseName) {
        fStore.collection("Courses")
                .whereEqualTo("courseName", courseName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        courseId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    } else {
                        Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error getting course", e);
                    Toast.makeText(getContext(), "Error finding course", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveData() {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CheckableEntityItem> checkedItems = checkableEntityAdapter.getCheckedItems();
        if (checkedItems.isEmpty()) {
            Toast.makeText(getContext(), "No " + currentRole + "s selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        for (CheckableEntityItem item : checkedItems) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userName", item.getEntityName());
            userMap.put("userEmail", item.getEntityDetail());

            fStore.collection(collectionPath)
                    .add(userMap)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), currentRole + " assigned successfully: " + item.getEntityName(), Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to assign " + currentRole + ": " + item.getEntityName(), Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void loadUnassignedUsers(String isRole) {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        // Fetch already assigned users
        fStore.collection(collectionPath)
                .get()
                .addOnCompleteListener(assignedTask -> {
                    if (assignedTask.isSuccessful()) {
                        Set<String> assignedEmails = new HashSet<>();
                        for (QueryDocumentSnapshot document : assignedTask.getResult()) {
                            assignedEmails.add(document.getString("userEmail"));
                        }

                        // Fetch all approved users of the given role, excluding already assigned ones
                        fStore.collection("Users")
                                .whereEqualTo("isApproved", "true")
                                .whereEqualTo(isRole, true)
                                .get()
                                .addOnCompleteListener(usersTask -> {
                                    if (usersTask.isSuccessful()) {
                                        userList.clear();
                                        for (QueryDocumentSnapshot document : usersTask.getResult()) {
                                            String userName = document.getString("FullName");
                                            String userDetail = document.getString("UserEmail");

                                            // Add only if not already assigned
                                            if (!assignedEmails.contains(userDetail)) {
                                                CheckableEntityItem item = new CheckableEntityItem(userName, userDetail);
                                                item.setChecked(false); // Reset check state
                                                userList.add(item);
                                            }
                                        }
                                        checkableEntityAdapter.notifyDataSetChanged();
                                        checkableEntityAdapter.clearCheckedPositions(); // Clear checked positions after updating
                                    } else {
                                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
