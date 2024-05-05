package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.databinding.FragmentStudentsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeachersFragment extends Fragment {
    private FragmentStudentsBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter<EntityItem> entityAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> entityItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once
        loadUsersFromFirestore(); // Load users initially here, only once when the fragment is first created
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentsBinding.inflate(inflater, container, false);
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
    }

    private void onItemClicked(EntityItem entityItem) {
//        loadCoursesForUser();
    }

    private void loadUsersFromFirestore() {
        fStore.collection("Users").whereEqualTo("isApproved", "true").whereEqualTo("isTeacher", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        entityItems.clear(); // Clear the existing items
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userName = document.getString("FullName");
                            String userEmail = (String)document.get("UserEmail");
                            entityItems.add(new EntityItem(userName, userEmail));
                        }
                        entityAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
