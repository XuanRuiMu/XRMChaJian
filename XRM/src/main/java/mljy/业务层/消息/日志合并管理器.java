package mljy.业务层.消息;

import com.google.inject.Inject;
import mljy.翻译服务;
import mljy.基础设施层.事件日志上下文;
import mljy.基础设施层.颜色码工具;
import mljy.基础设施层.调试日志器;
import 暮澜纪元.通用.文本.关键词解析器;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 日志合并管理器。
 * 同一玩家、同一因果事件的所有日志按前缀类型分组合并，避免聊天栏刷屏。
 *
 * 合并规则：
 * - 合并键为玩家标识和因果事件标识
 * - 第一条日志到达后调度 1 tick（50ms）延迟任务发送合并日志
 * - 延迟窗口内到达的同键片段追加合并，延迟任务执行时一次性发送
 * - 不依赖事件完成回调，避免根事件永不结束时日志被永久卡住
 * - 战斗日志和技能日志合并为单行消息，只保留开头一个前缀，以战斗日志优先级为准
 * - 前缀后不保留空格（避免消息过长触发 Minecraft 客户端自动换行，导致前缀独占一行）
 * - 同一频道的行内条目直接拼接，无空格分隔，所有输出无换行
 */
public class 日志合并管理器 {
    private static final String 战斗日志前缀键 = "日志合并管理器.前缀.战斗日志";
    private static final String 技能日志前缀键 = "日志合并管理器.前缀.技能日志";
    private static final String 合并发送失败键 = "日志合并管理器.错误.发送失败";
    private static final String 玩家不在线键 = "日志合并管理器.警告.玩家不在线";
    private static final String 吸血日志键 = "战斗日志.吸血";
    private static final String 数值格式 = "%.1f";
    private static final Pattern MiniMessage换行标签 = Pattern.compile("(?i)<\\s*(?:newline|br)\\s*/?\\s*>");
    private static final Pattern Unicode换行 = Pattern.compile("[\\r\\n\\u000B\\u000C\\u0085\\u2028\\u2029]+");
    private static final Pattern 连续空白 = Pattern.compile("[ \\t\\x0B\\f]+");
    private static final long 合并窗口延迟tick = 1L;

    private final JavaPlugin 插件;
    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final MiniMessage 迷你消息 = MiniMessage.miniMessage();
    private final ConcurrentHashMap<合并键, 合并条目> 合并表 = new ConcurrentHashMap<>();
    private final Map<合并键, Double> 吸血累计表 = new ConcurrentHashMap<>();

    @Inject
    public 日志合并管理器(JavaPlugin 插件, 翻译服务 翻译服务, 关键词解析器 关键词解析器) {
        this.插件 = 插件;
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
    }

    private static UUID 默认事件标识() {
        UUID 事件标识 = 事件日志上下文.获取当前事件标识或空();
        return 事件标识 != null ? 事件标识 : UUID.randomUUID();
    }

    /**
     * 添加技能日志到合并队列。
     * 日志内容应已完成关键词解析，不含前缀。
     */
    public void 添加技能日志(UUID 玩家标识, String 内容) {
        添加技能日志(玩家标识, 默认事件标识(), 内容);
    }

    public void 添加技能日志(UUID 玩家标识, UUID 事件标识, String 内容) {
        if (内容 == null || 内容.isEmpty()) {
            调试日志器.调试("日志合并管理器", "拒绝添加技能日志：玩家=%s 事件=%s 原因=内容为空",
                    玩家标识, 事件标识);
            return;
        }
        合并键 键 = new 合并键(玩家标识, 事件标识 != null ? 事件标识 : UUID.randomUUID());
        String 单行内容 = 单行化(内容);
        if (单行内容.isEmpty()) {
            调试日志器.调试("日志合并管理器", "拒绝添加技能日志：玩家=%s 事件=%s 原因=单行化后为空 原内容=%s",
                    玩家标识, 键.事件标识, 内容);
            return;
        }
        调试日志器.调试("日志合并管理器",
                "已记录技能日志：玩家=%s 事件=%s 类型=技能日志 内容=%s 优先级=10",
                玩家标识, 键.事件标识, 单行内容);
        synchronized (合并表) {
            合并条目 条目 = 合并表.computeIfAbsent(键, k -> new 合并条目());
            条目.添加技能日志(单行内容);
            调度发送(键, 条目);
        }
    }

