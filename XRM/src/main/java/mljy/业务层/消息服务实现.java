package mljy.业务层;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.基础设施层.事件日志上下文;
import mljy.基础设施层.调试日志器;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class 消息服务实现 implements 消息服务 {
    private static final String 错误前缀键 = "错误前缀";
    private static final String 文本分隔符 = " ";

    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final 玩家服务 玩家服务;
    private final 日志合并管理器 日志合并管理器;
    private final 战斗日志上下文管理器 战斗日志上下文管理器;
    private final MiniMessage 迷你消息 = MiniMessage.miniMessage();

    @Inject
    public 消息服务实现(翻译服务 翻译服务,
                      关键词解析器 关键词解析器,
                      玩家服务 玩家服务,
                      日志合并管理器 日志合并管理器,
                      战斗日志上下文管理器 战斗日志上下文管理器) {
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.玩家服务 = 玩家服务;
        this.日志合并管理器 = 日志合并管理器;
        this.战斗日志上下文管理器 = 战斗日志上下文管理器;
    }

    @Override
    public void 发送消息(玩家快照 玩家, String 键, Object... 参数) {
        Player 实体玩家 = Bukkit.getPlayer(玩家.唯一标识());
        if (实体玩家 == null || !实体玩家.isOnline()) {
            return;
        }
        String 文本 = 翻译服务.获取(键);
        String 解析后文本 = 关键词解析器.解析(文本, 玩家.唯一标识(), 参数);
        实体玩家.sendMessage(迷你消息.deserialize(解析后文本));
    }

    @Override
    public void 发送消息(UUID 玩家标识, String 键, Object... 参数) {
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        if (快照可选.isEmpty()) {
            return;
        }
        发送消息(快照可选.get(), 键, 参数);
    }

    @Override
    public void 发送技能日志(玩家快照 玩家, String 键, Object... 参数) {
        发送技能日志(玩家, 默认事件标识(), 键, 参数);
    }

    @Override
    public void 发送技能日志(玩家快照 玩家, UUID 事件标识, String 键, Object... 参数) {
        if (!检查技能日志开关(玩家.唯一标识())) {
            战斗日志上下文管理器.清除上下文(玩家.唯一标识(), 事件标识);
            return;
        }
        String 文本 = 翻译服务.获取(键);
        String 解析后文本 = 关键词解析器.解析(文本, 玩家.唯一标识(), 参数);
        UUID 有效事件标识 = 事件标识 != null ? 事件标识 : 默认事件标识();
        附加吸血尾缀(玩家.唯一标识(), 有效事件标识);
        调试日志器.调试("消息服务",
                "记录技能日志：玩家=%s 事件=%s 键=%s 文本=%s",
                玩家.唯一标识(), 有效事件标识, 键, 解析后文本);
        日志合并管理器.添加技能日志(玩家.唯一标识(), 有效事件标识, 解析后文本);
    }

    @Override
    public void 发送战斗日志(玩家快照 玩家, String 键, Object... 参数) {
        发送战斗日志(玩家, 默认事件标识(), 10, 键, 参数);
    }

    @Override
    public void 发送战斗日志(玩家快照 玩家, UUID 事件标识, String 键, Object... 参数) {
        发送战斗日志(玩家, 事件标识, 10, 键, 参数);
    }

    @Override
    public void 发送战斗日志(玩家快照 玩家, int 优先级, String 键, Object... 参数) {
        发送战斗日志(玩家, 默认事件标识(), 优先级, 键, 参数);
    }

    @Override
    public void 发送战斗日志(玩家快照 玩家, UUID 事件标识, int 优先级, String 键, Object... 参数) {
        if (!检查战斗日志开关(玩家.唯一标识())) {
            战斗日志上下文管理器.清除上下文(玩家.唯一标识(), 事件标识);
            return;
        }
        String 文本 = 翻译服务.获取(键);
        String 解析后文本 = 关键词解析器.解析(文本, 玩家.唯一标识(), 参数);
        UUID 有效事件标识 = 事件标识 != null ? 事件标识 : 默认事件标识();
        附加吸血尾缀(玩家.唯一标识(), 有效事件标识);
        调试日志器.调试("消息服务",
                "记录战斗日志：玩家=%s 事件=%s 优先级=%d 键=%s 文本=%s",
                玩家.唯一标识(), 有效事件标识, 优先级, 键, 解析后文本);
        日志合并管理器.添加战斗日志(玩家.唯一标识(), 有效事件标识, 解析后文本, 优先级);
    }

    @Override
    public void 发送错误(玩家快照 玩家, String 键, Object... 参数) {
        Player 实体玩家 = Bukkit.getPlayer(玩家.唯一标识());
        if (实体玩家 == null || !实体玩家.isOnline()) {
            return;
        }
        String 前缀 = 翻译服务.获取(错误前缀键);
        String 文本 = 翻译服务.获取(键);
        String 解析后文本 = 关键词解析器.解析(文本, 玩家.唯一标识(), 参数);
        实体玩家.sendMessage(迷你消息.deserialize(前缀 + 文本分隔符 + 解析后文本));
    }

    private boolean 检查技能日志开关(UUID 玩家标识) {
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        return 会话可选.map(玩家会话::获取技能日志开关)
                .orElseGet(玩家服务::获取默认技能日志开关);
    }

    private boolean 检查战斗日志开关(UUID 玩家标识) {
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        return 会话可选.map(玩家会话::获取战斗日志开关)
                .orElseGet(玩家服务::获取默认战斗日志开关);
    }

    private UUID 默认事件标识() {
        UUID 事件标识 = 事件日志上下文.获取当前事件标识或空();
        return 事件标识 != null ? 事件标识 : UUID.randomUUID();
    }

    /**
     * 将当前事件累计的吸血量登记为吸血尾缀（数值），由日志合并管理器在合并句末尾统一
     * 求和并输出唯一一条吸血日志（禁止换行，着色与句号由翻译模板在合并时解析提供）。
     */
    private void 附加吸血尾缀(UUID 玩家标识, UUID 事件标识) {
        double 吸血量 = 战斗日志上下文管理器.获取并清除吸血量(玩家标识, 事件标识);
        if (吸血量 <= 0) {
            return;
        }
        日志合并管理器.设置吸血尾缀(玩家标识, 事件标识, 吸血量);
    }
}
