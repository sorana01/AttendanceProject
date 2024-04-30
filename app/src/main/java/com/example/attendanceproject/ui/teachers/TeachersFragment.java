package com.example.attendanceproject.ui.teachers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendanceproject.databinding.FragmentTeachersBinding;

public class TeachersFragment extends Fragment {

    private FragmentTeachersBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TeachersViewModel teachersViewModel =
                new ViewModelProvider(this).get(TeachersViewModel.class);

        binding = FragmentTeachersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textTeachers;
        teachersViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}