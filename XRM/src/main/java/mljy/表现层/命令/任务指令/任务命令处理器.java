package mljy.表现层.命令.任务指令;

import com.google.inject.Inject;
import mljy.业务层.任务管理服务;
import mljy.业务层.任务注册服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import mljy.表现层.菜单.菜单服务;
import mljy.领域层.任务.任务定义;
import mljy.领域层.任务.任务进度;
import mljy.领域层.任务.任务状态;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class 任务命令处理器 extends 抽象命令处理器 {
    private static final String 未知子命令键 = "任务命令.错误.未知子命令";
    private static final String 用法主命令键 = "任务命令.用法.主命令";
    private static final String 用法管理键 = "任务命令.用法.管理";
    private static final String 进度标题键 = "任务命令.进度.标题";
    private static final String 重载成功键 = "任务命令.重载.成功";
    private static final String 菜单打开成功键 = "任务命令.菜单.打开成功";
    private static final String 进度摘要键 = "任务命令.进度.摘要";
    private static final String 进度条目键 = "任务命令.进度.条目";
    private static final String 目标不在线键 = "任务命令.错误.目标不在线";
    private static final String 任务不存在键 = "任务命令.错误.任务不存在";
    private static final String 未知操作键 = "任务命令.错误.未知操作";

    private static final String 子命令菜单 = "菜单";
    private static final String 子命令进度 = "进度";
    private static final String 子命令管理 = "管理";
    private static final String 子命令重载 = "重载";
    private static final String 子命令帮助 = "help";

    private static final String 管理权限 = "xrm.admin.quest";
    private static final String 功能未实现键 = "通用.提示.操作失败";

    private final 菜单服务 菜单服务;
    private final 任务管理服务 任务管理服务;
    private final 任务注册服务 任务注册服务;

    @Inject
    public 任务命令处理器(翻译服务 翻译服务,
                        关键词解析器 关键词解析器,
                        菜单服务 菜单服务,
                        任务管理服务 任务管理服务,
                        任务注册服务 任务注册服务) {
        super(翻译服务, 关键词解析器);
        this.菜单服务 = 菜单服务;
        this.任务管理服务 = 任务管理服务;
        this.任务注册服务 = 任务注册服务;
    }

    @Override
    public String 获取命令名() {
        return "任务";
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
            case 子命令菜单 -> 处理菜单(发送者);
            case 子命令进度 -> 处理进度(发送者);
            case 子命令管理 -> 处理管理(发送者, 参数);
            case 子命令重载 -> 处理重载(发送者);
            case 子命令帮助 -> 处理帮助(发送者);
            default -> {
                发送错误(发送者, 未知子命令键, 子命令);
                发送消息(发送者, 用法主命令键);
                yield true;
            }
        };
    }

    private boolean 处理菜单(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        if (菜单服务.打开菜单(玩家, "任务菜单")) {
            发送消息(发送者, 菜单打开成功键);
        }
        return true;
    }

    private boolean 处理进度(CommandSender 发送者) {
        UUID 玩家标识 = 获取玩家标识(发送者);
        List<任务进度> 进度列表 = 任务管理服务.获取所有进度(玩家标识);
        int 进行中 = 0;
        int 可交付 = 0;
        int 已完成 = 0;
        for (任务进度 进度 : 进度列表) {
            任务状态 状态 = 进度.获取状态();
            if (状态 == 任务状态.进行中) {
                进行中++;
            } else if (状态 == 任务状态.可交付) {
                可交付++;
            } else if (状态 == 任务状态.已完成) {
                已完成++;
            }
        }
        发送消息(发送者, 进度标题键);
        发送消息(发送者, 进度摘要键, 进行中, 可交付, 已完成);
        for (任务进度 进度 : 进度列表) {
            if (进度.获取状态() == 任务状态.进行中) {
                String 任务名 = 获取任务名称(进度.获取任务标识());
                发送消息(发送者, 进度条目键, 任务名, 进度.获取当前进度(), 进度.获取目标进度());
            }
        }
        return true;
    }

    private boolean 处理管理(CommandSender 发送者, String[] 参数) {
        if (!检查权限(发送者, 管理权限)) {
            return true;
        }
        if (参数.length < 4) {
            发送消息(发送者, 用法管理键);
            return true;
        }
        String 操作 = 参数[1];
        String 玩家名 = 参数[2];
        String 任务标识 = 参数[3];
        Player 目标玩家 = Bukkit.getPlayerExact(玩家名);
        if (目标玩家 == null) {
            发送错误(发送者, 目标不在线键, 玩家名);
            return true;
        }
        if (!任务注册服务.存在(任务标识)) {
            发送错误(发送者, 任务不存在键, 任务标识);
            return true;
        }
        UUID 目标标识 = 目标玩家.getUniqueId();
        String 任务名 = 获取任务名称(任务标识);
        switch (操作) {
            case "接取" -> {
                boolean 成功 = 任务管理服务.接取任务(目标标识, 任务标识);
                if (成功) {
                    发送消息(发送者, "任务命令.管理.接取成功", 目标玩家.getName(), 任务名);
                } else {
                    发送错误(发送者, "任务命令.错误.无进度", 任务标识);
                }
            }
            case "完成" -> {
                boolean 成功 = 任务管理服务.强制完成任务(目标标识, 任务标识);
                if (!成功) {
                    成功 = 任务管理服务.交付任务(目标标识, 任务标识);
                }
                if (成功) {
                    发送消息(发送者, "任务命令.管理.完成成功", 目标玩家.getName(), 任务名);
                } else {
                    发送错误(发送者, "任务命令.错误.无进度", 任务标识);
                }
            }
            case "重置" -> {
                boolean 成功 = 任务管理服务.重置任务(目标标识, 任务标识);
                if (成功) {
                    发送消息(发送者, "任务命令.管理.重置成功", 目标玩家.getName(), 任务名);
                } else {
                    发送错误(发送者, "任务命令.错误.无进度", 任务标识);
                }
            }
            default -> {
                发送错误(发送者, 未知操作键, 操作);
                发送消息(发送者, 用法管理键);
            }
        }
        return true;
    }

    private boolean 处理重载(CommandSender 发送者) {
        if (!检查权限(发送者, 管理权限)) {
            return true;
        }
        任务注册服务.重载();
        发送消息(发送者, 重载成功键);
        return true;
    }

    private boolean 处理帮助(CommandSender 发送者) {
        发送消息(发送者, 用法主命令键);
        return true;
    }

    private String 获取任务名称(String 任务标识) {
        Optional<任务定义> 定义 = 任务注册服务.获取定义(任务标识);
        return 定义.map(任务定义::名称翻译键)
                .map(翻译服务::获取)
                .orElse(任务标识);
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令菜单, 子命令进度, 子命令管理, 子命令重载, 子命令帮助);
        }
        if (参数.length == 2 && 参数[0].equals(子命令管理)) {
            return Arrays.asList("接取", "完成", "重置");
        }
        return Collections.emptyList();
    }
}
