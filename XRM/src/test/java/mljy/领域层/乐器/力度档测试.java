package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-05 力度档枚举测试（5 档）。
 * 验证各档位属性（标识/全名/velocity中点/音量/pitch微调）、默认档、上一档下一档切换、按标识查找。
 */
@DisplayName("FP-05: 力度档")
class 力度档测试 {

    @Nested
    @DisplayName("档位属性")
    class 档位属性测试 {

        @ParameterizedTest(name = "{0} 标识应为 {1}")
        @CsvSource({
                "PP, pp",
                "P, p",
                "MF, mf",
                "F, f",
                "FF, ff"
        })
        void 标识应正确(力度档 档, String 期望标识) {
            assertEquals(期望标识, 档.获取标识());
        }

        @ParameterizedTest(name = "{0} 全名应为 {1}")
        @CsvSource({
                "PP, pianissimo",
                "P, piano",
                "MF, mezzo-forte",
                "F, forte",
                "FF, fortissimo"
        })
        void 全名应正确(力度档 档, String 期望全名) {
            assertEquals(期望全名, 档.获取全名());
        }

        @ParameterizedTest(name = "{0} velocity中点应为 {1}")
        @CsvSource({
                "PP, 13",
                "P, 38",
                "MF, 70",
                "F, 100",
                "FF, 119"
        })
        void velocity中点应正确(力度档 档, int 期望) {
            assertEquals(期望, 档.获取Velocity中点());
        }

        @ParameterizedTest(name = "{0} 音量应为 {1}")
        @CsvSource({
                "PP, 0.30",
                "P, 0.50",
                "MF, 0.75",
                "F, 0.90",
                "FF, 1.00"
        })
        void 音量应正确(力度档 档, float 期望) {
            assertEquals(期望, 档.获取音量(), 0.0001f);
        }

        @ParameterizedTest(name = "{0} pitch微调应为 {1}")
        @CsvSource({
                "PP, -0.005",
                "P, -0.003",
                "MF, 0.0",
                "F, 0.002",
                "FF, 0.004"
        })
        void pitch微调应正确(力度档 档, float 期望) {
            assertEquals(期望, 档.获取pitch微调(), 0.0001f);
        }
    }

    @Nested
    @DisplayName("默认档")
    class 默认档测试 {

        @Test
        @DisplayName("默认档应为 MF")
        void 默认档应为MF() {
            assertEquals(力度档.MF, 力度档.默认档());
        }
    }

    @Nested
    @DisplayName("上一档切换")
    class 上一档测试 {

        @Test
        @DisplayName("PP 上一档应保持 PP（最弱档边界）")
        void pp上一档应保持PP() {
            assertEquals(力度档.PP, 力度档.PP.上一档());
        }

        @Test
        @DisplayName("P 上一档应为 PP")
        void p上一档应为PP() {
            assertEquals(力度档.PP, 力度档.P.上一档());
        }

        @Test
        @DisplayName("MF 上一档应为 P")
        void mf上一档应为P() {
            assertEquals(力度档.P, 力度档.MF.上一档());
        }

        @Test
        @DisplayName("F 上一档应为 MF")
        void f上一档应为MF() {
            assertEquals(力度档.MF, 力度档.F.上一档());
        }

        @Test
        @DisplayName("FF 上一档应为 F")
        void ff上一档应为F() {
            assertEquals(力度档.F, 力度档.FF.上一档());
        }
    }

    @Nested
    @DisplayName("下一档切换")
    class 下一档测试 {

        @Test
        @DisplayName("PP 下一档应为 P")
        void pp下一档应为P() {
            assertEquals(力度档.P, 力度档.PP.下一档());
        }

        @Test
        @DisplayName("P 下一档应为 MF")
        void p下一档应为MF() {
            assertEquals(力度档.MF, 力度档.P.下一档());
        }

        @Test
        @DisplayName("MF 下一档应为 F")
        void mf下一档应为F() {
            assertEquals(力度档.F, 力度档.MF.下一档());
        }

        @Test
        @DisplayName("F 下一档应为 FF")
        void f下一档应为FF() {
            assertEquals(力度档.FF, 力度档.F.下一档());
        }

        @Test
        @DisplayName("FF 下一档应保持 FF（最强档边界）")
        void ff下一档应保持FF() {
            assertEquals(力度档.FF, 力度档.FF.下一档());
        }
    }

