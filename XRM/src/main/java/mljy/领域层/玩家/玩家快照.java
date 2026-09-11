package mljy.领域层.玩家;

import mljy.领域层.战斗.战斗状态;
import mljy.领域层.属性.属性快照;
import mljy.领域层.资源.资源;

import java.util.Map;
import java.util.UUID;

public record 玩家快照(
        UUID 唯一标识,
        String 名称,
        int 等级,
        位置 位置,
        属性快照 属性,
        Map<String, 资源> 资源表,
        战斗状态 战斗状态
) {
}
