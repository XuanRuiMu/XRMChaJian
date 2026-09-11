package 暮澜纪元.职业;

/**
 * 角色类型枚举。
 * 定义职业的角色定位类型。
 */
public enum 角色类型 {
    输出("角色类型.输出"),
    治疗("角色类型.治疗"),
    坦克("角色类型.坦克");

    private final String 语言键;

    角色类型(String 语言键) {
        this.语言键 = 语言键;
    }

    public String 获取语言键() {
        return this.语言键;
    }
}
