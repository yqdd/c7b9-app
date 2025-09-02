package com.ow0b.c7b9.app;

import android.content.Context;

import java.io.File;
import java.nio.file.Path;

public interface TempDir
{
    default File temp(Context context, String name)
    {
        return context.getCacheDir().toPath().resolve(name).toFile();
    }
    default File file(Context context, String name)
    {
        return context.getFilesDir().toPath().resolve(name).toFile();
    }
}
