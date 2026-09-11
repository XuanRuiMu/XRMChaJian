package mljy.领域层.技能;

public record 技能定义(
        String 技能标识,
        String 名称翻译键,
        施法类型 施法类型,
        double 蓄力时间,
        double 引导时间,
        double 冷却时间,
        double 公共冷却,
        String 资源标识,
        double 资源消耗,
        double 射程,
        double 宽度,
        int 最大充能数,
        boolean 移动可打断,
        boolean 受伤害可打断,
        boolean 必须保持移动
) {
}
