package com.example.attendanceproject.account.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
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
    private Context context;
    private List<FaceItem> faceItemList;
    private LayoutInflater inflater;
    private OnEditTextChanged listener; // Interface listener

    public interface OnEditTextChanged {
        void onEditTextVisibilityChange(boolean shouldShowSave);
    }

    public FaceAdapter(Context context, List<FaceItem> faceItemList, OnEditTextChanged listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.faceItemList = faceItemList;
        this.listener = listener;
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

        // Clear and reset text watcher
        if (holder.etName.getTag() instanceof TextWatcher) {
            holder.etName.removeTextChangedListener((TextWatcher) holder.etName.getTag());
        }

        holder.etName.setText(currentFace.getName());  // Reset the text to the current item

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Inform that Save button state might need to change
                listener.onEditTextVisibilityChange(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Update item with current text
                faceItemList.get(holder.getAdapterPosition()).setName(editable.toString());
            }
        };

        holder.etName.addTextChangedListener(textWatcher);
        holder.etName.setTag(textWatcher);

        holder.etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                listener.onEditTextVisibilityChange(true);
                faceItemList.get(holder.getAdapterPosition()).setName(holder.etName.getText().toString());
            }
        });
    }




    @Override
    public int getItemCount() {
        return faceItemList.size();
    }
}
