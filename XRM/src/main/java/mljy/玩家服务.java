package mljy;

import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;

import java.util.Optional;
import java.util.UUID;

public interface 玩家服务 {
    Optional<玩家快照> 获取快照(UUID 玩家标识);

    Optional<玩家会话> 获取会话(UUID 玩家标识);

    void 玩家登录(UUID 玩家标识, String 名称);

    void 玩家退出(UUID 玩家标识);

    /**
     * 获取服务器默认技能日志开关值。
     * 当玩家会话不存在或尚未加载时，消息服务应使用此默认值决定是否输出技能日志。
     */
    boolean 获取默认技能日志开关();

    /**
     * 获取服务器默认战斗日志开关值。
     * 当玩家会话不存在或尚未加载时，消息服务应使用此默认值决定是否输出战斗日志。
     */
    boolean 获取默认战斗日志开关();
}
