package com.ow0b.c7b9.app.activity.chord;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.databinding.ActivityToolChordBinding;
import com.ow0b.c7b9.app.util.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChordComposeActivity extends Activity
{
    private ActivityToolChordBinding binding;
    private Handler handler = new Handler();
    private List<MusicalNote> userChord = new ArrayList<>();

    // 12 个音按钮名称（对应 C, C#, D, ... B）
    private String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityToolChordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MidiPlayer.init(this);
        binding.setting.setOnClickListener(v -> startActivity(new Intent(this, ChordSettingActivity.class)));
        binding.playListenButton.setOnClickListener(v ->
        {
            List<Integer> list = new ArrayList<>();
            userChord.forEach(n -> list.add(n.pitch));
            playChord(false, list);
        });
        binding.playChordButton.setOnClickListener(v -> playChord(false, null));
        binding.playSplitChordButton.setOnClickListener(v -> playChord(true, null));
        binding.clearChordButton.setOnClickListener(v ->
        {
            userChord.clear();
            updateChordDisplay();
        });
        binding.back.setOnClickListener(v -> finish());

        // 设置 12 个根音按钮
        for (int i = 0; i < 12; i++)
        {
            Button btn = getButton(i);
            binding.noteButtonsContainer.addView(btn);
        }
        for(int i = 0; i < binding.intervalButtonsContainer.getChildCount(); i ++)
        {
            if(binding.intervalButtonsContainer.getChildAt(i) instanceof ViewGroup vg)
            {
                for(int j = 0; j < 2; j ++)
                {
                    Button btn = (Button) vg.getChildAt(j);
                    btn.setOnClickListener(v ->
                    {
                        if(!userChord.isEmpty())
                        {
                            // 使用与 ChordUtil 中相同的映射规则
                            Semitones semitones = getIntervalSemitones(btn.getText().toString());
                            MusicalNote lastNote = userChord.get(userChord.size() - 1);
                            MusicalNote nextNote = transpose(lastNote, semitones.pitch, semitones.interval);
                            if(nextNote != null)
                            {
                                userChord.add(nextNote);
                                updateChordDisplay();
                            }
                        }
                    });
                }
            }
        }
        binding.submitChordButton.setOnClickListener(v ->
        {
            if(!ChordUtil.correctChord.isEmpty())
            {
                binding.noteButtonsContainer.setVisibility(View.GONE);
                binding.intervalButtonsContainer.setVisibility(View.GONE);
                binding.submitButtonsContainer.setVisibility(View.GONE);
                binding.answerDisplay.setVisibility(View.VISIBLE);
                if(isAnswerCorrect())
                {
                    binding.answerTextDisplay.setText("回答正确");
                    binding.answerChordDisplay.setVisibility(View.GONE);
                }
                else
                {
                    binding.answerTextDisplay.setText("回答错误，正确答案：");
                    binding.answerChordDisplay.setVisibility(View.VISIBLE);
                    List<MusicalNote> notes = new ArrayList<>();
                    ChordUtil.correctChord.forEach(i -> notes.add(new MusicalNote(i)));
                    binding.answerChordDisplay.setNotes(notes);
                }
            }
        });
        binding.answerNextDisplay.setOnClickListener(v ->
        {
            binding.noteButtonsContainer.setVisibility(View.VISIBLE);
            binding.intervalButtonsContainer.setVisibility(View.VISIBLE);
            binding.submitButtonsContainer.setVisibility(View.VISIBLE);
            binding.answerDisplay.setVisibility(View.GONE);
            binding.clearChordButton.performClick();
            ChordUtil.generateChord(this);
        });
    }
    private boolean isAnswerCorrect()
    {
        if(ChordUtil.correctChord.size() != userChord.size()) return false;
        for(int i = 0; i < userChord.size(); i ++)
        {
            if(ChordUtil.correctChord.get(i) != userChord.get(i).pitch)
                return false;
        }
        return true;
    }

    @NonNull
    private Button getButton(int i)
    {
        final int noteIndex = i;
        Button btn = new Button(this);
        btn.setText(noteNames[i]);
        btn.setOnClickListener(v ->
        {
            if (userChord.isEmpty())
            {
                // 设定根音，映射为 60 + noteIndex（确保与播放页面的和弦生成一致）
                int root = 60 + noteIndex;
                userChord.add(new MusicalNote(root));
                updateChordDisplay();
            }
        });
        return btn;
    }

    // 自然音对应的 pitch class (C=0, D=2, E=4, F=5, G=7, A=9, B=11)
    private static final int[] NATURAL_PITCH_CLASS = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] LETTER_NAMES = {"C", "D", "E", "F", "G", "A", "B"};
    public MusicalNote transpose(MusicalNote note, int pitch, int degree)
    {
        // 解析原始音符的音名字母索引和升降偏移量
        NoteInfo srcInfo = analyzeNote(note);
        if(srcInfo == null) return null;
        int srcLetterIdx = srcInfo.letterIndex;   // 0=C,1=D,...,6=B
        int srcAccidental = srcInfo.accidental;   // -1降, 0无, 1升
        // 计算目标字母索引
        int degreeOffset = degree - 1;
        int targetLetterIdx = Math.floorMod(srcLetterIdx + degreeOffset, 7);
        // 计算目标 MIDI 音高
        int targetPitch = note.pitch + pitch;
        int targetPitchClass = Math.floorMod(targetPitch, 12);
        // 目标字母的自然半音值
        int naturalTargetPitchClass = NATURAL_PITCH_CLASS[targetLetterIdx];
        // 计算所需的升降偏移量 (diff 在 -6..5 之间)
        int diff = (targetPitchClass - naturalTargetPitchClass + 12) % 12;
        if (diff > 6) diff -= 12;
        // 根据 diff 确定 flat 值
        Boolean flat;
        if (diff == 0) flat = null;
        else if (diff > 0) flat = true;   // 升号
        else flat = false;  // 降号
        //调整上一个音的升降号以符合音程
        int naturalPC = (targetPitch + (flat == null ? 0 : (flat ? -1 : 1) + 12)) % 12;
        int letterIdx = indexOf(NATURAL_PITCH_CLASS, naturalPC);
        if(letterIdx != -1 && Math.abs(letterIdx - srcInfo.letterIndex) != degree)
        {
            if(note.flat != null) note.flat = !note.flat;
        }
        // 实际音乐中由音程决定的 diff 通常不会超过 ±1（如增四度到升 F，diff=1）
        return new MusicalNote(targetPitch, flat);
    }
    private record NoteInfo(int letterIndex, int accidental) {}
    /// 解析音符，返回字母索引和升降偏移量
    private NoteInfo analyzeNote(MusicalNote note)
    {
        int pitchClass = Math.floorMod(note.pitch, 12);
        Boolean flat = note.flat;
        int letterIdx, accidental;// -1 降, 0 自然, 1 升
        if (flat == null)
        {
            // 白键，直接匹配自然音
            letterIdx = indexOf(NATURAL_PITCH_CLASS, pitchClass);
            accidental = 0;
        }
        else if (flat)
        {
            // 升号：pitchClass 比自然音高 1 个半音
            int naturalPC = (pitchClass - 1 + 12) % 12;
            letterIdx = indexOf(NATURAL_PITCH_CLASS, naturalPC);
            accidental = 1;
        }
        else
        {
            // 降号：pitchClass 比自然音低 1 个半音
            int naturalPC = (pitchClass + 1) % 12;
            letterIdx = indexOf(NATURAL_PITCH_CLASS, naturalPC);
            accidental = -1;
        }
        if(letterIdx == -1)
        {
            Toast.showInfo(this, "不存在的和弦");
            return null;
        }
        return new NoteInfo(letterIdx, accidental);
    }
    private int indexOf(int[] arr, int value)
    {
        for (int i = 0; i < arr.length; i++)
            if (arr[i] == value) return i;
        return -1;
    }

    private void updateChordDisplay()
    {
        binding.chordDisplay.setNotes(userChord);
    }

    private int getExpectedNumNotes()
    {
        return ChordUtil.numNotes;
    }

    private record Semitones(int pitch, int interval) {}
    // 与 ChordUtil 中的音程映射保持一致
    private Semitones getIntervalSemitones(String intervalType)
    {
        return switch (intervalType)
        {
            case "小二度" -> new Semitones(1, 2);
            case "大二度" -> new Semitones(2, 2);
            case "小三度" -> new Semitones(3, 3);
            case "大三度" -> new Semitones(4, 3);
            case "纯四度" -> new Semitones(5, 4);
            case "增四度" -> new Semitones(6, 4);
            case "减五度" -> new Semitones(6, 5);
            case "纯五度" -> new Semitones(7, 5);
            case "小六度" -> new Semitones(8, 6);
            case "大六度" -> new Semitones(9, 6);
            case "小七度" -> new Semitones(10, 7);
            case "大七度" -> new Semitones(11, 7);
            case "纯八度" -> new Semitones(12, 8);
            default -> throw new RuntimeException(intervalType);
        };
    }

    /// 根据设置生成和弦，并依次播放和弦中的每个音（延时 300ms 播放下一个音）。
    private void playChord(boolean split, List<Integer> chord)
    {
        handler.removeCallbacksAndMessages(null);
        MidiPlayer.stopAllKeys();
        List<Integer> c = Objects.requireNonNullElse(chord,
                ChordUtil.correctChord.isEmpty() ? ChordUtil.generateChord(this) : ChordUtil.correctChord);
        int delay = 0;
        for (final int note : c)
        {
            if(split)
            {
                handler.postDelayed(() -> MidiPlayer.playKey(note), delay);
                delay += 300;
            }
            else MidiPlayer.playKey(note);
            handler.postDelayed(() -> MidiPlayer.stopKey(note), delay + 600);
        }
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        MidiPlayer.stopAllKeys();
    }
    @Override
    public void finish()
    {
        super.finish();
        handler.removeCallbacksAndMessages(null);
        MidiPlayer.stopAllKeys();
    }
}