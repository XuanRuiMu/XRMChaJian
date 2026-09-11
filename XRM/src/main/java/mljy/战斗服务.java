package mljy;

import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;

public interface 战斗服务 {
    伤害结果 应用伤害(伤害上下文 上下文);
}
