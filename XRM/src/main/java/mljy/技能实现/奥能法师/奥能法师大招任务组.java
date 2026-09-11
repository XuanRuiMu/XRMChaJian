package mljy.技能实现.奥能法师;

import mljy.基础设施层.调试日志器;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每玩家大招任务句柄登记。技能只通过该类管理自身视觉与伤害任务。
 */
public final class 奥能法师大招任务组 {
    private final ConcurrentHashMap<UUID, Set<BukkitTask>> 活跃任务表 = new ConcurrentHashMap<>();

    public void 注册(UUID 玩家标识, BukkitTask 任务, String 技能标识) {
        if (玩家标识 == null || 任务 == null) {
            return;
        }
        活跃任务表.computeIfAbsent(玩家标识, ignored ->
                Collections.newSetFromMap(new ConcurrentHashMap<>())).add(任务);
        调试日志器.调试("奥能法师大招任务组",
                "登记任务：技能=%s 玩家=%s 任务数=%d", 技能标识, 玩家标识, 获取数量(玩家标识));
    }

    public void 移除(UUID 玩家标识, BukkitTask 任务, String 技能标识) {
        if (玩家标识 == null || 任务 == null) {
            return;
        }
        Set<BukkitTask> 任务集合 = 活跃任务表.get(玩家标识);
        if (任务集合 == null) {
            return;
        }
        任务集合.remove(任务);
        if (任务集合.isEmpty()) {
            活跃任务表.remove(玩家标识, 任务集合);
        }
        调试日志器.调试("奥能法师大招任务组",
                "移除任务：技能=%s 玩家=%s 剩余任务数=%d", 技能标识, 玩家标识, 获取数量(玩家标识));
    }

    public void 取消(UUID 玩家标识, String 技能标识, String 原因) {
        if (玩家标识 == null) {
            return;
        }
        Set<BukkitTask> 任务集合 = 活跃任务表.remove(玩家标识);
        if (任务集合 == null) {
            return;
        }
        for (BukkitTask 任务 : 任务集合) {
            if (任务 != null) {
                任务.cancel();
            }
        }
        调试日志器.调试("奥能法师大招任务组",
                "取消任务组：技能=%s 玩家=%s 原因=%s 取消数=%d",
                技能标识, 玩家标识, 原因, 任务集合.size());
    }

    public int 获取数量(UUID 玩家标识) {
        Set<BukkitTask> 任务集合 = 活跃任务表.get(玩家标识);
        return 任务集合 == null ? 0 : 任务集合.size();
    }
}
