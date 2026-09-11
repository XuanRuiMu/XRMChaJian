package mljy.业务层;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import mljy.基础设施层.调试日志器;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class 技能冷却服务实现 implements 技能冷却服务 {
    private final Map<UUID, Map<String, Long>> 技能冷却表 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> 公共冷却表 = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, BukkitTask>> 独立CD完成任务表 = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> 公CD完成任务表 = new ConcurrentHashMap<>();
    private BiConsumer<UUID, String> 独立CD开始回调;
    private BiConsumer<UUID, String> 独立CD完成回调;
    private Consumer<UUID> 公CD开始回调;
    private Consumer<UUID> 公CD完成回调;
    private final JavaPlugin 插件;

    @Inject
    public 技能冷却服务实现(JavaPlugin 插件) {
        this.插件 = 插件;
    }

    @Override
    public boolean 是否冷却中(UUID 玩家标识, String 技能标识) {
        return 获取剩余冷却(玩家标识, 技能标识) > 0;
    }

    @Override
    public double 获取剩余冷却(UUID 玩家标识, String 技能标识) {
        Map<String, Long> 玩家冷却 = 技能冷却表.get(玩家标识);
        if (玩家冷却 == null) {
            return 0;
        }
        Long 结束时间 = 玩家冷却.get(技能标识);
        if (结束时间 == null) {
            return 0;
        }
        return Math.max(0, (结束时间 - System.currentTimeMillis()) / 1000.0);
    }

    @Override
    public void 开始冷却(UUID 玩家标识, String 技能标识, double 冷却时间) {
        long 结束时间 = System.currentTimeMillis() + (long) (冷却时间 * 1000);
        技能冷却表.computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>()).put(技能标识, 结束时间);
        调试日志器.调试("技能冷却服务", "开始冷却：玩家=%s 技能=%s 冷却=%.1f秒", 玩家标识, 技能标识, 冷却时间);

        schedule独立CD完成(玩家标识, 技能标识, 冷却时间);

        if (独立CD开始回调 != null) {
            独立CD开始回调.accept(玩家标识, 技能标识);
        }
    }

    @Override
    public void 开始公共冷却(UUID 玩家标识, double 公共冷却时间) {
        long 结束时间 = System.currentTimeMillis() + (long) (公共冷却时间 * 1000);
        公共冷却表.put(玩家标识, 结束时间);
        调试日志器.调试("技能冷却服务", "开始公共冷却：玩家=%s 公CD=%.3f秒", 玩家标识, 公共冷却时间);

        schedule公CD完成(玩家标识, 公共冷却时间);

        if (公CD开始回调 != null) {
            公CD开始回调.accept(玩家标识);
        }
    }

    private void schedule独立CD完成(UUID 玩家标识, String 技能标识, double 冷却时间) {
        Map<String, BukkitTask> 玩家任务表 = 独立CD完成任务表.computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>());
        BukkitTask 旧任务 = 玩家任务表.get(技能标识);
        if (旧任务 != null && !旧任务.isCancelled()) {
            旧任务.cancel();
        }
        long 延迟刻 = Math.max(1, (long) (冷却时间 * 20));
        BukkitTask 新任务 = 插件.getServer().getScheduler().runTaskLater(插件, () -> {
            double 剩余 = 获取剩余冷却(玩家标识, 技能标识);
            if (剩余 <= 0) {
                调试日志器.调试("技能冷却服务", "独立CD完成：玩家=%s 技能=%s", 玩家标识, 技能标识);
                if (独立CD完成回调 != null) {
                    独立CD完成回调.accept(玩家标识, 技能标识);
                }
            }
        }, 延迟刻);
        玩家任务表.put(技能标识, 新任务);
    }

    private void schedule公CD完成(UUID 玩家标识, double 公共冷却时间) {
        BukkitTask 旧任务 = 公CD完成任务表.get(玩家标识);
        if (旧任务 != null && !旧任务.isCancelled()) {
            旧任务.cancel();
        }
        long 延迟刻 = Math.max(1, (long) (公共冷却时间 * 20));
        BukkitTask 新任务 = 插件.getServer().getScheduler().runTaskLater(插件, () -> {
            double 剩余 = 获取公共冷却剩余(玩家标识);
            if (剩余 <= 0) {
                调试日志器.调试("技能冷却服务", "公CD完成：玩家=%s", 玩家标识);
                if (公CD完成回调 != null) {
                    公CD完成回调.accept(玩家标识);
                }
            }
        }, 延迟刻);
        公CD完成任务表.put(玩家标识, 新任务);
    }

    @Override
    public boolean 是否公共冷却中(UUID 玩家标识) {
        return 获取公共冷却剩余(玩家标识) > 0;
    }

    @Override
    public double 获取公共冷却剩余(UUID 玩家标识) {
        Long 结束时间 = 公共冷却表.get(玩家标识);
        if (结束时间 == null) {
            return 0;
        }
        return Math.max(0, (结束时间 - System.currentTimeMillis()) / 1000.0);
    }

    @Override
    public void 设置独立CD开始回调(BiConsumer<UUID, String> 回调) {
        this.独立CD开始回调 = 回调;
    }

    @Override
    public void 设置独立CD完成回调(BiConsumer<UUID, String> 回调) {
        this.独立CD完成回调 = 回调;
    }

    @Override
    public void 设置公CD开始回调(Consumer<UUID> 回调) {
        this.公CD开始回调 = 回调;
    }

    @Override
    public void 设置公CD完成回调(Consumer<UUID> 回调) {
        this.公CD完成回调 = 回调;
    }

    @Override
    public void 清理玩家(UUID 玩家标识) {
        技能冷却表.remove(玩家标识);
        公共冷却表.remove(玩家标识);
        Map<String, BukkitTask> 玩家CD任务 = 独立CD完成任务表.remove(玩家标识);
        if (玩家CD任务 != null) {
            玩家CD任务.values().forEach(任务 -> { if (!任务.isCancelled()) 任务.cancel(); });
        }
        BukkitTask 公CD任务 = 公CD完成任务表.remove(玩家标识);
        if (公CD任务 != null && !公CD任务.isCancelled()) {
            公CD任务.cancel();
        }
        调试日志器.调试("技能冷却服务", "清理玩家冷却数据：玩家=%s", 玩家标识);
    }

    @Override
    public void 重置冷却(UUID 玩家标识, String 技能标识) {
        boolean 正在冷却 = 是否冷却中(玩家标识, 技能标识);

        Map<String, Long> 玩家冷却 = 技能冷却表.get(玩家标识);
        if (玩家冷却 != null) {
            玩家冷却.remove(技能标识);
        }

        Map<String, BukkitTask> 玩家任务表 = 独立CD完成任务表.get(玩家标识);
        if (玩家任务表 != null) {
            BukkitTask 任务 = 玩家任务表.remove(技能标识);
            if (任务 != null && !任务.isCancelled()) {
                任务.cancel();
            }
        }

        if (正在冷却 && 独立CD完成回调 != null) {
            独立CD完成回调.accept(玩家标识, 技能标识);
        }

        调试日志器.调试("技能冷却服务", "重置冷却：玩家=%s 技能=%s 触发回调=%s", 玩家标识, 技能标识, 正在冷却);
    }

    @Override
    public void 重置公共冷却(UUID 玩家标识) {
        boolean 正在冷却 = 是否公共冷却中(玩家标识);

        公共冷却表.remove(玩家标识);

        BukkitTask 任务 = 公CD完成任务表.remove(玩家标识);
        if (任务 != null && !任务.isCancelled()) {
            任务.cancel();
        }

        if (正在冷却 && 公CD完成回调 != null) {
            公CD完成回调.accept(玩家标识);
        }

        调试日志器.调试("技能冷却服务", "重置公共冷却：玩家=%s 触发回调=%s", 玩家标识, 正在冷却);
    }

    @Override
    public void 重置所有冷却(UUID 玩家标识) {
        List<String> 正在冷却技能 = new ArrayList<>();
        Map<String, Long> 玩家冷却 = 技能冷却表.get(玩家标识);
        if (玩家冷却 != null) {
            for (String 技能标识 : 玩家冷却.keySet()) {
                if (是否冷却中(玩家标识, 技能标识)) {
                    正在冷却技能.add(技能标识);
                }
            }
        }
        boolean 公CD正在冷却 = 是否公共冷却中(玩家标识);

        技能冷却表.remove(玩家标识);
        公共冷却表.remove(玩家标识);

        Map<String, BukkitTask> 玩家任务表 = 独立CD完成任务表.remove(玩家标识);
        if (玩家任务表 != null) {
            玩家任务表.values().forEach(任务 -> { if (!任务.isCancelled()) 任务.cancel(); });
        }
        BukkitTask 公CD任务 = 公CD完成任务表.remove(玩家标识);
        if (公CD任务 != null && !公CD任务.isCancelled()) {
            公CD任务.cancel();
        }

        if (独立CD完成回调 != null) {
            for (String 技能标识 : 正在冷却技能) {
                独立CD完成回调.accept(玩家标识, 技能标识);
            }
        }
        if (公CD正在冷却 && 公CD完成回调 != null) {
            公CD完成回调.accept(玩家标识);
        }

        调试日志器.调试("技能冷却服务", "重置所有冷却：玩家=%s 技能数=%d 公CD触发回调=%s", 玩家标识, 正在冷却技能.size(), 公CD正在冷却);
    }
}