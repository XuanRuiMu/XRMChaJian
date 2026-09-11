package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-11 和弦类型测试。
 * 12 种业界标准和弦预设（maj/min/7/maj7/m7/dim/aug/sus2/sus4/add9/6/9）。
 */
@DisplayName("FP-11: 和弦类型")
class 和弦类型测试 {

    @Nested
    @DisplayName("音程集合")
    class 音程集合测试 {

        @Test
        @DisplayName("大三 maj 音程应为 [0, 4, 7]")
        void 大三音程() {
            assertEquals(List.of(0, 4, 7), 和弦类型.大三.获取音程());
        }

        @Test
        @DisplayName("小三 min 音程应为 [0, 3, 7]")
        void 小三音程() {
            assertEquals(List.of(0, 3, 7), 和弦类型.小三.获取音程());
        }

        @Test
        @DisplayName("属七 7 音程应为 [0, 4, 7, 10]")
        void 属七音程() {
            assertEquals(List.of(0, 4, 7, 10), 和弦类型.属七.获取音程());
        }

        @Test
        @DisplayName("大七 maj7 音程应为 [0, 4, 7, 11]")
        void 大七音程() {
            assertEquals(List.of(0, 4, 7, 11), 和弦类型.大七.获取音程());
        }

        @Test
        @DisplayName("小七 m7 音程应为 [0, 3, 7, 10]")
        void 小七音程() {
            assertEquals(List.of(0, 3, 7, 10), 和弦类型.小七.获取音程());
        }

        @Test
        @DisplayName("减三 dim 音程应为 [0, 3, 6]")
        void 减三音程() {
            assertEquals(List.of(0, 3, 6), 和弦类型.减三.获取音程());
        }

        @Test
        @DisplayName("增三 aug 音程应为 [0, 4, 8]")
        void 增三音程() {
            assertEquals(List.of(0, 4, 8), 和弦类型.增三.获取音程());
        }

        @Test
        @DisplayName("挂二 sus2 音程应为 [0, 2, 7]")
        void 挂二音程() {
            assertEquals(List.of(0, 2, 7), 和弦类型.挂二.获取音程());
        }

        @Test
        @DisplayName("挂四 sus4 音程应为 [0, 5, 7]")
        void 挂四音程() {
            assertEquals(List.of(0, 5, 7), 和弦类型.挂四.获取音程());
        }

        @Test
        @DisplayName("加九 add9 音程应为 [0, 4, 7, 14]")
        void 加九音程() {
            assertEquals(List.of(0, 4, 7, 14), 和弦类型.加九.获取音程());
        }

        @Test
        @DisplayName("大六 6 音程应为 [0, 4, 7, 9]")
        void 大六音程() {
            assertEquals(List.of(0, 4, 7, 9), 和弦类型.大六.获取音程());
        }

        @Test
        @DisplayName("属九 9 音程应为 [0, 4, 7, 10, 14]")
        void 属九音程() {
            assertEquals(List.of(0, 4, 7, 10, 14), 和弦类型.属九.获取音程());
        }
    }

    @Nested
    @DisplayName("获取音高")
    class 获取音高测试 {

        @Test
        @DisplayName("大三根音 0 应返回 [0, 4, 7]")
        void 大三根音零() {
            assertEquals(List.of(0, 4, 7), 和弦类型.大三.获取音高(0));
        }

        @Test
        @DisplayName("大三根音 12 应返回 [12, 16, 19]")
        void 大三根音十二() {
            assertEquals(List.of(12, 16, 19), 和弦类型.大三.获取音高(12));
        }

        @Test
        @DisplayName("属九根音 5 应返回 [5, 9, 12, 15, 19]")
        void 属九根音五() {
            assertEquals(List.of(5, 9, 12, 15, 19), 和弦类型.属九.获取音高(5));
        }
    }

    @Nested
    @DisplayName("按标识查找")
    class 按标识查找测试 {

        @Test
        @DisplayName("标识 maj 应返回 大三")
        void 查找maj() {
            assertEquals(和弦类型.大三, 和弦类型.按标识查找("maj"));
        }

