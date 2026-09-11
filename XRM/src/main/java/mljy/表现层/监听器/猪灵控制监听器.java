package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTransformEvent.TransformReason;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class 猪灵控制监听器 implements Listener {
    private static final int 最小间隔 = 1;

    private final 额外功能配置服务 配置服务;
    private final JavaPlugin 插件;

    @Inject
    public 猪灵控制监听器(额外功能配置服务 配置服务, JavaPlugin 插件) {
        this.配置服务 = 配置服务;
        this.插件 = 插件;
        启动定时任务();
    }

    private void 启动定时任务() {
        if (!配置服务.总开关() || !配置服务.猪灵永久敌对()) {
            return;
        }
        int 间隔 = Math.max(最小间隔, 配置服务.猪灵检查间隔Tick());
        Bukkit.getScheduler().runTaskTimer(插件, this::强制猪灵敌对, 间隔, 间隔);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 猪灵拾取物品(EntityPickupItemEvent 事件) {
        if (!配置服务.总开关() || !配置服务.猪灵取消以物易物()) {
            return;
        }
        if (事件.getEntity() instanceof Piglin) {
            ItemStack 物品 = 事件.getItem().getItemStack();
            if (物品.getType() == Material.GOLD_INGOT) {
                事件.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 玩家交互实体(PlayerInteractEntityEvent 事件) {
        if (!配置服务.总开关() || !配置服务.猪灵取消以物易物()) {
            return;
        }
        if (事件.getRightClicked() instanceof Piglin) {
            ItemStack 主手物品 = 事件.getPlayer().getInventory().getItemInMainHand();
            ItemStack 副手物品 = 事件.getPlayer().getInventory().getItemInOffHand();
            if (主手物品.getType() == Material.GOLD_INGOT || 副手物品.getType() == Material.GOLD_INGOT) {
                事件.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 实体转换(EntityTransformEvent 事件) {
        if (!配置服务.总开关() || !配置服务.猪灵阻止僵尸化()) {
            return;
        }
        if (事件.getTransformReason() == TransformReason.PIGLIN_ZOMBIFIED) {
            事件.setCancelled(true);
        }
    }

    private void 强制猪灵敌对() {
        for (World 世界 : Bukkit.getWorlds()) {
            for (Piglin 猪灵 : 世界.getEntitiesByClass(Piglin.class)) {
                强制敌对玩家(猪灵, 配置服务.猪灵索敌范围());
            }
        }
    }

    private void 强制敌对玩家(Mob 怪物, double 范围) {
        if (怪物.getTarget() instanceof Player) {
            return;
        }
        Player 最近 = 找最近玩家(怪物, 范围);
        if (最近 != null) {
            怪物.setTarget(最近);
        }
    }

    private Player 找最近玩家(Entity 实体, double 范围) {
        Player 最近 = null;
        double 最近距离平方 = 范围 * 范围;
        for (Player 玩家 : 实体.getWorld().getPlayers()) {
            if (玩家.getGameMode() == GameMode.SPECTATOR || 玩家.isDead()) {
                continue;
            }
            double 距离平方 = 玩家.getLocation().distanceSquared(实体.getLocation());
            if (距离平方 <= 最近距离平方) {
                最近距离平方 = 距离平方;
                最近 = 玩家;
            }
        }
        return 最近;
    }
}
