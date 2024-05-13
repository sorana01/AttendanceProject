package com.example.attendanceproject.account.teacher;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceproject.R;

import java.util.ArrayList;
import java.util.List;

public class ViewAttendanceActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> attendanceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        listView = findViewById(R.id.list_view_attendance);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendanceList);
        listView.setAdapter(adapter);

        loadAttendance();
    }

    private void loadAttendance() {
        AttendanceManager attendanceManager = new AttendanceManager(this);
        List<AttendanceManager.AttendanceRecord> records = attendanceManager.loadExistingData();
        for (AttendanceManager.AttendanceRecord record : records) {
            attendanceList.add(record.courseName + ": " + record.names.toString());
        }
        adapter.notifyDataSetChanged();
    }
}
