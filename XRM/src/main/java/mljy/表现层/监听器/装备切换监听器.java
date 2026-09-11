package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.调试日志器;
import mljy.业务层.战斗状态服务;
import mljy.业务层.消息服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 装备切换监听器。
 * 战斗中禁止更换头盔、护甲、护腿、靴子四个部位的装备。
 * 监听 InventoryClickEvent、InventoryDragEvent。
 */
public class 装备切换监听器 implements Listener {
    private static final String 战斗中禁止换装键 = "战斗状态管理器.状态.战斗中禁止更换装备";
    private static final int 玩家背包合成栏大小 = 5;
    private static final int 盔甲槽起始 = 5;
    private static final int 盔甲槽结束 = 8;

    private final 战斗状态服务 战斗状态服务;
    private final 消息服务 消息服务;
    private final 玩家服务 玩家服务;

    @Inject
    public 装备切换监听器(战斗状态服务 战斗状态服务, 消息服务 消息服务, 玩家服务 玩家服务) {
        this.战斗状态服务 = 战斗状态服务;
        this.消息服务 = 消息服务;
        this.玩家服务 = 玩家服务;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 背包点击(InventoryClickEvent 事件) {
        if (!(事件.getWhoClicked() instanceof Player 玩家)) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        if (!战斗状态服务.是否战斗中(玩家标识)) {
            return;
        }
        if (是盔甲槽操作(事件)) {
            事件.setCancelled(true);
            调试日志器.调试("装备切换监听器", "战斗中禁止换装 玩家=%s 被阻止的槽位=%d",
                    玩家.getName(), 事件.getSlot());
            发送禁止提示(玩家标识);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 背包拖拽(InventoryDragEvent 事件) {
        if (!(事件.getWhoClicked() instanceof Player 玩家)) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        if (!战斗状态服务.是否战斗中(玩家标识)) {
            return;
        }
        if (是盔甲槽拖拽(事件)) {
            事件.setCancelled(true);
            调试日志器.调试("装备切换监听器", "战斗中禁止换装 玩家=%s 被阻止的拖拽槽位=%s",
                    玩家.getName(), 事件.getRawSlots());
            发送禁止提示(玩家标识);
        }
    }

    private boolean 是盔甲槽操作(InventoryClickEvent 事件) {
        InventoryType.SlotType 槽位类型 = 事件.getSlotType();
        if (槽位类型 == InventoryType.SlotType.ARMOR) {
            return true;
        }
        if (事件.getClick().isShiftClick() && 事件.getCurrentItem() != null) {
            return 是盔甲物品(事件.getCurrentItem());
        }
        return false;
    }

    private boolean 是盔甲槽拖拽(InventoryDragEvent 事件) {
        Inventory 顶部 = 事件.getInventory();
        if (顶部.getType() != InventoryType.CRAFTING || 顶部.getSize() != 玩家背包合成栏大小) {
            return false;
        }
        for (int 原始槽位 : 事件.getRawSlots()) {
            if (原始槽位 >= 盔甲槽起始 && 原始槽位 <= 盔甲槽结束) {
                return true;
            }
        }
        return false;
    }

    private boolean 是盔甲物品(ItemStack 物品) {
        String 名称 = 物品.getType().name();
        return 名称.endsWith("_HELMET") || 名称.endsWith("_CHESTPLATE")
                || 名称.endsWith("_LEGGINGS") || 名称.endsWith("_BOOTS");
    }

    private void 发送禁止提示(UUID 玩家标识) {
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        if (快照可选.isPresent()) {
            消息服务.发送消息(快照可选.get(), 战斗中禁止换装键);
        }
    }
}
