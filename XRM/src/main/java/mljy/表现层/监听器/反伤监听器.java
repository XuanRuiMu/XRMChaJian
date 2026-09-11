package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.属性.属性元数据;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.玩家.玩家快照;
import mljy.基础设施层.数值验证器;
import mljy.基础设施层.调试日志器;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * 反伤监听器。
 * 可插入性验证（需求.md 第 5 行）：新增反伤属性仅需本文件 + 玄锐暮插件注册 + 翻译文件，共 3 处变更。
 *
 * 设计：
 * - 反伤作为扩展属性存储在 属性快照.扩展属性 Map 中，无需修改 record 字段列表。
 * - 监听 EntityDamageByEntityEvent，受击玩家将部分伤害反弹给攻击者。
 * - 反伤比例 = 反伤值 × 反伤比例系数（每 1 点反伤反弹 1% 伤害）。
 * - 使用 ThreadLocal 标记防止反弹伤害再次触发反伤（无限递归）。
 * - 构造时自注册属性元数据，使 /管理 属性 命令自动支持反伤的查看与临时添加。
 */
public class 反伤监听器 implements Listener {
    private static final String 反伤属性名 = "反伤";
    private static final String 反伤显示翻译键 = "管理员指令.属性.查看.反伤";
    private static final double 反伤比例系数 = 0.01;

    private static final ThreadLocal<Boolean> 反伤处理中 = ThreadLocal.withInitial(() -> false);

    private final 玩家服务 玩家服务;
    private final 属性计算服务 属性计算服务;
    private final 属性注册表 属性注册表;

    @Inject
    public 反伤监听器(玩家服务 玩家服务, 属性计算服务 属性计算服务,
                     属性注册表 属性注册表) {
        this.玩家服务 = 玩家服务;
        this.属性计算服务 = 属性计算服务;
        this.属性注册表 = 属性注册表;
        属性注册表.注册(new 属性元数据(反伤属性名, 反伤属性名, 反伤显示翻译键));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 实体被攻击(EntityDamageByEntityEvent 事件) {
        if (反伤处理中.get()) {
            return;
        }
        if (!(事件.getEntity() instanceof Player 受击者)) {
            return;
        }
        if (!(事件.getDamager() instanceof LivingEntity 攻击者)) {
            return;
        }
        UUID 受击者标识 = 受击者.getUniqueId();
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(受击者标识);
        if (快照可选.isEmpty()) {
            return;
        }
        属性快照 属性 = 属性计算服务.计算(快照可选.get());
        double 反伤值 = 属性.获取扩展属性(反伤属性名);
        if (!数值验证器.是有限值(反伤值) || 反伤值 <= 0) {
            return;
        }
        double 原始伤害 = 事件.getFinalDamage();
        if (!数值验证器.是有限值(原始伤害) || 原始伤害 <= 0) {
            return;
        }
        double 反伤比例 = 反伤值 * 反伤比例系数;
        double 反弹伤害 = 原始伤害 * 反伤比例;
        if (反弹伤害 <= 0) {
            return;
        }
        反伤处理中.set(true);
        try {
            攻击者.damage(反弹伤害, 受击者);
        } finally {
            反伤处理中.set(false);
        }
        调试日志器.调试("反伤监听器", "反伤：受击者=%s 攻击者=%s 原始伤害=%.2f 反伤值=%.2f 反弹伤害=%.2f",
                受击者标识, 攻击者.getUniqueId(), 原始伤害, 反伤值, 反弹伤害);
    }
}
