package mljy.业务层.属性;

import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("修饰器管理器")
class 修饰器管理器测试 {

    private 修饰器管理器 管理器;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        管理器 = new 修饰器管理器();
        玩家标识 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("注册修饰器后应能查询到")
        void 注册修饰器_应能查询到() {
            修饰器 项 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            管理器.注册修饰器(玩家标识, 项);

            List<修饰器> 结果 = 管理器.查询修饰器(玩家标识, "攻击力");

            assertEquals(1, 结果.size());
            assertEquals(项, 结果.get(0));
        }

        @Test
        @DisplayName("注册多个同类修饰器应全部查询到")
        void 注册多个同类修饰器_应全部查询到() {
            修饰器 项1 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            修饰器 项2 = new 修饰器("攻击力", "天赋_2", 修饰方式.相加, 50, 0);

            管理器.注册修饰器(玩家标识, 项1);
            管理器.注册修饰器(玩家标识, 项2);

            List<修饰器> 结果 = 管理器.查询修饰器(玩家标识, "攻击力");
            assertEquals(2, 结果.size());
        }

        @Test
        @DisplayName("按类别注销修饰器应移除对应标签")
        void 按类别注销修饰器_应移除对应标签() {
            修饰器 项1 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            修饰器 项2 = new 修饰器("攻击力", "天赋_2", 修饰方式.相加, 50, 0);
            管理器.注册修饰器(玩家标识, 项1);
            管理器.注册修饰器(玩家标识, 项2);

            管理器.注销修饰器(玩家标识, "攻击力", "天赋_1");

            List<修饰器> 结果 = 管理器.查询修饰器(玩家标识, "攻击力");
            assertEquals(1, 结果.size());
            assertEquals("天赋_2", 结果.get(0).标签());
        }

        @Test
        @DisplayName("按来源注销修饰器应跨类别移除")
        void 按来源注销修饰器_应跨类别移除() {
            修饰器 项1 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            修饰器 项2 = new 修饰器("暴击", "天赋_1", 修饰方式.相加, 10, 0);
            修饰器 项3 = new 修饰器("攻击力", "天赋_2", 修饰方式.相加, 50, 0);
            管理器.注册修饰器(玩家标识, 项1);
            管理器.注册修饰器(玩家标识, 项2);
            管理器.注册修饰器(玩家标识, 项3);

            管理器.注销来源修饰器(玩家标识, "天赋_1");

            List<修饰器> 攻击力结果 = 管理器.查询修饰器(玩家标识, "攻击力");
            List<修饰器> 暴击结果 = 管理器.查询修饰器(玩家标识, "暴击");
            assertEquals(1, 攻击力结果.size());
            assertEquals("天赋_2", 攻击力结果.get(0).标签());
            assertTrue(暴击结果.isEmpty());
        }

        @Test
        @DisplayName("按来源查询修饰器应返回所有类别中匹配标签的修饰器")
        void 按来源查询修饰器_应返回所有类别匹配() {
            修饰器 项1 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            修饰器 项2 = new 修饰器("暴击", "天赋_1", 修饰方式.相加, 10, 0);
            修饰器 项3 = new 修饰器("攻击力", "天赋_2", 修饰方式.相加, 50, 0);
            管理器.注册修饰器(玩家标识, 项1);
            管理器.注册修饰器(玩家标识, 项2);
            管理器.注册修饰器(玩家标识, 项3);

            List<修饰器> 结果 = 管理器.按来源查询修饰器(玩家标识, "天赋_1");

            assertEquals(2, 结果.size());
        }

