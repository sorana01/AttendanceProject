package com.example.attendanceproject.account.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

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

        view.findViewById(R.id.mark_att_pic_TV).setOnClickListener(v -> showWeekPickerDialog());

        view.findViewById(R.id.mark_att_manual_TV).setOnClickListener(v -> {


        });

        view.findViewById(R.id.view_att_TV).setOnClickListener(v -> {


        });
    }

    private void showWeekPickerDialog() {
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
                    Intent intent = new Intent(getContext(), RecognizeActivity.class);
                    intent.putExtra("courseName", courseName);
                    intent.putExtra("courseDetail", courseDetail);
                    intent.putExtra("courseWeek", selectedWeek);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
