package mljy.业务层;

import java.util.UUID;
import java.util.function.BiConsumer;

public interface 资源变更服务 {
    double 获取当前值(UUID 玩家标识, String 资源标识);

    double 获取上限(UUID 玩家标识, String 资源标识);

    double 获取每秒恢复(UUID 玩家标识, String 资源标识);

    boolean 增加(UUID 玩家标识, String 资源标识, double 变化量);

    void 消耗(UUID 玩家标识, String 资源标识, double 变化量);

    void 设置上限(UUID 玩家标识, String 资源标识, double 上限);

    void 清空(UUID 玩家标识, String 资源标识);

    void 恢复(UUID 玩家标识, String 资源标识);

    void 设置资源变更回调(BiConsumer<UUID, String> 回调);
}
