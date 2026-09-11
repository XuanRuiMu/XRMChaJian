package 暮澜纪元.菜单;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import 暮澜纪元.登录服插件;
import 暮澜纪元.数据.数据库连接管理器;
import 暮澜纪元.数据.玩家数据;
import 暮澜纪元.职业.世界;
import 暮澜纪元.职业.职业;
import 暮澜纪元.职业.角色类型;
import 暮澜纪元.配置.消息管理器;

import java.util.ArrayList;
import java.util.List;

/**
 * 职业选择GUI：处理玩家职业选择的界面逻辑。
 */
public class 职业选择GUI {

    private final 登录服插件 插件;
    private final 数据库连接管理器 数据库;
    private final 消息管理器 消息;
    private String 目标服务器名 = "mmorpg";

    public 职业选择GUI(登录服插件 插件, 数据库连接管理器 数据库) {
        this.插件 = 插件;
        this.数据库 = 数据库;
        this.消息 = 插件.获取消息管理器();
        this.目标服务器名 = 插件.获取登录配置().获取目标服务器();
    }

    public void 设置目标服务器名(String 服务器名) {
        this.目标服务器名 = 服务器名;
    }

    public 玩家数据 获取玩家数据(Player 玩家) {
        return this.数据库.获取或创建玩家数据(玩家.getUniqueId(), 玩家.getName());
    }

    @SuppressWarnings("null")
    public void 打开职业选择菜单(Player 玩家) {
        插件.获取消息管理器().系统日志("[GUI] 打开职业选择菜单，玩家: " + 玩家.getName());
        String 标题 = 消息.获取类翻译(职业选择GUI.class, "界面.世界选择标题", 玩家);
        if (标题 == null) {
            标题 = "";
        }
        Inventory 菜单 = Bukkit.createInventory(new 菜单持有者(), 18, MiniMessage.miniMessage().deserialize(标题));
        String 包含专精文本 = 消息.获取类翻译(职业选择GUI.class, "界面.包含专精", 玩家);
        if (包含专精文本 == null) {
            包含专精文本 = "";
        }

        int 槽位 = 0;
        for (世界 当前世界 : 世界.values()) {
            ItemStack 物品 = new ItemStack(this.获取世界材料(当前世界));
            ItemMeta 元数据 = 物品.getItemMeta();
            String 世界名称 = 消息.获取类翻译(职业选择GUI.class, "世界." + 当前世界.name(), 玩家);
            if (世界名称 == null) {
                世界名称 = "";
            }
            元数据.displayName(MiniMessage.miniMessage().deserialize(世界名称));
            ArrayList<Component> 说明 = new ArrayList<>();
            说明.add(MiniMessage.miniMessage().deserialize(包含专精文本));
            for (职业 当前职业 : 当前世界.获取包含职业()) {
                角色类型 类型 = 当前职业.获取角色类型();
                String 类型名称 = 消息.获取类翻译(职业选择GUI.class, "角色类型." + 类型.name(), 玩家);
                String 职业名称 = 消息.获取类翻译(职业选择GUI.class, "职业." + 当前职业.name(), 玩家);
                String 职业描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述." + 当前职业.name(), 玩家);
                说明.add(MiniMessage.miniMessage().deserialize("<bold>" + 职业名称 + "</bold> " + 类型名称));
                if (职业描述 != null && !职业描述.isEmpty()) {
                    String[] 描述行 = 职业描述.split("\\|");
                    for (String 行 : 描述行) {
                        行 = 行.trim();
                        if (!行.isEmpty()) {
                            Component 描述组件 = 消息.格式化文本(行);
                            描述组件 = 描述组件.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                            说明.add(描述组件);
                        }
                    }
                }
            }
            元数据.lore(说明);
            物品.setItemMeta(元数据);
            菜单.setItem(槽位++, 物品);
        }
        玩家.openInventory(菜单);
    }

