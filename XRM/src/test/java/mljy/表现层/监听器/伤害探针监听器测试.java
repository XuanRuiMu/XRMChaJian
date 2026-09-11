package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.调试日志器;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("FP-04 伤害探针监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 伤害探针监听器测试 {

    private JavaPlugin 插件mock;
    private 伤害探针监听器 监听器;

    @BeforeEach
    void setUp() {
        插件mock = mock(JavaPlugin.class);
        when(插件mock.getName()).thenReturn("XRM");
        try (var _ = mockConstruction(NamespacedKey.class)) {
            监听器 = new 伤害探针监听器(插件mock);
        }
        调试日志器.初始化(mock(Logger.class), true);
    }

    @AfterEach
    void 清理() {
        调试日志器.初始化(null, false);
    }

    private LivingEntity 创建XRM技能伤害目标() {
        LivingEntity 生物 = mock(LivingEntity.class);
        when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
        when(生物.getName()).thenReturn("测试目标");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(true);
        when(生物.getPersistentDataContainer()).thenReturn(pdc);
        return 生物;
    }

    private LivingEntity 创建普通伤害目标() {
        LivingEntity 生物 = mock(LivingEntity.class);
        when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
        when(生物.getName()).thenReturn("普通目标");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(false);
        when(生物.getPersistentDataContainer()).thenReturn(pdc);
        return 生物;
    }

    private EntityDamageByEntityEvent 创建事件(Entity 目标, Entity 攻击者, double 伤害) {
        EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
        when(事件.getEntity()).thenReturn(目标);
        when(事件.getDamager()).thenReturn(攻击者);
        when(事件.getDamage()).thenReturn(伤害);
        when(事件.getFinalDamage()).thenReturn(伤害);
        when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(事件.isCancelled()).thenReturn(false);
        return 事件;
    }

    @Nested
    @DisplayName("构造器与依赖注入")
    class 构造器测试 {

        @Test
        @DisplayName("构造器应有@Inject注解(防止Guice创建失败)")
        void 构造器_应有Inject注解() throws NoSuchMethodException {
            java.lang.reflect.Constructor<伤害探针监听器> 构造器 =
                    伤害探针监听器.class.getDeclaredConstructor(JavaPlugin.class);
            assertTrue(构造器.isAnnotationPresent(Inject.class),
                    "伤害探针监听器构造器必须有@Inject注解，否则Guice无法创建实例");
        }

        @Test
        @DisplayName("构造器应只依赖JavaPlugin一个参数")
        void 构造器_应只依赖JavaPlugin() throws NoSuchMethodException {
            java.lang.reflect.Constructor<?>[] 构造器列表 = 伤害探针监听器.class.getDeclaredConstructors();
            assertEquals(1, 构造器列表.length, "伤害探针监听器应只有一个构造器");
            assertEquals(1, 构造器列表[0].getParameterCount(),
                    "构造器应只有1个参数(JavaPlugin)");
            assertEquals(JavaPlugin.class, 构造器列表[0].getParameterTypes()[0]);
        }
    }

    @Nested
    @DisplayName("只读探针：不修改事件")
    class 只读探针测试 {

        @Test
        @DisplayName("XRM技能伤害事件-LOWEST优先级不应修改事件")
        void XRM技能伤害_LOWEST_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针最低优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("XRM技能伤害事件-LOW优先级不应修改事件")
        void XRM技能伤害_LOW_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针低优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("XRM技能伤害事件-NORMAL优先级不应修改事件")
        void XRM技能伤害_NORMAL_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针普通优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("XRM技能伤害事件-HIGH优先级不应修改事件")
        void XRM技能伤害_HIGH_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针高优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("XRM技能伤害事件-HIGHEST优先级不应修改事件")
        void XRM技能伤害_HIGHEST_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针最高优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("XRM技能伤害事件-MONITOR优先级不应修改事件")
        void XRM技能伤害_MONITOR_不应修改事件() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            监听器.探针监控优先级(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("非XRM技能伤害事件-不应修改事件且不抛异常")
        void 非XRM技能伤害_不应修改事件() {
            LivingEntity 目标 = 创建普通伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 50.0);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    @DisplayName("边界场景：目标不是LivingEntity")
    class 边界场景测试 {

        @Test
        @DisplayName("目标为非LivingEntity实体-应安全跳过不抛异常")
        void 非LivingEntity目标_应安全跳过() {
            Entity 非生物目标 = mock(Entity.class);
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(非生物目标, 攻击者, 100.0);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("目标PDC为null-应安全跳过不抛异常")
        void PDC为null_应安全跳过() {
            LivingEntity 生物 = mock(LivingEntity.class);
            when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
            when(生物.getName()).thenReturn("无PDC目标");
            when(生物.getPersistentDataContainer()).thenReturn(null);
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(生物, 攻击者, 100.0);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    @DisplayName("6优先级注解完整性")
    class 优先级注解测试 {

        @Test
        @DisplayName("应包含6个@EventHandler注解的方法")
        void 应包含6个EventHandler方法() {
            Method[] 方法列表 = 伤害探针监听器.class.getDeclaredMethods();
            int 事件处理器数量 = 0;
            for (Method 方法 : 方法列表) {
                if (方法.isAnnotationPresent(EventHandler.class)) {
                    事件处理器数量++;
                }
            }
            assertEquals(6, 事件处理器数量, "伤害探针监听器应有6个@EventHandler方法(对应6个事件优先级)");
        }

        @Test
        @DisplayName("6个优先级应分别为LOWEST/LOW/NORMAL/HIGH/HIGHEST/MONITOR")
        void 六个优先级应完整覆盖() {
            Method[] 方法列表 = 伤害探针监听器.class.getDeclaredMethods();
            java.util.Set<EventPriority> 已覆盖优先级 = new java.util.HashSet<>();
            for (Method 方法 : 方法列表) {
                EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
                if (注解 != null) {
                    已覆盖优先级.add(注解.priority());
                }
            }
            assertTrue(已覆盖优先级.contains(EventPriority.LOWEST), "应包含LOWEST优先级");
            assertTrue(已覆盖优先级.contains(EventPriority.LOW), "应包含LOW优先级");
            assertTrue(已覆盖优先级.contains(EventPriority.NORMAL), "应包含NORMAL优先级");
            assertTrue(已覆盖优先级.contains(EventPriority.HIGH), "应包含HIGH优先级");
            assertTrue(已覆盖优先级.contains(EventPriority.HIGHEST), "应包含HIGHEST优先级");
            assertTrue(已覆盖优先级.contains(EventPriority.MONITOR), "应包含MONITOR优先级");
        }

        @Test
        @DisplayName("所有@EventHandler应设置ignoreCancelled=false(监听已取消事件)")
        void 所有EventHandler应监听已取消事件() {
            Method[] 方法列表 = 伤害探针监听器.class.getDeclaredMethods();
            for (Method 方法 : 方法列表) {
                EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
                if (注解 != null) {
                    assertFalse(注解.ignoreCancelled(),
                            "探针应监听所有事件(包括已取消的)，方法=" + 方法.getName() + " 的 ignoreCancelled 应为 false");
                }
            }
        }
    }

    @Nested
    @DisplayName("日志输出安全性")
    class 日志输出测试 {

        @Test
        @DisplayName("XRM技能伤害事件-调试日志启用时应安全执行不抛异常")
        void XRM技能伤害_调试日志启用_应安全执行() {
            调试日志器.初始化(mock(Logger.class), true);
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));
        }

        @Test
        @DisplayName("XRM技能伤害事件-调试日志禁用时应安全执行不抛异常")
        void XRM技能伤害_调试日志禁用_应安全执行() {
            调试日志器.初始化(null, false);
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, 100.0);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));
        }

        @Test
        @DisplayName("伤害数值为NaN-应安全执行不抛异常")
        void 伤害NaN_应安全执行() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, Double.NaN);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));
        }

        @Test
        @DisplayName("伤害数值为Infinity-应安全执行不抛异常")
        void 伤害Infinity_应安全执行() {
            LivingEntity 目标 = 创建XRM技能伤害目标();
            Entity 攻击者 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 攻击者, Double.POSITIVE_INFINITY);

            assertDoesNotThrow(() -> 监听器.探针普通优先级(事件));
        }
    }
}
