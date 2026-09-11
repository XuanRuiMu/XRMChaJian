package mljy.表现层.命令.技能指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class 技能命令处理器 extends 抽象命令处理器 {
    private static final String 菜单未配置键 = "技能指令.菜单.未配置";

    @Inject
    public 技能命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器) {
        super(翻译服务, 关键词解析器);
    }

    @Override
    public String 获取命令名() {
        return "技能";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }
        发送消息(发送者, 菜单未配置键);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        return Collections.emptyList();
    }
}
