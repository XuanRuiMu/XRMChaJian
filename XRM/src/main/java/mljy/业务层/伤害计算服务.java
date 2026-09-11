package mljy.业务层;

import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;

public interface 伤害计算服务 {
    伤害结果 计算(伤害上下文 上下文);
}
