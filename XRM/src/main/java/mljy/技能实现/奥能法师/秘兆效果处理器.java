package mljy.技能实现.奥能法师;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.效果.效果处理器;
import mljy.领域层.效果.效果实例;
import mljy.基础设施层.调试日志器;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
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
 * 秘兆效果处理器。
 * 精通魔法预兆触发时，玩家周身环绕紫色奥术粒子，预示下一次攻击将获得额外伤害。
 */
@Singleton
public class 秘兆效果处理器 implements 效果处理器 {

    @Inject
    private JavaPlugin 插件;

    private final Map<UUID, BukkitTask> 活跃任务表 = new ConcurrentHashMap<>();

    private static final double 高度偏移 = 1.5;
    private static final double 环半径 = 0.8;
    private static final Color 秘兆主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 秘兆修饰颜色 = Color.fromRGB(255, 200, 255);
    private static final Color 秘兆核心颜色 = Color.fromRGB(140, 40, 220);

    @Override
    public synchronized void on添加(UUID 目标标识, 效果实例 效果) {
        if (目标标识 == null || 效果 == null) {
            记录生命周期(目标标识, 效果, "添加", "无", "无", "跳过_参数无效", "none", null);
            return;
        }
        启动秘兆粒子任务(目标标识);
        记录生命周期(目标标识, 效果, "添加", "inactive", "active", "成功", "none", null);
    }

    @Override
    public void on移除(UUID 目标标识, 效果实例 效果) {
        终止生命周期(目标标识, 效果, "移除", null);
    }

    @Override
    public void on到期(UUID 目标标识, 效果实例 效果) {
        终止生命周期(目标标识, 效果, "自然到期", null);
    }

    @Override
    public void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数) {
        if (效果 == null || 效果.获取层数() <= 0) {
            终止生命周期(目标标识, 效果, "层数耗尽", null);
            return;
        }
        记录生命周期(目标标识, 效果, "层数变化", 旧层数, 效果.获取层数(), "成功_任务保持", "none", null);
    }

    private synchronized void 启动秘兆粒子任务(UUID 目标标识) {
        BukkitTask 旧任务 = 活跃任务表.remove(目标标识);
        if (旧任务 != null) {
            旧任务.cancel();
            记录生命周期(目标标识, null, "粒子任务停止", "registered", "unregistered", "成功", "刷新替换", null);
        }
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
                Location 位置 = 玩家.getLocation().clone().add(0, 高度偏移, 0);
                World 世界 = 位置.getWorld();
                if (世界 == null) {
                    清理失效任务(this, 任务句柄引用.get(), 目标标识, "玩家世界为空");
                    return;
                }
                播放秘兆粒子(世界, 位置, tick);
                tick++;
            }
        };
        BukkitTask 任务句柄 = 任务.runTaskTimer(插件, 0, 3);
        任务句柄引用.set(任务句柄);
        活跃任务表.put(目标标识, 任务句柄);
        记录生命周期(目标标识, null, "粒子任务启动", "unregistered", "registered", "成功", "none", null);
    }

    private void 播放秘兆粒子(World 世界, Location 位置, int tick) {
        // 三层旋转环
        for (int 环层 = 0; 环层 < 3; 环层++) {
            double y偏移 = (环层 - 1) * 0.5;
            double 相位 = 环层 * Math.PI * 2 / 3;
            int 每环粒子 = 8;
            for (int i = 0; i < 每环粒子; i++) {
                double 角 = i * Math.PI * 2 / 每环粒子 + tick * 0.2 + 相位;
                double x = 位置.getX() + Math.cos(角) * 环半径;
                double z = 位置.getZ() + Math.sin(角) * 环半径;
                Location 环点 = new Location(位置.getWorld(), x, 位置.getY() + y偏移, z);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.02, 0.02, 0.02, 0.0,
                        new Particle.DustOptions(环层 == 1 ? 秘兆主颜色 : (环层 == 0 ? 秘兆核心颜色 : 秘兆修饰颜色), 0.7f));
            }
        }

        // 中心光晕脉冲
        if (tick % 6 == 0) {
            世界.spawnParticle(Particle.DUST, 位置, 5,
                    0.3, 0.3, 0.3, 0.0,
                    new Particle.DustOptions(秘兆修饰颜色, 0.8f));
            世界.spawnParticle(Particle.END_ROD, 位置, 3, 0.2, 0.2, 0.2, 0.01);
        }

        // 外围上升粒子
        if (tick % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                double 角 = i * Math.PI * 2 / 3 + tick * 0.1;
                double x = 位置.getX() + Math.cos(角) * 环半径 * 1.3;
                double z = 位置.getZ() + Math.sin(角) * 环半径 * 1.3;
                Location 上升点 = new Location(位置.getWorld(), x, 位置.getY() + 0.2, z);
                世界.spawnParticle(Particle.SOUL_FIRE_FLAME, 上升点, 1,
                        0.05, 0.1, 0.05, 0.01);
            }
        }
    }

    public void 停止特效(UUID 目标标识) {
        终止生命周期(目标标识, null, "伤害结算消费", null);
    }

    private synchronized void 清理失效任务(BukkitRunnable 当前任务, BukkitTask 任务句柄, UUID 目标标识, String 原因) {
        if (当前任务 != null) {
            当前任务.cancel();
        }
        boolean 已移除 = 任务句柄 != null && 活跃任务表.remove(目标标识, 任务句柄);
        记录生命周期(目标标识, null, "粒子任务停止", "registered", 已移除 ? "unregistered" : "newer_task_kept",
                已移除 ? "成功" : "跳过_任务已替换", 原因, null);
    }

    private synchronized void 终止生命周期(UUID 目标标识, 效果实例 效果, String 结束原因, UUID 根事件标识) {
        BukkitTask 任务 = 目标标识 == null ? null : 活跃任务表.remove(目标标识);
        if (任务 != null) {
            任务.cancel();
        }
        记录生命周期(目标标识, 效果, "终止", 任务 == null ? "unregistered" : "registered", "unregistered",
                任务 == null ? "幂等_已终止" : "成功", 结束原因, 根事件标识);
    }

    private void 记录生命周期(UUID 目标标识, 效果实例 效果, String 阶段, Object 变化前, Object 变化后,
                            String 结果, String 结束原因, UUID 根事件标识) {
        UUID 有效根事件标识 = 根事件标识 != null ? 根事件标识 : 事件日志上下文.获取当前事件标识或空();
        String 效果标识 = 效果 == null ? "1_9_1" : 效果.获取效果标识();
        调试日志器.调试("秘兆效果处理器",
                "stage=%s player_uuid=%s effect_id=%s root_event_id=%s before=%s after=%s result=%s end_reason=%s",
                阶段, 目标标识, 效果标识, 有效根事件标识 == null ? "none" : 有效根事件标识,
                变化前, 变化后, 结果, 结束原因);
    }
}
