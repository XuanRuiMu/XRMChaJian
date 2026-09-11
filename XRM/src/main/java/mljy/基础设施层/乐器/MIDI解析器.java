package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FP-17 MIDI 格式导入器（基于 javax.sound.midi）。
 * <p>
 * MIDI（.mid）是数字音乐通用标准。本解析器使用 JDK 内置 javax.sound.midi 解析 MIDI 文件，
 * 提取 NOTE_ON/NOTE_OFF 事件转换为 {@link 乐谱}。
 * <p>
 * 关键映射规则：
 * <ul>
 *   <li>MIDI 音高 54 (F♯3) → MC 音高 0；MIDI 78 (F♯5) → MC 音高 24</li>
 *   <li>MC 音高 = clamp(midi_note - 54, 0, 24)</li>
 *   <li>MIDI velocity 0-127 → 内部力度 0.0-1.0（velocity / 127.0）</li>
 *   <li>MIDI tick → MC tick：MC tick = round(midi_tick × 1200 / (PPQ × BPM))</li>
 *   <li>BPM 从 MIDI tempo meta 事件读取：BPM = 60000000 / tempo_mpq</li>
 *   <li>每个 NOTE_ON/NOTE_OFF 对构成一个 {@link 音符}，时值 = NOTE_OFF tick - NOTE_ON tick（至少 1）</li>
 *   <li>NOTE_ON velocity=0 视为 NOTE_OFF（MIDI 惯例）</li>
 * </ul>
 * <p>
 * 实现限制：
 * <ul>
 *   <li>仅处理 NOTE_ON/NOTE_OFF 事件，忽略控制变更/弯音/程序变更等</li>
 *   <li>多轨道合并为单层音符列表</li>
 *   <li>无 tempo meta 时默认 120 BPM</li>
 *   <li>音高超出 [54, 78] 范围的 MIDI 音符会被 clamp 到 [0, 24]</li>
 * </ul>
 */
public final class MIDI解析器 {

    /** MIDI 音高偏移：MIDI 54 (F♯3) → MC 音高 0。 */
    private static final int MIDI音高偏移 = 54;
    /** MIDI 力度上限（标准 MIDI：0-127）。 */
    private static final int MIDI力度上限 = 127;
    /** MIDI 默认 BPM（无 tempo meta 时使用）。 */
    private static final int MIDI默认BPM = 120;
    /** MIDI tempo meta 事件类型（0x51）。 */
    private static final int MIDI_TEMPO_META = 0x51;
    /** MC tick 每秒（Bukkit 标准）。 */
    private static final double MC每秒tick = 20.0;
    /** 微秒每分钟。 */
    private static final long 微秒每分钟 = 60_000_000L;

    private MIDI解析器() {
    }

    /**
     * 从 .mid 文件解析为乐谱。
     *
     * @param 文件   .mid 文件
     * @param 作者标识 玩家 UUID（导入者）
     * @param 作者名 导入者名
     * @param 乐谱名 乐谱名（保存到乐谱对象的名称）
     * @return 乐谱对象
     * @throws IOException 文件读取失败或格式损坏
     */
    public static 乐谱 解析(File 文件, UUID 作者标识, String 作者名, String 乐谱名) throws IOException {
        if (文件 == null) {
            throw new IOException("MIDI 文件为空");
        }
        Sequence 序列;
        try {
            序列 = MidiSystem.getSequence(文件);
        } catch (InvalidMidiDataException e) {
            throw new IOException("MIDI 数据格式无效: " + e.getMessage(), e);
        }
        return 解析序列(序列, 作者标识, 作者名, 乐谱名);
    }

    /**
     * 从输入流解析为乐谱。
     *
     * @param 输入   输入流
     * @param 作者标识 玩家 UUID
     * @param 作者名 导入者名
     * @param 乐谱名 乐谱名
     * @return 乐谱对象
     * @throws IOException 读取失败
     */
    public static 乐谱 解析(InputStream 输入, UUID 作者标识, String 作者名, String 乐谱名) throws IOException {
        if (输入 == null) {
            throw new IOException("输入流为空");
        }
        Sequence 序列;
        try {
            序列 = MidiSystem.getSequence(输入);
        } catch (InvalidMidiDataException e) {
            throw new IOException("MIDI 数据格式无效: " + e.getMessage(), e);
        }
        return 解析序列(序列, 作者标识, 作者名, 乐谱名);
    }

