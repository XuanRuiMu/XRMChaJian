package mljy.领域层.组队;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 队伍领域对象。
 * 队伍ID = 队长UUID（队长变更时队伍ID同步变更）。
 * 包含队长、成员列表、邀请列表、准备状态。
 */
public class 队伍 {
    private UUID 队长标识;
    private final Set<UUID> 成员标识集合;
    private final Map<UUID, Long> 邀请过期时间表;
    private final Map<UUID, Boolean> 准备状态表;
    private boolean 就位检查进行中;

    public 队伍(UUID 队长标识) {
        this.队长标识 = 队长标识;
        this.成员标识集合 = new HashSet<>();
        this.邀请过期时间表 = new HashMap<>();
        this.准备状态表 = new HashMap<>();
        this.就位检查进行中 = false;
        this.成员标识集合.add(队长标识);
    }

    public UUID 获取队长标识() {
        return 队长标识;
    }

    public void 设置队长标识(UUID 队长标识) {
        this.队长标识 = 队长标识;
    }

    public UUID 获取队伍标识() {
        return 队长标识;
    }

    public Set<UUID> 获取成员标识集合() {
        return Collections.unmodifiableSet(new HashSet<>(成员标识集合));
    }

    public boolean 包含成员(UUID 玩家标识) {
        return 成员标识集合.contains(玩家标识);
    }

    public int 获取成员数量() {
        return 成员标识集合.size();
    }

    public boolean 添加成员(UUID 玩家标识) {
        boolean 已添加 = 成员标识集合.add(玩家标识);
        if (已添加) {
            准备状态表.put(玩家标识, false);
        }
        return 已添加;
    }

    public boolean 移除成员(UUID 玩家标识) {
        boolean 已移除 = 成员标识集合.remove(玩家标识);
        if (已移除) {
            准备状态表.remove(玩家标识);
        }
        return 已移除;
    }

    public Map<UUID, Long> 获取邀请过期时间表() {
        return Collections.unmodifiableMap(new HashMap<>(邀请过期时间表));
    }

    public boolean 包含邀请(UUID 玩家标识) {
        return 邀请过期时间表.containsKey(玩家标识);
    }

    public void 添加邀请(UUID 玩家标识, long 过期时间戳) {
        邀请过期时间表.put(玩家标识, 过期时间戳);
    }

    public boolean 移除邀请(UUID 玩家标识) {
        return 邀请过期时间表.remove(玩家标识) != null;
    }

    public void 清除所有邀请() {
        邀请过期时间表.clear();
    }

    public boolean 是否就位检查进行中() {
        return 就位检查进行中;
    }

    public void 设置就位检查进行中(boolean 进行中) {
        this.就位检查进行中 = 进行中;
        if (进行中) {
            for (UUID 成员标识 : 成员标识集合) {
                准备状态表.put(成员标识, false);
            }
        }
    }

    public Map<UUID, Boolean> 获取准备状态表() {
        return Collections.unmodifiableMap(new HashMap<>(准备状态表));
    }

    public boolean 设置准备状态(UUID 玩家标识, boolean 已准备) {
        if (!成员标识集合.contains(玩家标识)) {
            return false;
        }
        准备状态表.put(玩家标识, 已准备);
        return true;
    }

    public boolean 获取准备状态(UUID 玩家标识) {
        return 准备状态表.getOrDefault(玩家标识, false);
    }

    public boolean 是否全员已准备() {
        for (UUID 成员标识 : 成员标识集合) {
            if (!准备状态表.getOrDefault(成员标识, false)) {
                return false;
            }
        }
        return true;
    }

    public void 清除准备状态() {
        就位检查进行中 = false;
        for (UUID 成员标识 : 成员标识集合) {
            准备状态表.put(成员标识, false);
        }
    }
}
