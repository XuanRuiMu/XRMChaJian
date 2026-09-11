package mljy.业务层;

import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;

import java.util.UUID;

public interface 技能释放服务 {
    技能执行结果 释放(技能上下文 上下文);

    void 抑制通用释放日志(UUID 玩家标识, String 技能标识);
}
