package mljy;

import java.util.Set;
import java.util.UUID;

public interface 天赋服务 {
    Set<String> 获取已选天赋(UUID 玩家标识);

    void 选择天赋(UUID 玩家标识, String 天赋标识);

    void 取消天赋(UUID 玩家标识, String 天赋标识);
}
