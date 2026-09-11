package mljy.表现层.命令.总菜单指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import mljy.表现层.菜单.菜单服务;
import mljy.基础设施层.调试日志器;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class 总菜单命令处理器 extends 抽象命令处理器 {
    private static final String 总菜单标识 = "总菜单";

    private final 菜单服务 菜单服务;

    @Inject
    public 总菜单命令处理器(翻译服务 翻译服务,
                          关键词解析器 关键词解析器,
                          菜单服务 菜单服务) {
        super(翻译服务, 关键词解析器);
        this.菜单服务 = 菜单服务;
    }

    @Override
    public String 获取命令名() {
        return "cd";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        调试日志器.调试("总菜单命令", "收到 /%s 命令，发送者=%s", 标签, 发送者.getName());
        if (!检查玩家(发送者)) {
            return true;
        }
        Player 玩家 = (Player) 发送者;
        菜单服务.打开菜单(玩家, 总菜单标识);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        return Collections.emptyList();
    }
}