    /**
     * 添加战斗日志到合并队列。
     * 日志内容应已完成关键词解析和吸血附加，不含前缀。
     */
    public void 添加战斗日志(UUID 玩家标识, String 内容) {
        添加战斗日志(玩家标识, 内容, 10);
    }

    public void 添加战斗日志(UUID 玩家标识, UUID 事件标识, String 内容) {
        添加战斗日志(玩家标识, 事件标识, 内容, 10);
    }

    /**
     * 添加战斗日志到合并队列，指定优先级。
     * 优先级：0=战斗状态变化，10=普通攻击/伤害，20=技能释放公告。
     * 日志内容应已完成关键词解析和吸血附加，不含前缀。
     */
    public void 添加战斗日志(UUID 玩家标识, String 内容, int 优先级) {
        添加战斗日志(玩家标识, 默认事件标识(), 内容, 优先级);
    }

    public void 添加战斗日志(UUID 玩家标识, UUID 事件标识, String 内容, int 优先级) {
        if (内容 == null || 内容.isEmpty()) {
            调试日志器.调试("日志合并管理器", "拒绝添加战斗日志：玩家=%s 事件=%s 原因=内容为空",
                    玩家标识, 事件标识);
            return;
        }
        合并键 键 = new 合并键(玩家标识, 事件标识 != null ? 事件标识 : UUID.randomUUID());
        String 单行内容 = 单行化(内容);
        if (单行内容.isEmpty()) {
            调试日志器.调试("日志合并管理器", "拒绝添加战斗日志：玩家=%s 事件=%s 原因=单行化后为空 原内容=%s",
                    玩家标识, 键.事件标识, 内容);
            return;
        }
        调试日志器.调试("日志合并管理器",
                "已记录战斗日志：玩家=%s 事件=%s 类型=战斗日志 内容=%s 优先级=%d",
                玩家标识, 键.事件标识, 单行内容, 优先级);
        synchronized (合并表) {
            合并条目 条目 = 合并表.computeIfAbsent(键, k -> new 合并条目());
            // 根据优先级决定列表插入位置：0=最高优先（先输出），20=最低优先（后输出）
            条目.添加战斗日志(单行内容, 优先级);
            调度发送(键, 条目);
        }
    }

    private static String 单行化(String 内容) {
        if (内容 == null) {
            return "";
        }
        String 无标签换行 = MiniMessage换行标签.matcher(内容).replaceAll(" ");
        String 无物理换行 = Unicode换行.matcher(无标签换行).replaceAll(" ");
        return 连续空白.matcher(无物理换行).replaceAll(" ").trim();
    }

    private void 调度发送(合并键 键, 合并条目 条目) {
        if (条目.已调度.compareAndSet(false, true)) {
            调试日志器.调试("日志合并管理器",
                    "合并判定=延迟窗口调度 玩家=%s 事件=%s 原因=第一条日志触发 1 tick 后发送",
                    键.玩家标识, 键.事件标识);
            Bukkit.getScheduler().runTaskLater(插件, () -> 发送合并日志(键), 合并窗口延迟tick);
        } else {
            调试日志器.调试("日志合并管理器",
                    "合并判定=追加合并 玩家=%s 事件=%s 原因=同事件后续条目追加到既有合并组",
                    键.玩家标识, 键.事件标识);
        }
    }

