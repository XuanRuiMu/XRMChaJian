package mljy.业务层.消息;

import mljy.基础设施层.事件日志上下文;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 受击修正上下文。
 * 当伤害被效果修改时（护盾吸收、诅咒增伤、树皮减伤等），记录修正信息，
 * 供后续受到伤害日志附加显示。使用后自动清除。
 *
 * 通用设计：来源名称参数化，任何效果均可使用——
 *   护盾类：记录吸收(玩家, 吸收量, "奥术护盾") → "被奥术护盾吸收了5.0点伤害。"
 *   减伤类：记录吸收(玩家, 减伤量, "树皮术")   → "被树皮术减少了3.0点伤害。"
 *   诅咒类：未来可扩展 record 类型区分增伤/减伤
 */
public class 受击修正上下文 {

    private static final Map<修正键, List<修正信息>> 修正表 = new ConcurrentHashMap<>();

    /**
     * 追加一条修正到列表（不覆盖已有记录）。
     */
    public static void 追加(UUID 玩家标识, double 修正量, String 来源名称, 修正类型 类型) {
        追加(玩家标识, 获取默认事件标识(), 修正量, 来源名称, 类型);
    }

    public static void 追加(UUID 玩家标识, UUID 事件标识, double 修正量, String 来源名称, 修正类型 类型) {
        修正键 键 = new 修正键(玩家标识, 事件标识);
        修正表.computeIfAbsent(键, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new 修正信息(修正量, 来源名称, 类型));
    }

    /**
     * 记录一次伤害修正（吸收、减伤等）。
     */
    public static void 记录(UUID 玩家标识, double 修正量, String 来源名称, 修正类型 类型) {
        追加(玩家标识, 修正量, 来源名称, 类型);
    }

    public static void 记录(UUID 玩家标识, UUID 事件标识, double 修正量, String 来源名称, 修正类型 类型) {
        追加(玩家标识, 事件标识, 修正量, 来源名称, 类型);
    }

    /**
     * 记录伤害吸收（护盾等）。便捷方法。
     */
    public static void 记录吸收(UUID 玩家标识, double 吸收量, String 来源名称) {
        记录(玩家标识, 吸收量, 来源名称, 修正类型.吸收);
    }

    public static void 记录吸收(UUID 玩家标识, UUID 事件标识, double 吸收量, String 来源名称) {
        记录(玩家标识, 事件标识, 吸收量, 来源名称, 修正类型.吸收);
    }

    /**
     * 获取并清除修正信息列表。无记录时返回空列表。
     */
    public static List<修正信息> 获取并清除(UUID 玩家标识) {
        return 获取并清除(玩家标识, 获取默认事件标识());
    }

    public static List<修正信息> 获取并清除(UUID 玩家标识, UUID 事件标识) {
        List<修正信息> 列表 = 修正表.remove(new 修正键(玩家标识, 事件标识));
        return 列表 != null ? 列表 : Collections.emptyList();
    }

    private static UUID 获取默认事件标识() {
        return 事件日志上下文.获取当前事件标识或空();
    }

    public enum 修正类型 {
        吸收,
        减伤,
        增伤
    }

    public record 修正信息(double 修正量, String 来源名称, 修正类型 类型) {
        public String 获取翻译键() {
            return switch (类型) {
                case 吸收 -> "战斗日志.吸收";
                case 减伤 -> "战斗日志.减伤";
                case 增伤 -> "战斗日志.增伤";
            };
        }
    }

    private record 修正键(UUID 玩家标识, UUID 事件标识) {
    }
}
