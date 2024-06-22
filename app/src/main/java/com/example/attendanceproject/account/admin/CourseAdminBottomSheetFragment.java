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
import com.example.attendanceproject.account.adapters.CheckableEntityAdapter;
import com.example.attendanceproject.account.adapters.CheckableEntityItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO from save change to delete button for view options
// TODO delete button will delete assigned user from course and course for assigned user
public class CourseAdminBottomSheetFragment extends BottomSheetDialogFragment {
    private RecyclerView recyclerView;
    private FirebaseFirestore fStore;
    private CheckableEntityAdapter checkableEntityAdapter;
    // will be sent to the adapter
    private ArrayList<CheckableEntityItem> userList = new ArrayList<>();
    private LinearLayout toolbarLayout;
    private LinearLayout optionsLayout;
    private String currentRole = ""; // To track the current role being assigned
    private String courseId = null; // Store the course ID for easier reference
    private String courseName, courseDetail;
    private Task<?> currentLoadTask = null;  // Reference to track the latest async load task




    public static CourseAdminBottomSheetFragment newInstance() {
        return new CourseAdminBottomSheetFragment();
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
        return inflater.inflate(R.layout.bottom_sheet_course_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            courseName = getArguments().getString("courseName");
            courseDetail = getArguments().getString("courseDetail");
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
            currentRole = "student";
            loadUnassignedUsers("isStudent");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.viewStudentsTV).setOnClickListener(v -> {
            currentRole = "student";
            loadCourseUsers("isStudent");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.assignTeachersTV).setOnClickListener(v -> {
            currentRole = "teacher";
            loadUnassignedUsers("isTeacher");
            recyclerView.setVisibility(View.VISIBLE);
            optionsLayout.setVisibility(View.GONE);
            toolbarLayout.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.viewTeachersTV).setOnClickListener(v -> {
            currentRole = "teacher";
            loadCourseUsers("isTeacher");
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

//    private void saveData() {
//        if (courseId == null) {
//            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        ArrayList<CheckableEntityItem> checkedItems = checkableEntityAdapter.getCheckedItems();
//        if (checkedItems.isEmpty()) {
//            Toast.makeText(getContext(), "No " + currentRole + "s selected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");
//
//        for (CheckableEntityItem item : checkedItems) {
//            fStore.collection("Users")
//                    .whereEqualTo("UserEmail", item.getEntityDetail())
//                    .get()
//                    .addOnSuccessListener(userSnapshot -> {
//                        if (!userSnapshot.isEmpty()) {
//                            QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) userSnapshot.getDocuments().get(0);
//                            DocumentReference userRef = userDoc.getReference();
//
//                            // Add user reference to course's subcollection
//                            Map<String, Object> userRefData = new HashMap<>();
//                            userRefData.put("userReference", userRef);
//                            userRefData.put("userName", item.getEntityName());  // Optional for quick view
//                            userRefData.put("userEmail", item.getEntityDetail());   //Optional for quick view
//
//
//                            fStore.collection(collectionPath)
//                                    .add(userRefData)
//                                    .addOnSuccessListener(documentReference -> {
//                                        Toast.makeText(getContext(), currentRole + " assigned successfully: " + item.getEntityName(), Toast.LENGTH_LONG).show();
//                                    })
//                                    .addOnFailureListener(e -> {
//                                        Toast.makeText(getContext(), "Failed to assign " + currentRole + ": " + item.getEntityName(), Toast.LENGTH_SHORT).show();
//                                    });
//
//                            // Add course reference to user's subcollection
//                            String userCourseSubPath = "CoursesEnrolled";
//                            DocumentReference courseRef = fStore.collection("Courses").document(courseId);
//                            Map<String, Object> courseRefData = new HashMap<>();
//                            courseRefData.put("courseReference", courseRef);
//                            courseRefData.put("courseName", courseName);  // Optional for quick view
//                            courseRefData.put("courseDetail", courseDetail);  // Optional for quick view
//
//                            userRef.collection(userCourseSubPath)
//                                    .add(courseRefData)
//                                    .addOnFailureListener(e -> {
//                                        Log.w("Firestore", "Error adding course reference to user", e);
//                                    });
//                        } else {
//                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(getContext(), "Error finding user: " + item.getEntityDetail(), Toast.LENGTH_SHORT).show();
//                    });
//        }
//    }

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
            processUserAssignment(item, collectionPath);
        }
    }


    private void processUserAssignment(CheckableEntityItem item, String collectionPath) {
        fStore.collection("Users")
                .whereEqualTo("UserEmail", item.getEntityDetail())
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.isEmpty()) {
                        QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) userSnapshot.getDocuments().get(0);
                        DocumentReference userRef = userDoc.getReference();
                        addUserToCourseSubcollection(item, collectionPath, userRef);
                        addCourseToUserSubcollection(userRef);
                    } else {
                        Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error finding user: " + item.getEntityDetail(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addUserToCourseSubcollection(CheckableEntityItem item, String collectionPath, DocumentReference userRef) {
        Map<String, Object> userRefData = new HashMap<>();
        userRefData.put("userReference", userRef);
        userRefData.put("userName", item.getEntityName());  // Optional for quick view
        userRefData.put("userEmail", item.getEntityDetail());  // Optional for quick view

        fStore.collection(collectionPath)
                .add(userRefData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), currentRole + " assigned successfully: " + item.getEntityName(), Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to assign " + currentRole + ": " + item.getEntityName(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCourseToUserSubcollection(DocumentReference userRef) {
        String userCourseSubPath = "CoursesEnrolled";
        DocumentReference courseRef = fStore.collection("Courses").document(courseId);
        Map<String, Object> courseRefData = new HashMap<>();
        courseRefData.put("courseReference", courseRef);
        courseRefData.put("courseName", courseName);  // Optional for quick view
        courseRefData.put("courseDetail", courseDetail);  // Optional for quick view

        userRef.collection(userCourseSubPath)
                .add(courseRefData)
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding course reference to user", e);
                });
    }



