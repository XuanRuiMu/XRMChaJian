package mljy.表现层.监听器;

import mljy.业务层.技能释放服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.驭空术服务;
import mljy.玩家服务;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.驭空术.飞行状态;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.技能按键类型;
import mljy.领域层.技能.施法类型;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.属性快照;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("技能按键绑定监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能按键绑定监听器测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 技能释放服务 技能释放服务;
    @Mock
    private 战斗状态服务 战斗状态服务;
    @Mock
    private 技能注册服务 技能注册服务;
    @Mock
    private JavaPlugin 插件;
    @Mock
    private FileConfiguration 配置;
    @Mock
    private 驭空术服务 驭空术服务;
    @Mock
    private Server 服务器;

    private 技能按键绑定监听器 监听器;

    private UUID 玩家标识;
    private 玩家会话 会话;
    private 玩家快照 快照;
    private 技能定义 技能定义;

    @BeforeEach
    void setUp() {
        when(插件.getConfig()).thenReturn(配置);
        when(插件.getServer()).thenReturn(服务器);
        org.bukkit.configuration.ConfigurationSection 区段 = mock(org.bukkit.configuration.ConfigurationSection.class);
        when(配置.getConfigurationSection("技能.专精编号映射")).thenReturn(区段);
        when(区段.getKeys(false)).thenReturn(java.util.Set.of("奥能法师"));
        when(区段.getInt("奥能法师", 0)).thenReturn(1);

        监听器 = new 技能按键绑定监听器(玩家服务, 技能释放服务, 战斗状态服务, 技能注册服务, 驭空术服务, 插件);

        玩家标识 = UUID.randomUUID();
        会话 = new 玩家会话(玩家标识, "测试玩家");
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null,
                属性快照.创建(100, 1.5, 10, 50, 40, 30, 50, 40, 30, 20, 100, 15, 5, 8, 12, 2, 1),
                Map.of(), null);
        技能定义 = new 技能定义("1_1", "skill.奥能法师.slot.一.name",
                施法类型.瞬发, 0, 0, 1.0, 1.5, "秘能", 1, 30, 5, 1, true, false, false);

        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        when(技能注册服务.获取定义(anyString())).thenReturn(Optional.of(技能定义));
        when(技能释放服务.释放(any(技能上下文.class))).thenReturn(技能执行结果.成功);
    }

    @AfterEach
    void 清理事件上下文() {
        事件日志上下文.清理全部玩家目标因果凭证();
        事件日志上下文.清除();
    }

    private PlayerInteractEvent 创建交互事件(Player 玩家, Action 动作, ItemStack 主手物品) {
        PlayerInteractEvent 事件 = mock(PlayerInteractEvent.class);
        PlayerInventory 背包 = mock(PlayerInventory.class);
        when(事件.getAction()).thenReturn(动作);
        when(事件.getPlayer()).thenReturn(玩家);
        when(玩家.getInventory()).thenReturn(背包);
        when(背包.getItemInMainHand()).thenReturn(主手物品);
        return 事件;
    }

    private Player 创建玩家(UUID 标识) {
        Player 玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(标识);
        return 玩家;
    }

    private ItemStack 创建物品(Material 物品类型) {
        ItemStack 物品 = mock(ItemStack.class);
        when(物品.getType()).thenReturn(物品类型);
        return 物品;
    }

    // 9个槽位对应的物品类型参数源
    static Stream<Arguments> 技能槽位物品Provider() {
        return Stream.of(
                Arguments.of("第一技能-剑", Material.IRON_SWORD, 1, "1_1"),
                Arguments.of("第二技能-斧", Material.IRON_AXE, 2, "1_2"),
                Arguments.of("第三技能-镐", Material.IRON_PICKAXE, 3, "1_3"),
                Arguments.of("第四技能-铲", Material.IRON_SHOVEL, 4, "1_4"),
                Arguments.of("第五技能-锄", Material.IRON_HOE, 5, "1_5"),
                Arguments.of("第六技能-三叉戟", Material.TRIDENT, 6, "1_6"),
                Arguments.of("第六技能-木矛", Material.WOODEN_SPEAR, 6, "1_6"),
                Arguments.of("第六技能-铁矛", Material.IRON_SPEAR, 6, "1_6"),
                Arguments.of("第六技能-下界合金矛", Material.NETHERITE_SPEAR, 6, "1_6"),
                Arguments.of("第六技能-铜矛", Material.COPPER_SPEAR, 6, "1_6"),
                Arguments.of("第七技能-重锤", Material.MACE, 7, "1_7"),
                Arguments.of("第八技能-书", Material.BOOK, 8, "1_8")
        );
    }

    @Nested
    @DisplayName("正常流程 - 左键触发技能")
    class 左键触发技能 {

        @ParameterizedTest(name = "{0} - 应触发对应槽位技能")
        @MethodSource("mljy.表现层.监听器.技能按键绑定监听器测试#技能槽位物品Provider")
        void 左键物品_应触发对应槽位技能(String 描述, Material 物品类型, int 槽位编号, String 期望技能标识) {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(物品类型);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            ArgumentCaptor<技能上下文> 上下文捕获 = ArgumentCaptor.forClass(技能上下文.class);
            verify(技能释放服务).释放(上下文捕获.capture());
            assertEquals(期望技能标识, 上下文捕获.getValue().技能标识(),
                    描述 + " - 技能标识应为" + 期望技能标识);
            verify(战斗状态服务).进入战斗(eq(玩家标识), any(UUID.class));
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("左键方块 - 应触发技能且不取消事件")
        void 左键方块_应触发技能且不取消事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_BLOCK, 物品);
            UUID 输入根事件 = 事件日志上下文.获取或创建事件标识(事件);

            监听器.玩家交互(事件);

            verify(技能释放服务).释放(any(技能上下文.class));
            verify(战斗状态服务).进入战斗(eq(玩家标识), any(UUID.class));
            assertFalse(事件日志上下文.是否已托管(输入根事件),
                    "左键方块不是实体攻击前置输入，不应挂起玩家目标凭证");
            verify(事件, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    @DisplayName("边界值 - 空手和非左键")
    class 空手和非左键 {

        @Test
        @DisplayName("空手左键 - 不在驭空状态时不应触发技能")
        void 空手左键_不在驭空状态时不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 空手);
            when(驭空术服务.获取飞行状态(玩家标识)).thenReturn(飞行状态.地面);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
            verify(驭空术服务, never()).冲刺(any());
        }

        @Test
        @DisplayName("空手左键 - 飞行中时应触发驭空冲刺")
        void 空手左键_飞行中时应触发驭空冲刺() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 空手);
            when(驭空术服务.获取飞行状态(玩家标识)).thenReturn(飞行状态.飞行中);

            监听器.玩家交互(事件);

            verify(驭空术服务).冲刺(玩家标识);
            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("空手左键 - 准驭空状态时应触发驭空冲刺")
        void 空手左键_准驭空状态时应触发驭空冲刺() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 空手);
            when(驭空术服务.获取飞行状态(玩家标识)).thenReturn(飞行状态.准驭空);

            监听器.玩家交互(事件);

            verify(驭空术服务).冲刺(玩家标识);
            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("右键空气 - 不应触发技能")
        void 右键空气_不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
        }

        @Test
        @DisplayName("右键方块 - 不应触发技能")
        void 右键方块_不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.RIGHT_CLICK_BLOCK, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
        }
    }

    @Nested
    @DisplayName("边界值 - 非技能物品")
    class 非技能物品 {

        @Test
        @DisplayName("手持非技能物品左键 - 不应触发技能")
        void 手持非技能物品左键_不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            // 泥土不是技能物品
            ItemStack 物品 = 创建物品(Material.DIRT);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }
    }

    @Nested
    @DisplayName("异常输入 - 会话和定义不存在")
    class 会话和定义不存在 {

        @Test
        @DisplayName("玩家会话不存在 - 不应触发技能")
        void 玩家会话不存在_不应触发技能() {
            UUID 其他标识 = UUID.randomUUID();
            Player 玩家 = 创建玩家(其他标识);
            when(玩家服务.获取会话(其他标识)).thenReturn(Optional.empty());

            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
        }

        @Test
        @DisplayName("技能定义不存在 - 不应触发技能")
        void 技能定义不存在_不应触发技能() {
            when(技能注册服务.获取定义(anyString())).thenReturn(Optional.empty());

            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("玩家快照不存在 - 不应触发技能")
        void 玩家快照不存在_不应触发技能() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());

            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInteractEvent 事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家交互(事件);

            verify(技能释放服务, never()).释放(any());
        }
    }

    @Nested
    @DisplayName("实体被攻击 - 触发技能且不取消事件")
    class 实体被攻击触发技能 {

        @Test
        @DisplayName("玩家持剑攻击实体 - 应触发对应槽位技能且不取消事件")
        void 玩家持剑攻击实体_应触发技能且不取消事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(物品);

            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(玩家);
            when(事件.getEntity()).thenReturn(目标);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            ArgumentCaptor<技能上下文> 上下文捕获 = ArgumentCaptor.forClass(技能上下文.class);
            verify(技能释放服务).释放(上下文捕获.capture());
            assertEquals("1_1", 上下文捕获.getValue().技能标识(), "攻击实体应触发第一技能");
            verify(战斗状态服务).进入战斗(eq(玩家标识), any(UUID.class));
        }

        @Test
        @DisplayName("玩家空手攻击实体 - 不应触发技能且不取消事件")
        void 玩家空手攻击实体_不应触发技能且不取消事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(空手);

            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(玩家);
            when(事件.getEntity()).thenReturn(目标);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(技能释放服务, never()).释放(any());
        }

        @Test
        @DisplayName("玩家持非技能物品攻击 - 不应触发技能且不取消事件")
        void 玩家持非技能物品攻击_不应触发技能且不取消事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.DIRT);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(物品);

            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(玩家);
            when(事件.getEntity()).thenReturn(目标);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(技能释放服务, never()).释放(any());
        }

        @Test
        @DisplayName("非玩家攻击者 - 不应触发技能且不取消事件")
        void 非玩家攻击者_不应触发技能且不取消事件() {
            LivingEntity 攻击者 = mock(LivingEntity.class);
            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(anyBoolean());
            verify(技能释放服务, never()).释放(any());
        }
    }

    @Nested
    @DisplayName("因果事件关联")
    class 因果事件关联 {

        @Test
        @DisplayName("实体攻击事件自身作为根事件 - 技能与战斗监听器复用同一标识")
        void 实体攻击事件自身作为根事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(物品);
            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 伤害事件 = mock(EntityDamageByEntityEvent.class);
            when(伤害事件.getDamager()).thenReturn(玩家);
            when(伤害事件.getEntity()).thenReturn(目标);
            AtomicReference<UUID> 施法根事件 = new AtomicReference<>();
            doAnswer(调用 -> {
                施法根事件.set(事件日志上下文.获取当前事件标识或空());
                return 技能执行结果.成功;
            }).when(技能释放服务).释放(any(技能上下文.class));

            监听器.实体被攻击(伤害事件);

            UUID 伤害根事件 = 事件日志上下文.获取或创建事件标识(伤害事件);
            assertNotNull(伤害根事件);
            assertEquals(伤害根事件, 施法根事件.get(), "攻击与其触发施法必须共享同一根事件");
            verify(技能释放服务).释放(any(技能上下文.class));

            事件日志上下文.结束事件对象(伤害事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("左键空气输入立即执行 - 不依赖下一tick决定因果关系")
        void 左键空气输入立即执行() {
            Player 玩家 = 创建玩家(玩家标识);
            PlayerInteractEvent 交互事件 = 创建交互事件(
                    玩家, Action.LEFT_CLICK_AIR, 创建物品(Material.IRON_SWORD));
            UUID 输入根事件 = 事件日志上下文.获取或创建事件标识(交互事件);

            监听器.玩家交互(交互事件);

            verify(技能释放服务, times(1)).释放(any(技能上下文.class));
            assertTrue(事件日志上下文.是否已托管(输入根事件),
                    "明确输入完成后，根事件应保留到玩家目标命中或泄漏清理");
        }

        @Test
        @DisplayName("同一次物理攻击的前置输入与最终伤害事件 - 应共享显式根事件")
        void 同一次物理攻击_前置输入与最终伤害事件共享根事件() {
            Player 玩家 = 创建玩家(玩家标识);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            ItemStack 物品 = 创建物品(Material.IRON_AXE);
            when(背包.getItemInMainHand()).thenReturn(物品);

            PlayerInteractEvent 交互事件 = 创建交互事件(
                    玩家, Action.LEFT_CLICK_AIR, 创建物品(Material.IRON_AXE));
            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 伤害事件 = mock(EntityDamageByEntityEvent.class);
            when(伤害事件.getDamager()).thenReturn(玩家);
            when(伤害事件.getEntity()).thenReturn(目标);

            AtomicReference<UUID> 输入根事件 = new AtomicReference<>();
            doAnswer(调用 -> {
                输入根事件.set(事件日志上下文.获取当前事件标识或空());
                return 技能执行结果.成功;
            }).when(技能释放服务).释放(any(技能上下文.class));

            监听器.玩家交互(交互事件);
            监听器.实体被攻击(伤害事件);

            assertNotNull(输入根事件.get(), "前置输入必须创建根事件");
            // 去重窗口内实体被攻击不重复触发技能，仅消费凭证关联根事件
            verify(技能释放服务, times(1)).释放(any(技能上下文.class));
            UUID 伤害根事件 = 事件日志上下文.获取或创建事件标识(伤害事件);
            assertEquals(输入根事件.get(), 伤害根事件,
                    "同一次物理攻击的前置输入与最终伤害事件必须显式共享同一根事件");
            事件日志上下文.结束事件对象(伤害事件);
            事件日志上下文.清除();
        }
    }

    @Nested
    @DisplayName("玩家挥动 - 虚弱0伤害场景触发技能")
    class 玩家挥动触发技能 {

        private PlayerAnimationEvent 创建挥动事件(Player 玩家, ItemStack 主手物品) {
            PlayerAnimationEvent 事件 = mock(PlayerAnimationEvent.class);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(事件.getPlayer()).thenReturn(玩家);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(主手物品);
            return 事件;
        }

        @Test
        @DisplayName("虚弱0伤害挥动实体 - PlayerAnimationEvent应触发对应槽位技能")
        void 虚弱零伤害挥动_应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerAnimationEvent 事件 = 创建挥动事件(玩家, 物品);

            监听器.玩家挥动(事件);

            ArgumentCaptor<技能上下文> 上下文捕获 = ArgumentCaptor.forClass(技能上下文.class);
            verify(技能释放服务).释放(上下文捕获.capture());
            assertEquals("1_1", 上下文捕获.getValue().技能标识(),
                    "虚弱挥动应通过PlayerAnimationEvent触发第一技能");
            verify(战斗状态服务).进入战斗(eq(玩家标识), any(UUID.class));
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("玩家挥动监听器应为LOWEST优先级且ignoreCancelled=false")
        void 玩家挥动监听器_应为Lowest优先级且IgnoreCancelledFalse() throws NoSuchMethodException {
            java.lang.reflect.Method 方法 = 技能按键绑定监听器.class.getDeclaredMethod("玩家挥动", PlayerAnimationEvent.class);
            EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
            assertNotNull(注解, "玩家挥动方法必须有@EventHandler注解");
            assertFalse(注解.ignoreCancelled(),
                    "玩家挥动监听器应为ignoreCancelled=false，确保被取消的事件仍触发技能");
            assertEquals(org.bukkit.event.EventPriority.LOWEST, 注解.priority(),
                    "玩家挥动监听器应为LOWEST优先级，早于路径A(LOW)和路径B(LOW)派发，确保去重窗口生效");
        }

        @Test
        @DisplayName("玩家挥动后PlayerInteractEvent在去重窗口内 - 不应重复触发技能")
        void 玩家挥动后玩家交互不重复触发() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerAnimationEvent 挥动事件 = 创建挥动事件(玩家, 物品);
            PlayerInteractEvent 交互事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            监听器.玩家挥动(挥动事件);
            监听器.玩家交互(交互事件);

            verify(技能释放服务, times(1)).释放(any(技能上下文.class));
            verify(战斗状态服务, times(1)).进入战斗(eq(玩家标识), any(UUID.class));
        }

        @Test
        @DisplayName("玩家挥动后EntityDamageByEntityEvent在去重窗口内 - 不应重复触发技能")
        void 玩家挥动后实体被攻击不重复触发() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerAnimationEvent 挥动事件 = 创建挥动事件(玩家, 物品);

            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(物品);
            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 伤害事件 = mock(EntityDamageByEntityEvent.class);
            when(伤害事件.getDamager()).thenReturn(玩家);
            when(伤害事件.getEntity()).thenReturn(目标);

            监听器.玩家挥动(挥动事件);
            监听器.实体被攻击(伤害事件);

            verify(技能释放服务, times(1)).释放(any(技能上下文.class));
            verify(战斗状态服务, times(1)).进入战斗(eq(玩家标识), any(UUID.class));
            事件日志上下文.结束事件对象(伤害事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("第一次释放因公CD中失败 - 第二次触发应被去重拦截，不重复调用释放")
        void 公CD中失败路径也更新时间表() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerAnimationEvent 挥动事件 = 创建挥动事件(玩家, 物品);
            PlayerInteractEvent 交互事件 = 创建交互事件(玩家, Action.LEFT_CLICK_AIR, 物品);

            when(技能释放服务.释放(any(技能上下文.class))).thenReturn(技能执行结果.冷却中);

            监听器.玩家挥动(挥动事件);
            监听器.玩家交互(交互事件);

            verify(技能释放服务, times(1)).释放(any(技能上下文.class));
            verify(战斗状态服务, never()).进入战斗(any(), any());
        }

        @Test
        @DisplayName("空手挥动 - 不在驭空状态时不应触发技能")
        void 空手挥动_不在驭空状态时不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerAnimationEvent 事件 = 创建挥动事件(玩家, 空手);
            when(驭空术服务.获取飞行状态(玩家标识)).thenReturn(飞行状态.地面);

            监听器.玩家挥动(事件);

            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
            verify(驭空术服务, never()).冲刺(any());
        }

        @Test
        @DisplayName("空手挥动 - 飞行中时应触发驭空冲刺")
        void 空手挥动_飞行中时应触发驭空冲刺() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 空手 = 创建物品(Material.AIR);
            PlayerAnimationEvent 事件 = 创建挥动事件(玩家, 空手);
            when(驭空术服务.获取飞行状态(玩家标识)).thenReturn(飞行状态.飞行中);

            监听器.玩家挥动(事件);

            verify(驭空术服务).冲刺(玩家标识);
            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("持非技能物品挥动 - 不应触发技能")
        void 持非技能物品挥动_不应触发技能() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.DIRT);
            PlayerAnimationEvent 事件 = 创建挥动事件(玩家, 物品);

            监听器.玩家挥动(事件);

            verify(技能释放服务, never()).释放(any());
            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("玩家挥动触发技能后应创建玩家目标因果凭证 - 等待实体命中消费")
        void 玩家挥动触发技能后应创建玩家目标因果凭证() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerAnimationEvent 事件 = 创建挥动事件(玩家, 物品);
            UUID 输入根事件 = 事件日志上下文.获取或创建事件标识(事件);

            监听器.玩家挥动(事件);

            verify(技能释放服务).释放(any(技能上下文.class));
            assertTrue(事件日志上下文.是否已托管(输入根事件),
                    "玩家挥动触发技能后应保留玩家目标因果凭证，等待实体命中或泄漏清理");
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    @Nested
    @DisplayName("事件分发配置 - 确保被取消的事件仍触发技能")
    class 事件分发配置 {

        @Test
        @DisplayName("玩家交互监听器应为ignoreCancelled=false（修复左键空气不触发技能）")
        void 玩家交互监听器_应为ignoreCancelledFalse() throws NoSuchMethodException {
            java.lang.reflect.Method 方法 = 技能按键绑定监听器.class.getDeclaredMethod("玩家交互", PlayerInteractEvent.class);
            EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
            assertNotNull(注解, "玩家交互方法必须有@EventHandler注解");
            assertFalse(注解.ignoreCancelled(),
                    "玩家交互监听器应为ignoreCancelled=false，否则被取消的LEFT_CLICK_AIR事件不会触发技能");
        }

        @Test
        @DisplayName("实体被攻击监听器应为ignoreCancelled=false（确保躲闪成功取消事件后技能仍触发）")
        void 实体被攻击监听器_应为ignoreCancelledFalse() throws NoSuchMethodException {
            java.lang.reflect.Method 方法 = 技能按键绑定监听器.class.getDeclaredMethod("实体被攻击", EntityDamageByEntityEvent.class);
            EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
            assertNotNull(注解, "实体被攻击方法必须有@EventHandler注解");
            assertFalse(注解.ignoreCancelled(),
                    "实体被攻击监听器应为ignoreCancelled=false，确保躲闪成功取消事件后技能仍触发（符合需求'免疫普通攻击'语义）");
        }

        @Test
        @DisplayName("实体被攻击监听器不应提前结束根事件（确保后续战斗监听器复用同一事件标识）")
        void 实体被攻击监听器_不应提前结束根事件() {
            Player 玩家 = 创建玩家(玩家标识);
            ItemStack 物品 = 创建物品(Material.IRON_SWORD);
            PlayerInventory 背包 = mock(PlayerInventory.class);
            when(玩家.getInventory()).thenReturn(背包);
            when(背包.getItemInMainHand()).thenReturn(物品);

            LivingEntity 目标 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(玩家);
            when(事件.getEntity()).thenReturn(目标);

            UUID 根事件标识 = 事件日志上下文.获取或创建事件标识(事件);
            事件日志上下文.清除();

            监听器.实体被攻击(事件);

            assertEquals(根事件标识, 事件日志上下文.获取或创建事件标识(事件));
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    @Nested
    @DisplayName("技能按键类型枚举验证")
    class 技能按键类型枚举验证 {

        @Test
        @DisplayName("9个技能槽位 - 应全部存在")
        void 九个技能槽位_应全部存在() {
            assertEquals(9, 技能按键类型.values().length,
                    "应有9个技能按键类型");
        }

        @Test
        @DisplayName("槽位编号 - 应为1到9")
        void 槽位编号_应为1到9() {
            for (技能按键类型 类型 : 技能按键类型.values()) {
                int 编号 = 类型.获取槽位编号();
                assertTrue(编号 >= 1 && 编号 <= 9,
                        "槽位编号应在1-9范围内，实际: " + 编号);
            }
        }

        @Test
        @DisplayName("从物品查询null - 应返回null")
        void 从物品查询null_应返回null() {
            assertNull(技能按键类型.从物品查询(null));
        }

        @Test
        @DisplayName("从物品查询非技能物品 - 应返回null")
        void 从物品查询非技能物品_应返回null() {
            assertNull(技能按键类型.从物品查询(Material.DIRT));
            assertNull(技能按键类型.从物品查询(Material.STONE));
        }
    }
}
