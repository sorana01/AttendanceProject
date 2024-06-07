package com.example.attendanceproject.account.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.attendanceproject.R;

public class FullScreenImageDialog extends DialogFragment {
    private static final String ARG_IMAGE_URL = "image_url";

    public static FullScreenImageDialog newInstance(String imageUrl) {
        FullScreenImageDialog fragment = new FullScreenImageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_full_screen_image, container, false);
        ImageView imageView = view.findViewById(R.id.full_screen_image);

        String imageUrl = getArguments().getString(ARG_IMAGE_URL);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.avatar)
                .into(imageView);

        return view;
    }
}
