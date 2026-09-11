package mljy.技能实现.奥能法师;

import com.google.inject.Inject;
import mljy.业务层.伤害计算服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.资源变更服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.吸血处理服务;
import mljy.业务层.技能释放服务;
import mljy.战斗服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.实体;
import mljy.领域层.效果.效果定义;
import mljy.领域层.效果.效果实例;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.技能.参数读取器;
import mljy.领域层.技能.参数注册表;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.基础设施层.颜色码工具;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class 奥能法师技能基础 {
    protected static final String 秘能资源标识 = "秘能";
    protected static final String 秘兆效果标识 = "1_9_1";
    protected static final String 奥术护盾效果标识 = "1_3_1";
    protected static final String 奥能冥想蓄能效果标识 = "1_7_1";
    protected static final String 奥能冥想效果标识 = "1_7_2";
    protected static final double 秘能上限 = 4.0;
    protected static final double 默认暴击倍率 = 1.5;
    protected static final int Aoe衰减阈值 = 5;
    protected static final int 秘法湮灭衰减阈值 = 8;
    protected static final String 受到伤害日志键 = "战斗日志.受到伤害";
    protected static final String 受到伤害暴击日志键 = "战斗日志.受到伤害暴击";
    protected static final String 受到伤害带护盾抵挡日志键 = "战斗日志.受到伤害带护盾抵挡";
    protected static final String 技能伤害日志键 = "战斗日志.技能伤害";
    protected static final String 技能伤害暴击日志键 = "战斗日志.技能伤害暴击";
    protected static final String 命中日志键 = "技能日志.命中";
    protected static final String 暴击命中日志键 = "技能日志.暴击命中";
    protected static final String 命中无资源日志键 = "技能日志.命中无资源";
    protected static final String 暴击命中无资源日志键 = "技能日志.暴击命中无资源";
    protected static final String 命中仅资源日志键 = "技能日志.命中仅资源";
    protected static final String 命中仅资源带秘兆日志键 = "技能日志.命中仅资源带秘兆";
    protected static final String 范围命中日志键 = "技能日志.范围命中";
    protected static final String 范围暴击命中日志键 = "技能日志.范围暴击命中";
    protected static final String 范围命中无资源日志键 = "技能日志.范围命中无资源";
    protected static final String 范围命中仅资源日志键 = "技能日志.范围命中仅资源";
    protected static final String 范围命中仅资源带秘兆日志键 = "技能日志.范围命中仅资源带秘兆";
    protected static final String 范围命中仅资源带暴击日志键 = "技能日志.范围命中仅资源带暴击";
    protected static final String 范围命中仅资源带秘兆带暴击日志键 = "技能日志.范围命中仅资源带秘兆带暴击";
    protected static final String 数值格式 = "%.1f";

    protected UUID 获取调试根事件标识() {
        return 事件日志上下文.获取当前事件标识或空();
    }

    protected UUID 创建施法标识() {
        return UUID.randomUUID();
    }

    protected void 记录技能阶段(String 模块, 玩家快照 施法者, String 技能标识,
                            UUID 根事件标识, UUID 施法标识, UUID 目标标识,
                            String 阶段, String 结果, String 详情格式, Object... 详情参数) {
        String 详情 = 详情格式 == null || 详情格式.isBlank()
                ? ""
                : String.format(详情格式, 详情参数);
        调试日志器.调试(模块,
                "player_uuid=%s skill_id=%s root_event_id=%s cast_id=%s target_uuid=%s stage=%s result=%s before_after=%s",
                施法者 == null ? "null" : 施法者.唯一标识(),
                技能标识,
                根事件标识 == null ? "null" : 根事件标识,
                施法标识 == null ? "null" : 施法标识,
                目标标识 == null ? "null" : 目标标识,
                阶段,
                结果,
                详情);
    }

    @Inject
    protected JavaPlugin 插件;
    @Inject
    protected 战斗服务 战斗服务;
    @Inject
    protected 伤害计算服务 伤害计算服务;
    @Inject
    protected 目标选择服务 目标选择服务;
    @Inject
    protected 消息服务 消息服务;
    @Inject
    protected 效果调度服务 效果调度服务;
    @Inject
    protected 资源变更服务 资源变更服务;
    @Inject
    protected 效果注册服务 效果注册服务;
    @Inject
    protected 玩家服务 玩家服务;
    @Inject
    protected 翻译服务 翻译服务;
    @Inject
    protected 战斗状态服务 战斗状态服务;
    @Inject
    protected 技能释放服务 技能释放服务实例;
    @Inject
    protected 吸血处理服务 吸血处理服务;
    @Inject
    protected 秘兆效果处理器 秘兆效果处理器;
    @Inject
    protected 参数注册表 参数注册表实例;
    protected 参数读取器 当前参数读取器;

    protected double 获取秘能层数(玩家快照 玩家) {
        return 资源变更服务.获取当前值(玩家.唯一标识(), 秘能资源标识);
    }

    protected double 获取参数(String 键, double 默认值) {
        return 当前参数读取器 != null ? 当前参数读取器.获取双精度(键, 默认值) : 默认值;
    }

    protected int 获取参数(String 键, int 默认值) {
        return 当前参数读取器 != null ? 当前参数读取器.获取整数(键, 默认值) : 默认值;
    }

    protected long 获取参数(String 键, long 默认值) {
        return 当前参数读取器 != null ? 当前参数读取器.获取长整数(键, 默认值) : 默认值;
    }

    protected void 注册参数(String 技能或效果ID, String 参数名, String 默认值, String 描述) {
        if (参数注册表实例 != null) {
            参数注册表实例.注册(技能或效果ID, new 参数注册表.参数条目(参数名, 默认值, 描述));
        }
    }

    protected 位置 获取实时位置(玩家快照 施法者) {
        Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (玩家 == null) {
            return null;
        }
        return 位置适配器.转换(玩家.getLocation());
    }

    protected boolean 增加秘能(玩家快照 玩家, double 数量) {
        return 增加秘能(玩家, 数量, true);
    }

    protected boolean 增加秘能(玩家快照 玩家, double 数量, boolean 发射日志) {
        if (玩家 == null || 玩家.唯一标识() == null) {
            调试日志器.调试("奥能法师技能基础", "增加秘能跳过：施法者或标识为空 数量=%.1f", 数量);
            return false;
        }
        if (!Double.isFinite(数量) || 数量 <= 0) {
            调试日志器.调试("奥能法师技能基础", "增加秘能跳过：请求数量无效 施法者=%s 请求=%.1f", 玩家.名称(), 数量);
            return false;
        }
        double 当前 = 获取秘能层数(玩家);
        double 上限 = 资源变更服务.获取上限(玩家.唯一标识(), 秘能资源标识);
        if (!Double.isFinite(当前) || !Double.isFinite(上限)) {
            调试日志器.调试("奥能法师技能基础", "增加秘能跳过：资源状态无效 施法者=%s 当前=%.1f 上限=%.1f 请求=%.1f",
                    玩家.名称(), 当前, 上限, 数量);
            return false;
        }
        double 可增加 = Math.max(0, 上限 - 当前);
        double 新增 = Math.min(数量, 可增加);
        if (新增 > 0) {
            boolean 触发秘兆 = 资源变更服务.增加(玩家.唯一标识(), 秘能资源标识, 新增);
            double 增加后 = 获取秘能层数(玩家);
            double 实际增加 = 增加后 - 当前;
            调试日志器.调试("奥能法师技能基础", "增加秘能结果：施法者=%s 当前=%.1f 请求=%.1f 可增加=%.1f 调用增加=%.1f 增加后=%.1f 实际增加=%.1f 触发秘兆=%s",
                    玩家.名称(), 当前, 数量, 可增加, 新增, 增加后, 实际增加, 触发秘兆);
            if (发射日志 && 实际增加 > 0) {
                String 键 = 触发秘兆 ? 命中仅资源带秘兆日志键 : 命中仅资源日志键;
                消息服务.发送技能日志(玩家, 事件日志上下文.获取当前事件标识或空(), 键, String.valueOf(实际增加));
            }
            return 触发秘兆;
        }
        调试日志器.调试("奥能法师技能基础", "增加秘能跳过：已达上限或没有可用空间 施法者=%s 当前=%.1f 上限=%.1f 请求=%.1f 可增加=%.1f",
                玩家.名称(), 当前, 上限, 数量, 可增加);
        return false;
    }

    protected void 消耗所有秘能(玩家快照 玩家) {
        double 消耗前 = 获取秘能层数(玩家);
        调试日志器.调试("奥能法师技能基础", "消耗所有秘能：施法者=%s 消耗前=%.1f", 玩家.名称(), 消耗前);
        资源变更服务.清空(玩家.唯一标识(), 秘能资源标识);
        double 消耗后 = 获取秘能层数(玩家);
        double 实际扣减 = Math.max(0, 消耗前 - 消耗后);
        调试日志器.调试("奥能法师技能基础", "消耗所有秘能：施法者=%s 消耗后=%.1f 实际扣减=%.1f 是否清零=%b 消费流程已执行=%b",
                玩家.名称(), 消耗后, 实际扣减, 消耗后 <= 0, true);
        if (消耗前 <= 0) {
            调试日志器.调试("奥能法师技能基础", "消耗所有秘能：施法者=%s 初始秘能为零，消费流程仍已执行，实际扣减=0.0",
                    玩家.名称());
        }
    }

    protected long 计算效果到期时间(String 效果标识, long 默认持续) {
        long 当前时间 = System.currentTimeMillis();
        Optional<效果定义> 定义Opt = 效果注册服务 == null
                ? Optional.empty()
                : 效果注册服务.获取定义(效果标识);
        if (定义Opt != null && 定义Opt.isPresent()) {
            long 持续 = 定义Opt.get().持续时间();
            if (持续 == 效果定义.永久持续) {
                调试日志器.调试("奥能法师技能基础", "计算效果到期时间：效果=%s 来源=定义 永久=true 到期=%d",
                        效果标识, 效果定义.永久持续);
                return 效果定义.永久持续;
            }
            long 到期时间 = 当前时间 + 持续;
            调试日志器.调试("奥能法师技能基础", "计算效果到期时间：效果=%s 来源=定义 持续=%d 到期=%d",
                    效果标识, 持续, 到期时间);
            return 到期时间;
        }
        long 到期时间 = 当前时间 + 默认持续;
        调试日志器.调试("奥能法师技能基础", "计算效果到期时间：效果=%s 来源=默认 持续=%d 到期=%d",
                效果标识, 默认持续, 到期时间);
        return 到期时间;
    }

    protected boolean 有秘兆效果(玩家快照 玩家) {
        List<效果实例> 效果列表 = 玩家 == null || 玩家.唯一标识() == null
                ? List.of()
                : 获取效果列表(玩家.唯一标识());
        效果实例 秘兆 = 获取秘兆效果(效果列表);
        boolean 有 = 秘兆 != null;
        调试日志器.调试("奥能法师技能基础", "有秘兆效果：玩家=%s 结果=%s 当前效果=%s 效果总数=%d",
                玩家 == null ? null : 玩家.名称(), 有, 描述秘兆效果(秘兆), 效果列表.size());
        return 有;
    }

    private List<效果实例> 获取效果列表(UUID 玩家标识) {
        if (效果调度服务 == null || 玩家标识 == null) {
            return List.of();
        }
        List<效果实例> 效果列表 = 效果调度服务.获取列表(玩家标识);
        return 效果列表 == null ? List.of() : 效果列表;
    }

    private 效果实例 获取秘兆效果(List<效果实例> 效果列表) {
        return 效果列表.stream()
                .filter(效果 -> 效果 != null && 秘兆效果标识.equals(效果.获取效果标识()))
                .findFirst()
                .orElse(null);
    }

    private String 描述秘兆效果(效果实例 效果) {
        if (效果 == null) {
            return "不存在";
        }
        return String.format("存在=true 层数=%d 到期=%d", 效果.获取层数(), 效果.获取到期时间());
    }

    protected void 移除秘兆效果(玩家快照 玩家) {
        调试日志器.调试("奥能法师技能基础", "移除秘兆效果：玩家=%s 先手动停止特效再走正常移除流程", 玩家.名称());
        秘兆效果处理器.停止特效(玩家.唯一标识());
        效果调度服务.移除(玩家.唯一标识(), 秘兆效果标识);
    }

    protected double 计算秘兆增伤(玩家快照 玩家, double 基础伤害) {
        if (!有秘兆效果(玩家)) {
            调试日志器.调试("奥能法师技能基础", "计算秘兆增伤：玩家=%s 无秘兆效果 增伤=0", 玩家.名称());
            return 0;
        }
        double 增伤 = 基础伤害 * (玩家.属性().精通() * 0.3 / 100.0);
        调试日志器.调试("奥能法师技能基础", "计算秘兆增伤：玩家=%s 基础伤害=%.1f 精通=%.1f 增伤=%.2f", 玩家.名称(), 基础伤害, 玩家.属性().精通(), 增伤);
        return 增伤;
    }

    protected double 应用Aoe衰减(double 基础伤害, int 目标数, int 阈值) {
        if (目标数 <= 阈值) {
            return 基础伤害;
        }
        return 基础伤害 * Math.sqrt((double) 阈值 / 目标数);
    }

    protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害, boolean 是否暴击) {
        return 应用单体伤害(施法者, 目标, 基础伤害, 是否暴击, false);
    }

    protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害, boolean 是否暴击, boolean 消耗秘兆) {
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            double 实际基础伤害 = 基础伤害 + (消耗秘兆 ? 计算秘兆增伤(施法者, 基础伤害) : 0);
            double 当前暴击倍率 = 获取参数("暴击倍率", 默认暴击倍率);
            伤害上下文 上下文 = new 伤害上下文(施法者, 目标, 实际基础伤害, true, 是否暴击, 当前暴击倍率, List.of(), 事件标识);
            伤害结果 结果 = 战斗服务.应用伤害(上下文);
            if (结果 == null) {
                return null;
            }
            if (消耗秘兆 && 有秘兆效果(施法者)) {
                移除秘兆效果(施法者);
            }
            调试日志器.调试("奥能法师技能基础", "应用单体伤害：施法者=%s 目标=%s 基础伤害=%.1f 是否法术伤害=true 是否暴击=%s 最终伤害=%.1f", 施法者.名称(), 获取技能目标名称(目标), 基础伤害, 是否暴击, 结果.最终数值());
            发送受击日志(目标, 施法者.名称(), 结果, 事件标识);
            return 结果;
        } finally {
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    protected 伤害结果 应用单体伤害并记录命中(玩家快照 施法者, 实体 目标, double 基础伤害, boolean 是否暴击, String 技能名翻译键, int 资源获取量) {
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            伤害结果 结果 = 应用单体伤害(施法者, 目标, 基础伤害, 是否暴击);
            if (结果 == null) {
                return null;
            }
            boolean 获得秘兆 = false;
            if (资源获取量 > 0) {
                获得秘兆 = 增加秘能(施法者, 资源获取量, false);
            }
            String 槽位中文数字 = 查找槽位中文数字(技能名翻译键);
            String 目标名 = 获取技能目标名称(目标);
            String 伤害数值 = String.format(数值格式, 结果.最终数值());
            String 战斗日志键 = 结果.是否暴击() ? 技能伤害暴击日志键 : 技能伤害日志键;
            消息服务.发送技能日志(施法者, 事件标识, 战斗日志键, 槽位中文数字, 目标名, 伤害数值);
            if (资源获取量 > 0) {
                if (获得秘兆) {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源带秘兆日志键, String.valueOf(资源获取量));
                } else {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源日志键, String.valueOf(资源获取量));
                }
            }
            return 结果;
        } finally {
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    protected 伤害结果 应用单体伤害并记录命中(玩家快照 施法者, 实体 目标, double 基础伤害,
                                        boolean 是否暴击, String 技能名翻译键, int 资源获取量,
                                        boolean 消耗秘兆) {
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            伤害结果 结果 = 应用单体伤害(施法者, 目标, 基础伤害, 是否暴击, 消耗秘兆);
            if (结果 == null) {
                return null;
            }
            boolean 获得秘兆 = false;
            if (资源获取量 > 0) {
                获得秘兆 = 增加秘能(施法者, 资源获取量, false);
            }
            String 槽位中文数字 = 查找槽位中文数字(技能名翻译键);
            String 目标名 = 获取技能目标名称(目标);
            String 伤害数值 = String.format(数值格式, 结果.最终数值());
            String 战斗日志键 = 结果.是否暴击() ? 技能伤害暴击日志键 : 技能伤害日志键;
            消息服务.发送技能日志(施法者, 事件标识, 战斗日志键, 槽位中文数字, 目标名, 伤害数值);
            if (资源获取量 > 0) {
                if (获得秘兆) {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源带秘兆日志键, String.valueOf(资源获取量));
                } else {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源日志键, String.valueOf(资源获取量));
                }
            }
            return 结果;
        } finally {
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    protected 伤害结果 应用单体伤害并记录命中(玩家快照 施法者, 实体 目标, double 基础伤害,
                                        boolean 是否暴击, String 技能名翻译键, int 资源获取量,
                                        UUID 事件标识) {
        try {
            伤害结果 结果 = 应用单体伤害(施法者, 目标, 基础伤害, 是否暴击);
            if (结果 == null) {
                return null;
            }
            boolean 获得秘兆 = false;
            if (资源获取量 > 0) {
                获得秘兆 = 增加秘能(施法者, 资源获取量, false);
            }
            String 槽位中文数字 = 查找槽位中文数字(技能名翻译键);
            String 目标名 = 获取技能目标名称(目标);
            String 伤害数值 = String.format(数值格式, 结果.最终数值());
            String 战斗日志键 = 结果.是否暴击() ? 技能伤害暴击日志键 : 技能伤害日志键;
            消息服务.发送技能日志(施法者, 事件标识, 战斗日志键, 槽位中文数字, 目标名, 伤害数值);
            if (资源获取量 > 0) {
                if (获得秘兆) {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源带秘兆日志键, String.valueOf(资源获取量));
                } else {
                    消息服务.发送技能日志(施法者, 事件标识, 命中仅资源日志键, String.valueOf(资源获取量));
                }
            }
            return 结果;
        } finally {
        }
    }

    protected String 查找槽位中文数字(String 翻译键) {
        // 翻译键格式：skill.奥能法师.{技能名}.name
        // slot 键格式：skill.奥能法师.slot.{中文数字}.name = 技能名
        String 前缀 = "skill.奥能法师.";
        String 后缀 = ".name";
        if (!翻译键.startsWith(前缀) || !翻译键.endsWith(后缀)) {
            return "一";
        }
        String 技能名 = 翻译键.substring(前缀.length(), 翻译键.length() - 后缀.length());
        String[] 中文数字 = {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
        for (String 数字 : 中文数字) {
            String 槽位技能名 = 翻译服务.获取("skill.奥能法师.slot." + 数字 + ".name");
            if (技能名.equals(槽位技能名)) {
                return 数字;
            }
        }
        return "一";
    }

    private void 发送受击日志(实体 目标, String 来源名, 伤害结果 结果, UUID 事件标识) {
        if (结果 == null || 结果.最终数值() < 0) {
            return;
        }
        if (!(目标 instanceof 实体适配器 适配器)) {
            return;
        }
        if (!(适配器.获取原始实体() instanceof Player 玩家)) {
            return;
        }
        List<mljy.业务层.消息.受击修正上下文.修正信息> 修正列表 =
                mljy.业务层.消息.受击修正上下文.获取并清除(玩家.getUniqueId(), 事件标识);
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家.getUniqueId());
        if (快照可选.isEmpty()) {
            return;
        }
        String 伤害数值 = String.format(数值格式, 结果.最终数值());
        Optional<mljy.业务层.消息.受击修正上下文.修正信息> 护盾吸收可选 = 修正列表.stream()
                .filter(修正 -> 修正 != null
                        && 修正.类型() == mljy.业务层.消息.受击修正上下文.修正类型.吸收
                        && 修正.修正量() > 0)
                .findFirst();
        if (护盾吸收可选.isPresent()) {
            String 抵挡数值 = String.format(数值格式, 护盾吸收可选.get().修正量());
            消息服务.发送战斗日志(快照可选.get(), 事件标识, 受到伤害带护盾抵挡日志键,
                    来源名, 伤害数值, 抵挡数值);
        } else {
            String 键 = 结果.是否暴击() ? 受到伤害暴击日志键 : 受到伤害日志键;
            消息服务.发送战斗日志(快照可选.get(), 事件标识, 键, 来源名, 伤害数值);
        }
        附加上非吸收修正日志(快照可选.get(), 事件标识, 修正列表);
    }

    private void 附加上非吸收修正日志(玩家快照 受击快照, UUID 事件标识,
                                       List<mljy.业务层.消息.受击修正上下文.修正信息> 修正列表) {
        if (修正列表 == null || 修正列表.isEmpty()) {
            return;
        }
        for (mljy.业务层.消息.受击修正上下文.修正信息 修正 : 修正列表) {
            if (修正 == null) {
                continue;
            }
            if (修正.类型() == mljy.业务层.消息.受击修正上下文.修正类型.吸收) {
                continue;
            }
            if (修正.修正量() > 0) {
                String 修正数值 = String.format(数值格式, 修正.修正量());
                String 来源名 = 翻译服务.获取(修正.来源名称());
                if (来源名 == null || 来源名.isBlank() || 来源名.equals(修正.来源名称())) {
                    来源名 = 修正.来源名称();
                }
                消息服务.发送战斗日志(受击快照, 事件标识, 修正.获取翻译键(), 来源名,
                        修正数值);
            }
        }
    }

    protected String 获取技能目标名称(实体 目标) {
        if (目标 == null) {
            return "";
        }
        String 目标名 = 目标.获取名称();
        return 目标名 == null || 目标名.isBlank()
                ? ""
                : 颜色码工具.转换颜色码(目标名);
    }

    protected void 应用范围伤害(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害, int 衰减阈值) {
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            List<实体> 目标列表 = 目标选择服务.选择敌人(施法者, 中心, 半径);
            double 单目标伤害 = 应用Aoe衰减(基础伤害, 目标列表.size(), 衰减阈值);
            for (实体 目标 : 目标列表) {
                应用单体伤害(施法者, 目标, 单目标伤害, false);
            }
        } finally {
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害, int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量) {
        return 应用范围伤害并记录日志(施法者, 中心, 半径, 基础伤害, 衰减阈值, 技能名翻译键, 每目标秘能获取量, 每目标秘能获取量 > 0);
    }

    protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害, int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量, boolean 消耗秘兆) {
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            return 应用范围伤害并记录日志内部(
                    施法者, 中心, 半径, 基础伤害, 衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识, null, null);
        } finally {
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                        int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量,
                                        boolean 消耗秘兆, UUID 事件标识) {
        return 应用范围伤害并记录日志内部(
                施法者, 中心, 半径, 基础伤害, 衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识, null, null);
    }

    protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                        int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量,
                                        boolean 消耗秘兆, UUID 事件标识, String 自定义AOE日志键) {
        return 应用范围伤害并记录日志内部(
                施法者, 中心, 半径, 基础伤害, 衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识, 自定义AOE日志键, null);
    }

    protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                        int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量,
                                        boolean 消耗秘兆, UUID 事件标识, String 自定义AOE日志键, String 自定义AOE带资源日志键) {
        return 应用范围伤害并记录日志内部(
                施法者, 中心, 半径, 基础伤害, 衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识, 自定义AOE日志键, 自定义AOE带资源日志键);
    }

    private boolean 应用范围伤害并记录日志内部(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害, int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量, boolean 消耗秘兆, UUID 事件标识, String 自定义AOE日志键, String 自定义AOE带资源日志键) {
        List<实体> 目标列表 = 目标选择服务.选择敌人(施法者, 中心, 半径);
        if (目标列表 == null || 目标列表.isEmpty()) {
            调试日志器.调试("奥能法师技能基础", "范围伤害跳过：目标列表为空");
            return false;
        }
        double 单目标伤害 = 应用Aoe衰减(基础伤害, 目标列表.size(), 衰减阈值);
        double 总伤害 = 0;
        int 命中数 = 0;
        boolean 有暴击 = false;
        int 总秘能获取 = 0;
        boolean 获得秘兆 = false;
        boolean 施法前有秘兆 = 消耗秘兆 && 有秘兆效果(施法者);
        double 秘兆增伤 = 施法前有秘兆 ? 计算秘兆增伤(施法者, 单目标伤害) : 0;
        调试日志器.调试("奥能法师技能基础",
                "范围伤害开始：施法者=%s 目标数=%d 单目标伤害=%.1f 消耗秘兆=%s 施法前有秘兆=%s 秘兆增伤=%.1f 每目标秘能=%d",
                施法者.名称(), 目标列表.size(), 单目标伤害, 消耗秘兆, 施法前有秘兆, 秘兆增伤, 每目标秘能获取量);
        for (实体 目标 : 目标列表) {
            伤害结果 结果 = 应用单体伤害(施法者, 目标, 单目标伤害 + 秘兆增伤, false, false);
            if (结果 != null) {
                总伤害 += 结果.最终数值();
                命中数++;
                if (结果.是否暴击()) {
                    有暴击 = true;
                }
            }
            if (每目标秘能获取量 > 0) {
                boolean 本次获得秘兆 = 增加秘能(施法者, 每目标秘能获取量, false);
                调试日志器.调试("奥能法师技能基础", "范围伤害资源结算：施法者=%s 目标=%s 每目标秘能=%d 获得秘兆=%s",
                        施法者.名称(), 获取技能目标名称(目标), 每目标秘能获取量, 本次获得秘兆);
                if (本次获得秘兆) {
                    获得秘兆 = true;
                }
                总秘能获取 += 每目标秘能获取量;
            }
        }
        if (命中数 == 0) {
            调试日志器.调试("奥能法师技能基础", "范围伤害失败：目标均未命中");
            return false;
        }
        if (施法前有秘兆) {
            移除秘兆效果(施法者);
        }
        调试日志器.调试("奥能法师技能基础", "范围伤害结束：施法者=%s 命中数=%d 总伤害=%.1f 总秘能获取=%d 获得秘兆=%s 施法前秘兆已消费=%s",
                施法者.名称(), 命中数, 总伤害, 总秘能获取, 获得秘兆, 施法前有秘兆);
        String 槽位中文数字 = 查找槽位中文数字(技能名翻译键);
        String 伤害数值 = String.format(数值格式, 总伤害);
        String 目标数量 = String.valueOf(命中数);
        if (总秘能获取 > 0) {
            if (自定义AOE带资源日志键 != null && !自定义AOE带资源日志键.isEmpty()) {
                消息服务.发送技能日志(施法者, 事件标识, 自定义AOE带资源日志键, 槽位中文数字, 目标数量, 伤害数值, String.valueOf(总秘能获取));
            } else if (获得秘兆) {
                String 日志键 = 有暴击 ? 范围命中仅资源带秘兆带暴击日志键 : 范围命中仅资源带秘兆日志键;
                消息服务.发送技能日志(施法者, 事件标识, 日志键, 槽位中文数字, 目标数量, 伤害数值, String.valueOf(总秘能获取));
            } else {
                String 日志键 = 有暴击 ? 范围命中仅资源带暴击日志键 : 范围命中仅资源日志键;
                消息服务.发送技能日志(施法者, 事件标识, 日志键, 槽位中文数字, 目标数量, 伤害数值, String.valueOf(总秘能获取));
            }
        } else {
            String 日志键;
            if (自定义AOE日志键 != null && !自定义AOE日志键.isEmpty()) {
                日志键 = 自定义AOE日志键;
                消息服务.发送技能日志(施法者, 事件标识, 日志键, 槽位中文数字, 目标数量, 伤害数值);
            } else {
                日志键 = 有暴击 ? 范围暴击命中日志键 : 范围命中日志键;
                消息服务.发送技能日志(施法者, 事件标识, 日志键, 槽位中文数字, 目标数量, 伤害数值);
            }
        }
        return true;
    }

    protected 位置 获取前方位置(位置 起点, double 距离) {
        double 弧度 = Math.toRadians(起点.偏航角());
        double x = 起点.x() - Math.sin(弧度) * 距离;
        double z = 起点.z() + Math.cos(弧度) * 距离;
        return new 位置(起点.世界(), x, 起点.y(), z, 起点.偏航角(), 起点.俯仰角());
    }

    protected 位置 获取随机方向位置(位置 起点, double 距离) {
        double 弧度 = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double x = 起点.x() + Math.cos(弧度) * 距离;
        double z = 起点.z() + Math.sin(弧度) * 距离;
        return new 位置(起点.世界(), x, 起点.y(), z, 起点.偏航角(), 起点.俯仰角());
    }

    protected 位置 上方位置(位置 起点, double 高度) {
        return new 位置(起点.世界(), 起点.x(), 起点.y() + 高度, 起点.z(), 起点.偏航角(), 起点.俯仰角());
    }

    protected boolean 概率触发(double 概率) {
        return ThreadLocalRandom.current().nextDouble() < 概率;
    }
}
