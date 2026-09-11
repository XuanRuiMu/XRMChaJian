package mljy.表现层.监听器;

import mljy.玩家服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.仇恨服务;
import mljy.业务层.吸血处理服务;
import mljy.业务层.消息服务;
import mljy.业务层.消息.受击修正上下文;
import mljy.基础设施层.事件日志上下文;
import mljy.翻译服务;
import mljy.领域层.效果.效果实例;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("战斗监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 战斗监听器测试 {

    @Mock
    private 战斗状态服务 战斗状态服务;
    @Mock
    private 仇恨服务 仇恨服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private JavaPlugin 插件;

    private 战斗监听器 监听器;
    private 吸血处理服务 吸血处理服务mock;

    private UUID 攻击者标识;
    private UUID 受击者标识;
    private UUID 怪物标识;

    @BeforeEach
    void setUp() {
        吸血处理服务mock = mock(吸血处理服务.class);
        try (var _ = mockConstruction(NamespacedKey.class)) {
            监听器 = new 战斗监听器(
                    战斗状态服务, 仇恨服务, 消息服务, 玩家服务, 效果调度服务, 翻译服务, 插件, 吸血处理服务mock);
        }
        when(翻译服务.获取("战斗日志.伤害来源.未知")).thenReturn("未知来源");

        攻击者标识 = UUID.randomUUID();
        受击者标识 = UUID.randomUUID();
        怪物标识 = UUID.randomUUID();
    }

    private Player 创建玩家(UUID 标识) {
        Player 玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(标识);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(false);
        when(玩家.getPersistentDataContainer()).thenReturn(pdc);
        PlayerInventory 背包 = mock(PlayerInventory.class);
        when(背包.getItemInMainHand()).thenReturn(null);
        when(玩家.getInventory()).thenReturn(背包);
        return 玩家;
    }

    private Player 创建玩家带快照(UUID 标识, 玩家快照 快照) {
        Player 玩家 = 创建玩家(标识);
        when(玩家.getName()).thenReturn("玩家" + 标识.toString().substring(0, 4));
        when(玩家服务.获取快照(标识)).thenReturn(Optional.of(快照));
        return 玩家;
    }

    private 玩家快照 创建快照(UUID 标识) {
        return new 玩家快照(标识, "测试玩家", 1, null, null, Map.of(), null);
    }

    private LivingEntity 创建怪物(UUID 标识) {
        LivingEntity 怪物 = mock(LivingEntity.class);
        when(怪物.getUniqueId()).thenReturn(标识);
        when(怪物.getName()).thenReturn("怪物");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(false);
        when(怪物.getPersistentDataContainer()).thenReturn(pdc);
        return 怪物;
    }

    @Nested
    @DisplayName("正常流程 - 伤害事件进入战斗")
    class 伤害事件进入战斗 {

        @Test
        @DisplayName("玩家造成伤害 - 攻击者应进入战斗")
        void 玩家造成伤害_攻击者应进入战斗() {
            Player 攻击者 = 创建玩家(攻击者标识);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(战斗状态服务).进入战斗(eq(攻击者标识), any(UUID.class));
        }

        @Test
        @DisplayName("玩家造成伤害 - 应记录仇恨")
        void 玩家造成伤害_应记录仇恨() {
            Player 攻击者 = 创建玩家(攻击者标识);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(仇恨服务).记录伤害仇恨(怪物标识, 攻击者标识, 50.0);
        }

        @Test
        @DisplayName("玩家受到伤害 - 受击者应进入战斗")
        void 玩家受到伤害_受击者应进入战斗() {
            LivingEntity 攻击者 = 创建怪物(怪物标识);
            Player 受击者 = 创建玩家(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);

            监听器.实体被攻击(事件);

            verify(战斗状态服务).进入战斗(eq(受击者标识), any(UUID.class));
        }

        @Test
        @DisplayName("玩家攻击玩家 - 双方都应进入战斗但不记录仇恨")
        void 玩家攻击玩家_双方都应进入战斗但不记录仇恨() {
            Player 攻击者 = 创建玩家(攻击者标识);
            Player 受击者 = 创建玩家(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(30.0);

            监听器.实体被攻击(事件);

            verify(战斗状态服务).进入战斗(eq(攻击者标识), any(UUID.class));
            verify(战斗状态服务).进入战斗(eq(受击者标识), any(UUID.class));
            verify(仇恨服务, never()).记录伤害仇恨(any(), any(), anyDouble());
        }
    }

    @Nested
    @DisplayName("FP-01 受伤日志来源覆盖 - 每个伤害事件只输出一次")
    class 受伤日志来源覆盖 {

        @Test
        @DisplayName("怪物伤害 - 应输出怪物可读名称且只发送一次受伤日志")
        void 怪物伤害_应输出一次受伤日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            LivingEntity 怪物 = 创建怪物(怪物标识);
            when(怪物.getName()).thenReturn("僵尸");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(怪物);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(8.0);

            监听器.实体被攻击(事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("僵尸"), eq("8.0"));
        }

        @Test
        @DisplayName("无发射者箭 - 应使用官方中文名称")
        void 无发射者箭_应输出官方中文名称() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            Projectile 箭 = mock(Projectile.class);
            when(箭.getType()).thenReturn(org.bukkit.entity.EntityType.ARROW);
            when(翻译服务.获取("战斗日志.伤害来源.ARROW")).thenReturn("箭");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(箭);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(4.0);

            监听器.实体被攻击(事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("箭"), eq("4.0"));
        }

        @Test
        @DisplayName("TNT伤害 - 应输出TNT可读名称")
        void TNT伤害_应输出可读名称() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            Entity TNT = mock(Entity.class);
            when(TNT.getType()).thenReturn(org.bukkit.entity.EntityType.TNT);
            when(翻译服务.获取("战斗日志.伤害来源.TNT")).thenReturn("TNT");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(TNT);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(12.0);

            监听器.实体被攻击(事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("TNT"), eq("12.0"));
        }

        @Test
        @DisplayName("环境伤害 - 应按伤害原因输出可读名称")
        void 环境伤害_应输出可读名称() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("战斗日志.伤害来源.LAVA")).thenReturn("岩浆");
            EntityDamageEvent 事件 = mock(EntityDamageEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.LAVA);
            when(事件.getFinalDamage()).thenReturn(6.0);

            监听器.实体被攻击(事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("岩浆"), eq("6.0"));
        }

        @Test
        @DisplayName("血条方块怪物名 - 应拒绝污染并回退客户端官方实体翻译")
        void 血条方块怪物名_应回退客户端翻译() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            LivingEntity 怪物 = 创建怪物(怪物标识);
            when(怪物.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("§c[████████████]"));
            when(怪物.getName()).thenReturn("[████████████]");
            org.bukkit.entity.EntityType 类型 = mock(org.bukkit.entity.EntityType.class);
            when(类型.translationKey()).thenReturn("entity.minecraft.zombie");
            when(怪物.getType()).thenReturn(类型);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(怪物);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(37.4);

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("<lang:entity.minecraft.zombie>"),
                    eq("37.4"));
        }

        @Test
        @DisplayName("未知伤害 - 应输出未知来源而不是裸枚举名")
        void 未知伤害_应输出未知来源() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            EntityDamageEvent 事件 = mock(EntityDamageEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.CUSTOM);
            when(事件.getFinalDamage()).thenReturn(1.0);

            监听器.实体被攻击(事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("未知来源"), eq("1.0"));
        }
    }

    @Nested
    @DisplayName("FP-02 原生实体伤害护盾链路")
    class 原生实体伤害护盾链路 {

        @Test
        @DisplayName("原生物理伤害应先由奥术护盾吸收并合并为带护盾抵挡的单条日志")
        void 原生物理伤害_应吸收并合并为带护盾抵挡日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("effect.奥能法师.奥术护盾.name")).thenReturn("奥术护盾");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(10.0);
            when(事件.getFinalDamage()).thenReturn(6.0);
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 4, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));

            监听器.原生实体伤害护盾吸收(事件);
            监听器.实体被攻击(事件);

            verify(事件).setDamage(6.0);
            verify(效果调度服务).移除(受击者标识, "1_3_1");
            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), eq("攻击者"), eq("6.0"), eq("4.0"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), anyString(), anyString());
        }

        @Test
        @DisplayName("连续小数原生伤害应按精确护盾值消费并在耗尽时移除")
        void 连续小数原生伤害_按精确值消费并移除() {
            Player 受击者 = 创建玩家(受击者标识);
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 2, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));

            EntityDamageEvent 第一次事件 = mock(EntityDamageEvent.class);
            when(第一次事件.getEntity()).thenReturn(受击者);
            when(第一次事件.getDamage()).thenReturn(0.5D);
            监听器.原生实体伤害护盾吸收(第一次事件);

            EntityDamageEvent 第二次事件 = mock(EntityDamageEvent.class);
            when(第二次事件.getEntity()).thenReturn(受击者);
            when(第二次事件.getDamage()).thenReturn(1.5D);
            监听器.原生实体伤害护盾吸收(第二次事件);

            verify(第一次事件).setDamage(0.0D);
            verify(第二次事件).setDamage(0.0D);
            verify(效果调度服务).设置层数(受击者标识, "1_3_1", 2);
            verify(效果调度服务).移除(受击者标识, "1_3_1");
            事件日志上下文.结束事件对象(第一次事件);
            事件日志上下文.结束事件对象(第二次事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("带XRM技能标记的原生事件应跳过护盾处理")
        void XRM技能标记_应跳过原生护盾处理() {
            Player 受击者 = 创建玩家(受击者标识);
            PersistentDataContainer pdc = 受击者.getPersistentDataContainer();
            when(pdc.has(any(NamespacedKey.class), any())).thenReturn(true);
            EntityDamageEvent 事件 = mock(EntityDamageEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamage()).thenReturn(10.0);

            监听器.原生实体伤害护盾吸收(事件);

            verifyNoInteractions(效果调度服务);
            verify(事件, never()).setDamage(anyDouble());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("无奥术护盾效果时应保持原生伤害不变")
        void 无奥术护盾_应保持原生伤害() {
            Player 受击者 = 创建玩家(受击者标识);
            EntityDamageEvent 事件 = mock(EntityDamageEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamage()).thenReturn(10.0);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of());

            监听器.原生实体伤害护盾吸收(事件);

            verify(效果调度服务).获取列表(受击者标识);
            verify(事件, never()).setDamage(anyDouble());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("FP-04: 护盾耗尽时仍应发送合并抵挡日志（shield=4 vs damage=10）")
        void 护盾耗尽_仍应发送合并抵挡日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("effect.奥能法师.奥术护盾.name")).thenReturn("奥术护盾");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(10.0);
            when(事件.getFinalDamage()).thenReturn(6.0);
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 4, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));

            监听器.原生实体伤害护盾吸收(事件);
            监听器.实体被攻击(事件);

            verify(事件).setDamage(6.0);
            verify(效果调度服务).移除(受击者标识, "1_3_1");
            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), eq("攻击者"), eq("6.0"), eq("4.0"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), anyString(), anyString());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("FP-04: 护盾耗尽后再次受击只发送受到伤害日志不发送合并抵挡日志")
        void 护盾耗尽后再次受击_只发送受到伤害日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("effect.奥能法师.奥术护盾.name")).thenReturn("奥术护盾");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");

            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 4, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));
            EntityDamageByEntityEvent 第一次事件 = mock(EntityDamageByEntityEvent.class);
            when(第一次事件.getDamager()).thenReturn(攻击者);
            when(第一次事件.getEntity()).thenReturn(受击者);
            when(第一次事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(第一次事件.getDamage()).thenReturn(10.0);
            when(第一次事件.getFinalDamage()).thenReturn(6.0);

            监听器.原生实体伤害护盾吸收(第一次事件);
            监听器.实体被攻击(第一次事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), eq("攻击者"), eq("6.0"), eq("4.0"));
            事件日志上下文.结束事件对象(第一次事件);

            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of());
            EntityDamageByEntityEvent 第二次事件 = mock(EntityDamageByEntityEvent.class);
            when(第二次事件.getDamager()).thenReturn(攻击者);
            when(第二次事件.getEntity()).thenReturn(受击者);
            when(第二次事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(第二次事件.getDamage()).thenReturn(8.0);
            when(第二次事件.getFinalDamage()).thenReturn(8.0);

            监听器.原生实体伤害护盾吸收(第二次事件);
            监听器.实体被攻击(第二次事件);

            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), anyString(), anyString(), anyString());
            verify(消息服务, times(1)).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            事件日志上下文.结束事件对象(第二次事件);
            事件日志上下文.清除();
        }
    }

    @Nested
    @DisplayName("FP-06 普通攻击日志 - 非XRM技能伤害应发送战斗日志")
    class 普通攻击日志 {

        @Test
        @DisplayName("玩家普通攻击怪物 - 应发送普通攻击日志给攻击者")
        void 玩家普通攻击怪物_应发送普通攻击日志() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            when(翻译服务.获取("战斗日志.空手")).thenReturn("空手");
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(10.0);

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(攻击者快照), any(UUID.class),
                    eq("战斗日志.普通攻击"), eq("空手"), eq("怪物"), eq("10.0"));
        }

        @Test
        @DisplayName("玩家普通攻击玩家 - 应发送普通攻击日志给攻击者且受到伤害日志给受击者")
        void 玩家普通攻击玩家_应发送双方日志() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            when(攻击者.getName()).thenReturn("攻击者");
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("战斗日志.空手")).thenReturn("空手");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(15.0);

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(攻击者快照), any(UUID.class),
                    eq("战斗日志.普通攻击"), eq("空手"), anyString(), eq("15.0"));
            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("攻击者"), eq("15.0"));
        }

        @Test
        @DisplayName("XRM技能伤害 - 不应发送普通攻击日志")
        void XRM技能伤害_不应发送普通攻击日志() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            LivingEntity 目标 = mock(LivingEntity.class);
            when(目标.getUniqueId()).thenReturn(怪物标识);
            when(目标.getName()).thenReturn("怪物");
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(true);
            when(目标.getPersistentDataContainer()).thenReturn(pdc);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(消息服务, never()).发送战斗日志(any(), any(UUID.class),
                    eq("战斗日志.普通攻击"), any(), any(), any());
        }

        @Test
        @DisplayName("攻击者无快照 - 不应发送普通攻击日志")
        void 攻击者无快照_不应发送普通攻击日志() {
            Player 攻击者 = 创建玩家(攻击者标识);
            when(玩家服务.获取快照(攻击者标识)).thenReturn(Optional.empty());
            when(翻译服务.获取("战斗日志.空手")).thenReturn("空手");
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(10.0);

            监听器.实体被攻击(事件);

            verify(消息服务, never()).发送战斗日志(any(), any(UUID.class), anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("玩家手持无自定义名物品 - 武器名应无裸括号（FP-A点3：避免[WOODEN_PICKAXE]）")
        void 玩家手持物品_武器名应无裸括号() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            when(攻击者.locale()).thenReturn(Locale.SIMPLIFIED_CHINESE);
            ItemStack 主手物品 = mock(ItemStack.class);
            Material 类型 = mock(Material.class);
            when(类型.isAir()).thenReturn(false);
            when(类型.translationKey()).thenReturn("item.minecraft.wooden_pickaxe");
            when(主手物品.getType()).thenReturn(类型);
            when(主手物品.hasItemMeta()).thenReturn(false);
            when(攻击者.getInventory().getItemInMainHand()).thenReturn(主手物品);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(10.0);

            监听器.实体被攻击(事件);

            ArgumentCaptor<String> 武器名捕获 = ArgumentCaptor.forClass(String.class);
            verify(消息服务).发送战斗日志(eq(攻击者快照), any(UUID.class), eq("战斗日志.普通攻击"),
                    武器名捕获.capture(), eq("怪物"), eq("10.0"));
            String 武器名 = 武器名捕获.getValue();
            assertEquals("<lang:item.minecraft.wooden_pickaxe>", 武器名);
            assertFalse(武器名.contains("[") || 武器名.contains("]"),
                    "武器名不应含裸括号，实际: " + 武器名);
        }

        @Test
        @DisplayName("玩家手持石斧 - 武器名应使用客户端官方中文翻译")
        void 玩家手持石斧_武器名应为石斧() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            ItemStack 主手物品 = mock(ItemStack.class);
            Material 类型 = mock(Material.class);
            when(类型.isAir()).thenReturn(false);
            when(类型.translationKey()).thenReturn("item.minecraft.stone_axe");
            when(主手物品.getType()).thenReturn(类型);
            when(主手物品.hasItemMeta()).thenReturn(false);
            when(攻击者.getInventory().getItemInMainHand()).thenReturn(主手物品);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(10.0);

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(攻击者快照), any(UUID.class), eq("战斗日志.普通攻击"),
                    eq("<lang:item.minecraft.stone_axe>"), eq("怪物"), eq("10.0"));
        }

        @Test
        @DisplayName("玩家手持自定义名物品 - 武器名应使用自定义名（FP-A点3：自定义名分支保留）")
        void 玩家手持自定义名物品_武器名应使用自定义名() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            ItemStack 主手物品 = mock(ItemStack.class);
            ItemMeta 元数据 = mock(ItemMeta.class);
            Material 类型 = mock(Material.class);
            when(类型.isAir()).thenReturn(false);
            when(主手物品.getType()).thenReturn(类型);
            when(主手物品.hasItemMeta()).thenReturn(true);
            when(主手物品.getItemMeta()).thenReturn(元数据);
            when(元数据.hasDisplayName()).thenReturn(true);
            when(元数据.displayName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("神兵"));
            when(攻击者.getInventory().getItemInMainHand()).thenReturn(主手物品);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(10.0);

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(攻击者快照), any(UUID.class), eq("战斗日志.普通攻击"),
                    eq("神兵"), eq("怪物"), eq("10.0"));
        }
    }

    @Nested
    @DisplayName("正常流程 - 治疗事件进入战斗")
    class 治疗事件进入战斗 {

        @Test
        @DisplayName("玩家受到治疗 - 应进入战斗")
        void 玩家受到治疗_应进入战斗() {
            Player 玩家 = 创建玩家(攻击者标识);
            EntityRegainHealthEvent 事件 = mock(EntityRegainHealthEvent.class);
            when(事件.getEntity()).thenReturn(玩家);
            when(事件.getRegainReason()).thenReturn(EntityRegainHealthEvent.RegainReason.MAGIC);

            监听器.实体恢复生命(事件);

            verify(战斗状态服务).进入战斗(攻击者标识);
        }

        @Test
        @DisplayName("SATIATED治疗 - 不应进入战斗")
        void 饱食治疗_不应进入战斗() {
            Player 玩家 = 创建玩家(攻击者标识);
            EntityRegainHealthEvent 事件 = mock(EntityRegainHealthEvent.class);
            when(事件.getEntity()).thenReturn(玩家);
            when(事件.getRegainReason()).thenReturn(EntityRegainHealthEvent.RegainReason.SATIATED);

            监听器.实体恢复生命(事件);

            verify(战斗状态服务, never()).进入战斗(any());
        }
    }

    @Nested
    @DisplayName("边界值 - 非玩家实体")
    class 非玩家实体 {

        @Test
        @DisplayName("怪物受到伤害 - 不应进入战斗")
        void 怪物受到伤害_不应进入战斗() {
            LivingEntity 攻击者 = 创建怪物(怪物标识);
            LivingEntity 目标 = 创建怪物(UUID.randomUUID());
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);

            监听器.实体被攻击(事件);

            verify(战斗状态服务, never()).进入战斗(any());
        }

        @Test
        @DisplayName("怪物受到治疗 - 不应进入战斗")
        void 怪物受到治疗_不应进入战斗() {
            LivingEntity 怪物 = 创建怪物(怪物标识);
            EntityRegainHealthEvent 事件 = mock(EntityRegainHealthEvent.class);
            when(事件.getEntity()).thenReturn(怪物);
            when(事件.getRegainReason()).thenReturn(EntityRegainHealthEvent.RegainReason.MAGIC);

            监听器.实体恢复生命(事件);

            verify(战斗状态服务, never()).进入战斗(any());
        }
    }

    @Nested
    @DisplayName("FP-03 奥术护盾受伤日志合并")
    class 奥术护盾受伤日志合并 {

        @Test
        @DisplayName("有护盾吸收时发送一条合并抵挡日志，不发送受到伤害和吸收日志")
        void 有护盾吸收时_应发送合并抵挡日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("effect.奥能法师.奥术护盾.name")).thenReturn("奥术护盾");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(8.0);
            when(事件.getFinalDamage()).thenReturn(5.0);
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 3, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));

            监听器.原生实体伤害护盾吸收(事件);
            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), eq("攻击者"), eq("5.0"), eq("3.0"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("无护盾吸收时发送原受到伤害日志，不发送合并抵挡和吸收日志")
        void 无护盾吸收时_应发送原受到伤害日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(6.0);
            when(事件.getFinalDamage()).thenReturn(6.0);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of());

            监听器.原生实体伤害护盾吸收(事件);
            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("攻击者"), eq("6.0"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), anyString(), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("多个吸收修正时取第一个作为合并抵挡数值")
        void 多个吸收修正时_应取第一个作为抵挡数值() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("effect.奥能法师.奥术护盾.name")).thenReturn("奥术护盾");
            when(翻译服务.获取("skill.奥能法师.测试护盾2.name")).thenReturn("测试护盾2");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(10.0);
            when(事件.getFinalDamage()).thenReturn(8.0);
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 2, -1);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of(护盾));

            监听器.原生实体伤害护盾吸收(事件);

            UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
            事件日志上下文.在事件中执行(事件标识, () ->
                    受击修正上下文.记录吸收(受击者标识, 事件标识, 5.0,
                            "skill.奥能法师.测试护盾2.name"));

            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), eq("攻击者"), eq("8.0"), eq("2.0"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }

        @Test
        @DisplayName("有减伤修正但无护盾吸收时发送原受到伤害日志并附加减伤日志")
        void 有减伤无护盾吸收时_应发送原日志并附加减伤日志() {
            玩家快照 受击者快照 = 创建快照(受击者标识);
            Player 受击者 = 创建玩家带快照(受击者标识, 受击者快照);
            when(翻译服务.获取("skill.奥能法师.树皮术.name")).thenReturn("树皮术");

            Player 攻击者 = 创建玩家带快照(攻击者标识, 创建快照(攻击者标识));
            when(攻击者.getName()).thenReturn("攻击者");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
            when(事件.getDamage()).thenReturn(7.0);
            when(事件.getFinalDamage()).thenReturn(7.0);
            when(效果调度服务.获取列表(受击者标识)).thenReturn(List.of());

            UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
            事件日志上下文.在事件中执行(事件标识, () ->
                    受击修正上下文.记录(受击者标识, 事件标识, 1.5,
                            "skill.奥能法师.树皮术.name", 受击修正上下文.修正类型.减伤));

            监听器.原生实体伤害护盾吸收(事件);
            监听器.实体被攻击(事件);

            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害"), eq("攻击者"), eq("7.0"));
            verify(消息服务).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.减伤"), eq("树皮术"), eq("1.5"));
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.受到伤害带护盾抵挡"), anyString(), anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(eq(受击者快照), any(UUID.class),
                    eq("战斗日志.吸收"), anyString(), anyString());
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    @Nested
    @DisplayName("FP-07 吸血集中式接入 - 玩家造成伤害触发吸血")
    class 吸血集中式接入 {

        @Test
        @DisplayName("玩家普通攻击怪物 - 应触发处理伤害吸血（普攻权威值=事件.getFinalDamage）")
        void 普通攻击_应触发处理伤害吸血() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            when(玩家服务.获取快照(攻击者标识)).thenReturn(Optional.of(攻击者快照));
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(吸血处理服务mock).处理伤害吸血(eq(攻击者快照), eq(50.0));
        }

        @Test
        @DisplayName("XRM技能伤害 - 仍应触发处理伤害吸血（权威伤害值取自目标PDC技能伤害键）")
        void XRM技能伤害_应触发处理伤害吸血() {
            玩家快照 攻击者快照 = 创建快照(攻击者标识);
            when(玩家服务.获取快照(攻击者标识)).thenReturn(Optional.of(攻击者快照));
            Player 攻击者 = 创建玩家带快照(攻击者标识, 攻击者快照);
            LivingEntity 目标 = mock(LivingEntity.class);
            when(目标.getUniqueId()).thenReturn(怪物标识);
            when(目标.getName()).thenReturn("怪物");
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(true);
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(80.0);
            when(目标.getPersistentDataContainer()).thenReturn(pdc);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(吸血处理服务mock).处理伤害吸血(eq(攻击者快照), eq(80.0));
        }

        @Test
        @DisplayName("攻击者无快照 - 不应触发处理伤害吸血")
        void 攻击者无快照_不应触发处理伤害吸血() {
            when(玩家服务.获取快照(攻击者标识)).thenReturn(Optional.empty());
            Player 攻击者 = 创建玩家(攻击者标识);
            LivingEntity 目标 = 创建怪物(怪物标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(目标);
            when(事件.getFinalDamage()).thenReturn(50.0);

            监听器.实体被攻击(事件);

            verify(吸血处理服务mock, never()).处理伤害吸血(any(), anyDouble());
        }
    }
}
