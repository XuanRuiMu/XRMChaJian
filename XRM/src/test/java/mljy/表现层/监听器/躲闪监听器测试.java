package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.业务层.消息服务;
import mljy.业务层.消息.伤害日志格式化服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("躲闪监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 躲闪监听器测试 {

    private static final String 躲闪提示键 = "战斗日志.躲闪";

    @Mock
    private 玩家服务 玩家服务mock;
    @Mock
    private 属性计算服务 属性计算服务mock;
    @Mock
    private 消息服务 消息服务mock;
    @Mock
    private 伤害日志格式化服务 伤害日志格式化服务mock;
    @Mock
    private JavaPlugin 插件mock;

    private 躲闪监听器 监听器;
    private UUID 受击者标识;

    @BeforeEach
    void setUp() {
        try (var _ = mockConstruction(NamespacedKey.class)) {
            监听器 = new 躲闪监听器(玩家服务mock, 属性计算服务mock, 消息服务mock,
                    伤害日志格式化服务mock, 插件mock);
        }
        受击者标识 = UUID.randomUUID();
    }

    private Player 创建玩家(UUID 标识, String 名称) {
        Player 玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(标识);
        when(玩家.getName()).thenReturn(名称);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(玩家.getPersistentDataContainer()).thenReturn(pdc);
        return 玩家;
    }

    private EntityDamageByEntityEvent 创建事件(Player 受击者, Player 攻击者) {
        EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
        when(事件.getEntity()).thenReturn(受击者);
        when(事件.getDamager()).thenReturn(攻击者);
        return 事件;
    }

    private Player 创建攻击者(String 名称) {
        Player 攻击者 = mock(Player.class);
        when(攻击者.getName()).thenReturn(名称);
        return 攻击者;
    }

    private 玩家快照 创建快照(UUID 标识) {
        return new 玩家快照(标识, "受击者", 1, null, 创建属性快照(10), Map.of(), null);
    }

    private 属性快照 创建属性快照(double 躲闪) {
        return 属性快照.创建(100, 1.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 躲闪, 0, 0);
    }

    @Nested
    @DisplayName("躲闪成功 - 100%几率")
    class 躲闪成功 {

        @Test
        @DisplayName("躲闪几率为100%时应取消事件并发送战斗日志")
        void 百分之百躲闪_应取消事件并发送战斗日志() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            Player 攻击者 = 创建攻击者("怪物");
            玩家快照 快照 = 创建快照(受击者标识);
            EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 创建属性快照(100);
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(1.0);
            when(伤害日志格式化服务mock.获取实体来源名(攻击者)).thenReturn("怪物");

            监听器.实体被攻击(事件);

            verify(事件).setCancelled(true);
            verify(消息服务mock).发送战斗日志(
                    eq(快照), any(UUID.class), eq(躲闪提示键), eq("怪物"));
        }

        @Test
        @DisplayName("躲闪几率为100%时多次判定应全部躲闪")
        void 百分之百躲闪_多次判定应全部躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            Player 攻击者 = 创建攻击者("怪物");
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建属性快照(100);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(1.0);
            when(伤害日志格式化服务mock.获取实体来源名(any())).thenReturn("怪物");

            for (int i = 0; i < 100; i++) {
                EntityDamageByEntityEvent 事件 = 创建事件(受击者, 攻击者);
                监听器.实体被攻击(事件);
                verify(事件, atLeast(1)).setCancelled(true);
            }
            verify(消息服务mock, times(100)).发送战斗日志(
                    eq(快照), any(UUID.class), eq(躲闪提示键), eq("怪物"));
        }
    }

    @Nested
    @DisplayName("躲闪失败 - 0%几率")
    class 躲闪失败 {

        @Test
        @DisplayName("躲闪几率为0%时应不取消事件且不发送消息")
        void 零躲闪_不应取消事件() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            玩家快照 快照 = 创建快照(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 创建属性快照(0);
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(0.0);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }

        @Test
        @DisplayName("躲闪几率为0%时多次判定应全部不躲闪")
        void 零躲闪_多次判定应全部不躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            玩家快照 快照 = 创建快照(受击者标识);
            属性快照 属性 = 创建属性快照(0);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(0.0);

            for (int i = 0; i < 100; i++) {
                EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
                when(事件.getEntity()).thenReturn(受击者);
                监听器.实体被攻击(事件);
                verify(事件, never()).setCancelled(true);
            }
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }
    }

    @Nested
    @DisplayName("边界值 - 跳过判定场景")
    class 边界值 {

        @Test
        @DisplayName("XRM技能伤害应跳过躲闪判定")
        void XRM技能伤害_应跳过躲闪() {
            Player 受击者 = mock(Player.class);
            when(受击者.getUniqueId()).thenReturn(受击者标识);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(受击者.getPersistentDataContainer()).thenReturn(pdc);
            when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(true);

            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(玩家服务mock, never()).获取快照(any());
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }

        @Test
        @DisplayName("非玩家目标应跳过躲闪判定")
        void 非玩家目标_应跳过躲闪() {
            LivingEntity 怪物 = mock(LivingEntity.class);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(怪物);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(玩家服务mock, never()).获取快照(any());
        }

        @Test
        @DisplayName("玩家快照不存在时应跳过躲闪判定")
        void 玩家快照不存在_应跳过躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.empty());

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }

        @Test
        @DisplayName("NaN躲闪几率应跳过躲闪判定")
        void NaN躲闪几率_应跳过躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            玩家快照 快照 = 创建快照(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 创建属性快照(10);
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(Double.NaN);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }

        @Test
        @DisplayName("Infinity躲闪几率应跳过躲闪判定")
        void Infinity躲闪几率_应跳过躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            玩家快照 快照 = 创建快照(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 创建属性快照(10);
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(Double.POSITIVE_INFINITY);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }

        @Test
        @DisplayName("负数躲闪几率应跳过躲闪判定")
        void 负数躲闪几率_应跳过躲闪() {
            Player 受击者 = 创建玩家(受击者标识, "玄锐暮");
            玩家快照 快照 = 创建快照(受击者标识);
            EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务mock.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 创建属性快照(10);
            when(属性计算服务mock.计算(快照)).thenReturn(属性);
            when(属性计算服务mock.计算派生值(eq("躲闪几率"), any())).thenReturn(-0.5);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(消息服务mock, never()).发送战斗日志(
                    any(), any(UUID.class), any(String.class), any(), any());
        }
    }

    @Nested
    @DisplayName("Guice依赖注入回归")
    class Guice依赖注入回归 {

        @Test
        @DisplayName("构造器应有@Inject注解(防止XRM启动失败回归)")
        void 构造器_应有Inject注解() throws NoSuchMethodException {
            java.lang.reflect.Constructor<躲闪监听器> 构造器 = 躲闪监听器.class.getDeclaredConstructor(
                    玩家服务.class, 属性计算服务.class, 消息服务.class,
                    伤害日志格式化服务.class, JavaPlugin.class);
            assertTrue(构造器.isAnnotationPresent(Inject.class),
                    "躲闪监听器构造器必须有@Inject注解，否则Guice无法创建实例导致XRM启动失败");
        }
    }
}
