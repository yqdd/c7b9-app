package com.ow0b.c7b9.app.activity.chord;

public class MusicalNote
{
    public int pitch;
    public Boolean flat;
    public MusicalNote(int pitch, Boolean flat)
    {
        this.pitch = pitch;
        int pitchClass = pitch % 12;
        boolean isBlack = (pitchClass == 1 || pitchClass == 3 || pitchClass == 6 || pitchClass == 8 || pitchClass == 10);
        this.flat = isBlack ? flat : null;
    }
    public MusicalNote(int pitch)
    {
        this.pitch = pitch;
        int pitchClass = pitch % 12;
        boolean isBlack = (pitchClass == 1 || pitchClass == 3 || pitchClass == 6 || pitchClass == 8 || pitchClass == 10);
        this.flat = isBlack ? !StaffView.preferFlats : null;
    }
}
