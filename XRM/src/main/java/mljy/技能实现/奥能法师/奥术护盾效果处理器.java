package mljy.技能实现.奥能法师;

import com.google.inject.Inject;
import mljy.领域层.效果.效果处理器;
import mljy.领域层.效果.效果实例;
import mljy.业务层.效果调度服务;
import java.util.Set;
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

public class 奥术护盾效果处理器 implements 效果处理器 {

    @Inject
    private JavaPlugin 插件;

    private final Map<UUID, BukkitTask> 活跃任务表 = new ConcurrentHashMap<>();

    private final Map<UUID, Double> 精确护盾值表 = new ConcurrentHashMap<>();
    private final Set<UUID> 正在同步展示层数 = ConcurrentHashMap.newKeySet();
    @Inject
    private transient 效果调度服务 效果调度服务;

    public 奥术护盾效果处理器() {}

    public 奥术护盾效果处理器(效果调度服务 svc) {
        this.效果调度服务 = svc;
    }


    private static final Color 护盾主颜色 = Color.fromRGB(80, 150, 255);
    private static final Color 护盾修饰颜色 = Color.fromRGB(160, 210, 255);
    private static final Color 护盾核心颜色 = Color.fromRGB(200, 240, 255);
    private static final double 高度偏移 = 1.0;
    private static final double 护盾环半径 = 1.2;

