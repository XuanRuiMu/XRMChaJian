package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 音符领域对象测试。
 * 验证音高（0-24）/ 时值（tick）/ 力度（0.0-1.0）三参数的校验、工厂方法、八度音阶计算、equals/hashCode/toString。
 */
@DisplayName("音符")
class 音符测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("创建典型音符应保留三个参数")
        void 创建典型音符应保留参数() {
            音符 音 = new 音符(12, 4, 0.75);
            assertEquals(12, 音.获取音高());
            assertEquals(4, 音.获取时值Tick());
            assertEquals(0.75, 音.获取力度());
        }

        @Test
        @DisplayName("of 工厂方法应等价于构造器")
        void of工厂方法应等价() {
            音符 音 = 音符.of(6, 8, 0.5);
            assertEquals(6, 音.获取音高());
            assertEquals(8, 音.获取时值Tick());
            assertEquals(0.5, 音.获取力度());
        }

        @Test
        @DisplayName("简单 工厂方法应使用默认时值4和力度1.0")
        void 简单工厂方法应使用默认值() {
            音符 音 = 音符.简单(10);
            assertEquals(10, 音.获取音高());
            assertEquals(4, 音.获取时值Tick(), "简单工厂方法默认时值应为 4");
            assertEquals(1.0, 音.获取力度(), "简单工厂方法默认力度应为 1.0");
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("音高下限 0 应被接受")
        void 音高下限0应被接受() {
            音符 音 = new 音符(0, 4, 1.0);
            assertEquals(0, 音.获取音高());
        }

        @Test
        @DisplayName("音高上限 24 应被接受")
        void 音高上限24应被接受() {
            音符 音 = new 音符(24, 4, 1.0);
            assertEquals(24, 音.获取音高());
        }

        @Test
        @DisplayName("时值Tick 为 0 应被接受")
        void 时值零应被接受() {
            音符 音 = new 音符(0, 0, 1.0);
            assertEquals(0, 音.获取时值Tick());
        }

        @Test
        @DisplayName("力度下限 0.0 应被接受")
        void 力度下限应被接受() {
            音符 音 = new 音符(0, 4, 0.0);
            assertEquals(0.0, 音.获取力度());
        }

        @Test
        @DisplayName("力度上限 1.0 应被接受")
        void 力度上限应被接受() {
            音符 音 = new 音符(0, 4, 1.0);
            assertEquals(1.0, 音.获取力度());
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("音高 -1 应抛 IllegalArgumentException")
        void 音高负一应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> new 音符(-1, 4, 1.0));
        }

        @Test
        @DisplayName("音高 25 应抛 IllegalArgumentException")
        void 音高25应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> new 音符(25, 4, 1.0));
        }

        @Test
        @DisplayName("时值Tick -1 应抛 IllegalArgumentException")
        void 时值负一应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> new 音符(0, -1, 1.0));
        }

        @Test
        @DisplayName("力度 -0.01 应抛 IllegalArgumentException")
        void 力度低于零应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> new 音符(0, 4, -0.01));
        }

        @Test
        @DisplayName("力度 1.01 应抛 IllegalArgumentException")
        void 力度超过一应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> new 音符(0, 4, 1.01));
        }

        @Test
        @DisplayName("简单 工厂方法对越界音高也应抛异常")
        void 简单工厂越界应抛异常() {
            assertThrows(IllegalArgumentException.class, () -> 音符.简单(25));
        }
    }

    @Nested
    @DisplayName("八度与音阶")
    class 八度音阶测试 {

        @Test
        @DisplayName("音高0 八度=0 音阶=0")
        void 音高0八度音阶() {
            音符 音 = new 音符(0, 4, 1.0);
            assertEquals(0, 音.获取八度());
            assertEquals(0, 音.获取音阶());
        }

        @Test
        @DisplayName("音高6 八度=0 音阶=6")
        void 音高6八度音阶() {
            音符 音 = new 音符(6, 4, 1.0);
            assertEquals(0, 音.获取八度());
            assertEquals(6, 音.获取音阶());
        }

        @Test
        @DisplayName("音高11 八度=0 音阶=11")
        void 音高11八度音阶() {
            音符 音 = new 音符(11, 4, 1.0);
            assertEquals(0, 音.获取八度());
            assertEquals(11, 音.获取音阶());
        }

        @Test
        @DisplayName("音高12 八度=1 音阶=0")
        void 音高12八度音阶() {
            音符 音 = new 音符(12, 4, 1.0);
            assertEquals(1, 音.获取八度());
            assertEquals(0, 音.获取音阶());
        }

        @Test
        @DisplayName("音高13 八度=1 音阶=1")
        void 音高13八度音阶() {
            音符 音 = new 音符(13, 4, 1.0);
            assertEquals(1, 音.获取八度());
            assertEquals(1, 音.获取音阶());
        }

        @Test
        @DisplayName("音高24 八度=2 音阶=0")
        void 音高24八度音阶() {
            音符 音 = new 音符(24, 4, 1.0);
            assertEquals(2, 音.获取八度());
            assertEquals(0, 音.获取音阶());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class equals与hashCode测试 {

        @Test
        @DisplayName("相同三参数的两个音符应相等")
        void 相同参数应相等() {
            音符 a = new 音符(12, 4, 0.5);
            音符 b = new 音符(12, 4, 0.5);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("自身 equals 应为 true")
        void 自身equals应为true() {
            音符 音 = new 音符(12, 4, 0.5);
            assertEquals(音, 音);
        }

        @Test
        @DisplayName("音高不同应不相等")
        void 音高不同应不相等() {
            音符 a = new 音符(12, 4, 0.5);
            音符 b = new 音符(13, 4, 0.5);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("时值不同应不相等")
        void 时值不同应不相等() {
            音符 a = new 音符(12, 4, 0.5);
            音符 b = new 音符(12, 5, 0.5);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("力度不同应不相等")
        void 力度不同应不相等() {
            音符 a = new 音符(12, 4, 0.5);
            音符 b = new 音符(12, 4, 0.6);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应为 false")
        void 与null比较应为false() {
            音符 音 = new 音符(12, 4, 0.5);
            assertNotEquals(音, null);
        }

        @Test
        @DisplayName("与非音符类型比较应为 false")
        void 与非音符类型比较应为false() {
            音符 音 = new 音符(12, 4, 0.5);
            assertNotEquals(音, "音符");
        }
    }

    @Nested
    @DisplayName("toString")
    class toString测试 {

        @Test
        @DisplayName("toString 应包含音高、时值、力度信息")
        void toString应包含字段() {
            音符 音 = new 音符(12, 4, 0.5);
            String 文本 = 音.toString();
            assertTrue(文本.contains("12"), "toString 应包含音高 12: " + 文本);
            assertTrue(文本.contains("4"), "toString 应包含时值 4: " + 文本);
            assertTrue(文本.contains("0.5"), "toString 应包含力度 0.5: " + 文本);
        }
    }
}
