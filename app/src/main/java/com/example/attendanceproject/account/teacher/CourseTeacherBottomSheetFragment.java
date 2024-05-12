package com.example.attendanceproject.account.teacher;

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

        
    }
}
