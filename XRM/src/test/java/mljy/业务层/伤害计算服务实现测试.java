package mljy.业务层;

import com.google.inject.Injector;
import mljy.业务层.消息.受击修正上下文;
import mljy.基础设施层.事件日志上下文;
import mljy.基础设施层.内存效果服务;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害管线阶段;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.效果.效果实例;
import mljy.领域层.效果.效果定义;
import mljy.领域层.玩家.玩家快照;
import mljy.技能实现.奥能法师.奥术护盾效果处理器;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("伤害计算服务实现")
class 伤害计算服务实现测试 {

    @Mock
    private 属性计算服务 属性计算服务mock;
    @Mock
    private 效果调度服务 效果调度服务mock;

    private 奥术护盾效果处理器 护盾处理器;
    private 伤害计算服务实现 服务;

    private UUID 玩家标识;
    private 玩家快照 施法者;
    private 玩家快照 目标玩家;

    @BeforeEach
    void setUp() {
        护盾处理器 = new 奥术护盾效果处理器(效果调度服务mock);
        服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock, 护盾处理器);
        玩家标识 = UUID.randomUUID();
        属性快照 属性 = 创建属性快照();
        施法者 = new 玩家快照(玩家标识, "施法者", 1, null, 属性, java.util.Map.of(), null);
        目标玩家 = new 玩家快照(UUID.randomUUID(), "目标", 1, null, 属性, java.util.Map.of(), null);

