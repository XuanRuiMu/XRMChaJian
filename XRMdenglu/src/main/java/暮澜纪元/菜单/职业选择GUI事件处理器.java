package 暮澜纪元.菜单;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import 暮澜纪元.职业.世界;
import 暮澜纪元.职业.职业;

/**
 * 职业选择GUI事件处理器
 */
public class 职业选择GUI事件处理器 implements Listener {

    private final 职业选择GUI gui;

    public 职业选择GUI事件处理器(职业选择GUI gui) {
        this.gui = gui;
    }

    /**
     * 处理点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent 事件) {
        Inventory 被点击的背包 = 事件.getClickedInventory();
        if (被点击的背包 == null) {
            return;
        }

        InventoryHolder 持有者 = 被点击的背包.getHolder();
        if (!(持有者 instanceof 菜单持有者)) {
            return;
        }

        事件.setCancelled(true);

        HumanEntity humanEntity = 事件.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player 玩家 = (Player) humanEntity;

        ItemStack 被点击物品 = 事件.getCurrentItem();
        if (被点击物品 == null || 被点击物品.getType().isAir()) {
            return;
        }

        int 槽位 = 事件.getSlot();

        菜单持有者 菜单持有者 = (菜单持有者) 持有者;
        switch (菜单持有者.获取类型()) {
            case 世界选择 -> handleWorldSelection(玩家, 槽位);
            case 专精选择 -> handleClassSelection(玩家, 槽位, 菜单持有者.获取世界索引());
        }
    }

    /**
     * 处理世界选择
     */
    private void handleWorldSelection(Player 玩家, int 槽位) {
        世界[] 世界列表 = 世界.values();
        if (槽位 >= 0 && 槽位 < 世界列表.length) {
            gui.选择世界(玩家, 世界列表[槽位]);
        }
    }

    /**
     * 处理专精选择
     */
    private void handleClassSelection(Player 玩家, int 槽位, int 世界索引) {
        世界[] 世界列表 = 世界.values();
        if (世界索引 < 0 || 世界索引 >= 世界列表.length) {
            return;
        }

        世界 所属世界 = 世界列表[世界索引];
        java.util.List<职业> 职业列表 = 所属世界.获取包含职业();

        if (槽位 >= 0 && 槽位 < 职业列表.size()) {
            职业 选择的职业 = 职业列表.get(槽位);
            gui.选择专精(玩家, 选择的职业);
        }
    }

    /**
     * 处理拖拽事件，防止物品被拖出
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent 事件) {
        Inventory 背包 = 事件.getInventory();
        if (背包 == null) {
            return;
        }

        InventoryHolder 持有者 = 背包.getHolder();
        if (持有者 instanceof 菜单持有者) {
            事件.setCancelled(true);
        }
    }
}