        @Test
        @DisplayName("标识 min 应返回 小三")
        void 查找min() {
            assertEquals(和弦类型.小三, 和弦类型.按标识查找("min"));
        }

        @Test
        @DisplayName("标识 7 应返回 属七")
        void 查找7() {
            assertEquals(和弦类型.属七, 和弦类型.按标识查找("7"));
        }

        @Test
        @DisplayName("标识 maj7 应返回 大七")
        void 查找maj7() {
            assertEquals(和弦类型.大七, 和弦类型.按标识查找("maj7"));
        }

        @Test
        @DisplayName("标识 m7 应返回 小七")
        void 查找m7() {
            assertEquals(和弦类型.小七, 和弦类型.按标识查找("m7"));
        }

        @Test
        @DisplayName("标识 dim 应返回 减三")
        void 查找dim() {
            assertEquals(和弦类型.减三, 和弦类型.按标识查找("dim"));
        }

        @Test
        @DisplayName("标识 aug 应返回 增三")
        void 查找aug() {
            assertEquals(和弦类型.增三, 和弦类型.按标识查找("aug"));
        }

        @Test
        @DisplayName("标识 sus2 应返回 挂二")
        void 查找sus2() {
            assertEquals(和弦类型.挂二, 和弦类型.按标识查找("sus2"));
        }

        @Test
        @DisplayName("标识 sus4 应返回 挂四")
        void 查找sus4() {
            assertEquals(和弦类型.挂四, 和弦类型.按标识查找("sus4"));
        }

        @Test
        @DisplayName("标识 add9 应返回 加九")
        void 查找add9() {
            assertEquals(和弦类型.加九, 和弦类型.按标识查找("add9"));
        }

        @Test
        @DisplayName("标识 6 应返回 大六")
        void 查找6() {
            assertEquals(和弦类型.大六, 和弦类型.按标识查找("6"));
        }

        @Test
        @DisplayName("标识 9 应返回 属九")
        void 查找9() {
            assertEquals(和弦类型.属九, 和弦类型.按标识查找("9"));
        }

        @Test
        @DisplayName("未知标识应返回 null")
        void 未知标识返回null() {
            assertNull(和弦类型.按标识查找("unknown"));
        }

        @Test
        @DisplayName("null 标识应返回 null")
        void null标识返回null() {
            assertNull(和弦类型.按标识查找(null));
        }

        @Test
        @DisplayName("空字符串标识应返回 null")
        void 空字符串返回null() {
            assertNull(和弦类型.按标识查找(""));
        }

        @Test
        @DisplayName("带空格标识应 trim 后查找")
        void 带空格标识应trim() {
            assertEquals(和弦类型.大三, 和弦类型.按标识查找("  maj  "));
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class 枚举完整性测试 {

        @Test
        @DisplayName("和弦类型应有 12 个枚举值")
        void 应有12个枚举值() {
            assertEquals(12, 和弦类型.values().length,
                    "和弦类型应有 12 个枚举值（大三/小三/属七/大七/小七/减三/增三/挂二/挂四/加九/大六/属九）");
        }

        @Test
        @DisplayName("每个和弦类型标识应非空且唯一")
        void 标识非空且唯一() {
            java.util.Set<String> 标识集合 = new java.util.HashSet<>();
            for (和弦类型 类型 : 和弦类型.values()) {
                assertNotNull(类型.获取标识(), "和弦类型 " + 类型 + " 标识不应为 null");
                assertFalse(类型.获取标识().isEmpty(), "和弦类型 " + 类型 + " 标识不应为空字符串");
                assertTrue(标识集合.add(类型.获取标识()),
                        "和弦类型标识重复: " + 类型.获取标识());
            }
        }

        @Test
        @DisplayName("每个和弦类型音程集合应非空且首项为 0")
        void 音程集合首项为零() {
            for (和弦类型 类型 : 和弦类型.values()) {
                assertFalse(类型.获取音程().isEmpty(),
                        "和弦类型 " + 类型 + " 音程集合不应为空");
                assertEquals(0, 类型.获取音程().get(0),
                        "和弦类型 " + 类型 + " 音程首项应为 0（根音）");
            }
        }
    }
}
