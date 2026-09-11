package mljy.业务层;

import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.属性.派生属性计算器;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("属性计算服务实现")
class 属性计算服务实现测试 {

    private 修饰器管理器 修饰器管理器;
    private 属性计算服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        修饰器管理器 = new 修饰器管理器();
        服务 = new 属性计算服务实现(修饰器管理器);
        玩家标识 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("通用递减")
    class 通用递减 {

        @Test
        @DisplayName("0值应返回0")
        void 零值_应返回0() {
            assertEquals(0, 服务.应用通用递减(0));
        }

        @Test
        @DisplayName("负值应原样返回（负数属性不应用递减）")
        void 负值_应原样返回() {
            assertEquals(-10, 服务.应用通用递减(-10));
        }

        @Test
        @DisplayName("0-20区间应1.0系数")
        void 第一段_应1点对1点() {
            assertEquals(20, 服务.应用通用递减(20));
        }

        @Test
        @DisplayName("21-30区间应0.8系数")
        void 第二段_应0点8系数() {
            // 20*1.0 + 10*0.8 = 28
            assertEquals(28, 服务.应用通用递减(30));
        }

        @Test
        @DisplayName("31-40区间应0.6系数")
        void 第三段_应0点6系数() {
            // 20*1.0 + 10*0.8 + 10*0.6 = 34
            assertEquals(34, 服务.应用通用递减(40));
        }

        @Test
        @DisplayName("40+区间应0.4系数")
        void 第四段_应0点4系数() {
            // 20*1.0 + 10*0.8 + 10*0.6 + 10*0.4 = 38
            assertEquals(38, 服务.应用通用递减(50));
        }

        @Test
        @DisplayName("超大数值应正确分段计算")
        void 超大数值_应正确分段() {
            // 20*1.0 + 10*0.8 + 10*0.6 + 60*0.4 = 58
            assertEquals(58, 服务.应用通用递减(100));
        }
    }

    @Nested
    @DisplayName("急速除法模型")
    class 急速除法模型 {

        @Test
        @DisplayName("零急速应返回基础值")
        void 零急速_应返回基础值() {
            double 结果 = 服务.计算实际公共冷却(1.5, 0);
            assertEquals(1.5, 结果);
        }

        @Test
        @DisplayName("急速应缩短公共冷却")
        void 正急速_应缩短公共冷却() {
            // 有效急速=20，GCD = 1.5 / (1 + 20*0.01) = 1.5 / 1.2 = 1.25
            double 结果 = 服务.计算实际公共冷却(1.5, 20);
            assertEquals(1.25, 结果, 0.001);
        }

