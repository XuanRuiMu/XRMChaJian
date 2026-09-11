package mljy.业务层;

import com.google.inject.Inject;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * 吸血处理服务实现。
 * 处理吸血逻辑：每拥有1点吸血，则每造成1点伤害或造成1点治疗时，都会按比例治疗自身。
 * 吸血触发的治疗为独立治疗，不触发任何加成/减益计算（包括吸血、全能、暴击、精通、治疗加成等），避免无限循环。
 * 通过战斗日志上下文管理器累加吸血量，确保吸血日志附加在伤害来源的日志中。
 */
public class 吸血处理服务实现 implements 吸血处理服务 {
    private static final double 数值精度 = 10.0;

    private final 属性计算服务 属性计算服务;
    private final 战斗日志上下文管理器 战斗日志上下文管理器;

    @Inject
    public 吸血处理服务实现(属性计算服务 属性计算服务, 战斗日志上下文管理器 战斗日志上下文管理器) {
        this.属性计算服务 = 属性计算服务;
        this.战斗日志上下文管理器 = 战斗日志上下文管理器;
    }

    @Override
    public void 处理伤害吸血(玩家快照 施法者, double 伤害值) {
        处理吸血(施法者, 伤害值);
    }

    @Override
    public void 处理治疗吸血(玩家快照 施法者, double 治疗值) {
        处理吸血(施法者, 治疗值);
    }

    /**
     * 处理吸血核心逻辑。
     * 吸血量 = 数值 × 吸血比例（有效吸血×0.01）。
     * 吸血治疗直接调用Bukkit API，不走伤害计算服务，避免无限循环。
     * 吸血量累加到战斗日志上下文管理器。
     */
    private void 处理吸血(玩家快照 施法者, double 数值) {
        if (施法者 == null || 数值 <= 0) {
            return;
        }
        属性快照 施法者属性 = 属性计算服务.计算(施法者);
        double 吸血属性 = 施法者属性.吸血();
        if (吸血属性 <= 0) {
            return;
        }
        double 吸血比例 = 属性计算服务.计算派生值("吸血比例", 施法者属性);
        if (吸血比例 <= 0) {
            return;
        }
        double 吸血量 = 保留一位小数(数值 * 吸血比例);
        if (吸血量 <= 0) {
            调试日志器.调试("吸血处理服务", "跳过 原因=吸血量<=0 施法者=%s 原始数值=%.1f",
                    施法者.唯一标识(), 数值);
            return;
        }
        Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
            return;
        }
        double 当前生命值 = 玩家.getHealth();
        double 最大生命值 = 获取最大生命值(玩家);
        if (当前生命值 >= 最大生命值) {
            调试日志器.调试("吸血处理服务", "跳过 原因=生命值已满 施法者=%s 当前生命值=%.1f 最大生命值=%.1f",
                    施法者.唯一标识(), 当前生命值, 最大生命值);
            return;
        }
        double 新生命值 = Math.min(当前生命值 + 吸血量, 最大生命值);
        玩家.setHealth(新生命值);
        double 实际恢复量 = 新生命值 - 当前生命值;
        调试日志器.调试("吸血处理服务", "施法者=%s 原始数值=%.1f 吸血属性=%.1f 吸血比例=%.4f 吸血量=%.1f 当前生命值=%.1f 新生命值=%.1f 实际恢复量=%.1f",
                施法者.唯一标识(), 数值, 吸血属性, 吸血比例, 吸血量, 当前生命值, 新生命值, 实际恢复量);
        战斗日志上下文管理器.累加吸血量(施法者.唯一标识(), 事件日志上下文.获取当前事件标识或空(), 吸血量);
    }

    private double 获取最大生命值(Player 玩家) {
        AttributeInstance 属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        return 属性 != null ? 属性.getValue() : 20.0;
    }

    private double 保留一位小数(double 数值) {
        return Math.round(数值 * 数值精度) / 数值精度;
    }
}