        // 设置默认mock行为：属性计算服务返回零值属性快照
        when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
        when(属性计算服务mock.应用通用递减(anyDouble())).thenReturn(0.0);
        when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
        when(效果调度服务mock.获取列表(any())).thenReturn(List.of());
    }

    @Nested
    @DisplayName("阶段1-基础伤害确定")
    class 阶段1基础伤害确定 {

        @Test
        @DisplayName("基础数值应直接传递")
        void 基础数值_应直接传递() {
            伤害上下文 上下文 = new 伤害上下文(施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.最终数值() > 0);
        }

        @Test
        @DisplayName("FP-X7: 零基础数值应保证最小伤害值（技能应可造成伤害）")
        void 零基础数值_应保证最小伤害值() {
            伤害上下文 上下文 = new 伤害上下文(施法者, 目标玩家, 0, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(1.0, 结果.最终数值(), "基础伤害为0的技能应保证最小伤害值1.0");
        }

        @Test
        @DisplayName("FP-X7: 技能释放基础伤害为0时应造成伤害大于0")
        void 技能基础伤害为零_应造成伤害() {
            伤害上下文 上下文 = new 伤害上下文(施法者, 目标玩家, 0, true, false, 1.5, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.最终数值() > 0, "技能释放应造成伤害大于0，实际: " + 结果.最终数值());
            assertTrue(结果.最终数值() >= 1.0, "技能应至少造成最小伤害值1.0，实际: " + 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("阶段3-精通修饰")
    class 阶段3精通修饰 {

        @Test
        @DisplayName("精通修饰器应增加伤害")
        void 精通修饰_应增加伤害() {
            修饰器 精通增伤 = new 修饰器("精通_伤害百分比", "精通1", 修饰方式.相加, 20, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(精通增伤));

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1 + 20*0.01) = 120
            assertEquals(120, 结果.最终数值());
        }

        @Test
        @DisplayName("多个精通修饰器应相加")
        void 多个精通_应相加() {
            修饰器 精通1 = new 修饰器("精通_伤害百分比", "p1", 修饰方式.相加, 20, 0);
            修饰器 精通2 = new 修饰器("精通_伤害百分比", "p2", 修饰方式.相加, 30, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(精通1, 精通2));

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1 + 50*0.01) = 150
            assertEquals(150, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("阶段4-暴击判定")
    class 阶段4暴击判定 {

        @Test
        @DisplayName("上下文指定暴击应必定暴击")
        void 指定暴击_应必定暴击() {
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, true, true, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.是否暴击());
        }

        @Test
        @DisplayName("暴击应应用暴击伤害倍率")
        void 暴击_应应用暴击伤害倍率() {
            // 暴击倍率2.0，施法者暴击伤害0（mock返回0）
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.应用通用递减(anyDouble())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, true, true, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * 2.0 = 200
            assertEquals(200, 结果.最终数值());
            assertTrue(结果.是否暴击());
        }

        @Test
        @DisplayName("额外暴击伤害修饰器应增加暴击伤害")
        void 额外暴击伤害_应增加暴击伤害() {
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.应用通用递减(anyDouble())).thenReturn(0.0);

            修饰器 额外暴击伤害 = new 修饰器("伤害_暴击伤害额外", "crit_dmg", 修饰方式.相加, 50, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, true, true, 2.0, List.of(额外暴击伤害));

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (2.0 + 50*0.01) = 100 * 2.5 = 250
            assertEquals(250, 结果.最终数值());
        }

        @Test
        @DisplayName("无施法者时不暴击")
        void 无施法者_不暴击() {
            伤害上下文 上下文 = new 伤害上下文(
                    null, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertFalse(结果.是否暴击());
        }
    }

    @Nested
    @DisplayName("新版法术暴击属性驱动验证")
    class 新版法术暴击属性驱动 {

        @Test
        @DisplayName("法术暴击几率100应必定触发暴击（非上下文强制）")
        void 法术暴击几率100_应必定暴击() {
            属性快照 高暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    100, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(高暴击属性);
            when(属性计算服务mock.应用通用递减(anyDouble())).thenAnswer(inv -> inv.getArgument(0));

            for (int i = 0; i < 50; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, true, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                assertTrue(结果.是否暴击(), "法术暴击几率100应每次都暴击，第" + i + "次未暴击");
            }
        }

        @Test
        @DisplayName("法术暴击几率0应不触发暴击（非上下文强制）")
        void 法术暴击几率0_应不暴击() {
            属性快照 零暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    0, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(零暴击属性);
            when(属性计算服务mock.应用通用递减(anyDouble())).thenAnswer(inv -> inv.getArgument(0));

            for (int i = 0; i < 50; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, false, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                assertFalse(结果.是否暴击(), "法术暴击几率0不应暴击，第" + i + "次暴击了");
            }
        }

        @Test
        @DisplayName("法术暴击伤害应参与暴击伤害计算")
        void 法术暴击伤害_应参与暴击伤害计算() {
            属性快照 暴击伤害属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    0, 50, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(暴击伤害属性);
            when(属性计算服务mock.应用通用递减(anyDouble())).thenAnswer(inv -> inv.getArgument(0));

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, true, true, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.是否暴击());
            assertEquals(250, 结果.最终数值(),
                    "100*(2.0+50*0.01)=250，法术暴击伤害应参与计算");
        }
    }

    @Nested
    @DisplayName("FP-1: 暴击几率不递减")
    class FP1_暴击几率不递减 {

        @Test
        @DisplayName("暴击几率100时每次必暴击")
        void 暴击几率100时每次必暴击() {
            属性快照 满暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    100, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(满暴击属性);

            for (int i = 0; i < 100; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, true, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                assertTrue(结果.是否暴击(), "暴击几率100应每次都暴击，第" + i + "次未暴击");
            }
        }

        @Test
        @DisplayName("暴击几率0时永不暴击")
        void 暴击几率0时永不暴击() {
            属性快照 零暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    0, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(零暴击属性);

            for (int i = 0; i < 100; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, true, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                assertFalse(结果.是否暴击(), "暴击几率0不应暴击，第" + i + "次暴击了");
            }
        }

        @Test
        @DisplayName("暴击几率150时每次必暴击")
        void 暴击几率150时每次必暴击() {
            属性快照 超满暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    150, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(超满暴击属性);

            for (int i = 0; i < 100; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, true, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                assertTrue(结果.是否暴击(), "暴击几率150应每次都暴击，第" + i + "次未暴击");
            }
        }

        @Test
        @DisplayName("暴击几率50时统计性验证约50%暴击")
        void 暴击几率50时统计性验证() {
            属性快照 半暴击属性 = 属性快照.创建(
                    1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                    50, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务mock.计算(any())).thenReturn(半暴击属性);

            int 暴击次数 = 0;
            int 总次数 = 1000;
            for (int i = 0; i < 总次数; i++) {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, true, false, 2.0, List.of());
                伤害结果 结果 = 服务.计算(上下文);
                if (结果.是否暴击()) {
                    暴击次数++;
                }
            }
            double 实际暴击率 = (double) 暴击次数 / 总次数;
            assertTrue(实际暴击率 >= 0.45 && 实际暴击率 <= 0.55,
                    "暴击几率50时1000次抽样暴击率应在[45%, 55%]区间，实际: " + 实际暴击率);
        }
    }

    @Nested
    @DisplayName("阶段5-全局伤害修饰")
    class 阶段5全局伤害修饰 {

        @Test
        @DisplayName("全局增伤修饰器应增加伤害")
        void 全局增伤_应增加伤害() {
            修饰器 全局增伤 = new 修饰器("伤害_全伤害百分比", "global1", 修饰方式.相加, 30, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(全局增伤));

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1 + 30*0.01) = 130
            assertEquals(130, 结果.最终数值());
        }

        @Test
        @DisplayName("全能增伤应叠加到全局增伤")
        void 全能增伤_应叠加() {
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            // 全能增伤比例返回0.1（即10%），除以0.01得10
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.1);
            when(属性计算服务mock.计算派生值(eq("全能减伤比例"), any())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1 + 10*0.01) = 110
            assertEquals(110, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("阶段6-目标减伤")
    class 阶段6目标减伤 {

        @Test
        @DisplayName("全局减伤应减少伤害")
        void 全局减伤_应减少伤害() {
            修饰器 全局减伤 = new 修饰器("减伤_全局百分比", "global", 修饰方式.相加, 30, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(全局减伤));

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1-0.3) = 70
            assertEquals(70, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("阶段7-护盾吸收")
    class 阶段7护盾吸收 {

        @Test
        @DisplayName("无护盾应不吸收")
        void 无护盾_应不吸收() {
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertFalse(结果.被吸收());
            assertEquals(100, 结果.最终数值());
        }

        @Test
        @DisplayName("护盾值大于伤害应吸收全部且不移除")
        void 护盾值大于伤害_应吸收全部且不移除() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 150, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.被吸收());
            // 100 - 100 = 0
            assertEquals(0, 结果.最终数值());
            assertEquals(150, 护盾.获取层数());
            verify(效果调度服务mock).设置层数(目标玩家.唯一标识(), "1_3_1", 50);
            verify(效果调度服务mock, never()).移除(any(), any());
        }

        @Test
        @DisplayName("完整链中两次伤害应各扣盾一次并在耗尽时移除")
        void 完整链_两次伤害_各扣盾一次并耗尽移除() {
            内存效果服务 效果存储 = new 内存效果服务();
            效果注册服务 注册服务 = mock(效果注册服务.class);
            Injector 注入器 = mock(Injector.class);
            效果定义 护盾定义 = new 效果定义(
                    "1_3_1", "奥术护盾", "", "", "", 20000L,
                    效果定义.无层数上限, false, false);
            when(注册服务.获取定义("1_3_1")).thenReturn(Optional.of(护盾定义));
            效果调度服务实现 真实效果调度 = new 效果调度服务实现(效果存储, 注册服务, 注入器);
            伤害计算服务实现 完整链服务 = new 伤害计算服务实现(属性计算服务mock, 真实效果调度);
            真实效果调度.添加(目标玩家.唯一标识(),
                    new 效果实例("1_3_1", "shield_skill", 10, -1L));

            伤害结果 第一次 = 完整链服务.计算(new 伤害上下文(
                    施法者, 目标玩家, 6, false, false, 2.0, List.of()));
            效果实例 第一次后护盾 = 真实效果调度.获取(
                    目标玩家.唯一标识(), "1_3_1").orElseThrow();

            assertEquals(0, 第一次.最终数值());
            assertEquals(4, 第一次后护盾.获取层数());

            伤害结果 第二次 = 完整链服务.计算(new 伤害上下文(
                    施法者, 目标玩家, 10, false, false, 2.0, List.of()));

            assertEquals(6, 第二次.最终数值());
            assertFalse(真实效果调度.拥有(目标玩家.唯一标识(), "1_3_1"));
        }

        @Test
        @DisplayName("连续小数伤害应按精确护盾值扣减而非整数层数")
        void 连续小数伤害_按精确护盾值扣减() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 2, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));

            伤害结果 第一次 = 服务.计算(new 伤害上下文(
                    施法者, 目标玩家, 0.5D, false, false, 2.0, List.of()));
            伤害结果 第二次 = 服务.计算(new 伤害上下文(
                    施法者, 目标玩家, 1.0D, false, false, 2.0, List.of()));
            assertEquals(0.5D, 护盾处理器.获取精确护盾值(目标玩家.唯一标识()), 0.000001D,
                    "前两次消费后应保留0.5点精确护盾");
            伤害结果 第三次 = 服务.计算(new 伤害上下文(
                    施法者, 目标玩家, 0.5D, false, false, 2.0, List.of()));

            assertEquals(0.0D, 第一次.最终数值(), 0.000001D);
            assertEquals(0.0D, 第二次.最终数值(), 0.000001D);
            assertEquals(0.0D, 第三次.最终数值(), 0.000001D);
            assertEquals(0.0D, 护盾处理器.获取精确护盾值(目标玩家.唯一标识()), 0.000001D);
            verify(效果调度服务mock).设置层数(目标玩家.唯一标识(), "1_3_1", 1);
            verify(效果调度服务mock).移除(目标玩家.唯一标识(), "1_3_1");
        }

        @Test
        @DisplayName("护盾耗尽应移除")
        void 护盾耗尽_应移除() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 30, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.被吸收());
            // 100 - 30 = 70
            assertEquals(70, 结果.最终数值());
            verify(效果调度服务mock).移除(any(), eq("1_3_1"));
        }

        @Test
        @DisplayName("零护盾值应移除且不吸收")
        void 零护盾值_应移除且不吸收() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 0, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));
            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertFalse(结果.被吸收());
            verify(效果调度服务mock).移除(any(), eq("1_3_1"));
        }

        @Test
        @DisplayName("FP-04: 护盾耗尽时仍应记录吸收到受击修正上下文")
        void 护盾耗尽_仍应记录吸收到受击修正上下文() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 30, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));

            UUID 事件标识 = 事件日志上下文.开始新事件();
            try {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, false, false, 2.0, List.of(), 事件标识);

                伤害结果 结果 = 服务.计算(上下文);

                assertTrue(结果.被吸收(), "护盾耗尽时结果应标记为被吸收");
                assertEquals(70, 结果.最终数值(), "100-30=70 剩余伤害");
                verify(效果调度服务mock).移除(目标玩家.唯一标识(), "1_3_1");

                List<受击修正上下文.修正信息> 修正列表 =
                        受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件标识);
                assertEquals(1, 修正列表.size(), "护盾耗尽时仍应记录吸收到受击修正上下文");
                assertEquals(30.0, 修正列表.get(0).修正量(), 0.0001, "吸收量应为30");
                assertEquals("effect.奥能法师.奥术护盾.name", 修正列表.get(0).来源名称());
                assertEquals("战斗日志.吸收", 修正列表.get(0).获取翻译键());
            } finally {
                受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件标识);
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }

        @Test
        @DisplayName("FP-04: 护盾值等于伤害应耗尽并记录吸收")
        void 护盾等于伤害_应耗尽并记录吸收() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 100, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));

            UUID 事件标识 = 事件日志上下文.开始新事件();
            try {
                伤害上下文 上下文 = new 伤害上下文(
                        施法者, 目标玩家, 100, false, false, 2.0, List.of(), 事件标识);

                伤害结果 结果 = 服务.计算(上下文);

                assertTrue(结果.被吸收(), "护盾等于伤害时应标记为被吸收");
                verify(效果调度服务mock).移除(目标玩家.唯一标识(), "1_3_1");

                List<受击修正上下文.修正信息> 修正列表 =
                        受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件标识);
                assertEquals(1, 修正列表.size(), "护盾等于伤害时仍应记录吸收");
                assertEquals(100.0, 修正列表.get(0).修正量(), 0.0001, "吸收量应为100");
            } finally {
                受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件标识);
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }

        @Test
        @DisplayName("FP-04: 连续伤害第一次耗尽护盾第二次无吸收记录")
        void 连续伤害_第一次耗尽_第二次无吸收记录() {
            效果实例 护盾 = new 效果实例("1_3_1", "shield_skill", null, 30, -1);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of(护盾));

            UUID 事件一 = 事件日志上下文.开始新事件();
            伤害结果 第一次;
            try {
                伤害上下文 上下文一 = new 伤害上下文(
                        施法者, 目标玩家, 100, false, false, 2.0, List.of(), 事件一);
                第一次 = 服务.计算(上下文一);
            } finally {
                事件日志上下文.结束事件(事件一);
            }

            assertTrue(第一次.被吸收(), "第一次应被吸收");
            assertEquals(70, 第一次.最终数值(), "第一次剩余伤害70");
            List<受击修正上下文.修正信息> 修正一 =
                    受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件一);
            assertEquals(1, 修正一.size(), "第一次应记录吸收");
            assertEquals(30.0, 修正一.get(0).修正量(), 0.0001);

            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            UUID 事件二 = 事件日志上下文.开始新事件();
            伤害结果 第二次;
            try {
                伤害上下文 上下文二 = new 伤害上下文(
                        施法者, 目标玩家, 50, false, false, 2.0, List.of(), 事件二);
                第二次 = 服务.计算(上下文二);
            } finally {
                事件日志上下文.结束事件(事件二);
                事件日志上下文.清除();
            }

            assertFalse(第二次.被吸收(), "第二次无护盾不应被吸收");
            assertEquals(50, 第二次.最终数值(), "第二次伤害50");
            List<受击修正上下文.修正信息> 修正二 =
                    受击修正上下文.获取并清除(目标玩家.唯一标识(), 事件二);
            assertTrue(修正二.isEmpty(), "第二次无护盾不应有吸收记录");
        }
    }

    @Nested
    @DisplayName("阶段8-最终修正")
    class 阶段8最终修正 {

        @Test
        @DisplayName("伤害为零应返回零")
        void 伤害为零_应返回零() {
            // 通过减伤使伤害降为0或负数
            修饰器 全局减伤 = new 修饰器("减伤_全局百分比", "global", 修饰方式.相加, 200, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(全局减伤));

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1-2.0) = -100，最终修正返回0
            assertEquals(0, 结果.最终数值());
        }

        @Test
        @DisplayName("小伤害应保证最小伤害值1点")
        void 小伤害_应保证最小值() {
            修饰器 全局减伤 = new 修饰器("减伤_全局百分比", "global", 修饰方式.相加, 99, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(全局减伤));

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1-0.99) = 1，最小伤害值1.0
            assertEquals(1, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("超大伤害应正确处理")
        void 超大伤害_应正确处理() {
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, Double.MAX_VALUE, false, false, 2.0, List.of());

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertTrue(结果.最终数值() > 0);
        }

        @Test
        @DisplayName("零暴击倍率应正确处理")
        void 零暴击倍率_应正确处理() {
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, true, true, 0, List.of());

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.应用通用递减(anyDouble())).thenReturn(0.0);

            伤害结果 结果 = 服务.计算(上下文);

            // 暴击但倍率0，伤害为0
            assertEquals(0, 结果.最终数值());
            assertTrue(结果.是否暴击());
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("无施法者应正常计算")
        void 无施法者_应正常计算() {
            伤害上下文 上下文 = new 伤害上下文(
                    null, 目标玩家, 100, false, false, 2.0, List.of());

            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
            assertFalse(结果.是否暴击());
        }

        @Test
        @DisplayName("null目标应正常计算")
        void null目标_应正常计算() {
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, null, 100, false, false, 2.0, List.of());

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
        }

        @Test
        @DisplayName("空修饰器列表应正常计算")
        void 空修饰器列表_应正常计算() {
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            when(属性计算服务mock.计算(any())).thenReturn(创建属性快照());
            when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
            when(效果调度服务mock.获取列表(any())).thenReturn(List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
        }
    }

    private 属性快照 创建属性快照() {
        return 属性快照.创建(
                1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    @Nested
    @DisplayName("ERR-21: 阶段处理器注册表线程安全")
    class 阶段处理器注册表线程安全 {

        @Test
        @DisplayName("并发注册/清空阶段处理器和计算伤害应不抛ConcurrentModificationException")
        void 并发注册清空和计算_应不抛异常() throws Exception {
            int 线程数 = 20;
            int 迭代数 = 100;
            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        for (int j = 0; j < 迭代数; j++) {
                            int 模式 = (索引 + j) % 3;
                            if (模式 == 0) {
                                服务.注册阶段处理器(new 伤害管线阶段处理器.全能增伤阶段处理器());
                            } else if (模式 == 1) {
                                服务.清空阶段处理器(伤害管线阶段.全局伤害修饰);
                            } else {
                                伤害上下文 上下文 = new 伤害上下文(
                                        施法者, 目标玩家, 100, false, false, 2.0, List.of());
                                服务.计算(上下文);
                            }
                        }
                    } catch (Throwable e) {
                        异常引用.set(e);
                    } finally {
                        完成闸.countDown();
                    }
                });
                线程.start();
            }

            开始闸.countDown();
            assertTrue(完成闸.await(30, TimeUnit.SECONDS), "所有线程应在超时前完成");
            assertNull(异常引用.get(), "并发注册/清空和计算不应抛异常: " + 异常引用.get());
        }
    }
}
