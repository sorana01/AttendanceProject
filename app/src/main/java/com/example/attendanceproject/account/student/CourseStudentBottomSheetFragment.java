package com.example.attendanceproject.account.student;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.teacher.ViewAttendanceTeacherActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CourseStudentBottomSheetFragment  extends BottomSheetDialogFragment {
    private String courseName, courseDetail, courseId;
    private FirebaseFirestore firestore;
    private CollectionReference attendanceRecordsRef;


    public static CourseStudentBottomSheetFragment newInstance() {
        return new CourseStudentBottomSheetFragment();
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
        return inflater.inflate(R.layout.bottom_sheet_course_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            courseId = getArguments().getString("courseId");
            courseName = getArguments().getString("courseName");
            courseDetail = getArguments().getString("courseDetail");
        }
        Log.d("CourseStudentBottomSheet", "Course id " + courseId);

        view.findViewById(R.id.contact_teacher_TV).setOnClickListener(v -> contactTeacher());

        view.findViewById(R.id.view_att_TV).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ViewAttendanceStudentActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });
    }



    private void contactTeacher() {
    }
}
