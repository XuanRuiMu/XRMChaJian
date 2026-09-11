package mljy.表现层.命令.生命条缩放指令;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.数据库.生命条缩放设置存储;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class 生命条缩放命令处理器 extends 抽象命令处理器 {
    private static final int 总体最小值 = 1;
    private static final int 总体最大值 = 100;
    private static final int 单体最小值 = 1;
    private static final int 单体最大值 = 1000;
    private static final int 默认总体红心数 = 20;
    private static final double 红心点数 = 2.0;
    private static final double 默认最大生命值 = 20.0;
    private static final String 默认总体红心数配置键 = "显示.生命条.默认总体红心数";
    private static final String 模块名 = "生命条缩放命令处理器";

    private static final String 总体模式键 = "生命条缩放指令.模式.总体个数";
    private static final String 单体模式键 = "生命条缩放指令.模式.单体个数";
    private static final String 当前设置键 = "生命条缩放指令.当前设置";
    private static final String 用法标题键 = "生命条缩放指令.用法.标题";
    private static final String 用法总体键 = "生命条缩放指令.用法.总体个数";
    private static final String 用法单体键 = "生命条缩放指令.用法.单体个数";
    private static final String 用法重置键 = "生命条缩放指令.用法.重置";
    private static final String 用法错误键 = "生命条缩放指令.用法.错误";
    private static final String 设置成功总体键 = "生命条缩放指令.设置成功.总体个数";
    private static final String 设置成功单体键 = "生命条缩放指令.设置成功.单体个数";
    private static final String 重置成功键 = "生命条缩放指令.重置成功";
    private static final String 无效数字键 = "生命条缩放指令.通用.无效数字";
    private static final String 范围错误总体键 = "生命条缩放指令.错误.数值范围.总体";
    private static final String 范围错误单体键 = "生命条缩放指令.错误.数值范围.单体";

    private static final String 子命令总体 = "总体个数";
    private static final String 子命令单体 = "单体个数";
    private static final String 子命令重置 = "重置";

    private final ConcurrentHashMap<UUID, 缩放设置> 玩家设置表 = new ConcurrentHashMap<>();
    private final JavaPlugin 插件;
    private final 生命条缩放设置存储 持久化存储;

    private record 缩放设置(String 模式, int 数值) {}

    @Inject
    public 生命条缩放命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器, JavaPlugin 插件,
                              生命条缩放设置存储 持久化存储) {
        super(翻译服务, 关键词解析器);
        this.插件 = 插件;
        this.持久化存储 = 持久化存储;
    }

    @Override
    public String 获取命令名() {
        return "生命条缩放";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (发送者 == null || 参数 == null) {
            调试日志器.调试(模块名, "请求异常：发送者或参数为空");
            return true;
        }
        调试日志器.调试(模块名, "命令请求：发送者类型=%s，参数=%s", 发送者.getClass().getSimpleName(), Arrays.toString(参数));
        if (!检查玩家(发送者)) {
            调试日志器.调试(模块名, "命令分支：非玩家发送者，拒绝执行");
            return true;
        }
        Player 玩家 = (Player) 发送者;

        if (参数.length == 0) {
            调试日志器.调试(模块名, "命令分支：无参数，返回当前设置或用法：玩家=%s", 玩家.getUniqueId());
            发送当前设置或用法(发送者, 玩家);
            return true;
        }

        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令总体 -> 处理总体个数(发送者, 玩家, 参数);
            case 子命令单体 -> 处理单体个数(发送者, 玩家, 参数);
            case 子命令重置 -> 处理重置(发送者, 玩家, 参数);
            default -> {
                调试日志器.调试(模块名, "命令分支：未知子命令=%s，玩家=%s", 子命令, 玩家.getUniqueId());
                发送错误(发送者, 用法错误键);
                yield true;
            }
        };
    }

    private void 发送当前设置或用法(CommandSender 发送者, Player 玩家) {
        缩放设置 设置 = 玩家设置表.get(玩家.getUniqueId());
        if (设置 == null) {
            发送用法(发送者);
            return;
        }
        String 模式文本 = 翻译服务.获取(设置.模式().equals(子命令总体) ? 总体模式键 : 单体模式键);
        发送消息(发送者, 当前设置键, 模式文本, 设置.数值());
    }

    private void 发送用法(CommandSender 发送者) {
        发送消息(发送者, 用法标题键);
        发送消息(发送者, 用法总体键);
        发送消息(发送者, 用法单体键);
        发送消息(发送者, 用法重置键, 获取默认总体红心数());
    }

    private boolean 处理总体个数(CommandSender 发送者, Player 玩家, String[] 参数) {
        if (参数.length < 2) {
            调试日志器.调试(模块名, "参数异常：总体个数参数不足，玩家=%s", 玩家.getUniqueId());
            发送错误(发送者, 参数不足键);
            return true;
        }
        if (参数.length > 2) {
            调试日志器.调试(模块名, "参数异常：总体个数存在多余参数，玩家=%s", 玩家.getUniqueId());
            发送错误(发送者, 用法错误键);
            return true;
        }
        Integer 数值 = 解析整数(参数[1]);
        if (数值 == null) {
            调试日志器.调试(模块名, "参数异常：总体个数不是整数，玩家=%s，输入=%s", 玩家.getUniqueId(), 参数[1]);
            发送错误(发送者, 无效数字键, 参数[1]);
            return true;
        }
        if (数值 < 总体最小值 || 数值 > 总体最大值) {
            调试日志器.调试(模块名, "参数异常：总体个数越界，玩家=%s，输入=%d", 玩家.getUniqueId(), 数值);
            发送错误(发送者, 范围错误总体键);
            return true;
        }
        double 缩放值 = 数值 * 红心点数;
        if (!应用缩放值(玩家, 缩放值, "总体个数")) {
            发送错误(发送者, 未知错误键);
            return true;
        }
        玩家设置表.put(玩家.getUniqueId(), new 缩放设置(子命令总体, 数值));
        持久化存储.保存(玩家.getUniqueId(), 子命令总体, 数值);
        发送消息(发送者, 设置成功总体键, 数值);
        return true;
    }

    private boolean 处理单体个数(CommandSender 发送者, Player 玩家, String[] 参数) {
        if (参数.length < 2) {
            调试日志器.调试(模块名, "参数异常：单体个数参数不足，玩家=%s", 玩家.getUniqueId());
            发送错误(发送者, 参数不足键);
            return true;
        }
        if (参数.length > 2) {
            调试日志器.调试(模块名, "参数异常：单体个数存在多余参数，玩家=%s", 玩家.getUniqueId());
            发送错误(发送者, 用法错误键);
            return true;
        }
        Integer 数值 = 解析整数(参数[1]);
        if (数值 == null) {
            调试日志器.调试(模块名, "参数异常：单体个数不是整数，玩家=%s，输入=%s", 玩家.getUniqueId(), 参数[1]);
            发送错误(发送者, 无效数字键, 参数[1]);
            return true;
        }
        if (数值 < 单体最小值 || 数值 > 单体最大值) {
            调试日志器.调试(模块名, "参数异常：单体个数越界，玩家=%s，输入=%d", 玩家.getUniqueId(), 数值);
            发送错误(发送者, 范围错误单体键);
            return true;
        }
        double 最大生命值 = 获取最大生命值(玩家);
        double 缩放值 = Math.max(最大生命值 * 红心点数 / 数值, 红心点数);
        if (!应用缩放值(玩家, 缩放值, "单体个数")) {
            发送错误(发送者, 未知错误键);
            return true;
        }
        玩家设置表.put(玩家.getUniqueId(), new 缩放设置(子命令单体, 数值));
        持久化存储.保存(玩家.getUniqueId(), 子命令单体, 数值);
        发送消息(发送者, 设置成功单体键, 数值);
        return true;
    }

    private boolean 处理重置(CommandSender 发送者, Player 玩家, String[] 参数) {
        if (参数.length != 1) {
            调试日志器.调试(模块名, "参数异常：重置存在多余参数，玩家=%s", 玩家.getUniqueId());
            发送错误(发送者, 用法错误键);
            return true;
        }
        调试日志器.调试(模块名, "状态变更：清除自定义设置并应用默认：玩家=%s", 玩家.getUniqueId());
        玩家设置表.remove(玩家.getUniqueId());
        持久化存储.删除(玩家.getUniqueId());
        if (!应用默认缩放(玩家)) {
            发送错误(发送者, 未知错误键);
            return true;
        }
        发送消息(发送者, 重置成功键, 获取默认总体红心数());
        return true;
    }

    public boolean 应用持久化设置(Player 玩家) {
        if (玩家 == null) {
            调试日志器.调试(模块名, "请求异常：应用持久化设置的玩家为空");
            return false;
        }
        调试日志器.调试(模块名, "应用持久化设置入口：玩家=%s, 连接池状态=[%s]",
                玩家.getUniqueId(), 持久化存储.获取连接池状态());
        if (!持久化存储.连接池已启用()) {
            调试日志器.调试(模块名, "应用持久化设置警告：数据库连接池未启用，加载大概率返回空，将由调用方回退默认缩放，玩家=%s", 玩家.getUniqueId());
        }
        Optional<生命条缩放设置存储.缩放设置> 持久化 = 持久化存储.加载(玩家.getUniqueId());
        if (持久化.isEmpty()) {
            调试日志器.调试(模块名, "持久化加载：无记录（连接池未启用/SQL失败/无记录），玩家=%s", 玩家.getUniqueId());
            return false;
        }
        生命条缩放设置存储.缩放设置 设置 = 持久化.get();
        double 缩放值;
        if (子命令总体.equals(设置.模式())) {
            缩放值 = 设置.数值() * 红心点数;
            调试日志器.调试(模块名, "持久化计算：模式=总体个数, 数值=%d, 缩放值=%.1f, 玩家=%s",
                    设置.数值(), 缩放值, 玩家.getUniqueId());
        } else if (子命令单体.equals(设置.模式())) {
            double 最大生命值 = 获取最大生命值(玩家);
            缩放值 = Math.max(最大生命值 * 红心点数 / 设置.数值(), 红心点数);
            调试日志器.调试(模块名, "持久化计算：模式=单体个数, 数值=%d, 最大生命值=%.1f, 缩放值=%.1f, 玩家=%s",
                    设置.数值(), 最大生命值, 缩放值, 玩家.getUniqueId());
        } else {
            调试日志器.调试(模块名, "持久化加载失败：未知模式=%s，玩家=%s", 设置.模式(), 玩家.getUniqueId());
            return false;
        }
        if (!应用缩放值(玩家, 缩放值, "持久化加载")) {
            return false;
        }
        玩家设置表.put(玩家.getUniqueId(), new 缩放设置(设置.模式(), 设置.数值()));
        调试日志器.调试(模块名, "持久化应用成功：玩家=%s，模式=%s，数值=%d",
                玩家.getUniqueId(), 设置.模式(), 设置.数值());
        return true;
    }

    public boolean 应用默认缩放(Player 玩家) {
        if (玩家 == null) {
            调试日志器.调试(模块名, "请求异常：默认缩放的玩家为空");
            return false;
        }
        int 总体红心数 = 获取默认总体红心数();
        double 缩放值 = 总体红心数 * 红心点数;
        调试日志器.调试(模块名, "默认应用入口：玩家=%s，总体红心数=%d，healthScale=%.1f, 连接池状态=[%s]",
                玩家.getUniqueId(), 总体红心数, 缩放值, 持久化存储.获取连接池状态());
        玩家设置表.remove(玩家.getUniqueId());
        return 应用缩放值(玩家, 缩放值, "默认");
    }

    public void 清理玩家设置(UUID 玩家标识) {
        if (玩家标识 == null) {
            调试日志器.调试(模块名, "请求异常：清理设置的玩家标识为空");
            return;
        }
        boolean 已清理 = 玩家设置表.remove(玩家标识) != null;
        调试日志器.调试(模块名, "生命周期清理：玩家=%s，是否存在自定义设置=%s", 玩家标识, 已清理);
    }

    public void 清理全部设置() {
        int 清理数量 = 玩家设置表.size();
        玩家设置表.clear();
        调试日志器.调试(模块名, "生命周期清理：清空全部内存设置，数量=%d", 清理数量);
    }

    private boolean 应用缩放值(Player 玩家, double 缩放值, String 来源) {
        if (玩家 == null) {
            调试日志器.调试(模块名, "请求异常：应用缩放的玩家为空，来源=%s", 来源);
            return false;
        }
        if (!Double.isFinite(缩放值) || 缩放值 < 红心点数) {
            调试日志器.调试(模块名, "状态异常：缩放值非法，来源=%s，玩家=%s，healthScale=%.3f",
                    来源, 玩家.getUniqueId(), 缩放值);
            return false;
        }
        try {
            double 应用前缩放 = 玩家.getHealthScale();
            boolean 应用前缩放开关 = 玩家.isHealthScaled();
            调试日志器.调试(模块名, "应用缩放前状态：来源=%s，玩家=%s，当前healthScale=%.1f，isHealthScaled=%s，目标healthScale=%.1f",
                    来源, 玩家.getUniqueId(), 应用前缩放, 应用前缩放开关, 缩放值);
            玩家.setHealthScale(缩放值);
            玩家.setHealthScaled(true);
            double 应用后缩放 = 玩家.getHealthScale();
            boolean 应用后缩放开关 = 玩家.isHealthScaled();
            调试日志器.调试(模块名, "结果：应用缩放成功，来源=%s，玩家=%s，healthScale=%.1f→%.1f，isHealthScaled=%s→%s",
                    来源, 玩家.getUniqueId(), 应用前缩放, 应用后缩放, 应用前缩放开关, 应用后缩放开关);
            return true;
        } catch (RuntimeException 异常) {
            调试日志器.调试(模块名, "异常：应用缩放失败，来源=" + 来源 + "，玩家=" + 玩家.getUniqueId()
                    + "，异常类型=" + 异常.getClass().getSimpleName() + "，异常消息=" + 异常.getMessage(), 异常);
            return false;
        }
    }

    private int 获取默认总体红心数() {
        try {
            int 配置值 = 插件.getConfig().getInt(默认总体红心数配置键, 默认总体红心数);
            if (配置值 >= 总体最小值 && 配置值 <= 总体最大值) {
                return 配置值;
            }
            调试日志器.调试(模块名, "配置异常：%s=%d 越界，回退到默认值=%d",
                    默认总体红心数配置键, 配置值, 默认总体红心数);
        } catch (RuntimeException 异常) {
            调试日志器.调试(模块名, "配置异常：读取" + 默认总体红心数配置键 + "失败，回退到默认值=" + 默认总体红心数, 异常);
        }
        return 默认总体红心数;
    }

    private double 获取最大生命值(Player 玩家) {
        AttributeInstance 属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        double 最大生命值 = 属性 != null ? 属性.getValue() : 默认最大生命值;
        if (!Double.isFinite(最大生命值) || 最大生命值 <= 0) {
            调试日志器.调试(模块名, "属性异常：最大生命值非法，玩家=%s，值=%.3f，回退到默认值=%.1f",
                    玩家.getUniqueId(), 最大生命值, 默认最大生命值);
            return 默认最大生命值;
        }
        return 最大生命值;
    }

    private Integer 解析整数(String 文本) {
        try {
            return Integer.parseInt(文本);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令总体, 子命令单体, 子命令重置);
        }
        return Collections.emptyList();
    }
}
