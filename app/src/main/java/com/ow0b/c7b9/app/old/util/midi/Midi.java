package com.ow0b.c7b9.app.old.util.midi;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.*;

public class Midi implements Serializable
{
    public String name;
    public float totalTime;
    public LinkedList<Note> notes = new LinkedList<>();
    public List<Integer> indexes = new LinkedList<>();
    public ArrayList<HashSet<Note>> noteGroup = new ArrayList<>();

    @NonNull
    @Override
    public String toString()
    {
        return name;
    }
}
