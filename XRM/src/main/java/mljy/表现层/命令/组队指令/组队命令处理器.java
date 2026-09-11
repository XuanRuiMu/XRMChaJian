package mljy.表现层.命令.组队指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.组队服务;
import mljy.业务层.职业类型服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import mljy.领域层.战斗.角色类型;
import mljy.领域层.组队.队伍;
import mljy.领域层.组队.组队操作结果;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class 组队命令处理器 extends 抽象命令处理器 {
    private static final String 请指定玩家名键 = "队伍命令.队伍.请指定玩家名";
    private static final String 玩家不在线键 = "队伍命令.队伍.玩家不在线";
    private static final String 不在队伍中键 = "队伍命令.队伍.不在队伍中";
    private static final String 帮助标题键 = "队伍命令.帮助.标题";
    private static final String 帮助创建键 = "队伍命令.帮助.创建";
    private static final String 帮助邀请键 = "队伍命令.帮助.邀请";
    private static final String 帮助接受键 = "队伍命令.帮助.接受";
    private static final String 帮助拒绝键 = "队伍命令.帮助.拒绝";
    private static final String 帮助离开键 = "队伍命令.帮助.离开";
    private static final String 帮助踢出键 = "队伍命令.帮助.踢出";
    private static final String 帮助解散键 = "队伍命令.帮助.解散";
    private static final String 帮助转让键 = "队伍命令.帮助.转让";
    private static final String 帮助就位键 = "队伍命令.帮助.就位";
    private static final String 帮助就位检查键 = "队伍命令.帮助.就位检查";
    private static final String 帮助信息键 = "队伍命令.帮助.信息";
    private static final String 帮助队伍聊天键 = "队伍命令.帮助.队伍聊天";

    private static final String 信息标题键 = "队伍命令.信息.标题";
    private static final String 信息角色分布键 = "队伍命令.信息.角色分布";
    private static final String 信息队伍人数键 = "队伍命令.信息.队伍人数";
    private static final String 信息队长标记键 = "队伍命令.信息.队长标记";
    private static final String 信息离线标记键 = "队伍命令.信息.离线标记";
    private static final String 信息成员行坦克键 = "队伍命令.信息.成员行坦克";
    private static final String 信息成员行治疗键 = "队伍命令.信息.成员行治疗";
    private static final String 信息成员行输出键 = "队伍命令.信息.成员行输出";

    private static final String 子命令创建 = "create";
    private static final String 子命令邀请 = "invite";
    private static final String 子命令接受 = "accept";
    private static final String 子命令拒绝 = "decline";
    private static final String 子命令离开 = "leave";
    private static final String 子命令踢出 = "kick";
    private static final String 子命令解散 = "disband";
    private static final String 子命令转让 = "leader";
    private static final String 子命令就位 = "ready";
    private static final String 子命令就位检查 = "readycheck";
    private static final String 子命令信息 = "info";
    private static final String 子命令帮助 = "help";
    private static final String 子命令创建中 = "创建";
    private static final String 子命令邀请中 = "邀请";
    private static final String 子命令接受中 = "接受";
    private static final String 子命令拒绝中 = "拒绝";
    private static final String 子命令离开中 = "离开";
    private static final String 子命令踢出中 = "踢出";
    private static final String 子命令解散中 = "解散";
    private static final String 子命令转让中 = "转让";
    private static final String 子命令就位中 = "就位";
    private static final String 子命令就位检查中 = "就位检查";
    private static final String 子命令信息中 = "信息";
    private static final String 子命令帮助中 = "帮助";

    private static final String 坦克文本 = "坦克";
    private static final String 治疗文本 = "治疗";
    private static final String 输出文本 = "输出";
    private static final String 空字符串 = "";

    private final 组队服务 组队服务;
    private final 职业类型服务 职业类型服务;
    private final 玩家服务 玩家服务;

    @Inject
    public 组队命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器, 组队服务 组队服务, 职业类型服务 职业类型服务, 玩家服务 玩家服务) {
        super(翻译服务, 关键词解析器);
        this.组队服务 = 组队服务;
        this.职业类型服务 = 职业类型服务;
        this.玩家服务 = 玩家服务;
    }

    @Override
    public String 获取命令名() {
        return "party";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }

        if (参数.length == 0) {
            发送帮助信息(发送者);
            return true;
        }

        Player 玩家 = (Player) 发送者;
        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令创建, 子命令创建中 -> 处理创建(玩家);
            case 子命令邀请, 子命令邀请中 -> 处理邀请(玩家, 参数);
            case 子命令接受, 子命令接受中 -> 处理接受(玩家);
            case 子命令拒绝, 子命令拒绝中 -> 处理拒绝(玩家);
            case 子命令离开, 子命令离开中 -> 处理离开(玩家);
            case 子命令踢出, 子命令踢出中 -> 处理踢出(玩家, 参数);
            case 子命令解散, 子命令解散中 -> 处理解散(玩家);
            case 子命令转让, 子命令转让中 -> 处理转让(玩家, 参数);
            case 子命令就位, 子命令就位中 -> 处理就位(玩家);
            case 子命令就位检查, 子命令就位检查中 -> 处理就位检查(玩家);
            case 子命令信息, 子命令信息中 -> 处理信息(玩家);
            case 子命令帮助, 子命令帮助中 -> 处理帮助(发送者);
            default -> {
                发送帮助信息(发送者);
                yield true;
            }
        };
    }

    private boolean 处理创建(Player 玩家) {
        组队操作结果 结果 = 组队服务.创建队伍(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理邀请(Player 玩家, String[] 参数) {
        if (参数.length < 2) {
            发送错误(玩家, 请指定玩家名键);
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayerExact(参数[1]);
        if (目标玩家 == null) {
            发送错误(玩家, 玩家不在线键);
            return true;
        }
        组队操作结果 结果 = 组队服务.邀请玩家(玩家.getUniqueId(), 目标玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理接受(Player 玩家) {
        组队操作结果 结果 = 组队服务.接受邀请(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理拒绝(Player 玩家) {
        组队操作结果 结果 = 组队服务.拒绝邀请(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理离开(Player 玩家) {
        组队操作结果 结果 = 组队服务.离开队伍(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理踢出(Player 玩家, String[] 参数) {
        if (参数.length < 2) {
            发送错误(玩家, 请指定玩家名键);
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayerExact(参数[1]);
        if (目标玩家 == null) {
            发送错误(玩家, 玩家不在线键);
            return true;
        }
        组队操作结果 结果 = 组队服务.踢出成员(玩家.getUniqueId(), 目标玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理解散(Player 玩家) {
        组队操作结果 结果 = 组队服务.解散队伍(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理转让(Player 玩家, String[] 参数) {
        if (参数.length < 2) {
            发送错误(玩家, 请指定玩家名键);
            return true;
        }
        Player 目标玩家 = Bukkit.getPlayerExact(参数[1]);
        if (目标玩家 == null) {
            发送错误(玩家, 玩家不在线键);
            return true;
        }
        组队操作结果 结果 = 组队服务.转让队长(玩家.getUniqueId(), 目标玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理就位(Player 玩家) {
        组队操作结果 结果 = 组队服务.确认就位(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理就位检查(Player 玩家) {
        组队操作结果 结果 = 组队服务.发起就位检查(玩家.getUniqueId());
        处理操作结果(玩家, 结果);
        return true;
    }

    private boolean 处理信息(Player 玩家) {
        Optional<队伍> 队伍可选 = 组队服务.获取玩家队伍(玩家.getUniqueId());
        if (队伍可选.isEmpty()) {
            发送错误(玩家, 不在队伍中键);
            return true;
        }
        队伍 队伍 = 队伍可选.get();
        发送消息(玩家, 信息标题键);
        发送消息(玩家, 信息角色分布键, 构建角色分布(队伍));
        发送消息(玩家, 信息队伍人数键, 队伍.获取成员数量());
        String 队长标记 = 翻译服务.获取(信息队长标记键);
        String 离线标记 = 翻译服务.获取(信息离线标记键);
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            发送成员行(玩家, 成员标识, 队伍, 队长标记, 离线标记);
        }
        return true;
    }

    private boolean 处理帮助(CommandSender 发送者) {
        发送帮助信息(发送者);
        return true;
    }

    private void 处理操作结果(Player 玩家, 组队操作结果 结果) {
        if (结果.是否成功()) {
            return;
        }
        if (结果.有消息()) {
            发送错误(玩家, 结果.消息键(), 结果.参数());
        }
    }

    private String 构建角色分布(队伍 队伍) {
        Map<角色类型, Integer> 计数 = new HashMap<>();
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            角色类型 类型 = 获取成员角色类型(成员标识);
            计数.merge(类型, 1, (Integer a, Integer b) -> a + b);
        }
        StringBuilder 构建器 = new StringBuilder();
        append角色计数(构建器, 计数, 角色类型.坦克, 坦克文本);
        append角色计数(构建器, 计数, 角色类型.治疗, 治疗文本);
        append角色计数(构建器, 计数, 角色类型.输出, 输出文本);
        return 构建器.toString().trim();
    }

    private void append角色计数(StringBuilder 构建器, Map<角色类型, Integer> 计数, 角色类型 类型, String 名称) {
        int 数量 = 计数.getOrDefault(类型, 0);
        if (数量 > 0) {
            if (构建器.length() > 0) {
                构建器.append(" ");
            }
            构建器.append(名称).append(":").append(数量);
        }
    }

    private void 发送成员行(Player 玩家, UUID 成员标识, 队伍 队伍, String 队长标记, String 离线标记) {
        角色类型 类型 = 获取成员角色类型(成员标识);
        String 成员名 = 获取成员名(成员标识);
        String 队长后缀 = 队伍.获取队长标识().equals(成员标识) ? 队长标记 : 空字符串;
        String 离线后缀 = 是玩家在线(成员标识) ? 空字符串 : 离线标记;
        String 键 = switch (类型) {
            case 坦克 -> 信息成员行坦克键;
            case 治疗 -> 信息成员行治疗键;
            case 输出 -> 信息成员行输出键;
        };
        String 角色标记 = switch (类型) {
            case 坦克 -> 坦克文本;
            case 治疗 -> 治疗文本;
            case 输出 -> 输出文本;
        };
        发送消息(玩家, 键, 角色标记, 成员名, 队长后缀, 离线后缀);
    }

    private 角色类型 获取成员角色类型(UUID 成员标识) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(成员标识);
        if (会话.isEmpty()) {
            return 角色类型.输出;
        }
        return 职业类型服务.获取角色类型(会话.get().获取专精())
                .orElse(角色类型.输出);
    }

    private String 获取成员名(UUID 成员标识) {
        Player 成员 = Bukkit.getPlayer(成员标识);
        if (成员 != null) {
            return 成员.getName();
        }
        Optional<玩家会话> 会话 = 玩家服务.获取会话(成员标识);
        return 会话.map(玩家会话::获取名称).orElse(成员标识.toString());
    }

    private boolean 是玩家在线(UUID 玩家标识) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        return 玩家 != null && 玩家.isOnline();
    }

    private void 发送帮助信息(CommandSender 发送者) {
        发送消息(发送者, 帮助标题键);
        发送消息(发送者, 帮助创建键);
        发送消息(发送者, 帮助邀请键);
        发送消息(发送者, 帮助接受键);
        发送消息(发送者, 帮助拒绝键);
        发送消息(发送者, 帮助离开键);
        发送消息(发送者, 帮助踢出键);
        发送消息(发送者, 帮助解散键);
        发送消息(发送者, 帮助转让键);
        发送消息(发送者, 帮助就位键);
        发送消息(发送者, 帮助就位检查键);
        发送消息(发送者, 帮助信息键);
        发送消息(发送者, 帮助队伍聊天键);
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令创建, 子命令邀请, 子命令接受, 子命令拒绝, 子命令离开,
                    子命令踢出, 子命令解散, 子命令转让, 子命令就位, 子命令就位检查, 子命令信息, 子命令帮助,
                    子命令创建中, 子命令邀请中, 子命令接受中, 子命令拒绝中, 子命令离开中,
                    子命令踢出中, 子命令解散中, 子命令转让中, 子命令就位中, 子命令就位检查中, 子命令信息中, 子命令帮助中);
        }
        return Collections.emptyList();
    }
}
