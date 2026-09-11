package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-07 NBS 格式导出器（OpenNBS v4 写入）。
 * <p>
 * 将 {@link 乐谱} 写入 .nbs 文件，使用 OpenNBS v4 格式。
 * 关键映射规则（与 {@link NBS解析器} 互逆）：
 * <ul>
 *   <li>MC 音高 0 → NBS key 33（F♯3）</li>
 *   <li>BPM → tempo = round(BPM × 100 / 15)（ticks/sec × 100）</li>
 *   <li>内部力度 0.0-1.0 → NBS velocity 0-100</li>
 *   <li>所有音符放入单层（layer 0），按累计 tick 顺序排列</li>
 *   <li>每个音符的 NBS tick 位置 = 前序音符 时值Tick 累加</li>
 * </ul>
 * <p>
 * 单层策略说明：{@link 乐谱} 不保留层信息（解析时已扁平化），导出时统一放到 layer 0。
 * 这意味着 NBS → 乐谱 → NBS 往返会丢失多层级结构，但音高/时值/力度/BPM 完全保留。
 */
public final class NBS导出器 {

    /** NBS key 偏移：MC 音高 0 → NBS key 33。 */
    private static final int NBS键偏移 = 33;
    /** NBS 力度上限（OpenNBS 标准：0-100）。 */
    private static final int NBS力度上限 = 100;
    /** NBS 写入版本（OpenNBS v4，含 velocity/panning/pitch 字段）。 */
    private static final int NBS版本 = 4;
    /** NBS v4 原版乐器数（18 种含 Trumpet）。 */
    private static final int NBS原版乐器数 = 18;
    /** NBS 默认乐器（0 = 钢琴）。 */
    private static final int NBS默认乐器 = 0;
    /** NBS 默认声相（100 = 居中）。 */
    private static final int NBS默认声相 = 100;
    /** NBS 默认微调（0 = 不偏移）。 */
    private static final int NBS默认微调 = 0;
    /** BPM → NBS tempo 转换系数：tempo = BPM × 100 / 15。 */
    private static final double BPM转tempo系数 = 100.0 / 15.0;
    /** 单层导出时使用的层名。 */
    private static final String 单层层名 = "Layer 0";

    private NBS导出器() {
    }

    /**
     * 将乐谱导出为 .nbs 文件（使用默认钢琴乐器）。
     *
     * @param 乐谱 乐谱对象
     * @param 文件 目标 .nbs 文件
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, File 文件) throws IOException {
        导出(乐谱, 文件, NBS默认乐器);
    }

    /**
     * 将乐谱导出为 .nbs 文件（指定 NoteBlockAPI 乐器 ID）。
     *
     * @param 乐谱   乐谱对象
     * @param 文件   目标 .nbs 文件
     * @param 乐器ID NoteBlockAPI 乐器 ID（0-17）
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, File 文件, int 乐器ID) throws IOException {
        if (乐谱 == null) {
            throw new IOException("乐谱为空");
        }
        if (文件 == null) {
            throw new IOException("目标文件为空");
        }
        try (FileOutputStream 输出 = new FileOutputStream(文件)) {
            导出(乐谱, 输出, 乐器ID);
        }
    }

    /**
     * 将乐谱导出到输出流（使用默认钢琴乐器）。
     *
     * @param 乐谱  乐谱对象
     * @param 输出  输出流
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, OutputStream 输出) throws IOException {
        导出(乐谱, 输出, NBS默认乐器);
    }

    /**
     * 将乐谱导出到输出流（指定 NoteBlockAPI 乐器 ID）。
     * <p>
     * FP-03 修复：接受乐器 ID 参数，替代硬编码的 NBS默认乐器（钢琴）。
     * 导出的 NBS 文件中所有音符的 instrument 字段使用传入的乐器 ID。
     *
     * @param 乐谱   乐谱对象
     * @param 输出   输出流
     * @param 乐器ID NoteBlockAPI 乐器 ID（0 ~ NBS原版乐器数-1）
     * @throws IOException 写入失败
     */
    public static void 导出(乐谱 乐谱, OutputStream 输出, int 乐器ID) throws IOException {
        int clamp乐器ID = Math.max(0, Math.min(NBS原版乐器数 - 1, 乐器ID));
        DataOutputStream 数据 = new DataOutputStream(输出);
        写Header(数据, 乐谱);
        写音符块(数据, 乐谱, clamp乐器ID);
        写层信息(数据);
        写自定义乐器(数据);
    }

