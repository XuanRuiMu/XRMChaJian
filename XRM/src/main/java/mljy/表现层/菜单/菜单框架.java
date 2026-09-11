package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class 菜单框架 implements InventoryHolder {
    private static final int 最小尺寸 = 9;
    private static final int 最大尺寸 = 54;
    private static final int 尺寸步长 = 9;
    private static final String 左键 = "左键";
    private static final String 默认物品 = "STONE";
    private static final String 命令前缀 = "/";

    protected final 翻译服务 翻译服务;
    protected final 关键词解析器 关键词解析器;
    protected final 菜单占位符 占位符;
    protected final 菜单条件 条件;
    protected final MiniMessage 迷你消息;
    protected final Provider<菜单服务> 菜单服务提供器;

    private final Map<UUID, 菜单会话> 打开菜单表 = new ConcurrentHashMap<>();

    @Inject
    protected 菜单框架(翻译服务 翻译服务,
                      关键词解析器 关键词解析器,
                      菜单占位符 占位符,
                      菜单条件 条件,
                      Provider<菜单服务> 菜单服务提供器) {
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.占位符 = 占位符;
        this.条件 = 条件;
        this.迷你消息 = MiniMessage.miniMessage();
        this.菜单服务提供器 = 菜单服务提供器;
    }

    public abstract String 获取菜单标识();

    public abstract 菜单配置 构建配置(Player 玩家);

    public boolean 打开(Player 玩家) {
        菜单配置 配置 = 构建配置(玩家);
        if (!检查菜单权限(玩家, 配置)) {
            发送错误(玩家, "菜单框架.菜单.无权限");
            return false;
        }
        if (!条件.检查全部(玩家, 配置.条件())) {
            return false;
        }
        Inventory 界面 = 创建界面(玩家, 配置);
        菜单会话 会话 = new 菜单会话(获取菜单标识(), 配置, 界面);
        打开菜单表.put(玩家.getUniqueId(), 会话);
        玩家.openInventory(界面);
        return true;
    }

    public void 关闭(UUID 玩家标识) {
        打开菜单表.remove(玩家标识);
    }

    public boolean 处理点击(Player 玩家, int 槽位, String 点击类型) {
        菜单会话 会话 = 打开菜单表.get(玩家.getUniqueId());
        if (会话 == null) {
            return false;
        }
        if (!会话.菜单标识().equals(获取菜单标识())) {
            return false;
        }
        菜单按钮 按钮 = 查找按钮(会话.配置(), 槽位);
        if (按钮 == null) {
            return true;
        }
        菜单动作 动作 = 查找动作(按钮, 点击类型);
        if (动作 == null) {
            return true;
        }
        执行动作(玩家, 动作);
        return true;
    }

    public boolean 是否打开(UUID 玩家标识) {
        菜单会话 会话 = 打开菜单表.get(玩家标识);
        return 会话 != null && 会话.菜单标识().equals(获取菜单标识());
    }

    private boolean 检查菜单权限(Player 玩家, 菜单配置 配置) {
        if (配置.权限() == null || 配置.权限().isBlank()) {
            return true;
        }
        return 玩家.hasPermission(配置.权限());
    }

    private Inventory 创建界面(Player 玩家, 菜单配置 配置) {
        int 大小 = 规范化大小(配置.大小());
        String 标题 = 替换占位符(玩家, 配置.标题());
        Inventory 界面 = Bukkit.createInventory(this, 大小, 迷你消息.deserialize(标题));
        填充按钮(玩家, 界面, 配置);
        return 界面;
    }

    private void 填充按钮(Player 玩家, Inventory 界面, 菜单配置 配置) {
        for (菜单按钮 按钮 : 配置.按钮()) {
            if (!检查按钮权限(玩家, 按钮)) {
                continue;
            }
            if (!条件.检查全部(玩家, 按钮.条件())) {
                continue;
            }
            ItemStack 物品 = 创建物品(玩家, 按钮);
            int 槽位 = Math.max(0, Math.min(界面.getSize() - 1, 按钮.槽位()));
            界面.setItem(槽位, 物品);
        }
    }

    private boolean 检查按钮权限(Player 玩家, 菜单按钮 按钮) {
        if (按钮.权限() == null || 按钮.权限().isBlank()) {
            return true;
        }
        return 玩家.hasPermission(按钮.权限());
    }

    private ItemStack 创建物品(Player 玩家, 菜单按钮 按钮) {
        Material 材质 = 解析材质(按钮.物品标识());
        ItemStack 物品 = new ItemStack(材质);
        ItemMeta 元数据 = 物品.getItemMeta();
        if (元数据 != null) {
            String 显示名 = 替换占位符(玩家, 按钮.显示名());
            元数据.displayName(迷你消息.deserialize(显示名));
            List<net.kyori.adventure.text.Component> 描述 = new ArrayList<>();
            for (String 行 : 按钮.描述()) {
                描述.add(迷你消息.deserialize(替换占位符(玩家, 行)));
            }
            元数据.lore(描述);
            if (按钮.附魔光效()) {
                应用附魔光效(物品, 元数据);
            } else {
                物品.setItemMeta(元数据);
            }
        }
        return 物品;
    }

    private void 应用附魔光效(ItemStack 物品, ItemMeta 元数据) {
        元数据.addEnchant(Enchantment.UNBREAKING, 1, true);
        元数据.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        物品.setItemMeta(元数据);
    }

    private Material 解析材质(String 物品标识) {
        if (物品标识 == null || 物品标识.isBlank()) {
            return Material.valueOf(默认物品);
        }
        try {
            return Material.valueOf(物品标识.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.valueOf(默认物品);
        }
    }

    private 菜单按钮 查找按钮(菜单配置 配置, int 槽位) {
        for (菜单按钮 按钮 : 配置.按钮()) {
            if (按钮.槽位() == 槽位) {
                return 按钮;
            }
        }
        return null;
    }

    private 菜单动作 查找动作(菜单按钮 按钮, String 点击类型) {
        Map<String, 菜单动作> 动作表 = 按钮.动作();
        if (动作表 == null || 动作表.isEmpty()) {
            return null;
        }
        菜单动作 动作 = 动作表.get(点击类型);
        if (动作 != null) {
            return 动作;
        }
        return 动作表.get(左键);
    }

    private void 执行动作(Player 玩家, 菜单动作 动作) {
        switch (动作.类型()) {
            case 关闭菜单 -> {
                玩家.closeInventory();
                关闭(玩家.getUniqueId());
            }
            case 发送消息 -> {
                String 文本 = 翻译服务.获取(动作.参数());
                String 解析后 = 关键词解析器.解析(文本, 玩家.getUniqueId());
                玩家.sendMessage(迷你消息.deserialize(解析后));
            }
            case 执行命令 -> 执行命令动作(玩家, 动作.参数());
            case 打开菜单 -> 打开子菜单(玩家, 动作.参数());
            default -> {
            }
        }
    }

    private void 打开子菜单(Player 玩家, String 菜单标识) {
        玩家.closeInventory();
        关闭(玩家.getUniqueId());
        菜单服务提供器.get().打开菜单(玩家, 菜单标识);
    }

    private void 执行命令动作(Player 玩家, String 命令) {
        String 实际命令 = 命令.startsWith(命令前缀) ? 命令.substring(命令前缀.length()) : 命令;
        Bukkit.dispatchCommand(玩家, 实际命令);
    }

    private String 替换占位符(Player 玩家, String 文本) {
        if (文本 == null || 文本.isEmpty()) {
            return "";
        }
        String 替换后 = 占位符.替换(文本, 玩家);
        return 关键词解析器.解析(替换后, 玩家.getUniqueId());
    }

    private int 规范化大小(int 大小) {
        if (大小 < 最小尺寸) {
            return 最小尺寸;
        }
        if (大小 > 最大尺寸) {
            return 最大尺寸;
        }
        return ((大小 + 尺寸步长 - 1) / 尺寸步长) * 尺寸步长;
    }

    private void 发送错误(Player 玩家, String 键) {
        String 文本 = 翻译服务.获取(键);
        String 解析后 = 关键词解析器.解析(文本, 玩家.getUniqueId());
        玩家.sendMessage(迷你消息.deserialize(解析后));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    private record 菜单会话(String 菜单标识, 菜单配置 配置, Inventory 界面) {
    }
}