    /**
     * 累加登记吸血尾缀数值：同一合并句（同玩家同事件）内多次吸血按数值求和，
     * 在合并句末尾统一输出唯一一条求和吸血日志（禁止换行、句号由翻译模板提供）。
     * 数值在合并发送时才做关键词解析与着色，避免每条伤害日志各自拼一条吸血。
     */
    public void 设置吸血尾缀(UUID 玩家标识, UUID 事件标识, double 吸血量) {
        if (吸血量 <= 0) {
            return;
        }
        合并键 键 = new 合并键(玩家标识, 事件标识 != null ? 事件标识 : UUID.randomUUID());
        吸血累计表.merge(键, 吸血量, Double::sum);
        调试日志器.调试("日志合并管理器",
                "已累加吸血尾缀：玩家=%s 事件=%s 增量=%.1f", 玩家标识, 键.事件标识, 吸血量);
    }

    private void 发送合并日志(合并键 键) {
        合并条目 条目;
        synchronized (合并表) {
            条目 = 合并表.remove(键);
        }
        if (条目 == null) {
            调试日志器.调试("日志合并管理器", "发送合并日志跳过：键=%s 条目为空 原因=已发送或被并发移除", 键);
            return;
        }
        UUID 玩家标识 = 键.玩家标识;
        Player 实体玩家 = Bukkit.getPlayer(玩家标识);
        if (实体玩家 == null || !实体玩家.isOnline()) {
            插件.getLogger().warning(翻译服务.获取(玩家不在线键, 玩家标识));
            调试日志器.调试("日志合并管理器", "发送合并日志丢弃：键=%s 原因=玩家不在线或不存在", 键);
            return;
        }
        List<String> 合并文本列表;
        try {
            合并文本列表 = 条目.合并(翻译服务);
        } catch (Exception e) {
            调试日志器.调试("日志合并管理器", "合并构建异常：玩家=%s 事件=%s", 键.玩家标识, 键.事件标识, e);
            插件.getLogger().warning(翻译服务.获取(合并发送失败键, 键, e.getMessage()));
            return;
        }
        if (合并文本列表.isEmpty()) {
            调试日志器.调试("日志合并管理器", "发送合并日志跳过：键=%s 原因=合并文本为空", 键);
            return;
        }
        double 吸血数值 = 吸血累计表.getOrDefault(键, 0.0);
        吸血累计表.remove(键);
        String 吸血尾缀 = "";
        if (吸血数值 > 0) {
            String 吸血模板 = 翻译服务.获取(吸血日志键);
            String 吸血文本 = 关键词解析器.解析(吸血模板, 玩家标识, String.format(数值格式, 吸血数值));
            吸血尾缀 = 单行化(吸血文本);
        }
        String 战斗前缀 = 单行化(翻译服务.获取(战斗日志前缀键));
        String 技能前缀 = 单行化(翻译服务.获取(技能日志前缀键));
        int 发送成功数 = 0;
        int 发送失败数 = 0;
        for (String 合并文本 : 合并文本列表) {
            String 最终单行文本 = 单行化(合并文本) + 吸血尾缀;
            if (最终单行文本.isEmpty()) {
                continue;
            }
            String 频道类型 = (!战斗前缀.isEmpty() && 最终单行文本.contains(战斗前缀)) ? "战斗日志"
                    : (!技能前缀.isEmpty() && 最终单行文本.contains(技能前缀)) ? "技能日志" : "未知";
            调试日志器.调试("日志合并管理器",
                    "发送合并日志：玩家=%s 事件=%s 频道=%s 文本=%s",
                    玩家标识, 键.事件标识, 频道类型, 最终单行文本);
            try {
                String 颜色文本 = 颜色码工具.转换颜色码(最终单行文本);
                net.kyori.adventure.text.Component 组件 = 迷你消息.deserialize(颜色文本);
                实体玩家.sendMessage(组件);
                发送成功数++;
            } catch (Exception e) {
                发送失败数++;
                调试日志器.调试("日志合并管理器", "合并发送异常：玩家=%s 事件=%s 文本=%s",
                        键.玩家标识, 键.事件标识, 最终单行文本, e);
                插件.getLogger().warning(翻译服务.获取(合并发送失败键, 键, e.getMessage()));
                实体玩家.sendMessage(net.kyori.adventure.text.Component.text(最终单行文本));
            }
        }
        调试日志器.调试("日志合并管理器",
                "发送合并日志结果：玩家=%s 事件=%s 行数=%d 成功=%d 失败=%d",
                键.玩家标识, 键.事件标识, 合并文本列表.size(), 发送成功数, 发送失败数);
    }