        @Test
        @DisplayName("急速不应使GCD低于下限")
        void 高急速_不应低于下限() {
            // 基础1.5，下限=1.5*0.5=0.75
            // 有效急速=38(100急速递减后)，GCD = 1.5/(1+0.38) = 1.087，高于下限
            // 但极高急速时：有效急速=1000递减后=20+8+6+388=422，GCD=1.5/5.22=0.287，低于下限0.75
            assertEquals(0.75, 服务.计算实际公共冷却(1.5, 1000), 0.001);
            assertEquals(0.75, 服务.计算实际公共冷却(1.5, Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("负急速应延长公共冷却")
        void 负急速_应延长公共冷却() {
            assertEquals(150.0, 服务.计算实际公共冷却(1.5, -100), 0.001);
            assertEquals(1.667, 服务.计算实际公共冷却(1.5, -10), 0.001);
            assertEquals(1.5, 服务.计算实际公共冷却(1.5, Double.NaN));
            assertEquals(1.5, 服务.计算实际公共冷却(1.5, Double.NEGATIVE_INFINITY));
        }

        @Test
        @DisplayName("基础时间零应返回零")
        void 基础时间零_应返回零() {
            assertEquals(0, 服务.计算实际公共冷却(0, 1000));
            assertEquals(0, 服务.计算实际蓄力时间(0, 50));
        }

        @Test
        @DisplayName("蓄力时间应受急速影响")
        void 蓄力时间_应受急速影响() {
            // 有效急速=20，蓄力=2.0/(1+0.2)=1.667
            assertEquals(1.667, 服务.计算实际蓄力时间(2.0, 20), 0.001);
            assertEquals(200.0, 服务.计算实际蓄力时间(2.0, -100), 0.001);
            assertEquals(2.222, 服务.计算实际蓄力时间(2.0, -10), 0.001);
            assertEquals(2.0, 服务.计算实际蓄力时间(2.0, Double.NaN));
            assertEquals(2.0, 服务.计算实际蓄力时间(2.0, Double.NEGATIVE_INFINITY));
        }

        @Test
        @DisplayName("蓄力时间不应低于下限0点2秒")
        void 高急速蓄力_不应低于下限() {
            // 急速=10000，有效急速=34+(10000-40)*0.4=4018
            // 蓄力=2.0/(1+40.18)=0.0486 < 0.2，触发下限
            assertEquals(0.2, 服务.计算实际蓄力时间(2.0, 10000), 0.001);
            assertEquals(0.2, 服务.计算实际蓄力时间(2.0, Double.POSITIVE_INFINITY));
            assertEquals(0.1, 服务.计算实际蓄力时间(0.1, 0));
        }
    }

    @Nested
    @DisplayName("暴击期望增伤")
    class 暴击期望增伤 {

        @Test
        @DisplayName("零暴击应返回零")
        void 零暴击_应返回零() {
            属性快照 属性 = 创建暴击属性(0, 100);
            assertEquals(0, 服务.计算派生值("暴击期望增伤", 属性));
        }

        @Test
        @DisplayName("零暴击伤害应返回零")
        void 零暴击伤害_应返回零() {
            属性快照 属性 = 创建暴击属性(50, 0);
            assertEquals(0, 服务.计算派生值("暴击期望增伤", 属性));
        }

        @Test
        @DisplayName("正常暴击应正确计算")
        void 正常暴击_应正确计算() {
            // 有效暴击率=应用通用递减(20)=20
            // 有效暴击伤害=应用通用递减(50)=20+8+6+4=38
            // 期望增伤 = 20*0.01 * 38*0.01 = 0.076
            属性快照 属性 = 创建暴击属性(20, 50);
            double 结果 = 服务.计算派生值("暴击期望增伤", 属性);
            assertEquals(0.076, 结果, 0.001);
        }
    }

    @Nested
    @DisplayName("全能增伤减伤")
    class 全能计算 {

        @Test
        @DisplayName("零全能应返回零增伤")
        void 零全能_应返回零增伤() {
            属性快照 属性 = 创建全能属性(0);
            assertEquals(0, 服务.计算派生值("全能增伤比例", 属性));
        }

        @Test
        @DisplayName("正常全能应正确计算增伤")
        void 正常全能_应正确计算增伤() {
            // 有效全能=20，增伤=20*0.005=0.1
            属性快照 属性 = 创建全能属性(20);
            double 结果 = 服务.计算派生值("全能增伤比例", 属性);
            assertEquals(0.1, 结果, 0.001);
        }

        @Test
        @DisplayName("零全能应返回零减伤")
        void 零全能_应返回零减伤() {
            属性快照 属性 = 创建全能属性(0);
            assertEquals(0, 服务.计算派生值("全能减伤比例", 属性));
        }

        @Test
        @DisplayName("正常全能应正确计算减伤")
        void 正常全能_应正确计算减伤() {
            // 有效全能=20，减伤=20*0.005=0.1
            属性快照 属性 = 创建全能属性(20);
            double 结果 = 服务.计算派生值("全能减伤比例", 属性);
            assertEquals(0.1, 结果, 0.001);
        }

        @Test
        @DisplayName("全能增伤和减伤应相等")
        void 全能增伤和减伤_应相等() {
            属性快照 属性 = 创建全能属性(30);
            double 增伤 = 服务.计算派生值("全能增伤比例", 属性);
            double 减伤 = 服务.计算派生值("全能减伤比例", 属性);
            assertEquals(增伤, 减伤, 0.001);
        }
    }

    @Nested
    @DisplayName("吸血和躲闪")
    class 吸血和躲闪 {

        @Test
        @DisplayName("零吸血应返回零")
        void 零吸血_应返回零() {
            属性快照 属性 = 创建吸血属性(0);
            assertEquals(0, 服务.计算派生值("吸血比例", 属性));
        }

        @Test
        @DisplayName("正常吸血应正确计算")
        void 正常吸血_应正确计算() {
            // 有效吸血=20，比例=20*0.01=0.2
            属性快照 属性 = 创建吸血属性(20);
            double 结果 = 服务.计算派生值("吸血比例", 属性);
            assertEquals(0.2, 结果, 0.001);
        }

        @Test
        @DisplayName("零躲闪应返回零")
        void 零躲闪_应返回零() {
            属性快照 属性 = 创建躲闪属性(0);
            assertEquals(0, 服务.计算派生值("躲闪几率", 属性));
        }

        @Test
        @DisplayName("正常躲闪应正确计算")
        void 正常躲闪_应正确计算() {
            // 有效躲闪=20，几率=20*0.01=0.2
            属性快照 属性 = 创建躲闪属性(20);
            double 结果 = 服务.计算派生值("躲闪几率", 属性);
            assertEquals(0.2, 结果, 0.001);
        }
    }

    @Nested
    @DisplayName("修饰后属性计算")
    class 修饰后属性计算 {

        @Test
        @DisplayName("无修饰器应返回基础属性")
        void 无修饰器_应返回基础属性() {
            属性快照 基础 = 创建基础属性(1000, 1.5, 0, 10, 10, 10);

            属性快照 结果 = 服务.计算修饰后属性(玩家标识, 基础);

            assertEquals(1000, 结果.生命值上限());
            assertEquals(1.5, 结果.公共冷却时间());
            assertEquals(0, 结果.急速());
            assertEquals(10, 结果.力量());
        }

        @Test
        @DisplayName("有修饰器应返回修饰后属性")
        void 有修饰器_应返回修饰后属性() {
            属性快照 基础 = 创建基础属性(1000, 1.5, 0, 10, 10, 10);
            修饰器管理器.注册修饰器(玩家标识,
                    new 修饰器("生命值上限", "buff1", 修饰方式.相加, 500, 0));
            修饰器管理器.注册修饰器(玩家标识,
                    new 修饰器("急速", "buff2", 修饰方式.相加, 20, 0));

            属性快照 结果 = 服务.计算修饰后属性(玩家标识, 基础);

            assertEquals(1500, 结果.生命值上限());
            assertEquals(20, 结果.急速());
        }

        @Test
        @DisplayName("计算玩家快照应返回快照属性（透传）")
        void 计算玩家快照_应返回快照属性() {
            属性快照 基础 = 创建基础属性(1000, 1.5, 0, 10, 10, 10);
            玩家快照 快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, 基础, Map.of(), null);

            属性快照 结果 = 服务.计算(快照);

            assertEquals(1000, 结果.生命值上限());
            assertEquals(1.5, 结果.公共冷却时间());
        }

        @Test
        @DisplayName("计算玩家快照应透传不双重应用修饰器（FP-05修复）")
        void 计算玩家快照_有修饰器应透传不叠加() {
            属性快照 最终属性 = 创建基础属性(1500, 1.2, 20, 10, 10, 10);
            玩家快照 快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, 最终属性, Map.of(), null);
            修饰器管理器.注册修饰器(玩家标识,
                    new 修饰器("生命值上限", "buff1", 修饰方式.相加, 500, 0));

            属性快照 结果 = 服务.计算(快照);

            assertEquals(1500, 结果.生命值上限(), "透传快照属性，不应叠加修饰器");
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("递减边界值20应正确处理")
        void 递减边界20_应正确处理() {
            assertEquals(20, 服务.应用通用递减(20));
        }

        @Test
        @DisplayName("递减边界值30应正确处理")
        void 递减边界30_应正确处理() {
            assertEquals(28, 服务.应用通用递减(30));
        }

        @Test
        @DisplayName("递减边界值40应正确处理")
        void 递减边界40_应正确处理() {
            assertEquals(34, 服务.应用通用递减(40));
        }

        @Test
        @DisplayName("递减边界值41应正确处理")
        void 递减边界41_应正确处理() {
            // 20*1.0 + 10*0.8 + 10*0.6 + 1*0.4 = 34.4
            assertEquals(34.4, 服务.应用通用递减(41), 0.001);
        }
    }

    @ParameterizedTest(name = "属性值={0}时递减结果应为{1}")
    @CsvSource({
            "0, 0",
            "10, 10",
            "20, 20",
            "25, 24",
            "30, 28",
            "35, 31",
            "40, 34",
            "50, 38",
            "100, 58"
    })
    void 参数化通用递减(double 属性值, double 期望) {
        assertEquals(期望, 服务.应用通用递减(属性值), 0.001);
    }

    private 属性快照 创建基础属性(double 生命值上限, double 公共冷却, double 急速,
                                  double 力量, double 敏捷, double 智力) {
        return 属性快照.创建(
                生命值上限, 公共冷却, 急速,
                力量, 敏捷, 智力,
                力量, 敏捷, 智力,
                0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    private 属性快照 创建暴击属性(double 法术暴击几率, double 法术暴击伤害) {
        return 属性快照.创建(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                法术暴击几率, 法术暴击伤害,
                0, 0, 0, 0, 0, 0
        );
    }

    private 属性快照 创建全能属性(double 全能) {
        return 属性快照.创建(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 全能, 0, 0, 0, 0
        );
    }

    private 属性快照 创建吸血属性(double 吸血) {
        return 属性快照.创建(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 吸血, 0, 0, 0
        );
    }

    private 属性快照 创建躲闪属性(double 躲闪) {
        return 属性快照.创建(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 躲闪, 0, 0
        );
    }

    @Nested
    @DisplayName("ERR-19: 派生计算器注册表线程安全")
    class 派生计算器注册表线程安全 {

        @Test
        @DisplayName("并发注册和计算派生值应不抛ConcurrentModificationException")
        void 并发注册和计算_应不抛异常() throws Exception {
            int 线程数 = 20;
            int 迭代数 = 500;
            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        属性快照 属性 = 创建基础属性(0, 0, 0, 0, 0, 0);
                        for (int j = 0; j < 迭代数; j++) {
                            if (索引 % 2 == 0) {
                                final String 名 = "自定义" + 索引 + "_" + j;
                                派生属性计算器 计算器 = new 派生属性计算器() {
                                    @Override
                                    public String 派生名() {
                                        return 名;
                                    }

                                    @Override
                                    public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
                                        return 0;
                                    }
                                };
                                服务.注册派生计算器(计算器);
                            } else {
                                服务.计算派生值("全能增伤比例", 属性);
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
            assertNull(异常引用.get(), "并发注册和计算不应抛异常: " + 异常引用.get());
        }
    }
}
