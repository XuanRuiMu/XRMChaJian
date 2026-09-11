package mljy.表现层.监听器;

import mljy.业务层.乐器服务;
import mljy.业务层.乐器服务.乐谱保存结果;
import mljy.业务层.乐器服务实现;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.功能位定义;
import mljy.领域层.乐器.延音模式;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("乐器监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 乐器监听器测试 {

    // FP-F: 监听器内部需要 instanceof 乐器服务实现 检查，因此 mock 乐器服务实现 而非接口
    @Mock
    private 乐器服务实现 乐器服务;

    private 乐器监听器 监听器;

    @BeforeEach
    void setUp() {
        乐器注册表 注册表 = new 乐器注册表();
        FileConfiguration 配置 = 加载资源配置("乐器配置.yml");
        注册表.从配置加载(配置);
        JavaPlugin 插件mock = mock(JavaPlugin.class);
        监听器 = new 乐器监听器(乐器服务, 注册表, 插件mock);
        // FP-F: stub 默认布局查询方法，避免 mock 默认 int 值 0 与功能位槽位冲突
        stub默认布局();
    }

    /**
     * FP-F: stub 默认钢琴布局查询方法，与 乐器配置.yml 默认值一致。
     * 避免 mock 默认 int 值 0 导致槽位 0（保存功能位）被误识别为"音域减"按钮（槽位 9）。
     */
    private void stub默认布局() {
        Map<Integer, 功能位定义> 功能位表 = Map.of(
                0, 功能位定义.保存,
                1, 功能位定义.清除,
                2, 功能位定义.关闭,
                3, 功能位定义.NBS导入,
                4, 功能位定义.NBS导出,
                5, 功能位定义.和弦预设,
                6, 功能位定义.公共乐谱库,
                7, 功能位定义.钢琴卷帘编辑,
                8, 功能位定义.分层切换);
        when(乐器服务.获取功能位槽位表()).thenReturn(功能位表);
        when(乐器服务.获取槽位音域减()).thenReturn(9);
        when(乐器服务.获取槽位音域显示()).thenReturn(10);
        when(乐器服务.获取槽位音域加()).thenReturn(11);
        when(乐器服务.获取槽位力度减()).thenReturn(12);
        when(乐器服务.获取槽位力度显示()).thenReturn(13);
        when(乐器服务.获取槽位力度加()).thenReturn(14);
        when(乐器服务.获取槽位录制()).thenReturn(15);
        when(乐器服务.获取槽位停止()).thenReturn(16);
        when(乐器服务.获取槽位播放()).thenReturn(17);
        when(乐器服务.获取槽位延音模式切换()).thenReturn(18);
        when(乐器服务.获取黑键槽位数组()).thenReturn(new int[]{37, 39, 41, 42, 43});
        when(乐器服务.获取白键槽位数组()).thenReturn(new int[]{45, 46, 47, 48, 49, 50, 51});
        when(乐器服务.获取黑键音高偏移数组()).thenReturn(new int[]{1, 3, 6, 8, 10});
        when(乐器服务.获取白键音高偏移数组()).thenReturn(new int[]{0, 2, 4, 5, 7, 9, 11});
    }

    private static FileConfiguration 加载资源配置(String 资源名) {
        InputStream 输入流 = 乐器监听器测试.class.getClassLoader().getResourceAsStream(资源名);
        Objects.requireNonNull(输入流, "资源文件未找到: " + 资源名);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(输入流, StandardCharsets.UTF_8));
    }

    private Player 创建玩家() {
        Player 玩家 = mock(Player.class);
        PlayerInventory 背包 = mock(PlayerInventory.class);
        when(玩家.getInventory()).thenReturn(背包);
        return 玩家;
    }

    private ItemStack 创建乐器物品(int 自定义模型数据) {
        ItemStack 物品 = mock(ItemStack.class);
        ItemMeta 元数据 = mock(ItemMeta.class);
        CustomModelDataComponent 组件 = mock(CustomModelDataComponent.class);
        when(物品.getType()).thenReturn(Material.STICK);
        when(物品.getItemMeta()).thenReturn(元数据);
        when(元数据.hasCustomModelDataComponent()).thenReturn(true);
        when(元数据.getCustomModelDataComponent()).thenReturn(组件);
        when(组件.getFloats()).thenReturn(List.of((float) 自定义模型数据));
        return 物品;
    }

    private PlayerInteractEvent 创建交互事件(Player 玩家, Action 动作, EquipmentSlot 手) {
        PlayerInteractEvent 事件 = mock(PlayerInteractEvent.class);
        when(事件.getPlayer()).thenReturn(玩家);
        when(事件.getAction()).thenReturn(动作);
        when(事件.getHand()).thenReturn(手);
        return 事件;
    }

    @Test
    @DisplayName("右键空气持有乐器 - 应打开演奏界面并取消事件")
    void 右键空气持有乐器_应打开演奏界面并取消事件() {
        Player 玩家 = 创建玩家();
        ItemStack 乐器 = 创建乐器物品(9001);
        when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务).打开演奏界面(eq(玩家), any());
        verify(事件).setCancelled(true);
    }

    @Test
    @DisplayName("右键方块持有乐器 - 应打开演奏界面并取消事件")
    void 右键方块持有乐器_应打开演奏界面并取消事件() {
        Player 玩家 = 创建玩家();
        ItemStack 乐器 = 创建乐器物品(9001);
        when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务).打开演奏界面(eq(玩家), any());
        verify(事件).setCancelled(true);
    }

    @Test
    @DisplayName("左键空气持有乐器 - 不应打开演奏界面且不取消事件")
    void 左键空气持有乐器_不应打开演奏界面且不取消事件() {
        Player 玩家 = 创建玩家();
        ItemStack 乐器 = 创建乐器物品(9001);
        when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务, never()).打开演奏界面(any(), any());
        verify(事件, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("左键方块持有乐器 - 不应打开演奏界面且不取消事件")
    void 左键方块持有乐器_不应打开演奏界面且不取消事件() {
        Player 玩家 = 创建玩家();
        ItemStack 乐器 = 创建乐器物品(9001);
        when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_BLOCK, EquipmentSlot.HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务, never()).打开演奏界面(any(), any());
        verify(事件, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("右键持有非乐器物品 - 不应打开演奏界面")
    void 右键持有非乐器物品_不应打开演奏界面() {
        Player 玩家 = 创建玩家();
        ItemStack 普通木棍 = mock(ItemStack.class);
        ItemMeta 元数据 = mock(ItemMeta.class);
        CustomModelDataComponent 组件 = mock(CustomModelDataComponent.class);
        when(普通木棍.getType()).thenReturn(Material.STICK);
        when(普通木棍.getItemMeta()).thenReturn(元数据);
        when(元数据.hasCustomModelDataComponent()).thenReturn(true);
        when(元数据.getCustomModelDataComponent()).thenReturn(组件);
        when(组件.getFloats()).thenReturn(List.of(1.0f));
        when(玩家.getInventory().getItemInMainHand()).thenReturn(普通木棍);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务, never()).打开演奏界面(any(), any());
        verify(事件, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("副手右键持有乐器 - 不应打开演奏界面")
    void 副手右键持有乐器_不应打开演奏界面() {
        Player 玩家 = 创建玩家();
        ItemStack 乐器 = 创建乐器物品(9001);
        when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器);
        PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND);

        监听器.玩家右键物品(事件);

        verify(乐器服务, never()).打开演奏界面(any(), any());
        verify(事件, never()).setCancelled(anyBoolean());
    }

    @Nested
    @DisplayName("GAP-01: 保存按钮两次点击覆盖流程")
    class GAP01保存按钮两次点击测试 {

        private 乐器定义 创建测试乐器() {
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 1, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private InventoryClickEvent 创建保存点击事件(Player 玩家) {
            乐器服务实现.演奏界面持有者 持有者 = new 乐器服务实现.演奏界面持有者(创建测试乐器());
            Inventory 界面 = mock(Inventory.class);
            when(界面.getHolder()).thenReturn(持有者);
            when(界面.getSize()).thenReturn(54);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getInventory()).thenReturn(界面);
            when(事件.getWhoClicked()).thenReturn(玩家);
            // FP-F: 保存功能位从槽位 51 改为槽位 0（9 功能位槽 0-8）
            when(事件.getRawSlot()).thenReturn(0);
            return 事件;
        }

        @Test
        @DisplayName("首次保存返回重名 → 二次点击相同乐谱名应调用 保存乐谱并覆盖")
        void 首次重名_二次相同名应调用覆盖() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取乐谱列表(玩家标识)).thenReturn(List.of("已有乐谱"));
            when(乐器服务.保存乐谱带结果(玩家标识, "已有乐谱")).thenReturn(乐谱保存结果.重名);

            InventoryClickEvent 事件 = 创建保存点击事件(玩家);

            监听器.界面点击(事件);
            verify(乐器服务).保存乐谱带结果(玩家标识, "已有乐谱");
            verify(乐器服务, never()).保存乐谱并覆盖(any(), any());

            监听器.界面点击(事件);
            verify(乐器服务).保存乐谱并覆盖(玩家标识, "已有乐谱");
        }

        @Test
        @DisplayName("首次保存返回成功 → 二次点击不应调用 保存乐谱并覆盖")
        void 首次成功_二次不应调用覆盖() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取乐谱列表(玩家标识)).thenReturn(List.of("已有乐谱"));
            when(乐器服务.保存乐谱带结果(玩家标识, "已有乐谱")).thenReturn(乐谱保存结果.成功);

            InventoryClickEvent 事件 = 创建保存点击事件(玩家);

            监听器.界面点击(事件);
            监听器.界面点击(事件);
            verify(乐器服务, times(2)).保存乐谱带结果(玩家标识, "已有乐谱");
            verify(乐器服务, never()).保存乐谱并覆盖(any(), any());
        }

        @Test
        @DisplayName("首次保存返回重名 → 二次点击乐谱名变化应再次调用 保存乐谱带结果")
        void 首次重名_二次乐谱名变化不应覆盖() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取乐谱列表(玩家标识))
                    .thenReturn(List.of("乐谱A"))
                    .thenReturn(List.of("乐谱A", "乐谱B"));
            when(乐器服务.保存乐谱带结果(any(), anyString())).thenReturn(乐谱保存结果.重名);

            InventoryClickEvent 事件 = 创建保存点击事件(玩家);

            监听器.界面点击(事件);
            监听器.界面点击(事件);

            verify(乐器服务).保存乐谱带结果(玩家标识, "乐谱A");
            verify(乐器服务).保存乐谱带结果(玩家标识, "乐谱B");
            verify(乐器服务, never()).保存乐谱并覆盖(any(), any());
        }
    }

    // ========== FP-E 测试：GUI 点击归一 + 聊天命令模式 ==========

    @Nested
    @DisplayName("FP-01: GUI 点击应调用统一 演奏指令（三轨归一）")
    class FPE_GUI归一测试 {

        private 乐器定义 创建测试乐器() {
            // 默认音域=0：白键槽位 45 → 音高 0*12+0 = 0；白键槽位 48 → 音高 0*12+5 = 5
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 0, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private InventoryClickEvent 创建音符点击事件(Player 玩家, int 槽位) {
            乐器服务实现.演奏界面持有者 持有者 = new 乐器服务实现.演奏界面持有者(创建测试乐器());
            Inventory 界面 = mock(Inventory.class);
            when(界面.getHolder()).thenReturn(持有者);
            when(界面.getSize()).thenReturn(54);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getInventory()).thenReturn(界面);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getRawSlot()).thenReturn(槽位);
            return 事件;
        }

        @Test
        @DisplayName("点击白键应调用 开始延音（FP-04 延音系统接入，按住模式默认）")
        void 点击白键_应调用开始延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(mljy.领域层.乐器.延音模式.按住);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(false);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(false);
            // FP-F: stub 解析槽位音高，白键槽位 45 → 音高 0（八度 0 + 偏移 0）
            when(乐器服务.解析槽位音高(45, 0)).thenReturn(0);

            // FP-F: 白键槽位 45 → 音高 0（八度 0 + 偏移 0）
            InventoryClickEvent 事件 = 创建音符点击事件(玩家, 45);
            监听器.界面点击(事件);

            // FP-F: 应调用 开始延音（内部调用 演奏指令 完成初始触发）
            verify(乐器服务).开始延音(eq(玩家), any(乐器定义.class), eq(0), eq(力度档.MF));
            // 不应再调用旧 演奏音符 或 演奏指令（由 开始延音 内部转发，监听器层不直接调用）
            verify(乐器服务, never()).演奏指令(any(), any(), anyInt(), any(), anyInt());
            verify(乐器服务, never()).演奏音符(any(), any(), anyInt());
            verify(乐器服务, never()).演奏音符(any(), any(), anyInt(), anyFloat());
        }

        @Test
        @DisplayName("点击白键时录制中应使用力度档音量调用 记录按键按下")
        void 点击白键_录制中应使用力度档音量() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.PP);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(mljy.领域层.乐器.延音模式.按住);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(false);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(true);
            // FP-F: stub 解析槽位音高，白键槽位 48 → 音高 5（八度 0 + 偏移 5）
            when(乐器服务.解析槽位音高(48, 0)).thenReturn(5);

            // FP-F: 白键槽位 48 → 音高 5（八度 0 + 偏移 5）
            InventoryClickEvent 事件 = 创建音符点击事件(玩家, 48);
            监听器.界面点击(事件);

            // 应使用力度档 PP 的音量 0.30 调用 记录按键按下
            verify(乐器服务).记录按键按下(玩家标识, 5, 力度档.PP.获取音量());
            verify(乐器服务).记录按键松开(玩家标识, 5);
        }
    }

    @Nested
    @DisplayName("FP-01: 聊天命令模式（Apple Musical Typing）")
    @SuppressWarnings({"deprecation", "removal"})
    class FPE_聊天命令模式测试 {

        private 乐器定义 创建测试乐器() {
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 1, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private AsyncPlayerChatEvent 创建聊天事件(Player 玩家, String 消息) {
            AsyncPlayerChatEvent 事件 = mock(AsyncPlayerChatEvent.class);
            when(事件.getPlayer()).thenReturn(玩家);
            when(事件.getMessage()).thenReturn(消息);
            return 事件;
        }

        @Test
        @DisplayName("手持乐器物品发送单字母消息应调用 处理聊天按键 并取消事件")
        void 手持乐器_单字母消息_应调用处理聊天按键() {
            Player 玩家 = 创建玩家();
            ItemStack 乐器物品 = 创建乐器物品(9001);
            when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器物品);
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.处理聊天按键(玩家, "a")).thenReturn(true);

            AsyncPlayerChatEvent 事件 = 创建聊天事件(玩家, "a");
            // FP-E 线程安全：主线程分支直接执行 处理聊天按键
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::isPrimaryThread).thenReturn(true);
                监听器.玩家聊天(事件);
            }

            // matcher 与字面值不能混用，玩家标识需用 eq() 包裹
            verify(乐器服务).设置玩家当前乐器(eq(玩家标识), any());
            verify(乐器服务).处理聊天按键(玩家, "a");
            verify(事件).setCancelled(true);
        }

        @Test
        @DisplayName("手持乐器物品发送多字符消息不应触发处理")
        void 多字符消息_不应触发处理() {
            Player 玩家 = 创建玩家();
            ItemStack 乐器物品 = 创建乐器物品(9001);
            when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器物品);

            AsyncPlayerChatEvent 事件 = 创建聊天事件(玩家, "hello");
            监听器.玩家聊天(事件);

            verify(乐器服务, never()).处理聊天按键(any(), any());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("未手持乐器物品发送单字母消息不应触发处理")
        void 无乐器物品_单字母消息_不应触发() {
            Player 玩家 = 创建玩家();
            ItemStack 空手 = mock(ItemStack.class);
            when(空手.getType()).thenReturn(Material.AIR);
            when(玩家.getInventory().getItemInMainHand()).thenReturn(空手);

            AsyncPlayerChatEvent 事件 = 创建聊天事件(玩家, "a");
            监听器.玩家聊天(事件);

            verify(乐器服务, never()).处理聊天按键(any(), any());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("处理聊天按键返回 false（非音乐键）不应取消事件")
        void 非音乐键_不应取消事件() {
            Player 玩家 = 创建玩家();
            ItemStack 乐器物品 = 创建乐器物品(9001);
            when(玩家.getInventory().getItemInMainHand()).thenReturn(乐器物品);
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.处理聊天按键(玩家, "q")).thenReturn(false);

            AsyncPlayerChatEvent 事件 = 创建聊天事件(玩家, "q");
            // FP-E 线程安全：主线程分支直接执行 处理聊天按键
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::isPrimaryThread).thenReturn(true);
                监听器.玩家聊天(事件);
            }

            verify(乐器服务).处理聊天按键(玩家, "q");
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("null 消息应不抛异常且不触发处理")
        void null消息_应不抛异常() {
            Player 玩家 = 创建玩家();
            AsyncPlayerChatEvent 事件 = mock(AsyncPlayerChatEvent.class);
            when(事件.getPlayer()).thenReturn(玩家);
            when(事件.getMessage()).thenReturn(null);

            assertDoesNotThrow(() -> 监听器.玩家聊天(事件));
            verify(乐器服务, never()).处理聊天按键(any(), any());
        }
    }

    // ========== FP-F 测试：9 功能位点击分发 + 延音模式切换按钮 + 切换模式琴键点击 ==========

    @Nested
    @DisplayName("FP-03: 9 功能位点击分发")
    class FP03功能位点击测试 {

        private 乐器定义 创建测试乐器() {
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 1, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private InventoryClickEvent 创建功能位点击事件(Player 玩家, int 槽位) {
            乐器服务实现.演奏界面持有者 持有者 = new 乐器服务实现.演奏界面持有者(创建测试乐器());
            Inventory 界面 = mock(Inventory.class);
            when(界面.getHolder()).thenReturn(持有者);
            when(界面.getSize()).thenReturn(54);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getInventory()).thenReturn(界面);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getRawSlot()).thenReturn(槽位);
            return 事件;
        }

        @Test
        @DisplayName("槽位 0（保存）应调用 保存乐谱带结果")
        void 保存功能位_应调用保存乐谱带结果() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取乐谱列表(玩家标识)).thenReturn(List.of("测试乐谱"));

            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 0);
            监听器.界面点击(事件);

            verify(乐器服务).保存乐谱带结果(玩家标识, "测试乐谱");
            verify(乐器服务, never()).保存乐谱并覆盖(any(), any());
        }

        @Test
        @DisplayName("槽位 1（清除）应调用 停止录制 和 停止播放")
        void 清除功能位_应调用停止录制和停止播放() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);

            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 1);
            监听器.界面点击(事件);

            verify(乐器服务).停止录制(玩家标识);
            verify(乐器服务).停止播放(玩家标识);
        }

        @Test
        @DisplayName("槽位 2（关闭）应调用 玩家.closeInventory()")
        void 关闭功能位_应调用玩家closeInventory() {
            Player 玩家 = 创建玩家();

            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 2);
            监听器.界面点击(事件);

            verify(玩家).closeInventory();
        }

        @Test
        @DisplayName("槽位 3（NBS导入）应调用 发送功能未实现提示")
        void NBS导入功能位_应调用发送功能未实现提示() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 3);
            监听器.界面点击(事件);
            verify(乐器服务).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("槽位 4（NBS导出）应调用 发送功能未实现提示")
        void NBS导出功能位_应调用发送功能未实现提示() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 4);
            监听器.界面点击(事件);
            verify(乐器服务).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("槽位 5（和弦预设）应调用 发送功能未实现提示")
        void 和弦预设功能位_应调用发送功能未实现提示() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 5);
            监听器.界面点击(事件);
            verify(乐器服务).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("槽位 6（公共乐谱库）应调用 发送功能未实现提示")
        void 公共乐谱库功能位_应调用发送功能未实现提示() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 6);
            监听器.界面点击(事件);
            verify(乐器服务).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("槽位 7（钢琴卷帘编辑）应调用 发送功能未实现提示")
        void 钢琴卷帘编辑功能位_应调用发送功能未实现提示() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 7);
            监听器.界面点击(事件);
            verify(乐器服务).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("槽位 8（分层切换）应调用 切换分层模式（FP-19 已接入）")
        void 分层切换功能位_应调用切换分层模式() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 8);
            监听器.界面点击(事件);
            verify(乐器服务).切换分层模式(玩家.getUniqueId());
            verify(乐器服务, never()).发送功能未实现提示(玩家);
        }

        @Test
        @DisplayName("功能位点击应取消事件（setCancelled(true)）")
        void 功能位点击_应取消事件() {
            Player 玩家 = 创建玩家();
            InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 2);
            监听器.界面点击(事件);
            verify(事件).setCancelled(true);
        }

        @Test
        @DisplayName("后 6 个功能位不应调用 开始延音 或 演奏指令（仅提示未实现/分层切换）")
        void 后六功能位_不应触发演奏() {
            Player 玩家 = 创建玩家();
            for (int 槽位 = 3; 槽位 <= 8; 槽位++) {
                InventoryClickEvent 事件 = 创建功能位点击事件(玩家, 槽位);
                监听器.界面点击(事件);
            }
            verify(乐器服务, never()).开始延音(any(), any(), anyInt(), any());
            verify(乐器服务, never()).演奏指令(any(), any(), anyInt(), any(), anyInt());
            // 槽位 3-7 共 5 个功能位提示未实现；槽位 8（分层切换）已接入 FP-19 实际功能
            verify(乐器服务, times(5)).发送功能未实现提示(玩家);
            verify(乐器服务).切换分层模式(玩家.getUniqueId());
        }
    }

    @Nested
    @DisplayName("FP-04: 延音模式切换按钮（槽位 18）")
    class FP04延音模式切换按钮测试 {

        private 乐器定义 创建测试乐器() {
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 1, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private InventoryClickEvent 创建延音切换点击事件(Player 玩家) {
            乐器服务实现.演奏界面持有者 持有者 = new 乐器服务实现.演奏界面持有者(创建测试乐器());
            Inventory 界面 = mock(Inventory.class);
            when(界面.getHolder()).thenReturn(持有者);
            when(界面.getSize()).thenReturn(54);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getInventory()).thenReturn(界面);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getRawSlot()).thenReturn(18);
            return 事件;
        }

        @Test
        @DisplayName("点击延音模式切换按钮应调用 切换延音模式")
        void 点击延音模式切换按钮_应调用切换延音模式() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);

            InventoryClickEvent 事件 = 创建延音切换点击事件(玩家);
            监听器.界面点击(事件);

            verify(乐器服务).切换延音模式(玩家标识);
        }

        @Test
        @DisplayName("点击延音模式切换按钮应调用 重新填充界面")
        void 点击延音模式切换按钮_应调用重新填充界面() {
            Player 玩家 = 创建玩家();

            InventoryClickEvent 事件 = 创建延音切换点击事件(玩家);
            监听器.界面点击(事件);

            verify(乐器服务).重新填充界面(any(Inventory.class), any(乐器定义.class), anyInt());
        }

        @Test
        @DisplayName("点击延音模式切换按钮应取消事件")
        void 点击延音模式切换按钮_应取消事件() {
            Player 玩家 = 创建玩家();

            InventoryClickEvent 事件 = 创建延音切换点击事件(玩家);
            监听器.界面点击(事件);

            verify(事件).setCancelled(true);
        }

        @Test
        @DisplayName("点击延音模式切换按钮不应触发 开始延音 或 停止延音")
        void 点击延音模式切换按钮_不应触发延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);

            InventoryClickEvent 事件 = 创建延音切换点击事件(玩家);
            监听器.界面点击(事件);

            verify(乐器服务, never()).开始延音(any(), any(), anyInt(), any());
            verify(乐器服务, never()).停止延音(any());
        }
    }

    @Nested
    @DisplayName("FP-04: 切换模式琴键点击（再点击同音符停止延音）")
    class FP04切换模式琴键点击测试 {

        private 乐器定义 创建测试乐器() {
            // 默认音域=0：白键槽位 45 → 音高 0；白键槽位 46 → 音高 2
            return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 9001, 0, 0, 24, 0,
                    false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
        }

        private InventoryClickEvent 创建琴键点击事件(Player 玩家, int 槽位) {
            乐器服务实现.演奏界面持有者 持有者 = new 乐器服务实现.演奏界面持有者(创建测试乐器());
            Inventory 界面 = mock(Inventory.class);
            when(界面.getHolder()).thenReturn(持有者);
            when(界面.getSize()).thenReturn(54);
            InventoryClickEvent 事件 = mock(InventoryClickEvent.class);
            when(事件.getInventory()).thenReturn(界面);
            when(事件.getWhoClicked()).thenReturn(玩家);
            when(事件.getRawSlot()).thenReturn(槽位);
            return 事件;
        }

        @Test
        @DisplayName("切换模式_延音中_同音再点击应调用 停止延音")
        void 切换模式_延音中_同音再点击_应调用停止延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.切换);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(true);
            when(乐器服务.获取延音音高(玩家标识)).thenReturn(0);
            when(乐器服务.解析槽位音高(45, 0)).thenReturn(0);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 45);
            监听器.界面点击(事件);

            verify(乐器服务).停止延音(玩家标识);
        }

        @Test
        @DisplayName("切换模式_延音中_同音再点击不应调用 开始延音")
        void 切换模式_延音中_同音再点击_不应调用开始延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.切换);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(true);
            when(乐器服务.获取延音音高(玩家标识)).thenReturn(0);
            when(乐器服务.解析槽位音高(45, 0)).thenReturn(0);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 45);
            监听器.界面点击(事件);

            verify(乐器服务, never()).开始延音(any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("切换模式_延音中_不同音点击应调用 开始延音（替换旧延音）")
        void 切换模式_延音中_不同音点击_应调用开始延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.切换);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(true);
            when(乐器服务.获取延音音高(玩家标识)).thenReturn(0);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(false);
            when(乐器服务.解析槽位音高(46, 0)).thenReturn(2);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 46);
            监听器.界面点击(事件);

            verify(乐器服务).开始延音(eq(玩家), any(乐器定义.class), eq(2), eq(力度档.MF));
        }

        @Test
        @DisplayName("切换模式_延音中_不同音点击不应调用 停止延音")
        void 切换模式_延音中_不同音点击_不应调用停止延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.切换);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(true);
            when(乐器服务.获取延音音高(玩家标识)).thenReturn(0);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(false);
            when(乐器服务.解析槽位音高(46, 0)).thenReturn(2);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 46);
            监听器.界面点击(事件);

            verify(乐器服务, never()).停止延音(玩家标识);
        }

        @Test
        @DisplayName("切换模式_未延音_应调用 开始延音")
        void 切换模式_未延音_应调用开始延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.切换);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(false);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(false);
            when(乐器服务.解析槽位音高(45, 0)).thenReturn(0);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 45);
            监听器.界面点击(事件);

            verify(乐器服务).开始延音(eq(玩家), any(乐器定义.class), eq(0), eq(力度档.MF));
            verify(乐器服务, never()).停止延音(玩家标识);
        }

        @Test
        @DisplayName("按住模式_延音中_同音再点击应调用 开始延音（按住模式不停止，持续覆盖）")
        void 按住模式_延音中_同音再点击_应调用开始延音() {
            Player 玩家 = 创建玩家();
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家.getUniqueId()).thenReturn(玩家标识);
            when(乐器服务.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);
            when(乐器服务.获取延音模式(玩家标识)).thenReturn(延音模式.按住);
            when(乐器服务.是否延音中(玩家标识)).thenReturn(true);
            when(乐器服务.获取延音音高(玩家标识)).thenReturn(0);
            when(乐器服务.是否录制中(玩家标识)).thenReturn(false);
            when(乐器服务.解析槽位音高(45, 0)).thenReturn(0);

            InventoryClickEvent 事件 = 创建琴键点击事件(玩家, 45);
            监听器.界面点击(事件);

            verify(乐器服务).开始延音(eq(玩家), any(乐器定义.class), eq(0), eq(力度档.MF));
            verify(乐器服务, never()).停止延音(玩家标识);
        }
    }
}
