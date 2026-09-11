package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * FP-17 MIDI 格式导出器（基于 javax.sound.midi）。
 * <p>
 * 将 {@link 乐谱} 写入 .mid 文件（MIDI type 0，单轨道）。
 * 关键映射规则（与 {@link MIDI解析器} 互逆）：
 * <ul>
 *   <li>MC 音高 0 → MIDI 音高 54 (F♯3)；MC 音高 24 → MIDI 音高 78 (F♯5)</li>
 *   <li>内部力度 0.0-1.0 → MIDI velocity 0-127</li>
 *   <li>BPM → MIDI tempo meta：tempo_mpq = 60000000 / BPM</li>
 *   <li>MC tick → MIDI tick：midi_tick = round(mc_tick × PPQ × BPM / 1200)</li>
 *   <li>每个 {@link 音符} 生成 NOTE_ON + NOTE_OFF 事件对</li>
 *   <li>音符 i 的 NOTE_ON 位置 = sum(时值[0..i-1])，NOTE_OFF 位置 = NOTE_ON + 时值[i]</li>
 * </ul>
 * <p>
 * PPQ 选择：使用 480（高分辨率），保证 BPM=120 时 1 MC tick = 48 MIDI ticks，往返为整数。
 * <p>
 * 实现限制：
 * <ul>
 *   <li>导出为 MIDI type 0（单轨道）</li>
 *   <li>所有音符放入 channel 0</li>
 *   <li>不导出乐器程序变更（使用默认钢琴 0）</li>
 *   <li>音高超出 [54, 78] 范围会被 clamp</li>
 * </ul>
 */
public final class MIDI导出器 {

    /** MIDI 音高偏移：MC 音高 0 → MIDI 音高 54 (F♯3)。 */
    private static final int MIDI音高偏移 = 54;
    /** MIDI 力度上限（标准 MIDI：0-127）。 */
    private static final int MIDI力度上限 = 127;
    /** MIDI 序列分辨率（PPQ = 480，高分辨率保证往返整数）。 */
    private static final int MIDI_PPQ = 480;
    /** MC tick 每秒（Bukkit 标准）。 */
    private static final double MC每秒tick = 20.0;
    /** 微秒每分钟。 */
    private static final long 微秒每分钟 = 60_000_000L;
    /** MIDI tempo meta 事件类型（0x51）。 */
    private static final int MIDI_TEMPO_META = 0x51;
    /** MIDI 默认通道（channel 0）。 */
    private static final int MIDI默认通道 = 0;
    /** MIDI 默认力度（用于 NOTE_OFF）。 */
    private static final int MIDI默认释放力度 = 0;

    private MIDI导出器() {
    }

    /**
     * 将乐谱导出为 .mid 文件。
     *
     * @param 乐谱 乐谱对象
     * @param 文件 目标 .mid 文件
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, File 文件) throws IOException {
        if (乐谱 == null) {
            throw new IOException("乐谱为空");
        }
        if (文件 == null) {
            throw new IOException("目标文件为空");
        }
        try (FileOutputStream 输出 = new FileOutputStream(文件)) {
            导出(乐谱, 输出);
        }
    }

    /**
     * 将乐谱导出到输出流。
     *
     * @param 乐谱  乐谱对象
     * @param 输出  输出流
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, OutputStream 输出) throws IOException {
        Sequence 序列;
        try {
            序列 = new Sequence(Sequence.PPQ, MIDI_PPQ);
        } catch (InvalidMidiDataException e) {
            throw new IOException("无法创建 MIDI 序列: " + e.getMessage(), e);
        }
        Track 轨道 = 序列.createTrack();
        写TempoMeta(轨道, 乐谱.获取速度BPM());
        写音符事件(轨道, 乐谱.获取音符列表(), 乐谱.获取速度BPM());
        try {
            MidiSystem.write(序列, 0, 输出);
        } catch (IOException e) {
            throw new IOException("MIDI 写入失败: " + e.getMessage(), e);
        }
    }

    private static void 写TempoMeta(Track 轨道, int bpm) {
        if (bpm <= 0) {
            bpm = 120;
        }
        long tempo = 微秒每分钟 / bpm;
        byte[] 数据 = new byte[3];
        数据[0] = (byte) ((tempo >> 16) & 0xFF);
        数据[1] = (byte) ((tempo >> 8) & 0xFF);
        数据[2] = (byte) (tempo & 0xFF);
        MetaMessage meta;
        try {
            meta = new MetaMessage();
            meta.setMessage(MIDI_TEMPO_META, 数据, 数据.length);
            轨道.add(new MidiEvent(meta, 0));
        } catch (InvalidMidiDataException e) {
            // 静默跳过 tempo meta 写入失败，使用 MIDI 默认 120 BPM
        }
    }

    private static void 写音符事件(Track 轨道, List<音符> 音符列表, int bpm) throws IOException {
        // MC tick → MIDI tick 换算系数：midi_tick = mc_tick × PPQ × BPM / 1200
        double mc转MIDI系数 = (double) MIDI_PPQ * bpm / 1200.0;
        long 累计tick = 0;
        for (音符 当前 : 音符列表) {
            int 时值 = Math.max(1, 当前.获取时值Tick());
            long 开始midiTick = Math.round(累计tick * mc转MIDI系数);
            long 结束midiTick = Math.round((累计tick + 时值) * mc转MIDI系数);
            if (结束midiTick <= 开始midiTick) {
                结束midiTick = 开始midiTick + 1;
            }
            int midi音高 = clamp(当前.获取音高() + MIDI音高偏移, 0, 127);
            int 力度 = clamp((int) Math.round(当前.获取力度() * MIDI力度上限), 0, MIDI力度上限);
            if (力度 <= 0) {
                力度 = 1; // NOTE_ON 力度 0 会被视为 NOTE_OFF，强制至少 1
            }
            添加NoteOn(轨道, 开始midiTick, midi音高, 力度);
            添加NoteOff(轨道, 结束midiTick, midi音高, MIDI默认释放力度);
            累计tick += 时值;
        }
        // 写入 End of Track meta 事件
        try {
            MetaMessage eot = new MetaMessage();
            eot.setMessage(0x2F, new byte[0], 0);
            long 最后tick = 累计tick > 0 ? Math.round(累计tick * mc转MIDI系数) : 0;
            轨道.add(new MidiEvent(eot, 最后tick));
        } catch (InvalidMidiDataException e) {
            // 静默跳过
        }
    }

    private static void 添加NoteOn(Track 轨道, long tick, int 音高, int 力度) throws IOException {
        try {
            ShortMessage 消息 = new ShortMessage();
            消息.setMessage(ShortMessage.NOTE_ON, MIDI默认通道, 音高, 力度);
            轨道.add(new MidiEvent(消息, tick));
        } catch (InvalidMidiDataException e) {
            throw new IOException("NOTE_ON 事件创建失败: " + e.getMessage(), e);
        }
    }

    private static void 添加NoteOff(Track 轨道, long tick, int 音高, int 力度) throws IOException {
        try {
            ShortMessage 消息 = new ShortMessage();
            消息.setMessage(ShortMessage.NOTE_OFF, MIDI默认通道, 音高, 力度);
            轨道.add(new MidiEvent(消息, tick));
        } catch (InvalidMidiDataException e) {
            throw new IOException("NOTE_OFF 事件创建失败: " + e.getMessage(), e);
        }
    }

    private static int clamp(int 值, int 下限, int 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }
}
