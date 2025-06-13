package com.ow0b.c7b9.app.util.midi;

import androidx.annotation.NonNull;

public class Note
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