    /**
     * 合并条目，存储同一玩家、同一事件的战斗日志和技能日志。
     * 战斗日志按优先级升序排列：0=战斗状态变化，10=普通攻击/伤害（默认），20=技能释放公告。
     */
    private static class 合并条目 {
        private final List<日志条目> 条目列表 = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean 已调度 = new AtomicBoolean(false);

        void 添加技能日志(String 内容) {
            条目列表.add(new 日志条目(内容, 日志类型.技能日志));
        }

        void 添加战斗日志(String 内容) {
            条目列表.add(new 日志条目(内容, 日志类型.战斗日志));
        }

        void 添加战斗日志(String 内容, int 优先级) {
            条目列表.add(new 日志条目(内容, 日志类型.战斗日志, 优先级));
        }

        List<String> 合并(翻译服务 翻译服务) {
            synchronized (条目列表) {
                if (条目列表.isEmpty()) {
                    调试日志器.调试("日志合并管理器", "合并构建跳过：条目列表为空");
                    return List.of();
                }
                // 按优先级升序排序，同优先级保持到达顺序
                条目列表.sort((a, b) -> Integer.compare(a.优先级, b.优先级));

                String 战斗内容 = 构建类型内容(日志类型.战斗日志);
                String 技能内容 = 构建类型内容(日志类型.技能日志);
                int 战斗条目数 = (int) 条目列表.stream().filter(条目 -> 条目.类型 == 日志类型.战斗日志).count();
                int 技能条目数 = (int) 条目列表.stream().filter(条目 -> 条目.类型 == 日志类型.技能日志).count();
                调试日志器.调试("日志合并管理器",
                        "合并构建：战斗条目数=%d 技能条目数=%d 战斗内容长度=%d 技能内容长度=%d 前缀类型=合并为单行",
                        战斗条目数, 技能条目数, 战斗内容.length(), 技能内容.length());

                if (战斗内容.isEmpty() && 技能内容.isEmpty()) {
                    return List.of();
                }
                // 确定前缀：有战斗日志时用战斗日志前缀（优先级为准），否则用技能日志前缀
                String 前缀 = 单行化(!战斗内容.isEmpty()
                        ? 翻译服务.获取(战斗日志前缀键)
                        : 翻译服务.获取(技能日志前缀键));
                // 合并为单行：前缀 + 战斗内容 + 技能内容（不换行不加分隔空格）
                String 合并文本 = 前缀 + 战斗内容 + 技能内容;
                return List.of(合并文本);
            }
        }

        private String 构建类型内容(日志类型 类型) {
            StringBuilder 构建器 = new StringBuilder();
            for (日志条目 条目 : 条目列表) {
                if (条目.类型 == 类型) {
                    构建器.append(条目.内容.trim());
                }
            }
            return 构建器.toString();
        }

        private enum 日志类型 { 战斗日志, 技能日志 }

        private static class 日志条目 {
            final String 内容;
            final 日志类型 类型;
            final int 优先级;

            日志条目(String 内容, 日志类型 类型) {
                this.内容 = 内容;
                this.类型 = 类型;
                this.优先级 = 10;
            }

            日志条目(String 内容, 日志类型 类型, int 优先级) {
                this.内容 = 内容;
                this.类型 = 类型;
                this.优先级 = 优先级;
            }
        }
    }

    private record 合并键(UUID 玩家标识, UUID 事件标识) {
    }
}