    private void loadUnassignedUsers(String isRole) {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine the correct subcollection path based on the role
        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        // Clear the list and notify adapter
        userList.clear();
        checkableEntityAdapter.notifyDataSetChanged();

        // Cancel any previous loading task
        if (currentLoadTask != null && !currentLoadTask.isComplete()) {
            currentLoadTask = null;
        }

        // Fetch already assigned users synchronously first
        currentLoadTask = fStore.collection(collectionPath)
                .get()
                .addOnCompleteListener(assignedTask -> {
                    if (assignedTask.isSuccessful()) {
                        Set<String> assignedEmails = new HashSet<>();
                        List<DocumentReference> userRefs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : assignedTask.getResult()) {
                            DocumentReference userRef = document.getDocumentReference("userReference");
                            if (userRef != null) {
                                userRefs.add(userRef);
                            }
                        }

                        if (!userRefs.isEmpty()) {
                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                            for (DocumentReference ref : userRefs) {
                                tasks.add(ref.get());
                            }

                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                for (Object result : results) {
                                    DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                    if (userSnapshot.exists()) {
                                        assignedEmails.add(userSnapshot.getString("UserEmail"));
                                    }
                                }
                                fetchUnassignedUsers(isRole, assignedEmails);
                            }).addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            fetchUnassignedUsers(isRole, assignedEmails);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUnassignedUsers(String isRole, Set<String> assignedEmails) {
        currentLoadTask = fStore.collection("Users")
                .whereEqualTo("isApproved", "true")
                .whereEqualTo(isRole, true)
                .get()
                .addOnCompleteListener(usersTask -> {
                    if (usersTask.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : usersTask.getResult()) {
                            String userName = document.getString("FullName");
                            String userDetail = document.getString("UserEmail");

                            if (!assignedEmails.contains(userDetail)) {
                                CheckableEntityItem item = new CheckableEntityItem(userName, userDetail);
                                item.setChecked(false);
                                userList.add(item);
                            }
                        }
                        checkableEntityAdapter.notifyDataSetChanged();
                        checkableEntityAdapter.clearCheckedPositions();
                    } else {
                        Toast.makeText(getContext(), "Error loading users", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadCourseUsers(String role) {
        if (courseId == null) {
            Toast.makeText(getContext(), "Course not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionPath = "Courses/" + courseId + (currentRole.equals("student") ? "/AssignedStudents" : "/AssignedTeachers");

        // Clear user list and notify adapter first
        userList.clear();
        checkableEntityAdapter.notifyDataSetChanged();

        // Cancel any previous loading task
        if (currentLoadTask != null && !currentLoadTask.isComplete()) {
            currentLoadTask = null;
        }

        // Fetch user references asynchronously
        currentLoadTask = fStore.collection(collectionPath)
                .get()
                .addOnCompleteListener(assignedTask -> {
                    if (assignedTask.isSuccessful()) {
                        List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : assignedTask.getResult()) {
                            DocumentReference userRef = document.getDocumentReference("userReference");
                            if (userRef != null) {
                                userTasks.add(userRef.get());
                            }
                        }

                        Tasks.whenAllSuccess(userTasks).addOnSuccessListener(results -> {
                            for (Object result : results) {
                                DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                if (userSnapshot.exists()) {
                                    String userName = userSnapshot.getString("FullName");
                                    String userEmail = userSnapshot.getString("UserEmail");
                                    CheckableEntityItem item = new CheckableEntityItem(userName, userEmail);
                                    userList.add(item);
                                }
                            }
                            checkableEntityAdapter.notifyDataSetChanged();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(getContext(), "Error loading assigned users", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
