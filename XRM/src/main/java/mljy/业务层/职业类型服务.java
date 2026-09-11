package mljy.业务层;

import mljy.领域层.战斗.角色类型;

import java.util.Optional;

/**
 * 职业类型服务接口。
 * 根据专精名称查询对应的角色类型（坦克/治疗/输出）。
 * 角色类型用于仇恨系数计算（坦克3倍/治疗0.5倍/输出1倍）。
 */
public interface 职业类型服务 {
    Optional<角色类型> 获取角色类型(String 专精名);

    角色类型 获取角色类型或默认(String 专精名, 角色类型 默认类型);
}
