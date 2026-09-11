package mljy.领域层.属性;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("属性快照")
class 属性快照测试 {

    @Nested
    @DisplayName("静态工厂创建")
    class 静态工厂创建 {

        @Test
        @DisplayName("创建工厂应返回字段值正确的快照")
        void 创建_应返回正确字段值() {
            属性快照 属性 = 属性快照.创建(
                    1000, 1.5, 10,
                    50, 40, 30,
                    20, 15, 10,
                    50, 100, 30,
                    40, 15, 8, 5, 3
            );

            assertEquals(1000, 属性.生命值上限());
            assertEquals(1.5, 属性.公共冷却时间());
            assertEquals(10, 属性.急速());
            assertEquals(50, 属性.力量());
            assertEquals(40, 属性.敏捷());
            assertEquals(30, 属性.智力());
            assertEquals(20, 属性.基础力量());
            assertEquals(15, 属性.基础敏捷());
            assertEquals(10, 属性.基础智力());
            assertEquals(50, 属性.法术暴击几率());
            assertEquals(100, 属性.法术暴击伤害());
            assertEquals(30, 属性.精通());
            assertEquals(40, 属性.全能());
            assertEquals(15, 属性.吸血());
            assertEquals(8, 属性.躲闪());
            assertEquals(5, 属性.生命恢复());
            assertEquals(3, 属性.移速());
        }

        @Test
        @DisplayName("创建工厂应初始化空扩展属性")
        void 创建_应初始化空扩展属性() {
            属性快照 属性 = 属性快照.创建(
                    1000, 1.5, 10,
                    50, 40, 30,
                    20, 15, 10,
                    50, 100, 30,
                    40, 15, 8, 5, 3
            );

            assertTrue(属性.扩展属性().isEmpty());
        }

        @Test
        @DisplayName("创建工厂与18参数构造(传Map.of)结果应等价")
        void 创建_应等价于18参数构造空Map() {
            属性快照 工厂结果 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );
            属性快照 直接构造 = new 属性快照(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2, Map.of()
            );

