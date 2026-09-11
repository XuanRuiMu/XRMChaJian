package mljy.业务层;

import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;

import java.util.UUID;

public interface 属性计算服务 {
    属性快照 计算(玩家快照 玩家);

    属性快照 计算修饰后属性(UUID 玩家标识, 属性快照 基础属性);

    double 应用通用递减(double 属性值);

    double 计算实际公共冷却(double 基础公共冷却, double 急速);

    double 计算实际蓄力时间(double 基础蓄力时间, double 急速);

    double 计算派生值(String 派生名, 属性快照 属性);
}
