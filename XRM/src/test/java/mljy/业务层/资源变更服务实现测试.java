package mljy.业务层;

import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.效果调度服务;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;
import mljy.领域层.技能.参数注册表;
import mljy.领域层.效果.效果实例;
import mljy.资源服务;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("资源变更服务实现")
@SuppressWarnings("unchecked")
class 资源变更服务实现测试 {

    @Mock
    private 资源服务 资源服务mock;
    @Mock
    private 资源注册服务 资源注册服务mock;
    @Mock
    private 效果调度服务 效果调度服务mock;

    private 修饰器管理器 修饰器管理器;
    private 参数注册表 参数注册表实例;
    private 资源变更服务实现 服务;

    private UUID 玩家标识;
    private static final String 资源标识 = "秘能";
    private static final String 秘兆效果标识 = "1_9_1";

    @BeforeEach
    void setUp() {
        修饰器管理器 = new 修饰器管理器();
        参数注册表实例 = new 参数注册表();
        服务 = new 资源变更服务实现(资源服务mock, 资源注册服务mock, 修饰器管理器, 效果调度服务mock, 参数注册表实例);
        玩家标识 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("获取当前值应返回资源当前值")
        void 获取当前值_应返回资源当前值() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            double 结果 = 服务.获取当前值(玩家标识, 资源标识);

            assertEquals(3, 结果);
        }

        @Test
        @DisplayName("获取上限应返回修饰后上限")
        void 获取上限_应返回修饰后上限() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            double 结果 = 服务.获取上限(玩家标识, 资源标识);