            assertEquals(直接构造, 工厂结果);
        }
    }

    @Nested
    @DisplayName("compact constructor null安全")
    class CompactConstructorNull安全 {

        @Test
        @DisplayName("构造时传入null扩展属性应替换为空Map")
        void 构造_null扩展属性_应替换为空Map() {
            属性快照 属性 = new 属性快照(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2, null
            );

            assertNotNull(属性.扩展属性());
            assertTrue(属性.扩展属性().isEmpty());
        }

        @Test
        @DisplayName("构造时传入非空扩展属性应做不可变拷贝")
        void 构造_非空扩展属性_应做不可变拷贝() {
            Map<String, Double> 可变Map = new HashMap<>();
            可变Map.put("反伤", 30.0);

            属性快照 属性 = new 属性快照(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2, 可变Map
            );

            // 修改原Map不应影响快照内的扩展属性
            可变Map.put("反伤", 999.0);
            assertEquals(30.0, 属性.扩展属性().get("反伤"));

            // 快照内的扩展属性应为不可变
            assertThrows(UnsupportedOperationException.class, () -> 属性.扩展属性().put("新增", 1.0));
        }
    }

    @Nested
    @DisplayName("带扩展属性方法")
    class 带扩展属性方法 {

        @Test
        @DisplayName("带扩展属性应返回包含新扩展属性的新实例")
        void 带扩展属性_应返回新实例() {
            属性快照 原始 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            属性快照 带扩展 = 原始.带扩展属性("反伤", 50.0);

            assertNotSame(原始, 带扩展);
            assertEquals(50.0, 带扩展.获取扩展属性("反伤"));
            assertEquals(0.0, 原始.获取扩展属性("反伤"));
        }

        @Test
        @DisplayName("带扩展属性应保留所有标准字段不变")
        void 带扩展属性_应保留标准字段不变() {
            属性快照 原始 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            属性快照 带扩展 = 原始.带扩展属性("反伤", 50.0);

            assertEquals(原始.生命值上限(), 带扩展.生命值上限());
            assertEquals(原始.法术暴击几率(), 带扩展.法术暴击几率());
            assertEquals(原始.法术暴击伤害(), 带扩展.法术暴击伤害());
            assertEquals(原始.全能(), 带扩展.全能());
            assertEquals(原始.移速(), 带扩展.移速());
        }

        @Test
        @DisplayName("带扩展属性重复调用同一键应累加替换")
        void 带扩展属性_重复调用同一键_应替换() {
            属性快照 原始 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            属性快照 第一次 = 原始.带扩展属性("反伤", 30.0);
            属性快照 第二次 = 第一次.带扩展属性("反伤", 60.0);

            assertEquals(60.0, 第二次.获取扩展属性("反伤"));
        }

        @Test
        @DisplayName("带扩展属性支持多个不同键")
        void 带扩展属性_支持多键() {
            属性快照 原始 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            属性快照 多扩展 = 原始
                    .带扩展属性("反伤", 30.0)
                    .带扩展属性("额外穿透", 15.0);

            assertEquals(30.0, 多扩展.获取扩展属性("反伤"));
            assertEquals(15.0, 多扩展.获取扩展属性("额外穿透"));
            assertEquals(2, 多扩展.扩展属性().size());
        }

        @Test
        @DisplayName("带扩展属性返回的新扩展Map应为不可变")
        void 带扩展属性_返回不可变Map() {
            属性快照 原始 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            属性快照 带扩展 = 原始.带扩展属性("反伤", 30.0);

            assertThrows(UnsupportedOperationException.class, () -> 带扩展.扩展属性().put("x", 1.0));
        }
    }

    @Nested
    @DisplayName("获取属性值方法")
    class 获取属性值方法 {

        @Test
        @DisplayName("获取属性值应正确返回17项标准属性")
        void 获取属性值_应返回17项标准属性() {
            属性快照 属性 = 属性快照.创建(
                    1000, 1.5, 10,
                    50, 40, 30,
                    20, 15, 10,
                    50, 100, 30,
                    40, 15, 8, 5, 3
            );

            assertEquals(1000, 属性.获取属性值("生命值上限"));
            assertEquals(1.5, 属性.获取属性值("公共冷却时间"));
            assertEquals(10, 属性.获取属性值("急速"));
            assertEquals(50, 属性.获取属性值("力量"));
            assertEquals(40, 属性.获取属性值("敏捷"));
            assertEquals(30, 属性.获取属性值("智力"));
            assertEquals(20, 属性.获取属性值("基础力量"));
            assertEquals(15, 属性.获取属性值("基础敏捷"));
            assertEquals(10, 属性.获取属性值("基础智力"));
            assertEquals(50, 属性.获取属性值("法术暴击几率"));
            assertEquals(100, 属性.获取属性值("法术暴击伤害"));
            assertEquals(30, 属性.获取属性值("精通"));
            assertEquals(40, 属性.获取属性值("全能"));
            assertEquals(15, 属性.获取属性值("吸血"));
            assertEquals(8, 属性.获取属性值("躲闪"));
            assertEquals(5, 属性.获取属性值("生命恢复"));
            assertEquals(3, 属性.获取属性值("移速"));
        }

        @Test
        @DisplayName("获取属性值对扩展属性应回退到扩展Map")
        void 获取属性值_扩展属性_应回退到扩展Map() {
            属性快照 属性 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            ).带扩展属性("反伤", 45.0);

            assertEquals(45.0, 属性.获取属性值("反伤"));
        }

        @Test
        @DisplayName("获取属性值对未注册的类别应返回0")
        void 获取属性值_未注册类别_应返回0() {
            属性快照 属性 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            assertEquals(0.0, 属性.获取属性值("不存在的属性"));
        }
    }

    @Nested
    @DisplayName("获取扩展属性方法")
    class 获取扩展属性方法 {

        @Test
        @DisplayName("获取扩展属性对已注册键应返回对应值")
        void 获取扩展属性_已注册键_应返回值() {
            属性快照 属性 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            ).带扩展属性("反伤", 25.5);

            assertEquals(25.5, 属性.获取扩展属性("反伤"));
        }

        @Test
        @DisplayName("获取扩展属性对未注册键应返回0")
        void 获取扩展属性_未注册键_应返回0() {
            属性快照 属性 = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            assertEquals(0.0, 属性.获取扩展属性("反伤"));
        }
    }

    @Nested
    @DisplayName("Record等值性")
    class Record等值性 {

        @Test
        @DisplayName("相同字段值(含空扩展)应相等")
        void 相同字段_应相等() {
            属性快照 a = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );
            属性快照 b = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("扩展属性不同应不相等")
        void 扩展属性不同_应不相等() {
            属性快照 a = 属性快照.创建(
                    100, 1.0, 5, 10, 8, 6, 4, 3, 2, 30, 60, 15, 20, 10, 5, 4, 2
            );
            属性快照 b = a.带扩展属性("反伤", 30.0);

            assertNotEquals(a, b);
        }
    }
}
