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

import com.example.attendanceproject.account.adapters.CheckableEntityAdapter;
import com.example.attendanceproject.account.adapters.CheckableEntityItem;
import com.example.attendanceproject.account.adapters.EntityAdapter;
import com.example.attendanceproject.account.adapters.EntityItem;
import com.example.attendanceproject.databinding.FragmentCoursesForStudentsAndTeachersBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

// TODO delete button for each course from each user and from each user that course
public class CoursesForStudentsAndTeachersFragment extends Fragment {
    private FragmentCoursesForStudentsAndTeachersBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter entityAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> courseItems = new ArrayList<>();
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
        binding = FragmentCoursesForStudentsAndTeachersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        entityAdapter = new EntityAdapter(getActivity(), courseItems);
        recyclerView.setAdapter(entityAdapter);
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
                                            courseItems.add(new EntityItem(courseName, courseDetail));
                                            entityAdapter.notifyDataSetChanged();
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
