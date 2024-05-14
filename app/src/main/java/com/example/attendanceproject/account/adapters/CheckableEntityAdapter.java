package com.example.attendanceproject.account.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import com.example.attendanceproject.R;

import java.util.ArrayList;
import java.util.HashSet;

public class CheckableEntityAdapter extends EntityAdapter<CheckableEntityItem> {
    private HashSet<Integer> checkedPositions = new HashSet<>();

    public CheckableEntityAdapter(Context context, ArrayList<CheckableEntityItem> entityItems) {
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
        CheckableEntityItem item = entityItems.get(position);
        CheckableEntityViewHolder checkableHolder = (CheckableEntityViewHolder) holder;

        checkableHolder.checkBox.setChecked(item.isChecked());
        checkableHolder.checkBox.setOnCheckedChangeListener(null);  // Detach the listener to prevent firing during initial setup

        checkableHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            if (isChecked) {
                checkedPositions.add(position);
            } else {
                checkedPositions.remove(position);
            }
        });
    }

    // Method to retrieve a list of checked items
    public ArrayList<CheckableEntityItem> getCheckedItems() {
        ArrayList<CheckableEntityItem> checkedItems = new ArrayList<>();
        for (int position : checkedPositions) {
            if (position < entityItems.size()) {  // Check if position is valid
                checkedItems.add(entityItems.get(position));
            }
        }
        return checkedItems;
    }

    public void clearCheckedPositions() {
        checkedPositions.clear();
    }

}
