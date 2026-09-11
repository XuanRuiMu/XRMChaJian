package mljy;

import mljy.领域层.资源.资源;

import java.util.Optional;
import java.util.UUID;

public interface 资源服务 {
    Optional<资源> 获取资源(UUID 玩家标识, String 资源标识);

    void 变更资源(UUID 玩家标识, String 资源标识, double 变化量);

    void 设置资源上限(UUID 玩家标识, String 资源标识, double 上限);

    void 初始化玩家资源(UUID 玩家标识);
}
