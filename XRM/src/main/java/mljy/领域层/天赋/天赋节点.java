package mljy.领域层.天赋;

import mljy.领域层.属性.修饰器;

import java.util.List;

public record 天赋节点(
        String 天赋标识,
        String 专精,
        天赋类型 类型,
        int 坐标X,
        int 坐标Y,
        List<String> 前置天赋列表,
        String 选择组,
        int 所需总点数,
        List<String> 影响技能,
        List<修饰器> 修饰器列表,
        String 名称翻译键,
        String 描述翻译键
) {
    private static final int 槽位列数 = 9;

    public static 天赋节点 of(String 天赋标识,
                              String 专精,
                              天赋类型 类型,
                              int 坐标X,
                              int 坐标Y,
                              List<String> 前置天赋列表,
                              String 选择组,
                              int 所需总点数,
                              List<String> 影响技能,
                              List<修饰器> 修饰器列表,
                              String 名称翻译键,
                              String 描述翻译键) {
        return new 天赋节点(
                天赋标识,
                专精,
                类型,
                坐标X,
                坐标Y,
                List.copyOf(前置天赋列表),
                选择组 == null ? "" : 选择组,
                所需总点数,
                List.copyOf(影响技能),
                List.copyOf(修饰器列表),
                名称翻译键,
                描述翻译键
        );
    }

    public int 获取槽位() {
        return 坐标Y * 槽位列数 + 坐标X;
    }

    public boolean 有选择组() {
        return 选择组 != null && !选择组.isBlank();
    }

    public boolean 有前置() {
        return 前置天赋列表 != null && !前置天赋列表.isEmpty();
    }

    public boolean 有影响技能() {
        return 影响技能 != null && !影响技能.isEmpty();
    }

    public boolean 有修饰器() {
        return 修饰器列表 != null && !修饰器列表.isEmpty();
    }
}
