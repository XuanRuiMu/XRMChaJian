package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class 岩浆块免疫监听器 implements Listener {
    private final 额外功能配置服务 配置服务;

    @Inject
    public 岩浆块免疫监听器(额外功能配置服务 配置服务) {
        this.配置服务 = 配置服务;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 实体受伤(EntityDamageEvent 事件) {
        if (!配置服务.总开关()) {
            return;
        }
        if (事件.getCause() == DamageCause.CONTACT && 事件 instanceof EntityDamageByBlockEvent 方块事件) {
            Block 方块 = 方块事件.getDamager();
            if (方块 != null && 方块.getType() == Material.MAGMA_BLOCK && 岩浆块免疫(事件.getEntity())) {
                事件.setCancelled(true);
            }
        }
    }

    private boolean 岩浆块免疫(Entity 实体) {
        if (实体 instanceof Player && 配置服务.玩家岩浆块免疫()) {
            return true;
        }
        return 实体 instanceof Wolf && 配置服务.狗岩浆块免疫();
    }
}
