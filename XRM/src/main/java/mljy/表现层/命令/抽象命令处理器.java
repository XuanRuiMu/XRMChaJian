package mljy.表现层.命令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class 抽象命令处理器 implements 命令处理器 {
    protected static final String 错误前缀键 = "错误前缀";
    protected static final String 仅玩家可用键 = "通用.错误.仅玩家可用";
    protected static final String 无权限键 = "通用.错误.无权限";
    protected static final String 玩家不在线键 = "通用.错误.玩家不在线";
    protected static final String 参数不足键 = "通用.错误.参数不足";
    protected static final String 无效参数键 = "通用.错误.无效参数";
    protected static final String 未知错误键 = "通用.错误.未知错误";
    protected static final String 会话不存在键 = "通用.错误.会话不存在";
    protected static final String 文本分隔符 = " ";

    protected final 翻译服务 翻译服务;
    protected final 关键词解析器 关键词解析器;
    protected final MiniMessage 迷你消息;

    @Inject
    protected 抽象命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器) {
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.迷你消息 = MiniMessage.miniMessage();
    }

    protected void 发送消息(CommandSender 发送者, String 键, Object... 参数) {
        String 文本 = 翻译服务.获取(键);
        String 解析后 = 关键词解析器.解析(文本, 获取玩家标识(发送者), 参数);
        发送者.sendMessage(迷你消息.deserialize(解析后));
    }

    protected void 发送错误(CommandSender 发送者, String 键, Object... 参数) {
        String 前缀 = 翻译服务.获取(错误前缀键);
        String 文本 = 翻译服务.获取(键);
        String 解析后 = 关键词解析器.解析(文本, 获取玩家标识(发送者), 参数);
        发送者.sendMessage(迷你消息.deserialize(前缀 + 文本分隔符 + 解析后));
    }

    protected void 发送原始文本(CommandSender 发送者, String 文本) {
        String 解析后 = 关键词解析器.解析(文本, 获取玩家标识(发送者));
        发送者.sendMessage(迷你消息.deserialize(解析后));
    }

    protected UUID 获取玩家标识(CommandSender 发送者) {
        return 发送者 instanceof Player 玩家 ? 玩家.getUniqueId() : null;
    }

    protected boolean 检查玩家(CommandSender 发送者) {
        if (!(发送者 instanceof Player)) {
            发送错误(发送者, 仅玩家可用键);
            return false;
        }
        return true;
    }

    protected boolean 检查权限(CommandSender 发送者, String 权限) {
        if (!发送者.hasPermission(权限)) {
            发送错误(发送者, 无权限键);
            return false;
        }
        return true;
    }
}
