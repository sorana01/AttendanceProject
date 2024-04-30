package com.example.attendanceproject.ui.students;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendanceproject.databinding.FragmentStudentsBinding;

public class StudentsFragment extends Fragment {
    private FragmentStudentsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StudentsViewModel studentsViewModel =
                new ViewModelProvider(this).get(StudentsViewModel.class);

        binding = FragmentStudentsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textStudents;
        studentsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
