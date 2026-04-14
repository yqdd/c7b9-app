package com.ow0b.c7b9.app.activity.chord;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChordUtil
{
    public static List<Integer> correctChord = new ArrayList<>();
    public static boolean[] allowedInterval = new boolean[] { true, true, true, true, true, true, false, false, false, false, false };
    /// 叠加音个数
    public static int numNotes = 3;


    /**
     * 根据设置随机生成一个和弦。
     * 和弦第一个音为根音，采用 60~71 内的随机值（对应 C~B, 采用同一八度），后续音根据允许音程随机叠加生成，
     * 若超出 87 则直接置为 87（钢琴按键范围为 0～87）。
     */
    public static List<Integer> generateChord(Context context)
    {
        int numNotes = ChordUtil.numNotes;
        List<Integer> chord = new ArrayList<>();
        Random random = new Random();
        // 选择根音：在 60～71 之间（即 C～B）
        int root = 60 + random.nextInt(12);
        chord.add(root);
        int currentNote = root;

        for (int i = 1; i < numNotes; i++)
        {
            int v = getIntervalSemitones(random);
            if(v == -1) Toast.makeText(context, "当前未设置可以生成的音程", android.widget.Toast.LENGTH_SHORT).show();
            currentNote += v;
            if (currentNote > 87) currentNote = 87;
            chord.add(currentNote);
        }
        correctChord = chord;
        return chord;
    }
    private static int getIntervalSemitones(Random random)
    {
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < allowedInterval.length; i ++)
            if(allowedInterval[i]) list.add(i + 1);
        return list.isEmpty() ? -1 : list.get(random.nextInt(list.size()));
    }
}