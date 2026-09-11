package mljy.表现层.菜单;

public record 菜单动作(菜单动作类型 类型, String 参数) {
    public static 菜单动作 打开菜单(String 菜单标识) {
        return new 菜单动作(菜单动作类型.打开菜单, 菜单标识);
    }

    public static 菜单动作 执行命令(String 命令) {
        return new 菜单动作(菜单动作类型.执行命令, 命令);
    }

    public static 菜单动作 发送消息(String 消息键) {
        return new 菜单动作(菜单动作类型.发送消息, 消息键);
    }

    public static 菜单动作 关闭菜单() {
        return new 菜单动作(菜单动作类型.关闭菜单, "");
    }
}
