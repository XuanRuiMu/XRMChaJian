package mljy.表现层.监听器;

import mljy.业务层.战斗状态服务;
import mljy.业务层.消息服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("装备切换监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 装备切换监听器测试 {

    private static final String 战斗中禁止换装键 = "战斗状态管理器.状态.战斗中禁止更换装备";

    @Mock
    private 战斗状态服务 战斗状态服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 玩家服务 玩家服务;

    private 装备切换监听器 监听器;

    private UUID 玩家标识;
    private 玩家快照 快照;

    @BeforeEach
    void setUp() {
        监听器 = new 装备切换监听器(战斗状态服务, 消息服务, 玩家服务);

        玩家标识 = UUID.randomUUID();
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, null, Map.of(), null);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
    }

    private Player 创建玩家() {
        Player 玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        return 玩家;
    }

    private ItemStack 创建物品(Material 物品类型) {
        ItemStack 物品 = mock(ItemStack.class);
        when(物品.getType()).thenReturn(物品类型);
        return 物品;
    }

    private Inventory 创建玩家顶部背包() {
        Inventory 顶部 = mock(Inventory.class);
        when(顶部.getType()).thenReturn(InventoryType.CRAFTING);
        when(顶部.getSize()).thenReturn(5);
        return 顶部;
    }

    @Nested
    @DisplayName("战斗中点击盔甲槽 - 应取消")
    class 战斗中点击盔甲槽 {

        @Test
        @DisplayName("战斗中点击盔甲槽 - 应取消事件并发送提示")
        void 战斗中点击盔甲槽_应取消事件() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.ARMOR);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包点击(事件);

            verify(事件).setCancelled(true);
            verify(消息服务).发送消息(快照, 战斗中禁止换装键);
        }
    }

    @Nested
    @DisplayName("战斗中Shift点击盔甲物品 - 应取消")
    class 战斗中Shift点击盔甲物品 {

        @Test
        @DisplayName("战斗中Shift点击铁头盔 - 应取消事件")
        void 战斗中Shift点击铁头盔_应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 头盔 = 创建物品(Material.IRON_HELMET);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.CONTAINER);
            when(事件.getClick()).thenReturn(ClickType.SHIFT_LEFT);
            when(事件.getCurrentItem()).thenReturn(头盔);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包点击(事件);

            verify(事件).setCancelled(true);
            verify(消息服务).发送消息(快照, 战斗中禁止换装键);
        }

        @Test
        @DisplayName("战斗中Shift点击铁胸甲 - 应取消事件")
        void 战斗中Shift点击铁胸甲_应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 胸甲 = 创建物品(Material.IRON_CHESTPLATE);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.CONTAINER);
            when(事件.getClick()).thenReturn(ClickType.SHIFT_LEFT);
            when(事件.getCurrentItem()).thenReturn(胸甲);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包点击(事件);

            verify(事件).setCancelled(true);
        }
    }

    @Nested
    @DisplayName("战斗中拖拽到盔甲槽 - 应取消")
    class 战斗中拖拽到盔甲槽 {

        @Test
        @DisplayName("战斗中拖拽到5-8槽 - 应取消事件")
        void 战斗中拖拽到盔甲槽_应取消事件() {
            Player 玩家 = 创建玩家();
            Inventory 顶部 = 创建玩家顶部背包();
            InventoryDragEvent 事件 = mock(InventoryDragEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getInventory()).thenReturn(顶部);
            when(事件.getRawSlots()).thenReturn(Set.of(5, 6, 7, 8));
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包拖拽(事件);

            verify(事件).setCancelled(true);
            verify(消息服务).发送消息(快照, 战斗中禁止换装键);
        }

        @Test
        @DisplayName("战斗中拖拽到包含盔甲槽的范围 - 应取消事件")
        void 战斗中拖拽包含盔甲槽_应取消事件() {
            Player 玩家 = 创建玩家();
            Inventory 顶部 = 创建玩家顶部背包();
            InventoryDragEvent 事件 = mock(InventoryDragEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getInventory()).thenReturn(顶部);
            when(事件.getRawSlots()).thenReturn(Set.of(1, 5, 9));
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包拖拽(事件);

            verify(事件).setCancelled(true);
        }
    }

    @Nested
    @DisplayName("非战斗中所有操作 - 不应取消")
    class 非战斗中操作 {

        @Test
        @DisplayName("非战斗中点击盔甲槽 - 不应取消事件")
        void 非战斗中点击盔甲槽_不应取消事件() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.ARMOR);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(false);

            监听器.背包点击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

        @Test
        @DisplayName("非战斗中Shift点击盔甲物品 - 不应取消事件")
        void 非战斗中Shift点击盔甲物品_不应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 头盔 = 创建物品(Material.IRON_HELMET);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.CONTAINER);
            when(事件.getClick()).thenReturn(ClickType.SHIFT_LEFT);
            when(事件.getCurrentItem()).thenReturn(头盔);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(false);

            监听器.背包点击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

        @Test
        @DisplayName("非战斗中拖拽到盔甲槽 - 不应取消事件")
        void 非战斗中拖拽到盔甲槽_不应取消事件() {
            Player 玩家 = 创建玩家();
            Inventory 顶部 = 创建玩家顶部背包();
            InventoryDragEvent 事件 = mock(InventoryDragEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getInventory()).thenReturn(顶部);
            when(事件.getRawSlots()).thenReturn(Set.of(5, 6, 7, 8));
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(false);

            监听器.背包拖拽(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

    }

    @Nested
    @DisplayName("战斗中点击非盔甲槽 - 不应取消")
    class 战斗中点击非盔甲槽 {

        @Test
        @DisplayName("战斗中点击普通容器槽 - 不应取消事件")
        void 战斗中点击普通容器槽_不应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 普通物品 = 创建物品(Material.IRON_SWORD);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.CONTAINER);
            when(事件.getClick()).thenReturn(ClickType.LEFT);
            when(事件.getCurrentItem()).thenReturn(普通物品);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包点击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

        @Test
        @DisplayName("战斗中Shift点击非盔甲物品 - 不应取消事件")
        void 战斗中Shift点击非盔甲物品_不应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 剑 = 创建物品(Material.IRON_SWORD);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getSlotType()).thenReturn(InventoryType.SlotType.CONTAINER);
            when(事件.getClick()).thenReturn(ClickType.SHIFT_LEFT);
            when(事件.getCurrentItem()).thenReturn(剑);
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包点击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

        @Test
        @DisplayName("战斗中拖拽到非盔甲槽 - 不应取消事件")
        void 战斗中拖拽到非盔甲槽_不应取消事件() {
            Player 玩家 = 创建玩家();
            Inventory 顶部 = 创建玩家顶部背包();
            InventoryDragEvent 事件 = mock(InventoryDragEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getInventory()).thenReturn(顶部);
            when(事件.getRawSlots()).thenReturn(Set.of(9, 10, 11));
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包拖拽(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }

        @Test
        @DisplayName("战斗中拖拽到工作台槽位 - 不应取消事件")
        void 战斗中拖拽到工作台槽位_不应取消事件() {
            Player 玩家 = 创建玩家();
            Inventory 顶部 = mock(Inventory.class);
            when(顶部.getType()).thenReturn(InventoryType.CRAFTING);
            when(顶部.getSize()).thenReturn(10);
            InventoryDragEvent 事件 = mock(InventoryDragEvent.class);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getInventory()).thenReturn(顶部);
            when(事件.getRawSlots()).thenReturn(Set.of(5, 6, 7, 8));
            when(战斗状态服务.是否战斗中(玩家标识)).thenReturn(true);

            监听器.背包拖拽(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(消息服务, never()).发送消息(any(玩家快照.class), anyString(), any());
        }
    }
}
