package mljy.领域层.效果;

import mljy.领域层.技能.参数读取器;

import java.util.UUID;

public class 效果实例 {
    private final String 效果标识;
    private final String 来源技能标识;
    private final UUID 施法者标识;
    private final long 创建时间;
    private final 参数读取器 参数覆盖;
    private int 层数;
    private long 到期时间;

    public 效果实例(String 效果标识, String 来源技能标识, int 层数, long 到期时间) {
        this(效果标识, 来源技能标识, null, 层数, 到期时间, System.currentTimeMillis());
    }

    public 效果实例(String 效果标识, String 来源技能标识, UUID 施法者标识, int 层数, long 到期时间) {
        this(效果标识, 来源技能标识, 施法者标识, 层数, 到期时间, System.currentTimeMillis());
    }

    public 效果实例(String 效果标识, String 来源技能标识, UUID 施法者标识, int 层数, long 到期时间, long 创建时间) {
        this(效果标识, 来源技能标识, 施法者标识, 层数, 到期时间, 创建时间, new 参数读取器());
    }

    public 效果实例(String 效果标识, String 来源技能标识, UUID 施法者标识, int 层数, long 到期时间, 参数读取器 参数覆盖) {
        this(效果标识, 来源技能标识, 施法者标识, 层数, 到期时间, System.currentTimeMillis(), 参数覆盖);
    }

    public 效果实例(String 效果标识, String 来源技能标识, UUID 施法者标识, int 层数, long 到期时间, long 创建时间, 参数读取器 参数覆盖) {
        this.效果标识 = 效果标识;
        this.来源技能标识 = 来源技能标识;
        this.施法者标识 = 施法者标识;
        this.层数 = 层数;
        this.到期时间 = 到期时间;
        this.创建时间 = 创建时间;
        this.参数覆盖 = 参数覆盖 == null ? new 参数读取器() : 参数覆盖;
    }

    public String 获取效果标识() {
        return 效果标识;
    }

    public String 获取来源技能标识() {
        return 来源技能标识;
    }

    public UUID 获取施法者标识() {
        return 施法者标识;
    }

    public int 获取层数() {
        return 层数;
    }

    public void 设置层数(int 层数) {
        this.层数 = 层数;
    }

    public long 获取到期时间() {
        return 到期时间;
    }

    public void 设置到期时间(long 到期时间) {
        this.到期时间 = 到期时间;
    }

    public long 获取创建时间() {
        return 创建时间;
    }

    public 参数读取器 获取参数覆盖() {
        return 参数覆盖;
    }

    public boolean 是否到期(long 当前时间) {
        return 到期时间 >= 0 && 当前时间 >= 到期时间;
    }

    public boolean 是否永久() {
        return 到期时间 < 0;
    }
}
