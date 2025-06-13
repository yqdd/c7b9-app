package com.ow0b.c7b9.app.util.midi;

import androidx.annotation.NonNull;
import java.util.*;

public class Midi
{
    public String name;
    public float totalTime;
    public LinkedList<Note> notes;
    public List<Integer> indexes;
    public ArrayList<HashSet<Note>> noteGroup;

    @NonNull
    @Override
    public String toString()
    {
        return name;
    }
}
