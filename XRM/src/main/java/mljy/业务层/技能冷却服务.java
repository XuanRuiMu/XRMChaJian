package mljy.业务层;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface 技能冷却服务 {
    boolean 是否冷却中(UUID 玩家标识, String 技能标识);

    double 获取剩余冷却(UUID 玩家标识, String 技能标识);

    void 开始冷却(UUID 玩家标识, String 技能标识, double 冷却时间);

    void 开始公共冷却(UUID 玩家标识, double 公共冷却时间);

    boolean 是否公共冷却中(UUID 玩家标识);

    double 获取公共冷却剩余(UUID 玩家标识);

    void 设置独立CD开始回调(BiConsumer<UUID, String> 回调);

    void 设置独立CD完成回调(BiConsumer<UUID, String> 回调);

    void 设置公CD开始回调(Consumer<UUID> 回调);

    void 设置公CD完成回调(Consumer<UUID> 回调);

    void 清理玩家(UUID 玩家标识);

    void 重置冷却(UUID 玩家标识, String 技能标识);

    void 重置公共冷却(UUID 玩家标识);

    void 重置所有冷却(UUID 玩家标识);
}
