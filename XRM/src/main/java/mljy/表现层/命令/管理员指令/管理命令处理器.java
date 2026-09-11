package mljy.表现层.命令.管理员指令;

import com.google.inject.Inject;
import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.仇恨服务;
import mljy.业务层.仇恨服务.仇恨信息条目;
import mljy.业务层.战斗状态服务;
import mljy.业务层.技能冷却服务;
import mljy.业务层.公共冷却显示服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.效果注册服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.数据服务;
import mljy.基础设施层.Yaml配置加载器;
import mljy.属性服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.属性快照;
import mljy.领域层.属性.属性元数据;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.效果.效果定义;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.技能.参数读取器;
import mljy.领域层.技能.参数注册表;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.技能执行结果;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 管理命令处理器。
 * 实现 /管理 <子命令> 系列管理员调试指令。
 * 权限：xrm.op
 * 子命令：仇恨/战斗/等级/职业/属性/重载/刷新/技能
 */
public class 管理命令处理器 extends 抽象命令处理器 {
    private static final String 管理权限 = "xrm.op";

    private static final String 子命令仇恨 = "仇恨";
    private static final String 子命令战斗 = "战斗";
    private static final String 子命令等级 = "等级";
    private static final String 子命令职业 = "职业";
    private static final String 子命令属性 = "属性";
    private static final String 移速命令名 = "移速";
    private static final String 子命令重载 = "重载";
    private static final String 子命令刷新 = "刷新";
    private static final String 子命令技能 = "技能";

    private static final String 冷却关键词 = "冷却";
    private static final String 重置关键词 = "重置";
    private static final String 参数关键词 = "参数";

    private static final String 操作调试 = "调试";
    private static final String 操作清空 = "清空";
    private static final String 操作信息 = "信息";
    private static final String 操作列表 = "列表";
    private static final String 开 = "开";
    private static final String 关 = "关";

    private static final String 职业类型配置文件 = "职业类型.yml";
    private static final String 职业类型根键 = "职业类型";

    private static final String 修饰器标签后缀 = "_管理临时_";
    private static final int 修饰器默认优先级 = 0;
    private static final long 每秒Tick = 20L;

    private static final String 数值格式 = "%.1f";

    private static final String 生命值命令名 = "生命值";

    private final 玩家服务 玩家服务;
    private final 数据服务 数据服务;
    private final 属性服务 属性服务;
    private final 修饰器管理器 修饰器管理器;
    private final 仇恨服务 仇恨服务;
    private final 战斗状态服务 战斗状态服务;
    private final JavaPlugin 插件;
    private final Yaml配置加载器 配置加载器;
    private final 属性注册表 属性注册表;
    private final 技能注册服务 技能注册服务;
    private final 效果注册服务 效果注册服务;
    private final 技能冷却服务 技能冷却服务;
    private final 公共冷却显示服务 公共冷却显示服务;
    private final 效果调度服务 效果调度服务;
    private final 技能释放服务 技能释放服务;
    private final 参数注册表 参数注册表;
    private final Logger 日志器;

    private final Map<UUID, Map<String, AtomicInteger>> 临时效果序号表 = new ConcurrentHashMap<>();

