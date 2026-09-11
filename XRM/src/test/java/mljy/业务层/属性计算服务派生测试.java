package mljy.业务层;

import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.属性.派生属性计算器;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 属性计算服务.计算派生值 5个派生名验证。
 * 验证点：
 * 1. 5个内置派生名("全能增伤比例"/"全能减伤比例"/"吸血比例"/"躲闪几率"/"暴击期望增伤")
 *    通过 计算派生值(String, 属性快照) 接口均能正确返回。
 * 2. 未注册派生名返回0。
 * 3. null入参返回0。
 * 4. 派生计算器内部使用 属性计算服务实现.应用通用递减 (this::应用通用递减)。
 * 5. 松散性：动态注册新派生计算器后，计算派生值能识别新派生名。
 */
@DisplayName("属性计算服务.计算派生值")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 属性计算服务派生测试 {

    @Mock
    private 修饰器管理器 修饰器管理器;

    private 属性计算服务实现 服务;

    @BeforeEach
    void setUp() {
        // 修饰器管理器对任意类别返回基础值本身(恒等)
        when(修饰器管理器.计算最终值(any(UUID.class), anyString(), anyDouble()))
                .thenAnswer(inv -> inv.getArgument(2));
        服务 = new 属性计算服务实现(修饰器管理器);
    }

    private 属性快照 创建属性(double 全能, double 吸血, double 躲闪, double 法术暴击几率, double 法术暴击伤害) {
        return 属性快照.创建(
                1000, 1.0, 0, 0, 0, 0, 0, 0, 0,
                法术暴击几率, 法术暴击伤害, 0,
                全能, 吸血, 躲闪, 0, 0
        );
    }

    @Nested
    @DisplayName("5个内置派生名")
    class 内置派生名 {

        @Test
        @DisplayName("全能增伤比例 - 全能20点应返回0.10")
        void 全能增伤比例_应正确计算() {
            属性快照 属性 = 创建属性(20, 0, 0, 0, 0);

            double 结果 = 服务.计算派生值("全能增伤比例", 属性);

            // 应用通用递减: 20点全在第一段(系数1.0)→有效20 → 20*0.005=0.10
            assertEquals(0.10, 结果, 0.000001);
        }

        @Test
        @DisplayName("全能减伤比例 - 全能20点应返回0.10")
        void 全能减伤比例_应正确计算() {
            属性快照 属性 = 创建属性(20, 0, 0, 0, 0);

            double 结果 = 服务.计算派生值("全能减伤比例", 属性);

            assertEquals(0.10, 结果, 0.000001);
        }

        @Test
        @DisplayName("吸血比例 - 吸血15点应返回0.15")
        void 吸血比例_应正确计算() {
            属性快照 属性 = 创建属性(0, 15, 0, 0, 0);

            double 结果 = 服务.计算派生值("吸血比例", 属性);

            assertEquals(0.15, 结果, 0.000001);
        }

        @Test
        @DisplayName("躲闪几率 - 躲闪10点应返回0.10")
        void 躲闪几率_应正确计算() {
            属性快照 属性 = 创建属性(0, 0, 10, 0, 0);

            double 结果 = 服务.计算派生值("躲闪几率", 属性);

            assertEquals(0.10, 结果, 0.000001);
        }

        @Test
        @DisplayName("暴击期望增伤 - 几率10伤害20(均在第一段无递减)应返回0.02")
        void 暴击期望增伤_应正确计算() {
            // 几率10递减后10(第一段系数1.0)，伤害20递减后20(第一段系数1.0)
            // 期望增伤 = 10*0.01 * 20*0.01 = 0.10 * 0.20 = 0.02
            属性快照 属性 = 创建属性(0, 0, 0, 10, 20);

            double 结果 = 服务.计算派生值("暴击期望增伤", 属性);

            assertEquals(0.02, 结果, 0.000001);
        }

        @Test
        @DisplayName("5个派生名都应能被计算派生值识别(不返回0)")
        void 五个派生名_都应被识别() {
            属性快照 属性 = 创建属性(100, 100, 100, 100, 100);

            assertNotEquals(0.0, 服务.计算派生值("全能增伤比例", 属性));
            assertNotEquals(0.0, 服务.计算派生值("全能减伤比例", 属性));
            assertNotEquals(0.0, 服务.计算派生值("吸血比例", 属性));
            assertNotEquals(0.0, 服务.计算派生值("躲闪几率", 属性));
            assertNotEquals(0.0, 服务.计算派生值("暴击期望增伤", 属性));
        }
    }

    @Nested
    @DisplayName("递减函数集成")
    class 递减函数集成 {

        @Test
        @DisplayName("计算派生值应通过this::应用通用递减传递递减函数给计算器")
        void 计算派生值_应使用应用通用递减() {
            // 60点全能: 第一段20*1.0 + 第二段10*0.8 + 第三段10*0.6 + 第四段20*0.4 = 20+8+6+8 = 42
            // 全能增伤比例: 42 * 0.005 = 0.21
            属性快照 属性 = 创建属性(60, 0, 0, 0, 0);

            double 结果 = 服务.计算派生值("全能增伤比例", 属性);
            double 直接调用 = 60.0 * 0.005;

            assertNotEquals(直接调用, 结果, 0.0001,
                    "应通过应用通用递减产生递减效果，不应等于直接计算");
            assertEquals(0.21, 结果, 0.000001,
                    "60点全能递减后42点 * 0.005 = 0.21");
        }

        @Test
        @DisplayName("吸血比例应受递减影响")
        void 吸血比例_应受递减影响() {
            // 50点吸血: 第一段20*1.0 + 第二段10*0.8 + 第三段10*0.6 + 第四段10*0.4 = 20+8+6+4 = 38
            // 吸血比例: 38 * 0.01 = 0.38
            属性快照 属性 = 创建属性(0, 50, 0, 0, 0);

            double 结果 = 服务.计算派生值("吸血比例", 属性);

            assertEquals(0.38, 结果, 0.000001);
        }
    }

    @Nested
    @DisplayName("未注册与null入参")
    class 未注册与null入参 {

        @Test
        @DisplayName("未注册的派生名应返回0")
        void 未注册派生名_应返回0() {
            属性快照 属性 = 创建属性(100, 100, 100, 100, 100);

            assertEquals(0.0, 服务.计算派生值("不存在的派生", 属性));
        }

        @Test
        @DisplayName("null派生名应返回0")
        void null派生名_应返回0() {
            属性快照 属性 = 创建属性(100, 100, 100, 100, 100);

            assertEquals(0.0, 服务.计算派生值(null, 属性));
        }

        @Test
        @DisplayName("null属性快照应返回0")
        void null属性快照_应返回0() {
            assertEquals(0.0, 服务.计算派生值("全能增伤比例", null));
        }

        @Test
        @DisplayName("null派生名和null属性应返回0")
        void null派生名和null属性_应返回0() {
            assertEquals(0.0, 服务.计算派生值(null, null));
        }
    }

    @Nested
    @DisplayName("动态注册(松散性/可插入性)")
    class 动态注册 {

        @Test
        @DisplayName("动态注册新派生计算器后应能通过计算派生值识别")
        void 动态注册新计算器_应被识别() {
            派生属性计算器 自定义计算器 = new 派生属性计算器() {
                private static final String 派生名 = "自定义派生";

                @Override
                public String 派生名() {
                    return 派生名;
                }

                @Override
                public double 计算(属性快照 属性, java.util.function.DoubleUnaryOperator 应用通用递减) {
                    return 999.0;
                }
            };

            服务.注册派生计算器(自定义计算器);
            属性快照 属性 = 创建属性(0, 0, 0, 0, 0);

            assertEquals(999.0, 服务.计算派生值("自定义派生", 属性));
        }

        @Test
        @DisplayName("注册null计算器应被忽略")
        void 注册null计算器_应被忽略() {
            服务.注册派生计算器(null);

            属性快照 属性 = 创建属性(20, 0, 0, 0, 0);
            // 已注册的5个内置计算器仍正常工作
            assertEquals(0.10, 服务.计算派生值("全能增伤比例", 属性), 0.000001);
        }

        @Test
        @DisplayName("注册返回null派生名的计算器应被忽略")
        void 注册null派生名计算器_应被忽略() {
            派生属性计算器 异常计算器 = new 派生属性计算器() {
                @Override
                public String 派生名() {
                    return null;
                }

                @Override
                public double 计算(属性快照 属性, java.util.function.DoubleUnaryOperator 应用通用递减) {
                    return 1.0;
                }
            };

            服务.注册派生计算器(异常计算器);

            // 计算派生值对任何派生名都不应返回1.0
            属性快照 属性 = 创建属性(0, 0, 0, 0, 0);
            assertEquals(0.0, 服务.计算派生值("任何名字", 属性));
        }

        @Test
        @DisplayName("重复注册相同派生名应保留首次注册的计算器(幂等)")
        void 重复注册相同派生名_应保留首次() {
            派生属性计算器 第一 = new 派生属性计算器() {
                @Override
                public String 派生名() {
                    return "测试派生";
                }

                @Override
                public double 计算(属性快照 属性, java.util.function.DoubleUnaryOperator 应用通用递减) {
                    return 1.0;
                }
            };
            派生属性计算器 第二 = new 派生属性计算器() {
                @Override
                public String 派生名() {
                    return "测试派生";
                }

                @Override
                public double 计算(属性快照 属性, java.util.function.DoubleUnaryOperator 应用通用递减) {
                    return 2.0;
                }
            };

            服务.注册派生计算器(第一);
            服务.注册派生计算器(第二);

            属性快照 属性 = 创建属性(0, 0, 0, 0, 0);
            assertEquals(1.0, 服务.计算派生值("测试派生", 属性),
                    "重复注册应保留首次注册的计算器");
        }
    }
}