    private static void 写Header(DataOutputStream 数据, 乐谱 乐谱) throws IOException {
        写Short(数据, 0); // 新格式 magic：length = 0
        数据.writeByte(NBS版本);
        数据.writeByte(NBS原版乐器数);
        写Short(数据, Math.min(Short.MAX_VALUE, 计算总tick(乐谱)));
        写Short(数据, 1); // 单层导出
        写字符串(数据, 空安全(乐谱.获取名称()));
        写字符串(数据, 空安全(乐谱.获取作者名()));
        写字符串(数据, ""); // original author
        写字符串(数据, ""); // description
        int tempo = Math.max(1, (int) Math.round(乐谱.获取速度BPM() * BPM转tempo系数));
        写Short(数据, Math.min(Short.MAX_VALUE, tempo));
        数据.writeByte(0); // auto-save off
        数据.writeByte(10); // auto-save duration
        数据.writeByte(4); // time signature 4/4
        写Int(数据, 0); // minutes spent
        写Int(数据, 0); // left clicks
        写Int(数据, 0); // right clicks
        写Int(数据, 0); // note blocks added
        写Int(数据, 0); // note blocks removed
        写字符串(数据, ""); // midi sponge file
        // v4 loop 字段
        数据.writeByte(0); // loop on finish off
        数据.writeByte(0); // max loop count
        写Short(数据, 0); // loop start tick
    }

    private static void 写音符块(DataOutputStream 数据, 乐谱 乐谱, int 乐器ID) throws IOException {
        List<音符> 音符列表 = 乐谱.获取音符列表();
        if (音符列表.isEmpty()) {
            写Short(数据, 0); // 无音符，直接结束
            return;
        }
        // 计算每个音符的绝对 tick 位置（累计时值）
        List<Integer> tick位置 = new ArrayList<>(音符列表.size());
        int 累计tick = 0;
        for (音符 当前 : 音符列表) {
            tick位置.add(累计tick);
            累计tick += Math.max(1, 当前.获取时值Tick());
        }
        int 上一tick = -1;
        for (int i = 0; i < 音符列表.size(); i++) {
            int 当前tick = tick位置.get(i);
            int tick跳跃 = 当前tick - 上一tick;
            写Short(数据, tick跳跃);
            // 单层导出：层跳跃 = 1（从 -1 跳到 0）
            写Short(数据, 1);
            音符 当前 = 音符列表.get(i);
            // FP-03 修复：使用传入的乐器ID替代硬编码的 NBS默认乐器（钢琴）
            数据.writeByte(乐器ID);
            int key = NBS键偏移 + 当前.获取音高();
            数据.writeByte(Math.min(255, Math.max(0, key)));
            int velocity = Math.max(0, Math.min(NBS力度上限,
                    (int) Math.round(当前.获取力度() * NBS力度上限)));
            数据.writeByte(velocity);
            数据.writeByte(NBS默认声相);
            写Short(数据, NBS默认微调);
            // 当前 tick 层结束
            写Short(数据, 0);
            上一tick = 当前tick;
        }
        // 全部音符结束
        写Short(数据, 0);
    }

    private static void 写层信息(DataOutputStream 数据) throws IOException {
        // 单层导出：写一层信息
        写字符串(数据, 单层层名);
        数据.writeByte(0); // layer velocity
        数据.writeByte(NBS默认声相); // layer panning
    }

    private static void 写自定义乐器(DataOutputStream 数据) throws IOException {
        数据.writeByte(0); // 无自定义乐器
    }

    private static int 计算总tick(乐谱 乐谱) {
        int 总tick = 0;
        for (音符 音符 : 乐谱.获取音符列表()) {
            总tick += Math.max(1, 音符.获取时值Tick());
        }
        return 总tick;
    }

    private static String 空安全(String 字符串) {
        return 字符串 == null ? "" : 字符串;
    }

    private static void 写Short(DataOutputStream 数据, int 值) throws IOException {
        数据.writeByte(值 & 0xFF);
        数据.writeByte((值 >> 8) & 0xFF);
    }

    private static void 写Int(DataOutputStream 数据, int 值) throws IOException {
        数据.writeByte(值 & 0xFF);
        数据.writeByte((值 >> 8) & 0xFF);
        数据.writeByte((值 >> 16) & 0xFF);
        数据.writeByte((值 >> 24) & 0xFF);
    }

    private static void 写字符串(DataOutputStream 数据, String 字符串) throws IOException {
        byte[] 字节 = 字符串.getBytes(StandardCharsets.UTF_8);
        写Int(数据, 字节.length);
        数据.write(字节);
    }
}
