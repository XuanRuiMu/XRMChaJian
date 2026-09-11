package mljy.业务层;

import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害管线阶段;
import mljy.领域层.战斗.伤害上下文;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("伤害管线阶段处理器")
class 伤害管线阶段处理器测试 {

    @Mock
    private 属性计算服务 属性计算服务mock;

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
    }

    @Nested
    @DisplayName("全能增伤阶段处理器")
    class 全能增伤阶段处理器测试 {

        @Test
        @DisplayName("阶段应返回全局伤害修饰")
        void 阶段_应返回全局伤害修饰() {
            伤害管线阶段处理器.全能增伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能增伤阶段处理器();

            assertEquals(伤害管线阶段.全局伤害修饰, 处理器.阶段());
        }

        @Test
        @DisplayName("无施法者应返回0")
        void 无施法者_应返回0() {
            伤害管线阶段处理器.全能增伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能增伤阶段处理器();
            伤害上下文 上下文 = new 伤害上下文(
                    null, 目标玩家, 100, false, false, 2.0, List.of());

            double 结果 = 处理器.计算增减伤百分比(上下文, 属性计算服务mock);

            assertEquals(0, 结果);
            verify(属性计算服务mock, never()).计算(any());
            verify(属性计算服务mock, never()).计算派生值(anyString(), any());
        }

        @Test
        @DisplayName("有施法者应调用服务获取全能增伤比例")
        void 有施法者_应调用服务() {
            when(属性计算服务mock.计算派生值(eq("全能增伤比例"), any())).thenReturn(0.15);
            伤害管线阶段处理器.全能增伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能增伤阶段处理器();
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            double 结果 = 处理器.计算增减伤百分比(上下文, 属性计算服务mock);

            assertEquals(0.15, 结果);
            verify(属性计算服务mock).计算(施法者);
            verify(属性计算服务mock).计算派生值(eq("全能增伤比例"), any());
        }
    }

    @Nested
    @DisplayName("全能减伤阶段处理器")
    class 全能减伤阶段处理器测试 {

        @Test
        @DisplayName("阶段应返回目标减伤")
        void 阶段_应返回目标减伤() {
            伤害管线阶段处理器.全能减伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能减伤阶段处理器();

            assertEquals(伤害管线阶段.目标减伤, 处理器.阶段());
        }

        @Test
        @DisplayName("null目标应返回0")
        void null目标_应返回0() {
            伤害管线阶段处理器.全能减伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能减伤阶段处理器();
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, null, 100, false, false, 2.0, List.of());

            double 结果 = 处理器.计算增减伤百分比(上下文, 属性计算服务mock);

            assertEquals(0, 结果);
            verify(属性计算服务mock, never()).计算(any());
            verify(属性计算服务mock, never()).计算派生值(anyString(), any());
        }

        @Test
        @DisplayName("非玩家目标应返回0")
        void 非玩家目标_应返回0() {
            伤害管线阶段处理器.全能减伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能减伤阶段处理器();
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, "字符串目标", 100, false, false, 2.0, List.of());

            double 结果 = 处理器.计算增减伤百分比(上下文, 属性计算服务mock);

            assertEquals(0, 结果);
            verify(属性计算服务mock, never()).计算(any());
        }

        @Test
        @DisplayName("玩家目标应调用服务获取全能减伤比例")
        void 玩家目标_应调用服务() {
            when(属性计算服务mock.计算派生值(eq("全能减伤比例"), any())).thenReturn(0.2);
            伤害管线阶段处理器.全能减伤阶段处理器 处理器 = new 伤害管线阶段处理器.全能减伤阶段处理器();
            伤害上下文 上下文 = new 伤害上下文(
                    施法者, 目标玩家, 100, false, false, 2.0, List.of());

            double 结果 = 处理器.计算增减伤百分比(上下文, 属性计算服务mock);

            assertEquals(0.2, 结果);
            verify(属性计算服务mock).计算(目标玩家);
            verify(属性计算服务mock).计算派生值(eq("全能减伤比例"), any());
        }
    }

    @Nested
    @DisplayName("record 相等性")
    class Record相等性 {

        @Test
        @DisplayName("全能增伤阶段处理器应相等")
        void 全能增伤_应相等() {
            伤害管线阶段处理器.全能增伤阶段处理器 处理器1 = new 伤害管线阶段处理器.全能增伤阶段处理器();
            伤害管线阶段处理器.全能增伤阶段处理器 处理器2 = new 伤害管线阶段处理器.全能增伤阶段处理器();

            assertEquals(处理器1, 处理器2);
            assertEquals(处理器1.hashCode(), 处理器2.hashCode());
        }

        @Test
        @DisplayName("全能减伤阶段处理器应相等")
        void 全能减伤_应相等() {
            伤害管线阶段处理器.全能减伤阶段处理器 处理器1 = new 伤害管线阶段处理器.全能减伤阶段处理器();
            伤害管线阶段处理器.全能减伤阶段处理器 处理器2 = new 伤害管线阶段处理器.全能减伤阶段处理器();

            assertEquals(处理器1, 处理器2);
            assertEquals(处理器1.hashCode(), 处理器2.hashCode());
        }
    }
}
