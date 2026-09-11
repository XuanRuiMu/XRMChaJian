package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.驭空术服务;
import mljy.领域层.驭空术.飞行状态;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/**
 * 驭空术监听器。
 * 处理飞行物理、防止原版飞行、摔落保护、状态检测等Bukkit事件。
 */
public class 驭空术监听器 implements Listener {
    private final 驭空术服务 驭空术服务;

    @Inject
    public 驭空术监听器(驭空术服务 驭空术服务) {
        this.驭空术服务 = 驭空术服务;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家切换飞行(PlayerToggleFlightEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        if (玩家.getGameMode() == GameMode.CREATIVE || 玩家.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (驭空术服务.处理切换飞行(玩家)) {
            事件.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家移动(PlayerMoveEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        驭空术服务.处理玩家移动(玩家);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家退出载具(VehicleExitEvent 事件) {
        if (!(事件.getExited() instanceof Player 玩家)) {
            return;
        }
        飞行状态 状态 = 驭空术服务.获取飞行状态(玩家.getUniqueId());
        if (状态 == 飞行状态.飞行中 || 状态 == 飞行状态.起飞中) {
            驭空术服务.强制结束(玩家.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家退出(PlayerQuitEvent 事件) {
        驭空术服务.玩家退出清理(事件.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家受伤(EntityDamageEvent 事件) {
        if (!(事件.getEntity() instanceof Player 玩家)) {
            return;
        }
        if (事件.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (驭空术服务.是否摔落保护中(玩家.getUniqueId())) {
                事件.setCancelled(true);
            }
        }
        飞行状态 状态 = 驭空术服务.获取飞行状态(玩家.getUniqueId());
        if (状态 == 飞行状态.飞行中 || 状态 == 飞行状态.起飞中) {
            if (事件.getCause() == EntityDamageEvent.DamageCause.FALL
                    || 事件.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
                事件.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家游戏模式切换(PlayerGameModeChangeEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        飞行状态 状态 = 驭空术服务.获取飞行状态(玩家.getUniqueId());
        if (状态 == 飞行状态.飞行中 || 状态 == 飞行状态.起飞中) {
            驭空术服务.强制结束(玩家.getUniqueId());
        }
    }
}
