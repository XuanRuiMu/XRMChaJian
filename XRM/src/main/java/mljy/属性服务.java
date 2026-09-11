package mljy;

import mljy.领域层.属性.属性快照;

import java.util.Optional;
import java.util.UUID;

public interface 属性服务 {
    属性快照 计算属性(UUID 玩家标识);

    属性快照 获取基础属性(UUID 玩家标识);

    void 刷新属性(UUID 玩家标识);

    Optional<属性快照> 获取专精基础属性(String 专精);

    void 设置玩家基础属性(UUID 玩家标识, 属性快照 基础属性);

    void 重载();
}
