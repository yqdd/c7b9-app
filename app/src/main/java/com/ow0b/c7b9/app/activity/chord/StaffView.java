package com.ow0b.c7b9.app.activity.chord;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 五线谱自定义 View（高音谱号）
 * 修正：MIDI -> 五线位置映射，保证 C4 (MIDI 60) 在五线下方一条加线（treble clef 正确位置）
 */
public class StaffView extends View
{
    private Paint staffPaint;
    private Paint notePaint;
    private TextPaint symbolPaint;
    private TextPaint accidentalPaint;
    private float lineSpacing; // 五线间距
    private float staffLeftPadding;
    private float staffTop;
    private float staffBottom;
    private List<MusicalNote> notes = new ArrayList<>(); // MIDI 列表
    private Integer keySignatureCount = null; // null = 自动默认
    public static boolean preferFlats = false; // 当 keySignatureCount==null 时使用的偏好（false = prefer sharps）
    private float noteSpacing = 80f; // 横向间距
    private float startX; // 第一个音符的起始 X

    public StaffView(Context context)
    {
        super(context);
        init();
    }
    public StaffView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    public StaffView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init()
    {
        staffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        staffPaint.setColor(Color.BLACK);
        staffPaint.setStrokeWidth(4f);

        notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setColor(Color.BLACK);
        notePaint.setStyle(Paint.Style.FILL);

        symbolPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        symbolPaint.setColor(Color.BLACK);
        symbolPaint.setTextSize(80f);
        symbolPaint.setTextAlign(Paint.Align.LEFT);

        accidentalPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        accidentalPaint.setColor(Color.BLACK);
        accidentalPaint.setTextSize(48f);
        accidentalPaint.setTextAlign(Paint.Align.LEFT);

        lineSpacing = 36f;
        staffLeftPadding = 24f;
        startX = 160f;
    }
    public void setNotes(List<MusicalNote> midiNotes)
    {
        notes = new ArrayList<>(midiNotes);
        invalidate();
    }
    public void setNote(MusicalNote note)
    {
        notes = new ArrayList<>();
        notes.add(note);
        invalidate();
    }
    public void setKeySignature(Integer count)
    {
        keySignatureCount = count;
        invalidate();
    }
    public void setPreferFlats(boolean prefer)
    {
        this.preferFlats = prefer;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int desiredHeight = (int) (lineSpacing * 6 + getPaddingTop() + getPaddingBottom() + 200);
        int width = resolveSize((int) (startX + noteSpacing * Math.max(1, notes.size()) + 100), widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
        // 计算 staffTop 和 staffBottom（五线垂直位置）
        float centerY = height / 2f;
        float totalStaffHeight = lineSpacing * 4; // 五线之间的总高度（4 * spacing）
        staffTop = centerY - totalStaffHeight / 2f;
        staffBottom = centerY + totalStaffHeight / 2f;
    }
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        // draw staff lines (5 lines)
        for (int i = 0; i < 5; i++)
        {
            float y = staffTop + i * lineSpacing;
            canvas.drawLine(staffLeftPadding, y, getWidth() - getPaddingRight() - 20, y, staffPaint);
        }
        // draw treble clef (高音谱号) using Unicode musical G clef (U+1D11E)
        float clefX = staffLeftPadding + 6f;
        float clefY = staffTop + lineSpacing * 2.5f; // roughly centered on staff
        symbolPaint.setTextSize(lineSpacing * 3f);
        canvas.drawText("\uD834\uDD1E", clefX, clefY, symbolPaint);
        // draw key signature if any (or if null, use preferFlats/auto behavior)
        Integer ks = keySignatureCount;
        boolean drawFlats;
        int ksCount = 0;
        if (ks == null)
        {
            drawFlats = preferFlats;
            ksCount = 0;
        }
        else
        {
            drawFlats = ks < 0;
            ksCount = Math.abs(ks);
        }
        float accX = clefX + lineSpacing * 2.2f;
        accidentalPaint.setTextSize(lineSpacing * 0.9f);
        if (ksCount > 0)
        {
            if (!drawFlats)
            {
                int[] order = {3, 0, 4, 1, 5, 2, 6}; // F C G D A E B -> degree index
                for (int i = 0; i < ksCount && i < 7; i++)
                {
                    float y = degreeIndexToY(order[i], 4); // octave 4 baseline
                    canvas.drawText("\u266F", accX, y + 8f, accidentalPaint); // ♯
                    accX += lineSpacing * 0.9f;
                }
            }
            else
            {
                int[] order = {6, 2, 5, 1, 4, 0, 3}; // B E A D G C F
                for (int i = 0; i < ksCount && i < 7; i++)
                {
                    float y = degreeIndexToY(order[i], 4);
                    canvas.drawText("\u266D", accX, y + 8f, accidentalPaint); // ♭
                    accX += lineSpacing * 0.9f;
                }
            }
        }
        // draw notes
        float x = startX;
        for (MusicalNote note : notes)
        {
            drawNoteAtMidi(canvas, note, x);
            x += noteSpacing;
        }
    }

    /**
     * 将 degreeIndex（C=0..B=6）在指定 octave 上转换为 Y 坐标
     * 使用规则（treble clef）：
     *  - 以 C4 (MIDI 60) 为基准 diatonicStep = 0，对应 y = staffTop + 5 * lineSpacing（五线下方一条加线）
     *  - 每个 diatonic step 对应 unit = lineSpacing / 2（line <-> space）
     */
    private float degreeIndexToY(int degreeIndex, int octave)
    {
        float unit = lineSpacing / 2f;
        int diatonicStep = (octave - 4) * 7 + degreeIndex; // C4 -> 0
        // C4 在 staffTop + 5 * lineSpacing（即底线 E4 下一个 lineSpacing）
        return staffTop + 5f * lineSpacing - diatonicStep * unit;
    }

