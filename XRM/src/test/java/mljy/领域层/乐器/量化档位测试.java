package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-09 录音量化档位枚举测试。
 * 6 档（1/8、1/16、1/32 + 三连音变体），验证属性、网格 tick 计算、量化吸附、按标识查找。
 */
@DisplayName("FP-09: 量化档位")
class 量化档位测试 {

    @Nested
    @DisplayName("档位属性")
    class 档位属性测试 {

        @ParameterizedTest(name = "{0} 标识应为 {1}")
        @CsvSource({
                "一分之八, 1/8",
                "一分之十六, 1/16",
                "一分之三十二, 1/32",
                "一分之八T, 1/8T",
                "一分之十六T, 1/16T",
                "一分之三十二T, 1/32T"
        })
        void 标识应正确(量化档位 档, String 期望标识) {
            assertEquals(期望标识, 档.获取标识());
        }

        @ParameterizedTest(name = "{0} 每拍分割数应为 {1}")
        @CsvSource({
                "一分之八, 2",
                "一分之十六, 4",
                "一分之三十二, 8",
                "一分之八T, 3",
                "一分之十六T, 6",
                "一分之三十二T, 12"
        })
        void 每拍分割数应正确(量化档位 档, int 期望) {
            assertEquals(期望, 档.获取每拍分割数());
        }

        @Test
        @DisplayName("基础档位应为非三连音")
        void 基础档位非三连音() {
            assertFalse(量化档位.一分之八.是否三连音());
            assertFalse(量化档位.一分之十六.是否三连音());
            assertFalse(量化档位.一分之三十二.是否三连音());
        }

        @Test
        @DisplayName("T 后缀档位应为三连音")
        void 三连音档位() {
            assertTrue(量化档位.一分之八T.是否三连音());
            assertTrue(量化档位.一分之十六T.是否三连音());
            assertTrue(量化档位.一分之三十二T.是否三连音());
        }
    }

    @Nested
    @DisplayName("默认档位")
    class 默认档位测试 {

        @Test
        @DisplayName("默认档位应为 一分之十六")
        void 默认档位应为一分之十六() {
            assertEquals(量化档位.一分之十六, 量化档位.默认档位());
        }
    }

    @Nested
    @DisplayName("计算网格Tick")
    class 计算网格Tick测试 {

        @Test
        @DisplayName("BPM=120 时各档位网格tick应正确")
        void bpm120网格tick() {
            assertEquals(5, 量化档位.一分之八.计算网格Tick(120));
            assertEquals(2, 量化档位.一分之十六.计算网格Tick(120));
            assertEquals(1, 量化档位.一分之三十二.计算网格Tick(120));
            assertEquals(3, 量化档位.一分之八T.计算网格Tick(120));
            assertEquals(1, 量化档位.一分之十六T.计算网格Tick(120));
            assertEquals(1, 量化档位.一分之三十二T.计算网格Tick(120));
        }

        @Test
        @DisplayName("BPM=60 时网格tick应增大一倍")
        void bpm60网格tick() {
            assertEquals(10, 量化档位.一分之八.计算网格Tick(60));
            assertEquals(5, 量化档位.一分之十六.计算网格Tick(60));
            assertEquals(2, 量化档位.一分之三十二.计算网格Tick(60));
        }

        @Test
        @DisplayName("BPM=240 时网格tick应减半")
        void bpm240网格tick() {
            assertEquals(2, 量化档位.一分之八.计算网格Tick(240));
            assertEquals(1, 量化档位.一分之十六.计算网格Tick(240));
        }

        @Test
        @DisplayName("BPM=1 时一分之十六网格tick应为 300")
        void bpm1网格tick() {
            assertEquals(300, 量化档位.一分之十六.计算网格Tick(1));
        }

        @Test
        @DisplayName("BPM=0 应返回最小值 1")
        void bpm0应返回1() {
            assertEquals(1, 量化档位.一分之十六.计算网格Tick(0));
        }

        @Test
        @DisplayName("BPM 为负数应返回最小值 1")
        void bpm负数应返回1() {
            assertEquals(1, 量化档位.一分之十六.计算网格Tick(-1));
            assertEquals(1, 量化档位.一分之八.计算网格Tick(-100));
        }

        @Test
        @DisplayName("极小网格tick应被 clamp 到 1")
        void 极小网格tick应clamp到1() {
            assertEquals(1, 量化档位.一分之三十二T.计算网格Tick(120));
        }
    }

    @Nested
    @DisplayName("量化Tick")
    class 量化Tick测试 {

        @Test
        @DisplayName("tick=0 应返回 0")
        void tick0应返回0() {
            assertEquals(0, 量化档位.一分之十六.量化Tick(0, 120));
        }

        @Test
        @DisplayName("tick 为负数应返回 0")
        void tick负数应返回0() {
            assertEquals(0, 量化档位.一分之十六.量化Tick(-5, 120));
        }

