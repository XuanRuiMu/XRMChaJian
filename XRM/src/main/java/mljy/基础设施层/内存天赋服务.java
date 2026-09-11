package mljy.基础设施层;

import mljy.天赋服务;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 内存天赋服务 implements 天赋服务 {
    private final Map<UUID, Set<String>> 天赋表 = new ConcurrentHashMap<>();

    @Override
    public Set<String> 获取已选天赋(UUID 玩家标识) {
        return Collections.unmodifiableSet(天赋表.getOrDefault(玩家标识, Collections.newSetFromMap(new ConcurrentHashMap<>())));
    }

    @Override
    public void 选择天赋(UUID 玩家标识, String 天赋标识) {
        天赋表.computeIfAbsent(玩家标识, 键 -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(天赋标识);
    }

    @Override
    public void 取消天赋(UUID 玩家标识, String 天赋标识) {
        Set<String> 天赋集 = 天赋表.get(玩家标识);
        if (天赋集 != null) {
            天赋集.remove(天赋标识);
        }
    }
}