    @Override
    public synchronized void on添加(UUID 目标标识, 效果实例 效果) {
        if (目标标识 == null || 效果 == null) {
            记录生命周期(目标标识, 效果, "添加", "无", "无", "跳过_参数无效", "none", null);
            return;
        }
        double 添加前 = 精确护盾值表.getOrDefault(目标标识, 0.0D);
        double 添加后 = Math.max(0.0D, 效果.获取参数覆盖().获取双精度("护盾吸收量", 效果.获取层数()));
        精确护盾值表.put(目标标识, 添加后);
        启动护盾粒子任务(目标标识, 效果.获取层数());
        记录生命周期(目标标识, 效果, "添加", 添加前, 添加后, "成功", "none", null);
    }
    @Override
    public void on移除(UUID 目标标识, 效果实例 效果) {
        终止生命周期(目标标识, 效果, "移除");
    }
    @Override
    public void on到期(UUID 目标标识, 效果实例 效果) {
        终止生命周期(目标标识, 效果, "自然到期");
    }
    @Override
    public void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数) {
        调试日志器.调试("奥术护盾效果处理器", "on层数变化：玩家=%s 旧层数=%d 新层数=%d",
                目标标识, 旧层数, 效果.获取层数());
        取消任务(目标标识);
        启动护盾粒子任务(目标标识, 效果.获取层数());
    }

    private void 启动护盾粒子任务(UUID 目标标识, int 层数) {
        // 防御：取消任何已存在的旧任务，防止任务泄漏
        BukkitTask 旧任务 = 活跃任务表.remove(目标标识);
        if (旧任务 != null) {
            旧任务.cancel();
            调试日志器.调试("奥术护盾效果处理器", "启动护盾粒子任务：取消了残留旧任务 玩家=%s", 目标标识);
        }
        调试日志器.调试("奥术护盾效果处理器", "启动护盾粒子任务：玩家=%s 层数=%d", 目标标识, 层数);
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
                播放护盾粒子(世界, 位置, tick, 层数);
                tick++;
            }
        };
        BukkitTask 任务句柄 = 任务.runTaskTimer(插件, 0, 4);
        任务句柄引用.set(任务句柄);
        活跃任务表.put(目标标识, 任务句柄);
    }

    private void 播放护盾粒子(World 世界, Location 位置, int tick, int 层数) {
        float 强度 = Math.min(1.0f + (层数 - 1) * 0.1f, 2.5f);
        int 环数 = Math.min(层数 / 5 + 1, 4);
        int 每环粒子 = Math.min(6 + 层数 / 2, 20);

        for (int 环层 = 0; 环层 < 环数; 环层++) {
            double y偏移 = (环层 - (环数 - 1) * 0.5) * 0.8;
            double 相位 = 环层 * Math.PI / 环数;
            for (int i = 0; i < 每环粒子; i++) {
                double 角 = i * Math.PI * 2 / 每环粒子 + tick * 0.15 + 相位;
                double x = 位置.getX() + Math.cos(角) * 护盾环半径;
                double z = 位置.getZ() + Math.sin(角) * 护盾环半径;
                Location 环点 = new Location(位置.getWorld(), x, 位置.getY() + y偏移, z);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.02, 0.02, 0.02, 0.0,
                        new Particle.DustOptions(环层 == 0 ? 护盾核心颜色 : (环层 % 2 == 0 ? 护盾主颜色 : 护盾修饰颜色), 0.8f * 强度));
            }
        }

        if (tick % 10 == 0) {
            世界.spawnParticle(Particle.DUST, 位置, (int) (3 * 强度),
                    0.3, 0.3, 0.3, 0.0,
                    new Particle.DustOptions(护盾核心颜色, 0.5f * 强度));
        }
    }

    private void 取消任务(UUID 目标标识) {
        BukkitTask 任务 = 活跃任务表.remove(目标标识);
        if (任务 != null) {
            任务.cancel();
            调试日志器.调试("奥术护盾效果处理器", "取消任务：已取消粒子任务 玩家=%s 剩余活跃任务数=%d",
                    目标标识, 活跃任务表.size());
        } else {
            调试日志器.调试("奥术护盾效果处理器", "取消任务：无活跃任务可取消 玩家=%s 活跃任务总数=%d",
                    目标标识, 活跃任务表.size());
        }
    }

    private void 清理失效任务(BukkitRunnable 当前任务, BukkitTask 任务句柄, UUID 目标标识, String 原因) {
        调试日志器.调试("奥术护盾效果处理器", "粒子任务自动清理：玩家=%s 原因=%s", 目标标识, 原因);
        当前任务.cancel();
        活跃任务表.remove(目标标识, 任务句柄);
    }

    public synchronized 护盾吸收结果 吸收伤害(UUID 目标标识, 效果实例 效果, double 输入伤害, UUID 根事件标识) {
        if (目标标识 == null || 效果 == null || !Double.isFinite(输入伤害) || 输入伤害 <= 0.0D) {
            调试日志器.调试("奥术护盾效果处理器",
                    "吸收伤害跳过：参数无效 target_uuid=%s input=%.2f reason=%s event=%s",
                    目标标识, 输入伤害,
                    目标标识 == null ? "missing_target" : (效果 == null ? "missing_effect" : "invalid_input"),
                    根事件标识);
            return new 护盾吸收结果(输入伤害, 0.0D, 输入伤害, 0.0D, false);
        }
        double 扣减前 = 精确护盾值表.computeIfAbsent(目标标识, k -> Math.max(0.0D, 效果.获取参数覆盖().获取双精度("护盾吸收量", 效果.获取层数())));
        if (扣减前 <= 0.0D) {
            调试日志器.调试("奥术护盾效果处理器",
                    "吸收伤害未触发：护盾为空 target_uuid=%s input=%.2f shield_before=%.2f event=%s",
                    目标标识, 输入伤害, 扣减前, 根事件标识);
            终止生命周期(目标标识, 效果, "护盾为空");
            return new 护盾吸收结果(输入伤害, 0.0D, 输入伤害, 0.0D, true);
        }
        double 吸收 = Math.min(输入伤害, 扣减前);
        double 剩余伤 = Math.max(0.0D, 输入伤害 - 吸收);
        double 剩余盾 = Math.max(0.0D, 扣减前 - 吸收);
        boolean 耗尽 = 剩余盾 <= 0.0D;
        调试日志器.调试("奥术护盾效果处理器",
                "吸收伤害结算：target_uuid=%s input=%.2f shield_before=%.2f absorbed=%.2f damage_remaining=%.2f shield_remaining=%.2f exhausted=%b event=%s",
                目标标识, 输入伤害, 扣减前, 吸收, 剩余伤, 剩余盾, 耗尽, 根事件标识);
        if (耗尽) {
            终止生命周期(目标标识, 效果, "护盾耗尽");
        } else {
            精确护盾值表.put(目标标识, 剩余盾);
            同步展示层数(目标标识, 效果, 剩余盾);
        }
        return new 护盾吸收结果(输入伤害, 吸收, 剩余伤, 剩余盾, 耗尽);
    }

    public record 护盾吸收结果(double 输入伤害, double 吸收量, double 剩余伤害, double 剩余护盾, boolean 已耗尽) {
    }

    public double 获取精确护盾值(UUID 目标标识) {
        return 目标标识 == null ? 0.0D : 精确护盾值表.getOrDefault(目标标识, 0.0D);
    }

    private void 同步展示层数(UUID 目标标识, 效果实例 效果, double 剩余护盾) {
        if (效果调度服务 == null) {
            调试日志器.调试("奥术护盾效果处理器",
                    "同步展示层数跳过：效果调度服务未注入 target_uuid=%s shield=%.2f",
                    目标标识, 剩余护盾);
            return;
        }
        int 展示层数 = (int) Math.ceil(剩余护盾);
        正在同步展示层数.add(目标标识);
        try {
            效果调度服务.设置层数(目标标识, 效果.获取效果标识(), 展示层数);
            调试日志器.调试("奥术护盾效果处理器",
                    "同步展示层数：target_uuid=%s shield_remaining=%.2f display_layers=%d effect_id=%s",
                    目标标识, 剩余护盾, 展示层数, 效果.获取效果标识());
        } finally {
            正在同步展示层数.remove(目标标识);
        }
    }

    private void 终止生命周期(UUID 目标标识, 效果实例 效果, String 原因) {
        精确护盾值表.remove(目标标识);
        正在同步展示层数.remove(目标标识);
        取消任务(目标标识);
        if (效果调度服务 != null) {
            效果调度服务.移除(目标标识, 效果 != null ? 效果.获取效果标识() : null);
        } else {
            调试日志器.调试("奥术护盾效果处理器",
                    "终止生命周期跳过移除：效果调度服务未注入 target_uuid=%s reason=%s",
                    目标标识, 原因);
        }
    }

    private void 记录生命周期(UUID 目标标识, 效果实例 效果, String 阶段,
            Object 旧值, Object 新值, String 结果, String 备注, UUID 根事件) {
        调试日志器.调试("奥术护盾效果处理器", "生命周期 id=%s stage=%s old=%s new=%s result=%s note=%s event=%s",
                目标标识, 阶段, 旧值, 新值, 结果, 备注, 根事件);
    }
}
