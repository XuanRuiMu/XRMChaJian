package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-07 NBS 格式导入器（OpenNBS v3.11.0 标准，v4 兼容读取）。
 * <p>
 * NBS（Note Block Song）是 MC 音乐生态事实标准。本解析器读取 .nbs 文件并转换为 {@link 乐谱}。
 * 仅读取必要字段：长度/层数/标题/作者/描述/BPM/音符（乐器/键位/力度）。
 * 自定义乐器与声相/pitch bend 等扩展字段不读取（保留以兼容格式）。
 * <p>
 * 关键映射规则：
 * <ul>
 *   <li>NBS key 33 = F♯3 = MC 音高 0</li>
 *   <li>MC 音高 = clamp(NBS key - 33, 0, 24)</li>
 *   <li>NBS velocity 0-100 → 内部力度 0.0-1.0（velocity / 100.0）</li>
 *   <li>NBS tempo（ticks per second × 100）→ BPM = round(tempo / 100 × 60 / 4) ≈ tempo / 100 × 15</li>
 *   <li>NBS tick → 乐谱音符时值Tick：1 NBS tick = 1 MC tick（同源）</li>
 *   <li>每个 NBS 音符成为一条 {@link 音符} 记录，时值 = 到下一同层音符的 tick 差，最后音符时值 = 1</li>
 * </ul>
 */
public final class NBS解析器 {

    /** NBS key 偏移：NBS key 33 = F♯3 = MC 音高 0。 */
    private static final int NBS键偏移 = 33;
    /** NBS 力度上限（OpenNBS 标准：0-100）。 */
    private static final int NBS力度上限 = 100;
    /** NBS 默认 tempo（10 ticks per second × 100 = 1000）。 */
    private static final int NBS默认tempo = 1000;
    /** NBS 默认原版乐器数（OpenNBS v4：18 种含 Trumpet）。 */
    private static final int NBS默认原版乐器数 = 18;
    /** MC tick 每秒（Bukkit 标准）。 */
    private static final double MC每秒tick = 20.0;
    /** NBS 每秒 tick → BPM 转换系数。 */
    private static final double NBS每秒tick转BPM = 60.0 / 4.0;

    private NBS解析器() {
    }