    private static 乐谱 解析序列(Sequence 序列, UUID 作者标识, String 作者名, String 乐谱名) throws IOException {
        if (序列 == null) {
            throw new IOException("MIDI 序列为空");
        }
        int ppq = 序列.getResolution();
        if (ppq <= 0) {
            ppq = 4;
        }
        // 第一遍：解析 tempo meta 事件获取 BPM
        int bpm = 提取BPM(序列);
        // MIDI tick → MC tick 换算系数：MC tick = midi_tick × 1200 / (ppq × bpm)
        double midi转MC系数 = 1200.0 / (ppq * bpm);

        // 第二遍：解析 NOTE_ON/NOTE_OFF 事件
        // key = (note, channel)，value = (start_tick, velocity)
        Map<Long, PendingNote> 待匹配 = new HashMap<>();
        List<MidiNoteEvent> 已完成 = new ArrayList<>();
        for (Track 轨道 : 序列.getTracks()) {
            for (int i = 0; i < 轨道.size(); i++) {
                MidiEvent 事件 = 轨道.get(i);
                long midiTick = 事件.getTick();
                MidiMessage 消息 = 事件.getMessage();
                if (消息 instanceof ShortMessage 短消息) {
                    int 命令 = 短消息.getCommand();
                    int 音高 = 短消息.getData1();
                    int 力度 = 短消息.getData2();
                    int 通道 = 短消息.getChannel();
                    long 键 = 键(音高, 通道);
                    if (命令 == ShortMessage.NOTE_ON && 力度 > 0) {
                        // NOTE_ON
                        long mcTick = Math.round(midiTick * midi转MC系数);
                        待匹配.put(键, new PendingNote(mcTick, 力度));
                    } else if (命令 == ShortMessage.NOTE_OFF
                            || (命令 == ShortMessage.NOTE_ON && 力度 == 0)) {
                        // NOTE_OFF 或 NOTE_ON velocity=0
                        PendingNote 开始 = 待匹配.remove(键);
                        if (开始 != null) {
                            long mcTickEnd = Math.round(midiTick * midi转MC系数);
                            long 时值 = Math.max(1, mcTickEnd - 开始.tick);
                            已完成.add(new MidiNoteEvent(开始.tick, 音高, 开始.velocity, (int) 时值));
                        }
                    }
                }
            }
        }
        // 未匹配的 NOTE_ON 视为零时值音符（1 tick）
        for (PendingNote 未结束 : 待匹配.values()) {
            已完成.add(new MidiNoteEvent(未结束.tick, 0, 未结束.velocity, 1));
        }

        // 按 tick 排序
        已完成.sort((a, b) -> Long.compare(a.tick, b.tick));

        // 转换为音符列表，计算时值（下一音符 tick - 当前 tick；末音符 = 1）
        List<音符> 音符列表 = new ArrayList<>();
        for (int i = 0; i < 已完成.size(); i++) {
            MidiNoteEvent 当前 = 已完成.get(i);
            int 时值;
            if (i + 1 < 已完成.size()) {
                时值 = Math.max(1, (int) (已完成.get(i + 1).tick - 当前.tick));
            } else {
                时值 = Math.max(1, 当前.duration);
            }
            int mc音高 = clamp(当前.note - MIDI音高偏移, 0, 24);
            double 内部力度 = clamp(当前.velocity / (double) MIDI力度上限, 0.0, 1.0);
            音符列表.add(音符.of(mc音高, 时值, 内部力度));
        }
        long 创建时间 = System.currentTimeMillis();
        String 最终名 = (乐谱名 == null || 乐谱名.isBlank()) ? "导入MIDI" : 乐谱名;
        return new 乐谱(最终名, 作者标识, 作者名, 创建时间, bpm, 音符列表);
    }

    private static int 提取BPM(Sequence 序列) {
        // 遍历所有轨道找第一个 tempo meta 事件
        for (Track 轨道 : 序列.getTracks()) {
            for (int i = 0; i < 轨道.size(); i++) {
                MidiEvent 事件 = 轨道.get(i);
                MidiMessage 消息 = 事件.getMessage();
                if (消息 instanceof MetaMessage meta) {
                    if (meta.getType() == MIDI_TEMPO_META) {
                        byte[] 数据 = meta.getData();
                        if (数据.length >= 3) {
                            int tempo = ((数据[0] & 0xFF) << 16)
                                    | ((数据[1] & 0xFF) << 8)
                                    | (数据[2] & 0xFF);
                            if (tempo > 0) {
                                return Math.max(1, (int) (微秒每分钟 / tempo));
                            }
                        }
                    }
                }
            }
        }
        return MIDI默认BPM;
    }

    private static long 键(int 音高, int 通道) {
        return ((long) 通道 << 8) | 音高;
    }

    private static int clamp(int 值, int 下限, int 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    private static double clamp(double 值, double 下限, double 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    /** 等待 NOTE_OFF 的音符。 */
    private static final class PendingNote {
        final long tick;
        final int velocity;

        PendingNote(long tick, int velocity) {
            this.tick = tick;
            this.velocity = velocity;
        }
    }

    /** 已完成的 MIDI 音符事件。 */
    private static final class MidiNoteEvent {
        final long tick;
        final int note;
        final int velocity;
        final int duration;

        MidiNoteEvent(long tick, int note, int velocity, int duration) {
            this.tick = tick;
            this.note = note;
            this.velocity = velocity;
            this.duration = duration;
        }
    }
}
