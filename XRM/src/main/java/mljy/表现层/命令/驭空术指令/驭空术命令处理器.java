package mljy.表现层.命令.驭空术指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class 驭空术命令处理器 extends 抽象命令处理器 {
    private static final String 未知命令键 = "驭空术命令.未知命令";
    private static final String 帮助标题键 = "驭空术命令.help_title";
    private static final String 帮助命令行键 = "驭空术命令.help_command_line";
    private static final String 操控标题键 = "驭空术命令.controls_title";
    private static final String 操控行键 = "驭空术命令.controls_line";

    private static final String 帮助切换命令键 = "驭空术命令.help_cmd_toggle";
    private static final String 帮助切换说明键 = "驭空术命令.help_toggle";
    private static final String 帮助开启命令键 = "驭空术命令.help_cmd_on";
    private static final String 帮助开启说明键 = "驭空术命令.help_on";
    private static final String 帮助关闭命令键 = "驭空术命令.help_cmd_off";
    private static final String 帮助关闭说明键 = "驭空术命令.help_off";
    private static final String 帮助起飞命令键 = "驭空术命令.help_cmd_takeoff";
    private static final String 帮助起飞说明键 = "驭空术命令.help_takeoff";
    private static final String 帮助冲刺命令键 = "驭空术命令.help_cmd_dash";
    private static final String 帮助冲刺说明键 = "驭空术命令.help_dash";
    private static final String 帮助爆发命令键 = "驭空术命令.help_cmd_burst";
    private static final String 帮助爆发说明键 = "驭空术命令.help_burst";
    private static final String 帮助状态命令键 = "驭空术命令.help_cmd_status";
    private static final String 帮助状态说明键 = "驭空术命令.help_status";
    private static final String 帮助帮助命令键 = "驭空术命令.help_cmd_help";
    private static final String 帮助帮助说明键 = "驭空术命令.help_help";

    private static final String 操控空格键 = "驭空术命令.control_space";
    private static final String 操控潜行键 = "驭空术命令.control_sneak";
    private static final String 操控俯冲键 = "驭空术命令.control_dive";
    private static final String 操控爬升键 = "驭空术命令.control_climb";
    private static final String 操控双跳键 = "驭空术命令.control_double_jump";

    private static final String 子命令开启 = "开启";
    private static final String 子命令关闭 = "关闭";
    private static final String 子命令起飞 = "起飞";
    private static final String 子命令冲刺 = "冲刺";
    private static final String 子命令爆发 = "爆发";
    private static final String 子命令状态 = "状态";
    private static final String 子命令帮助 = "帮助";

    private static final String 功能未实现键 = "通用.提示.操作失败";

    @Inject
    public 驭空术命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器) {
        super(翻译服务, 关键词解析器);
    }

    @Override
    public String 获取命令名() {
        return "驭空术";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }

        if (参数.length == 0) {
            发送错误(发送者, 功能未实现键);
            return true;
        }

        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令开启 -> 处理未实现功能(发送者);
            case 子命令关闭 -> 处理未实现功能(发送者);
            case 子命令起飞 -> 处理未实现功能(发送者);
            case 子命令冲刺 -> 处理未实现功能(发送者);
            case 子命令爆发 -> 处理未实现功能(发送者);
            case 子命令状态 -> 处理未实现功能(发送者);
            case 子命令帮助 -> 处理帮助(发送者);
            default -> {
                发送错误(发送者, 未知命令键);
                yield true;
            }
        };
    }

    private boolean 处理未实现功能(CommandSender 发送者) {
        发送错误(发送者, 功能未实现键);
        return true;
    }

    private boolean 处理帮助(CommandSender 发送者) {
        发送帮助信息(发送者);
        return true;
    }

    private void 发送帮助信息(CommandSender 发送者) {
        发送消息(发送者, 帮助标题键);
        发送帮助行(发送者, 帮助切换命令键, 帮助切换说明键);
        发送帮助行(发送者, 帮助开启命令键, 帮助开启说明键);
        发送帮助行(发送者, 帮助关闭命令键, 帮助关闭说明键);
        发送帮助行(发送者, 帮助起飞命令键, 帮助起飞说明键);
        发送帮助行(发送者, 帮助冲刺命令键, 帮助冲刺说明键);
        发送帮助行(发送者, 帮助爆发命令键, 帮助爆发说明键);
        发送帮助行(发送者, 帮助状态命令键, 帮助状态说明键);
        发送帮助行(发送者, 帮助帮助命令键, 帮助帮助说明键);

        发送消息(发送者, 操控标题键);
        发送操控行(发送者, 操控空格键);
        发送操控行(发送者, 操控潜行键);
        发送操控行(发送者, 操控俯冲键);
        发送操控行(发送者, 操控爬升键);
        发送操控行(发送者, 操控双跳键);
    }

    private void 发送帮助行(CommandSender 发送者, String 命令键, String 说明键) {
        String 命令 = 翻译服务.获取(命令键);
        String 说明 = 翻译服务.获取(说明键);
        发送消息(发送者, 帮助命令行键, 命令, 说明);
    }

    private void 发送操控行(CommandSender 发送者, String 操控键) {
        String 操控 = 翻译服务.获取(操控键);
        发送消息(发送者, 操控行键, 操控);
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令开启, 子命令关闭, 子命令起飞, 子命令冲刺, 子命令爆发, 子命令状态, 子命令帮助);
        }
        return Collections.emptyList();
    }
}
