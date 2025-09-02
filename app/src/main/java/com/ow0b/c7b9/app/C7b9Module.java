package com.ow0b.c7b9.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)    //注入到应用程序的生命周期
public class C7b9Module
{
    @Provides
    @Singleton
    public Gson gson()
    {
        return new GsonBuilder().serializeNulls().create();
    }
}
