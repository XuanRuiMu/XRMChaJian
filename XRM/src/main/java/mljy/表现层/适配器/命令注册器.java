package mljy.表现层.适配器;

import com.google.inject.Inject;
import com.google.inject.Injector;
import mljy.表现层.命令.命令处理器;
import mljy.表现层.命令.技能指令.技能命令处理器;
import mljy.表现层.命令.玩家指令.技能日志命令处理器;
import mljy.表现层.命令.玩家指令.战斗日志命令处理器;
import mljy.表现层.命令.属性指令.属性命令处理器;
import mljy.表现层.命令.生命条缩放指令.生命条缩放命令处理器;
import mljy.表现层.命令.管理员指令.管理命令处理器;
import mljy.表现层.命令.驭空术指令.驭空术命令处理器;
import mljy.表现层.命令.天赋指令.天赋命令处理器;
import mljy.表现层.命令.任务指令.任务命令处理器;
import mljy.表现层.命令.组队指令.组队命令处理器;
import mljy.表现层.命令.乐器指令.乐器命令处理器;
import mljy.表现层.命令.总菜单指令.总菜单命令处理器;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class 命令注册器 {
    private final JavaPlugin 插件;
    private final Injector 注入器;
    private final Logger 日志器;
    private final Map<String, 命令处理器> xrm路由表 = new HashMap<>();
    private CommandMap 命令映射;

    @Inject
    public 命令注册器(JavaPlugin 插件, Injector 注入器) {
        this.插件 = 插件;
        this.注入器 = 注入器;
        this.日志器 = 插件.getLogger();
    }

    public void 注册命令() {
        命令映射 = 获取命令映射();
        if (命令映射 == null) {
            日志器.severe("无法获取Bukkit CommandMap，命令五重注册失败");
            return;
        }

        注册命令定义("技能", "jn", "jineng", "skill", 技能命令处理器.class);
        注册命令定义("技能日志", "jnrz", "jinengrizhi", "skilllog", 技能日志命令处理器.class);
        注册命令定义("战斗日志", "zdrz", "zhandourizhi", "combatlog", 战斗日志命令处理器.class);
        注册命令定义("属性", "sx", "shuxing", "stats", 属性命令处理器.class);
        注册命令定义("生命条缩放", "smtsf", "shengmingtiaosuofang", "healthbar", 生命条缩放命令处理器.class);
        注册命令定义("管理", "gl", "guanli", "admin", 管理命令处理器.class);
        注册命令定义("驭空术", "yks", "yukongshu", "flight", 驭空术命令处理器.class);
        注册命令定义("天赋", "tf", "tianfu", "talent", 天赋命令处理器.class);
        注册命令定义("任务", "rw", "renwu", "quest", 任务命令处理器.class);
        注册命令定义("组队", "zd", "zudui", "party", 组队命令处理器.class);
        注册命令定义("乐器", "yq", "yueqi", "instrument", 乐器命令处理器.class);
        注册命令定义("菜单", "cd", "caidan", "menu", 总菜单命令处理器.class);

        注册xrm命令();
        日志器.info("命令五重注册完成：12条命令 × 5变体 + xrm分发器");
    }

    private void 注册命令定义(String 中文名, String 首字母, String 全拼, String 英文名,
                              Class<? extends 命令处理器> 处理器类) {
        命令处理器 处理器 = 注入器.getInstance(处理器类);

        for (String 名称 : new String[]{中文名, 首字母, 全拼, 英文名}) {
            注册单个命令(处理器, 名称);
            xrm路由表.put(名称, 处理器);
        }
    }

    private void 注册单个命令(命令处理器 处理器, String 名称) {
        注销冲突命令(名称);
        PluginCommand 命令 = 创建插件命令(名称);
        if (命令 == null) {
            日志器.warning("无法创建PluginCommand实例：" + 名称);
            return;
        }
        命令.setExecutor(处理器);
        命令.setTabCompleter(处理器);
        命令映射.register(插件.getName(), 命令);
    }

    private void 注册xrm命令() {
        注销冲突命令("xrm");
        PluginCommand xrm命令 = 创建插件命令("xrm");
        if (xrm命令 == null) {
            日志器.severe("无法创建xrm命令实例");
            return;
        }
        xrm命令.setExecutor(this::xrm执行);
        xrm命令.setTabCompleter(this::xrm补全);
        命令映射.register(插件.getName(), xrm命令);
        日志器.info("已注册/xrm分发命令，覆盖" + xrm路由表.size() + "个子命令路由");
    }

    private boolean xrm执行(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 0) {
            发送者.sendMessage("§e/xrm <子命令> — 可用子命令：" + String.join("、", xrm路由表.keySet()));
            return true;
        }
        命令处理器 处理器 = xrm路由表.get(参数[0]);
        if (处理器 == null) {
            发送者.sendMessage("§c未知子命令：" + 参数[0]);
            return true;
        }
        String[] 剩余参数 = Arrays.copyOfRange(参数, 1, 参数.length);
        return 处理器.onCommand(发送者, 命令, 参数[0], 剩余参数);
    }

    private List<String> xrm补全(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length <= 1) {
            String 前缀 = 参数.length == 1 ? 参数[0] : "";
            return 筛选前缀(xrm路由表.keySet(), 前缀);
        }
        命令处理器 处理器 = xrm路由表.get(参数[0]);
        if (处理器 == null) {
            return Collections.emptyList();
        }
        String[] 剩余参数 = Arrays.copyOfRange(参数, 1, 参数.length);
        return 处理器.onTabComplete(发送者, 命令, 参数[0], 剩余参数);
    }

    private void 注销冲突命令(String 名称) {
        try {
            Field 已知命令字段 = SimpleCommandMap.class.getDeclaredField("knownCommands");
            已知命令字段.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> 已知命令表 = (Map<String, Command>) 已知命令字段.get(命令映射);
            Command 已存在命令 = 已知命令表.get(名称);
            if (已存在命令 != null) {
                已知命令表.remove(名称);
                String 来源 = 已存在命令 instanceof PluginCommand pc ? pc.getPlugin().getName() : "未知";
                日志器.info("已注销冲突命令: /" + 名称 + " (来源: " + 来源 + ")");
            }
        } catch (Exception e) {
            日志器.warning("注销冲突命令异常: /" + 名称 + " — " + e.getMessage());
        }
    }

    private PluginCommand 创建插件命令(String 名称) {
        try {
            Constructor<PluginCommand> 构造器 = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            构造器.setAccessible(true);
            return 构造器.newInstance(名称, 插件);
        } catch (Exception e) {
            日志器.warning("创建PluginCommand失败: " + 名称 + " — " + e.getMessage());
            return null;
        }
    }

    private CommandMap 获取命令映射() {
        try {
            Field 字段 = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            字段.setAccessible(true);
            return (CommandMap) 字段.get(Bukkit.getServer());
        } catch (Exception e) {
            日志器.severe("获取CommandMap失败: " + e.getMessage());
            return null;
        }
    }

    private List<String> 筛选前缀(java.util.Collection<String> 候选集, String 前缀) {
        String 前缀小写 = 前缀.toLowerCase();
        return 候选集.stream()
                .filter(项 -> 项.toLowerCase().startsWith(前缀小写))
                .sorted()
                .toList();
    }
}
