package com.ow0b.c7b9.app.activity.chord;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChordUtil
{
    public static List<Integer> correctChord = new ArrayList<>();
    public static final String PREFS_NAME = "ChordSettings";
    public static final String KEY_ALLOWED_INTERVALS = "allowed_intervals";
    public static final String KEY_NUM_NOTES = "num_notes";

    // 默认允许的音程选项
    public static final String[] DEFAULT_INTERVALS = {"大小三度", "减增三度", "倍增减三度", "大小二度", "纯四度"};
    // 默认的叠加音个数
    public static final int DEFAULT_NUM_NOTES = 3;

    /**
     * 根据设置随机生成一个和弦。
     * 和弦第一个音为根音，采用 60~71 内的随机值（对应 C~B, 采用同一八度），后续音根据允许音程随机叠加生成，
     * 若超出 87 则直接置为 87（钢琴按键范围为 0～87）。
     */
    public static List<Integer> generateChord(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 从 SharedPreferences 获取允许的音程，默认所有音程均允许
        String intervalsStr = prefs.getString(KEY_ALLOWED_INTERVALS, "大小三度,减增三度,倍增减三度,大小二度,纯四度");
        String[] allowedIntervals = intervalsStr.split(",");
        int numNotes = prefs.getInt(KEY_NUM_NOTES, DEFAULT_NUM_NOTES);

        List<Integer> chord = new ArrayList<>();
        Random random = new Random();
        // 选择根音：在 60～71 之间（即 C～B）
        int root = 60 + random.nextInt(12);
        chord.add(root);
        int currentNote = root;

        for (int i = 1; i < numNotes; i++)
        {
            // 随机选取允许的音程
            String intervalType = allowedIntervals[random.nextInt(allowedIntervals.length)].trim();
            int interval = getIntervalSemitones(intervalType, random);
            currentNote += interval;
            if (currentNote > 87)
            {
                currentNote = 87;
            }
            chord.add(currentNote);
        }
        correctChord = chord;
        return chord;
    }

    /**
     * 根据音程类型映射为半音数：
     * - "大小二度"：随机为 1 或 2 个半音
     * - "大小三度"：随机为 3 或 4 个半音
     * - "减增三度"：随机为 2 或 5 个半音
     * - "倍增减三度"：固定 6 个半音
     * - "纯四度"  ：固定 5 个半音
     */
    private static int getIntervalSemitones(String intervalType, Random random)
    {
        switch(intervalType)
        {
            case "大小二度":
                return random.nextBoolean() ? 1 : 2;
            case "大小三度":
                return random.nextBoolean() ? 3 : 4;
            case "减增三度":
                return random.nextBoolean() ? 2 : 5;
            case "倍增减三度":
                return 6;
            case "纯四度":
                return 5;
            default:
                return 3;
        }
    }
}