package mljy.领域层.效果;

import java.util.UUID;

public interface 效果处理器 {
    void on添加(UUID 目标标识, 效果实例 效果);

    void on移除(UUID 目标标识, 效果实例 效果);

    void on到期(UUID 目标标识, 效果实例 效果);

    void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数);
}
