package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合奏状态领域对象测试（record）。
 * 验证组件访问器、equals/hashCode、toString、null 与边界值。
 */
@DisplayName("合奏状态")
class 合奏状态测试 {

    @Nested
    @DisplayName("创建与访问器")
    class 创建与访问器测试 {

        @Test
        @DisplayName("创建典型合奏状态应保留所有组件")
        void 创建典型状态应保留组件() {
            合奏状态 状态 = new 合奏状态("月光奏鸣曲", 120, true, 3, 4);
            assertEquals("月光奏鸣曲", 状态.曲目名());
            assertEquals(120, 状态.BPM());
            assertTrue(状态.是否演奏中());
            assertEquals(3, 状态.已准备人数());
            assertEquals(4, 状态.总人数());
        }

        @Test
        @DisplayName("未演奏状态应正确记录")
        void 未演奏状态() {
            合奏状态 状态 = new 合奏状态("小星星", 90, false, 1, 4);
            assertFalse(状态.是否演奏中());
            assertEquals(1, 状态.已准备人数());
        }

        @Test
        @DisplayName("全员已准备状态")
        void 全员已准备() {
            合奏状态 状态 = new 合奏状态("卡农", 100, false, 4, 4);
            assertEquals(4, 状态.已准备人数());
            assertEquals(4, 状态.总人数());
            assertEquals(状态.已准备人数(), 状态.总人数());
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值测试 {

        @Test
        @DisplayName("BPM 为 0 应被接受")
        void bpm0应被接受() {
            合奏状态 状态 = new 合奏状态("曲目", 0, false, 0, 4);
            assertEquals(0, 状态.BPM());
        }

        @Test
        @DisplayName("已准备人数为 0 应被接受")
        void 已准备0应被接受() {
            合奏状态 状态 = new 合奏状态("曲目", 120, false, 0, 4);
            assertEquals(0, 状态.已准备人数());
        }

        @Test
        @DisplayName("总人数为 0 应被接受")
        void 总人数0应被接受() {
            合奏状态 状态 = new 合奏状态("曲目", 120, false, 0, 0);
            assertEquals(0, 状态.总人数());
        }

        @Test
        @DisplayName("曲目名为 null 应被接受（record 允许）")
        void 曲目名null应被接受() {
            合奏状态 状态 = new 合奏状态(null, 120, false, 0, 4);
            assertNull(状态.曲目名());
        }

        @Test
        @DisplayName("曲目名为空字符串应被接受")
        void 曲目名空字符串应被接受() {
            合奏状态 状态 = new 合奏状态("", 120, false, 0, 4);
            assertEquals("", 状态.曲目名());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class equals与hashCode测试 {

        @Test
        @DisplayName("相同组件的两个对象应相等")
        void 相同组件应相等() {
            合奏状态 a = new 合奏状态("曲目", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目", 120, true, 2, 4);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("自身 equals 应为 true")
        void 自身equals应为true() {
            合奏状态 状态 = new 合奏状态("曲目", 120, true, 2, 4);
            assertEquals(状态, 状态);
        }

        @Test
        @DisplayName("曲目名不同应不相等")
        void 曲目名不同应不相等() {
            合奏状态 a = new 合奏状态("曲目A", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目B", 120, true, 2, 4);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("BPM不同应不相等")
        void bpm不同应不相等() {
            合奏状态 a = new 合奏状态("曲目", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目", 90, true, 2, 4);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("是否演奏中不同应不相等")
        void 演奏状态不同应不相等() {
            合奏状态 a = new 合奏状态("曲目", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目", 120, false, 2, 4);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("已准备人数不同应不相等")
        void 已准备人数不同应不相等() {
            合奏状态 a = new 合奏状态("曲目", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目", 120, true, 3, 4);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("总人数不同应不相等")
        void 总人数不同应不相等() {
            合奏状态 a = new 合奏状态("曲目", 120, true, 2, 4);
            合奏状态 b = new 合奏状态("曲目", 120, true, 2, 5);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应为 false")
        void 与null比较应为false() {
            合奏状态 状态 = new 合奏状态("曲目", 120, true, 2, 4);
            assertNotEquals(状态, null);
        }

        @Test
        @DisplayName("与非合奏状态类型比较应为 false")
        void 与非同类型比较应为false() {
            合奏状态 状态 = new 合奏状态("曲目", 120, true, 2, 4);
            assertNotEquals(状态, "曲目");
        }
    }

    @Nested
    @DisplayName("toString")
    class toString测试 {

        @Test
        @DisplayName("toString 应包含所有组件信息")
        void toString应包含组件() {
            合奏状态 状态 = new 合奏状态("月光", 120, true, 2, 4);
            String 文本 = 状态.toString();
            assertNotNull(文本);
            assertTrue(文本.contains("月光"), "toString 应包含曲目名: " + 文本);
            assertTrue(文本.contains("120"), "toString 应包含 BPM: " + 文本);
            assertTrue(文本.contains("2"), "toString 应包含已准备人数: " + 文本);
            assertTrue(文本.contains("4"), "toString 应包含总人数: " + 文本);
        }
    }
}
