package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.databinding.FragmentStudentsBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class StudentsFragment extends Fragment {
    private FragmentStudentsBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private EntityAdapter<EntityItem> userAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<EntityItem> userItems = new ArrayList<>();
    private ArrayList<String> userIds = new ArrayList<>();

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

        userAdapter = new EntityAdapter(getActivity(), userItems);
        userAdapter.setOnItemClickListener(this::onItemClicked);
        recyclerView.setAdapter(userAdapter);
    }

    private void onItemClicked(EntityItem entityItem) {
        int index = userItems.indexOf(entityItem);
        if (index != -1) {
            String userId = userIds.get(index);
            Bundle bundle = new Bundle();
            bundle.putString("userId", userId);
            NavHostFragment.findNavController(this).navigate(R.id.action_studentsFragment_to_coursesForStudentsAndTeachersFragment, bundle);
        }
    }

    private void loadUsersFromFirestore() {
        fStore.collection("Users").whereEqualTo("isApproved", "true").whereEqualTo("isStudent", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userItems.clear(); // Clear the existing items
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userName = document.getString("FullName");
                            String userEmail = (String)document.get("UserEmail");
                            userItems.add(new EntityItem(userName, userEmail));
                            userIds.add(document.getId());
                        }
                        userAdapter.notifyDataSetChanged();
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
