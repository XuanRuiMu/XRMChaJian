package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-23 NoteBlockAPI 适配器（softdepend 降级模式）。
 * <p>
 * NoteBlockAPI 是 MC 音乐生态主流插件，提供更高质量的音符音色（18 种含 Trumpet）。
 * 本适配器作为可选依赖接入：当服务器安装并启用 NoteBlockAPI 时，使用其播放引擎；
 * 未安装时降级为返回 false，由调用方回退到原生 playSound。
 * <p>
 * 设计模式：使用懒加载内部类 {@link 桥接} 隔离 NoteBlockAPI 类引用，
 * 确保未安装 NoteBlockAPI 时本类仍可被 JVM 加载（不会触发 NoClassDefFoundError）。
 * 仅当 {@link #是否可用()} 返回 true 后才会触发 {@link 桥接} 的类加载。
 * <p>
 * 关键映射规则（与 {@link NBS导出器} 一致）：
 * <ul>
 *   <li>MC 音高 0 → NBS key 33（F♯3）</li>
 *   <li>内部力度 0.0-1.0 → NBS velocity 0-100</li>
 *   <li>BPM → Song.speed = BPM / 15.0（ticks per second）</li>
 *   <li>所有音符放入单层（Layer 0），tick 位置 = 前序音符时值累加</li>
 * </ul>
 */
public final class NoteBlockAPI适配器 {

    /** NoteBlockAPI 插件名（Bukkit 注册名）。 */
    private static final String 插件名 = "NoteBlockAPI";
    /** NBS key 偏移：MC 音高 0 → NBS key 33。 */
    private static final int NBS键偏移 = 33;
    /** NBS 力度上限。 */
    private static final int NBS力度上限 = 100;
    /** NBS 默认乐器（0 = 钢琴）。 */
    private static final int NBS默认乐器 = 0;
    /** NBS v4 原版乐器数（18 种含 Trumpet，乐器 ID 范围 0-17）。 */
    private static final int NBS原版乐器数 = 18;
    /** NBS 默认声相（100 = 居中）。 */
    private static final int NBS默认声相 = 100;
    /** NBS 默认微调。 */
    private static final int NBS默认微调 = 0;
    /** 默认自定义乐器起始索引（OpenNBS 标准：原版乐器数）。 */
    private static final int 默认自定义乐器起始索引 = 10;
    /** BPM → ticks/sec 转换系数：speed = BPM / 15.0。 */
    private static final double BPM转speed系数 = 1.0 / 15.0;
    /** 单层层名。 */
    private static final String 单层层名 = "Layer 0";

    /** 可用性缓存（null=未检查，true/false=已检查结果）。 */
    private static volatile Boolean 可用缓存;

    private NoteBlockAPI适配器() {
    }

    /**
     * 检查 NoteBlockAPI 是否已安装并启用。
     * 结果会被缓存，插件状态变化后需调用 {@link #刷新可用性()} 重置缓存。
     *
     * @return true=NoteBlockAPI 已启用；false=未安装或未启用
     */
    public static boolean 是否可用() {
        Boolean 缓存 = 可用缓存;
        if (缓存 != null) {
            return 缓存;
        }
        synchronized (NoteBlockAPI适配器.class) {
            if (可用缓存 != null) {
                return 可用缓存;
            }
            Plugin 插件 = Bukkit.getPluginManager().getPlugin(插件名);
            可用缓存 = (插件 != null && 插件.isEnabled());
            return 可用缓存;
        }
    }

    /**
     * 刷新可用性缓存（插件加载/卸载后调用）。
     */
    public static void 刷新可用性() {
        可用缓存 = null;
    }

    /**
     * 使用 NoteBlockAPI 播放乐谱给指定玩家（FP-03 修复：使用默认钢琴乐器ID）。
     * 调用前应先检查 {@link #是否可用()}；不可用时本方法返回 false。
     *
     * @param 玩家 目标玩家
     * @param 乐谱 乐谱对象
     * @return true=已开始播放；false=NoteBlockAPI 不可用或播放失败
     */
    public static boolean 播放乐谱(Player 玩家, 乐谱 乐谱) {
        return 播放乐谱(玩家, 乐谱, NBS默认乐器);
    }

    /**
     * 使用 NoteBlockAPI 播放乐谱给指定玩家（FP-03 修复：传入乐器ID决定音色）。
     * 调用前应先检查 {@link #是否可用()}；不可用时本方法返回 false。
     *
     * @param 玩家    目标玩家
     * @param 乐谱    乐谱对象
     * @param 乐器ID  NoteBlockAPI 乐器 ID（0 ~ NBS原版乐器数-1，见 FP-23.4）；超出范围 clamp 到 [0, NBS原版乐器数-1]
     * @return true=已开始播放；false=NoteBlockAPI 不可用或播放失败
     */
    public static boolean 播放乐谱(Player 玩家, 乐谱 乐谱, int 乐器ID) {
        if (玩家 == null || 乐谱 == null || !是否可用()) {
            return false;
        }
        int clamp乐器ID = Math.max(0, Math.min(NBS原版乐器数 - 1, 乐器ID));
        try {
            return 桥接.播放(玩家, 乐谱, clamp乐器ID);
        } catch (Throwable t) {
            // 任何 NoteBlockAPI 内部异常都视为降级
            return false;
        }
    }

    /**
     * 停止玩家的 NoteBlockAPI 播放。
     * NoteBlockAPI 不可用时本方法无操作。
     *
     * @param 玩家标识 玩家 UUID
     */
    public static void 停止播放(UUID 玩家标识) {
        if (玩家标识 == null || !是否可用()) {
            return;
        }
        try {
            桥接.停止(玩家标识);
        } catch (Throwable t) {
            // 静默忽略
        }
    }

    /**
     * 停止所有玩家的 NoteBlockAPI 播放（插件禁用时清理用）。
     */
    public static void 停止全部播放() {
        if (!是否可用()) {
            return;
        }
        try {
            桥接.停止全部();
        } catch (Throwable t) {
            // 静默忽略
        }
    }

    /**
     * 懒加载桥接类：隔离所有 NoteBlockAPI 类引用。
     * 仅当 {@link #是否可用()} 返回 true 后才会被类加载器加载，
     * 因此未安装 NoteBlockAPI 时不会触发 NoClassDefFoundError。
     */
    private static final class 桥接 {
        /** 玩家 UUID → 活跃 SongPlayer 映射。 */
        private static final java.util.Map<UUID, com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer> 活跃播放器 =
                new ConcurrentHashMap<>();

        static boolean 播放(Player 玩家, 乐谱 乐谱, int 乐器ID) {
            // 先停止该玩家已有的播放
            停止(玩家.getUniqueId());
            // 转换乐谱为 NoteBlockAPI Song（FP-03 修复：传入乐器ID决定音色）
            com.xxmicloxx.NoteBlockAPI.model.Song song = 转换乐谱(乐谱, 乐器ID);
            // 创建 RadioSongPlayer 并播放
            com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer rsp =
                    new com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer(song);
            rsp.addPlayer(玩家);
            rsp.setPlaying(true);
            rsp.setAutoDestroy(true);
            活跃播放器.put(玩家.getUniqueId(), rsp);
            return true;
        }

        static void 停止(UUID 玩家标识) {
            com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer sp = 活跃播放器.remove(玩家标识);
            if (sp != null) {
                try {
                    sp.setPlaying(false);
                    sp.destroy();
                } catch (Throwable t) {
                    // 静默忽略
                }
            }
        }

        static void 停止全部() {
            for (java.util.Map.Entry<UUID, com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer> 条目 :
                    活跃播放器.entrySet()) {
                try {
                    com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer sp = 条目.getValue();
                    sp.setPlaying(false);
                    sp.destroy();
                } catch (Throwable t) {
                    // 静默忽略
                }
            }
            活跃播放器.clear();
        }

        static com.xxmicloxx.NoteBlockAPI.model.Song 转换乐谱(乐谱 乐谱, int 乐器ID) {
            java.util.HashMap<Integer, com.xxmicloxx.NoteBlockAPI.model.Layer> 层表 = new java.util.HashMap<>();
            com.xxmicloxx.NoteBlockAPI.model.Layer 层 = new com.xxmicloxx.NoteBlockAPI.model.Layer();
            层.setName(单层层名);
            层.setVolume((byte) NBS力度上限);
            层.setPanning(NBS默认声相);

            int 累计tick = 0;
            for (音符 当前 : 乐谱.获取音符列表()) {
                int key = NBS键偏移 + 当前.获取音高();
                int velocity = Math.max(0, Math.min(NBS力度上限,
                        (int) Math.round(当前.获取力度() * NBS力度上限)));
                // FP-03 修复：使用传入的乐器ID替代硬编码的 NBS默认乐器（钢琴）
                com.xxmicloxx.NoteBlockAPI.model.Note note =
                        new com.xxmicloxx.NoteBlockAPI.model.Note(
                                (byte) 乐器ID,
                                (byte) Math.min(127, Math.max(0, key)),
                                (byte) velocity,
                                NBS默认声相,
                                (short) NBS默认微调);
                层.setNote(累计tick, note);
                累计tick += Math.max(1, 当前.获取时值Tick());
            }
            层表.put(0, 层);

            float speed = (float) Math.max(0.1, 乐谱.获取速度BPM() * BPM转speed系数);
            short length = (short) Math.min(Short.MAX_VALUE, Math.max(0, 累计tick));
            short songHeight = 1;
            String 标题 = 乐谱.获取名称() == null ? "" : 乐谱.获取名称();
            String 作者 = 乐谱.获取作者名() == null ? "" : 乐谱.获取作者名();

            return new com.xxmicloxx.NoteBlockAPI.model.Song(
                    speed,
                    层表,
                    songHeight,
                    length,
                    标题,
                    作者,
                    "", // original author
                    "", // description
                    null, // path
                    默认自定义乐器起始索引,
                    false // isStereo
            );
        }
    }
}
