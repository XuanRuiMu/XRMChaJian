package mljy.表现层.命令;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public interface 命令处理器 extends CommandExecutor, TabCompleter {
    String 获取命令名();
}
