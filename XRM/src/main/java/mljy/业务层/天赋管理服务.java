package mljy.业务层;

import mljy.领域层.天赋.天赋操作结果;

import java.util.Set;
import java.util.UUID;

public interface 天赋管理服务 {

    天赋操作结果 学习天赋(UUID 玩家标识, String 天赋标识);

    天赋操作结果 取消天赋(UUID 玩家标识, String 天赋标识);

    天赋操作结果 重置天赋(UUID 玩家标识);

    Set<String> 获取已学天赋(UUID 玩家标识);

    int 获取已用点数(UUID 玩家标识);

    int 获取最大点数(UUID 玩家标识);
}
