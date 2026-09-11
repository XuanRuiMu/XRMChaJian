package mljy.表现层.菜单;

import java.util.List;

public record 菜单配置(
        String 标题,
        int 大小,
        String 权限,
        List<String> 条件,
        List<菜单按钮> 按钮
) {
    public static 菜单配置 of(String 标题, int 大小) {
        return new 菜单配置(标题, 大小, "", List.of(), List.of());
    }

    public 菜单配置 with权限(String 权限) {
        return new 菜单配置(标题, 大小, 权限, 条件, 按钮);
    }

    public 菜单配置 with条件(List<String> 条件) {
        return new 菜单配置(标题, 大小, 权限, 条件, 按钮);
    }

    public 菜单配置 with按钮(List<菜单按钮> 按钮) {
        return new 菜单配置(标题, 大小, 权限, 条件, 按钮);
    }
}
