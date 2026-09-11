package mljy.技能实现.奥能法师;

import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.资源变更服务;
import mljy.战斗服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.实体;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 混沌束缚 FP-3 日志合并回归测试。
 *
 * 根因：原实现中，应用单体伤害并记录命中 与 应用范围伤害并记录日志 均通过
 * 事件日志上下文.获取当前事件标识或空() 读取 ThreadLocal 取事件标识。
 * 但 战斗服务.应用伤害 会触发 Bukkit EntityDamageByEntityEvent，
 * 战斗监听器处理该事件时调用 事件日志上下文.获取或创建事件标识(事件)，
 * 该方法在第75行 当前事件标识.set(事件标识) 强制覆盖 ThreadLocal 为新的独立根事件。
 * 导致主目标伤害与 AOE 伤害使用了不同的事件标识，违反"同一事件同一时间必须合并"原则。
 *
 * 修复：在 奥能法师技能基础 添加接受事件标识参数的重载方法，
 * 显式传入根事件标识，绕过 ThreadLocal 依赖。混沌束缚.命中 与 触发二次伤害
 * 改为调用新重载，确保主目标伤害与 AOE 伤害共享同一根事件标识。
 */
@DisplayName("混沌束缚 - FP-3 日志合并")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 混沌束缚测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 目标选择服务 目标选择服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 秘兆效果处理器 秘兆效果处理器;
    @Mock
    private 实体 主目标;
    @Mock
    private 实体 aoe目标1;
    @Mock
    private 实体 aoe目标2;

    /**
     * 测试用技能子类：暴露 protected 重载方法供测试直接调用，
     * 避免触发 BukkitRunnable/调度器等复杂依赖。
     */
    private static class 测试技能 extends 混沌束缚 {
        public 伤害结果 调用单体伤害并记录命中(玩家快照 施法者, 实体 目标, double 基础伤害,
                                        boolean 是否暴击, String 技能名翻译键, int 资源获取量,
                                        UUID 事件标识) {
            return super.应用单体伤害并记录命中(施法者, 目标, 基础伤害, 是否暴击,
                    技能名翻译键, 资源获取量, 事件标识);
        }

        public boolean 调用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                            int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量,
                                            boolean 消耗秘兆, UUID 事件标识) {
            return super.应用范围伤害并记录日志(施法者, 中心, 半径, 基础伤害,
                    衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识);
        }

        public boolean 调用范围伤害并记录日志带自定义键(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                                    int 衰减阈值, String 技能名翻译键, int 每目标秘能获取量,
                                                    boolean 消耗秘兆, UUID 事件标识, String 自定义AOE日志键) {
            return super.应用范围伤害并记录日志(施法者, 中心, 半径, 基础伤害,
                    衰减阈值, 技能名翻译键, 每目标秘能获取量, 消耗秘兆, 事件标识, 自定义AOE日志键);
        }
    }

    private 测试技能 技能;
    private UUID 施法者标识;
    private 玩家快照 施法者快照;
    private 位置 中心位置;

    @BeforeEach
    void setUp() throws Exception {
        技能 = new 测试技能();
        施法者标识 = UUID.randomUUID();

        注入父类字段(技能, "插件", 插件);
        注入父类字段(技能, "战斗服务", 战斗服务);
        注入父类字段(技能, "目标选择服务", 目标选择服务);
        注入父类字段(技能, "消息服务", 消息服务);
        注入父类字段(技能, "效果调度服务", 效果调度服务);
        注入父类字段(技能, "资源变更服务", 资源变更服务);
        注入父类字段(技能, "翻译服务", 翻译服务);
        注入父类字段(技能, "玩家服务", 玩家服务);
        注入父类字段(技能, "秘兆效果处理器", 秘兆效果处理器);

        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        中心位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        施法者快照 = new 玩家快照(
                施法者标识, "测试法师", 1, 中心位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);

        when(主目标.获取名称()).thenReturn("主目标");
        when(aoe目标1.获取名称()).thenReturn("AOE目标1");
        when(aoe目标2.获取名称()).thenReturn("AOE目标2");
        when(翻译服务.获取(anyString())).thenReturn("混沌束缚");
        when(效果调度服务.获取列表(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        事件日志上下文.清除();
    }

    private void 注入父类字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    /**
     * 模拟 战斗服务.应用伤害 触发 Bukkit EntityDamageByEntityEvent 时，
     * 战斗监听器会调用 事件日志上下文.获取或创建事件标识(事件) 覆盖 ThreadLocal。
     * 这里用 mock 的 Answer 在每次应用伤害时主动覆盖 ThreadLocal 为一个随机新事件，
     * 模拟生产环境中的 ThreadLocal 污染场景。
     */
    private void 模拟战斗服务触发ThreadLocal污染() {
        when(战斗服务.应用伤害(any())).thenAnswer(invocation -> {
            // 模拟战斗监听器处理 EntityDamageByEntityEvent 时覆盖 ThreadLocal
            Object 事件对象 = new Object();
            事件日志上下文.获取或创建事件标识(事件对象);
            return new 伤害结果(10.0, false, false);
        });
    }

    @Test
    @DisplayName("主目标伤害使用显式事件标识，不受 ThreadLocal 污染影响")
    void 主目标伤害_使用显式事件标识() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        模拟战斗服务触发ThreadLocal污染();

        伤害结果 结果 = 技能.调用单体伤害并记录命中(
                施法者快照, 主目标, 100.0, false,
                "skill.奥能法师.混沌束缚.name", 2, 根事件标识);

        assertNotNull(结果, "应用单体伤害应返回非空结果");
        ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
        verify(消息服务, atLeastOnce()).发送技能日志(
                eq(施法者快照), 事件标识捕获.capture(), anyString(), any(Object[].class));

        assertEquals(根事件标识, 事件标识捕获.getValue(),
                "主目标伤害日志应使用显式传入的根事件标识，而非被 ThreadLocal 污染的事件标识");
    }

    @Test
    @DisplayName("范围伤害使用显式事件标识，不受 ThreadLocal 污染影响")
    void 范围伤害_使用显式事件标识() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        模拟战斗服务触发ThreadLocal污染();
        when(目标选择服务.选择敌人(any(), any(), anyDouble()))
                .thenReturn(List.of(aoe目标1, aoe目标2));

        boolean 成功 = 技能.调用范围伤害并记录日志(
                施法者快照, 中心位置, 3.0, 50.0, 3,
                "skill.奥能法师.混沌束缚.name", 0, false, 根事件标识);

        assertEquals(true, 成功, "范围伤害应成功");
        ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
        verify(消息服务, atLeastOnce()).发送技能日志(
                eq(施法者快照), 事件标识捕获.capture(), anyString(), any(Object[].class));

        assertEquals(根事件标识, 事件标识捕获.getValue(),
                "范围伤害日志应使用显式传入的根事件标识，而非被 ThreadLocal 污染的事件标识");
    }

    @Test
    @DisplayName("主目标伤害与 AOE 伤害使用相同事件标识，确保日志合并为一条")
    void 主目标与AOE_使用相同事件标识_日志合并() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        模拟战斗服务触发ThreadLocal污染();
        when(目标选择服务.选择敌人(any(), any(), anyDouble()))
                .thenReturn(List.of(aoe目标1, aoe目标2));

        // 模拟混沌束缚.命中 方法的执行顺序：先主目标伤害，后范围伤害
        // 两步均在 事件日志上下文.在事件中执行(根事件标识, ...) 上下文中执行，
        // 但战斗服务会污染 ThreadLocal，模拟生产环境中 Event 触发链覆盖 ThreadLocal 的场景
        事件日志上下文.在事件中执行(根事件标识, () -> {
            技能.调用单体伤害并记录命中(
                    施法者快照, 主目标, 100.0, false,
                    "skill.奥能法师.混沌束缚.name", 2, 根事件标识);
            技能.调用范围伤害并记录日志(
                    施法者快照, 中心位置, 3.0, 30.0, 3,
                    "skill.奥能法师.混沌束缚.name", 0, false, 根事件标识);
        });

        ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
        verify(消息服务, atLeastOnce()).发送技能日志(
                eq(施法者快照), 事件标识捕获.capture(), anyString(), any(Object[].class));

        // 验证所有发送技能日志调用使用的事件标识都是同一个根事件标识
        // 这意味着主目标伤害和 AOE 伤害的日志会被合并为一条（同一事件同一时间）
        for (UUID 捕获的标识 : 事件标识捕获.getAllValues()) {
            assertEquals(根事件标识, 捕获的标识,
                    "主目标伤害与 AOE 伤害必须使用相同的根事件标识以确保日志合并为一条");
        }
        // 至少应有 2 次发送技能日志调用：主目标伤害日志 + AOE 范围命中日志
        assertEquals(true, 事件标识捕获.getAllValues().size() >= 2,
                "主目标伤害和 AOE 伤害应分别触发发送技能日志调用，实际调用次数：" +
                        事件标识捕获.getAllValues().size());
    }

    @Test
    @DisplayName("ThreadLocal 污染场景下显式事件标识优先于 ThreadLocal 当前事件")
    void ThreadLocal污染_显式事件标识优先() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        UUID 污染事件标识 = 事件日志上下文.开始新事件();
        // 设置 ThreadLocal 为污染事件，模拟战斗监听器覆盖 ThreadLocal 后的场景
        事件日志上下文.在事件中执行(污染事件标识, () -> {
            模拟战斗服务触发ThreadLocal污染();

            伤害结果 结果 = 技能.调用单体伤害并记录命中(
                    施法者快照, 主目标, 100.0, false,
                    "skill.奥能法师.混沌束缚.name", 2, 根事件标识);

            assertNotNull(结果, "应用单体伤害应返回非空结果");
            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务, atLeastOnce()).发送技能日志(
                    eq(施法者快照), 事件标识捕获.capture(), anyString(), any(Object[].class));

            assertEquals(根事件标识, 事件标识捕获.getValue(),
                    "ThreadLocal 当前事件为 " + 污染事件标识 + " 时，日志应使用显式传入的根事件标识 " +
                            根事件标识 + " 而非 ThreadLocal 中的污染事件");
        });
    }

    @Test
    @DisplayName("FP-4: 调用带自定义AOE日志键的重载时，使用自定义键而非默认范围命中键")
    void 调用带自定义AOE日志键_应使用自定义键() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(10.0, false, false));
        when(目标选择服务.选择敌人(any(), any(), anyDouble()))
                .thenReturn(List.of(aoe目标1, aoe目标2));

        boolean 成功 = 技能.调用范围伤害并记录日志带自定义键(
                施法者快照, 中心位置, 3.0, 50.0, 3,
                "skill.奥能法师.混沌束缚.name", 0, false, 根事件标识,
                "技能日志.混沌束缚AOE范围命中");

        assertEquals(true, 成功, "范围伤害应成功");
        verify(消息服务).发送技能日志(
                eq(施法者快照), eq(根事件标识),
                eq("技能日志.混沌束缚AOE范围命中"),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-4: 不传自定义AOE日志键时，使用默认范围命中键（无回归）")
    void 不传自定义AOE日志键_应使用默认范围命中键() {
        UUID 根事件标识 = 事件日志上下文.开始新事件();
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(10.0, false, false));
        when(目标选择服务.选择敌人(any(), any(), anyDouble()))
                .thenReturn(List.of(aoe目标1, aoe目标2));

        boolean 成功 = 技能.调用范围伤害并记录日志(
                施法者快照, 中心位置, 3.0, 50.0, 3,
                "skill.奥能法师.混沌束缚.name", 0, false, 根事件标识);

        assertEquals(true, 成功, "范围伤害应成功");
        verify(消息服务).发送技能日志(
                eq(施法者快照), eq(根事件标识),
                eq("技能日志.范围命中"),
                anyString(), anyString(), anyString());
    }
}
