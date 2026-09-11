package mljy.领域层.玩家;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class 玩家会话 {
    private static final boolean 默认技能日志开关 = true;
    private static final boolean 默认战斗日志开关 = true;
    private static final int 默认等级 = 1;
    private static final String 默认职业 = "魔法世界";
    private static final String 默认专精 = "奥能法师";

    private final UUID 玩家标识;
    private final String 名称;
    private final Set<String> 已选天赋;
    private boolean 已登录;
    private int 等级;
    private String 职业;
    private String 专精;
    private boolean 技能日志开关;
    private boolean 战斗日志开关;

    public 玩家会话(UUID 玩家标识, String 名称) {
        this.玩家标识 = 玩家标识;
        this.名称 = 名称;
        this.已选天赋 = new HashSet<>();
        this.已登录 = true;
        this.等级 = 默认等级;
        this.职业 = 默认职业;
        this.专精 = 默认专精;
        this.技能日志开关 = 默认技能日志开关;
        this.战斗日志开关 = 默认战斗日志开关;
    }

    public UUID 获取玩家标识() {
        return 玩家标识;
    }

    public String 获取名称() {
        return 名称;
    }

    public Set<String> 获取已选天赋() {
        return new HashSet<>(已选天赋);
    }

    public void 添加天赋(String 天赋标识) {
        已选天赋.add(天赋标识);
    }

    public void 移除天赋(String 天赋标识) {
        已选天赋.remove(天赋标识);
    }

    public boolean 是否已登录() {
        return 已登录;
    }

    public void 设置登录状态(boolean 已登录) {
        this.已登录 = 已登录;
    }

    public int 获取等级() {
        return 等级;
    }

    public void 设置等级(int 等级) {
        this.等级 = 等级;
    }

    public String 获取职业() {
        return 职业;
    }

    public void 设置职业(String 职业) {
        this.职业 = 职业;
    }

    public String 获取专精() {
        return 专精;
    }

    public void 设置专精(String 专精) {
        this.专精 = 专精;
    }

    public boolean 获取技能日志开关() {
        return 技能日志开关;
    }

    public void 设置技能日志开关(boolean 技能日志开关) {
        this.技能日志开关 = 技能日志开关;
    }

    public boolean 获取战斗日志开关() {
        return 战斗日志开关;
    }

    public void 设置战斗日志开关(boolean 战斗日志开关) {
        this.战斗日志开关 = 战斗日志开关;
    }
}
