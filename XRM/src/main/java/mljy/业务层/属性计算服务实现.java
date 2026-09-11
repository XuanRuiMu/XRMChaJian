package mljy.业务层;

import com.google.inject.Inject;
import mljy.基础设施层.调试日志器;
import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.属性.派生属性计算器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 属性计算服务实现 implements 属性计算服务 {
    private static final double 公共冷却下限比例 = 0.5;
    private static final double 蓄力时间下限秒 = 0.2;
    private static final double 急速百分比系数 = 0.01;

    private static final double 递减第一段上限 = 20.0;
    private static final double 递减第二段上限 = 30.0;
    private static final double 递减第三段上限 = 40.0;
    private static final double 递减第一段系数 = 1.0;
    private static final double 递减第二段系数 = 0.8;
    private static final double 递减第三段系数 = 0.6;
    private static final double 递减第四段系数 = 0.4;

    private static final String 类别生命值上限 = "生命值上限";
    private static final String 类别公共冷却时间 = "公共冷却时间";
    private static final String 类别急速 = "急速";
    private static final String 类别力量 = "力量";
    private static final String 类别敏捷 = "敏捷";
    private static final String 类别智力 = "智力";
    private static final String 类别法术暴击几率 = "法术暴击几率";
    private static final String 类别法术暴击伤害 = "法术暴击伤害";
    private static final String 类别精通 = "精通";
    private static final String 类别全能 = "全能";
    private static final String 类别吸血 = "吸血";
    private static final String 类别躲闪 = "躲闪";
    private static final String 类别生命恢复 = "生命恢复";
    private static final String 类别移速 = "移速";

    private final 修饰器管理器 修饰器管理器;
    private final Map<String, 派生属性计算器> 派生计算器注册表 = new ConcurrentHashMap<>();

    @Inject
    public 属性计算服务实现(修饰器管理器 修饰器管理器) {
        this.修饰器管理器 = 修饰器管理器;
        注册内置派生计算器();
    }

    private void 注册内置派生计算器() {
        注册派生计算器(new 派生属性计算器.全能增伤比例计算器());
        注册派生计算器(new 派生属性计算器.全能减伤比例计算器());
        注册派生计算器(new 派生属性计算器.吸血比例计算器());
        注册派生计算器(new 派生属性计算器.躲闪几率计算器());
        注册派生计算器(new 派生属性计算器.暴击期望增伤计算器());
    }

    public void 注册派生计算器(派生属性计算器 计算器) {
        if (计算器 == null || 计算器.派生名() == null) {
            return;
        }
        派生计算器注册表.putIfAbsent(计算器.派生名(), 计算器);
    }

    @Override
    public 属性快照 计算(玩家快照 玩家) {
        return 玩家.属性();
    }

    @Override
    public 属性快照 计算修饰后属性(UUID 玩家标识, 属性快照 基础属性) {
        调试日志器.调试("属性计算服务", "开始计算修饰后属性：玩家=%s", 玩家标识);
        属性快照 结果 = 属性快照.创建(
                修饰器管理器.计算最终值(玩家标识, 类别生命值上限, 基础属性.生命值上限()),
                修饰器管理器.计算最终值(玩家标识, 类别公共冷却时间, 基础属性.公共冷却时间()),
                修饰器管理器.计算最终值(玩家标识, 类别急速, 基础属性.急速()),
                修饰器管理器.计算最终值(玩家标识, 类别力量, 基础属性.力量()),
                修饰器管理器.计算最终值(玩家标识, 类别敏捷, 基础属性.敏捷()),
                修饰器管理器.计算最终值(玩家标识, 类别智力, 基础属性.智力()),
                基础属性.基础力量(),
                基础属性.基础敏捷(),
                基础属性.基础智力(),
                修饰器管理器.计算最终值(玩家标识, 类别法术暴击几率, 基础属性.法术暴击几率()),
                修饰器管理器.计算最终值(玩家标识, 类别法术暴击伤害, 基础属性.法术暴击伤害()),
                修饰器管理器.计算最终值(玩家标识, 类别精通, 基础属性.精通()),
                修饰器管理器.计算最终值(玩家标识, 类别全能, 基础属性.全能()),
                修饰器管理器.计算最终值(玩家标识, 类别吸血, 基础属性.吸血()),
                修饰器管理器.计算最终值(玩家标识, 类别躲闪, 基础属性.躲闪()),
                修饰器管理器.计算最终值(玩家标识, 类别生命恢复, 基础属性.生命恢复()),
                修饰器管理器.计算最终值(玩家标识, 类别移速, 基础属性.移速())
        );
        调试日志器.调试("属性计算服务", "修饰后属性计算完成：玩家=%s 生命值上限=%.1f 急速=%.1f 公CD=%.3f",
                玩家标识, 结果.生命值上限(), 结果.急速(), 结果.公共冷却时间());
        return 结果;
    }

    @Override
    public double 应用通用递减(double 属性值) {
        if (属性值 <= 0) {
            return 属性值;
        }
        double 有效值 = 0;
        有效值 += Math.min(属性值, 递减第一段上限) * 递减第一段系数;
        if (属性值 > 递减第一段上限) {
            有效值 += Math.min(属性值 - 递减第一段上限, 递减第二段上限 - 递减第一段上限) * 递减第二段系数;
        }
        if (属性值 > 递减第二段上限) {
            有效值 += Math.min(属性值 - 递减第二段上限, 递减第三段上限 - 递减第二段上限) * 递减第三段系数;
        }
        if (属性值 > 递减第三段上限) {
            有效值 += (属性值 - 递减第三段上限) * 递减第四段系数;
        }
        return 有效值;
    }

    @Override
    public double 计算实际公共冷却(double 基础公共冷却, double 急速) {
        if (基础公共冷却 <= 0) {
            return 0;
        }
        double 有效急速 = 规范化有效急速(急速);
        double 公共冷却下限 = 基础公共冷却 * 公共冷却下限比例;
        return Math.max(公共冷却下限, 基础公共冷却 / (1 + 有效急速 * 急速百分比系数));
    }

    @Override
    public double 计算实际蓄力时间(double 基础蓄力时间, double 急速) {
        if (基础蓄力时间 <= 0) {
            return 0;
        }
        double 有效急速 = 规范化有效急速(急速);
        double 蓄力时间下限 = Math.min(基础蓄力时间, 蓄力时间下限秒);
        return Math.max(蓄力时间下限, 基础蓄力时间 / (1 + 有效急速 * 急速百分比系数));
    }

    private double 规范化有效急速(double 急速) {
        if (Double.isNaN(急速) || 急速 == Double.NEGATIVE_INFINITY) {
            return 0;
        }
        if (急速 == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(-99.0, 应用通用递减(急速));
    }

    @Override
    public double 计算派生值(String 派生名, 属性快照 属性) {
        if (派生名 == null || 属性 == null) {
            return 0;
        }
        派生属性计算器 计算器 = 派生计算器注册表.get(派生名);
        if (计算器 == null) {
            调试日志器.调试("属性计算服务", "派生计算器未注册：派生名=%s", 派生名);
            return 0;
        }
        double 结果 = 计算器.计算(属性, this::应用通用递减);
        调试日志器.调试("属性计算服务", "派生值计算：派生名=%s 结果=%.4f", 派生名, 结果);
        return 结果;
    }
}
