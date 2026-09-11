package mljy.基础设施层;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FP-03 数值验证器测试")
class 数值验证器测试 {

    @Nested
    @DisplayName("是有限值：判定 NaN/Infinity/正常值")
    class 是有限值判定 {

        @Test
        @DisplayName("NaN 应判定为非有限值")
        void nan应判定为非有限值() {
            assertFalse(数值验证器.是有限值(Double.NaN), "NaN 应为非有限值");
        }

        @Test
        @DisplayName("正无穷 应判定为非有限值")
        void 正无穷应判定为非有限值() {
            assertFalse(数值验证器.是有限值(Double.POSITIVE_INFINITY), "正无穷应为非有限值");
        }

        @Test
        @DisplayName("负无穷 应判定为非有限值")
        void 负无穷应判定为非有限值() {
            assertFalse(数值验证器.是有限值(Double.NEGATIVE_INFINITY), "负无穷应为非有限值");
        }

        @Test
        @DisplayName("正常值应判定为有限值")
        void 正常值应判定为有限值() {
            assertTrue(数值验证器.是有限值(100.5), "100.5 应为有限值");
        }

        @Test
        @DisplayName("零应判定为有限值")
        void 零应判定为有限值() {
            assertTrue(数值验证器.是有限值(0.0), "0.0 应为有限值");
        }

        @Test
        @DisplayName("负数应判定为有限值")
        void 负数应判定为有限值() {
            assertTrue(数值验证器.是有限值(-50.0), "-50.0 应为有限值");
        }
    }

    @Nested
    @DisplayName("清理为零：NaN/Infinity 返回 0")
    class 清理为零判定 {

        @Test
        @DisplayName("NaN 应清理为 0")
        void nan应清理为零() {
            assertEquals(0.0, 数值验证器.清理为零(Double.NaN), 0.0001, "NaN 应清理为 0");
        }

        @Test
        @DisplayName("正无穷 应清理为 0")
        void 正无穷应清理为零() {
            assertEquals(0.0, 数值验证器.清理为零(Double.POSITIVE_INFINITY), 0.0001, "正无穷应清理为 0");
        }

        @Test
        @DisplayName("正常值应保持不变")
        void 正常值应保持不变() {
            assertEquals(123.45, 数值验证器.清理为零(123.45), 0.0001, "正常值应保持不变");
        }

        @Test
        @DisplayName("零应保持为零")
        void 零应保持为零() {
            assertEquals(0.0, 数值验证器.清理为零(0.0), 0.0001, "零应保持为零");
        }

        @Test
        @DisplayName("负数应保持不变")
        void 负数应保持不变() {
            assertEquals(-99.0, 数值验证器.清理为零(-99.0), 0.0001, "负数应保持不变");
        }
    }

    @Nested
    @DisplayName("清理为默认：NaN/Infinity 返回默认值")
    class 清理为默认判定 {

        @Test
        @DisplayName("NaN 应清理为默认值")
        void nan应清理为默认值() {
            assertEquals(50.0, 数值验证器.清理为默认(Double.NaN, 50.0), 0.0001, "NaN 应清理为默认值 50.0");
        }

        @Test
        @DisplayName("正无穷 应清理为默认值")
        void 正无穷应清理为默认值() {
            assertEquals(50.0, 数值验证器.清理为默认(Double.POSITIVE_INFINITY, 50.0), 0.0001, "正无穷应清理为默认值");
        }

        @Test
        @DisplayName("负无穷 应清理为默认值")
        void 负无穷应清理为默认值() {
            assertEquals(-25.0, 数值验证器.清理为默认(Double.NEGATIVE_INFINITY, -25.0), 0.0001, "负无穷应清理为默认值");
        }

        @Test
        @DisplayName("正常值应保持不变（不取默认值）")
        void 正常值应保持不变() {
            assertEquals(88.8, 数值验证器.清理为默认(88.8, 50.0), 0.0001, "正常值应保持不变");
        }

        @Test
        @DisplayName("零应保持为零（零也是有限值）")
        void 零应保持为零() {
            assertEquals(0.0, 数值验证器.清理为默认(0.0, 50.0), 0.0001, "零应保持为零");
        }
    }

