package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTeleportEvent;

public class 末影人控制监听器 implements Listener {
    private final 额外功能配置服务 配置服务;

    @Inject
    public 末影人控制监听器(额外功能配置服务 配置服务) {
        this.配置服务 = 配置服务;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 实体受伤(EntityDamageEvent 事件) {
        if (!配置服务.总开关()) {
            return;
        }
        if (事件.getCause() == DamageCause.DROWNING && 末影人水雨免疫(事件.getEntity())) {
            事件.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 末影人传送(EntityTeleportEvent 事件) {
        if (!配置服务.总开关() || !配置服务.末影人禁止传送()) {
            return;
        }
        if (事件.getEntity() instanceof Enderman) {
            事件.setCancelled(true);
        }
    }

    private boolean 末影人水雨免疫(Entity 实体) {
        return 配置服务.末影人免疫水雨伤害() && 实体 instanceof Enderman;
    }
}
