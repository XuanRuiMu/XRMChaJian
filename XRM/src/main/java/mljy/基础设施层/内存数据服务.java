package mljy.基础设施层;

import mljy.数据服务;
import mljy.领域层.玩家.玩家会话;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 内存数据服务 implements 数据服务 {
    private final Map<UUID, 玩家会话> 会话缓存 = new ConcurrentHashMap<>();

    @Override
    public void 保存(玩家会话 会话) {
        会话缓存.put(会话.获取玩家标识(), 会话);
    }

    @Override
    public Optional<玩家会话> 加载(UUID 玩家标识) {
        return Optional.ofNullable(会话缓存.get(玩家标识));
    }

    @Override
    public boolean 存在(UUID 玩家标识) {
        return 会话缓存.containsKey(玩家标识);
    }
}
