package 暮澜纪元.通用.文本;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class 文本格式化器测试 {

    @Test
    void dt标记整段应为红色且伤害类型关键词单独着色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[dt]远程魔法输出。[/dt]");
        assertEquals("<red>远程<blue>魔法</blue>输出。</red>", 结果);
    }

    @Test
    void dt标记内物理关键词应为灰色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[dt]物理近战输出[/dt]");
        assertEquals("<red><gray>物理</gray>近战输出</red>", 结果);
    }

    @Test
    void dt标记内多个伤害类型关键词应分别着色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[dt]魔法与物理混合[/dt]");
        assertEquals("<red><blue>魔法</blue>与<gray>物理</gray>混合</red>", 结果);
    }

    @Test
    void plain标记应套灰色且不关键词着色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[plain]释放魔法物质。[/plain]");
        assertEquals("<gray>释放魔法物质。</gray>", 结果);
    }

    @Test
    void 普通文本应套灰色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("普通描述");
        assertEquals("<gray>普通描述</gray>", 结果);
    }

    @Test
    void 普通文本中方括号关键词应着色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("造成[魔法]伤害");
        assertEquals("<gray>造成<blue>魔法</blue>伤害</gray>", 结果);
    }

    @Test
    void 普通文本中无方括号的伤害类型词不应自动着色() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("造成魔法伤害");
        assertEquals("<gray>造成魔法伤害</gray>", 结果);
    }

    @Test
    void 空文本应返回空字符串() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        assertEquals("", 格式化器.处理标记(""));
        assertEquals("", 格式化器.处理标记(null));
    }

    @Test
    void 混合标记应正确分段处理() {
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
    void 纯dt标记无前后文本应正确处理() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[dt]魔法[/dt]");
        assertEquals("<red><blue>魔法</blue></red>", 结果);
    }

    @Test
    void 多个dt标记应分别处理() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 文本 = "[dt]魔法输出[/dt]与[dt]物理输出[/dt]";
        String 结果 = 格式化器.处理标记(文本);
        String 期望 = "<red><blue>魔法</blue>输出</red>"
                + "<gray>与</gray>"
                + "<red><gray>物理</gray>输出</red>";
        assertEquals(期望, 结果);
    }

    @Test
    void 多个plain标记应分别处理() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 文本 = "[plain]魔法物质[/plain]分隔[plain]物理护甲[/plain]";
        String 结果 = 格式化器.处理标记(文本);
        String 期望 = "<gray>魔法物质</gray>"
                + "<gray>分隔</gray>"
                + "<gray>物理护甲</gray>";
        assertEquals(期望, 结果);
    }

    @Test
    void 格式化方法应返回非空Component() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        net.kyori.adventure.text.Component 组件 = 格式化器.格式化("[dt]魔法[/dt]");
        assertNotNull(组件);
    }

    @Test
    void 嵌套标记应作为纯文本处理() {
        文本格式化器 格式化器 = 文本格式化器.创建纯文本();
        String 结果 = 格式化器.处理标记("[dt]外层[plain]内层[/plain]外层[/dt]");
        assertEquals("<red>外层[plain]内层[/plain]外层</red>", 结果);
    }
}
