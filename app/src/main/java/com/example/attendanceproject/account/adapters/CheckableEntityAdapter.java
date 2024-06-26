package com.example.attendanceproject.account.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import com.example.attendanceproject.R;

import java.util.ArrayList;

public class CheckableEntityAdapter extends EntityAdapter<CheckableEntityItem> {

    public CheckableEntityAdapter(Context context, ArrayList<CheckableEntityItem> entityItems) {
        super(context, entityItems);
    }

    public static class CheckableEntityViewHolder extends EntityViewHolder {
        public CheckBox checkBox;  // Checkbox component

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

        // Detach the listener to prevent firing during initial setup
        checkableHolder.checkBox.setOnCheckedChangeListener(null);
        // Set checked state based on item state
        checkableHolder.checkBox.setChecked(item.isChecked());

        // Attach the listener and update item state when checkbox is toggled
        checkableHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the item's checked state whenever the checkbox is toggled
            item.setChecked(isChecked);
        });
    }

    // Method to retrieve a list of checked items directly from entityItems
    public ArrayList<CheckableEntityItem> getCheckedItems() {
        ArrayList<CheckableEntityItem> checkedItems = new ArrayList<>();
        for (CheckableEntityItem item : entityItems) {
            if (item.isChecked()) {
                checkedItems.add(item);
            }
        }
        return checkedItems;
    }

    public void clearCheckedPositions() {
        for (CheckableEntityItem item : entityItems) {
            item.setChecked(false);
        }
        notifyDataSetChanged();
    }

}
