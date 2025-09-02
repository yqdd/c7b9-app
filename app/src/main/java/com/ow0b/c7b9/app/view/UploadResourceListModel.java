package com.ow0b.c7b9.app.view;

import androidx.lifecycle.ViewModel;

import com.ow0b.c7b9.app.old.activity.main.AudioService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class UploadResourceListModel extends ViewModel
{
    @Inject
    public AudioService audioService;
}
