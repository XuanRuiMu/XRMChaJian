package mljy.技能实现.奥能法师;

import com.google.inject.Inject;
import mljy.领域层.效果.效果处理器;
import mljy.领域层.效果.效果实例;
import mljy.基础设施层.调试日志器;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 奥能冥想蓄能效果处理器。
 * 蓄能阶段（2秒）：金色/白色上升粒子+光环，层数=消耗的秘能层数。
 */
public class 奥能冥想蓄能效果处理器 implements 效果处理器 {

    @Inject
    private JavaPlugin 插件;

    private final Map<UUID, BukkitTask> 活跃任务表 = new ConcurrentHashMap<>();

    private static final double 高度偏移 = 1.2;
    private static final double 环半径 = 0.7;
    private static final double 光柱高度 = 0.8;
    private static final Color 蓄能主颜色 = Color.fromRGB(255, 220, 120);
    private static final Color 蓄能修饰颜色 = Color.fromRGB(255, 200, 80);
    private static final Color 蓄能核心颜色 = Color.fromRGB(255, 255, 255);
    private static final Color 光环颜色 = Color.fromRGB(255, 240, 200);

    @Override
    public void on添加(UUID 目标标识, 效果实例 效果) {
        int 层数 = Math.max(1, 效果.获取层数());
        调试日志器.调试("奥能冥想蓄能效果处理器", "on添加：玩家=%s 层数=%d", 目标标识, 层数);
        启动蓄能粒子任务(目标标识, 层数, 奥能法师大招特效配置.加载(插件));
    }

    @Override
    public void on移除(UUID 目标标识, 效果实例 效果) {
        调试日志器.调试("奥能冥想蓄能效果处理器", "on移除：玩家=%s", 目标标识);
        取消任务(目标标识);
    }

    @Override
    public void on到期(UUID 目标标识, 效果实例 效果) {
        调试日志器.调试("奥能冥想蓄能效果处理器", "on到期：玩家=%s", 目标标识);
        取消任务(目标标识);
    }

