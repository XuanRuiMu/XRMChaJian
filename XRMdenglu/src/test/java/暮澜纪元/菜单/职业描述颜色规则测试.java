package 暮澜纪元.菜单;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.测试工具.翻译文件加载器;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 职业描述颜色规则回归测试。
 *
 * 验证目标（基于需求文档"职业，专精与属性.md"）：
 * - [dt] 标记段：整段红色，段内伤害类型关键词按关键词颜色表着色
 *   （新文档描述不再含具体伤害类型关键词，[dt] 段为纯红色文本）
 * - 普通文本段（无标记）：灰色，段内方括号关键词如 [魔法] 会着色，
 *   但单独出现的关键词（如"魔法物质"中的"魔法"）不会自动着色
 * - 标记边界：空文本、单标记、混合标记、段落分隔符 | 的处理
 *
 * 测试方式：
 * - 通过 翻译文件加载器 加载 zh.yml 翻译文件
 * - 调用 消息.格式化文本(行) 渲染为 Component（与 GUI 实际调用路径一致）
 * - 通过 MiniMessage.serialize(component) 序列化回字符串验证颜色标签
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("职业描述颜色规则")
class 职业描述颜色规则测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;
    private MiniMessage 序列化器;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "菜单/职业选择GUI");
        序列化器 = MiniMessage.miniMessage();
    }

    /**
     * 调用 消息.格式化文本 渲染文本，并序列化回 MiniMessage 字符串用于断言。
     */
    private String 渲染(String 文本) {
        Component 组件 = 消息.格式化文本(文本);
        return 序列化器.serialize(组件);
    }

    /**
     * 从翻译文件获取职业描述原文（含 | 分隔符）。
     */
    private String 获取职业描述(String 职业名) {
        return 消息.获取类翻译(职业选择GUI.class, "职业描述." + 职业名, null);
    }

    // ===== 大魔导师 =====

    @Nested
    @DisplayName("大魔导师：[dt] + 普通文本段")
    class 大魔导师测试 {

        @Test
        @DisplayName("[dt]段：整段红色，不含伤害类型关键词（纯红色文本）")
        void dt段_整段红色_无关键词着色() {
            String 描述 = 获取职业描述("大魔导师");
            assertNotNull(描述, "大魔导师描述应存在");
            String[] 段落 = 描述.split("\\|");
            assertEquals(2, 段落.length, "大魔导师描述应分为2段，实际: " + 描述);

            String 渲染结果 = 渲染(段落[0]);
            assertTrue(渲染结果.contains("<red>"),
                    "[dt]段应包含红色标签，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("普通段：灰色，'魔法物质'中的'魔法'不自动着色（无方括号标记）")
        void 普通段_灰色_魔法物质不着色() {
            String 描述 = 获取职业描述("大魔导师");
            String[] 段落 = 描述.split("\\|");
            assertEquals(2, 段落.length);

            String 渲染结果 = 渲染(段落[1]);
            assertTrue(渲染结果.contains("<gray>"),
                    "普通段应包含灰色标签，实际: " + 渲染结果);
            assertFalse(渲染结果.contains("<blue>魔法</blue>"),
                    "普通段内'魔法物质'的'魔法'不应被着色为蓝色，实际: " + 渲染结果);
            assertFalse(渲染结果.contains("<red>"),
                    "普通段不应包含红色标签，实际: " + 渲染结果);
        }
    }

    // ===== 女巫 =====

    @Nested
    @DisplayName("女巫：[dt] + 普通文本段")
    class 女巫测试 {

        @Test
        @DisplayName("[dt]段：整段红色，不含伤害类型关键词")
        void dt段_整段红色() {
            String 描述 = 获取职业描述("女巫");
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length >= 2, "女巫描述应至少2段，实际: " + 描述);

            String 渲染结果 = 渲染(段落[0]);
            assertTrue(渲染结果.contains("<red>"), "[dt]段应包含红色标签，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("普通段：'范围性伤害和治疗。'应为灰色")
        void 普通段_灰色() {
            String 描述 = 获取职业描述("女巫");
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length >= 2);

            String 渲染结果 = 渲染(段落[1]);
            assertFalse(渲染结果.contains("<red>"),
                    "普通段不应包含红色，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<gray>"),
                    "普通段应为灰色，实际: " + 渲染结果);
        }
    }

    // ===== 精灵卫士（多段描述）=====

    @Nested
    @DisplayName("精灵卫士：[dt] + 多段普通文本")
    class 精灵卫士测试 {

        @Test
        @DisplayName("应分为3段：[dt]段红色 + 2段普通灰色")
        void 三段描述() {
            String 描述 = 获取职业描述("精灵卫士");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertEquals(3, 段落.length, "精灵卫士应分为3段，实际: " + 描述);

            // 第1段 [dt] 红色
            String 渲染结果0 = 渲染(段落[0]);
            assertTrue(渲染结果0.contains("<red>"),
                    "第1段应包含红色标签，实际: " + 渲染结果0);

            // 第2段普通灰色
            String 渲染结果1 = 渲染(段落[1]);
            assertFalse(渲染结果1.contains("<red>"),
                    "第2段不应包含红色，实际: " + 渲染结果1);
            assertTrue(渲染结果1.contains("<gray>"),
                    "第2段应为灰色，实际: " + 渲染结果1);

            // 第3段普通灰色
            String 渲染结果2 = 渲染(段落[2]);
            assertFalse(渲染结果2.contains("<red>"),
                    "第3段不应包含红色，实际: " + 渲染结果2);
            assertTrue(渲染结果2.contains("<gray>"),
                    "第3段应为灰色，实际: " + 渲染结果2);
        }
    }

    // ===== 沸血战士（单段普通文本）=====

    @Nested
    @DisplayName("沸血战士：单段普通文本")
    class 沸血战士测试 {

        @Test
        @DisplayName("整段应为灰色（无伤害类型描述句）")
        void 单段灰色() {
            String 描述 = 获取职业描述("沸血战士");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertEquals(1, 段落.length, "沸血战士应为单段描述，实际: " + 描述);

            String 渲染结果 = 渲染(段落[0]);
            assertFalse(渲染结果.contains("<red>"),
                    "不应包含红色，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<gray>"),
                    "应为灰色，实际: " + 渲染结果);
        }
    }

    // ===== 无伤害类型描述的职业（全灰）=====

    @Nested
    @DisplayName("无伤害类型描述的职业：全灰")
    class 无伤害类型描述职业测试 {

        @Test
        @DisplayName("暗影守卫所有描述段应为灰色")
        void 暗影守卫_灰色() {
            String 描述 = 获取职业描述("暗影守卫");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length >= 1, "暗影守卫应至少1段描述，实际: " + 描述);

            for (int i = 0; i < 段落.length; i++) {
                String 渲染结果 = 渲染(段落[i]);
                assertFalse(渲染结果.contains("<red>"),
                        "第" + (i + 1) + "段不应包含红色，实际: " + 渲染结果);
                assertTrue(渲染结果.contains("<gray>"),
                        "第" + (i + 1) + "段应为灰色，实际: " + 渲染结果);
            }
        }

        @Test
        @DisplayName("末日预言者所有描述段应为灰色")
        void 末日预言者_灰色() {
            String 描述 = 获取职业描述("末日预言者");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length >= 1, "末日预言者应至少1段描述，实际: " + 描述);

            for (int i = 0; i < 段落.length; i++) {
                String 渲染结果 = 渲染(段落[i]);
                assertFalse(渲染结果.contains("<red>"),
                        "第" + (i + 1) + "段不应包含红色，实际: " + 渲染结果);
                assertTrue(渲染结果.contains("<gray>"),
                        "第" + (i + 1) + "段应为灰色，实际: " + 渲染结果);
            }
        }

        @Test
        @DisplayName("树人守卫所有描述段应为灰色")
        void 树人守卫_灰色() {
            String 描述 = 获取职业描述("树人守卫");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length > 1, "树人守卫应有多段描述，实际: " + 描述);

            for (int i = 0; i < 段落.length; i++) {
                String 渲染结果 = 渲染(段落[i]);
                assertFalse(渲染结果.contains("<red>"),
                        "第" + (i + 1) + "段不应包含红色，实际: " + 渲染结果);
                assertTrue(渲染结果.contains("<gray>"),
                        "第" + (i + 1) + "段应为灰色，实际: " + 渲染结果);
            }
        }

        @Test
        @DisplayName("冰霜战士所有描述段应为灰色")
        void 冰霜战士_灰色() {
            String 描述 = 获取职业描述("冰霜战士");
            assertNotNull(描述);
            String[] 段落 = 描述.split("\\|");
            assertTrue(段落.length > 1, "冰霜战士应有多段描述，实际: " + 描述);

            for (int i = 0; i < 段落.length; i++) {
                String 渲染结果 = 渲染(段落[i]);
                assertFalse(渲染结果.contains("<red>"),
                        "第" + (i + 1) + "段不应包含红色，实际: " + 渲染结果);
                assertTrue(渲染结果.contains("<gray>"),
                        "第" + (i + 1) + "段应为灰色，实际: " + 渲染结果);
            }
        }
    }

    // ===== 标记边界 =====

    @Nested
    @DisplayName("标记边界")
    class 标记边界测试 {

        @Test
        @DisplayName("空文本应渲染为空字符串")
        void 空文本() {
            String 渲染结果 = 渲染("");
            assertTrue(渲染结果.isEmpty(),
                    "空文本应渲染为空字符串，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("null文本应渲染为空字符串")
        void null文本() {
            String 渲染结果 = 渲染(null);
            assertTrue(渲染结果.isEmpty(),
                    "null文本应渲染为空字符串，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("只有[dt]标记的文本：整段红色，段内伤害类型关键词着色")
        void 只有dt标记() {
            String 渲染结果 = 渲染("[dt]远程魔法输出。[/dt]");
            assertTrue(渲染结果.contains("<red>"),
                    "应包含红色标签，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<blue>魔法</blue>"),
                    "'魔法'应着色为蓝色，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("只有[plain]标记的文本：灰色，段内关键词不着色")
        void 只有plain标记() {
            String 渲染结果 = 渲染("[plain]释放魔法物质。[/plain]");
            assertTrue(渲染结果.contains("<gray>"),
                    "应包含灰色标签，实际: " + 渲染结果);
            assertFalse(渲染结果.contains("<blue>"),
                    "不应着色任何关键词，实际: " + 渲染结果);
            assertFalse(渲染结果.contains("<red>"),
                    "不应包含红色标签，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("混合标记文本：dt段红色+关键词着色，plain段灰色+不关键词着色，普通段灰色")
        void 混合标记() {
            String 文本 = "[dt]远程魔法输出。[/dt]说明[plain]包含魔法物质。[/plain]";
            String 渲染结果 = 渲染(文本);
            assertTrue(渲染结果.contains("<red>"),
                    "应包含红色标签（dt段），实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<blue>魔法</blue>"),
                    "dt段'魔法'应着色为蓝色，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<gray>"),
                    "应包含灰色标签（plain段和普通段），实际: " + 渲染结果);
        }

        @Test
        @DisplayName("段落分隔符'|'应作为普通字符处理（文本格式化器不识别|）")
        void 段落分隔符作为普通字符() {
            String 文本 = "[dt]远程魔法输出。[/dt]|普通说明";
            String 渲染结果 = 渲染(文本);
            assertTrue(渲染结果.contains("<red>"),
                    "dt段应红色，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<gray>"),
                    "普通段应灰色，实际: " + 渲染结果);
            assertTrue(渲染结果.contains("|"),
                    "段落分隔符应保留在输出中，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("普通文本中的方括号关键词应着色（如[魔法]）")
        void 普通文本方括号关键词着色() {
            String 渲染结果 = 渲染("擅长抵抗[魔法]伤害。");
            assertTrue(渲染结果.contains("<gray>"),
                    "应包含灰色标签（普通文本套灰），实际: " + 渲染结果);
            assertTrue(渲染结果.contains("<blue>魔法</blue>"),
                    "方括号标记的'魔法'应着色为蓝色，实际: " + 渲染结果);
        }

        @Test
        @DisplayName("普通文本中无方括号的伤害类型词不应自动着色")
        void 普通文本无方括号不着色() {
            String 渲染结果 = 渲染("造成魔法伤害。");
            assertTrue(渲染结果.contains("<gray>"),
                    "应包含灰色标签，实际: " + 渲染结果);
            assertFalse(渲染结果.contains("<blue>"),
                    "无方括号的'魔法'不应着色，实际: " + 渲染结果);
        }
    }
}