        @Test
        @DisplayName("清空玩家修饰器应移除所有数据")
        void 清空玩家修饰器_应移除所有数据() {
            修饰器 项 = new 修饰器("攻击力", "天赋_1", 修饰方式.相加, 100, 0);
            管理器.注册修饰器(玩家标识, 项);

            管理器.清空玩家修饰器(玩家标识);

            assertTrue(管理器.查询修饰器(玩家标识, "攻击力").isEmpty());
        }
    }

    @Nested
    @DisplayName("计算最终值")
    class 计算最终值 {

        @Test
        @DisplayName("无修饰器时应返回基础值")
        void 无修饰器_应返回基础值() {
            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);
            assertEquals(100, 结果);
        }

        @Test
        @DisplayName("相加修饰器应累加到基础值")
        void 相加修饰器_应累加到基础值() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, 50, 0));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.相加, 30, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(180, 结果);
        }

        @Test
        @DisplayName("相乘修饰器应累乘")
        void 相乘修饰器_应累乘() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相乘, 1.5, 0));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.相乘, 2.0, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(300, 结果);
        }

        @Test
        @DisplayName("覆盖修饰器应替换基础值")
        void 覆盖修饰器_应替换基础值() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.覆盖, 200, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(200, 结果);
        }

        @Test
        @DisplayName("多个覆盖修饰器应取优先级最高的")
        void 多个覆盖修饰器_应取优先级最高的() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.覆盖, 200, 1));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.覆盖, 300, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(300, 结果);
        }

        @Test
        @DisplayName("相加和相乘混合时应先加后乘")
        void 相加和相乘混合_应先加后乘() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, 50, 0));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.相乘, 2.0, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(300, 结果);
        }

        @Test
        @DisplayName("覆盖与相乘混合时应覆盖后乘")
        void 覆盖与相乘混合_应覆盖后乘() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.覆盖, 200, 0));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.相乘, 1.5, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(300, 结果);
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("修饰器数值为零时应正确处理")
        void 修饰器数值为零_应正确处理() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, 0, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(100, 结果);
        }

        @Test
        @DisplayName("基础值为零时应正确处理")
        void 基础值为零_应正确处理() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, 50, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 0);

            assertEquals(50, 结果);
        }

        @Test
        @DisplayName("负数修饰器应正确处理")
        void 负数修饰器_应正确处理() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, -30, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(70, 结果);
        }

        @Test
        @DisplayName("超大数值修饰器应正确处理")
        void 超大数值修饰器_应正确处理() {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, Double.MAX_VALUE, 0));

            double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 100);

            assertEquals(Double.MAX_VALUE + 100, 结果);
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("查询不存在玩家的修饰器应返回空列表")
        void 查询不存在玩家_应返回空列表() {
            List<修饰器> 结果 = 管理器.查询修饰器(UUID.randomUUID(), "攻击力");
            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("查询不存在的类别应返回空列表")
        void 查询不存在类别_应返回空列表() {
            修饰器 项 = new 修饰器("攻击力", "t1", 修饰方式.相加, 100, 0);
            管理器.注册修饰器(玩家标识, 项);

            List<修饰器> 结果 = 管理器.查询修饰器(玩家标识, "防御力");
            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("注销不存在玩家的修饰器不应抛出异常")
        void 注销不存在玩家_不应抛出异常() {
            assertDoesNotThrow(() -> 管理器.注销修饰器(UUID.randomUUID(), "攻击力", "t1"));
        }

        @Test
        @DisplayName("按来源注销不存在玩家修饰器不应抛出异常")
        void 按来源注销不存在玩家_不应抛出异常() {
            assertDoesNotThrow(() -> 管理器.注销来源修饰器(UUID.randomUUID(), "t1"));
        }

        @Test
        @DisplayName("计算不存在玩家修饰器最终值应返回基础值")
        void 计算不存在玩家_应返回基础值() {
            double 结果 = 管理器.计算最终值(UUID.randomUUID(), "攻击力", 100);
            assertEquals(100, 结果);
        }

        @Test
        @DisplayName("清空不存在玩家修饰器不应抛出异常")
        void 清空不存在玩家_不应抛出异常() {
            assertDoesNotThrow(() -> 管理器.清空玩家修饰器(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("并发安全")
    class 并发安全 {

        @Test
        @DisplayName("多线程同时注册修饰器应全部成功")
        void 多线程注册_应全部成功() throws InterruptedException {
            int 线程数 = 10;
            int 每线程注册数 = 100;
            Thread[] 线程数组 = new Thread[线程数];

            for (int i = 0; i < 线程数; i++) {
                final int 线程序号 = i;
                线程数组[i] = new Thread(() -> {
                    for (int j = 0; j < 每线程注册数; j++) {
                        管理器.注册修饰器(玩家标识,
                                new 修饰器("攻击力", "t" + 线程序号 + "_" + j, 修饰方式.相加, 1, 0));
                    }
                });
                线程数组[i].start();
            }

            for (Thread t : 线程数组) {
                t.join();
            }

            List<修饰器> 结果 = 管理器.查询修饰器(玩家标识, "攻击力");
            assertEquals(线程数 * 每线程注册数, 结果.size());
        }

        @Test
        @DisplayName("多线程同时计算最终值应返回一致结果")
        void 多线程计算_应返回一致结果() throws InterruptedException {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t1", 修饰方式.相加, 100, 0));
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "t2", 修饰方式.相乘, 2.0, 0));

            int 线程数 = 10;
            Thread[] 线程数组 = new Thread[线程数];
            double[] 结果数组 = new double[线程数];

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                线程数组[i] = new Thread(() -> {
                    结果数组[索引] = 管理器.计算最终值(玩家标识, "攻击力", 100);
                });
                线程数组[i].start();
            }

            for (Thread t : 线程数组) {
                t.join();
            }

            for (int i = 0; i < 线程数; i++) {
                assertEquals(400, 结果数组[i], "线程" + i + "结果不一致");
            }
        }
    }

    @ParameterizedTest(name = "基础值={0}, 加法={1}, 乘法={2}, 期望={3}")
    @CsvSource({
            "100, 50, 1.0, 150",
            "100, 0, 2.0, 200",
            "100, 50, 2.0, 300",
            "0, 50, 2.0, 100",
            "100, -30, 1.0, 70"
    })
    void 参数化计算最终值_相加相乘(double 基础值, double 加法, double 乘法, double 期望) {
        if (加法 != 0) {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "add", 修饰方式.相加, 加法, 0));
        }
        if (乘法 != 1.0) {
            管理器.注册修饰器(玩家标识, new 修饰器("攻击力", "mul", 修饰方式.相乘, 乘法, 0));
        }

        double 结果 = 管理器.计算最终值(玩家标识, "攻击力", 基础值);
        assertEquals(期望, 结果);
    }
}
