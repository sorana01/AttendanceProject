package com.example.attendanceproject.account.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.attendanceproject.R;
import com.example.attendanceproject.account.student.FullScreenImageDialog;

import java.util.ArrayList;

public class AcceptAccEntityAdapter extends EntityAdapter<EntityItem>{
    private static final int VIEW_TYPE_STUDENT = 1;
    private static final int VIEW_TYPE_TEACHER = 2;

    /**
     * Initialize the dataset of the Adapter
     *
     * @param context
     * @param entityItems ArrayList<T> containing the data to populate views to be used
     *                    by RecyclerView
     */
    public AcceptAccEntityAdapter(Context context, ArrayList<EntityItem> entityItems) {
        super(context, entityItems);
    }


    @Override
    public int getItemViewType(int position) {
        if (entityItems.get(position) instanceof StudentItem) {
            return VIEW_TYPE_STUDENT;
        } else if (entityItems.get(position) instanceof TeacherItem) {
            return VIEW_TYPE_TEACHER;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public EntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_STUDENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
            return new StudentViewHolder(view);
        } else if (viewType == VIEW_TYPE_TEACHER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
            return new TeacherViewHolder(view);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull EntityViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        EntityItem item = entityItems.get(position);
        if (viewType == VIEW_TYPE_STUDENT) {
            ((StudentViewHolder) holder).bind((StudentItem) item);
        } else if (viewType == VIEW_TYPE_TEACHER) {
            ((TeacherViewHolder) holder).bind((TeacherItem) item);
        } else {
            super.onBindViewHolder((EntityViewHolder) holder, position);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    static class StudentViewHolder extends EntityViewHolder {
        private TextView fullName;
        private TextView phoneNumber;
        private TextView ssn;
        private TextView studentId;
        private ImageView photo;

        StudentViewHolder(View itemView) {
            super(itemView);
            fullName = itemView.findViewById(R.id.student_full_name);
            phoneNumber = itemView.findViewById(R.id.student_phone_number);
            ssn = itemView.findViewById(R.id.student_ssn);
            studentId = itemView.findViewById(R.id.student_id);
            photo = itemView.findViewById(R.id.student_photo);
        }

        void bind(StudentItem student) {
            fullName.setText(student.getFullName());
            phoneNumber.setText(student.getPhoneNumber());
            ssn.setText(student.getSsn());
            studentId.setText(student.getStudentId());
            // Load the image using Glide
            Glide.with(photo.getContext())
                    .load(student.getImageUrl())
                    .placeholder(R.drawable.avatar)
                    .into(photo);

            photo.setOnClickListener(v -> {
                FullScreenImageDialog dialog = FullScreenImageDialog.newInstance(student.getImageUrl());
                dialog.show(((FragmentActivity) photo.getContext()).getSupportFragmentManager(), "full_screen_image");
            });
        }

    }

    static class TeacherViewHolder extends EntityViewHolder {
        private TextView fullName;
        private TextView phoneNumber;

        TeacherViewHolder(View itemView) {
            super(itemView);
            fullName = itemView.findViewById(R.id.teacher_full_name);
            phoneNumber = itemView.findViewById(R.id.teacher_phone_number);
        }

        void bind(TeacherItem teacher) {
            fullName.setText(teacher.getFullName());
            phoneNumber.setText(teacher.getPhoneNumber());
        }
    }
}
