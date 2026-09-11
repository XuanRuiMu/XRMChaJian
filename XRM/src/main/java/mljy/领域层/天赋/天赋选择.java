package mljy.领域层.天赋;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class 天赋选择 {
    private final UUID 玩家标识;
    private final Set<String> 已选天赋;
    private int 可用点数;

    public 天赋选择(UUID 玩家标识, int 可用点数) {
        this.玩家标识 = 玩家标识;
        this.已选天赋 = new HashSet<>();
        this.可用点数 = 可用点数;
    }

    public UUID 获取玩家标识() {
        return 玩家标识;
    }

    public Set<String> 获取已选天赋() {
        return new HashSet<>(已选天赋);
    }

    public boolean 选择天赋(String 天赋标识) {
        return 已选天赋.add(天赋标识);
    }

    public boolean 取消天赋(String 天赋标识) {
        return 已选天赋.remove(天赋标识);
    }

    public int 获取可用点数() {
        return 可用点数;
    }

    public void 设置可用点数(int 可用点数) {
        this.可用点数 = 可用点数;
    }
}
