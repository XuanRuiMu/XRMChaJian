package mljy.技能实现.奥能法师;

import com.google.inject.Inject;
import mljy.属性服务;
import mljy.玩家服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.修饰器;
import mljy.领域层.效果.效果处理器;
import mljy.领域层.效果.效果实例;
import mljy.领域层.玩家.玩家快照;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class 奥能冥想效果处理器 implements 效果处理器 {

    private static final String 智力类别 = "智力";
    private static final String 修饰器标签 = "奥能冥想";
    private static final int 修饰器优先级 = 100;

    @Inject
    private JavaPlugin 插件;

    @Inject
    private 修饰器管理器 修饰器管理器;

    @Inject
    private 玩家服务 玩家服务;

    @Inject
    private 属性服务 属性服务;

    private final Map<UUID, BukkitTask> 活跃任务表 = new ConcurrentHashMap<>();

    private static final Color 一层主颜色 = Color.fromRGB(60, 20, 120);
    private static final Color 二层主颜色 = Color.fromRGB(120, 50, 200);
    private static final Color 三层主颜色 = Color.fromRGB(160, 70, 240);
    private static final Color 四层主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 极强核心颜色 = Color.fromRGB(220, 140, 255);
    private static final double 高度偏移 = 1.0;
    private static final double 光柱高度 = 1.0;

    @Override
    public void on添加(UUID 目标标识, 效果实例 效果) {
        调试日志器.调试("奥能冥想效果处理器", "on添加：玩家=%s 层数=%d", 目标标识, 效果.获取层数());
        int 秘能层数 = Math.max(1, 效果.获取层数());
        取消任务(目标标识);
        注册智力修饰器(目标标识, 效果);
        奥能法师大招特效配置 配置 = 奥能法师大招特效配置.加载(插件);
        Player 初始玩家 = Bukkit.getPlayer(目标标识);
        Location 起点 = 初始玩家 == null ? null : 初始玩家.getLocation();
        String 起点世界 = 起点 == null || 起点.getWorld() == null ? null : 起点.getWorld().getName();
        调试日志器.调试("奥能冥想效果处理器", "启动粒子任务：玩家=%s 层数=%d", 目标标识, 秘能层数);
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

                if (秘能层数 <= 1) {
                    播放一层特效(世界, 位置, tick, 配置);
                } else if (秘能层数 == 2) {
                    播放二层特效(世界, 位置, tick, 配置);
                } else if (秘能层数 == 3) {
                    播放三层特效(世界, 位置, tick, 配置);
                } else {
                    播放四层以上特效(世界, 位置, tick, 秘能层数, 配置);
                }
                tick++;
            }
        };
        BukkitTask 任务句柄 = 任务.runTaskTimer(插件, 0, 配置.冥想持续间隔刻());
        任务句柄引用.set(任务句柄);
        活跃任务表.put(目标标识, 任务句柄);
    }

    private void 播放一层特效(World 世界, Location 位置, int tick, 奥能法师大招特效配置 配置) {
        世界.spawnParticle(Particle.DUST, 位置, 配置.粒子数量(2),
                0.3, 0.3, 0.3, 0.0,
                new Particle.DustOptions(一层主颜色, 0.8f));
        if (tick % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double 角 = i * Math.PI / 2 + tick * 0.1;
                double x = 位置.getX() + Math.cos(角) * 0.5;
                double z = 位置.getZ() + Math.sin(角) * 0.5;
                世界.spawnParticle(Particle.DUST,
                        new Location(世界, x, 位置.getY() + 0.3, z), 1,
                        0.05, 0.05, 0.05, 0.0,
                        new Particle.DustOptions(一层主颜色, 0.5f));
            }
        }
    }

    private void 播放二层特效(World 世界, Location 位置, int tick, 奥能法师大招特效配置 配置) {
        世界.spawnParticle(Particle.DUST, 位置, 配置.粒子数量(3),
                0.4, 0.4, 0.4, 0.0,
                new Particle.DustOptions(二层主颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 位置, 配置.粒子数量(1), 0.3, 0.3, 0.3, 0.02);
        if (tick % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double 角 = i * Math.PI / 3 + tick * 0.15;
                double x = 位置.getX() + Math.cos(角) * 0.6;
                double z = 位置.getZ() + Math.sin(角) * 0.6;
                世界.spawnParticle(Particle.DUST,
                        new Location(世界, x, 位置.getY() + 0.3, z), 1,
                        0.05, 0.05, 0.05, 0.0,
                        new Particle.DustOptions(二层主颜色, 0.6f));
            }
            if (tick % 12 == 0) {
                世界.playSound(位置, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 配置.音量(0.2f), 0.6f);
            }
        }
    }

    private void 播放三层特效(World 世界, Location 位置, int tick, 奥能法师大招特效配置 配置) {
        世界.spawnParticle(Particle.DUST, 位置, 配置.粒子数量(4),
                0.5, 0.5, 0.5, 0.0,
                new Particle.DustOptions(三层主颜色, 1.2f));
        世界.spawnParticle(Particle.END_ROD, 位置, 配置.粒子数量(2), 0.4, 0.4, 0.4, 0.03);
        世界.spawnParticle(Particle.SOUL_FIRE_FLAME, 位置, 配置.粒子数量(1), 0.3, 0.3, 0.3, 0.01);

        double 角步 = Math.PI * 2 / 8;
        for (int i = 0; i < 8; i++) {
            double 角 = i * 角步 + tick * 0.25;
            double x = 位置.getX() + Math.cos(角) * 0.7;
            double z = 位置.getZ() + Math.sin(角) * 0.7;
            世界.spawnParticle(Particle.DUST,
                    new Location(世界, x, 位置.getY() + Math.sin(tick * 0.3) * 0.3, z), 1,
                    0.03, 0.03, 0.03, 0.0,
                    new Particle.DustOptions(三层主颜色, 0.7f));
        }

        if (tick % 8 == 0) {
            世界.playSound(位置, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 配置.音量(0.3f), 0.5f);
        }
    }

    private void 播放四层以上特效(World 世界, Location 位置, int tick, int 实际层数,
                             奥能法师大招特效配置 配置) {
        float 强化系数 = (float) (配置.强度()
                * (实际层数 > 4 ? Math.min(1.0f + (实际层数 - 4) * 0.15f, 2.0f) : 1.0f));

        世界.spawnParticle(Particle.DUST, 位置, 配置.粒子数量((int) (4 * 强化系数)),
                0.6, 0.6, 0.6, 0.0,
                new Particle.DustOptions(四层主颜色, 1.5f * 强化系数));
        世界.spawnParticle(Particle.END_ROD, 位置, 配置.粒子数量((int) (1.0 * 强化系数)), 0.5, 0.5, 0.5, 0.04);

        // 旋转光柱（精简：10→6柱，加大步长降低密度）
        double 角步 = Math.PI * 2 / 6;
        for (int i = 0; i < 6; i++) {
            double 角 = i * 角步 + tick * 0.3;
            double 半径 = 0.8 + Math.sin(tick * 0.2) * 0.2;
            double x = 位置.getX() + Math.cos(角) * 半径;
            double z = 位置.getZ() + Math.sin(角) * 半径;
            for (double yOff = 0; yOff < 光柱高度; yOff += 0.6) {
                世界.spawnParticle(Particle.DUST,
                        new Location(世界, x, 位置.getY() + yOff, z), 1,
                        0.02, 0.02, 0.02, 0.0,
                        new Particle.DustOptions(极强核心颜色, 0.5f * 强化系数));
            }
        }

        if (tick % 5 == 0) {
            世界.playSound(位置, Sound.BLOCK_BEACON_AMBIENT, 配置.音量(0.4f * 强化系数), 0.8f);
        }
    }

    @Override
    public void on移除(UUID 目标标识, 效果实例 效果) {
        调试日志器.调试("奥能冥想效果处理器", "on移除：玩家=%s", 目标标识);
        取消任务(目标标识);
        注销智力修饰器(目标标识);
    }

    @Override
    public void on到期(UUID 目标标识, 效果实例 效果) {
        调试日志器.调试("奥能冥想效果处理器", "on到期：玩家=%s 当前活跃任务数=%d",
                目标标识, 活跃任务表.size());
        取消任务(目标标识);
        注销智力修饰器(目标标识);
    }

    @Override
    public void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数) {
        调试日志器.调试("奥能冥想效果处理器", "on层数变化：玩家=%s 旧层数=%d 新层数=%d",
                目标标识, 旧层数, 效果.获取层数());
        on添加(目标标识, 效果);
    }

    private void 取消任务(UUID 目标标识) {
        BukkitTask 任务 = 活跃任务表.remove(目标标识);
        if (任务 != null) {
            任务.cancel();
            调试日志器.调试("奥能冥想效果处理器", "取消任务：玩家=%s 剩余活跃任务数=%d",
                    目标标识, 活跃任务表.size());
        }
    }

    private void 清理失效任务(BukkitRunnable 当前任务, BukkitTask 任务句柄, UUID 目标标识, String 原因) {
        调试日志器.调试("奥能冥想效果处理器", "粒子任务自动清理：玩家=%s 原因=%s", 目标标识, 原因);
        当前任务.cancel();
        活跃任务表.remove(目标标识, 任务句柄);
    }

    private void 注册智力修饰器(UUID 目标标识, 效果实例 效果) {
        修饰器管理器.注销修饰器(目标标识, 智力类别, 修饰器标签);
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(目标标识);
        if (快照可选.isEmpty()) {
            调试日志器.调试("奥能冥想效果处理器",
                    "注册智力修饰器跳过：玩家=%s 原因=玩家快照缺失", 目标标识);
            属性服务.刷新属性(目标标识);
            return;
        }
        玩家快照 快照 = 快照可选.get();
        int 智力加成值 = 效果.获取参数覆盖().获取整数("智力加成", 快照.等级() * 效果.获取层数());
        修饰器 修饰器 = new 修饰器(智力类别, 修饰器标签, 修饰方式.相加, 智力加成值, 修饰器优先级);
        修饰器管理器.注册修饰器(目标标识, 修饰器);
        调试日志器.调试("奥能冥想效果处理器",
                "注册智力修饰器：玩家=%s 等级=%d 层数=%d 智力加成=%d",
                目标标识, 快照.等级(), 效果.获取层数(), 智力加成值);
        属性服务.刷新属性(目标标识);
    }

    private void 注销智力修饰器(UUID 目标标识) {
        修饰器管理器.注销修饰器(目标标识, 智力类别, 修饰器标签);
        调试日志器.调试("奥能冥想效果处理器", "注销智力修饰器：玩家=%s", 目标标识);
        属性服务.刷新属性(目标标识);
    }
}