        @Test
        @DisplayName("网格对齐的 tick 应保持不变")
        void 对齐tick应不变() {
            assertEquals(6, 量化档位.一分之十六.量化Tick(6, 120));
            assertEquals(4, 量化档位.一分之十六.量化Tick(4, 120));
            assertEquals(10, 量化档位.一分之八.量化Tick(10, 120));
        }

        @Test
        @DisplayName("未对齐 tick 应吸附到最近网格点")
        void 未对齐tick应吸附() {
            assertEquals(8, 量化档位.一分之十六.量化Tick(7, 120));
            assertEquals(6, 量化档位.一分之十六.量化Tick(5, 120));
            assertEquals(4, 量化档位.一分之十六.量化Tick(3, 120));
        }

        @Test
        @DisplayName("一分之八 BPM=120 网格5 的吸附")
        void 一分之八吸附() {
            assertEquals(5, 量化档位.一分之八.量化Tick(7, 120));
            assertEquals(10, 量化档位.一分之八.量化Tick(8, 120));
            assertEquals(10, 量化档位.一分之八.量化Tick(10, 120));
        }

        @Test
        @DisplayName("BPM=0 时网格为1 应原样返回 tick")
        void bpm0网格1应原样返回() {
            assertEquals(7, 量化档位.一分之十六.量化Tick(7, 0));
            assertEquals(100, 量化档位.一分之八.量化Tick(100, -1));
        }

        @Test
        @DisplayName("tick=1 网格2 应吸附到最近网格")
        void tick1网格2应吸附() {
            assertEquals(2, 量化档位.一分之十六.量化Tick(1, 120));
        }
    }

    @Nested
    @DisplayName("按标识查找")
    class 按标识查找测试 {

        @ParameterizedTest(name = "标识 {0} 应返回 {1}")
        @CsvSource({
                "1/8, 一分之八",
                "1/16, 一分之十六",
                "1/32, 一分之三十二",
                "1/8T, 一分之八T",
                "1/16T, 一分之十六T",
                "1/32T, 一分之三十二T"
        })
        void 标识应查找成功(String 标识, 量化档位 期望) {
            assertEquals(期望, 量化档位.按标识查找(标识));
        }

        @Test
        @DisplayName("带空格标识应 trim 后匹配")
        void 带空格标识应trim() {
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找("  1/16  "));
            assertEquals(量化档位.一分之八T, 量化档位.按标识查找(" 1/8T "));
        }

        @Test
        @DisplayName("大小写敏感：小写 t 后缀应未匹配返回默认")
        void 大小写敏感未匹配返回默认() {
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找("1/8t"));
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找("1/16t"));
        }

        @Test
        @DisplayName("null 标识应返回默认档位")
        void null标识应返回默认() {
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找(null));
        }

        @Test
        @DisplayName("空字符串标识应返回默认档位")
        void 空字符串应返回默认() {
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找(""));
        }

        @Test
        @DisplayName("纯空白标识应返回默认档位")
        void 纯空白应返回默认() {
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找("   "));
        }

        @Test
        @DisplayName("未知标识应返回默认档位")
        void 未知标识应返回默认() {
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找("1/64"));
            assertEquals(量化档位.默认档位(), 量化档位.按标识查找("unknown"));
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class 枚举完整性测试 {

        @Test
        @DisplayName("量化档位应有 6 个枚举值")
        void 应有6个枚举值() {
            assertEquals(6, 量化档位.values().length,
                    "量化档位应有 6 个枚举值");
        }

        @Test
        @DisplayName("每个档位标识应非空且唯一")
        void 标识非空且唯一() {
            java.util.Set<String> 标识集合 = new java.util.HashSet<>();
            for (量化档位 档 : 量化档位.values()) {
                assertNotNull(档.获取标识(), "量化档位 " + 档 + " 标识不应为 null");
                assertFalse(档.获取标识().isEmpty(), "量化档位 " + 档 + " 标识不应为空字符串");
                assertTrue(标识集合.add(档.获取标识()),
                        "量化档位标识重复: " + 档.获取标识());
            }
        }

        @Test
        @DisplayName("每个档位每拍分割数应大于 0")
        void 每拍分割数应大于0() {
            for (量化档位 档 : 量化档位.values()) {
                assertTrue(档.获取每拍分割数() > 0,
                        "量化档位 " + 档 + " 每拍分割数应大于 0");
            }
        }

        @Test
        @DisplayName("三连音档位标识应以 T 结尾")
        void 三连音标识应以T结尾() {
            for (量化档位 档 : 量化档位.values()) {
                if (档.是否三连音()) {
                    assertTrue(档.获取标识().endsWith("T"),
                            "三连音档位 " + 档 + " 标识应以 T 结尾");
                } else {
                    assertFalse(档.获取标识().endsWith("T"),
                            "非三连音档位 " + 档 + " 标识不应以 T 结尾");
                }
            }
        }
    }
}
