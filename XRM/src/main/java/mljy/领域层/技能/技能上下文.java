package mljy.领域层.技能;

import mljy.领域层.玩家.玩家快照;

public record 技能上下文(
        玩家快照 施法者,
        String 技能标识,
        技能定义 技能定义,
        Object 目标,
        long 触发时间,
        参数读取器 参数覆盖
) {
    public 技能上下文 {
        if (参数覆盖 == null) {
            参数覆盖 = new 参数读取器();
        }
    }

    public 技能上下文(玩家快照 施法者, String 技能标识, 技能定义 技能定义, Object 目标, long 触发时间) {
        this(施法者, 技能标识, 技能定义, 目标, 触发时间, new 参数读取器());
    }

    public 参数读取器 获取参数覆盖() {
        return 参数覆盖;
    }
}
