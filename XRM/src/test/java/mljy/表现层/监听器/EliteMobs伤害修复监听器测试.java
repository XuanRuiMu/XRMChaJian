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
import org.bukkit.persistence.PersistentDataType;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-03 EliteMobs伤害修复监听器测试
 *
 * 覆盖维度：
 * 1. 构造器与依赖注入
 * 2. EliteMobs归一化修复：PDC含XRM伤害键时强制恢复伤害值
 * 3. 普通伤害事件不修改：PDC无XRM伤害键时不影响事件
 * 4. 边界场景：非LivingEntity、PDC为null、伤害值异常
 * 5. 注解完整性：HIGHEST优先级 + ignoreCancelled=false
 */
@DisplayName("FP-03 EliteMobs伤害修复监听器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliteMobs伤害修复监听器测试 {

    private JavaPlugin 插件mock;
    private EliteMobs伤害修复监听器 监听器;

    @BeforeEach
    void setUp() {
        插件mock = mock(JavaPlugin.class);
        when(插件mock.getName()).thenReturn("XRM");
        try (var _ = mockConstruction(NamespacedKey.class)) {
            监听器 = new EliteMobs伤害修复监听器(插件mock);
        }
        调试日志器.初始化(mock(Logger.class), true);
    }

    @AfterEach
    void 清理() {
        调试日志器.初始化(null, false);
    }

    private LivingEntity 创建XRM技能伤害目标(double 预期伤害) {
        LivingEntity 生物 = mock(LivingEntity.class);
        when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
        when(生物.getName()).thenReturn("XRM目标");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(true);
        when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(预期伤害);
        when(生物.getPersistentDataContainer()).thenReturn(pdc);
        return 生物;
    }

    private LivingEntity 创建普通伤害目标() {
        LivingEntity 生物 = mock(LivingEntity.class);
        when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
        when(生物.getName()).thenReturn("普通目标");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), any())).thenReturn(false);
        when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(null);
        when(生物.getPersistentDataContainer()).thenReturn(pdc);
        return 生物;
    }

    private EntityDamageByEntityEvent 创建事件(Entity 目标, double 伤害, boolean 已取消) {
        EntityDamageByEntityEvent 事件 = mock(EntityDamageByEntityEvent.class);
        Entity 攻击者 = mock(Entity.class);
        when(事件.getEntity()).thenReturn(目标);
        when(事件.getDamager()).thenReturn(攻击者);
        when(事件.getDamage()).thenReturn(伤害);
        when(事件.getFinalDamage()).thenReturn(伤害);
        when(事件.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(事件.isCancelled()).thenReturn(已取消);
        return 事件;
    }

    @Nested
    @DisplayName("构造器与依赖注入")
    class 构造器测试 {

        @Test
        @DisplayName("构造器应有@Inject注解(防止Guice创建失败)")
        void 构造器_应有Inject注解() throws NoSuchMethodException {
            java.lang.reflect.Constructor<EliteMobs伤害修复监听器> 构造器 =
                    EliteMobs伤害修复监听器.class.getDeclaredConstructor(JavaPlugin.class);
            assertTrue(构造器.isAnnotationPresent(Inject.class),
                    "EliteMobs伤害修复监听器构造器必须有@Inject注解，否则Guice无法创建实例");
        }

        @Test
        @DisplayName("构造器应只依赖JavaPlugin一个参数")
        void 构造器_应只依赖JavaPlugin() throws NoSuchMethodException {
            java.lang.reflect.Constructor<?>[] 构造器列表 = EliteMobs伤害修复监听器.class.getDeclaredConstructors();
            assertEquals(1, 构造器列表.length, "应只有一个构造器");
            assertEquals(1, 构造器列表[0].getParameterCount(), "构造器应只有1个参数");
            assertEquals(JavaPlugin.class, 构造器列表[0].getParameterTypes()[0]);
        }
    }

    @Nested
    @DisplayName("EliteMobs归一化修复")
    class 归一化修复测试 {

        @Test
        @DisplayName("EliteMobs将2.0伤害归一化为1.0_应恢复为2.0")
        void 伤害被归一化为1_应恢复为XRM预期值2() {
            LivingEntity 目标 = 创建XRM技能伤害目标(2.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件).setDamage(2.0);
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("EliteMobs将4.0伤害归一化为1.0_应恢复为4.0")
        void 伤害被归一化为1_应恢复为XRM预期值4() {
            LivingEntity 目标 = 创建XRM技能伤害目标(4.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件).setDamage(4.0);
        }

        @Test
        @DisplayName("EliteMobs将8.0伤害归一化为1.0_应恢复为8.0")
        void 伤害被归一化为1_应恢复为XRM预期值8() {
            LivingEntity 目标 = 创建XRM技能伤害目标(8.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件).setDamage(8.0);
        }

        @Test
        @DisplayName("EliteMobs取消事件_应恢复事件为未取消并设置伤害")
        void 事件被取消_应恢复并设置伤害() {
            LivingEntity 目标 = 创建XRM技能伤害目标(5.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, true);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件).setCancelled(false);
            verify(事件).setDamage(5.0);
        }

        @Test
        @DisplayName("伤害未被修改_当前伤害等于预期伤害_不应调用setDamage")
        void 伤害未被修改_不应调用setDamage() {
            LivingEntity 目标 = 创建XRM技能伤害目标(3.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 3.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    @DisplayName("普通伤害事件不修改")
    class 普通伤害测试 {

        @Test
        @DisplayName("PDC无XRM伤害键_不应修改事件")
        void 无XRM伤害键_不应修改事件() {
            LivingEntity 目标 = 创建普通伤害目标();
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 10.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    @DisplayName("边界场景")
    class 边界场景测试 {

        @Test
        @DisplayName("目标为非LivingEntity_应安全跳过")
        void 非LivingEntity目标_应安全跳过() {
            Entity 非生物目标 = mock(Entity.class);
            EntityDamageByEntityEvent 事件 = 创建事件(非生物目标, 1.0, false);

            assertDoesNotThrow(() -> 监听器.修复EliteMobs伤害归一化(事件));

            verify(事件, never()).setDamage(anyDouble());
            verify(事件, never()).setCancelled(anyBoolean());
        }

        @Test
        @DisplayName("PDC为null_应安全跳过")
        void PDC为null_应安全跳过() {
            LivingEntity 生物 = mock(LivingEntity.class);
            when(生物.getUniqueId()).thenReturn(UUID.randomUUID());
            when(生物.getPersistentDataContainer()).thenReturn(null);
            EntityDamageByEntityEvent 事件 = 创建事件(生物, 1.0, false);

            assertDoesNotThrow(() -> 监听器.修复EliteMobs伤害归一化(事件));

            verify(事件, never()).setDamage(anyDouble());
        }

        @Test
        @DisplayName("预期伤害为0_应跳过不修改")
        void 预期伤害为0_应跳过() {
            LivingEntity 目标 = 创建XRM技能伤害目标(0.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
        }

        @Test
        @DisplayName("预期伤害为负数_应跳过不修改")
        void 预期伤害为负数_应跳过() {
            LivingEntity 目标 = 创建XRM技能伤害目标(-5.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
        }

        @Test
        @DisplayName("预期伤害为NaN_应跳过不修改")
        void 预期伤害为NaN_应跳过() {
            LivingEntity 目标 = 创建XRM技能伤害目标(Double.NaN);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
        }

        @Test
        @DisplayName("预期伤害为Infinity_应跳过不修改")
        void 预期伤害为Infinity_应跳过() {
            LivingEntity 目标 = 创建XRM技能伤害目标(Double.POSITIVE_INFINITY);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            监听器.修复EliteMobs伤害归一化(事件);

            verify(事件, never()).setDamage(anyDouble());
        }
    }

    @Nested
    @DisplayName("注解完整性")
    class 注解完整性测试 {

        @Test
        @DisplayName("应有1个@EventHandler方法")
        void 应有1个EventHandler方法() {
            Method[] 方法列表 = EliteMobs伤害修复监听器.class.getDeclaredMethods();
            int 事件处理器数量 = 0;
            for (Method 方法 : 方法列表) {
                if (方法.isAnnotationPresent(EventHandler.class)) {
                    事件处理器数量++;
                }
            }
            assertEquals(1, 事件处理器数量, "应有1个@EventHandler方法");
        }

        @Test
        @DisplayName("EventHandler优先级应为HIGHEST")
        void 优先级应为HIGHEST() {
            Method[] 方法列表 = EliteMobs伤害修复监听器.class.getDeclaredMethods();
            for (Method 方法 : 方法列表) {
                EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
                if (注解 != null) {
                    assertEquals(EventPriority.HIGHEST, 注解.priority(),
                            "EventHandler优先级应为HIGHEST，确保在EliteMobs归一化之后覆盖");
                }
            }
        }

        @Test
        @DisplayName("ignoreCancelled应为false(监听已取消事件)")
        void ignoreCancelled应为false() {
            Method[] 方法列表 = EliteMobs伤害修复监听器.class.getDeclaredMethods();
            for (Method 方法 : 方法列表) {
                EventHandler 注解 = 方法.getAnnotation(EventHandler.class);
                if (注解 != null) {
                    assertFalse(注解.ignoreCancelled(),
                            "ignoreCancelled应为false，监听已取消事件以便恢复XRM技能伤害");
                }
            }
        }
    }

    @Nested
    @DisplayName("日志输出安全性")
    class 日志输出测试 {

        @Test
        @DisplayName("调试日志启用时_修复操作应安全执行不抛异常")
        void 调试日志启用_应安全执行() {
            调试日志器.初始化(mock(Logger.class), true);
            LivingEntity 目标 = 创建XRM技能伤害目标(2.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            assertDoesNotThrow(() -> 监听器.修复EliteMobs伤害归一化(事件));
        }

        @Test
        @DisplayName("调试日志禁用时_修复操作应安全执行不抛异常")
        void 调试日志禁用_应安全执行() {
            调试日志器.初始化(null, false);
            LivingEntity 目标 = 创建XRM技能伤害目标(2.0);
            EntityDamageByEntityEvent 事件 = 创建事件(目标, 1.0, false);

            assertDoesNotThrow(() -> 监听器.修复EliteMobs伤害归一化(事件));
            verify(事件).setDamage(2.0);
        }
    }
}
