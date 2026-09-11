package mljy;

import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;

import java.util.Optional;
import java.util.Set;

public interface 技能服务 {
    void 释放(技能上下文 上下文);

    Optional<技能定义> 获取定义(String 技能标识);

    Optional<技能执行器> 获取执行器(String 技能标识);

    Set<String> 获取所有技能标识();
}
