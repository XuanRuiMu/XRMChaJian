package mljy.领域层.任务;

import java.util.List;
import java.util.Map;

/**
 * 任务奖励领域对象。
 * 支持三种奖励类型：经验等级、金币、物品。
 * 物品奖励以 Map<物品标识, 数量> 表示。
 */
public record 任务奖励(
        int 经验等级,
        int 金币,
        Map<String, Integer> 物品列表
) {
    /** 空奖励常量 */
    public static final 任务奖励 空 = new 任务奖励(0, 0, Map.of());

    /**
     * 工厂方法：仅经验奖励。
     */
    public static 任务奖励 经验(int 经验等级) {
        return new 任务奖励(经验等级, 0, Map.of());
    }

    /**
     * 工厂方法：经验+金币奖励。
     */
    public static 任务奖励 经验金币(int 经验等级, int 金币) {
        return new 任务奖励(经验等级, 金币, Map.of());
    }

    /**
     * 工厂方法：完整奖励。
     */
    public static 任务奖励 完整(int 经验等级, int 金币, Map<String, Integer> 物品列表) {
        return new 任务奖励(经验等级, 金币, 物品列表 == null ? Map.of() : Map.copyOf(物品列表));
    }

    /**
     * 判断是否有任何奖励。
     */
    public boolean 是否有奖励() {
        return 经验等级 > 0 || 金币 > 0 || (物品列表 != null && !物品列表.isEmpty());
    }

    /**
     * 获取物品列表（不可变）。
     */
    public List<Map.Entry<String, Integer>> 获取物品列表() {
        return 物品列表 == null ? List.of() : List.copyOf(物品列表.entrySet());
    }
}
