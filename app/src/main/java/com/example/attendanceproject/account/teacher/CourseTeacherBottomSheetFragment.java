package com.example.attendanceproject.account.teacher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.admin.CourseAdminBottomSheetFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class CourseTeacherBottomSheetFragment extends BottomSheetDialogFragment {
    private String courseName, courseDetail, courseId;
    private FirebaseFirestore firestore;
    private CollectionReference attendanceRecordsRef;

    public static CourseTeacherBottomSheetFragment newInstance() {
        return new CourseTeacherBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        attendanceRecordsRef = firestore.collection("AttendanceRecords");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_course_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            courseId = getArguments().getString("courseId");
            courseName = getArguments().getString("courseName");
            courseDetail = getArguments().getString("courseDetail");
        }
        Log.d("CourseTeacherBottomSheet", "Course id " + courseId);

        view.findViewById(R.id.mark_att_pic_TV).setOnClickListener(v -> showWeekPickerDialog(RecognizeActivity.class));

        view.findViewById(R.id.mark_att_manual_TV).setOnClickListener(v -> showWeekPickerDialog(ManualAttendanceActivity.class));

        view.findViewById(R.id.view_att_TV).setOnClickListener(v -> showWeekPickerDialog(ViewAttendanceActivity.class));
    }

    private void showWeekPickerDialog(Class<? extends Activity> activityClass) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Week");

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.week_picker_dialog, null);
        final NumberPicker numberPicker = dialogView.findViewById(R.id.week_picker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(14);
        numberPicker.setWrapSelectorWheel(true);

        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, id) -> {
                    int selectedWeek = numberPicker.getValue();
                    checkAttendanceRecordExists(activityClass, selectedWeek);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkAttendanceRecordExists(Class<? extends Activity> activityClass, int selectedWeek) {
        attendanceRecordsRef
                .whereEqualTo("courseID", firestore.document("Courses/" + courseId))
                .whereEqualTo("week", selectedWeek)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            showAttendanceExistsDialog(activityClass, selectedWeek);
                        } else {
                            startAttendanceActivity(activityClass, selectedWeek);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error checking attendance records", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startAttendanceActivity(Class<? extends Activity> activityClass, int selectedWeek) {
        Intent intent = new Intent(getContext(), activityClass);
        intent.putExtra("courseId", courseId);
        intent.putExtra("courseName", courseName);
        intent.putExtra("courseDetail", courseDetail);
        intent.putExtra("courseWeek", selectedWeek);
        startActivity(intent);
    }

    private void showAttendanceExistsDialog(Class<? extends Activity> activityClass, int selectedWeek) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Attendance Exists")
                .setMessage("Attendance for course " + courseName + " and week " + selectedWeek + " already exists.")
                .setPositiveButton("Delete Attendance", (dialog, id) -> {
                    deleteAttendanceRecord(selectedWeek, () -> startAttendanceActivity(activityClass, selectedWeek));
                })
                .setNegativeButton("Append to Attendance", (dialog, id) -> startAttendanceActivity(activityClass, selectedWeek))
                .create()
                .show();
    }

    private void deleteAttendanceRecord(int selectedWeek, Runnable onSuccess) {
        attendanceRecordsRef
                .whereEqualTo("courseID", firestore.document("Courses/" + courseId))
                .whereEqualTo("week", selectedWeek)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                attendanceRecordsRef.document(document.getId()).delete();
                            }
                            Toast.makeText(getContext(), "Attendance record deleted", Toast.LENGTH_SHORT).show();
                            onSuccess.run();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error deleting attendance record", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
