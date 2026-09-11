package mljy.领域层.属性;

import java.util.HashMap;
import java.util.Map;

/**
 * 属性快照。
 * 升级要点（FP-01）：
 * 1. 字段重命名：暴击几率 → 法术暴击几率，暴击伤害 → 法术暴击伤害。
 * 2. 新增扩展属性 Map，支持反伤等动态属性（可插入性）。
 * 3. 新增静态工厂 创建(...)，与 17 个标准字段顺序一致，便于测试与调用方构造。
 * 4. 新增方法 带扩展属性(String,double) 返回带扩展属性的新实例（不可变拷贝）。
 * 5. 新增方法 获取属性值(String 修饰器类别) 按类别查询标准或扩展属性。
 * 6. 新增方法 获取扩展属性(String 名) 查询扩展属性，未注册返回 0。
 */
public record 属性快照(
        double 生命值上限,
        double 公共冷却时间,
        double 急速,
        double 力量,
        double 敏捷,
        double 智力,
        double 基础力量,
        double 基础敏捷,
        double 基础智力,
        double 法术暴击几率,
        double 法术暴击伤害,
        double 精通,
        double 全能,
        double 吸血,
        double 躲闪,
        double 生命恢复,
        double 移速,
        Map<String, Double> 扩展属性
) {
    public 属性快照 {
        扩展属性 = 扩展属性 == null ? Map.of() : Map.copyOf(扩展属性);
    }

    public static 属性快照 创建(
            double 生命值上限,
            double 公共冷却时间,
            double 急速,
            double 力量,
            double 敏捷,
            double 智力,
            double 基础力量,
            double 基础敏捷,
            double 基础智力,
            double 法术暴击几率,
            double 法术暴击伤害,
            double 精通,
            double 全能,
            double 吸血,
            double 躲闪,
            double 生命恢复,
            double 移速
    ) {
        return new 属性快照(
                生命值上限, 公共冷却时间, 急速,
                力量, 敏捷, 智力,
                基础力量, 基础敏捷, 基础智力,
                法术暴击几率, 法术暴击伤害,
                精通, 全能, 吸血, 躲闪,
                生命恢复, 移速, Map.of()
        );
    }

    public 属性快照 带扩展属性(String 名, double 值) {
        Map<String, Double> 新扩展 = new HashMap<>(扩展属性);
        新扩展.put(名, 值);
        return new 属性快照(
                生命值上限, 公共冷却时间, 急速,
                力量, 敏捷, 智力,
                基础力量, 基础敏捷, 基础智力,
                法术暴击几率, 法术暴击伤害,
                精通, 全能, 吸血, 躲闪,
                生命恢复, 移速, 新扩展
        );
    }

    public double 获取属性值(String 修饰器类别) {
        return switch (修饰器类别) {
            case "生命值上限" -> 生命值上限;
            case "公共冷却时间" -> 公共冷却时间;
            case "急速" -> 急速;
            case "力量" -> 力量;
            case "敏捷" -> 敏捷;
            case "智力" -> 智力;
            case "基础力量" -> 基础力量;
            case "基础敏捷" -> 基础敏捷;
            case "基础智力" -> 基础智力;
            case "法术暴击几率" -> 法术暴击几率;
            case "法术暴击伤害" -> 法术暴击伤害;
            case "精通" -> 精通;
            case "全能" -> 全能;
            case "吸血" -> 吸血;
            case "躲闪" -> 躲闪;
            case "生命恢复" -> 生命恢复;
            case "移速" -> 移速;
            default -> 获取扩展属性(修饰器类别);
        };
    }

    public double 获取扩展属性(String 名) {
        return 扩展属性.getOrDefault(名, 0.0);
    }
}
