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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
            fStore.collection("Users")
                    .whereEqualTo("UserEmail", item.getEntityDetail())
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (!userSnapshot.isEmpty()) {
                            QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) userSnapshot.getDocuments().get(0);
                            DocumentReference userRef = userDoc.getReference();

                            Map<String, Object> assignmentData = new HashMap<>();
                            assignmentData.put("userReference", userRef);

                            fStore.collection(collectionPath)
                                    .add(assignmentData)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(getContext(), currentRole + " assigned successfully: " + item.getEntityName(), Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to assign " + currentRole + ": " + item.getEntityName(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error finding user: " + item.getEntityDetail(), Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void loadUnassignedUsers(String isRole) {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine the correct subcollection path based on the role
        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        // Fetch already assigned users synchronously first
        fStore.collection(collectionPath)
                .get()
                .addOnCompleteListener(assignedTask -> {
                    if (assignedTask.isSuccessful()) {
                        // Create a set to store the email addresses of already assigned users
                        Set<String> assignedEmails = new HashSet<>();
                        // Create a list to store references to user documents
                        List<DocumentReference> userRefs = new ArrayList<>();
                        // Loop through each document in the subcollection (`AssignedStudents` or `AssignedTeachers`)
                        for (QueryDocumentSnapshot document : assignedTask.getResult()) {
                            // Retrieve the `DocumentReference` of the user document
                            DocumentReference userRef = document.getDocumentReference("userReference");
                            if (userRef != null) {
                                // Add the reference to the list for later fetching
                                userRefs.add(userRef);
                            }
                        }

                        // Fetch all user documents pointed to by these references
                        if (!userRefs.isEmpty()) {
                            // Create a list of tasks to fetch the user documents
                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                            for (DocumentReference ref : userRefs) {
                                tasks.add(ref.get());
                            }

                            // Wait until all tasks are done
                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                // Loop through the results and add the user email addresses to the set
                                for (Object result : results) {
                                    DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                    if (userSnapshot.exists()) {
                                        assignedEmails.add(userSnapshot.getString("UserEmail"));
                                    }
                                }

                                // Fetch all approved users of the given role, excluding already assigned ones
                                fetchUnassignedUsers(isRole, assignedEmails);
                            });
                        } else {
                            // If there are no assigned users, fetch unassigned users directly
                            fetchUnassignedUsers(isRole, assignedEmails);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUnassignedUsers(String isRole, Set<String> assignedEmails) {
        // Query the `Users` collection for users with the specific role and approved status
        fStore.collection("Users")
                .whereEqualTo("isApproved", "true")
                .whereEqualTo(isRole, true)
                .get()
                .addOnCompleteListener(usersTask -> {
                    if (usersTask.isSuccessful()) {
                        // Clear the current `userList`
                        userList.clear();
                        // Loop through each document in the `Users` collection query results
                        for (QueryDocumentSnapshot document : usersTask.getResult()) {
                            // Extract the user's name and email address
                            String userName = document.getString("FullName");
                            String userDetail = document.getString("UserEmail");

                            // Add only users who are not already assigned (i.e., not in `assignedEmails`)
                            if (!assignedEmails.contains(userDetail)) {
                                // Create a `CheckableEntityItem` and add it to the list
                                CheckableEntityItem item = new CheckableEntityItem(userName, userDetail);
                                item.setChecked(false); // Reset check state
                                userList.add(item);
                            }
                        }
                        // Notify the adapter that the data set has changed
                        checkableEntityAdapter.notifyDataSetChanged();
                        checkableEntityAdapter.clearCheckedPositions(); // Clear checked positions after updating
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
