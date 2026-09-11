package mljy;

import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;

public interface 技能执行器 {
    技能执行结果 执行(技能上下文 上下文);

    default void 声明参数() {
    }
}
