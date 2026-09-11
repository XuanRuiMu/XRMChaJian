package mljy.回归测试;

import 暮澜纪元.通用.文本.关键词解析器;
import 暮澜纪元.通用.文本.文本格式化器;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FP-08 关键词解析器边界测试")
class 关键词解析器边界测试 {

    @Nested
    @DisplayName("空值与边界输入")
    class 空值与边界 {

        @Test
        @DisplayName("空文本应返回空字符串")
        void 空文本应返回空字符串() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            assertEquals("", 解析器.解析(""));
        }

        @Test
        @DisplayName("null文本应返回空字符串")
        void null文本应返回空字符串() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            assertEquals("", 解析器.解析(null));
        }

        @Test
        @DisplayName("纯文本无关键词应原样返回")
        void 纯文本无关键词应原样返回() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            assertEquals("这是一段普通描述", 解析器.解析("这是一段普通描述"));
        }

        @Test
        @DisplayName("纯英文文本无关键词应原样返回")
        void 纯英文文本无关键词应原样返回() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            assertEquals("plain english text", 解析器.解析("plain english text"));
        }

        @Test
        @DisplayName("空参数调用应正常处理")
        void 空参数调用应正常处理() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            assertEquals("文本", 解析器.解析("文本"));
        }
    }

    @Nested
    @DisplayName("多个关键词嵌套")
    class 多关键词嵌套 {

        @Test
        @DisplayName("吸血日志三关键词嵌套应正确着色")
        void 吸血日志三关键词嵌套应正确着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 文本 = "通过[吸血]恢复了[治疗数值:30.0]点[生命值]";
            String 结果 = 解析器.解析(文本);
            assertTrue(结果.contains("<dark_red>吸血</dark_red>"), "应包含着色的[吸血]，实际: " + 结果);
            assertTrue(结果.contains("<dark_green>30.0</dark_green>"), "应包含着色的[治疗数值:30.0]，实际: " + 结果);
            assertTrue(结果.contains("<dark_green>"), "应包含<dark_green>标签([生命值])，实际: " + 结果);
        }

        @Test
        @DisplayName("战斗日志多关键词组合应正确着色")
        void 战斗日志多关键词组合应正确着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 文本 = "你用铁剑对僵尸造成了[伤害数值:100.0]点[物理]伤害。";
            String 结果 = 解析器.解析(文本);
            assertTrue(结果.contains("<dark_red>100.0</dark_red>"), "应包含着色的[伤害数值:100.0]，实际: " + 结果);
            assertTrue(结果.contains("<gray>物理</gray>"), "应包含着色的[物理]，实际: " + 结果);
        }

        @Test
        @DisplayName("技能伤害日志带暴击关键词应正确着色")
        void 技能伤害日志带暴击关键词应正确着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 文本 = "[第一技能]对僵尸造成了[伤害数值:200.0]（[暴击]！）点[魔法]伤害。";
            String 结果 = 解析器.解析(文本);
            assertTrue(结果.contains("<red>第一技能</red>"), "应包含着色的[第一技能]（无玩家时返回默认），实际: " + 结果);
            assertTrue(结果.contains("<dark_red>200.0</dark_red>"), "应包含着色的[伤害数值:200.0]，实际: " + 结果);
            assertTrue(结果.contains("<gold><bold>暴击</bold></gold>"), "应包含着色的[暴击]，实际: " + 结果);
            assertTrue(结果.contains("<blue>魔法</blue>"), "应包含着色的[魔法]，实际: " + 结果);
        }

        @Test
        @DisplayName("相邻多个静态关键词应分别着色")
        void 相邻多个静态关键词应分别着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[魔法][物理][火焰]");
            assertTrue(结果.contains("<blue>魔法</blue>"), "应包含<blue>魔法</blue>，实际: " + 结果);
            assertTrue(结果.contains("<gray>物理</gray>"), "应包含<gray>物理</gray>，实际: " + 结果);
            assertTrue(结果.contains("<red>火焰</red>"), "应包含<red>火焰</red>，实际: " + 结果);
        }
    }

    @Nested
    @DisplayName("dt与plain标记与关键词混用")
    class 标记与关键词混用 {

        @Test
        @DisplayName("dt标记内中文关键词应自动着色")
        void dt标记内中文关键词应自动着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[dt]远程魔法输出[/dt]");
            assertEquals("<red>远程<blue>魔法</blue>输出</red>", 结果);
        }

        @Test
        @DisplayName("dt标记内多个中文关键词应分别着色")
        void dt标记内多个中文关键词应分别着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[dt]魔法与物理混合[/dt]");
            assertEquals("<red><blue>魔法</blue>与<gray>物理</gray>混合</red>", 结果);
        }

        @Test
        @DisplayName("plain标记内关键词不应着色")
        void plain标记内关键词不应着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[plain]包含魔法物质[/plain]");
            assertEquals("<gray>包含魔法物质</gray>", 结果);
        }

        @Test
        @DisplayName("dt标记与plain标记混合应分段处理")
        void dt标记与plain标记混合应分段处理() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 文本 = "介绍[dt]远程魔法输出[/dt]说明[plain]包含魔法物质[/plain]";
            String 结果 = 格式化器.处理标记(文本);
            String 期望 = "<gray>介绍</gray>"
                    + "<red>远程<blue>魔法</blue>输出</red>"
                    + "<gray>说明</gray>"
                    + "<gray>包含魔法物质</gray>";
            assertEquals(期望, 结果);
        }

        @Test
        @DisplayName("普通文本中方括号关键词应着色")
        void 普通文本中方括号关键词应着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("造成[魔法]伤害");
            assertEquals("<gray>造成<blue>魔法</blue>伤害</gray>", 结果);
        }

        @Test
        @DisplayName("普通文本中无方括号的关键词不应自动着色")
        void 普通文本中无方括号的关键词不应自动着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("造成魔法伤害");
            assertEquals("<gray>造成魔法伤害</gray>", 结果);
        }
    }

    @Nested
    @DisplayName("跨语言关键词边界")
    class 跨语言关键词 {

        @Test
        @DisplayName("中文关键词[物理]应被着色")
        void 中文关键词物理应被着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[物理]");
            assertEquals("<gray>物理</gray>", 结果);
        }

        @Test
        @DisplayName("中文关键词[魔法]应被着色")
        void 中文关键词魔法应被着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[魔法]");
            assertEquals("<blue>魔法</blue>", 结果);
        }

        @Test
        @DisplayName("英文关键词[magic]不应被着色(关键词颜色枚举无magic项)")
        void 英文关键词magic不应被着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[magic]");
            assertEquals("[magic]", 结果, "英文 magic 不在关键词颜色枚举中，应保持原样");
        }

        @Test
        @DisplayName("英文关键词[physical]不应被着色(关键词颜色枚举无physical项)")
        void 英文关键词physical不应被着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[physical]");
            assertEquals("[physical]", 结果, "英文 physical 不在关键词颜色枚举中，应保持原样");
        }

        @Test
        @DisplayName("中文与英文关键词混用应仅中文被着色")
        void 中英文关键词混用应仅中文被着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[物理]和[physical]混合");
            assertTrue(结果.contains("<gray>物理</gray>"), "中文[物理]应着色，实际: " + 结果);
            assertTrue(结果.contains("[physical]"), "英文[physical]应保持原样，实际: " + 结果);
            assertFalse(结果.contains("<gray>physical</gray>"), "英文 physical 不应被着色，实际: " + 结果);
        }

        @Test
        @DisplayName("dt标记内英文关键词不应自动着色(已知设计限制)")
        void dt标记内英文关键词不应自动着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[dt]magic damage[/dt]");
            assertEquals("<red>magic damage</red>", 结果,
                    "英文 magic 在 [dt] 标记内不应被自动着色（关键词颜色枚举显示文本为中文），实际: " + 结果);
        }

        @Test
        @DisplayName("dt标记内中文关键词应自动着色")
        void dt标记内中文关键词应自动着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[dt]魔法伤害[/dt]");
            assertEquals("<red><blue>魔法</blue>伤害</red>", 结果);
        }

        @Test
        @DisplayName("dt标记内中英文关键词混用应仅中文被自动着色")
        void dt标记内中英文关键词混用应仅中文被自动着色() {
            文本格式化器 格式化器 = 文本格式化器.创建纯文本();
            String 结果 = 格式化器.处理标记("[dt]魔法magic混合[/dt]");
            assertTrue(结果.contains("<blue>魔法</blue>"), "中文'魔法'应被着色，实际: " + 结果);
            assertFalse(结果.contains("<blue>magic</blue>"), "英文'magic'不应被着色，实际: " + 结果);
            assertTrue(结果.contains("magic"), "英文'magic'文本应保留，实际: " + 结果);
        }

        @Test
        @DisplayName("英文文本中夹杂中文关键词应仅中文着色")
        void 英文文本中夹杂中文关键词应仅中文着色() {
            关键词解析器 解析器 = 关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("dealt [魔法] damage");
            assertEquals("dealt <blue>魔法</blue> damage", 结果);
        }
    }
}
