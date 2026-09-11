package mljy.业务层;

import mljy.领域层.玩家.玩家快照;

import java.util.UUID;

public interface 消息服务 {
    void 发送消息(玩家快照 玩家, String 键, Object... 参数);

    void 发送消息(UUID 玩家标识, String 键, Object... 参数);

    void 发送技能日志(玩家快照 玩家, String 键, Object... 参数);

    default void 发送技能日志(玩家快照 玩家, UUID 事件标识, String 键, Object... 参数) {
        发送技能日志(玩家, 键, 参数);
    }

    void 发送战斗日志(玩家快照 玩家, String 键, Object... 参数);

    default void 发送战斗日志(玩家快照 玩家, UUID 事件标识, String 键, Object... 参数) {
        发送战斗日志(玩家, 键, 参数);
    }

    /**
     * 发送战斗日志，指定优先级。
     * 优先级：0=战斗状态变化，10=普通攻击/伤害（默认），20=技能释放公告。
     */
    void 发送战斗日志(玩家快照 玩家, int 优先级, String 键, Object... 参数);

    default void 发送战斗日志(玩家快照 玩家, UUID 事件标识, int 优先级, String 键, Object... 参数) {
        发送战斗日志(玩家, 优先级, 键, 参数);
    }

    void 发送错误(玩家快照 玩家, String 键, Object... 参数);
}
