package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.内存战斗服务;
import mljy.基础设施层.调试日志器;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class 伤害探针监听器 implements Listener {
    private static final String 模块名 = "伤害探针监听器";
    private final NamespacedKey 技能伤害键;

    @Inject
    public 伤害探针监听器(JavaPlugin 插件) {
        this.技能伤害键 = new NamespacedKey(插件, 内存战斗服务.技能伤害标记键);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void 探针最低优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "LOWEST");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void 探针低优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "LOW");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void 探针普通优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "NORMAL");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void 探针高优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "HIGH");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void 探针最高优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "HIGHEST");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void 探针监控优先级(EntityDamageByEntityEvent 事件) {
        记录探针(事件, "MONITOR");
    }

    private void 记录探针(EntityDamageByEntityEvent 事件, String 优先级) {
        Entity 目标实体 = 事件.getEntity();
        if (!(目标实体 instanceof LivingEntity 生物)) {
            return;
        }
        PersistentDataContainer pdc = 生物.getPersistentDataContainer();
        boolean 是XRM技能伤害 = pdc != null && pdc.has(技能伤害键, PersistentDataType.DOUBLE);
        if (!是XRM技能伤害) {
            return;
        }
        调试日志器.调试(模块名,
                "探针[%s]：目标=%s 目标名称=%s 伤害=%.2f 最终伤害=%.2f 原因=%s 攻击者=%s 取消=%s",
                优先级, 生物.getUniqueId(), 生物.getName(),
                事件.getDamage(), 事件.getFinalDamage(),
                事件.getCause(), 事件.getDamager(), 事件.isCancelled());
    }
}
