package com.example.attendanceproject.account.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;

import java.util.List;

public class FaceAdapter extends RecyclerView.Adapter<FaceAdapter.FaceViewHolder> {
    private List<FaceItem> faceItemList;
    private LayoutInflater inflater;

    public FaceAdapter(Context context, List<FaceItem> faceItemList) {
        this.inflater = LayoutInflater.from(context);
        this.faceItemList = faceItemList;
    }

    public static class FaceViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFace;
        EditText etName;

        public FaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFace = itemView.findViewById(R.id.imageCroppedFace);
            etName = itemView.findViewById(R.id.editTextName);
        }
    }

    @NonNull
    @Override
    public FaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_face, parent, false);
        return new FaceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceViewHolder holder, int position) {
        FaceItem currentFace = faceItemList.get(position);
        holder.imgFace.setImageBitmap(currentFace.getFaceImage());
        holder.etName.setText(currentFace.getName());

        // Set the EditText editable only if the name is "Unknown"
        if ("Unknown".equals(currentFace.getName())) {
            holder.etName.setEnabled(true);
            holder.etName.setFocusableInTouchMode(true);  // Enable editing and focusing
        } else {
            holder.etName.setEnabled(false);
            holder.etName.setFocusable(false);
        }

        // Set OnFocusChangeListener to handle focus loss
        holder.etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && "Unknown".equals(currentFace.getName())) {
                    // If focus is lost and the original name was "Unknown"
                    final EditText editText = (EditText) v;
                    String newName = editText.getText().toString();

                    // Confirmation dialog
                    new AlertDialog.Builder(holder.itemView.getContext())
                            .setTitle("Confirm Name")
                            .setMessage("Do you want to save this name: " + newName + "?")
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Save the new name
                                    currentFace.setName(newName);
                                    editText.setEnabled(false);
                                    editText.setFocusable(false);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Revert the text to "Unknown"
                                    editText.setText("Unknown");
                                }
                            })
                            .show();
                }
            }
        });
    }
    

    @Override
    public int getItemCount() {
        return faceItemList.size();
    }
}
