package 暮澜纪元.菜单;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import 暮澜纪元.职业.世界;
import 暮澜纪元.职业.职业;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("职业选择GUI事件处理器")
@SuppressWarnings("null")
class 职业选择GUI事件处理器测试 {

    private 职业选择GUI mockGui;
    private 职业选择GUI事件处理器 处理器;

    @BeforeEach
    void 准备() {
        mockGui = mock(职业选择GUI.class);
        处理器 = new 职业选择GUI事件处理器(mockGui);
    }

    @Nested
    @DisplayName("点击事件")
    class 点击事件 {

        @Test
        @DisplayName("被点击背包为null时不应取消事件")
        void 被点击背包为null_不应取消事件() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            when(mock事件.getClickedInventory()).thenReturn(null);

            处理器.onInventoryClick(mock事件);

            verify(mock事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("持有者非菜单持有者时不应取消事件")
        void 持有者非菜单持有者_不应取消事件() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            InventoryHolder mock持有者 = mock(InventoryHolder.class);
            when(mock事件.getClickedInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(mock持有者);

            处理器.onInventoryClick(mock事件);

            verify(mock事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("菜单持有者持有时应取消事件")
        void 菜单持有者持有_应取消事件() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            菜单持有者 持有者 = new 菜单持有者();
            Player mock玩家 = mock(Player.class);
            ItemStack mock物品 = mock(ItemStack.class);
            Material mockMaterial = mock(Material.class);
            when(mock事件.getClickedInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(持有者);
            when(mock事件.getWhoClicked()).thenReturn(mock玩家);
            when(mock事件.getCurrentItem()).thenReturn(mock物品);
            when(mock物品.getType()).thenReturn(mockMaterial);
            when(mockMaterial.isAir()).thenReturn(false);

            处理器.onInventoryClick(mock事件);

            verify(mock事件).setCancelled(true);
        }

        @Test
        @DisplayName("非玩家点击者应取消事件但不调用GUI方法")
        void 非玩家点击者_应取消但不调用GUI() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            菜单持有者 持有者 = new 菜单持有者();
            HumanEntity mockHuman = mock(HumanEntity.class);
            when(mock事件.getClickedInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(持有者);
            when(mock事件.getWhoClicked()).thenReturn(mockHuman);

            处理器.onInventoryClick(mock事件);

            verify(mock事件).setCancelled(true);
            verify(mockGui, never()).选择世界(any(), any());
            verify(mockGui, never()).选择专精(any(), any());
        }

        @Test
        @DisplayName("点击null物品应取消事件但不调用GUI方法")
        void 点击null物品_应取消但不调用GUI() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            菜单持有者 持有者 = new 菜单持有者();
            Player mock玩家 = mock(Player.class);
            when(mock事件.getClickedInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(持有者);
            when(mock事件.getWhoClicked()).thenReturn(mock玩家);
            when(mock事件.getCurrentItem()).thenReturn(null);

            处理器.onInventoryClick(mock事件);

            verify(mock事件).setCancelled(true);
            verify(mockGui, never()).选择世界(any(), any());
            verify(mockGui, never()).选择专精(any(), any());
        }

        @Test
        @DisplayName("点击空气物品应取消事件但不调用GUI方法")
        void 点击空气物品_应取消但不调用GUI() {
            InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            菜单持有者 持有者 = new 菜单持有者();
            Player mock玩家 = mock(Player.class);
            ItemStack mock物品 = mock(ItemStack.class);
            Material mockMaterial = mock(Material.class);
            when(mock事件.getClickedInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(持有者);
            when(mock事件.getWhoClicked()).thenReturn(mock玩家);
            when(mock事件.getCurrentItem()).thenReturn(mock物品);
            when(mock物品.getType()).thenReturn(mockMaterial);
            when(mockMaterial.isAir()).thenReturn(true);

            处理器.onInventoryClick(mock事件);

            verify(mock事件).setCancelled(true);
            verify(mockGui, never()).选择世界(any(), any());
            verify(mockGui, never()).选择专精(any(), any());
        }

        @Nested
        @DisplayName("世界选择菜单")
        class 世界选择菜单 {

            @Test
            @DisplayName("点击有效槽位应调用选择世界")
            void 有效槽位_应调用选择世界() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者();
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(0);

                处理器.onInventoryClick(mock事件);

                verify(mockGui).选择世界(eq(mock玩家), eq(世界.魔法世界));
            }

            @Test
            @DisplayName("点击第二个世界槽位应选择自然世界")
            void 第二个槽位_应选择自然世界() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者();
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(1);

                处理器.onInventoryClick(mock事件);

                verify(mockGui).选择世界(eq(mock玩家), eq(世界.自然世界));
            }

            @Test
            @DisplayName("点击越界槽位不应调用选择世界")
            void 越界槽位_不应调用选择世界() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者();
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(999);

                处理器.onInventoryClick(mock事件);

                verify(mockGui, never()).选择世界(any(), any());
            }

            @Test
            @DisplayName("点击负数槽位不应调用选择世界")
            void 负数槽位_不应调用选择世界() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者();
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(-1);

                处理器.onInventoryClick(mock事件);

                verify(mockGui, never()).选择世界(any(), any());
            }
        }

