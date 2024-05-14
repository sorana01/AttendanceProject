package com.example.attendanceproject.account.teacher;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAttendanceTeacherActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> attendeesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance_teacher);

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendeesList);
        listView.setAdapter(adapter);

        // Retrieve data from the previous activity
        String courseId = getIntent().getStringExtra("courseId");
        int week = getIntent().getIntExtra("courseWeek", 1);

        if (courseId != null) {
            loadAttendance(courseId, week);
        } else {
            Toast.makeText(this, "Error: Course data not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAttendance(String courseId, int week) {
        FirebaseFirestore.getInstance()
                .collection("AttendanceRecords")
                .whereEqualTo("courseID", FirebaseFirestore.getInstance().document("Courses/" + courseId))
                .whereEqualTo("week", week)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            List<String> names = (List<String>) document.get("attendees");
                            if (names != null) {
                                attendeesList.addAll(names);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } else {
                        Toast.makeText(ViewAttendanceTeacherActivity.this, "No attendance records found for this week.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ViewAttendanceTeacherActivity.this, "Failed to load attendance data.", Toast.LENGTH_SHORT).show());
    }
}
