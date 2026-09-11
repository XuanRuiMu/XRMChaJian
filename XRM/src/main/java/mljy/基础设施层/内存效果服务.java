package mljy.基础设施层;

import mljy.效果服务;
import mljy.领域层.效果.效果实例;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class 内存效果服务 implements 效果服务 {
    private final Map<UUID, List<效果实例>> 效果表 = new ConcurrentHashMap<>();

    @Override
    public void 添加效果(UUID 玩家标识, 效果实例 效果) {
        效果表.computeIfAbsent(玩家标识, 键 -> new CopyOnWriteArrayList<>()).add(效果);
    }

    @Override
    public void 移除效果(UUID 玩家标识, String 效果标识) {
        List<效果实例> 列表 = 效果表.get(玩家标识);
        if (列表 == null) {
            return;
        }
        列表.removeIf(效果 -> 效果.获取效果标识().equals(效果标识));
    }

    @Override
    public Optional<效果实例> 查找效果(UUID 玩家标识, String 效果标识) {
        List<效果实例> 列表 = 效果表.get(玩家标识);
        if (列表 == null) {
            return Optional.empty();
        }
        for (效果实例 效果 : 列表) {
            if (效果.获取效果标识().equals(效果标识)) {
                return Optional.of(效果);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<效果实例> 获取效果列表(UUID 玩家标识) {
        return new ArrayList<>(效果表.getOrDefault(玩家标识, List.of()));
    }

    @Override
    public List<UUID> 获取所有玩家标识() {
        return new ArrayList<>(效果表.keySet());
    }

    @Override
    public boolean 拥有效果(UUID 玩家标识, String 效果标识) {
        return 查找效果(玩家标识, 效果标识).isPresent();
    }

    @Override
    public void 更新效果(UUID 玩家标识, 效果实例 效果) {
        List<效果实例> 列表 = 效果表.get(玩家标识);
        if (列表 == null) {
            return;
        }
        for (int 索引 = 0; 索引 < 列表.size(); 索引++) {
            效果实例 现有 = 列表.get(索引);
            if (现有.获取效果标识().equals(效果.获取效果标识())) {
                列表.set(索引, 效果);
                return;
            }
        }
    }
}
