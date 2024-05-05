package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.admin.CourseDetailsBottomSheetFragment;
import com.example.attendanceproject.account.admin.EntityAdapter;
import com.example.attendanceproject.account.admin.EntityItem;
import com.example.attendanceproject.databinding.FragmentCoursesBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter<EntityItem> entityAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> entityItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        entityAdapter = new EntityAdapter(getActivity(), entityItems);
        entityAdapter.setOnItemClickListener(this::onItemClicked);
        recyclerView.setAdapter(entityAdapter);

        FloatingActionButton fab = getActivity().findViewById(R.id.fab); // Access FAB from the Activity
        if (fab != null) {
            fab.setOnClickListener(v -> {
                // Define what happens when FAB is clicked in CoursesFragment
                showAddCourseDialog();
            });
        }

        loadCoursesFromFirestore();
    }

    private void onItemClicked(EntityItem item) {
        showCourseDetailsBottomSheet(item);
    }

    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.entity_dialog, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText("Add new course");

        EditText entity_name_edt = view.findViewById(R.id.entity_name_edt);
        entity_name_edt.setHint("Course name");

        EditText entity_detail_edt = view.findViewById(R.id.entity_detail_edt);
        entity_detail_edt.setHint("Course detail");

        Button cancel_btn = view.findViewById(R.id.cancel_btn);
        Button add_btn = view.findViewById(R.id.add_btn);

        cancel_btn.setOnClickListener(v -> dialog.dismiss());

        add_btn.setOnClickListener(v -> {
            String courseName = entity_name_edt.getText().toString().trim();
            String courseDetail = entity_detail_edt.getText().toString().trim();
            // Check if course name or detail is not empty
            if (!courseName.isEmpty() && !courseDetail.isEmpty()) {
                EntityItem newItem = new EntityItem(courseName, courseDetail);
                entityItems.add(newItem);  // Add the new course to your data source
                int position = entityItems.size() - 1;  // Position where the new item was added
                entityAdapter.notifyItemInserted(position);  // Notify the adapter of the new item
                recyclerView.scrollToPosition(position);  // Optionally, scroll to the new item
                saveCourseToFirestore(courseName, courseDetail);
            } else {
                // Optionally, show an error message or toast notification here if fields are empty
                Toast.makeText(getActivity(), "Both fields are required", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
    }

    private void saveCourseToFirestore(String courseName, String courseDetail) {
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("courseName", courseName);
        courseData.put("courseDetail", courseDetail);
        courseData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        courseData.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        fStore.collection("Courses")
                .add(courseData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Course added with ID: " + documentReference.getId());
                    Toast.makeText(getActivity(), "Course added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding course", e);
                    Toast.makeText(getActivity(), "Error adding course", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCoursesFromFirestore() {
        fStore.collection("Courses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        entityItems.clear(); // Clear the existing items
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String courseName = document.getString("courseName");
                            String courseDetail = document.getString("courseDetail");
                            entityItems.add(new EntityItem(courseName, courseDetail));
                        }
                        entityAdapter.notifyDataSetChanged(); // Refresh the adapter
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                        Toast.makeText(getActivity(), "Failed to load courses.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCourseDetailsBottomSheet(EntityItem item) {
        // You can pass data to your bottom sheet fragment via arguments if needed
        Bundle bundle = new Bundle();
        bundle.putString("courseName", item.getEntityName());
        bundle.putString("courseDetail", item.getEntityDetail());

        CourseDetailsBottomSheetFragment bottomSheet = CourseDetailsBottomSheetFragment.newInstance();
        bottomSheet.setArguments(bundle); // Pass data
        bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}