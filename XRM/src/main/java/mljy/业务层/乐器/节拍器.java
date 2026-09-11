package mljy.业务层.乐器;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-08 节拍器。
 * 作为演奏、录音、合奏的统一节奏时钟源。
 * 按 BPM 定时向玩家播放节拍音，强拍（每小节第一拍）与弱拍区分。
 * tick 间隔计算：每拍 tick = 1200 / BPM（1秒=20tick，每拍秒数=60/BPM，每拍tick=60/BPM*20=1200/BPM）。
 * 作为合奏同步时钟源：暴露 {@link #获取起拍时刻毫秒()} 供合奏系统对齐（FP-15）。
 *
 * FP-C 扩展：
 * - count-in 预备拍（FP-08.3 / O1）：0/1/2 小节可配，默认 1。
 * - {@link #启动带CountIn}：玩家个人演奏/录制前 count-in，结束后启动主节拍器并回调。
 * - {@link #启动合奏CountIn}：合奏 T0 同步起拍（FP-15），count-in 期间对所有成员广播节拍声与标题字幕，
 *   count-in 结束时刻即 T0，回调中各成员同时开始播放。
 */
@Singleton
public class 节拍器 {

    private static final int 每秒tick = 20;
    private static final int BPM下限 = 20;
    private static final int BPM上限 = 400;
    private static final int 拍号下限 = 1;
    private static final int 拍号上限 = 16;
    private static final float 音高下限 = 0.5f;
    private static final float 音高上限 = 2.0f;
    private static final int countIn小节数下限 = 0;
    private static final int countIn小节数上限 = 2;
    private static final int 标题淡入tick = 2;
    private static final int 标题停留tick = 10;
    private static final int 标题淡出tick = 2;
    // FP-10 播放中拍号显示的标题停留 tick（短停留避免遮挡玩家视野）
    private static final int 拍号标题停留tick = 6;

    private final JavaPlugin 插件;

    private volatile int 默认BPM;
    private volatile int 默认拍号分子;
    private volatile int 默认拍号分母;
    private volatile Sound 强拍音色;
    private volatile Sound 弱拍音色;
    private volatile float 强拍音高;
    private volatile float 弱拍音高;
    private volatile float 强拍音量;
    private volatile float 弱拍音量;
    private volatile int countIn小节数;
    // FP-10 播放中拍号显示开关（false=仅 count-in 显示，true=播放中也显示当前拍号位置）
    private volatile boolean 拍号显示启用;

    private final ConcurrentHashMap<UUID, 节拍会话> 会话表 = new ConcurrentHashMap<>();

    /**
     * Guice 注入构造器。仅初始化插件引用，参数从 {@link #从配置加载} 加载。
     *
     * @param 插件 插件实例
     */
    @Inject
    public 节拍器(JavaPlugin 插件) {
        this.插件 = 插件;
        应用默认参数();
    }

    /**
     * 测试与外部构造器：从 {@code 乐器配置.yml} 的 {@code 节拍器} 段加载参数。
     *
     * @param 插件 插件实例
     * @param 配置 已加载的乐器配置（{@code 乐器配置.yml}）
     */
    public 节拍器(JavaPlugin 插件, FileConfiguration 配置) {
        this.插件 = 插件;
        从配置加载(配置);
    }

    private void 应用默认参数() {
        this.默认BPM = 120;
        this.默认拍号分子 = 4;
        this.默认拍号分母 = 4;
        this.强拍音色 = Sound.BLOCK_NOTE_BLOCK_HAT;
        this.弱拍音色 = Sound.BLOCK_NOTE_BLOCK_HAT;
        this.强拍音高 = 1.0f;
        this.弱拍音高 = 0.5f;
        this.强拍音量 = 1.0f;
        this.弱拍音量 = 0.6f;
        this.countIn小节数 = 1;
        this.拍号显示启用 = false;
    }

    /**
     * 从配置加载节拍器参数。支持热重载（调用后新会话使用新参数，运行中会话不变）。
     *
     * @param 配置 已加载的乐器配置
     */
    public final void 从配置加载(FileConfiguration 配置) {
        if (配置 == null) {
            应用默认参数();
            return;
        }
        String 根 = "节拍器";
        this.默认BPM = clamp(配置.getInt(根 + ".默认BPM", 120), BPM下限, BPM上限);
        this.默认拍号分子 = clamp(配置.getInt(根 + ".默认拍号分子", 4), 拍号下限, 拍号上限);
        this.默认拍号分母 = clamp(配置.getInt(根 + ".默认拍号分母", 4), 拍号下限, 拍号上限);
        this.强拍音色 = 解析音色(配置.getString(根 + ".强拍音色", "BLOCK_NOTE_BLOCK_HAT"));
        this.弱拍音色 = 解析音色(配置.getString(根 + ".弱拍音色", "BLOCK_NOTE_BLOCK_HAT"));
        this.强拍音高 = clamp((float) 配置.getDouble(根 + ".强拍音高", 1.0), 音高下限, 音高上限);
        this.弱拍音高 = clamp((float) 配置.getDouble(根 + ".弱拍音高", 0.5), 音高下限, 音高上限);
        this.强拍音量 = clamp((float) 配置.getDouble(根 + ".强拍音量", 1.0), 0.0f, 1.0f);
        this.弱拍音量 = clamp((float) 配置.getDouble(根 + ".弱拍音量", 0.6), 0.0f, 1.0f);
        this.countIn小节数 = clamp(配置.getInt(根 + ".count-in小节数", 1), countIn小节数下限, countIn小节数上限);
        this.拍号显示启用 = 配置.getBoolean(根 + ".拍号显示", false);
    }

    /**
     * 为玩家启动节拍器，使用默认 BPM 与拍号。
     *
     * @param 玩家 目标玩家
     */
    public void 启动(Player 玩家) {
        启动(玩家, 默认BPM, 默认拍号分子, 默认拍号分母);
    }

    /**
     * 为玩家启动节拍器，使用指定 BPM 与拍号。
     *
     * @param 玩家 目标玩家
     * @param bpm 每分钟拍数，必须落在 [{@value BPM下限}, {@value BPM上限}]
     * @param 拍号分子 每小节拍数，必须 >= {@value 拍号下限}
     * @param 拍号分母 以何种音符为一拍，必须 >= {@value 拍号下限}
     * @throws IllegalArgumentException BPM 或拍号越界
     */
    // Purpur 26.2 的 Player 仅提供已过时的定时 sendTitle（无 adventure Title 重载），保留使用并抑制告警。
    @SuppressWarnings({"deprecation", "removal"})
    public void 启动(Player 玩家, int bpm, int 拍号分子, int 拍号分母) {
        校验参数(bpm, 拍号分子, 拍号分母);
        UUID 玩家标识 = 玩家.getUniqueId();
        停止(玩家标识);

        long 起拍时刻 = System.currentTimeMillis();
        节拍会话 会话 = new 节拍会话(bpm, 拍号分子, 拍号分母, 起拍时刻);
        会话表.put(玩家标识, 会话);

        long 每拍tick = bpm转tick(bpm);
        会话.任务 = new BukkitRunnable() {
            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    停止(玩家标识);
                    return;
                }
                boolean 强拍 = 会话.当前拍 % 拍号分子 == 0;
                Sound 音色 = 强拍 ? 强拍音色 : 弱拍音色;
                float 音高 = 强拍 ? 强拍音高 : 弱拍音高;
                float 音量 = 强拍 ? 强拍音量 : 弱拍音量;
                玩家.playSound(玩家.getLocation(), 音色, 音量, 音高);
                if (拍号显示启用) {
                    int 当前小节内拍 = 会话.当前拍 % 拍号分子 + 1;
                玩家.sendTitle(当前小节内拍 + "/" + 拍号分子, "", 标题淡入tick, 拍号标题停留tick, 标题淡出tick);
                }
                会话.当前拍++;
            }
        };
        会话.任务.runTaskTimer(插件, 0L, 每拍tick);
    }

    /**
     * FP-08.3 / FP-C：启动带 count-in 的节拍器。
     * count-in 期间按 BPM 发出节拍声 + 标题字幕（剩余拍数），count-in 结束后启动主节拍器并触发回调。
     * count-in 小节数为 0 时直接启动主节拍器并立即回调。
     *
     * @param 玩家 目标玩家
     * @param bpm 每分钟拍数
     * @param 拍号分子 每小节拍数
     * @param 拍号分母 以何种音符为一拍
     * @param 完成回调 count-in 结束且主节拍器启动后回调（可为 null）
     */
    @SuppressWarnings({"deprecation", "removal"})
    public void 启动带CountIn(Player 玩家, int bpm, int 拍号分子, int 拍号分母, Runnable 完成回调) {
        校验参数(bpm, 拍号分子, 拍号分母);
        UUID 玩家标识 = 玩家.getUniqueId();
        停止(玩家标识);

        if (countIn小节数 <= 0) {
            启动(玩家, bpm, 拍号分子, 拍号分母);
            if (完成回调 != null) {
                完成回调.run();
            }
            return;
        }

        long 起拍时刻 = System.currentTimeMillis();
        节拍会话 会话 = new 节拍会话(bpm, 拍号分子, 拍号分母, 起拍时刻);
        会话表.put(玩家标识, 会话);

        long 每拍tick = bpm转tick(bpm);
        int 总拍数 = countIn小节数 * 拍号分子;
        会话.任务 = new BukkitRunnable() {
            int 已执行拍 = 0;

            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    停止(玩家标识);
                    return;
                }
                if (已执行拍 >= 总拍数) {
                    停止(玩家标识);
                    启动(玩家, bpm, 拍号分子, 拍号分母);
                    if (完成回调 != null) {
                        完成回调.run();
                    }
                    return;
                }
                boolean 强拍 = 已执行拍 % 拍号分子 == 0;
                Sound 音色 = 强拍 ? 强拍音色 : 弱拍音色;
                float 音高 = 强拍 ? 强拍音高 : 弱拍音高;
                float 音量 = 强拍 ? 强拍音量 : 弱拍音量;
                玩家.playSound(玩家.getLocation(), 音色, 音量, 音高);
                int 剩余 = 总拍数 - 已执行拍;
                玩家.sendTitle(String.valueOf(剩余), "", 标题淡入tick, 标题停留tick, 标题淡出tick);
                已执行拍++;
            }
        };
        会话.任务.runTaskTimer(插件, 0L, 每拍tick);
    }

    /**
     * FP-15：启动合奏 count-in（T0 同步起拍）。
     * count-in 期间对所有成员广播节拍声 + 标题字幕（剩余拍数），count-in 结束时刻即 T0，触发回调。
     * 回调中各成员应同时开始播放各自乐谱，消除独立定时器漂移（修复 GAP-04）。
     * 注意：此方法不启动任何成员的个人主节拍器，仅做 count-in 同步。
     *
     * @param 成员集合 合奏所有成员
     * @param bpm 乐谱 BPM
     * @param 拍号分子 拍号分子
     * @param 拍号分母 拍号分母
     * @param 完成回调 count-in 结束回调（T0 时刻，在主线程执行）
     */
    @SuppressWarnings({"deprecation", "removal"})
    public void 启动合奏CountIn(Set<UUID> 成员集合, int bpm, int 拍号分子, int 拍号分母, Runnable 完成回调) {
        校验参数(bpm, 拍号分子, 拍号分母);
        if (成员集合 == null || 成员集合.isEmpty()) {
            if (完成回调 != null) {
                完成回调.run();
            }
            return;
        }

        UUID 会话标识 = 成员集合.iterator().next();
        停止(会话标识);

        if (countIn小节数 <= 0) {
            if (完成回调 != null) {
                完成回调.run();
            }
            return;
        }

        long 起拍时刻 = System.currentTimeMillis();
        节拍会话 会话 = new 节拍会话(bpm, 拍号分子, 拍号分母, 起拍时刻);
        会话表.put(会话标识, 会话);

        long 每拍tick = bpm转tick(bpm);
        int 总拍数 = countIn小节数 * 拍号分子;
        会话.任务 = new BukkitRunnable() {
            int 已执行拍 = 0;

            @Override
            public void run() {
                if (已执行拍 >= 总拍数) {
                    停止(会话标识);
                    if (完成回调 != null) {
                        完成回调.run();
                    }
                    return;
                }
                boolean 强拍 = 已执行拍 % 拍号分子 == 0;
                Sound 音色 = 强拍 ? 强拍音色 : 弱拍音色;
                float 音高 = 强拍 ? 强拍音高 : 弱拍音高;
                float 音量 = 强拍 ? 强拍音量 : 弱拍音量;
                int 剩余 = 总拍数 - 已执行拍;
                String 显示文本 = String.valueOf(剩余);
                for (UUID 成员 : 成员集合) {
                    Player 玩家 = Bukkit.getPlayer(成员);
                    if (玩家 == null || !玩家.isOnline()) {
                        continue;
                    }
                    玩家.playSound(玩家.getLocation(), 音色, 音量, 音高);
                    玩家.sendTitle(显示文本, "", 标题淡入tick, 标题停留tick, 标题淡出tick);
                }
                已执行拍++;
            }
        };
        会话.任务.runTaskTimer(插件, 0L, 每拍tick);
    }

    /**
     * 停止玩家的节拍器。
     *
     * @param 玩家标识 玩家唯一标识
     */
    public void 停止(UUID 玩家标识) {
        节拍会话 会话 = 会话表.remove(玩家标识);
        if (会话 != null && 会话.任务 != null) {
            try {
                会话.任务.cancel();
            } catch (IllegalStateException 忽略已取消) {
                // 任务已自行取消，忽略
            }
        }
    }

    /**
     * 判断玩家节拍器是否正在运行。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否运行中
     */
    public boolean 是否运行(UUID 玩家标识) {
        return 会话表.containsKey(玩家标识);
    }

    /**
     * 获取玩家节拍器的起拍时刻（毫秒时间戳）。作为合奏同步基准（FP-15）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 起拍时刻毫秒；未运行返回 -1
     */
    public long 获取起拍时刻毫秒(UUID 玩家标识) {
        节拍会话 会话 = 会话表.get(玩家标识);
        return 会话 == null ? -1L : 会话.起拍时刻;
    }

    /**
     * 获取玩家节拍器 BPM。
     *
     * @param 玩家标识 玩家唯一标识
     * @return BPM；未运行返回 -1
     */
    public int 获取BPM(UUID 玩家标识) {
        节拍会话 会话 = 会话表.get(玩家标识);
        return 会话 == null ? -1 : 会话.bpm;
    }

    /**
     * 获取玩家节拍器拍号分子。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 拍号分子；未运行返回 -1
     */
    public int 获取拍号分子(UUID 玩家标识) {
        节拍会话 会话 = 会话表.get(玩家标识);
        return 会话 == null ? -1 : 会话.拍号分子;
    }

    /**
     * 获取玩家节拍器拍号分母。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 拍号分母；未运行返回 -1
     */
    public int 获取拍号分母(UUID 玩家标识) {
        节拍会话 会话 = 会话表.get(玩家标识);
        return 会话 == null ? -1 : 会话.拍号分母;
    }

    public int 获取默认BPM() {
        return 默认BPM;
    }

    public int 获取默认拍号分子() {
        return 默认拍号分子;
    }

    public int 获取默认拍号分母() {
        return 默认拍号分母;
    }

    /**
     * 获取 count-in 小节数（FP-08.3 / O1）。
     *
     * @return count-in 小节数（0/1/2）
     */
    public int 获取CountIn小节数() {
        return countIn小节数;
    }

    /**
     * 设置 count-in 小节数（运行时覆盖配置，热重载会重置为配置值）。
     *
     * @param 小节数 0/1/2
     */
    public void 设置CountIn小节数(int 小节数) {
        this.countIn小节数 = clamp(小节数, countIn小节数下限, countIn小节数上限);
    }

    /**
     * FP-10 播放中拍号显示是否启用。
     *
     * @return true=播放中持续显示当前拍号位置；false=仅 count-in 显示
     */
    public boolean 是否拍号显示启用() {
        return 拍号显示启用;
    }

    /**
     * FP-10 设置播放中拍号显示开关（运行时覆盖配置，热重载会重置为配置值）。
     *
     * @param 启用 true=启用；false=关闭
     */
    public void 设置拍号显示启用(boolean 启用) {
        this.拍号显示启用 = 启用;
    }

    /**
     * BPM 转换为每拍 tick 数。
     * 每拍秒数 = 60 / BPM；每拍 tick = 每拍秒数 × 20 = 1200 / BPM。
     * 最小为 1 tick。
     *
     * @param bpm 每分钟拍数
     * @return 每拍 tick 数
     */
    public static long bpm转tick(int bpm) {
        return Math.max(1L, Math.round((double) 每秒tick * 60 / bpm));
    }

    private static void 校验参数(int bpm, int 拍号分子, int 拍号分母) {
        if (bpm < BPM下限 || bpm > BPM上限) {
            throw new IllegalArgumentException("BPM必须在" + BPM下限 + "-" + BPM上限 + "范围内: " + bpm);
        }
        if (拍号分子 < 拍号下限 || 拍号分子 > 拍号上限) {
            throw new IllegalArgumentException("拍号分子必须在" + 拍号下限 + "-" + 拍号上限 + "范围内: " + 拍号分子);
        }
        if (拍号分母 < 拍号下限 || 拍号分母 > 拍号上限) {
            throw new IllegalArgumentException("拍号分母必须在" + 拍号下限 + "-" + 拍号上限 + "范围内: " + 拍号分母);
        }
    }

    private static int clamp(int 值, int 下限, int 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    private static float clamp(float 值, float 下限, float 上限) {
        return Math.max(下限, Math.min(上限, 值));
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static Sound 解析音色(String 名称) {
        if (名称 == null || 名称.isBlank()) {
            return Sound.BLOCK_NOTE_BLOCK_HAT;
        }
        // 遍历比较，避开 Sound.valueOf 内部的 Bukkit.getUnsafe() 调用（测试环境无服务器）。
        for (Sound 候选 : Sound.values()) {
            if (候选.name().equals(名称)) {
                return 候选;
            }
        }
        return Sound.BLOCK_NOTE_BLOCK_HAT;
    }

    /**
     * 节拍会话状态。
     */
    private static final class 节拍会话 {
        final int bpm;
        final int 拍号分子;
        final int 拍号分母;
        final long 起拍时刻;
        int 当前拍;
        BukkitRunnable 任务;

        节拍会话(int bpm, int 拍号分子, int 拍号分母, long 起拍时刻) {
            this.bpm = bpm;
            this.拍号分子 = 拍号分子;
            this.拍号分母 = 拍号分母;
            this.起拍时刻 = 起拍时刻;
            this.当前拍 = 0;
        }
    }
}
