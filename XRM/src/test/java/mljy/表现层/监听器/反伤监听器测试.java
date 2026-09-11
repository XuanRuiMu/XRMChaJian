package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.仇恨服务;
import mljy.业务层.吸血处理服务;
import mljy.业务层.消息服务;
import mljy.翻译服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("反伤监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 反伤监听器测试 {

    @Mock
    private 玩家服务 玩家服务mock;
    @Mock
    private 属性计算服务 属性计算服务mock;
    @Mock
    private 属性注册表 属性注册表mock;

    private 反伤监听器 监听器;
    private UUID 受击者标识;

    @BeforeEach
    void setUp() {
        监听器 = new 反伤监听器(玩家服务mock, 属性计算服务mock, 属性注册表mock);
        受击者标识 = UUID.randomUUID();
    }

    private Player 创建玩家(UUID 标识, String 名称) {
        Player 玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(标识);
        when(玩家.getName()).thenReturn(名称);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(false);
        when(玩家.getPersistentDataContainer()).thenReturn(pdc);
        PlayerInventory 背包 = mock(PlayerInventory.class);
        when(背包.getItemInMainHand()).thenReturn(null);
        when(玩家.getInventory()).thenReturn(背包);
        return 玩家;
    }

    private LivingEntity 创建攻击者(UUID 标识) {
        LivingEntity 攻击者 = mock(LivingEntity.class);
        when(攻击者.getUniqueId()).thenReturn(标识);
        return 攻击者;
    }

    private 玩家快照 创建快照(UUID 标识) {
        return new 玩家快照(标识, "受击者", 1, null, 创建零属性快照(), Map.of(), null);
    }

    private 属性快照 创建零属性快照() {
        return 属性快照.创建(
                100, 1.0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0);
    }

    private 属性快照 创建带反伤属性快照(double 反伤值) {
        return 创建零属性快照().带扩展属性("反伤", 反伤值);
    }

    private EntityDamageByEntityEvent 创建事件(Entity 受击者, Entity 攻击者, double 伤害) {
        EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
        when(事件.getEntity()).thenReturn(受击者);
        when(事件.getDamager()).thenReturn(攻击者);
        when(事件.getFinalDamage()).thenReturn(伤害);
        return 事件;
    }

    @Nested
    @DisplayName("反伤触发")
    class 反伤触发 {

        @Test
        @DisplayName("攻击者造成100伤害 受击者反伤20点→攻击者受到20伤害")
        void 伤害100_反伤20_应反弹20() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者).damage(eq(20.0), eq(受击者));
        }

        @Test
        @DisplayName("反伤为0→不反弹伤害")
        void 反伤0_不应反弹() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(0);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("反伤为100→反弹等额伤害(100%)")
        void 反伤100_应反弹等额() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(100);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者).damage(eq(100.0), eq(受击者));
        }

        @Test
        @DisplayName("反伤50 原始伤害200→反弹100")
        void 伤害200_反伤50_应反弹100() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(50);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 200.0);

            监听器.实体被攻击(事件);

            verify(攻击者).damage(eq(100.0), eq(受击者));
        }
    }

    @Nested
    @DisplayName("ThreadLocal防递归")
    class ThreadLocal防递归 {

        @Test
        @DisplayName("反伤触发的伤害不应再次触发反伤(防无限递归)")
        void 反伤触发的伤害_不应再次触发反伤() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 递归事件 = mock(EntityDamageByEntityEvent.class);
            when(递归事件.getEntity()).thenReturn(受击者);
            when(递归事件.getDamager()).thenReturn(攻击者);
            when(递归事件.getFinalDamage()).thenReturn(20.0);

            doAnswer(invocation -> {
                监听器.实体被攻击(递归事件);
                return null;
            }).when(攻击者).damage(anyDouble(), any(Entity.class));

            EntityDamageByEntityEvent 原始事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(原始事件);

            verify(攻击者, times(1)).damage(anyDouble(), any(Entity.class));
            verify(攻击者, times(1)).damage(eq(20.0), eq(受击者));
        }

        @Test
        @DisplayName("反伤处理后ThreadLocal应被重置(允许后续事件正常触发反伤)")
        void 反伤处理后_ThreadLocal应被重置() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            UUID 攻击者标识 = UUID.randomUUID();
            LivingEntity 攻击者 = 创建攻击者(攻击者标识);
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件1 = 创建事件(受击者, 攻击者, 100.0);
            EntityDamageByEntityEvent 事件2 = 创建事件(受击者, 攻击者, 50.0);

            监听器.实体被攻击(事件1);
            监听器.实体被攻击(事件2);

            verify(攻击者, times(1)).damage(eq(20.0), eq(受击者));
            verify(攻击者, times(1)).damage(eq(10.0), eq(受击者));
            verify(攻击者, times(2)).damage(anyDouble(), eq(受击者));
        }
    }

    @Nested
    @DisplayName("跳过反伤场景")
    class 跳过反伤场景 {

        @Test
        @DisplayName("非Player受击者应不触发反伤")
        void 非Player受击者_应不触发() {
            LivingEntity 怪物受击者 = mock(LivingEntity.class);
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());

            EntityDamageByEntityEvent 事件 = 创建事件(怪物受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(玩家服务mock, never()).获取快照(any());
            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("非LivingEntity攻击者应不触发反伤")
        void 非LivingEntity攻击者_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            Entity 非生物攻击者 = mock(Entity.class);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 非生物攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(玩家服务mock, never()).获取快照(any());
        }

        @Test
        @DisplayName("玩家快照不存在应不触发反伤")
        void 玩家快照不存在_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.empty());

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(属性计算服务mock, never()).计算(any());
            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Nested
    @DisplayName("边界值(反伤值异常)")
    class 边界值反伤异常 {

        @Test
        @DisplayName("NaN反伤值应不触发反伤")
        void NaN反伤值_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(Double.NaN);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("Infinity反伤值应不触发反伤")
        void Infinity反伤值_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(Double.POSITIVE_INFINITY);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("负数反伤值应不触发反伤")
        void 负数反伤值_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(-10);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("反伤属性不存在(Map中无键)应返回0不触发反伤")
        void 无反伤属性_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建零属性快照();

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Nested
    @DisplayName("边界值(原始伤害异常)")
    class 边界值伤害异常 {

        @Test
        @DisplayName("NaN原始伤害应不触发反伤")
        void NaN原始伤害_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, Double.NaN);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("Infinity原始伤害应不触发反伤")
        void Infinity原始伤害_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, Double.POSITIVE_INFINITY);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("零原始伤害应不触发反伤")
        void 零原始伤害_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, 0.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("负数原始伤害应不触发反伤")
        void 负数原始伤害_应不触发() {
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            LivingEntity 攻击者 = 创建攻击者(UUID.randomUUID());
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者, -50.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Nested
    @DisplayName("Guice依赖注入与构造时注册")
    class Guice依赖注入与构造 {

        @Test
        @DisplayName("构造器应有@Inject注解(防止XRM启动失败回归)")
        void 构造器_应有Inject注解() throws NoSuchMethodException {
            java.lang.reflect.Constructor<反伤监听器> 构造器 = 反伤监听器.class.getDeclaredConstructor(
                    玩家服务.class, 属性计算服务.class, 属性注册表.class);

            assertTrue(构造器.isAnnotationPresent(Inject.class),
                    "反伤监听器构造器必须有@Inject注解，否则Guice无法创建实例导致XRM启动失败");
        }

        @Test
        @DisplayName("构造时应向属性注册表注册反伤属性元数据")
        void 构造_应注册反伤属性() {
            verify(属性注册表mock).注册(argThat(元数据 ->
                    元数据 != null &&
                    "反伤".equals(元数据.命令名()) &&
                    "反伤".equals(元数据.修饰器类别()) &&
                    "管理员指令.属性.查看.反伤".equals(元数据.显示翻译键())));
        }

        @Test
        @DisplayName("构造器应只注册一次反伤属性")
        void 构造_应只注册一次() {
            verify(属性注册表mock, times(1)).注册(any());
        }
    }

    @Nested
    @DisplayName("可插入性验证(需求.md第5行)")
    class 可插入性验证 {

        @Test
        @DisplayName("不注册反伤监听器时 战斗监听器仍正常处理伤害事件(进入战斗)")
        void 无反伤监听器_战斗监听器仍正常处理伤害() {
            战斗状态服务 战斗状态mock = mock(战斗状态服务.class);
            仇恨服务 仇恨服务mock = mock(仇恨服务.class);
            消息服务 消息服务mock = mock(消息服务.class);
            玩家服务 玩家服务局部mock = mock(玩家服务.class);
            翻译服务 翻译服务mock = mock(翻译服务.class);
            JavaPlugin 插件mock = mock(JavaPlugin.class);
            战斗监听器 战斗监听;
            try (var _ = mockConstruction(NamespacedKey.class)) {
                战斗监听 = new 战斗监听器(
                        战斗状态mock, 仇恨服务mock, 消息服务mock, 玩家服务局部mock,
                        mock(效果调度服务.class), 翻译服务mock, 插件mock, mock(吸血处理服务.class));
            }

            Player 攻击者 = 创建玩家(UUID.randomUUID(), "攻击者");
            UUID 受击UUID = UUID.randomUUID();
            Player 受击者 = 创建玩家(受击UUID, "受击者");

            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(50.0);

            战斗监听.实体被攻击(事件);

            verify(战斗状态mock).进入战斗(eq(攻击者.getUniqueId()), any(UUID.class));
            verify(战斗状态mock).进入战斗(eq(受击UUID), any(UUID.class));
        }

        @Test
        @DisplayName("反伤监听器与战斗监听器可共存(同一事件被两者独立处理)")
        void 反伤监听器与战斗监听器可共存() {
            战斗状态服务 战斗状态mock = mock(战斗状态服务.class);
            仇恨服务 仇恨服务mock = mock(仇恨服务.class);
            消息服务 消息服务mock = mock(消息服务.class);
            玩家服务 玩家服务局部mock = mock(玩家服务.class);
            翻译服务 翻译服务mock = mock(翻译服务.class);
            JavaPlugin 插件mock = mock(JavaPlugin.class);
            战斗监听器 战斗监听;
            try (var _ = mockConstruction(NamespacedKey.class)) {
                战斗监听 = new 战斗监听器(
                        战斗状态mock, 仇恨服务mock, 消息服务mock, 玩家服务局部mock,
                        mock(效果调度服务.class), 翻译服务mock, 插件mock, mock(吸血处理服务.class));
            }

            Player 攻击者 = 创建玩家(UUID.randomUUID(), "攻击者");
            Player 受击者 = 创建玩家(受击者标识, "受击者");
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建带反伤属性快照(20);

            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);

            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getDamager()).thenReturn(攻击者);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getFinalDamage()).thenReturn(100.0);

            战斗监听.实体被攻击(事件);
            监听器.实体被攻击(事件);

            verify(战斗状态mock).进入战斗(eq(攻击者.getUniqueId()), any(UUID.class));
            verify(战斗状态mock).进入战斗(eq(受击者标识), any(UUID.class));
            verify(攻击者).damage(eq(20.0), eq(受击者));
        }

        @Test
        @DisplayName("反伤监听器不依赖战斗监听器(独立组件,删除不影响核心伤害流程)")
        void 反伤监听器_是独立组件() throws NoSuchMethodException {
            java.lang.reflect.Constructor<?>[] 构造器列表 = 反伤监听器.class.getDeclaredConstructors();
            assertEquals(1, 构造器列表.length, "反伤监听器应只有一个构造器");

            java.lang.reflect.Constructor<?> 构造器 = 构造器列表[0];
            Class<?>[] 参数类型 = 构造器.getParameterTypes();
            assertEquals(3, 参数类型.length, "反伤监听器应依赖3个服务: 玩家服务/属性计算服务/属性注册表");
            assertEquals(玩家服务.class, 参数类型[0]);
            assertEquals(属性计算服务.class, 参数类型[1]);
            assertEquals(属性注册表.class, 参数类型[2]);

            assertNotEquals(战斗监听器.class, 参数类型[0], "反伤监听器不应依赖战斗监听器");
            for (Class<?> 类型 : 参数类型) {
                assertNotEquals(战斗监听器.class, 类型, "反伤监听器不应依赖战斗监听器");
            }
        }
    }
}