    @Nested
    @DisplayName("限制下限：小于下限返回下限")
    class 限制下限判定 {

        @Test
        @DisplayName("NaN 应返回下限")
        void nan应返回下限() {
            assertEquals(10.0, 数值验证器.限制下限(Double.NaN, 10.0), 0.0001, "NaN 应返回下限");
        }

        @Test
        @DisplayName("正无穷 应返回下限（按 WIP 实现统一处理非有限值）")
        void 正无穷应返回下限() {
            assertEquals(10.0, 数值验证器.限制下限(Double.POSITIVE_INFINITY, 10.0), 0.0001, "正无穷按 WIP 统一处理为下限");
        }

        @Test
        @DisplayName("负无穷 应返回下限")
        void 负无穷应返回下限() {
            assertEquals(10.0, 数值验证器.限制下限(Double.NEGATIVE_INFINITY, 10.0), 0.0001, "负无穷应返回下限");
        }

        @Test
        @DisplayName("小于下限应返回下限")
        void 小于下限应返回下限() {
            assertEquals(10.0, 数值验证器.限制下限(5.0, 10.0), 0.0001, "5.0 小于下限 10.0 应返回 10.0");
        }

        @Test
        @DisplayName("等于下限应返回下限")
        void 等于下限应返回下限() {
            assertEquals(10.0, 数值验证器.限制下限(10.0, 10.0), 0.0001, "等于下限应返回下限");
        }

        @Test
        @DisplayName("大于下限应返回原值")
        void 大于下限应返回原值() {
            assertEquals(99.0, 数值验证器.限制下限(99.0, 10.0), 0.0001, "大于下限应返回原值");
        }
    }

    @Nested
    @DisplayName("限制范围：超出范围返回边界值")
    class 限制范围判定 {

        @Test
        @DisplayName("NaN 应返回下限")
        void nan应返回下限() {
            assertEquals(10.0, 数值验证器.限制范围(Double.NaN, 10.0, 100.0), 0.0001, "NaN 应返回下限");
        }

        @Test
        @DisplayName("正无穷 应返回下限（按 WIP 实现统一处理非有限值）")
        void 正无穷应返回下限() {
            assertEquals(10.0, 数值验证器.限制范围(Double.POSITIVE_INFINITY, 10.0, 100.0), 0.0001, "正无穷按 WIP 统一处理为下限");
        }

        @Test
        @DisplayName("负无穷 应返回下限")
        void 负无穷应返回下限() {
            assertEquals(10.0, 数值验证器.限制范围(Double.NEGATIVE_INFINITY, 10.0, 100.0), 0.0001, "负无穷应返回下限");
        }

        @Test
        @DisplayName("小于下限应返回下限")
        void 小于下限应返回下限() {
            assertEquals(10.0, 数值验证器.限制范围(5.0, 10.0, 100.0), 0.0001, "5.0 小于下限 10.0 应返回 10.0");
        }

        @Test
        @DisplayName("大于上限应返回上限")
        void 大于上限应返回上限() {
            assertEquals(100.0, 数值验证器.限制范围(150.0, 10.0, 100.0), 0.0001, "150.0 大于上限 100.0 应返回 100.0");
        }

        @Test
        @DisplayName("范围内应返回原值")
        void 范围内应返回原值() {
            assertEquals(50.0, 数值验证器.限制范围(50.0, 10.0, 100.0), 0.0001, "范围内应返回原值");
        }

        @Test
        @DisplayName("等于下限应返回下限")
        void 等于下限应返回下限() {
            assertEquals(10.0, 数值验证器.限制范围(10.0, 10.0, 100.0), 0.0001, "等于下限应返回下限");
        }

        @Test
        @DisplayName("等于上限应返回上限")
        void 等于上限应返回上限() {
            assertEquals(100.0, 数值验证器.限制范围(100.0, 10.0, 100.0), 0.0001, "等于上限应返回上限");
        }

        @Test
        @DisplayName("上限小于下限时应返回下限（按 WIP 实现）")
        void 上限小于下限时应返回下限() {
            assertEquals(100.0, 数值验证器.限制范围(50.0, 100.0, 10.0), 0.0001, "上限<下限时按 WIP 实现返回下限");
        }
    }
}
