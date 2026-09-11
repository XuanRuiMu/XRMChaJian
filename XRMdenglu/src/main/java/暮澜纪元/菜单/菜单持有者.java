package 暮澜纪元.菜单;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义菜单持有者，用于标识GUI菜单类型
 */
public class 菜单持有者 implements InventoryHolder {

    /**
     * 菜单类型枚举
     */
    public enum 菜单类型 {
        世界选择,
        专精选择
    }

    private final 菜单类型 类型;
    private final int 世界索引;

    /**
     * 创建世界选择菜单持有者
     */
    public 菜单持有者() {
        this.类型 = 菜单类型.世界选择;
        this.世界索引 = -1;
    }

    /**
     * 创建专精选择菜单持有者
     * @param 世界索引 所属世界的索引
     */
    public 菜单持有者(int 世界索引) {
        this.类型 = 菜单类型.专精选择;
        this.世界索引 = 世界索引;
    }

    /**
     * 获取菜单类型
     */
    public 菜单类型 获取类型() {
        return 类型;
    }

    /**
     * 获取世界索引（仅专精选择菜单有效）
     */
    public int 获取世界索引() {
        return 世界索引;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return null;
    }
}
