package mljy.表现层.命令.玩家指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.数据服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.领域层.玩家.玩家会话;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class 技能日志命令处理器 extends 抽象命令处理器 {
    private static final String 无效值键 = "玩家指令.日志.无效值";
    private static final String 设置成功键 = "玩家指令.日志.设置成功";
    private static final String 状态键 = "玩家指令.日志.状态";
    private static final String 开启键 = "玩家指令.日志.开启";
    private static final String 关闭键 = "玩家指令.日志.关闭";
    private static final String 技能日志名称键 = "玩家指令.日志.技能日志名称";

    private static final Set<String> 开启词集 = Set.of("开", "true", "on", "开启");
    private static final Set<String> 关闭词集 = Set.of("关", "false", "off", "关闭");

    private final 玩家服务 玩家服务;
    private final 数据服务 数据服务;

    @Inject
    public 技能日志命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器,
                              玩家服务 玩家服务, 数据服务 数据服务) {
        super(翻译服务, 关键词解析器);
        this.玩家服务 = 玩家服务;
        this.数据服务 = 数据服务;
    }

    @Override
    public String 获取命令名() {
        return "技能日志";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }
        Player 玩家 = (Player) 发送者;
        UUID 玩家标识 = 玩家.getUniqueId();

        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        if (会话可选.isEmpty()) {
            发送错误(发送者, 会话不存在键);
            return true;
        }
        玩家会话 会话 = 会话可选.get();

        if (参数.length == 0) {
            发送状态(发送者, 会话);
            return true;
        }

        String 参数值 = 参数[0].toLowerCase();
        if (开启词集.contains(参数值)) {
            切换开关(发送者, 会话, true);
        } else if (关闭词集.contains(参数值)) {
            切换开关(发送者, 会话, false);
        } else {
            发送错误(发送者, 无效值键);
        }
        return true;
    }

    public void 切换开关(Player 玩家, boolean 开启) {
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家.getUniqueId());
        if (会话可选.isEmpty()) {
            发送错误(玩家, 会话不存在键);
            return;
        }
        切换开关(玩家, 会话可选.get(), 开启);
    }

    private void 发送状态(CommandSender 发送者, 玩家会话 会话) {
        boolean 当前状态 = 会话.获取技能日志开关();
        String 状态文本 = 翻译服务.获取(当前状态 ? 开启键 : 关闭键);
        String 名称 = 翻译服务.获取(技能日志名称键);
        发送消息(发送者, 状态键, 名称, 状态文本);
    }

    private void 切换开关(CommandSender 发送者, 玩家会话 会话, boolean 开启) {
        会话.设置技能日志开关(开启);
        数据服务.保存(会话);
        String 名称 = 翻译服务.获取(技能日志名称键);
        String 状态文本 = 翻译服务.获取(开启 ? 开启键 : 关闭键);
        发送消息(发送者, 设置成功键, 名称, 状态文本);
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList("开", "关", "on", "off");
        }
        return Collections.emptyList();
    }
}
