package mljy.业务层;

import mljy.领域层.实体;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;

import java.util.List;
import java.util.function.Predicate;

public interface 目标选择服务 {
    List<实体> 选择敌人(玩家快照 施法者, 位置 中心, double 半径);

    List<实体> 选择友方(玩家快照 施法者, 位置 中心, double 半径);

    实体 选择最近敌人(玩家快照 施法者, 位置 中心, double 半径);

    List<实体> 选择范围内(位置 中心, double 半径, Predicate<实体> 过滤条件);
}