    @Nested
    @DisplayName("按标识查找")
    class 按标识测试 {

        @ParameterizedTest(name = "标识 {0} 应返回 {1}")
        @CsvSource({
                "pp, PP",
                "p, P",
                "mf, MF",
                "f, F",
                "ff, FF"
        })
        void 标识应查找成功(String 标识, 力度档 期望) {
            assertEquals(期望, 力度档.按标识(标识));
        }

        @Test
        @DisplayName("大写标识应忽略大小写匹配")
        void 大写标识应忽略大小写() {
            assertEquals(力度档.MF, 力度档.按标识("MF"));
            assertEquals(力度档.FF, 力度档.按标识("FF"));
            assertEquals(力度档.PP, 力度档.按标识("PP"));
        }

        @Test
        @DisplayName("带空格标识应 trim 后匹配")
        void 带空格标识应trim() {
            assertEquals(力度档.FF, 力度档.按标识("  ff  "));
            assertEquals(力度档.MF, 力度档.按标识(" mf "));
        }

        @Test
        @DisplayName("null 标识应返回默认档 MF")
        void null标识应返回默认档() {
            assertEquals(力度档.MF, 力度档.按标识(null));
        }

        @Test
        @DisplayName("空字符串标识应返回默认档 MF")
        void 空字符串应返回默认档() {
            assertEquals(力度档.MF, 力度档.按标识(""));
        }

        @Test
        @DisplayName("纯空白标识应返回默认档 MF")
        void 纯空白应返回默认档() {
            assertEquals(力度档.MF, 力度档.按标识("   "));
        }

        @Test
        @DisplayName("未知标识应返回默认档 MF")
        void 未知标识应返回默认档() {
            assertEquals(力度档.MF, 力度档.按标识("unknown"));
            assertEquals(力度档.MF, 力度档.按标识("fff"));
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class 枚举完整性测试 {

        @Test
        @DisplayName("力度档应有 5 个枚举值")
        void 应有5个枚举值() {
            assertEquals(5, 力度档.values().length,
                    "力度档应有 5 个枚举值（PP/P/MF/F/FF）");
        }

        @Test
        @DisplayName("每个档位标识应非空且唯一")
        void 标识非空且唯一() {
            java.util.Set<String> 标识集合 = new java.util.HashSet<>();
            for (力度档 档 : 力度档.values()) {
                assertNotNull(档.获取标识(), "力度档 " + 档 + " 标识不应为 null");
                assertFalse(档.获取标识().isEmpty(), "力度档 " + 档 + " 标识不应为空字符串");
                assertTrue(标识集合.add(档.获取标识()),
                        "力度档标识重复: " + 档.获取标识());
            }
        }

        @Test
        @DisplayName("velocity中点应随档位递增")
        void velocity中点应递增() {
            力度档[] 顺序 = {力度档.PP, 力度档.P, 力度档.MF, 力度档.F, 力度档.FF};
            for (int i = 1; i < 顺序.length; i++) {
                assertTrue(顺序[i].获取Velocity中点() > 顺序[i - 1].获取Velocity中点(),
                        "档位 " + 顺序[i] + " velocity中点应大于 " + 顺序[i - 1]);
            }
        }

        @Test
        @DisplayName("音量应随档位递增")
        void 音量应递增() {
            力度档[] 顺序 = {力度档.PP, 力度档.P, 力度档.MF, 力度档.F, 力度档.FF};
            for (int i = 1; i < 顺序.length; i++) {
                assertTrue(顺序[i].获取音量() > 顺序[i - 1].获取音量(),
                        "档位 " + 顺序[i] + " 音量应大于 " + 顺序[i - 1]);
            }
        }

        @Test
        @DisplayName("pitch微调应随档位递增")
        void pitch微调应递增() {
            力度档[] 顺序 = {力度档.PP, 力度档.P, 力度档.MF, 力度档.F, 力度档.FF};
            for (int i = 1; i < 顺序.length; i++) {
                assertTrue(顺序[i].获取pitch微调() > 顺序[i - 1].获取pitch微调(),
                        "档位 " + 顺序[i] + " pitch微调应大于 " + 顺序[i - 1]);
            }
        }
    }
}
