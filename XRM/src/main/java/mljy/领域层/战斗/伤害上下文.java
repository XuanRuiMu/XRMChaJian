package mljy.领域层.战斗;

import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.修饰器;

import java.util.List;
import java.util.UUID;

public record 伤害上下文(
        玩家快照 施法者,
        Object 目标,
        double 基础数值,
        boolean 是否法术伤害,
        boolean 是否暴击,
        double 暴击倍率,
        List<修饰器> 修饰器列表,
        UUID 事件标识
) {
    public 伤害上下文(玩家快照 施法者, Object 目标, double 基础数值, boolean 是否法术伤害,
                    boolean 是否暴击, double 暴击倍率, List<修饰器> 修饰器列表) {
        this(施法者, 目标, 基础数值, 是否法术伤害, 是否暴击, 暴击倍率, 修饰器列表, null);
    }
}
