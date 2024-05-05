package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.attendanceproject.R;
import com.example.attendanceproject.databinding.FragmentAcceptAccBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceptAccFragment extends Fragment {
    private FirebaseFirestore fStore;
    private ListView usersListView;
    private ArrayAdapter<String> adapter;
    private List<String> userList = new ArrayList<>();
    private Map<String, String> userMap = new HashMap<>();
    private FragmentAcceptAccBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once
        loadUsers(); // Load users initially here, only once when the fragment is first created
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAcceptAccBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usersListView = binding.usersListView;
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, userList);
        usersListView.setAdapter(adapter);

        usersListView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedUser = adapter.getItem(position);
            String userId = userMap.get(selectedUser);
            showApprovalDialog(userId, selectedUser);
        });
    }

    private void loadUsers() {
        fStore.collection("Users").whereEqualTo("isApproved", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userDetail = document.getString("FullName") + " - ";
                            userDetail += Boolean.TRUE.equals(document.getBoolean("isTeacher")) ? "Teacher" : "Student";
                            userList.add(userDetail);
                            userMap.put(userDetail, document.getId());
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showApprovalDialog(String userId, String userDetail) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Approve User");
        builder.setMessage("Do you want to approve " + userDetail + "?");
        builder.setPositiveButton("Yes", (dialog, which) -> updateUserStatus(userId, "true"));
        builder.setNegativeButton("No", (dialog, which) -> updateUserStatus(userId, "false"));
        builder.show();
    }

    private void updateUserStatus(String userId, String status) {
        fStore.collection("Users").document(userId)
                .update("isApproved", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "User status updated", Toast.LENGTH_SHORT).show();
                    reloadUserList();  // Refresh list after update
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating user status", Toast.LENGTH_SHORT).show());
    }

    private void reloadUserList() {
        userList.clear();
        userMap.clear();
        loadUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
