package com.example.attendanceproject.ui.teachers;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TeachersViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TeachersViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is teachers fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}