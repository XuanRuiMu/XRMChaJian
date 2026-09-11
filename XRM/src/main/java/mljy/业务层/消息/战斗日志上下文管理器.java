package mljy.业务层.消息;

import mljy.基础设施层.调试日志器;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗日志上下文管理器。
 * 使用按玩家 UUID + 因果事件 UUID 隔离的嵌套 Map 管理吸血量，
 * 解决 PvP 场景下 ThreadLocal 将攻击者吸血误归因到受害者受击日志的问题，
 * 并支持同一事件内多条吸血累计到合并句末尾（数值求和、仅一条）。
 * 核心原则：吸血量必须附加在伤害来源的日志中，不能单独发送吸血日志。
 */
public class 战斗日志上下文管理器 {
    private static final double 初始吸血量 = 0.0;

    private final Map<UUID, Map<UUID, Double>> 吸血量映射 = new ConcurrentHashMap<>();

    /**
     * 累加指定玩家在指定事件中的吸血量。
     * 造成伤害/治疗时调用，吸血量累加到该玩家对应事件的上下文。
     */
    public void 累加吸血量(UUID 玩家标识, UUID 事件标识, double 吸血量) {
        if (玩家标识 == null) {
            return;
        }
        UUID 有效事件 = 事件标识 != null ? 事件标识 : 玩家标识;
        吸血量映射.computeIfAbsent(玩家标识, k -> new ConcurrentHashMap<>())
                .merge(有效事件, 吸血量, Double::sum);
        调试日志器.调试("战斗日志上下文", "累加吸血量：本次=%.3f 累计=%.3f", 吸血量, 获取吸血量(玩家标识, 事件标识));
    }

    /**
     * 兼容重载：未携带事件标识时退化为按玩家单桶累计。
     */
    public void 累加吸血量(UUID 玩家标识, double 吸血量) {
        累加吸血量(玩家标识, null, 吸血量);
    }

    /**
     * 获取指定玩家在指定事件中累加的吸血量（不清除上下文）。
     */
    public double 获取吸血量(UUID 玩家标识, UUID 事件标识) {
        if (玩家标识 == null) {
            return 初始吸血量;
        }
        UUID 有效事件 = 事件标识 != null ? 事件标识 : 玩家标识;
        Map<UUID, Double> 玩家桶 = 吸血量映射.get(玩家标识);
        return 玩家桶 == null ? 初始吸血量 : 玩家桶.getOrDefault(有效事件, 初始吸血量);
    }

    public double 获取吸血量(UUID 玩家标识) {
        return 获取吸血量(玩家标识, null);
    }

    /**
     * 获取指定玩家在指定事件中累加的吸血量并清除该事件上下文。
     * 发送伤害/治疗日志时调用，自动附加吸血信息后清除对应事件上下文。
     */
    public double 获取并清除吸血量(UUID 玩家标识, UUID 事件标识) {
        if (玩家标识 == null) {
            return 初始吸血量;
        }
        UUID 有效事件 = 事件标识 != null ? 事件标识 : 玩家标识;
        Map<UUID, Double> 玩家桶 = 吸血量映射.get(玩家标识);
        if (玩家桶 == null) {
            return 初始吸血量;
        }
        Double 值 = 玩家桶.remove(有效事件);
        if (玩家桶.isEmpty()) {
            吸血量映射.remove(玩家标识);
        }
        return 值 == null ? 初始吸血量 : 值;
    }

    public double 获取并清除吸血量(UUID 玩家标识) {
        return 获取并清除吸血量(玩家标识, null);
    }

    /**
     * 清除指定玩家在指定事件中的吸血量上下文。
     * 每次使用后必须调用，避免上下文泄漏。
     */
    public void 清除上下文(UUID 玩家标识, UUID 事件标识) {
        if (玩家标识 == null) {
            return;
        }
        UUID 有效事件 = 事件标识 != null ? 事件标识 : 玩家标识;
        Map<UUID, Double> 玩家桶 = 吸血量映射.get(玩家标识);
        if (玩家桶 == null) {
            return;
        }
        玩家桶.remove(有效事件);
        if (玩家桶.isEmpty()) {
            吸血量映射.remove(玩家标识);
        }
    }

    public void 清除上下文(UUID 玩家标识) {
        清除上下文(玩家标识, null);
    }

    /**
     * 判断指定玩家在指定事件中是否有累加的吸血量。
     */
    public boolean 有吸血量(UUID 玩家标识, UUID 事件标识) {
        return 获取吸血量(玩家标识, 事件标识) > 初始吸血量;
    }

    public boolean 有吸血量(UUID 玩家标识) {
        return 有吸血量(玩家标识, null);
    }
}
