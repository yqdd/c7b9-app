package com.ow0b.c7b9.app.old.util.midi;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Note implements Serializable
{
    public String note;
    public byte pitch;
    public float start;
    public float end;
    public int force;
    public Note(String note, int pitch, float start, int force)
    {
        this.note = note;
        this.pitch = (byte) pitch;
        this.start = start;
        this.force = force;
    }

    @NonNull
    @Override
    public String toString()
    {
        return note;
    }
}
