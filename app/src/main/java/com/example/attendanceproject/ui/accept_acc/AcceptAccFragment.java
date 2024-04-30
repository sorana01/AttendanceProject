package com.example.attendanceproject.ui.accept_acc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendanceproject.databinding.FragmentAcceptAccBinding;

public class AcceptAccFragment extends Fragment {

    private FragmentAcceptAccBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AcceptAccViewModel acceptAccViewModel =
                new ViewModelProvider(this).get(AcceptAccViewModel.class);

        binding = FragmentAcceptAccBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAcceptAcc;
        acceptAccViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}