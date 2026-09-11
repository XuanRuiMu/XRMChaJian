package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-04 延音模式测试。
 * - 按住（默认）：点击音符=开始持续播放，关闭GUI=停止（真实钢琴踏板模式）
 * - 切换（可选）：点击音符=开始延音，再点击同音符=停止（电钢 Sustain 开关模式）
 */
@DisplayName("FP-04: 延音模式")
class 延音模式测试 {

    @Nested
    @DisplayName("默认模式")
    class 默认模式测试 {

        @Test
        @DisplayName("默认延音模式应为 按住")
        void 默认模式应为按住() {
            assertEquals(延音模式.按住, 延音模式.默认模式());
        }

        @Test
        @DisplayName("默认模式不应为 切换")
        void 默认模式不应为切换() {
            assertNotEquals(延音模式.切换, 延音模式.默认模式());
        }
    }

    @Nested
    @DisplayName("模式切换")
    class 模式切换测试 {

        @Test
        @DisplayName("按住 切换后应为 切换")
        void 按住切换后应为切换() {
            assertEquals(延音模式.切换, 延音模式.按住.切换());
        }

        @Test
        @DisplayName("切换 切换后应为 按住")
        void 切换切换后应为按住() {
            assertEquals(延音模式.按住, 延音模式.切换.切换());
        }

        @Test
        @DisplayName("连续两次切换应回到原模式")
        void 连续两次切换应回到原模式() {
            延音模式 初始 = 延音模式.按住;
            延音模式 一次 = 初始.切换();
            延音模式 二次 = 一次.切换();
            assertEquals(初始, 二次, "连续两次切换应回到原模式");
        }

        @Test
        @DisplayName("按住 与 切换 互为对方的切换结果")
        void 按住与切换互为切换结果() {
            assertEquals(延音模式.切换, 延音模式.按住.切换(),
                    "按住 切换后应为 切换");
            assertEquals(延音模式.按住, 延音模式.切换.切换(),
                    "切换 切换后应为 按住");
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class 枚举完整性测试 {

        @Test
        @DisplayName("延音模式应有 2 个枚举值")
        void 应有2个枚举值() {
            assertEquals(2, 延音模式.values().length,
                    "延音模式应有 2 个枚举值（按住/切换）");
        }

        @Test
        @DisplayName("延音模式应包含 按住")
        void 应包含按住() {
            boolean 包含 = false;
            for (延音模式 模式 : 延音模式.values()) {
                if (模式 == 延音模式.按住) {
                    包含 = true;
                    break;
                }
            }
            assertTrue(包含, "延音模式应包含 按住");
        }

        @Test
        @DisplayName("延音模式应包含 切换")
        void 应包含切换() {
            boolean 包含 = false;
            for (延音模式 模式 : 延音模式.values()) {
                if (模式 == 延音模式.切换) {
                    包含 = true;
                    break;
                }
            }
            assertTrue(包含, "延音模式应包含 切换");
        }

        @Test
        @DisplayName("每个模式切换后应得到另一个模式")
        void 每个模式切换应得到另一个() {
            for (延音模式 模式 : 延音模式.values()) {
                assertNotEquals(模式, 模式.切换(),
                        "模式 " + 模式 + " 切换后应得到另一个模式");
            }
        }
    }
}
