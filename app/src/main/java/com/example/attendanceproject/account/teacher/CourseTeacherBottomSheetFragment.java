package com.example.attendanceproject.account.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.admin.CourseAdminBottomSheetFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CourseTeacherBottomSheetFragment extends BottomSheetDialogFragment {
    private String courseName, courseDetail;
    public static CourseTeacherBottomSheetFragment newInstance() {
        return new CourseTeacherBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            courseName = getArguments().getString("courseName");
            courseDetail = getArguments().getString("courseDetail");
        }

        view.findViewById(R.id.mark_att_pic_TV).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), RecognizeActivity.class);

            // Optionally, you can add extra information to pass to RecognizeActivity
            intent.putExtra("courseName", courseName);
            intent.putExtra("courseDetail", courseDetail);

            // Start the activity
            startActivity(intent);
        });

        view.findViewById(R.id.mark_att_manual_TV).setOnClickListener(v -> {


        });

        view.findViewById(R.id.view_att_TV).setOnClickListener(v -> {


        });
    }
}
