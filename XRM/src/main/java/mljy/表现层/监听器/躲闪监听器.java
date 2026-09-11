package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.业务层.消息服务;
import mljy.业务层.消息.伤害日志格式化服务;
import mljy.基础设施层.事件日志上下文;
import mljy.基础设施层.内存战斗服务;
import mljy.基础设施层.数值验证器;
import mljy.基础设施层.调试日志器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 躲闪监听器。
 * 监听 EntityDamageByEntityEvent，对非 XRM 技能伤害的普通攻击进行躲闪判定。
 * 需求.md 第 87 行："每有1点躲闪，你就有1%几率免疫普通攻击"。
 *
 * 躲闪判定逻辑：
 * 1. 目标必须是玩家
 * 2. 跳过 XRM 技能伤害（通过 PersistentDataContainer 识别，技能伤害不躲闪）
 * 3. 计算目标玩家的躲闪几率（属性计算服务应用通用递减）
 * 4. 随机判定，躲闪成功则取消事件并发送提示消息
 *
 * 优先级 HIGH：在反伤监听器(LOW)之后、战斗监听器(MONITOR)之前执行。
 * 躲闪成功时取消事件后，战斗监听器(MONITOR, ignoreCancelled=true)不再处理。
 * 注意：反伤监听器(LOW)在躲闪之前执行，反伤效果在躲闪判定前已生效。
 *
 * 技能按键绑定监听器(LOW, ignoreCancelled=false)与反伤监听器(LOW)在躲闪之前执行，
 * 因此玩家手持技能物品攻击时，技能会在躲闪判定前触发并造成 XRM 技能伤害。
 * 即使躲闪取消了原始普通攻击，技能伤害仍已造成。
 * 这符合需求"免疫普通攻击"的语义（技能伤害不属于普通攻击）。
 */
public class 躲闪监听器 implements Listener {
    private static final String 躲闪提示键 = "战斗日志.躲闪";
    private static final String 躲闪几率派生名 = "躲闪几率";

    private final 玩家服务 玩家服务;
    private final 属性计算服务 属性计算服务;
    private final 消息服务 消息服务;
    private final 伤害日志格式化服务 伤害日志格式化服务;
    private final NamespacedKey 技能伤害键;

    @Inject
    public 躲闪监听器(玩家服务 玩家服务, 属性计算服务 属性计算服务,
                     消息服务 消息服务,
                     伤害日志格式化服务 伤害日志格式化服务,
                     JavaPlugin 插件) {
        this.玩家服务 = 玩家服务;
        this.属性计算服务 = 属性计算服务;
        this.消息服务 = 消息服务;
        this.伤害日志格式化服务 = 伤害日志格式化服务;
        this.技能伤害键 = new NamespacedKey(插件, 内存战斗服务.技能伤害标记键);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 实体被攻击(EntityDamageByEntityEvent 事件) {
        if (!(事件.getEntity() instanceof Player 受击者)) {
            return;
        }
        UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        try {
            事件日志上下文.在事件中执行(事件标识, () -> {
                if (是XRM技能伤害(受击者)) {
                    return;
                }
                UUID 受击者标识 = 受击者.getUniqueId();
                Optional<玩家快照> 快照可选 = 玩家服务.获取快照(受击者标识);
                if (快照可选.isEmpty()) {
                    return;
                }
                属性快照 属性 = 属性计算服务.计算(快照可选.get());
                double 躲闪几率 = 属性计算服务.计算派生值(躲闪几率派生名, 属性);
                if (!数值验证器.是有限值(躲闪几率) || 躲闪几率 <= 0) {
                    return;
                }
                double 判定值 = ThreadLocalRandom.current().nextDouble();
                if (判定值 >= 躲闪几率) {
                    调试日志器.调试("躲闪监听器", "躲闪失败：受击者=%s 躲闪几率=%.4f 判定值=%.4f 事件=%s",
                            受击者标识, 躲闪几率, 判定值, 事件标识);
                    return;
                }
                事件.setCancelled(true);
                String 攻击者名 = 伤害日志格式化服务.获取实体来源名(事件.getDamager());
                消息服务.发送战斗日志(快照可选.get(), 事件标识, 躲闪提示键, 攻击者名);
                调试日志器.调试("躲闪监听器", "躲闪成功：受击者=%s 攻击者=%s 躲闪几率=%.4f 判定值=%.4f 事件=%s",
                        受击者标识, 攻击者名, 躲闪几率, 判定值, 事件标识);
            });
        } finally {
            事件日志上下文.清除();
        }
    }

    private boolean 是XRM技能伤害(Player 受击者) {
        return 受击者.getPersistentDataContainer().has(技能伤害键, PersistentDataType.DOUBLE);
    }
}
