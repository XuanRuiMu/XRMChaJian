package 暮澜纪元.通用.消息;

import java.util.UUID;

public interface 消息服务 {

    void 发送消息(UUID 玩家标识, String 键, Object... 参数);

    void 发送技能日志(UUID 玩家标识, String 键, Object... 参数);

    void 发送战斗日志(UUID 玩家标识, String 键, Object... 参数);

    void 发送错误(UUID 玩家标识, String 键, Object... 参数);
}
