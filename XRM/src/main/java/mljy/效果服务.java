package mljy;

import mljy.领域层.效果.效果实例;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface 效果服务 {
    void 添加效果(UUID 玩家标识, 效果实例 效果);

    void 移除效果(UUID 玩家标识, String 效果标识);

    Optional<效果实例> 查找效果(UUID 玩家标识, String 效果标识);

    List<效果实例> 获取效果列表(UUID 玩家标识);

    List<UUID> 获取所有玩家标识();

    boolean 拥有效果(UUID 玩家标识, String 效果标识);

    void 更新效果(UUID 玩家标识, 效果实例 效果);
}
