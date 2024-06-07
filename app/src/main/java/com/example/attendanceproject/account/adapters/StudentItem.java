package com.example.attendanceproject.account.adapters;

import com.example.attendanceproject.face_rec.FaceClassifier;

public class StudentItem extends EntityItem{
    private String fullName;
    private String phoneNumber;
    private String ssn;
    private String studentId;
    private String imageUrl;

    public StudentItem(String fullName, String phoneNumber, String ssn, String studentId, String imageUrl, String entityId) {
        super(fullName, phoneNumber, entityId);
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.ssn = ssn;
        this.studentId = studentId;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getSsn() {
        return ssn;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isTeacher() {
        return false;
    }
}
