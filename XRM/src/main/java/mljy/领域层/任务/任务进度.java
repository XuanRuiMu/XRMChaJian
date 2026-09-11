package mljy.领域层.任务;

import java.util.UUID;

/**
 * 玩家任务进度领域对象。
 * 跟踪玩家在某个任务上的当前状态和进度数值。
 * 状态生命周期：未解锁 → 可接取 → 进行中 → 可交付 → 已完成
 * 对应需求文档《任务.md》1.2 章节任务状态生命周期。
 */
public class 任务进度 {
    private final UUID 玩家标识;
    private final String 任务标识;
    private 任务状态 状态;
    private int 当前进度;
    private int 目标进度;
    private long 接取时间;
    private long 完成时间;
    private long 上次重置时间;

    public 任务进度(UUID 玩家标识, String 任务标识, int 目标进度) {
        this.玩家标识 = 玩家标识;
        this.任务标识 = 任务标识;
        this.状态 = 任务状态.未解锁;
        this.当前进度 = 0;
        this.目标进度 = 目标进度;
        this.接取时间 = 0L;
        this.完成时间 = 0L;
        this.上次重置时间 = 0L;
    }

    public 任务进度(UUID 玩家标识, String 任务标识, 任务状态 状态,
                   int 当前进度, int 目标进度,
                   long 接取时间, long 完成时间, long 上次重置时间) {
        this.玩家标识 = 玩家标识;
        this.任务标识 = 任务标识;
        this.状态 = 状态;
        this.当前进度 = 当前进度;
        this.目标进度 = 目标进度;
        this.接取时间 = 接取时间;
        this.完成时间 = 完成时间;
        this.上次重置时间 = 上次重置时间;
    }

    public UUID 获取玩家标识() {
        return 玩家标识;
    }

    public String 获取任务标识() {
        return 任务标识;
    }

    public 任务状态 获取状态() {
        return 状态;
    }

    public void 设置状态(任务状态 状态) {
        this.状态 = 状态;
    }

    public int 获取当前进度() {
        return 当前进度;
    }

    public void 设置当前进度(int 当前进度) {
        this.当前进度 = Math.max(0, 当前进度);
    }

    public int 获取目标进度() {
        return 目标进度;
    }

    public void 设置目标进度(int 目标进度) {
        this.目标进度 = 目标进度;
    }

    public long 获取接取时间() {
        return 接取时间;
    }

    public void 设置接取时间(long 接取时间) {
        this.接取时间 = 接取时间;
    }

    public long 获取完成时间() {
        return 完成时间;
    }

    public void 设置完成时间(long 完成时间) {
        this.完成时间 = 完成时间;
    }

    public long 获取上次重置时间() {
        return 上次重置时间;
    }

    public void 设置上次重置时间(long 上次重置时间) {
        this.上次重置时间 = 上次重置时间;
    }

    /**
     * 接取任务：状态从"可接取"变为"进行中"。
     */
    public void 接取(long 当前时间) {
        if (状态 == 任务状态.可接取 || 状态 == 任务状态.未解锁) {
            状态 = 任务状态.进行中;
            接取时间 = 当前时间;
            当前进度 = 0;
        }
    }

    /**
     * 增加进度。当进度达到目标时，状态自动从"进行中"变为"可交付"。
     */
    public void 增加进度(int 增量) {
        if (状态 != 任务状态.进行中) {
            return;
        }
        当前进度 += 增量;
        if (当前进度 >= 目标进度) {
            当前进度 = 目标进度;
            状态 = 任务状态.可交付;
        }
    }

    /**
     * 设置进度。当进度达到目标时，状态自动从"进行中"变为"可交付"。
     */
    public void 更新进度(int 新进度) {
        if (状态 != 任务状态.进行中) {
            return;
        }
        当前进度 = Math.max(0, 新进度);
        if (当前进度 >= 目标进度) {
            当前进度 = 目标进度;
            状态 = 任务状态.可交付;
        }
    }

    /**
     * 完成任务：状态从"可交付"变为"已完成"。
     */
    public void 完成(long 当前时间) {
        if (状态 == 任务状态.可交付) {
            状态 = 任务状态.已完成;
            完成时间 = 当前时间;
        }
    }

    /**
     * 重置任务进度（用于日常任务每日重置或管理员强制重置）。
     */
    public void 重置(long 当前时间) {
        状态 = 任务状态.可接取;
        当前进度 = 0;
        接取时间 = 0L;
        完成时间 = 0L;
        上次重置时间 = 当前时间;
    }

    /**
     * 强制重置到未解锁状态（管理员重置）。
     */
    public void 强制重置() {
        状态 = 任务状态.未解锁;
        当前进度 = 0;
        接取时间 = 0L;
        完成时间 = 0L;
    }

    public boolean 是否未解锁() {
        return 状态 == 任务状态.未解锁;
    }

    public boolean 是否可接取() {
        return 状态 == 任务状态.可接取;
    }

    public boolean 是否进行中() {
        return 状态 == 任务状态.进行中;
    }

    public boolean 是否可交付() {
        return 状态 == 任务状态.可交付;
    }

    public boolean 是否已完成() {
        return 状态 == 任务状态.已完成;
    }

    /**
     * 获取进度百分比（0.0 ~ 1.0）。
     */
    public double 获取进度百分比() {
        if (目标进度 <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) 当前进度 / 目标进度);
    }
}