            assertEquals(4, 结果);
        }

        @Test
        @DisplayName("获取每秒恢复应返回资源定义的恢复值")
        void 获取每秒恢复_应返回定义值() {
            资源定义 定义 = new 资源定义(资源标识, "秘能", 4, 1, "resource.arcane");
            when(资源注册服务mock.获取资源定义(资源标识)).thenReturn(Optional.of(定义));

            double 结果 = 服务.获取每秒恢复(玩家标识, 资源标识);

            assertEquals(1, 结果);
        }

        @Test
        @DisplayName("增加资源应正确增加")
        void 增加_应正确增加() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.增加(玩家标识, 资源标识, 1);

            assertEquals(3, 资源.获取当前值());
        }

        @Test
        @DisplayName("消耗资源应正确消耗")
        void 消耗_应正确消耗() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.消耗(玩家标识, 资源标识, 1);

            assertEquals(2, 资源.获取当前值());
        }

        @Test
        @DisplayName("消耗资源后应在同一次变更中通知计分板回调")
        void 消耗_应同步通知回调() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            java.util.function.BiConsumer<UUID, String> 回调 = mock(java.util.function.BiConsumer.class);
            服务.设置资源变更回调(回调);

            服务.消耗(玩家标识, 资源标识, 1);

            assertEquals(2, 资源.获取当前值());
            verify(回调).accept(玩家标识, 资源标识);
        }

        @Test
        @DisplayName("计分板回调异常时资源应回滚到变更前")
        void 消耗_回调异常_资源回滚() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            java.util.function.BiConsumer<UUID, String> 回调 = mock(java.util.function.BiConsumer.class);
            doThrow(new IllegalStateException("同步失败")).when(回调).accept(玩家标识, 资源标识);
            服务.设置资源变更回调(回调);

            assertThrows(IllegalStateException.class, () -> 服务.消耗(玩家标识, 资源标识, 1));

            assertEquals(3, 资源.获取当前值(), "回调失败后资源值必须恢复，避免资源与计分板分叉");
        }

        @Test
        @DisplayName("清空资源应归零")
        void 清空_应归零() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.清空(玩家标识, 资源标识);

            assertEquals(0, 资源.获取当前值());
        }

        @Test
        @DisplayName("设置上限应委托给资源服务")
        void 设置上限_应委托() {
            服务.设置上限(玩家标识, 资源标识, 10);

            verify(资源服务mock).设置资源上限(玩家标识, 资源标识, 10);
        }

        @Test
        @DisplayName("恢复应按每秒恢复值增加")
        void 恢复_应按每秒恢复增加() {
            资源 资源 = new 资源(资源标识, 2, 4);
            资源定义 定义 = new 资源定义(资源标识, "秘能", 4, 1, "resource.arcane");
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            when(资源注册服务mock.获取资源定义(资源标识)).thenReturn(Optional.of(定义));

            服务.恢复(玩家标识, 资源标识);

            assertEquals(3, 资源.获取当前值());
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("增加超过上限应截断到上限")
        void 增加超过上限_应截断() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.增加(玩家标识, 资源标识, 10);

            assertEquals(4, 资源.获取当前值());
        }

        @Test
        @DisplayName("消耗超过当前值应截断到零")
        void 消耗超过当前值_应截断到零() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.消耗(玩家标识, 资源标识, 10);

            assertEquals(0, 资源.获取当前值());
        }

        @Test
        @DisplayName("零资源增加应正确处理")
        void 零资源增加_应正确处理() {
            资源 资源 = new 资源(资源标识, 0, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.增加(玩家标识, 资源标识, 2);

            assertEquals(2, 资源.获取当前值());
        }

        @Test
        @DisplayName("零每秒恢复应不增加")
        void 零每秒恢复_应不增加() {
            资源定义 定义 = new 资源定义(资源标识, "秘能", 4, 0, "resource.arcane");
            when(资源注册服务mock.获取资源定义(资源标识)).thenReturn(Optional.of(定义));

            服务.恢复(玩家标识, 资源标识);

            // 每秒恢复为0，不调用获取资源
            verify(资源服务mock, never()).获取资源(any(), any());
        }

        @Test
        @DisplayName("负数增加应正确处理")
        void 负数增加_应正确处理() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));

            服务.增加(玩家标识, 资源标识, -1);

            assertEquals(2, 资源.获取当前值());
        }
    }

    @Nested
    @DisplayName("修饰器影响上限")
    class 修饰器影响上限 {

        @Test
        @DisplayName("上限修饰器应影响最终上限")
        void 上限修饰器_应影响最终上限() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            修饰器管理器.注册修饰器(玩家标识,
                    new 修饰器("资源_秘能_上限", "buff1", 修饰方式.相加, 2, 0));

            double 上限 = 服务.获取上限(玩家标识, 资源标识);

            assertEquals(6, 上限);
        }

        @Test
        @DisplayName("上限修饰器应影响增加的截断值")
        void 上限修饰器_应影响增加截断() {
            资源 资源 = new 资源(资源标识, 3, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            修饰器管理器.注册修饰器(玩家标识,
                    new 修饰器("资源_秘能_上限", "buff1", 修饰方式.相加, 2, 0));

            服务.增加(玩家标识, 资源标识, 10);

            // 基础上限4 + 修饰器2 = 6
            assertEquals(6, 资源.获取当前值());
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("资源不存在时获取当前值应返回零")
        void 资源不存在_获取当前值应返回零() {
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.empty());

            double 结果 = 服务.获取当前值(玩家标识, 资源标识);

            assertEquals(0, 结果);
        }

        @Test
        @DisplayName("资源不存在时获取上限应返回零")
        void 资源不存在_获取上限应返回零() {
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.empty());

            double 结果 = 服务.获取上限(玩家标识, 资源标识);

            assertEquals(0, 结果);
        }

        @Test
        @DisplayName("资源定义不存在时获取每秒恢复应返回零")
        void 资源定义不存在_获取每秒恢复应返回零() {
            when(资源注册服务mock.获取资源定义(资源标识)).thenReturn(Optional.empty());

            double 结果 = 服务.获取每秒恢复(玩家标识, 资源标识);

            assertEquals(0, 结果);
        }

        @Test
        @DisplayName("资源不存在时增加应不抛出异常")
        void 资源不存在_增加应不抛出异常() {
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> 服务.增加(玩家标识, 资源标识, 1));
        }

        @Test
        @DisplayName("资源不存在时清空应不抛出异常")
        void 资源不存在_清空应不抛出异常() {
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> 服务.清空(玩家标识, 资源标识));
        }
    }

    @Nested
    @DisplayName("FP-08 秘兆数据驱动(下沉资源变更服务.增加)")
    class 秘兆数据驱动 {
        private void 注册秘兆参数(String 概率, String 持续) {
            参数注册表实例.注册(秘兆效果标识, new 参数注册表.参数条目("秘兆触发概率", 概率, "测试"));
            参数注册表实例.注册(秘兆效果标识, new 参数注册表.参数条目("秘兆持续时间", 持续, "测试"));
        }

        @Test
        @DisplayName("秘能实际增加>0且概率为1应触发秘兆并返回true")
        void 秘能_实际增加大于零_概率一_应触发() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            注册秘兆参数("1.0", "5000");

            boolean 触发 = 服务.增加(玩家标识, 资源标识, 1);

            assertTrue(触发, "秘能实际增加>0且概率=1时必须触发秘兆");
            verify(效果调度服务mock).添加(eq(玩家标识), any(效果实例.class));
        }

        @Test
        @DisplayName("秘能实际增加>0但概率为0不应触发秘兆并返回false")
        void 秘能_实际增加大于零_概率零_不应触发() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            注册秘兆参数("0.0", "5000");

            boolean 触发 = 服务.增加(玩家标识, 资源标识, 1);

            assertFalse(触发, "概率=0时不应触发秘兆");
            verify(效果调度服务mock, never()).添加(any(), any());
        }

        @Test
        @DisplayName("非秘能资源即使概率为1也不应触发秘兆")
        void 非秘能_概率一_不应触发() {
            String 其他资源 = "生命";
            资源 资源 = new 资源(其他资源, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 其他资源)).thenReturn(Optional.of(资源));
            注册秘兆参数("1.0", "5000");

            boolean 触发 = 服务.增加(玩家标识, 其他资源, 1);

            assertFalse(触发, "非秘能资源走增加不应触发秘兆");
            verify(效果调度服务mock, never()).添加(any(), any());
        }

        @Test
        @DisplayName("秘能已满实际增加为0不应触发秘兆")
        void 秘能已满_实际增加为零_不应触发() {
            资源 资源 = new 资源(资源标识, 4, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            注册秘兆参数("1.0", "5000");

            boolean 触发 = 服务.增加(玩家标识, 资源标识, 1);

            assertFalse(触发, "秘能已满、实际增加=0时不应触发秘兆");
            verify(效果调度服务mock, never()).添加(any(), any());
        }

        @Test
        @DisplayName("秘兆效果实例应由参数注册表驱动(标识/来源/层数/持续)")
        void 秘兆效果实例_由参数驱动() {
            资源 资源 = new 资源(资源标识, 2, 4);
            when(资源服务mock.获取资源(玩家标识, 资源标识)).thenReturn(Optional.of(资源));
            注册秘兆参数("1.0", "3000");
            long 开始 = System.currentTimeMillis();

            boolean 触发 = 服务.增加(玩家标识, 资源标识, 1);

            assertTrue(触发);
            ArgumentCaptor<效果实例> 捕获 = ArgumentCaptor.forClass(效果实例.class);
            verify(效果调度服务mock).添加(eq(玩家标识), 捕获.capture());
            效果实例 效果 = 捕获.getValue();
            assertEquals("1_9_1", 效果.获取效果标识());
            assertEquals("1_9", 效果.获取来源技能标识());
            assertEquals(1, 效果.获取层数());
            long 预期到期 = 开始 + 3000;
            assertTrue(效果.获取到期时间() >= 开始 && 效果.获取到期时间() <= 预期到期 + 200,
                    "秘兆到期时间应由注册的秘兆持续时间参数驱动");
        }
    }
}
