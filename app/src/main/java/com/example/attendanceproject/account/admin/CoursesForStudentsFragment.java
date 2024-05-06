package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.databinding.FragmentCoursesForStudentsBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class CoursesForStudentsFragment extends Fragment {
    private FragmentCoursesForStudentsBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private CheckableEntityAdapter courseAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<CheckableEntityItem> courseItems = new ArrayList<>();
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
        loadCoursesForUser(userId); // Load courses
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoursesForStudentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        courseAdapter = new CheckableEntityAdapter(getActivity(), courseItems);
        recyclerView.setAdapter(courseAdapter);
    }

    private void loadCoursesForUser(String userId) {
        DocumentReference userDocRef = fStore.collection("Users").document(userId);
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
                                            courseItems.add(new CheckableEntityItem(courseName, courseDetail));
                                            courseAdapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading course", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading courses", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
