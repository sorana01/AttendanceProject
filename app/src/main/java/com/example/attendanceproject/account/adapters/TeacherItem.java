package com.example.attendanceproject.account.adapters;

public class TeacherItem extends EntityItem{
    private String fullName;
    private String phoneNumber;

    public TeacherItem(String fullName, String phoneNumber, String entityId) {
        super(fullName, phoneNumber, entityId);
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    // Getters and setters
    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isTeacher() {
        return true;
    }
}