    /**
     * 从 .nbs 文件解析为乐谱。
     *
     * @param 文件   .nbs 文件
     * @param 作者标识 玩家 UUID（导入者）
     * @param 作者名 导入者名
     * @param 乐谱名 乐谱名（保存到乐谱对象的名称）
     * @return 乐谱对象
     * @throws IOException 文件读取失败或格式损坏
     */
    public static 乐谱 解析(File 文件, UUID 作者标识, String 作者名, String 乐谱名) throws IOException {
        if (文件 == null) {
            throw new IOException("NBS 文件为空");
        }
        try (FileInputStream 输入 = new FileInputStream(文件)) {
            return 解析(输入, 作者标识, 作者名, 乐谱名);
        }
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
        DataInputStream 数据 = new DataInputStream(输入);
        // 新格式判断：先读 short，若为 0 则为新格式（v3+），否则为旧格式（v0）
        short 首字节 = 读Short(数据);
        int 版本;
        int 原版乐器数;
        if (首字节 == 0) {
            // 新格式：magic byte 已读（应为 0），继续读 version + vanillaInstrumentCount
            // 注意：首字节就是 magic byte（NBS 标准约定首字节 = 0 表示新格式）
            // 但 OpenNBS 实际是：先 short length（=0），再 byte magic（=0），再 byte version
            // 这里首字节为 0 已等同于 length=0；接下来读 version + vanillaInstrumentCount
            // 注意：OpenNBS 协议中 length=0 之后没有独立的 magic byte
            版本 = 数据.readUnsignedByte();
            原版乐器数 = 数据.readUnsignedByte();
        } else {
            // 旧格式（v0）：首字节即为 songLength
            版本 = 0;
            原版乐器数 = 10;
        }
        short 歌曲长度tick = 读Short(数据);
        short 层数 = (版本 >= 3) ? 读Short(数据) : (short) 0;
        String 标题 = 读字符串(数据);
        String 作者 = 读字符串(数据);
        String 原作者 = 读字符串(数据);
        String 描述 = 读字符串(数据);
        short tempo = 读Short(数据);
        // auto-save + auto-save duration
        数据.readByte();
        数据.readByte();
        byte 拍号 = 数据.readByte();
        // v0 不读 minutes spent / left clicks / right clicks / note blocks added / removed / midi sponge file
        if (版本 >= 3) {
            读Int(数据); // minutes spent
            读Int(数据); // left clicks
            读Int(数据); // right clicks
            读Int(数据); // note blocks added
            读Int(数据); // note blocks removed
            读字符串(数据); // midi sponge file
            // v3+ loop 字段
            if (版本 >= 4) {
                数据.readByte(); // loop on finish
                数据.readByte(); // max loop count
                读Short(数据); // loop start tick
            }
        }
        double 每秒tick = (tempo <= 0 ? NBS默认tempo : tempo) / 100.0;
        int 速度BPM = Math.max(1, (int) Math.round(每秒tick * NBS每秒tick转BPM));

        // 解析音符：tick 顺序读
        // 每层 -> 每 tick 的音符列表
        List<List<NBS音符>> 按层音符 = new ArrayList<>();
        int 当前tick = -1;
        while (true) {
            short tick跳跃 = 读Short(数据);
            if (tick跳跃 == 0) {
                break;
            }
            当前tick += tick跳跃;
            int 当前层 = -1;
            while (true) {
                short 层跳跃 = 读Short(数据);
                if (层跳跃 == 0) {
                    break;
                }
                当前层 += 层跳跃;
                while (按层音符.size() <= 当前层) {
                    按层音符.add(new ArrayList<>());
                }
                int 乐器 = 数据.readUnsignedByte();
                int 键 = 数据.readUnsignedByte();
                int 力度 = (版本 >= 4) ? 数据.readUnsignedByte() : 100;
                int 声相 = 100;
                int 微调 = 0;
                if (版本 >= 4) {
                    声相 = 数据.readUnsignedByte();
                    微调 = 读Short(数据);
                }
                int mc音高 = clamp(键 - NBS键偏移, 0, 24);
                double 内部力度 = clamp(力度 / (double) NBS力度上限, 0.0, 1.0);
                按层音符.get(当前层).add(new NBS音符(当前tick, mc音高, 乐器, 内部力度, 声相, 微调));
            }
        }
        // 旧格式（v0）没有层信息；新格式层信息可跳过
        if (版本 >= 1 && 版本 < 3) {
            // 旧版 layers 不可读（OpenNBS 标准 v1/v2 layers 字段省略）
        } else if (版本 >= 3) {
            for (int i = 0; i < 层数; i++) {
                读字符串(数据); // layer name
                if (版本 >= 4) {
                    数据.readByte(); // layer velocity
                    数据.readByte(); // layer panning
                }
            }
        }
        // 自定义乐器（v3+）：跳过
        if (版本 >= 3) {
            int 自定义乐器数 = 数据.readUnsignedByte();
            for (int i = 0; i < 自定义乐器数; i++) {
                读字符串(数据); // name
                读字符串(数据); // sound file
                数据.readByte(); // pitch
                数据.readByte(); // key
            }
        }

        // 将 NBS 音符按 tick 顺序扁平化为 乐谱.音符列表
        // 同一 tick 内多个音符按层序号排序，每个音符的时值 = 下一同层音符的 tick - 当前 tick；末音符时值 = 1
        List<音符> 音符列表 = new ArrayList<>();
        long 创建时间 = System.currentTimeMillis();
        for (List<NBS音符> 层音符 : 按层音符) {
            层音符.sort((a, b) -> Integer.compare(a.tick, b.tick));
            for (int i = 0; i < 层音符.size(); i++) {
                NBS音符 当前 = 层音符.get(i);
                int 时值;
                if (i + 1 < 层音符.size()) {
                    时值 = Math.max(1, 层音符.get(i + 1).tick - 当前.tick);
                } else {
                    时值 = 1;
                }
                音符列表.add(音符.of(当前.mc音高, 时值, 当前.力度));
            }
        }
        String 最终名 = (乐谱名 == null || 乐谱名.isBlank()) ? 标题 : 乐谱名;
        return new 乐谱(最终名, 作者标识, 作者名, 创建时间, 速度BPM, 音符列表);
    }

    private static short 读Short(DataInputStream 数据) throws IOException {
        return (short) (数据.readByte() & 0xFF | (数据.readByte() & 0xFF) << 8);
    }

    private static int 读Int(DataInputStream 数据) throws IOException {
        return (数据.readByte() & 0xFF) | (数据.readByte() & 0xFF) << 8
                | (数据.readByte() & 0xFF) << 16 | (数据.readByte() & 0xFF) << 24;
    }

    private static String 读字符串(DataInputStream 数据) throws IOException {
        int 长度 = 读Int(数据);
        if (长度 <= 0) {
            return "";
        }
        byte[] 字节 = new byte[长度];
        数据.readFully(字节);
        return new String(字节, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int clamp(int 值, int 下限, int 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    private static double clamp(double 值, double 下限, double 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    /** NBS 内部音符记录。 */
    private static final class NBS音符 {
        final int tick;
        final int mc音高;
        final int 乐器;
        final double 力度;
        final int 声相;
        final int 微调;

        NBS音符(int tick, int mc音高, int 乐器, double 力度, int 声相, int 微调) {
            this.tick = tick;
            this.mc音高 = mc音高;
            this.乐器 = 乐器;
            this.力度 = 力度;
            this.声相 = 声相;
            this.微调 = 微调;
        }
    }
}