    @SuppressWarnings("null")
    public void 打开专精选择菜单(Player 玩家, 世界 世界2) {
        插件.获取消息管理器().系统日志("[GUI] 打开专精选择菜单，玩家: " + 玩家.getName() + ", 世界: " + 世界2.name());
        String 世界名称 = 消息.获取类翻译(职业选择GUI.class, "世界." + 世界2.name(), 玩家);
        String 标题 = 消息.获取类翻译(职业选择GUI.class, "界面.职业选择标题", 玩家, 世界名称);
        if (标题 == null) {
            标题 = "";
        }
        List<职业> 职业列表 = 世界2.获取包含职业();
        int 菜单大小 = (职业列表.size() / 9 + 1) * 9;
        if (菜单大小 < 9) {
            菜单大小 = 9;
        }
        int 世界索引 = 世界2.ordinal();
        Inventory 菜单 = Bukkit.createInventory(new 菜单持有者(世界索引), 菜单大小, MiniMessage.miniMessage().deserialize(标题));
        String 世界标签 = 消息.获取类翻译(职业选择GUI.class, "界面.世界标签", 玩家);
        String 职责标签 = 消息.获取类翻译(职业选择GUI.class, "界面.角色类型标签", 玩家);
        int 槽位 = 0;
        for (职业 当前职业 : 职业列表) {
            ItemStack 物品 = new ItemStack(this.获取职业材料(当前职业));
            ItemMeta 元数据 = 物品.getItemMeta();
            角色类型 类型 = 当前职业.获取角色类型();
            String 类型名称 = 消息.获取类翻译(职业选择GUI.class, "角色类型." + 类型.name(), 玩家);
            String 职业名称 = 消息.获取类翻译(职业选择GUI.class, "职业." + 当前职业.name(), 玩家);
            String 职业描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述." + 当前职业.name(), 玩家);
            if (职业名称 == null) {
                职业名称 = "";
            }
            if (职业描述 == null) {
                职业描述 = "";
            }
            元数据.displayName(MiniMessage.miniMessage().deserialize(职业名称));
            ArrayList<Component> 说明 = new ArrayList<>();
            说明.add(MiniMessage.miniMessage().deserialize(世界标签 + 世界名称));
            说明.add(MiniMessage.miniMessage().deserialize(职责标签 + 类型名称));
            说明.add(Component.empty());
            if (职业描述 != null && !职业描述.isEmpty()) {
                String[] 描述行 = 职业描述.split("\\|");
                for (String 行 : 描述行) {
                    行 = 行.trim();
                    if (!行.isEmpty()) {
                        说明.add(消息.格式化文本(行));
                    }
                }
            }
            元数据.lore(说明);
            物品.setItemMeta(元数据);
            菜单.setItem(槽位++, 物品);
        }
        玩家.openInventory(菜单);
    }

    public void 选择世界(Player 玩家, 世界 世界2) {
        插件.获取消息管理器().系统日志("[GUI] 玩家 " + 玩家.getName() + " 选择世界: " + 世界2.name());
        玩家数据 数据 = this.获取玩家数据(玩家);
        boolean 首次选择 = 数据.获取职业() == null;
        String 世界名称 = 消息.获取类翻译(职业选择GUI.class, "世界." + 世界2.name(), 玩家);
        if (首次选择) {
            职业 初始职业 = 世界2.获取包含职业().get(0);
            数据.设置职业(初始职业);
            数据.设置等级(1);
            if (!this.数据库.保存玩家数据(数据)) {
                消息.发送类翻译(玩家, 职业选择GUI.class, "保存失败");
                return;
            }
            玩家.closeInventory();
            String 职业名称 = 消息.获取类翻译(职业选择GUI.class, "职业." + 初始职业.name(), 玩家);
            消息.发送类翻译(玩家, 职业选择GUI.class, "世界和职业已选择", 世界名称, 职业名称);
            插件.获取消息管理器().系统日志(玩家.getName() + " 选择了世界: " + 世界名称 + ", 职业: " + 职业名称 + ", 等级: 1");
            this.传送玩家到服务器(玩家);
        } else {
            if (!this.数据库.保存玩家数据(数据)) {
                消息.发送类翻译(玩家, 职业选择GUI.class, "保存失败");
                return;
            }
            玩家.closeInventory();
            this.传送玩家到服务器(玩家);
        }
    }

    private void 传送玩家到服务器(Player 玩家) {
        插件.获取消息管理器().系统日志("[GUI] 玩家 " + 玩家.getName() + " 正在传送至服务器: " + this.目标服务器名);
        消息.发送类翻译(玩家, 职业选择GUI.class, "服务器欢迎");
        插件.传送玩家到服务器(玩家, this.目标服务器名);
    }

    public void 选择专精(Player 玩家, 职业 职业2) {
        插件.获取消息管理器().系统日志("[GUI] 玩家 " + 玩家.getName() + " 选择专精: " + 职业2.name());
        玩家数据 数据 = this.获取玩家数据(玩家);
        boolean 首次选择职业 = 数据.获取职业() == null;
        数据.设置职业(职业2);
        if (首次选择职业) {
            数据.设置等级(1);
        }
        if (!this.数据库.保存玩家数据(数据)) {
            消息.发送类翻译(玩家, 职业选择GUI.class, "保存失败");
            return;
        }
        玩家.closeInventory();
        String 职业名称 = 消息.获取类翻译(职业选择GUI.class, "职业." + 职业2.name(), 玩家);
        消息.发送类翻译(玩家, 职业选择GUI.class, "职业已更改", 职业名称);
        插件.获取消息管理器().系统日志(玩家.getName() + " 更改职业为: " + 职业名称 + (首次选择职业 ? ", 初始等级: 1" : ""));
    }

    private Material 获取世界材料(世界 世界2) {
        return switch (世界2) {
            case 魔法世界 -> Material.ENCHANTED_BOOK;
            case 自然世界 -> Material.OAK_SAPLING;
            case 机械世界 -> Material.REDSTONE;
            case 神灵世界 -> Material.SOUL_LANTERN;
            case 圣洁世界 -> Material.BEACON;
            case 暗影世界 -> Material.WITHER_ROSE;
            case 元素世界 -> Material.BLAZE_POWDER;
            case 混沌世界 -> Material.END_PORTAL_FRAME;
        };
    }

    private Material 获取职业材料(职业 职业2) {
        角色类型 类型 = 职业2.获取角色类型();
        return switch (类型) {
            case 输出 -> Material.DIAMOND_SWORD;
            case 治疗 -> Material.GOLDEN_APPLE;
            case 坦克 -> Material.SHIELD;
        };
    }
}
