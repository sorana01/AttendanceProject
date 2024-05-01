package com.example.attendanceproject.account.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.attendanceproject.R;
import com.example.attendanceproject.databinding.BottomSheetCourseDetailsBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CourseDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    public static CourseDetailsBottomSheetFragment newInstance() {
        return new CourseDetailsBottomSheetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog); // Apply the custom style here
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
            // Use courseName and courseDetail as needed
        }

        view.findViewById(R.id.assignStudentsTV).setOnClickListener(v -> {
            // TODO: Implement selection logic or show another dialog to select teachers
        });

        view.findViewById(R.id.assignTeachersTV).setOnClickListener(v -> {
            // TODO: Implement selection logic or show another dialog to select students
        });

    }
}
