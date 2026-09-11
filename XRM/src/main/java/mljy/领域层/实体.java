package mljy.领域层;

import mljy.领域层.玩家.位置;

import java.util.UUID;

public interface 实体 {
    UUID 获取唯一标识();

    String 获取名称();

    位置 获取位置();

    boolean 是否存活();
}
