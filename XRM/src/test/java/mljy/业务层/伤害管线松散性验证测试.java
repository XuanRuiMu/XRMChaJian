package mljy.业务层;

import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害管线阶段;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.玩家快照;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("伤害管线松散性与注册表验证")
class 伤害管线松散性验证测试 {

    @Mock
    private 属性计算服务 属性计算服务mock;
    @Mock
    private 效果调度服务 效果调度服务mock;

    private 玩家快照 施法者;
    private 玩家快照 目标玩家;

    @BeforeEach
    void setUp() {
        属性快照 属性 = 属性快照.创建(
                1000, 1.5, 0, 10, 10, 10, 10, 10, 10,
                0, 0, 0, 0, 0, 0, 0, 0
        );
        施法者 = new 玩家快照(UUID.randomUUID(), "施法者", 1, null, 属性, java.util.Map.of(), null);
        目标玩家 = new 玩家快照(UUID.randomUUID(), "目标", 1, null, 属性, java.util.Map.of(), null);

        when(属性计算服务mock.计算(any())).thenReturn(属性);
        when(属性计算服务mock.应用通用递减(anyDouble())).thenReturn(0.0);
        when(属性计算服务mock.计算派生值(anyString(), any())).thenReturn(0.0);
        when(效果调度服务mock.获取列表(any())).thenReturn(List.of());
    }

    @Nested
    @DisplayName("阶段5遍历注册表")
    class 阶段5遍历注册表 {

        @Test
        @DisplayName("默认注册全能增伤处理器应叠加到全局增伤")
        void 默认注册_应应用全能增伤() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.1);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(110, 结果.最终数值());
        }

        @Test
        @DisplayName("注册自定义阶段5处理器应叠加到全局增伤")
        void 注册自定义处理器_应叠加() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.注册阶段处理器(new 自定义增伤处理器(0.05));
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.1);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 全能增伤0.1 + 自定义0.05 = 0.15，100*(1+0.15)=115
            assertEquals(115, 结果.最终数值());
        }

        @Test
        @DisplayName("注册多个阶段5处理器应累加")
        void 注册多个处理器_应累加() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.注册阶段处理器(new 自定义增伤处理器(0.05));
            服务.注册阶段处理器(new 自定义增伤处理器(0.03));
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.1);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 全能0.1 + 自定义1 0.05 + 自定义2 0.03 = 0.18，100*(1+0.18)=118
            assertEquals(118, 结果.最终数值());
        }

        @Test
        @DisplayName("注册null处理器应被忽略")
        void 注册null处理器_应被忽略() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);

            服务.注册阶段处理器(null);

            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());
            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("阶段6遍历注册表")
    class 阶段6遍历注册表 {

        @Test
        @DisplayName("默认注册全能减伤处理器应叠加到全局减伤")
        void 默认注册_应应用全能减伤() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            when(属性计算服务mock.计算派生值(eq("全能减伤比例"), any())).thenReturn(0.2);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100 * (1-0.2) = 80
            assertEquals(80, 结果.最终数值());
        }

        @Test
        @DisplayName("注册自定义阶段6处理器应叠加到全局减伤")
        void 注册自定义处理器_应叠加() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.注册阶段处理器(new 自定义减伤处理器(0.1));
            when(属性计算服务mock.计算派生值(eq("全能减伤比例"), any())).thenReturn(0.2);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 全能减伤0.2 + 自定义0.1 = 0.3，100*(1-0.3)=70
            assertEquals(70, 结果.最终数值());
        }
    }

    @Nested
    @DisplayName("松散性验证（核心）")
    class 松散性验证 {

        @Test
        @DisplayName("删除全能增伤处理器后阶段5不再应用全能增伤")
        void 删除全能增伤_阶段5不应用全能增伤() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.清空阶段处理器(伤害管线阶段.全局伤害修饰);
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.5);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
            verify(属性计算服务mock, never()).计算派生值(eq("全能增伤比例"), any());
        }

        @Test
        @DisplayName("删除全能增伤处理器后修饰器增伤仍生效")
        void 删除全能增伤_修饰器增伤仍生效() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.清空阶段处理器(伤害管线阶段.全局伤害修饰);
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.5);
            修饰器 全局增伤 = new 修饰器("伤害_全伤害百分比", "g1", 修饰方式.相加, 30, 0);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of(全局增伤));

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(130, 结果.最终数值());
            verify(属性计算服务mock, never()).计算派生值(eq("全能增伤比例"), any());
        }

        @Test
        @DisplayName("删除全能减伤处理器后阶段6不再应用全能减伤")
        void 删除全能减伤_阶段6不应用全能减伤() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.清空阶段处理器(伤害管线阶段.目标减伤);
            when(属性计算服务mock.计算派生值(eq("全能减伤比例"), any())).thenReturn(0.5);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
            verify(属性计算服务mock, never()).计算派生值(eq("全能减伤比例"), any());
        }

        @Test
        @DisplayName("清空全部阶段5和6处理器后系统仍正常运行")
        void 清空全部处理器_系统仍正常运行() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.清空阶段处理器(伤害管线阶段.全局伤害修饰);
            服务.清空阶段处理器(伤害管线阶段.目标减伤);
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            assertEquals(100, 结果.最终数值());
            assertDoesNotThrow(() -> 服务.计算(上下文));
        }

        @Test
        @DisplayName("清空后再注册自定义处理器仍可生效")
        void 清空后重新注册_仍可生效() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);
            服务.清空阶段处理器(伤害管线阶段.全局伤害修饰);
            服务.注册阶段处理器(new 自定义增伤处理器(0.2));
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            伤害结果 结果 = 服务.计算(上下文);

            // 100*(1+0.2)=120
            assertEquals(120, 结果.最终数值());
            verify(属性计算服务mock, never()).计算派生值(eq("全能增伤比例"), any());
        }

        @Test
        @DisplayName("清空不存在的阶段处理器不应报错")
        void 清空不存在阶段_不应报错() {
            伤害计算服务实现 服务 = new 伤害计算服务实现(属性计算服务mock, 效果调度服务mock);

            assertDoesNotThrow(() -> 服务.清空阶段处理器(伤害管线阶段.护盾吸收));
        }
    }

    private record 自定义增伤处理器(double 增伤比例) implements 伤害管线阶段处理器 {
        @Override
        public 伤害管线阶段 阶段() {
            return 伤害管线阶段.全局伤害修饰;
        }

        @Override
        public double 计算增减伤百分比(伤害上下文 上下文, 属性计算服务 服务) {
            return 增伤比例;
        }
    }

    private record 自定义减伤处理器(double 减伤比例) implements 伤害管线阶段处理器 {
        @Override
        public 伤害管线阶段 阶段() {
            return 伤害管线阶段.目标减伤;
        }

        @Override
        public double 计算增减伤百分比(伤害上下文 上下文, 属性计算服务 服务) {
            return 减伤比例;
        }
    }
}
