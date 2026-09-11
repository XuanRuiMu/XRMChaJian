package 暮澜纪元.通用.文本;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class 关键词解析器测试 {

    @Test
    void 空文本应返回空字符串() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        assertEquals("", 解析器.解析(""));
        assertEquals("", 解析器.解析(null));
    }

    @Test
    void 普通文本无关键词应原样返回() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        assertEquals("普通描述", 解析器.解析("普通描述"));
    }

    @Test
    void 单个静态关键词应着色() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[魔法]");
        assertEquals("<blue>魔法</blue>", 结果);
    }

    @Test
    void 多个静态关键词应分别着色() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[魔法]与[物理]");
        assertEquals("<blue>魔法</blue>与<gray>物理</gray>", 结果);
    }

    @Test
    void 静态关键词与普通文本混合应正确处理() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("造成[魔法]伤害");
        assertEquals("造成<blue>魔法</blue>伤害", 结果);
    }

    @Test
    void 带值关键词应着色值部分() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[魔法:远程]");
        assertEquals("<blue>远程</blue>", 结果);
    }

    @Test
    void 未知关键词应保持原样() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[未知关键词]");
        assertEquals("[未知关键词]", 结果);
    }

    @Test
    void 未知带值关键词应保持原样() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[未知:值]");
        assertEquals("[未知:值]", 结果);
    }

    @Test
    void 参数占位符应被替换() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("造成{0}点伤害", 100);
        assertEquals("造成100点伤害", 结果);
    }

    @Test
    void 多参数占位符应按顺序替换() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("{0}消耗{1}点{2}", "火球术", 50, "秘能");
        assertEquals("火球术消耗50点秘能", 结果);
    }

    @Test
    void 超出索引的参数应保持原样() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("值{0}和{1}", "甲");
        assertEquals("值甲和{1}", 结果);
    }

    @Test
    void 技能槽位在纯文本模式下应返回默认槽位名() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[第一技能]");
        assertEquals("<red>第一技能</red>", 结果);
    }

    @Test
    void 自定义技能名解析器应能提供技能名() {
        关键词解析器 解析器 = new 关键词解析器((槽位, 玩家) -> java.util.Optional.of("奥能弹射"));
        String 结果 = 解析器.解析("[第一技能]");
        assertEquals("<red>奥能弹射</red>", 结果);
    }

    @Test
    void 自定义技能名解析器返回空时应使用默认名() {
        关键词解析器 解析器 = new 关键词解析器((槽位, 玩家) -> java.util.Optional.empty());
        String 结果 = 解析器.解析("[第二技能]");
        assertEquals("<gold>第二技能</gold>", 结果);
    }

    @Test
    void 综合文本应正确解析() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 文本 = "释放[魔法]造成{0}点伤害，触发[暴击]";
        String 结果 = 解析器.解析(文本, 36);
        assertEquals("释放<blue>魔法</blue>造成36点伤害，触发<gold><bold>暴击</bold></gold>", 结果);
    }

    @Test
    void 奥术护盾关键词应着色为黄色() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[奥术护盾]");
        assertEquals("<yellow>奥术护盾</yellow>", 结果);
    }

    @Test
    void 奥术护盾释放日志应正确渲染() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 文本 = "释放了[第三技能]，获得了可以吸收[伤害数值:2.0]点伤害的[奥术护盾]。";
        String 结果 = 解析器.解析(文本);
        assertEquals("释放了<yellow>第三技能</yellow>，获得了可以吸收<dark_red>2.0</dark_red>点伤害的<yellow>奥术护盾</yellow>。", 结果);
    }
}
