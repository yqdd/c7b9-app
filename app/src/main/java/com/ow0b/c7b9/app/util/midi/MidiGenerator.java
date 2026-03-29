package com.ow0b.c7b9.app.util.midi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MidiGenerator
{
    // 设定每拍的tick数（ticks per quarter note）
    private static final int RESOLUTION = 480;
    // tempo: 500,000 微秒/拍（即每拍 0.5 秒，120 BPM）
    private static final int TEMPO = 500000;
    // 根据 tempo 和 RESOLUTION 计算每秒的 tick 数
    // 每拍 = 0.5秒，则每秒 tick 数 = RESOLUTION / 0.5 = 960
    private static final int TICKS_PER_SECOND = 960;

    /**
     * 将 List<Note> 转换为标准 MIDI 文件的二进制数据.
     * @param notes 音符列表，每个 Note 的 start 和 end 单位均为秒
     * @return MIDI 文件的二进制内容
     * @throws IOException
     */
    public static byte[] generateMidiFile(List<Note> notes) throws IOException {
        // 将 Note 转换为 MIDI 事件
        List<MidiEvent> events = new ArrayList<>();
        for (Note note : notes) {
            // 将秒转换为 tick (四舍五入)
            int startTick = Math.round(note.start * TICKS_PER_SECOND);
            int endTick = Math.round(note.end * TICKS_PER_SECOND);
            // note on 事件：0x90 表示音符按下，所属通道 0
            events.add(new MidiEvent(startTick, 0x90, note.pitch, note.force));
            // note off 事件：0x80 表示音符松开，velocity 设为0
            events.add(new MidiEvent(endTick, 0x80, note.pitch, 0));
        }
        // 对事件按 tick 时间排序，如果 tick 相同，则 note off 放在 note on 前面以确保正确处理重叠音符
        Collections.sort(events, (e1, e2) ->
        {
            if (e1.tick != e2.tick) return Integer.compare(e1.tick, e2.tick);
            else return Integer.compare(e1.status, e2.status);
        });

        // 构造 track chunk 数据
        ByteArrayOutputStream trackData = new ByteArrayOutputStream();
        int lastTick = 0;
        for (MidiEvent event : events)
        {
            int deltaTime = event.tick - lastTick;
            // 将 deltaTime 写入为 Variable Length Quantity (变长数量)
            writeVarLength(trackData, deltaTime);
            // 写入事件数据：status, data1, data2
            trackData.write(event.status);
            trackData.write(event.data1);
            trackData.write(event.data2);
            lastTick = event.tick;
        }
        // 写入结束事件：delta time 0, 0xFF 0x2F 0x00 表示该 track 结束
        writeVarLength(trackData, 0);
        trackData.write(0xFF);
        trackData.write(0x2F);
        trackData.write(0x00);
        byte[] trackChunkData = trackData.toByteArray();

        // 构造整个 MIDI 文件
        ByteArrayOutputStream midiFile = new ByteArrayOutputStream();

        // 写入 header chunk: "MThd" + 6 字节 header 数据 (格式类型 0, 1 track, 分辨率)
        midiFile.write("MThd".getBytes("US-ASCII"));
        midiFile.write(new byte[]{0x00, 0x00, 0x00, 0x06});
        // 格式类型 0：两字节 0x00 0x00
        midiFile.write(new byte[]{0x00, 0x00});
        // track 数量：两字节 0x00 0x01
        midiFile.write(new byte[]{0x00, 0x01});
        // 写入 division (分辨率)：RESOLUTION 两字节大端方式
        midiFile.write((RESOLUTION >> 8) & 0xFF);
        midiFile.write(RESOLUTION & 0xFF);

        // 写入 track chunk header "MTrk"
        midiFile.write("MTrk".getBytes("US-ASCII"));
        // 写入 track chunk 的长度（4字节大端整数）
        int trackLength = trackChunkData.length;
        midiFile.write(new byte[]
                {
                        (byte)((trackLength >> 24) & 0xFF),
                        (byte)((trackLength >> 16) & 0xFF),
                        (byte)((trackLength >> 8) & 0xFF),
                        (byte)(trackLength & 0xFF)
                });
        // 写入 track chunk 数据
        midiFile.write(trackChunkData);

        return midiFile.toByteArray();
    }

    /**
     * 将整数写为 MIDI 文件所要求的变长数量（variable-length quantity）格式.
     * @param out 输出流
     * @param value 要编码的整数
     */
    private static void writeVarLength(ByteArrayOutputStream out, int value)
    {
        int buffer = value & 0x7F;
        while ((value >>= 7) > 0)
        {
            buffer <<= 8;
            buffer |= ((value & 0x7F) | 0x80);
        }
        while (true)
        {
            out.write(buffer & 0xFF);
            if ((buffer & 0x80) != 0)
                buffer >>= 8;
            else
                break;
        }
    }

    // 内部类，表示一个 MIDI 事件
    private static class MidiEvent
    {
        int tick;
        int status;
        int data1;
        int data2;

        public MidiEvent(int tick, int status, int data1, int data2)
        {
            this.tick = tick;
            this.status = status;
            this.data1 = data1;
            this.data2 = data2;
        }
    }
}