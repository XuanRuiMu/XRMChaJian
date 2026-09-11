package mljy.领域层.天赋;

public record 天赋操作结果(boolean 是否成功, String 消息键, Object[] 参数) {

    public static 天赋操作结果 成功() {
        return new 天赋操作结果(true, "", new Object[0]);
    }

    public static 天赋操作结果 成功(String 消息键, Object... 参数) {
        return new 天赋操作结果(true, 消息键, 参数);
    }

    public static 天赋操作结果 失败(String 消息键, Object... 参数) {
        return new 天赋操作结果(false, 消息键, 参数);
    }

    public boolean 有消息() {
        return 消息键 != null && !消息键.isBlank();
    }
}
