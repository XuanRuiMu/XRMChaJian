package mljy.业务层;

import com.google.inject.Inject;
import mljy.业务层.消息.受击修正上下文;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害管线阶段;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.效果.效果实例;
import mljy.领域层.实体;
import mljy.领域层.玩家.玩家快照;
import mljy.技能实现.奥能法师.奥术护盾效果处理器;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class 伤害计算服务实现 implements 伤害计算服务 {
    private static final String 类别伤害全伤害百分比 = "伤害_全伤害百分比";
    private static final String 类别伤害暴击率额外 = "伤害_暴击率额外";
    private static final String 类别伤害暴击伤害额外 = "伤害_暴击伤害额外";
    private static final String 类别减伤全局百分比 = "减伤_全局百分比";
    private static final String 类别精通伤害百分比 = "精通_伤害百分比";

    private static final String 护盾效果标识 = "1_3_1";

    private static final double 百分比系数 = 0.01;
    private static final double 最小伤害值 = 1.0;
    private static final double 数值精度 = 10.0;
    private static final double 暴击率判定基数 = 100.0;

    private final 属性计算服务 属性计算服务;
    private final 效果调度服务 效果调度服务;
    private final 奥术护盾效果处理器 护盾处理器;
    private final Map<伤害管线阶段, List<伤害管线阶段处理器>> 阶段处理器注册表 = new ConcurrentHashMap<>();

    @Inject
    public 伤害计算服务实现(属性计算服务 属性计算服务, 效果调度服务 效果调度服务,
                          奥术护盾效果处理器 护盾处理器) {
        this.属性计算服务 = 属性计算服务;
        this.效果调度服务 = 效果调度服务;
        this.护盾处理器 = 护盾处理器;
        注册阶段处理器(new 伤害管线阶段处理器.全能增伤阶段处理器());
        注册阶段处理器(new 伤害管线阶段处理器.全能减伤阶段处理器());
    }

    public 伤害计算服务实现(属性计算服务 属性计算服务, 效果调度服务 效果调度服务) {
        this(属性计算服务, 效果调度服务, new 奥术护盾效果处理器(效果调度服务));
    }

    public void 注册阶段处理器(伤害管线阶段处理器 处理器) {
        if (处理器 == null || 处理器.阶段() == null) {
            return;
        }
        阶段处理器注册表.computeIfAbsent(处理器.阶段(), k -> new CopyOnWriteArrayList<>()).add(处理器);
    }

    public void 清空阶段处理器(伤害管线阶段 阶段) {
        阶段处理器注册表.remove(阶段);
    }

    @Override
    public 伤害结果 计算(伤害上下文 上下文) {
        调试日志器.调试("伤害计算服务", "计算入口：基础数值=%.1f 是否法术伤害=%s 施法者=%s", 上下文.基础数值(), 上下文.是否法术伤害(), 上下文.施法者());
        double 数值 = 阶段1_基础伤害确定(上下文);
        数值 = 阶段3_精通修饰(上下文, 数值);
        boolean 是否暴击 = 阶段4_暴击判定(上下文);
        数值 = 应用暴击(上下文, 数值, 是否暴击);
        数值 = 阶段5_全局伤害修饰(上下文, 数值);
        数值 = 阶段6_目标减伤(上下文, 数值);
        护盾吸收结果 吸收结果 = 阶段7_护盾吸收(上下文, 数值);
        数值 = 吸收结果.剩余伤害;
        数值 = 阶段8_最终修正(上下文, 数值);
        数值 = 保留一位小数(数值);
        调试日志器.调试("伤害计算服务", "计算完成：最终数值=%.1f 是否暴击=%s 被吸收=%s", 数值, 是否暴击, 吸收结果.被吸收);
        return new 伤害结果(数值, 是否暴击, 吸收结果.被吸收);
    }

    private double 阶段1_基础伤害确定(伤害上下文 上下文) {
        调试日志器.调试("伤害计算服务", "阶段1_基础伤害确定：基础数值=%.1f 是否法术伤害=%s", 上下文.基础数值(), 上下文.是否法术伤害());
        return 上下文.基础数值();
    }

    private double 阶段3_精通修饰(伤害上下文 上下文, double 数值) {
        double 精通增伤总和 = 0;
        for (修饰器 项 : 上下文.修饰器列表()) {
            if (项.类别().equals(类别精通伤害百分比)) {
                精通增伤总和 += 项.数值();
            }
        }
        double 修饰后 = 数值 * (1 + 精通增伤总和 * 百分比系数);
        调试日志器.调试("伤害计算服务", "阶段3_精通修饰：精通增伤总和=%.4f 输入=%.1f 输出=%.1f",
                精通增伤总和, 数值, 修饰后);
        return 修饰后;
    }

    private boolean 阶段4_暴击判定(伤害上下文 上下文) {
        if (!上下文.是否法术伤害()) {
            调试日志器.调试("伤害计算服务", "暴击判定：跳过=非法术伤害不触发法术暴击");
            return false;
        }
        if (上下文.是否暴击()) {
            调试日志器.调试("伤害计算服务", "暴击判定：上下文强制暴击");
            return true;
        }
        if (上下文.施法者() == null) {
            调试日志器.调试("伤害计算服务", "暴击判定：无施法者不暴击");
            return false;
        }
        属性快照 施法者属性 = 属性计算服务.计算(上下文.施法者());
        double 暴击率 = 施法者属性.法术暴击几率();
        double 额外暴击率 = 0;
        for (修饰器 项 : 上下文.修饰器列表()) {
            if (项.类别().equals(类别伤害暴击率额外)) {
                额外暴击率 += 项.数值();
            }
        }
        double 最终暴击率 = 暴击率 + 额外暴击率;
        boolean 暴击 = ThreadLocalRandom.current().nextDouble() * 暴击率判定基数 < 最终暴击率;
        调试日志器.调试("伤害计算服务", "暴击判定：暴击率=%.2f 额外=%.2f 最终=%.2f 是否暴击=%s", 暴击率, 额外暴击率, 最终暴击率, 暴击);
        return 暴击;
    }

    private double 应用暴击(伤害上下文 上下文, double 数值, boolean 是否暴击) {
        if (!是否暴击) {
            调试日志器.调试("伤害计算服务", "应用暴击：跳过=未暴击 输入=%.1f 输出=%.1f", 数值, 数值);
            return 数值;
        }
        double 暴击伤害倍率 = 上下文.暴击倍率();
        double 暴击伤害 = 0;
        if (上下文.施法者() != null) {
            属性快照 施法者属性 = 属性计算服务.计算(上下文.施法者());
            暴击伤害 = 施法者属性.法术暴击伤害();
            暴击伤害倍率 += 暴击伤害 * 百分比系数;
        }
        double 额外暴击伤害 = 0;
        for (修饰器 项 : 上下文.修饰器列表()) {
            if (项.类别().equals(类别伤害暴击伤害额外)) {
                额外暴击伤害 += 项.数值();
            }
        }
        暴击伤害倍率 += 额外暴击伤害 * 百分比系数;
        double 暴击后 = 数值 * 暴击伤害倍率;
        调试日志器.调试("伤害计算服务", "应用暴击：暴击倍率=%.4f 暴击伤害=%.4f 额外暴击伤害=%.4f 最终倍率=%.4f 输入=%.1f 输出=%.1f",
                上下文.暴击倍率(), 暴击伤害, 额外暴击伤害, 暴击伤害倍率, 数值, 暴击后);
        return 暴击后;
    }

    private double 阶段5_全局伤害修饰(伤害上下文 上下文, double 数值) {
        double 全局增伤总和 = 0;
        for (修饰器 项 : 上下文.修饰器列表()) {
            if (项.类别().equals(类别伤害全伤害百分比)) {
                全局增伤总和 += 项.数值();
            }
        }
        List<伤害管线阶段处理器> 处理器列表 = 阶段处理器注册表.getOrDefault(伤害管线阶段.全局伤害修饰, List.of());
        for (伤害管线阶段处理器 处理器 : 处理器列表) {
            全局增伤总和 += 处理器.计算增减伤百分比(上下文, 属性计算服务) / 百分比系数;
        }
        double 修饰后 = 数值 * (1 + 全局增伤总和 * 百分比系数);
        调试日志器.调试("伤害计算服务", "阶段5_全局伤害修饰：全局增伤总和=%.4f 处理器数量=%d 输入=%.1f 输出=%.1f",
                全局增伤总和, 处理器列表.size(), 数值, 修饰后);
        return 修饰后;
    }

    private double 阶段6_目标减伤(伤害上下文 上下文, double 数值) {
        double 全局减伤总和 = 0;
        for (修饰器 项 : 上下文.修饰器列表()) {
            if (项.类别().equals(类别减伤全局百分比)) {
                全局减伤总和 += 项.数值();
            }
        }
        List<伤害管线阶段处理器> 处理器列表 = 阶段处理器注册表.getOrDefault(伤害管线阶段.目标减伤, List.of());
        for (伤害管线阶段处理器 处理器 : 处理器列表) {
            全局减伤总和 += 处理器.计算增减伤百分比(上下文, 属性计算服务) / 百分比系数;
        }
        double 全局减伤倍率 = 1 - 全局减伤总和 * 百分比系数;
        double 减伤后 = 数值 * 全局减伤倍率;
        调试日志器.调试("伤害计算服务", "阶段6_目标减伤：全局减伤总和=%.4f 全局倍率=%.4f 输入=%.1f 输出=%.1f 处理器数量=%d",
                全局减伤总和, 全局减伤倍率, 数值, 减伤后, 处理器列表.size());
        return 减伤后;
    }

    private 护盾吸收结果 阶段7_护盾吸收(伤害上下文 上下文, double 数值) {
        Optional<UUID> 目标标识Opt = 提取目标标识(上下文.目标());
        if (目标标识Opt.isEmpty()) {
            调试日志器.调试("伤害计算服务", "护盾效果查找跳过：目标无可用UUID 输入伤害=%.6f", 数值);
            return new 护盾吸收结果(数值, false);
        }
        UUID 目标标识 = 目标标识Opt.get();
        UUID 上下文事件标识 = 上下文.事件标识();
        UUID 根事件标识 = 上下文事件标识 != null ? 上下文事件标识 : 事件日志上下文.获取当前事件标识或空();
        调试日志器.调试("伤害计算服务",
                "护盾效果查找：目标=%s 是否法术伤害=%s 输入伤害=%.6f root_event_id=%s 来源=%s",
                目标标识, 上下文.是否法术伤害(), 数值,
                根事件标识 == null ? "none" : 根事件标识,
                上下文事件标识 != null ? "上下文" : "ThreadLocal");
        Optional<效果实例> 护盾效果Opt = 查找护盾效果(目标标识);
        if (护盾效果Opt.isEmpty()) {
            调试日志器.调试("伤害计算服务",
                    "护盾效果查找完成：目标=%s 命中=false 输入伤害=%.6f 剩余伤害=%.6f root_event_id=%s",
                    目标标识, 数值, 数值, 根事件标识 == null ? "none" : 根事件标识);
            return new 护盾吸收结果(数值, false);
        }
        效果实例 护盾效果 = 护盾效果Opt.get();
        奥术护盾效果处理器.护盾吸收结果 处理结果 = 护盾处理器.吸收伤害(
                目标标识, 护盾效果, 数值, 根事件标识);
        if (处理结果.吸收量() > 0.0D) {
            受击修正上下文.记录吸收(
                    目标标识,
                    根事件标识,
                    处理结果.吸收量(),
                    "effect.奥能法师.奥术护盾.name");
        }
        调试日志器.调试("伤害计算服务",
                "护盾扣减后：目标=%s 是否法术伤害=%s 输入伤害=%.6f 吸收量=%.6f 剩余伤害=%.6f 剩余护盾=%.6f 最终效果状态=%s root_event_id=%s",
                目标标识, 上下文.是否法术伤害(), 数值, 处理结果.吸收量(), 处理结果.剩余伤害(),
                处理结果.剩余护盾(), 处理结果.已耗尽() ? "已移除" : "保留精确值",
                根事件标识 == null ? "none" : 根事件标识);
        return new 护盾吸收结果(处理结果.剩余伤害(), 处理结果.吸收量() > 0.0D);
    }

    private double 阶段8_最终修正(伤害上下文 上下文, double 数值) {
        调试日志器.调试("伤害计算服务", "阶段8_最终修正入口：数值=%.1f 基础数值=%.1f", 数值, 上下文.基础数值());
        if (数值 <= 0) {
            if (上下文.基础数值() <= 0) {
                调试日志器.调试("伤害计算服务", "阶段8_最终修正：分支=数值非正且基础非正 返回最小伤害=%.1f", 最小伤害值);
                return 最小伤害值;
            }
            调试日志器.调试("伤害计算服务", "阶段8_最终修正：分支=数值非正但基础为正 返回=0");
            return 0;
        }
        double 最终值 = Math.max(数值, 最小伤害值);
        调试日志器.调试("伤害计算服务", "阶段8_最终修正：分支=数值为正 返回=%.1f 最小伤害值=%.1f", 最终值, 最小伤害值);
        return 最终值;
    }

    private Optional<效果实例> 查找护盾效果(UUID 目标标识) {
        List<效果实例> 效果列表 = 效果调度服务.获取列表(目标标识);
        if (效果列表 == null) {
            return Optional.empty();
        }
        for (效果实例 效果 : 效果列表) {
            if (效果 != null && 护盾效果标识.equals(效果.获取效果标识())) {
                return Optional.of(效果);
            }
        }
        return Optional.empty();
    }

    private Optional<UUID> 提取目标标识(Object 目标) {
        if (目标 instanceof 实体 实体目标) {
            return Optional.of(实体目标.获取唯一标识());
        }
        if (目标 instanceof 玩家快照 玩家目标) {
            return Optional.of(玩家目标.唯一标识());
        }
        return Optional.empty();
    }

    private double 保留一位小数(double 数值) {
        return Math.round(数值 * 数值精度) / 数值精度;
    }

    private record 护盾吸收结果(double 剩余伤害, boolean 被吸收) {
    }
}
