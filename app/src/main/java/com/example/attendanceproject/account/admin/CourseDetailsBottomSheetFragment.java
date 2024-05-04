package com.example.attendanceproject.account.admin;

import android.os.Bundle;
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
import java.util.List;
import java.util.Map;

public class CourseDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore fStore;
    private CheckableEntityAdapter checkableEntityAdapter;
    private ArrayList<CheckableEntityItem> userList = new ArrayList<>();
    private LinearLayout toolbarLayout;
    private LinearLayout optionsLayout;
    private String currentRole = ""; // To track the current role being assigned


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
            loadUsers("isStudent");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.assignTeachersTV).setOnClickListener(v -> {
            // TODO: Implement selection logic or show another dialog to select students
            currentRole = "teacher";
            loadUsers("isTeacher");
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

    private void saveData() {
        String courseName = getArguments().getString("courseName", "Unknown Course");
        ArrayList<CheckableEntityItem> checkedItems = checkableEntityAdapter.getCheckedItems();
        if (checkedItems.isEmpty()) {
            Toast.makeText(getContext(), "No " + currentRole + "s selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Depending on the role, decide the collection name
        String collectionPath = "Courses/" + courseName + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        for (CheckableEntityItem item : checkedItems) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userName", item.getEntityName());
            userMap.put("userEmail", item.getEntityDetail());

            fStore.collection(collectionPath)
                    .add(userMap)
                    .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), currentRole + " assigned successfully: " + item.getEntityName(), Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to assign " + currentRole + ": " + item.getEntityName(), Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUsers(String isRole) {
        fStore.collection("Users").whereEqualTo("isApproved", "true").whereEqualTo(isRole, true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userName = document.getString("FullName");
                            String userDetail = document.getString("UserEmail");
                            CheckableEntityItem item = new CheckableEntityItem(userName, userDetail);
                            item.setChecked(false);  // Make sure to reset check state
                            userList.add(item);
                        }
                        checkableEntityAdapter.notifyDataSetChanged();
                        checkableEntityAdapter.clearCheckedPositions(); // Clear checked positions after updating
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
