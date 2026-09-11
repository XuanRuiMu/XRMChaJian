package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-11 和弦预设服务测试。
 * 验证按根音 + 和弦类型生成实际音高列表的 clamp 行为。
 */
@DisplayName("FP-11: 和弦预设")
class 和弦预设测试 {

    @Nested
    @DisplayName("生成和弦 - 正常音域")
    class 正常音域测试 {

        @Test
        @DisplayName("大三根音 0 音域 [0, 24] 应返回 [0, 4, 7]")
        void 大三根音零正常() {
            List<Integer> 结果 = 和弦预设.生成和弦(0, 和弦类型.大三, 0, 24);
            assertEquals(List.of(0, 4, 7), 结果);
        }

        @Test
        @DisplayName("大三根音 12 音域 [0, 24] 应返回 [12, 16, 19]")
        void 大三根音十二正常() {
            List<Integer> 结果 = 和弦预设.生成和弦(12, 和弦类型.大三, 0, 24);
            assertEquals(List.of(12, 16, 19), 结果);
        }

        @Test
        @DisplayName("属九根音 5 音域 [0, 24] 应返回 [5, 9, 12, 15, 19]")
        void 属九根音五正常() {
            List<Integer> 结果 = 和弦预设.生成和弦(5, 和弦类型.属九, 0, 24);
            assertEquals(List.of(5, 9, 12, 15, 19), 结果);
        }
    }

    @Nested
    @DisplayName("生成和弦 - 八度下移 clamp")
    class 八度下移测试 {

        @Test
        @DisplayName("大三根音 22 音域 [0, 24] 应将 26 下移到 14")
        void 大三根音高八度下移() {
            // 根音 22 → 22, 26 (越界), 29 (越界) → 22, 14, 17
            List<Integer> 结果 = 和弦预设.生成和弦(22, 和弦类型.大三, 0, 24);
            assertEquals(List.of(22, 14, 17), 结果);
        }

        @Test
        @DisplayName("属九根音 20 音域 [0, 24] 应将 30/34 下移")
        void 属九根音高八度下移() {
            // 根音 20 → 20, 24, 27 (越界), 30 (越界), 34 (越界)
            // 27 → 15, 30 → 18, 34 → 22
            List<Integer> 结果 = 和弦预设.生成和弦(20, 和弦类型.属九, 0, 24);
            assertEquals(List.of(20, 24, 15, 18, 22), 结果);
        }
    }

    @Nested
    @DisplayName("生成和弦 - 边界与异常")
    class 边界与异常测试 {

        @Test
        @DisplayName("音域下限大于上限应返回空列表")
        void 下限大于上限返回空() {
            List<Integer> 结果 = 和弦预设.生成和弦(0, 和弦类型.大三, 10, 5);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("和弦类型为 null 应返回空列表")
        void null类型返回空() {
            List<Integer> 结果 = 和弦预设.生成和弦(0, null, 0, 24);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("超出下限的音应被舍弃")
        void 超出下限舍弃() {
            // 根音 0, dim [0, 3, 6], 音域 [4, 24] → 0/3 都被舍弃，仅 6 保留
            List<Integer> 结果 = 和弦预设.生成和弦(0, 和弦类型.减三, 4, 24);
            assertEquals(List.of(6), 结果);
        }

        @Test
        @DisplayName("全部音高都越界应返回空列表")
        void 全部越界返回空() {
            // 根音 0, 大三 [0, 4, 7], 音域 [10, 24] → 0/4/7 越界舍弃，仅 7 可下移到 -5（仍越界）舍弃
            // 但实际上 0/4/7 没有越上限，所以不会下移；只是低于下限被舍弃
            List<Integer> 结果 = 和弦预设.生成和弦(0, 和弦类型.大三, 10, 24);
            assertTrue(结果.isEmpty());
        }
    }
}
