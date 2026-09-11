package mljy.技能实现.奥能法师;

import mljy.战斗服务;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.基础设施层.事件日志上下文;
import mljy.业务层.效果调度服务;
import mljy.业务层.消息.受击修正上下文;
import mljy.业务层.消息服务;
import mljy.业务层.目标选择服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.实体;
import mljy.领域层.属性.属性快照;
import mljy.领域层.效果.效果实例;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("奥能法师技能基础 - FP-04 秘兆消费")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 奥能法师技能基础测试 {

    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 目标选择服务 目标选择服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 秘兆效果处理器 秘兆效果处理器;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 玩家服务 玩家服务;

    private 测试技能 技能;
    private 玩家快照 施法者;
    private 位置 中心;

    @BeforeEach
    void setUp() throws Exception {
        技能 = new 测试技能();
        注入字段("战斗服务", 战斗服务);
        注入字段("目标选择服务", 目标选择服务);
        注入字段("效果调度服务", 效果调度服务);
        注入字段("秘兆效果处理器", 秘兆效果处理器);
        注入字段("消息服务", 消息服务);
        注入字段("翻译服务", 翻译服务);
        注入字段("玩家服务", 玩家服务);
        when(翻译服务.获取(any())).thenReturn("测试技能");
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 0.0,
                0.0, 0.0, 100.0,
                0.0, 0.0, 100.0,
                0.0, 1.5, 100.0,
                0.0, 0.0, 0.0, 0.0, 0.0);
        施法者 = new 玩家快照(UUID.randomUUID(), "测试法师", 1, null, 属性, Map.of(), null);
        中心 = new 位置("world", 0, 64, 0, 0, 0);
    }

    private void 注入字段(String 名称, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(名称);
        字段.setAccessible(true);
        字段.set(技能, 值);
    }

    private void 准备秘兆和两个目标() {
        实体 目标一 = mock(实体.class);
        实体 目标二 = mock(实体.class);
        when(目标一.获取名称()).thenReturn("目标一");
        when(目标二.获取名称()).thenReturn("目标二");
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一, 目标二));
        when(效果调度服务.获取列表(施法者.唯一标识())).thenReturn(
                List.of(new 效果实例("1_9_1", "1_9", 1, System.currentTimeMillis() + 5000)));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(130.0, false, false));
    }

    @Test
    @DisplayName("不消耗秘能的AOE不得应用或移除秘兆")
    void 不消耗秘能的AOE_不消费秘兆() {
        准备秘兆和两个目标();

        技能.施放范围伤害(施法者, 中心, false);

        ArgumentCaptor<伤害上下文> 捕获 = ArgumentCaptor.forClass(伤害上下文.class);
        verify(战斗服务, times(2)).应用伤害(捕获.capture());
        assertEquals(List.of(100.0, 100.0), 捕获.getAllValues().stream().map(伤害上下文::基础数值).toList());
        verify(效果调度服务, never()).移除(eq(施法者.唯一标识()), eq("1_9_1"));
        verify(秘兆效果处理器, never()).停止特效(施法者.唯一标识());
    }

    @Test
    @DisplayName("消耗秘能的AOE每次施法只结算并移除一次秘兆")
    void 消耗秘能的AOE_只消费一次秘兆() {
        准备秘兆和两个目标();

        技能.施放范围伤害(施法者, 中心, true);

        ArgumentCaptor<伤害上下文> 捕获 = ArgumentCaptor.forClass(伤害上下文.class);
        verify(战斗服务, times(2)).应用伤害(捕获.capture());
        assertEquals(List.of(130.0, 130.0), 捕获.getAllValues().stream().map(伤害上下文::基础数值).toList());
        verify(效果调度服务, times(1)).移除(施法者.唯一标识(), "1_9_1");
        verify(秘兆效果处理器, times(1)).停止特效(施法者.唯一标识());
    }

    @Test
    @DisplayName("非玩家目标有真实自定义名称时应使用该名称")
    void 非玩家目标有真实自定义名称_应使用真实名称() {
        LivingEntity 原始实体 = mock(LivingEntity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("血缚恶魔"));
        when(原始实体.getUniqueId()).thenReturn(UUID.randomUUID());
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(50.0, false, false));

        技能.施放单体伤害(施法者, new 实体适配器(原始实体));

        verify(消息服务).发送技能日志(eq(施法者), any(UUID.class),
                eq("战斗日志.技能伤害"), eq("一"), eq("血缚恶魔"), eq("50.0"));
    }

    @Test
    @DisplayName("纯HealthBar目标名应回退实体类型客户端翻译")
    void 纯HealthBar目标名_应回退实体类型客户端翻译() {
        LivingEntity 原始实体 = mock(LivingEntity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("§c[████████████]"));
        when(原始实体.getName()).thenReturn("[████████████]");
        EntityType 实体类型 = mock(EntityType.class);
        when(实体类型.translationKey()).thenReturn("entity.minecraft.zombie");
        when(原始实体.getType()).thenReturn(实体类型);
        when(原始实体.getUniqueId()).thenReturn(UUID.randomUUID());
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(50.0, false, false));

        技能.施放单体伤害(施法者, new 实体适配器(原始实体));

        verify(消息服务).发送技能日志(eq(施法者), any(UUID.class),
                eq("战斗日志.技能伤害"), eq("一"), eq("<lang:entity.minecraft.zombie>"),
                eq("50.0"));
    }

    @Test
    @DisplayName("最终伤害为零时仍清理受击修正并发送同根零值日志")
    void 最终伤害为零_仍清理受击修正并发送同根零值日志() {
        Player 受击者 = mock(Player.class);
        UUID 受击者标识 = UUID.randomUUID();
        when(受击者.getUniqueId()).thenReturn(受击者标识);
        when(受击者.getName()).thenReturn("受击者");
        玩家快照 受击快照 = new 玩家快照(
                受击者标识, "受击者", 1, null, null, Map.of(), null);
        when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(受击快照));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(0.0, false, true));

        UUID 根事件标识 = 事件日志上下文.开始新事件();
        受击修正上下文.记录吸收(
                受击者标识, 根事件标识, 4.0, "skill.奥能法师.奥术护盾.name");
        try {
            事件日志上下文.在事件中执行(
                    根事件标识, () -> 技能.施放单体伤害(施法者, new 实体适配器(受击者)));

            assertTrue(受击修正上下文.获取并清除(受击者标识, 根事件标识).isEmpty());
            verify(消息服务).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.受到伤害带护盾抵挡"),
                    eq("测试法师"), eq("0.0"), eq("4.0"));
            verify(消息服务, never()).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.受到伤害"),
                    anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.吸收"),
                    anyString(), anyString());
        } finally {
            受击修正上下文.获取并清除(受击者标识, 根事件标识);
            事件日志上下文.结束事件(根事件标识);
            事件日志上下文.清除();
        }
    }

    @Test
    @DisplayName("暴击且有护盾吸收时应合并为带护盾抵挡日志（修复盲区：避免护盾抵挡量丢失）")
    void 暴击且有护盾吸收_应发送合并抵挡日志() {
        Player 受击者 = mock(Player.class);
        UUID 受击者标识 = UUID.randomUUID();
        when(受击者.getUniqueId()).thenReturn(受击者标识);
        when(受击者.getName()).thenReturn("受击者");
        玩家快照 受击快照 = new 玩家快照(
                受击者标识, "受击者", 1, null, null, Map.of(), null);
        when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(受击快照));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(50.0, true, false));

        UUID 根事件标识 = 事件日志上下文.开始新事件();
        受击修正上下文.记录吸收(
                受击者标识, 根事件标识, 8.0, "skill.奥能法师.奥术护盾.name");
        try {
            事件日志上下文.在事件中执行(
                    根事件标识,
                    () -> 技能.施放单体伤害并指定暴击(施法者, new 实体适配器(受击者), true));

            assertTrue(受击修正上下文.获取并清除(受击者标识, 根事件标识).isEmpty());
            verify(消息服务).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.受到伤害带护盾抵挡"),
                    eq("测试法师"), eq("50.0"), eq("8.0"));
            verify(消息服务, never()).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.受到伤害暴击"),
                    anyString(), anyString());
            verify(消息服务, never()).发送战斗日志(
                    eq(受击快照), eq(根事件标识), eq("战斗日志.吸收"),
                    anyString(), anyString());
        } finally {
            受击修正上下文.获取并清除(受击者标识, 根事件标识);
            事件日志上下文.结束事件(根事件标识);
            事件日志上下文.清除();
        }
    }

    @Test
    @DisplayName("FP-5: 范围命中带秘能+有暴击+无秘兆 → 范围命中仅资源带暴击日志键")
    void 范围命中带秘能_有暴击_无秘兆_应使用范围命中仅资源带暴击日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, true, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 1, false);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围命中仅资源带暴击"),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-5: 范围命中带秘能+有暴击+有秘兆 → 范围命中仅资源带秘兆带暴击日志键")
    void 范围命中带秘能_有暴击_有秘兆_应使用范围命中仅资源带秘兆带暴击日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, true, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 1, true);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围命中仅资源带秘兆带暴击"),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-5: 范围命中带秘能+无暴击+无秘兆 → 范围命中仅资源日志键（无回归）")
    void 范围命中带秘能_无暴击_无秘兆_应使用范围命中仅资源日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, false, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 1, false);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围命中仅资源"),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-5: 范围命中带秘能+无暴击+有秘兆 → 范围命中仅资源带秘兆日志键（无回归）")
    void 范围命中带秘能_无暴击_有秘兆_应使用范围命中仅资源带秘兆日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, false, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 1, true);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围命中仅资源带秘兆"),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-5: 范围命中无秘能+有暴击 → 范围暴击命中日志键（无回归）")
    void 范围命中无秘能_有暴击_应使用范围暴击命中日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, true, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 0, false);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围暴击命中"),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("FP-5: 范围命中无秘能+无暴击 → 范围命中日志键（无回归）")
    void 范围命中无秘能_无暴击_应使用范围命中日志键() {
        实体 目标一 = mock(实体.class);
        when(目标选择服务.选择敌人(施法者, 中心, 5.0)).thenReturn(List.of(目标一));
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(100.0, false, false));

        技能.施放范围伤害带秘能(施法者, 中心, false, 0, false);

        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.范围命中"),
                anyString(), anyString(), anyString());
    }

    private static class 测试技能 extends 奥能法师技能基础 {
        private boolean 增加秘能返回值 = false;

        void 施放范围伤害(玩家快照 施法者, 位置 中心, boolean 消耗秘能) {
            应用范围伤害并记录日志(
                    施法者, 中心, 5.0, 100.0,
                    5, "skill.奥能法师.测试.name", 0, 消耗秘能);
        }

        void 施放范围伤害带秘能(玩家快照 施法者, 位置 中心, boolean 消耗秘能,
                                int 每目标秘能获取量, boolean 触发秘兆) {
            this.增加秘能返回值 = 触发秘兆;
            应用范围伤害并记录日志(
                    施法者, 中心, 5.0, 100.0,
                    5, "skill.奥能法师.测试.name", 每目标秘能获取量, 消耗秘能);
        }

        @Override
        protected boolean 增加秘能(玩家快照 玩家, double 数量, boolean 发射日志) {
            return 增加秘能返回值;
        }

        void 施放单体伤害(玩家快照 施法者, 实体 目标) {
            应用单体伤害并记录命中(
                    施法者, 目标, 50.0, false,
                    "skill.奥能法师.测试.name", 0);
        }

        void 施放单体伤害并指定暴击(玩家快照 施法者, 实体 目标, boolean 是否暴击) {
            应用单体伤害(施法者, 目标, 50.0, 是否暴击, false);
        }
    }
}
