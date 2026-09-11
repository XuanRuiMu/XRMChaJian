package mljy.领域层.战斗;

public record 伤害结果(
        double 最终数值,
        boolean 是否暴击,
        boolean 被吸收
) {
}