    @Inject
    public 管理命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器,
                         玩家服务 玩家服务, 数据服务 数据服务, 属性服务 属性服务,
                         修饰器管理器 修饰器管理器, 仇恨服务 仇恨服务,
                         战斗状态服务 战斗状态服务, JavaPlugin 插件,
                         Yaml配置加载器 配置加载器, 属性注册表 属性注册表,
                         技能注册服务 技能注册服务, 效果注册服务 效果注册服务,
                         技能冷却服务 技能冷却服务, 公共冷却显示服务 公共冷却显示服务, 效果调度服务 效果调度服务,
                         技能释放服务 技能释放服务, 参数注册表 参数注册表) {
        super(翻译服务, 关键词解析器);
        this.玩家服务 = 玩家服务;
        this.数据服务 = 数据服务;
        this.属性服务 = 属性服务;
        this.修饰器管理器 = 修饰器管理器;
        this.仇恨服务 = 仇恨服务;
        this.战斗状态服务 = 战斗状态服务;
        this.插件 = 插件;
        this.配置加载器 = 配置加载器;
        this.属性注册表 = 属性注册表;
        this.技能注册服务 = 技能注册服务;
        this.效果注册服务 = 效果注册服务;
        this.技能冷却服务 = 技能冷却服务;
        this.公共冷却显示服务 = 公共冷却显示服务;
        this.效果调度服务 = 效果调度服务;
        this.技能释放服务 = 技能释放服务;
        this.参数注册表 = 参数注册表;
        this.日志器 = 插件.getLogger();
    }

    @Override
    public String 获取命令名() {
        return "管理";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查权限(发送者, 管理权限)) {
            return true;
        }
        if (参数.length == 0) {
            发送帮助(发送者);
            return true;
        }

        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令仇恨 -> 处理仇恨(发送者, 参数);
            case 子命令战斗 -> 处理战斗(发送者, 参数);
            case 子命令等级 -> 处理等级(发送者, 参数);
            case 子命令职业 -> 处理职业(发送者, 参数);
            case 子命令属性 -> 处理属性(发送者, 参数);
            case 子命令重载 -> 处理重载(发送者);
            case 子命令刷新 -> 处理刷新(发送者, 参数);
            case 子命令技能 -> 处理技能(发送者, 参数);
            default -> {
                发送帮助(发送者);
                yield true;
            }
        };
    }

    private void 发送帮助(CommandSender 发送者) {
        发送消息(发送者, "管理员指令.帮助.标题");
        发送消息(发送者, "管理员指令.帮助.仇恨说明");
        发送消息(发送者, "管理员指令.帮助.战斗说明");
        发送消息(发送者, "管理员指令.帮助.等级说明");
        发送消息(发送者, "管理员指令.帮助.职业说明");
        发送消息(发送者, "管理员指令.帮助.职业列表说明");
        发送消息(发送者, "管理员指令.帮助.属性说明");
        发送消息(发送者, "管理员指令.帮助.属性注");
        发送消息(发送者, "管理员指令.帮助.重载说明");
        发送消息(发送者, "管理员指令.帮助.刷新说明");
        发送消息(发送者, "管理员指令.帮助.技能说明");
    }

    private boolean 处理仇恨(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, "管理员指令.仇恨.用法");
            发送消息(发送者, "管理员指令.仇恨.用法调试");
            发送消息(发送者, "管理员指令.仇恨.用法清空");
            发送消息(发送者, "管理员指令.仇恨.用法信息");
            return true;
        }

        String 操作 = 参数[1];
        return switch (操作) {
            case 操作调试 -> 处理仇恨调试(发送者, 参数);
            case 操作清空 -> 处理仇恨清空(发送者, 参数);
            case 操作信息 -> 处理仇恨信息(发送者, 参数);
            default -> {
                发送错误(发送者, "管理员指令.仇恨.错误.未知操作", 操作);
                yield true;
            }
        };
    }

    private boolean 处理仇恨调试(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, "管理员指令.仇恨.调试.用法");
            return true;
        }
        String 开关值 = 参数[2];
        if (开.equals(开关值)) {
            仇恨服务.设置调试模式(true);
            发送消息(发送者, "管理员指令.仇恨.调试.开启成功");
        } else if (关.equals(开关值)) {
            仇恨服务.设置调试模式(false);
            发送消息(发送者, "管理员指令.仇恨.调试.关闭成功");
        } else {
            发送错误(发送者, "管理员指令.仇恨.调试.错误.无效开关", 开关值);
        }
        return true;
    }

    private boolean 处理仇恨清空(CommandSender 发送者, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }
        Player 玩家 = (Player) 发送者;
        double 范围 = 0;
        if (参数.length >= 3) {
            Double 解析值 = 解析双精度(参数[2]);
            if (解析值 == null) {
                发送错误(发送者, "管理员指令.仇恨.清空.错误.范围非数字", 参数[2]);
                return true;
            }
            if (解析值 <= 0) {
                发送错误(发送者, "管理员指令.仇恨.清空.错误.范围必须大于0");
                return true;
            }
            范围 = 解析值;
        }
        int 清除数量 = 仇恨服务.清除范围仇恨(玩家.getUniqueId(), 范围);
        double 显示范围 = 范围 > 0 ? 范围 : 获取默认仇恨清空范围();
        发送消息(发送者, "管理员指令.仇恨.清空.成功", String.format(数值格式, 显示范围), 清除数量);
        return true;
    }

    private boolean 处理仇恨信息(CommandSender 发送者, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }
        Player 玩家 = (Player) 发送者;
        double 范围 = 0;
        if (参数.length >= 3) {
            Double 解析值 = 解析双精度(参数[2]);
            if (解析值 == null) {
                发送错误(发送者, "管理员指令.仇恨.信息.错误.范围非数字", 参数[2]);
                return true;
            }
            if (解析值 <= 0) {
                发送错误(发送者, "管理员指令.仇恨.信息.错误.范围必须大于0");
                return true;
            }
            范围 = 解析值;
        }
        List<仇恨信息条目> 信息列表 = 仇恨服务.获取附近仇恨信息(玩家.getUniqueId(), 范围);
        double 显示范围 = 范围 > 0 ? 范围 : 获取默认仇恨清空范围();
        if (信息列表.isEmpty()) {
            发送消息(发送者, "管理员指令.仇恨.信息.无怪物", String.format(数值格式, 显示范围));
            return true;
        }
        发送消息(发送者, "管理员指令.仇恨.信息.标题", String.format(数值格式, 显示范围));
        int 怪物计数 = 0;
        for (仇恨信息条目 条目 : 信息列表) {
            怪物计数++;
            String 怪物标识文本 = 条目.怪物名() + " (" + 条目.怪物标识() + ")";
            发送消息(发送者, "管理员指令.仇恨.信息.怪物行", 怪物标识文本);
            for (Map.Entry<String, Double> 仇恨条目 : 条目.玩家仇恨表().entrySet()) {
                发送消息(发送者, "管理员指令.仇恨.信息.仇恨行",
                        仇恨条目.getKey(), String.format(数值格式, 仇恨条目.getValue()));
            }
        }
        if (怪物计数 == 0) {
            发送消息(发送者, "管理员指令.仇恨.信息.无仇恨");
        } else {
            发送消息(发送者, "管理员指令.仇恨.信息.统计", 怪物计数);
        }
        return true;
    }

    private boolean 处理战斗(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, "管理员指令.战斗.用法");
            发送消息(发送者, "管理员指令.战斗.用法调试");
            发送消息(发送者, "管理员指令.战斗.用法列表");
            return true;
        }

        String 操作 = 参数[1];
        return switch (操作) {
            case 操作调试 -> 处理战斗调试(发送者, 参数);
            case 操作列表 -> 处理战斗列表(发送者);
            default -> {
                发送错误(发送者, "管理员指令.战斗.错误.未知操作", 操作);
                yield true;
            }
        };
    }

    private boolean 处理战斗调试(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, "管理员指令.战斗.调试.用法");
            return true;
        }
        String 开关值 = 参数[2];
        if (开.equals(开关值)) {
            战斗状态服务.设置调试模式(true);
            发送消息(发送者, "管理员指令.战斗.调试.开启成功");
        } else if (关.equals(开关值)) {
            战斗状态服务.设置调试模式(false);
            发送消息(发送者, "管理员指令.战斗.调试.关闭成功");
        } else {
            发送错误(发送者, "管理员指令.战斗.调试.错误.无效开关", 开关值);
        }
        return true;
    }

    private boolean 处理战斗列表(CommandSender 发送者) {
        List<UUID> 战斗中玩家 = 战斗状态服务.获取战斗中玩家列表();
        if (战斗中玩家.isEmpty()) {
            发送消息(发送者, "管理员指令.战斗.列表.无玩家");
            return true;
        }
        发送消息(发送者, "管理员指令.战斗.列表.标题", 战斗中玩家.size());
        for (UUID 标识 : 战斗中玩家) {
            Player 玩家 = Bukkit.getPlayer(标识);
            String 名称 = 玩家 != null ? 玩家.getName() : 标识.toString();
            发送消息(发送者, "管理员指令.战斗.列表.玩家行", 名称);
        }
        return true;
    }

    private boolean 处理等级(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, "管理员指令.等级.用法");
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayer(参数[1]);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 参数[1]);
            return true;
        }
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(目标玩家.getUniqueId());
        if (会话可选.isEmpty()) {
            发送错误(发送者, 会话不存在键);
            return true;
        }
        玩家会话 会话 = 会话可选.get();

        if (参数.length < 3) {
            发送消息(发送者, "管理员指令.等级.查看", 目标玩家.getName(), 会话.获取等级(), 会话.获取专精());
            return true;
        }

        Integer 新等级 = 解析整数(参数[2]);
        if (新等级 == null) {
            发送错误(发送者, "管理员指令.等级.错误.非数字");
            return true;
        }
        if (新等级 <= 0) {
            发送错误(发送者, "管理员指令.等级.错误.必须大于0");
            return true;
        }
        int 旧等级 = 会话.获取等级();
        会话.设置等级(新等级);
        数据服务.保存(会话);
        属性服务.刷新属性(目标玩家.getUniqueId());
        发送消息(发送者, "管理员指令.等级.设置成功", 目标玩家.getName(), 旧等级, 新等级);
        发送消息(目标玩家, "管理员指令.等级.被设置通知", 新等级);
        return true;
    }

    private boolean 处理职业(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, "管理员指令.职业.用法");
            发送消息(发送者, "管理员指令.职业.提示.查看列表");
            return true;
        }

        if (操作列表.equals(参数[1])) {
            return 处理职业列表(发送者);
        }

        if (参数.length < 3) {
            发送错误(发送者, "管理员指令.职业.用法");
            return true;
        }
        String 目标玩家名 = 参数[1];
        String 职业名 = 参数[2];

        List<String> 可用职业 = 获取可用职业列表();
        if (!可用职业.contains(职业名)) {
            发送错误(发送者, "管理员指令.职业.错误.无效职业", 职业名);
            发送消息(发送者, "管理员指令.职业.提示.查看列表");
            return true;
        }

        Player 目标玩家 = Bukkit.getPlayer(目标玩家名);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 目标玩家名);
            return true;
        }
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(目标玩家.getUniqueId());
        if (会话可选.isEmpty()) {
            发送错误(发送者, 会话不存在键);
            return true;
        }
        玩家会话 会话 = 会话可选.get();
        String 旧职业 = 会话.获取专精();
        会话.设置专精(职业名);
        数据服务.保存(会话);
        属性服务.刷新属性(目标玩家.getUniqueId());
        发送消息(发送者, "管理员指令.职业.设置成功", 目标玩家.getName(), 旧职业, 职业名);
        发送消息(目标玩家, "管理员指令.职业.被设置通知", 职业名);
        return true;
    }

    private boolean 处理职业列表(CommandSender 发送者) {
        List<String> 可用职业 = 获取可用职业列表();
        发送消息(发送者, "管理员指令.职业.列表标题");
        for (String 职业 : 可用职业) {
            发送消息(发送者, "管理员指令.职业.列表项", 职业);
        }
        return true;
    }

    private boolean 处理属性(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, "管理员指令.属性.用法");
            发送消息(发送者, "管理员指令.属性.用法补充");
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayer(参数[1]);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 参数[1]);
            return true;
        }

        if (参数.length == 2) {
            return 处理属性查看(发送者, 目标玩家);
        }
        return 处理属性添加临时(发送者, 目标玩家, 参数);
    }

    private boolean 处理属性查看(CommandSender 发送者, Player 目标玩家) {
        属性快照 属性 = 属性服务.计算属性(目标玩家.getUniqueId());
        发送消息(发送者, "管理员指令.属性.查看.标题", 目标玩家.getName());
        for (属性元数据 元数据 : 属性注册表.获取所有元数据()) {
            if (生命值命令名.equals(元数据.命令名())) {
                发送消息(发送者, 元数据.显示翻译键(),
                        String.format(数值格式, 目标玩家.getHealth()),
                        String.format(数值格式, 元数据.取值(属性)));
            } else {
                String 显示格式 = 移速命令名.equals(元数据.命令名()) ? "%.2f" : 数值格式;
                发送消息(发送者, 元数据.显示翻译键(), String.format(显示格式, 元数据.取值(属性)));
            }
        }
        return true;
    }

    private boolean 处理属性添加临时(CommandSender 发送者, Player 目标玩家, String[] 参数) {
        if (参数.length < 5) {
            发送错误(发送者, "管理员指令.属性.用法");
            发送消息(发送者, "管理员指令.属性.用法补充");
            return true;
        }
        String 属性名 = 参数[2];
        Optional<属性元数据> 元数据可选 = 属性注册表.查询(属性名);
        if (元数据可选.isEmpty()) {
            发送错误(发送者, "管理员指令.属性.错误.无效属性名", 属性名);
            String 可用列表 = String.join("、", 属性注册表.获取所有命令名());
            发送消息(发送者, "管理员指令.属性.可用属性", 可用列表);
            return true;
        }

        Double 数值 = 解析双精度(参数[3]);
        if (数值 == null) {
            发送错误(发送者, "管理员指令.属性.错误.数值非数字", 参数[3]);
            return true;
        }

        Double 秒数 = 解析双精度(参数[4]);
        if (秒数 == null) {
            发送错误(发送者, "管理员指令.属性.错误.秒数非数字", 参数[4]);
            return true;
        }
        if (秒数 <= 0) {
            发送错误(发送者, "管理员指令.属性.错误.持续时间必须大于0");
            return true;
        }

        String 类别 = 元数据可选.get().修饰器类别();
        int 序号 = 获取下一个序号(目标玩家.getUniqueId(), 类别);
        String 标签 = 类别 + 修饰器标签后缀 + 序号;

        修饰器 修饰器 = new 修饰器(类别, 标签, 修饰方式.相加, 数值, 修饰器默认优先级);
        修饰器管理器.注册修饰器(目标玩家.getUniqueId(), 修饰器);
        属性服务.刷新属性(目标玩家.getUniqueId());

        调度修饰器移除(目标玩家.getUniqueId(), 类别, 标签, 秒数);

        String 属性显示名 = 翻译服务.获取("管理员指令.属性名." + 属性名);
        发送消息(发送者, "管理员指令.属性.添加成功", 目标玩家.getName());
        发送消息(发送者, "管理员指令.属性.效果ID标签", 标签);
        发送消息(发送者, "管理员指令.属性.详情",
                属性显示名, 格式化临时属性数值(属性名, 数值), String.format("%.0f", 秒数));
        return true;
    }

    private String 格式化临时属性数值(String 属性名, double 数值) {
        if (移速命令名.equals(属性名)) {
            return String.format("%.2f", 数值);
        }
        return String.format("%.0f", 数值);
    }

    private boolean 处理重载(CommandSender 发送者) {
        插件.reloadConfig();
        属性服务.重载();
        日志器.info("插件配置已由管理员重载");
        发送消息(发送者, "管理员指令.通用.重载完成");
        return true;
    }

    private boolean 处理刷新(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, "管理员指令.刷新.用法");
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayer(参数[1]);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 参数[1]);
            return true;
        }
        属性服务.刷新属性(目标玩家.getUniqueId());
        技能冷却服务.重置所有冷却(目标玩家.getUniqueId());
        技能冷却服务.重置公共冷却(目标玩家.getUniqueId());
        公共冷却显示服务.清理(目标玩家.getUniqueId());
        发送消息(发送者, "管理员指令.刷新.成功", 目标玩家.getName());
        发送消息(目标玩家, "管理员指令.刷新.被刷新通知");
        return true;
    }

    private boolean 处理技能(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送技能帮助(发送者);
            return true;
        }
        String 第二参数 = 参数[1];
        if (冷却关键词.equals(第二参数)) {
            return 处理技能冷却(发送者, 参数);
        }
        if (参数关键词.equals(第二参数)) {
            return 处理技能参数查询(发送者, 参数);
        }
        return 处理技能释放或施加(发送者, 参数);
    }

    private void 发送技能帮助(CommandSender 发送者) {
        发送消息(发送者, "管理员指令.技能.帮助.标题");
        发送消息(发送者, "管理员指令.技能.帮助.释放");
        发送消息(发送者, "管理员指令.技能.帮助.冷却重置");
        发送消息(发送者, "管理员指令.技能.帮助.参数查询");
    }

    private boolean 处理技能冷却(CommandSender 发送者, String[] 参数) {
        if (参数.length < 4 || !重置关键词.equals(参数[2])) {
            发送错误(发送者, "管理员指令.技能.冷却.用法");
            return true;
        }
        String 玩家名 = 参数[3];
        Player 目标玩家 = Bukkit.getPlayer(玩家名);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 玩家名);
            return true;
        }
        UUID 玩家标识 = 目标玩家.getUniqueId();
        if (参数.length >= 5) {
            String 技能ID = 参数[4];
            技能冷却服务.重置冷却(玩家标识, 技能ID);
            技能冷却服务.重置公共冷却(玩家标识);
            发送消息(发送者, "管理员指令.技能.冷却.重置单个成功", 目标玩家.getName(), 技能ID);
        } else {
            技能冷却服务.重置所有冷却(玩家标识);
            发送消息(发送者, "管理员指令.技能.冷却.重置全部成功", 目标玩家.getName());
        }
        return true;
    }

    private boolean 处理技能参数查询(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, "管理员指令.技能.参数.用法");
            return true;
        }
        String 技能或效果ID = 参数[2];
        List<参数注册表.参数条目> 参数列表 = 参数注册表.获取参数列表(技能或效果ID);
        if (参数列表.isEmpty()) {
            发送消息(发送者, "管理员指令.技能.参数.无参数");
            return true;
        }
        发送消息(发送者, "管理员指令.技能.参数.列表标题", 技能或效果ID);
        for (参数注册表.参数条目 条目 : 参数列表) {
            发送消息(发送者, "管理员指令.技能.参数.参数行",
                    条目.参数名(), 条目.默认值(), 条目.描述());
        }
        return true;
    }

    private boolean 处理技能释放或施加(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, "管理员指令.技能.释放.用法");
            return true;
        }
        String 技能或效果ID = 参数[1];
        String 玩家名 = 参数[2];
        Player 目标玩家 = Bukkit.getPlayer(玩家名);
        if (目标玩家 == null) {
            发送错误(发送者, "管理员指令.通用.不在线", 玩家名);
            return true;
        }
        UUID 玩家标识 = 目标玩家.getUniqueId();

        Optional<技能定义> 技能定义Opt = 技能注册服务.获取定义(技能或效果ID);
        if (技能定义Opt.isPresent()) {
            return 释放技能(发送者, 目标玩家, 玩家标识, 技能或效果ID, 技能定义Opt.get(), 参数);
        }

        Optional<效果定义> 效果定义Opt = 效果注册服务.获取定义(技能或效果ID);
        if (效果定义Opt.isPresent()) {
            return 施加效果(发送者, 玩家标识, 技能或效果ID, 效果定义Opt.get(), 参数);
        }

        发送错误(发送者, "管理员指令.技能.错误.未知ID", 技能或效果ID);
        return true;
    }

    private boolean 释放技能(CommandSender 发送者, Player 目标玩家, UUID 玩家标识,
                             String 技能ID, 技能定义 定义, String[] 参数) {
        Optional<玩家快照> 快照Opt = 玩家服务.获取快照(玩家标识);
        if (快照Opt.isEmpty()) {
            发送错误(发送者, 会话不存在键);
            return true;
        }
        玩家快照 快照 = 快照Opt.get();

        参数读取器 覆盖 = 参数读取器.从参数数组解析(参数, 3);

        技能冷却服务.重置冷却(玩家标识, 技能ID);
        技能冷却服务.重置公共冷却(玩家标识);

        技能上下文 上下文 = new 技能上下文(快照, 技能ID, 定义, null, System.currentTimeMillis(), 覆盖);

        技能执行结果 结果;
        try {
            结果 = 技能释放服务.释放(上下文);
        } catch (RuntimeException 异常) {
            日志器.warning("管理命令技能子命令释放异常：技能ID=" + 技能ID
                    + " 玩家=" + 目标玩家.getName()
                    + " 异常=" + 异常.getClass().getSimpleName()
                    + " 消息=" + 异常.getMessage());
            发送错误(发送者, "管理员指令.技能.释放.异常", 目标玩家.getName(), 技能ID);
            return true;
        }

        if (结果 == 技能执行结果.成功) {
            发送消息(发送者, "管理员指令.技能.释放.成功", 目标玩家.getName(), 技能ID);
        } else {
            发送错误(发送者, "管理员指令.技能.释放.失败", 目标玩家.getName(), 技能ID, 结果.name());
        }
        return true;
    }

    private boolean 施加效果(CommandSender 发送者, UUID 玩家标识, String 效果ID,
                             效果定义 定义, String[] 参数) {
        参数读取器 覆盖 = 参数读取器.从参数数组解析(参数, 3);
        int 层数 = 1;
        long 持续时间毫秒 = 定义.持续时间();

        try {
            效果调度服务.添加效果带覆盖(玩家标识, 效果ID, "", 层数, 持续时间毫秒, 覆盖);
            发送消息(发送者, "管理员指令.技能.效果.施加成功", 效果ID);
        } catch (RuntimeException 异常) {
            日志器.warning("管理命令技能子命令施加效果异常：效果ID=" + 效果ID
                    + " 异常=" + 异常.getClass().getSimpleName()
                    + " 消息=" + 异常.getMessage());
            发送错误(发送者, "管理员指令.技能.效果.施加失败", 效果ID);
        }
        return true;
    }

    private void 调度修饰器移除(UUID 玩家标识, String 类别, String 标签, double 秒数) {
        long 延迟Tick = (long) (秒数 * 每秒Tick);
        Bukkit.getScheduler().runTaskLater(插件, () -> {
            修饰器管理器.注销修饰器(玩家标识, 类别, 标签);
            属性服务.刷新属性(玩家标识);
        }, 延迟Tick);
    }

    private int 获取下一个序号(UUID 玩家标识, String 类别) {
        return 临时效果序号表
                .computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>())
                .computeIfAbsent(类别, 键 -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private List<String> 获取可用职业列表() {
        FileConfiguration 配置 = 配置加载器.加载(职业类型配置文件);
        ConfigurationSection 根 = 配置.getConfigurationSection(职业类型根键);
        if (根 == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(根.getKeys(false));
    }

    private double 获取默认仇恨清空范围() {
        return 插件.getConfig().getDouble("战斗.仇恨清空范围", 40.0);
    }

    private Integer 解析整数(String 文本) {
        try {
            return Integer.parseInt(文本);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double 解析双精度(String 文本) {
        try {
            return Double.parseDouble(文本);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!发送者.hasPermission(管理权限)) {
            return Collections.emptyList();
        }
        if (参数.length == 1) {
            return 筛选前缀(Arrays.asList(
                    子命令仇恨, 子命令战斗, 子命令等级,
                    子命令职业, 子命令属性, 子命令重载, 子命令刷新, 子命令技能
            ), 参数[0]);
        }
        String 子命令 = 参数[0];
        if (参数.length == 2) {
            return switch (子命令) {
                case 子命令仇恨, 子命令战斗 -> 筛选前缀(Arrays.asList(操作调试, 操作清空, 操作信息), 参数[1]);
                case 子命令职业 -> 筛选前缀(Arrays.asList(操作列表), 参数[1]);
                case 子命令属性, 子命令等级, 子命令刷新 -> 筛选前缀(获取在线玩家名列表(), 参数[1]);
                case 子命令技能 -> {
                    List<String> 候选 = new ArrayList<>();
                    候选.add(冷却关键词);
                    候选.add(参数关键词);
                    候选.addAll(获取所有技能与效果ID());
                    yield 筛选前缀(候选, 参数[1]);
                }
                default -> Collections.emptyList();
            };
        }
        if (参数.length == 3) {
            return switch (子命令) {
                case 子命令仇恨, 子命令战斗 -> {
                    if (操作调试.equals(参数[1])) {
                        yield 筛选前缀(Arrays.asList(开, 关), 参数[2]);
                    }
                    yield Collections.emptyList();
                }
                case 子命令属性 -> 筛选前缀(属性注册表.获取所有命令名(), 参数[2]);
                case 子命令职业 -> 筛选前缀(获取可用职业列表(), 参数[2]);
                case 子命令技能 -> {
                    if (冷却关键词.equals(参数[1])) {
                        yield 筛选前缀(Arrays.asList(重置关键词), 参数[2]);
                    }
                    if (参数关键词.equals(参数[1])) {
                        yield 筛选前缀(获取所有技能与效果ID(), 参数[2]);
                    }
                    yield 筛选前缀(获取在线玩家名列表(), 参数[2]);
                }
                default -> Collections.emptyList();
            };
        }
        if (子命令技能.equals(子命令) && 参数.length >= 4) {
            if (冷却关键词.equals(参数[1]) && 重置关键词.equals(参数[2]) && 参数.length == 4) {
                return 筛选前缀(获取在线玩家名列表(), 参数[3]);
            }
            if (!冷却关键词.equals(参数[1]) && !参数关键词.equals(参数[1])) {
                String 可能ID = 参数[1];
                List<参数注册表.参数条目> 条目列表 = 参数注册表.获取参数列表(可能ID);
                if (!条目列表.isEmpty()) {
                    List<String> 候选 = new ArrayList<>();
                    for (参数注册表.参数条目 条目 : 条目列表) {
                        候选.add(构建参数补全项(可能ID, 条目));
                    }
                    return 筛选前缀(候选, 参数[参数.length - 1]);
                }
                return Collections.emptyList();
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<String> 获取所有技能与效果ID() {
        List<String> 结果 = new ArrayList<>(技能注册服务.获取所有技能标识());
        for (效果定义 定义 : 效果注册服务.获取所有定义()) {
            结果.add(定义.效果标识());
        }
        return 结果;
    }

    private String 构建参数补全项(String 技能或效果ID, 参数注册表.参数条目 条目) {
        String 说明 = 解析参数说明(技能或效果ID, 条目);
        if (说明 != null && !说明.isEmpty()) {
            return 条目.参数名() + "=<" + 条目.默认值() + " " + 说明 + ">";
        }
        return 条目.参数名() + "=" + 条目.默认值();
    }

    private String 解析参数说明(String 技能或效果ID, 参数注册表.参数条目 条目) {
        String 键 = "管理员指令.技能.参数说明." + 技能或效果ID + "." + 条目.参数名();
        String 翻译值 = 翻译服务.获取(键);
        if (翻译值 != null && !翻译值.equals(键)) {
            return 翻译值;
        }
        return 条目.描述();
    }

    private List<String> 获取在线玩家名列表() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<String> 筛选前缀(List<String> 候选, String 前缀) {
        List<String> 结果 = new ArrayList<>();
        for (String 项 : 候选) {
            if (项.startsWith(前缀)) {
                结果.add(项);
            }
        }
        return 结果;
    }
}
