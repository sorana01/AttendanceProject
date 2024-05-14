package com.example.attendanceproject.account.adapters;

public class CheckableEntityItem extends EntityItem {
    private boolean isChecked;  // This field is specific to the checkable item

    public CheckableEntityItem(String entityName, String entityDetail) {
        super(entityName, entityDetail);
        this.isChecked = false;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