        @Nested
        @DisplayName("专精选择菜单")
        class 专精选择菜单 {

            @Test
            @DisplayName("点击有效槽位应调用选择专精")
            void 有效槽位_应调用选择专精() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者(0);
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(0);

                处理器.onInventoryClick(mock事件);

                verify(mockGui).选择专精(eq(mock玩家), eq(职业.奥能法师));
            }

            @Test
            @DisplayName("点击第二个槽位应选择战斗法师")
            void 第二个槽位_应选择战斗法师() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者(0);
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(1);

                处理器.onInventoryClick(mock事件);

                verify(mockGui).选择专精(eq(mock玩家), eq(职业.战斗法师));
            }

            @Test
            @DisplayName("点击越界槽位不应调用选择专精")
            void 越界槽位_不应调用选择专精() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者(0);
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(999);

                处理器.onInventoryClick(mock事件);

                verify(mockGui, never()).选择专精(any(), any());
            }

            @Test
            @DisplayName("无效世界索引不应调用选择专精")
            void 无效世界索引_不应调用选择专精() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者(999);
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(0);

                处理器.onInventoryClick(mock事件);

                verify(mockGui, never()).选择专精(any(), any());
            }

            @Test
            @DisplayName("负数世界索引不应调用选择专精")
            void 负数世界索引_不应调用选择专精() {
                InventoryClickEvent mock事件 = mock(InventoryClickEvent.class);
                Inventory mock背包 = mock(Inventory.class);
                菜单持有者 持有者 = new 菜单持有者(-1);
                Player mock玩家 = mock(Player.class);
                ItemStack mock物品 = mock(ItemStack.class);
                Material mockMaterial = mock(Material.class);
                when(mock事件.getClickedInventory()).thenReturn(mock背包);
                when(mock背包.getHolder()).thenReturn(持有者);
                when(mock事件.getWhoClicked()).thenReturn(mock玩家);
                when(mock事件.getCurrentItem()).thenReturn(mock物品);
                when(mock物品.getType()).thenReturn(mockMaterial);
                when(mockMaterial.isAir()).thenReturn(false);
                when(mock事件.getSlot()).thenReturn(0);

                处理器.onInventoryClick(mock事件);

                verify(mockGui, never()).选择专精(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("拖拽事件")
    class 拖拽事件 {

        @Test
        @DisplayName("菜单持有者持有时应取消拖拽事件")
        void 菜单持有者持有_应取消拖拽() {
            InventoryDragEvent mock事件 = mock(InventoryDragEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            菜单持有者 持有者 = new 菜单持有者();
            when(mock事件.getInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(持有者);

            处理器.onInventoryDrag(mock事件);

            verify(mock事件).setCancelled(true);
        }

        @Test
        @DisplayName("非菜单持有者持有时不应取消拖拽事件")
        void 非菜单持有者持有_不应取消拖拽() {
            InventoryDragEvent mock事件 = mock(InventoryDragEvent.class);
            Inventory mock背包 = mock(Inventory.class);
            InventoryHolder mock持有者 = mock(InventoryHolder.class);
            when(mock事件.getInventory()).thenReturn(mock背包);
            when(mock背包.getHolder()).thenReturn(mock持有者);

            处理器.onInventoryDrag(mock事件);

            verify(mock事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("背包为null时不应取消拖拽事件")
        void 背包为null_不应取消拖拽() {
            InventoryDragEvent mock事件 = mock(InventoryDragEvent.class);
            when(mock事件.getInventory()).thenReturn(null);

            处理器.onInventoryDrag(mock事件);

            verify(mock事件, never()).setCancelled(anyBoolean());
        }
    }
}
