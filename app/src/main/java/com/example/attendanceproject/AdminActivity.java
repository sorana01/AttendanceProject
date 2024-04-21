package com.example.attendanceproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private ListView usersListView;
    private ArrayAdapter<String> adapter;
    private List<String> userList = new ArrayList<>();
    private Map<String, String> userMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        usersListView = findViewById(R.id.usersListView);
        fStore = FirebaseFirestore.getInstance();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        usersListView.setAdapter(adapter);

        loadUsers();

        usersListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = adapter.getItem(position);
            String userId = userMap.get(selectedUser);
            showApprovalDialog(userId, selectedUser);
        });
    }

    private void loadUsers() {
        fStore.collection("Users").whereEqualTo("isApproved", "false")
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
                        Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showApprovalDialog(String userId, String userDetail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Approve User");
        builder.setMessage("Do you want to approve " + userDetail + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            updateUserStatus(userId, "true");
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            updateUserStatus(userId, "forbidden");
        });

        builder.show();
    }

    private void updateUserStatus(String userId, String status) {
        fStore.collection("Users").document(userId)
                .update("isApproved", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User status updated", Toast.LENGTH_SHORT).show();
                    reloadUserList();  // Refresh list after update
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating user status", Toast.LENGTH_SHORT).show());
    }

    private void reloadUserList() {
        userList.clear();
        userMap.clear();
        loadUsers();
    }
}
