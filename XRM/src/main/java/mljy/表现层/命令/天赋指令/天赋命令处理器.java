package mljy.表现层.命令.天赋指令;

import com.google.inject.Inject;
import mljy.业务层.天赋管理服务;
import mljy.业务层.天赋注册服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import mljy.表现层.菜单.菜单服务;
import mljy.领域层.天赋.天赋节点;
import mljy.领域层.天赋.天赋操作结果;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class 天赋命令处理器 extends 抽象命令处理器 {
    private static final String 未知子命令键 = "天赋命令.未知子命令";
    private static final String 重置失败键 = "天赋命令.重置.失败";
    private static final String 列表无天赋键 = "天赋命令.列表.无天赋";
    private static final String 学习用法键 = "天赋命令.学习.用法";
    private static final String 学习成功键 = "天赋命令.学习.成功";
    private static final String 学习失败键 = "天赋命令.学习.失败";
    private static final String 信息用法键 = "天赋命令.信息.用法";
    private static final String 信息未找到键 = "天赋命令.信息.未找到";

    private static final String 帮助标题键 = "天赋命令.帮助.标题";
    private static final String 帮助菜单键 = "天赋命令.帮助.菜单";
    private static final String 帮助重置键 = "天赋命令.帮助.重置";
    private static final String 帮助列表键 = "天赋命令.帮助.列表";
    private static final String 帮助学习键 = "天赋命令.帮助.学习";
    private static final String 帮助信息键 = "天赋命令.帮助.信息";

    private static final String 子命令重置 = "reset";
    private static final String 子命令列表 = "list";
    private static final String 子命令学习 = "learn";
    private static final String 子命令信息 = "info";
    private static final String 子命令帮助 = "help";
    private static final String 子命令重置中 = "重置";
    private static final String 子命令列表中 = "列表";
    private static final String 子命令学习中 = "学习";
    private static final String 子命令信息中 = "信息";
    private static final String 子命令帮助中 = "帮助";

    private static final String 菜单打开成功键 = "天赋命令.菜单.打开成功";

    private final 菜单服务 菜单服务;
    private final 天赋管理服务 天赋管理服务;
    private final 天赋注册服务 天赋注册服务;

    @Inject
    public 天赋命令处理器(翻译服务 翻译服务,
                        关键词解析器 关键词解析器,
                        菜单服务 菜单服务,
                        天赋管理服务 天赋管理服务,
                        天赋注册服务 天赋注册服务) {
        super(翻译服务, 关键词解析器);
        this.菜单服务 = 菜单服务;
        this.天赋管理服务 = 天赋管理服务;
        this.天赋注册服务 = 天赋注册服务;
    }

    @Override
    public String 获取命令名() {
        return "天赋";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }

        if (参数.length == 0) {
            return 处理菜单(发送者);
        }

        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令重置, 子命令重置中 -> 处理重置(发送者);
            case 子命令列表, 子命令列表中 -> 处理列表(发送者);
            case 子命令学习, 子命令学习中 -> 处理学习(发送者, 参数);
            case 子命令信息, 子命令信息中 -> 处理信息(发送者, 参数);
            case 子命令帮助, 子命令帮助中 -> 处理帮助(发送者);
            default -> {
                发送错误(发送者, 未知子命令键, 子命令);
                yield true;
            }
        };
    }

    private boolean 处理菜单(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        if (菜单服务.打开菜单(玩家, "天赋菜单")) {
            发送消息(发送者, 菜单打开成功键);
        }
        return true;
    }

    private boolean 处理重置(CommandSender 发送者) {
        UUID 玩家标识 = 获取玩家标识(发送者);
        天赋操作结果 结果 = 天赋管理服务.重置天赋(玩家标识);
        if (结果.是否成功()) {
            if (结果.有消息()) {
                发送消息(发送者, 结果.消息键(), 结果.参数());
            } else {
                发送消息(发送者, "天赋命令.重置.成功");
            }
        } else {
            if (结果.有消息()) {
                发送错误(发送者, 结果.消息键(), 结果.参数());
            } else {
                发送错误(发送者, 重置失败键);
            }
        }
        return true;
    }

    private boolean 处理列表(CommandSender 发送者) {
        UUID 玩家标识 = 获取玩家标识(发送者);
        Set<String> 已学天赋 = 天赋管理服务.获取已学天赋(玩家标识);
        if (已学天赋.isEmpty()) {
            发送消息(发送者, 列表无天赋键);
            return true;
        }
        int 已用点数 = 天赋管理服务.获取已用点数(玩家标识);
        int 最大点数 = 天赋管理服务.获取最大点数(玩家标识);
        发送消息(发送者, "天赋命令.列表.已投入", 已用点数, Math.max(最大点数, 已用点数));
        for (String 天赋标识 : 已学天赋) {
            Optional<天赋节点> 节点 = 查找天赋节点(天赋标识);
            String 名称 = 节点.map(天赋节点::名称翻译键).map(翻译服务::获取).orElse(天赋标识);
            String 描述 = 节点.map(天赋节点::描述翻译键).map(翻译服务::获取).orElse("");
            发送消息(发送者, "天赋命令.列表.天赋项", 名称, 描述);
        }
        return true;
    }

    private boolean 处理学习(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, 学习用法键);
            return true;
        }
        String 天赋标识 = 参数[1];
        UUID 玩家标识 = 获取玩家标识(发送者);
        天赋操作结果 结果 = 天赋管理服务.学习天赋(玩家标识, 天赋标识);
        if (结果.是否成功()) {
            if (结果.有消息()) {
                发送消息(发送者, 结果.消息键(), 结果.参数());
            } else {
                发送消息(发送者, 学习成功键, 天赋标识);
            }
        } else {
            if (结果.有消息()) {
                发送错误(发送者, 结果.消息键(), 结果.参数());
            } else {
                发送错误(发送者, 学习失败键, 天赋标识);
            }
        }
        return true;
    }

    private boolean 处理信息(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, 信息用法键);
            return true;
        }
        String 天赋标识 = 参数[1];
        Optional<天赋节点> 节点可选 = 查找天赋节点(天赋标识);
        if (节点可选.isEmpty()) {
            发送错误(发送者, 信息未找到键, 天赋标识);
            return true;
        }
        天赋节点 节点 = 节点可选.get();
        发送消息(发送者, "天赋命令.信息.详情标题");
        发送消息(发送者, "天赋命令.信息.名称", 翻译服务.获取(节点.名称翻译键()));
        发送消息(发送者, "天赋命令.信息.描述", 翻译服务.获取(节点.描述翻译键()));
        发送消息(发送者, "天赋命令.信息.所需点数", 节点.所需总点数());
        if (节点.有前置()) {
            String 前置 = String.join(", ", 节点.前置天赋列表());
            发送消息(发送者, "天赋命令.信息.前置天赋", 前置);
        }
        if (节点.有选择组()) {
            发送消息(发送者, "天赋命令.信息.选择组", 节点.选择组());
        }
        return true;
    }

    private boolean 处理帮助(CommandSender 发送者) {
        发送消息(发送者, 帮助标题键);
        发送消息(发送者, 帮助菜单键);
        发送消息(发送者, 帮助重置键);
        发送消息(发送者, 帮助列表键);
        发送消息(发送者, 帮助学习键);
        发送消息(发送者, 帮助信息键);
        return true;
    }

    private Optional<天赋节点> 查找天赋节点(String 天赋标识) {
        Collection<String> 专精列表 = 天赋注册服务.获取所有专精();
        for (String 专精 : 专精列表) {
            Optional<天赋节点> 节点 = 天赋注册服务.获取天赋定义(专精, 天赋标识);
            if (节点.isPresent()) {
                return 节点;
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令重置, 子命令列表, 子命令学习, 子命令信息, 子命令帮助,
                    子命令重置中, 子命令列表中, 子命令学习中, 子命令信息中, 子命令帮助中);
        }
        return Collections.emptyList();
    }
}