    @Override
    public void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数) {
        调试日志器.调试("奥能冥想蓄能效果处理器", "on层数变化：玩家=%s 旧层数=%d 新层数=%d",
                目标标识, 旧层数, 效果.获取层数());
        on添加(目标标识, 效果);
    }

    private void 启动蓄能粒子任务(UUID 目标标识, int 层数, 奥能法师大招特效配置 配置) {
        取消任务(目标标识);
        Player 初始玩家 = Bukkit.getPlayer(目标标识);
        Location 起点 = 初始玩家 == null ? null : 初始玩家.getLocation();
        String 起点世界 = 起点 == null || 起点.getWorld() == null ? null : 起点.getWorld().getName();
        调试日志器.调试("奥能冥想蓄能效果处理器", "启动粒子任务：玩家=%s 层数=%d", 目标标识, 层数);
        AtomicReference<BukkitTask> 任务句柄引用 = new AtomicReference<>();
        BukkitRunnable 任务 = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                Player 玩家 = Bukkit.getPlayer(目标标识);
                if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
                    清理失效任务(this, 任务句柄引用.get(), 目标标识,
                            玩家 == null ? "玩家不存在" : (!玩家.isOnline() ? "玩家离线" : "玩家死亡"));
                    return;
                }
                Location 当前原点 = 玩家.getLocation();
                if (当前原点 == null || 当前原点.getWorld() == null
                        || 起点世界 == null || !起点世界.equals(当前原点.getWorld().getName())
                        || 起点 != null && 起点.getWorld() != null
                        && 起点.distanceSquared(当前原点) > 配置.最大距离() * 配置.最大距离()) {
                    清理失效任务(this, 任务句柄引用.get(), 目标标识, "玩家世界切换或超出最大距离");
                    return;
                }
                Location 位置 = 当前原点.clone().add(0, 高度偏移, 0);
                World 世界 = 位置.getWorld();
                if (世界 == null) {
                    清理失效任务(this, 任务句柄引用.get(), 目标标识, "玩家世界为空");
                    return;
                }
                播放蓄能粒子(世界, 位置, tick, 层数, 配置);
                tick++;
            }
        };
        BukkitTask 任务句柄 = 任务.runTaskTimer(插件, 0, 配置.冥想蓄能间隔刻());
        任务句柄引用.set(任务句柄);
        活跃任务表.put(目标标识, 任务句柄);
    }

    private void 播放蓄能粒子(World 世界, Location 位置, int tick, int 层数,
                         奥能法师大招特效配置 配置) {
        float 强度 = (float) Math.min(配置.强度() * (1.0f + (层数 - 1) * 0.2f), 3.0f);

        // 地面光环：金色旋转环
        int 每环粒子 = 3;
        for (int i = 0; i < 每环粒子; i++) {
            double 角 = i * Math.PI * 2 / 每环粒子 + tick * 0.15;
            double x = 位置.getX() + Math.cos(角) * 环半径;
            double z = 位置.getZ() + Math.sin(角) * 环半径;
            Location 环点 = new Location(位置.getWorld(), x, 位置.getY() - 0.5, z);
            世界.spawnParticle(Particle.DUST, 环点, 1,
                    0.02, 0.02, 0.02, 0.0,
                    new Particle.DustOptions(光环颜色, 0.7f * 强度));
        }

        // 上升粒子：白色/金色上升粒子柱
        for (int i = 0; i < (int) (1 * 强度); i++) {
            double 角度 = tick * 0.3 + i * Math.PI * 2 / (4 * 强度);
            double 半径 = 环半径 * 0.4;
            double x = 位置.getX() + Math.cos(角度) * 半径;
            double z = 位置.getZ() + Math.sin(角度) * 半径;
            double y = 位置.getY() + (tick * 0.08 + i * 0.15) % 光柱高度;
            Location 上升点 = new Location(位置.getWorld(), x, y, z);
            Color 颜色 = i % 3 == 0 ? 蓄能核心颜色 : (i % 3 == 1 ? 蓄能主颜色 : 蓄能修饰颜色);
            世界.spawnParticle(Particle.DUST, 上升点, 1,
                    0.03, 0.03, 0.03, 0.0,
                    new Particle.DustOptions(颜色, 0.6f * 强度));
        }

        // 中心光柱
        if (tick % 4 == 0) {
            世界.spawnParticle(Particle.END_ROD, 位置, 配置.粒子数量((int) (1 * 强度)), 0.3, 0.5, 0.3, 0.03);
            世界.spawnParticle(Particle.DUST, 位置, 配置.粒子数量((int) (2 * 强度)),
                    0.3, 0.5, 0.3, 0.0,
                    new Particle.DustOptions(蓄能核心颜色, 0.8f * 强度));
        }

        // 蓄能音效
        if (tick % 15 == 0) {
            世界.playSound(位置, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 配置.音量(0.2f), 0.7f + 层数 * 0.1f);
        }
        if (tick % 20 == 0) {
            世界.playSound(位置, Sound.BLOCK_BEACON_AMBIENT, 配置.音量(0.3f), 0.6f);
        }
    }

    private void 取消任务(UUID 目标标识) {
        BukkitTask 任务 = 活跃任务表.remove(目标标识);
        if (任务 != null) {
            任务.cancel();
            调试日志器.调试("奥能冥想蓄能效果处理器", "取消任务：玩家=%s 剩余活跃任务数=%d",
                    目标标识, 活跃任务表.size());
        }
    }

    private void 清理失效任务(BukkitRunnable 当前任务, BukkitTask 任务句柄, UUID 目标标识, String 原因) {
        调试日志器.调试("奥能冥想蓄能效果处理器", "粒子任务自动清理：玩家=%s 原因=%s", 目标标识, 原因);
        当前任务.cancel();
        活跃任务表.remove(目标标识, 任务句柄);
    }
}
