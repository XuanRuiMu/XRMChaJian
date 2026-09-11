package mljy.表现层.菜单;

import java.util.List;
import java.util.Map;

public record 菜单按钮(
        int 槽位,
        String 物品标识,
        String 显示名,
        List<String> 描述,
        String 权限,
        List<String> 条件,
        Map<String, 菜单动作> 动作,
        boolean 附魔光效
) {
    public static 菜单按钮 of(int 槽位, String 物品标识, String 显示名) {
        return new 菜单按钮(槽位, 物品标识, 显示名, List.of(), "", List.of(), Map.of(), false);
    }

    public 菜单按钮 with描述(List<String> 描述) {
        return new 菜单按钮(槽位, 物品标识, 显示名, 描述, 权限, 条件, 动作, 附魔光效);
    }

    public 菜单按钮 with权限(String 权限) {
        return new 菜单按钮(槽位, 物品标识, 显示名, 描述, 权限, 条件, 动作, 附魔光效);
    }

    public 菜单按钮 with条件(List<String> 条件) {
        return new 菜单按钮(槽位, 物品标识, 显示名, 描述, 权限, 条件, 动作, 附魔光效);
    }

    public 菜单按钮 with动作(Map<String, 菜单动作> 动作) {
        return new 菜单按钮(槽位, 物品标识, 显示名, 描述, 权限, 条件, 动作, 附魔光效);
    }

    public 菜单按钮 with附魔光效(boolean 附魔光效) {
        return new 菜单按钮(槽位, 物品标识, 显示名, 描述, 权限, 条件, 动作, 附魔光效);
    }
}
