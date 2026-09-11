package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-03 6×9 GUI 功能位定义测试。
 * 9 个功能位默认分配到 GUI 第 0 行（槽 0-8）。
 * 翻译键固定为「演奏GUI.功能位.{标识}」。
 */
@DisplayName("FP-03: 功能位定义")
class 功能位定义测试 {

    @Nested
    @DisplayName("枚举值标识")
    class 枚举值标识测试 {

        @Test
        @DisplayName("保存 功能位标识应为 保存")
        void 保存标识() {
            assertEquals("保存", 功能位定义.保存.获取标识());
        }

        @Test
        @DisplayName("清除 功能位标识应为 清除")
        void 清除标识() {
            assertEquals("清除", 功能位定义.清除.获取标识());
        }

        @Test
        @DisplayName("关闭 功能位标识应为 关闭")
        void 关闭标识() {
            assertEquals("关闭", 功能位定义.关闭.获取标识());
        }

        @Test
        @DisplayName("NBS导入 功能位标识应为 NBS导入")
        void NBS导入标识() {
            assertEquals("NBS导入", 功能位定义.NBS导入.获取标识());
        }

        @Test
        @DisplayName("NBS导出 功能位标识应为 NBS导出")
        void NBS导出标识() {
            assertEquals("NBS导出", 功能位定义.NBS导出.获取标识());
        }

        @Test
        @DisplayName("和弦预设 功能位标识应为 和弦预设")
        void 和弦预设标识() {
            assertEquals("和弦预设", 功能位定义.和弦预设.获取标识());
        }

        @Test
        @DisplayName("公共乐谱库 功能位标识应为 公共乐谱库")
        void 公共乐谱库标识() {
            assertEquals("公共乐谱库", 功能位定义.公共乐谱库.获取标识());
        }

        @Test
        @DisplayName("钢琴卷帘编辑 功能位标识应为 钢琴卷帘编辑")
        void 钢琴卷帘编辑标识() {
            assertEquals("钢琴卷帘编辑", 功能位定义.钢琴卷帘编辑.获取标识());
        }

        @Test
        @DisplayName("分层切换 功能位标识应为 分层切换")
        void 分层切换标识() {
            assertEquals("分层切换", 功能位定义.分层切换.获取标识());
        }
    }

    @Nested
    @DisplayName("翻译键生成")
    class 翻译键测试 {

        @Test
        @DisplayName("保存 翻译键应为 演奏GUI.功能位.保存")
        void 保存翻译键() {
            assertEquals("演奏GUI.功能位.保存", 功能位定义.保存.获取名称键());
        }

        @Test
        @DisplayName("清除 翻译键应为 演奏GUI.功能位.清除")
        void 清除翻译键() {
            assertEquals("演奏GUI.功能位.清除", 功能位定义.清除.获取名称键());
        }

        @Test
        @DisplayName("关闭 翻译键应为 演奏GUI.功能位.关闭")
        void 关闭翻译键() {
            assertEquals("演奏GUI.功能位.关闭", 功能位定义.关闭.获取名称键());
        }

        @Test
        @DisplayName("NBS导入 翻译键应为 演奏GUI.功能位.NBS导入")
        void NBS导入翻译键() {
            assertEquals("演奏GUI.功能位.NBS导入", 功能位定义.NBS导入.获取名称键());
        }

        @Test
        @DisplayName("NBS导出 翻译键应为 演奏GUI.功能位.NBS导出")
        void NBS导出翻译键() {
            assertEquals("演奏GUI.功能位.NBS导出", 功能位定义.NBS导出.获取名称键());
        }

        @Test
        @DisplayName("和弦预设 翻译键应为 演奏GUI.功能位.和弦预设")
        void 和弦预设翻译键() {
            assertEquals("演奏GUI.功能位.和弦预设", 功能位定义.和弦预设.获取名称键());
        }

        @Test
        @DisplayName("公共乐谱库 翻译键应为 演奏GUI.功能位.公共乐谱库")
        void 公共乐谱库翻译键() {
            assertEquals("演奏GUI.功能位.公共乐谱库", 功能位定义.公共乐谱库.获取名称键());
        }

        @Test
        @DisplayName("钢琴卷帘编辑 翻译键应为 演奏GUI.功能位.钢琴卷帘编辑")
        void 钢琴卷帘编辑翻译键() {
            assertEquals("演奏GUI.功能位.钢琴卷帘编辑", 功能位定义.钢琴卷帘编辑.获取名称键());
        }

        @Test
        @DisplayName("分层切换 翻译键应为 演奏GUI.功能位.分层切换")
        void 分层切换翻译键() {
            assertEquals("演奏GUI.功能位.分层切换", 功能位定义.分层切换.获取名称键());
        }

