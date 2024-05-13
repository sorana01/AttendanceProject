package com.example.attendanceproject.account.adapters;

import android.graphics.Bitmap;

public class FaceItem {
    private Bitmap faceImage;
    private String name;

    public FaceItem(Bitmap faceImage, String name) {
        this.faceImage = faceImage;
        this.name = name;
    }

    public Bitmap getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(Bitmap faceImage) {
        this.faceImage = faceImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
