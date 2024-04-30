package com.example.attendanceproject.ui.accept_acc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AcceptAccViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AcceptAccViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is accept accounts fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}