        @Test
        @DisplayName("所有功能位翻译键均以 演奏GUI.功能位. 为前缀")
        void 所有翻译键前缀一致() {
            for (功能位定义 位 : 功能位定义.values()) {
                assertTrue(位.获取名称键().startsWith("演奏GUI.功能位."),
                        "功能位 " + 位 + " 翻译键应以 演奏GUI.功能位. 为前缀: " + 位.获取名称键());
            }
        }
    }

    @Nested
    @DisplayName("从标识查找")
    class 从标识查找测试 {

        @Test
        @DisplayName("标识 保存 应返回 保存 功能位")
        void 查找保存() {
            assertEquals(功能位定义.保存, 功能位定义.从标识查找("保存"));
        }

        @Test
        @DisplayName("标识 清除 应返回 清除 功能位")
        void 查找清除() {
            assertEquals(功能位定义.清除, 功能位定义.从标识查找("清除"));
        }

        @Test
        @DisplayName("标识 关闭 应返回 关闭 功能位")
        void 查找关闭() {
            assertEquals(功能位定义.关闭, 功能位定义.从标识查找("关闭"));
        }

        @Test
        @DisplayName("标识 NBS导入 应返回 NBS导入 功能位")
        void 查找NBS导入() {
            assertEquals(功能位定义.NBS导入, 功能位定义.从标识查找("NBS导入"));
        }

        @Test
        @DisplayName("标识 NBS导出 应返回 NBS导出 功能位")
        void 查找NBS导出() {
            assertEquals(功能位定义.NBS导出, 功能位定义.从标识查找("NBS导出"));
        }

        @Test
        @DisplayName("标识 和弦预设 应返回 和弦预设 功能位")
        void 查找和弦预设() {
            assertEquals(功能位定义.和弦预设, 功能位定义.从标识查找("和弦预设"));
        }

        @Test
        @DisplayName("标识 公共乐谱库 应返回 公共乐谱库 功能位")
        void 查找公共乐谱库() {
            assertEquals(功能位定义.公共乐谱库, 功能位定义.从标识查找("公共乐谱库"));
        }

        @Test
        @DisplayName("标识 钢琴卷帘编辑 应返回 钢琴卷帘编辑 功能位")
        void 查找钢琴卷帘编辑() {
            assertEquals(功能位定义.钢琴卷帘编辑, 功能位定义.从标识查找("钢琴卷帘编辑"));
        }

        @Test
        @DisplayName("标识 分层切换 应返回 分层切换 功能位")
        void 查找分层切换() {
            assertEquals(功能位定义.分层切换, 功能位定义.从标识查找("分层切换"));
        }

        @Test
        @DisplayName("未知标识应返回 null")
        void 未知标识应返回null() {
            assertNull(功能位定义.从标识查找("不存在的功能位"));
        }

        @Test
        @DisplayName("null 标识应返回 null 不抛NPE")
        void null标识应返回null() {
            assertNull(功能位定义.从标识查找(null));
        }

        @Test
        @DisplayName("空字符串标识应返回 null")
        void 空字符串标识应返回null() {
            assertNull(功能位定义.从标识查找(""));
        }
    }

    @Nested
    @DisplayName("枚举完整性")
    class 枚举完整性测试 {

        @Test
        @DisplayName("功能位定义应有 9 个枚举值")
        void 应有9个枚举值() {
            assertEquals(9, 功能位定义.values().length,
                    "功能位定义应有 9 个枚举值（保存/清除/关闭/NBS导入/NBS导出/和弦预设/公共乐谱库/钢琴卷帘编辑/分层切换）");
        }

        @Test
        @DisplayName("每个功能位标识应非空且唯一")
        void 标识非空且唯一() {
            java.util.Set<String> 标识集合 = new java.util.HashSet<>();
            for (功能位定义 位 : 功能位定义.values()) {
                assertNotNull(位.获取标识(), "功能位 " + 位 + " 标识不应为 null");
                assertFalse(位.获取标识().isEmpty(), "功能位 " + 位 + " 标识不应为空字符串");
                assertTrue(标识集合.add(位.获取标识()),
                        "功能位标识重复: " + 位.获取标识());
            }
        }

        @Test
        @DisplayName("每个功能位翻译键应非空且唯一")
        void 翻译键非空且唯一() {
            java.util.Set<String> 键集合 = new java.util.HashSet<>();
            for (功能位定义 位 : 功能位定义.values()) {
                assertNotNull(位.获取名称键(), "功能位 " + 位 + " 翻译键不应为 null");
                assertFalse(位.获取名称键().isEmpty(), "功能位 " + 位 + " 翻译键不应为空字符串");
                assertTrue(键集合.add(位.获取名称键()),
                        "功能位翻译键重复: " + 位.获取名称键());
            }
        }

        @Test
        @DisplayName("每个标识应能通过 从标识查找 反查到对应枚举")
        void 标识反查应一致() {
            for (功能位定义 位 : 功能位定义.values()) {
                assertEquals(位, 功能位定义.从标识查找(位.获取标识()),
                        "功能位 " + 位 + " 的标识反查应返回自身");
            }
        }
    }
}