    // 根据 MIDI 计算并画音符（音头 + 符干 + 附加线）
    private void drawNoteAtMidi(Canvas canvas, MusicalNote note, float x)
    {
        int pitchClass = note.pitch % 12;
        int octave = note.pitch / 12 - 1; // MIDI -> octave number (C4 = 60 => 4)
        boolean isBlack = (pitchClass == 1 || pitchClass == 3 || pitchClass == 6 || pitchClass == 8 || pitchClass == 10);
        boolean useFlatsForThis = !Objects.requireNonNullElse(note.flat, !preferFlats);
        //if (keySignatureCount != null) useFlatsForThis = keySignatureCount < 0;
        int degreeIndex;
        String accidental = null;
        if (!isBlack) degreeIndex = pitchClassToDegreeIndex(pitchClass);
        else
        {
            if (useFlatsForThis)
            {
                switch (pitchClass)
                {
                    case 1: degreeIndex = 1; accidental = "flat"; break; // Db -> D♭
                    case 3: degreeIndex = 2; accidental = "flat"; break; // Eb
                    case 6: degreeIndex = 4; accidental = "flat"; break; // Gb
                    case 8: degreeIndex = 5; accidental = "flat"; break; // Ab
                    case 10: degreeIndex = 6; accidental = "flat"; break; // Bb
                    default: degreeIndex = 0; accidental = "flat"; break;
                }
            }
            else
            {
                switch (pitchClass)
                {
                    case 1: degreeIndex = 0; accidental = "sharp"; break; // C#
                    case 3: degreeIndex = 1; accidental = "sharp"; break; // D#
                    case 6: degreeIndex = 3; accidental = "sharp"; break; // F#
                    case 8: degreeIndex = 4; accidental = "sharp"; break; // G#
                    case 10: degreeIndex = 5; accidental = "sharp"; break; // A#
                    default: degreeIndex = 0; accidental = "sharp"; break;
                }
            }
        }
        // 计算 diatonicStep 相对于 C4，然后求 Y
        int diatonicStep = (octave - 4) * 7 + degreeIndex;
        float unit = lineSpacing / 2f;
        float y = staffTop + 5f * lineSpacing - diatonicStep * unit;
        // 绘制 accidental（简化：总是在黑键上显示 accidental）
        if (accidental != null)
        {
            String accSymbol = accidental.equals("sharp") ? "\u266F" : "\u266D";
            canvas.drawText(accSymbol, x - lineSpacing * 0.9f, y + 10f, accidentalPaint);
        }
        // 绘制音头（椭圆）
        float noteRadiusX = lineSpacing * 0.9f;
        float noteRadiusY = lineSpacing * 0.65f;
        Path oval = new Path();
        oval.addOval(x, y - noteRadiusY, x + noteRadiusX, y + noteRadiusY, Path.Direction.CW);
        canvas.drawPath(oval, notePaint);
        // 绘制符干（向上或向下）
        float staffCenterY = staffTop + lineSpacing * 2f; // 中线位置
        float stemLength = lineSpacing * 3f;
        if (y > staffCenterY)
        {
            // 向上符干（音头右侧，向上）
            float stemX = x + noteRadiusX;
            float stemTopY = y - stemLength;
            canvas.drawLine(stemX, y, stemX, stemTopY, staffPaint);
        }
        else
        {
            // 向下符干（音头左侧，向下）
            float stemX = x;
            float stemBottomY = y + stemLength;
            canvas.drawLine(stemX, y, stemX, stemBottomY, staffPaint);
        }

        // 附加线（当音符在五线外）
        int bottomLineDiatonic = (4 - 4) * 7 + 2; // E4 -> diatonicStep = 2
        int topLineDiatonic = bottomLineDiatonic + 8; // F5 -> diatonicStep = 10
        if (diatonicStep < bottomLineDiatonic || diatonicStep > topLineDiatonic)
        {
            if (diatonicStep < bottomLineDiatonic)
            {
                // 向下多条线（每两个 diatonicStep 一条实线）
                for (int s = diatonicStep; s <= bottomLineDiatonic - 1; s += 2)
                {
                    float ly = staffTop + 5f * lineSpacing - s * unit;
                    canvas.drawLine(x - noteRadiusX * 1.1f, ly, x + noteRadiusX * 1.8f, ly, staffPaint);
                }
            }
            else
            {
                // 向上多条线
                for (int s = topLineDiatonic + 1; s <= diatonicStep; s += 2)
                {
                    float ly = staffTop + 5f * lineSpacing - s * unit;
                    canvas.drawLine(x - noteRadiusX * 1.1f, ly, x + noteRadiusX * 1.8f, ly, staffPaint);
                }
            }
        }
    }

    // pitchClass (0..11) -> natural degree index (C=0..B=6)
    private int pitchClassToDegreeIndex(int pitchClass)
    {
        switch (pitchClass)
        {
            case 0: return 0; // C
            case 2: return 1; // D
            case 4: return 2; // E
            case 5: return 3; // F
            case 7: return 4; // G
            case 9: return 5; // A
            case 11: return 6; // B
            default: return 0;
        }
    }
}