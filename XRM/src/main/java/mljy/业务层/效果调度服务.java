package mljy.业务层;

import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数读取器;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface 效果调度服务 {
    void 添加(UUID 玩家标识, 效果实例 效果);

    void 添加效果带覆盖(UUID 玩家标识, String 效果标识, String 来源技能标识, int 层数, long 持续时间毫秒, 参数读取器 覆盖);

    void 移除(UUID 玩家标识, String 效果标识);

    Optional<效果实例> 获取(UUID 玩家标识, String 效果标识);

    List<效果实例> 获取列表(UUID 玩家标识);

    boolean 拥有(UUID 玩家标识, String 效果标识);

    void 增加层数(UUID 玩家标识, String 效果标识, int 增量);

    void 设置层数(UUID 玩家标识, String 效果标识, int 层数);

    void 刷新(UUID 玩家标识, long 当前时间);

    void 清空(UUID 玩家标识);

    void 清空(UUID 玩家标识, String 清理原因);

    void 清空全部();

    void 清空全部(String 清理原因);
}
