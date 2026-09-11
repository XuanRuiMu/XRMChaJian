package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.内存战斗服务;
import mljy.基础设施层.调试日志器;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FP-03 EliteMobs伤害修复监听器。
 *
 * 根因（基于 EliteMobs 官方文档 wiki.nightbreak.io/EliteMobs/damage_system 与 docs.superiormc.cn/elitemobs-wiki/zhi-nan/shang-hai-xi-tong）：
 * - EliteMobs 10 通过监听 EntityDamageByEntityEvent 重新计算"玩家对精英怪"的伤害
 * - 重新计算公式基于玩家武器技能等级 + 物品等级 + Boss等级 + 技能加成，与传入 damage() 的数值无关
 * - 当玩家武器技能/物品等级较低时，公式输出值小于1，被 max(formulaDamage, 1) 下限提升为1.0
 * - 这导致 XRM 技能伤害 2.0/4.0/8.0 被统一替换为 1.0
 *
 * 修复方案（方案D）：
 * - 在 HIGHEST 优先级监听 EntityDamageByEntityEvent
 * - 通过 PDC 键 xrm_skill_damage 识别 XRM 技能伤害事件，并读取预期伤害值
 * - 若当前伤害值与预期值不一致，说明被 EliteMobs 归一化修改过，强制恢复为 XRM 预期值
 * - 若事件被 EliteMobs 取消，恢复事件为未取消状态（XRM 技能伤害必须生效）
 *
 * 安全性：
 * - 仅修改 PDC 中含 xrm_skill_damage 键的事件，不影响普通伤害或其他插件
 * - EliteMobs 未安装时，当前伤害等于预期伤害，监听器无操作，无副作用
 * - 内存战斗服务在调用 damage() 前设置 PDC，调用后清除 PDC，确保键仅在一次伤害事件中有效
 */
public class EliteMobs伤害修复监听器 implements Listener {
    private static final String 模块名 = "EliteMobs伤害修复";
    private final NamespacedKey 技能伤害键;

    @Inject
    public EliteMobs伤害修复监听器(JavaPlugin 插件) {
        this.技能伤害键 = new NamespacedKey(插件, 内存战斗服务.技能伤害标记键);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void 修复EliteMobs伤害归一化(EntityDamageByEntityEvent 事件) {
        if (!(事件.getEntity() instanceof LivingEntity 生物)) {
            return;
        }
        PersistentDataContainer pdc = 生物.getPersistentDataContainer();
        if (pdc == null) {
            return;
        }
        Double 预期伤害 = pdc.get(技能伤害键, PersistentDataType.DOUBLE);
        if (预期伤害 == null || 预期伤害 <= 0 || Double.isNaN(预期伤害) || Double.isInfinite(预期伤害)) {
            return;
        }
        double 当前伤害 = 事件.getDamage();
        if (Double.compare(当前伤害, 预期伤害) == 0) {
            return;
        }
        if (事件.isCancelled()) {
            事件.setCancelled(false);
        }
        事件.setDamage(预期伤害);
        调试日志器.调试(模块名,
                "修复EliteMobs归一化：目标=%s 原伤害=%.2f 修复后伤害=%.2f 取消状态=false",
                生物.getUniqueId(), 当前伤害, 预期伤害);
    }
}
