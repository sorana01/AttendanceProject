package com.example.attendanceproject.ui.courses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;
import com.example.attendanceproject.account.admin.ClassAdapter;
import com.example.attendanceproject.account.admin.ClassItem;
import com.example.attendanceproject.databinding.FragmentCoursesBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private ClassAdapter classAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ClassItem> classItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance(); // Initialize Firestore once

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        classAdapter = new ClassAdapter(getActivity(), classItems);
        recyclerView.setAdapter(classAdapter);

        FloatingActionButton fab = getActivity().findViewById(R.id.fab); // Access FAB from the Activity
        if (fab != null) {
            fab.setOnClickListener(v -> {
                // Define what happens when FAB is clicked in CoursesFragment
                showAddCourseDialog();
            });
        }
    }

    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.class_dialog, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText class_edt = view.findViewById(R.id.class_edt);
        EditText subject_edt = view.findViewById(R.id.subject_edt);
        Button cancel_btn = view.findViewById(R.id.cancel_btn);
        Button add_btn = view.findViewById(R.id.add_btn);

        cancel_btn.setOnClickListener(v -> dialog.dismiss());
        add_btn.setOnClickListener(v -> {
            String className = class_edt.getText().toString();
            String subjectName = subject_edt.getText().toString();
            classItems.add(new ClassItem(className, subjectName));
            classAdapter.notifyDataSetChanged();
            dialog.dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}