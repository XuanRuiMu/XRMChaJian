package mljy.领域层.技能;

import org.bukkit.Material;

import java.util.Set;

/**
 * 技能按键类型枚举。
 * 对应需求文档中9个技能槽位，每个槽位绑定一种工具类型。
 * 槽位顺序：剑/斧/镐/铲/锄/矛/重锤/书/自定义。
 */
public enum 技能按键类型 {
    第一技能(1, Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    )),
    第二技能(2, Set.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    )),
    第三技能(3, Set.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    )),
    第四技能(4, Set.of(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
    )),
    第五技能(5, Set.of(
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
    )),
    第六技能(6, Set.of(
            Material.TRIDENT,
            Material.WOODEN_SPEAR, Material.STONE_SPEAR, Material.IRON_SPEAR,
            Material.GOLDEN_SPEAR, Material.DIAMOND_SPEAR, Material.NETHERITE_SPEAR,
            Material.COPPER_SPEAR
    )),
    第七技能(7, Set.of(
            Material.MACE
    )),
    第八技能(8, Set.of(
            Material.BOOK, Material.WRITABLE_BOOK, Material.WRITTEN_BOOK,
            Material.ENCHANTED_BOOK, Material.KNOWLEDGE_BOOK
    )),
    第九技能(9, Set.of());

    private final int 槽位编号;
    private final Set<Material> 绑定物品;

    技能按键类型(int 槽位编号, Set<Material> 绑定物品) {
        this.槽位编号 = 槽位编号;
        this.绑定物品 = 绑定物品;
    }

    public int 获取槽位编号() {
        return 槽位编号;
    }

    public Set<Material> 获取绑定物品() {
        return 绑定物品;
    }

    /**
     * 根据物品类型查询对应的技能按键类型。
     * 返回null表示该物品不绑定任何技能槽位。
     */
    public static 技能按键类型 从物品查询(Material 物品) {
        if (物品 == null) {
            return null;
        }
        for (技能按键类型 类型 : values()) {
            if (类型.绑定物品.contains(物品)) {
                return 类型;
            }
        }
        return null;
    }
}
