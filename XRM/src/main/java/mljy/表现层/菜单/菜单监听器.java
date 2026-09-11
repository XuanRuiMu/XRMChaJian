package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.调试日志器;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

@Singleton
public class 菜单监听器 implements Listener {
    private static final String 左键 = "LEFT";
    private static final String 右键 = "RIGHT";
    private static final String 中键 = "MIDDLE";
    private static final String 默认点击 = "左键";

    @Inject
    public 菜单监听器() {
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 点击事件(InventoryClickEvent 事件) {
        InventoryHolder 持有者 = 事件.getInventory().getHolder();
        if (!(持有者 instanceof 菜单框架 菜单)) {
            return;
        }
        事件.setCancelled(true);
        if (!(事件.getWhoClicked() instanceof Player 玩家)) {
            return;
        }
        if (事件.getClickedInventory() == null || !事件.getClickedInventory().equals(事件.getView().getTopInventory())) {
            return;
        }
        int 槽位 = 事件.getSlot();
        String 点击类型 = 转换点击类型(事件.getClick().name());
        调试日志器.调试("菜单", "点击菜单项：玩家=%s 菜单=%s 槽位=%d 点击=%s", 玩家.getName(), 菜单.getClass().getSimpleName(), 槽位, 点击类型);
        菜单.处理点击(玩家, 槽位, 点击类型);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void 关闭事件(InventoryCloseEvent 事件) {
        InventoryHolder 持有者 = 事件.getInventory().getHolder();
        if (!(持有者 instanceof 菜单框架 菜单)) {
            return;
        }
        if (!(事件.getPlayer() instanceof Player 玩家)) {
            return;
        }
        调试日志器.调试("菜单", "关闭菜单：玩家=%s 菜单=%s", 玩家.getName(), 菜单.getClass().getSimpleName());
        菜单.关闭(玩家.getUniqueId());
    }

    private String 转换点击类型(String bukkit点击) {
        return switch (bukkit点击) {
            case 左键 -> "左键";
            case 右键 -> "右键";
            case 中键 -> "中键";
            default -> 默认点击;
        };
    }
}
