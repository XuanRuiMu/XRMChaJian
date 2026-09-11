package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class 菜单服务实现 implements 菜单服务 {
    private static final String 菜单不存在键 = "菜单框架.菜单.不存在";

    private final Map<String, 菜单框架> 菜单表 = new ConcurrentHashMap<>();
    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final MiniMessage 迷你消息;
    private final 总菜单 总菜单实例;
    private final 技能菜单 技能菜单实例;
    private final 任务菜单 任务菜单实例;
    private final 天赋菜单 天赋菜单实例;
    private final 静态菜单 静态菜单实例;

    @Inject
    public 菜单服务实现(翻译服务 翻译服务,
                      关键词解析器 关键词解析器,
                      总菜单 总菜单实例,
                      技能菜单 技能菜单实例,
                      任务菜单 任务菜单实例,
                      天赋菜单 天赋菜单实例,
                      静态菜单 静态菜单实例) {
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.迷你消息 = MiniMessage.miniMessage();
        this.总菜单实例 = 总菜单实例;
        this.技能菜单实例 = 技能菜单实例;
        this.任务菜单实例 = 任务菜单实例;
        this.天赋菜单实例 = 天赋菜单实例;
        this.静态菜单实例 = 静态菜单实例;
    }

    @Override
    public void 注册菜单() {
        注册菜单(总菜单实例);
        注册菜单(技能菜单实例);
        注册菜单(任务菜单实例);
        注册菜单(天赋菜单实例);
        注册菜单(静态菜单实例);
        调试日志器.调试("菜单服务", "已注册菜单: 总菜单/技能菜单/任务菜单/天赋菜单/静态菜单");
    }

    @Override
    public void 注册菜单(菜单框架 菜单) {
        菜单表.put(菜单.获取菜单标识(), 菜单);
    }

    @Override
    public Optional<菜单框架> 获取菜单(String 菜单标识) {
        return Optional.ofNullable(菜单表.get(菜单标识));
    }

    @Override
    public boolean 打开菜单(Player 玩家, String 菜单标识) {
        菜单框架 菜单 = 菜单表.get(菜单标识);
        if (菜单 == null) {
            发送菜单不存在(玩家, 菜单标识);
            return false;
        }
        return 菜单.打开(玩家);
    }

    @Override
    public void 关闭菜单(Player 玩家) {
        for (菜单框架 菜单 : 菜单表.values()) {
            if (菜单.是否打开(玩家.getUniqueId())) {
                菜单.关闭(玩家.getUniqueId());
            }
        }
        玩家.closeInventory();
    }

    private void 发送菜单不存在(Player 玩家, String 菜单标识) {
        String 文本 = 翻译服务.获取(菜单不存在键, 菜单标识);
        String 解析后 = 关键词解析器.解析(文本, 玩家.getUniqueId(), 菜单标识);
        玩家.sendMessage(迷你消息.deserialize(解析后));
    }
}
