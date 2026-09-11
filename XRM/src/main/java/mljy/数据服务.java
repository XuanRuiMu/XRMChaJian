package mljy;

import mljy.领域层.玩家.玩家会话;

import java.util.Optional;
import java.util.UUID;

public interface 数据服务 {
    void 保存(玩家会话 会话);

    Optional<玩家会话> 加载(UUID 玩家标识);

    boolean 存在(UUID 玩家标识);
}
