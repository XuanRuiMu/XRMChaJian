package mljy.领域层.乐器;

import java.util.UUID;

/**
 * FP-18 公共乐谱领域对象。
 * 玩家上传的乐谱经审核通过后进入公共乐谱库，所有玩家可下载/评分。
 * <p>
 * 审核状态：
 * - 待审核：上传后默认状态，等待管理员审核
 * - 已通过：审核通过，进入公共库可被下载/评分
 * - 已拒绝：审核未通过，不入公共库
 * <p>
 * 评分聚合：累计评分总和 / 评分人数 = 平均评分。
 */
public class 公共乐谱 {
    private final String 乐谱ID;
    private final 乐谱 原乐谱;
    private final UUID 上传者标识;
    private final String 上传者名;
    private final long 上传时间戳;
    private 审核状态 状态;
    private long 评分总和;
    private long 评分人数;
    private long 下载次数;

    public 公共乐谱(String 乐谱ID, 乐谱 原乐谱, UUID 上传者标识, String 上传者名,
                 long 上传时间戳, 审核状态 状态, long 评分总和, long 评分人数, long 下载次数) {
        this.乐谱ID = 乐谱ID;
        this.原乐谱 = 原乐谱;
        this.上传者标识 = 上传者标识;
        this.上传者名 = 上传者名;
        this.上传时间戳 = 上传时间戳;
        this.状态 = 状态;
        this.评分总和 = 评分总和;
        this.评分人数 = 评分人数;
        this.下载次数 = 下载次数;
    }

    public static 公共乐谱 创建(String 乐谱ID, 乐谱 原乐谱, UUID 上传者标识, String 上传者名) {
        return new 公共乐谱(乐谱ID, 原乐谱, 上传者标识, 上传者名,
                System.currentTimeMillis(), 审核状态.待审核, 0L, 0L, 0L);
    }

    public String 获取乐谱ID() {
        return 乐谱ID;
    }

    public 乐谱 获取原乐谱() {
        return 原乐谱;
    }

    public UUID 获取上传者标识() {
        return 上传者标识;
    }

    public String 获取上传者名() {
        return 上传者名;
    }

    public long 获取上传时间戳() {
        return 上传时间戳;
    }

    public 审核状态 获取状态() {
        return 状态;
    }

    public void 设置状态(审核状态 状态) {
        this.状态 = 状态;
    }

    public long 获取评分总和() {
        return 评分总和;
    }

    public long 获取评分人数() {
        return 评分人数;
    }

    public long 获取下载次数() {
        return 下载次数;
    }

    /**
     * 增加一次评分。评分值会被 clamp 到 [0, 5] 范围。
     * 同一玩家重复评分会被业务层拦截（业务层维护已评分玩家集合）。
     *
     * @param 评分 评分值（0-5）
     */
    public void 增加评分(int 评分) {
        int 限制 = Math.max(0, Math.min(5, 评分));
        this.评分总和 += 限制;
        this.评分人数 += 1;
    }

    /**
     * 增加一次下载计数。
     */
    public void 增加下载() {
        this.下载次数 += 1;
    }

    /**
     * 获取平均评分（0-5，无评分返回 0）。
     *
     * @return 平均评分
     */
    public double 获取平均评分() {
        if (评分人数 <= 0) {
            return 0.0;
        }
        return (double) 评分总和 / 评分人数;
    }

    /**
     * 审核状态枚举。
     */
    public enum 审核状态 {
        待审核,
        已通过,
        已拒绝
    }
}
