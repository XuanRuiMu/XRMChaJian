package mljy.领域层.天赋;

public record 天赋定义(
        String 天赋标识,
        String 名称翻译键,
        String 描述翻译键,
        int 所需点数,
        String[] 前置天赋,
        String 影响技能标识
) {
}
