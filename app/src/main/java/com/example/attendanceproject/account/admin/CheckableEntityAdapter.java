package com.example.attendanceproject.account.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import com.example.attendanceproject.R;
import java.util.ArrayList;

public class CheckableEntityAdapter extends EntityAdapter{
    public CheckableEntityAdapter(Context context, ArrayList<EntityItem> entityItems) {
        super(context, entityItems);
    }

    public static class CheckableEntityViewHolder extends EntityViewHolder {
        public CheckBox checkBox;  // Additional component

        public CheckableEntityViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.entity_checkbox);
        }
    }

    @NonNull
    @Override
    public CheckableEntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkable_entity_item, parent, false);
        return new CheckableEntityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EntityViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        CheckableEntityItem item = (CheckableEntityItem) getEntityItems().get(position);
        ((CheckableEntityViewHolder) holder).checkBox.setChecked(item.isChecked());

        ((CheckableEntityViewHolder) holder).checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
        });
    }
}
