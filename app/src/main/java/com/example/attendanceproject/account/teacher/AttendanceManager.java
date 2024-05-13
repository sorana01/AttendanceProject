package com.example.attendanceproject.account.teacher;

import android.content.Context;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AttendanceManager {
    private Context context;

    public AttendanceManager(Context context) {
        this.context = context;
    }

    // Data class to represent the attendance record
    public static class AttendanceRecord {
        String courseName;
        List<String> names;

        public AttendanceRecord(String courseName, List<String> names) {
            this.courseName = courseName;
            this.names = names;
        }
    }

    public void saveRecognitionResults(String courseName, List<String> names) {
        // Create an attendance record object
        AttendanceRecord record = new AttendanceRecord(courseName, names);

        // Load existing data and append new record
        List<AttendanceRecord> records = loadExistingData();
        records.add(record);

        // Convert the updated list of records to JSON and save it
        saveData(new Gson().toJson(records));

        Toast.makeText(context, "Recognition results saved for " + courseName, Toast.LENGTH_SHORT).show();
    }

    private void saveData(String data) {
        try {
            FileOutputStream fos = context.openFileOutput("attendance_data.json", Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public List<AttendanceRecord> loadExistingData() {
        try {
            FileInputStream fis = context.openFileInput("attendance_data.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String jsonData = sb.toString();
            return new Gson().fromJson(jsonData, new TypeToken<List<AttendanceRecord>>(){}.getType());
        } catch (IOException e) {
            return new ArrayList<>(); // Return empty list if there's an error or file doesn't exist
        }
    }
}

