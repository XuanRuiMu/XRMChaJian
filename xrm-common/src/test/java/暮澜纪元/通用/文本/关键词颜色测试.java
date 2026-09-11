package 暮澜纪元.通用.文本;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class 关键词颜色测试 {

    @Test
    void 魔法颜色标签应为蓝色() {
        关键词颜色 颜色 = 关键词颜色.魔法;
        assertEquals("<blue>", 颜色.获取MiniMessage开标签());
        assertEquals("</blue>", 颜色.获取MiniMessage关标签());
    }

    @Test
    void 物理颜色标签应为灰色() {
        关键词颜色 颜色 = 关键词颜色.物理;
        assertEquals("<gray>", 颜色.获取MiniMessage开标签());
        assertEquals("</gray>", 颜色.获取MiniMessage关标签());
    }

    @Test
    void 蓄力颜色应包含加粗() {
        关键词颜色 颜色 = 关键词颜色.蓄力;
        assertEquals("<aqua><bold>", 颜色.获取MiniMessage开标签());
        assertEquals("</bold></aqua>", 颜色.获取MiniMessage关标签());
    }

    @Test
    void 混乱颜色应为加粗深红色() {
        关键词颜色 颜色 = 关键词颜色.混乱;
        assertEquals("<dark_red><bold>", 颜色.获取MiniMessage开标签());
        assertEquals("</bold></dark_red>", 颜色.获取MiniMessage关标签());
    }

    @Test
    void 格式化MiniMessage应拼接开闭标签() {
        关键词颜色 颜色 = 关键词颜色.魔法;
        String 结果 = 颜色.格式化MiniMessage("魔法");
        assertEquals("<blue>魔法</blue>", 结果);
    }

    @Test
    void 传统颜色代码应正确返回() {
        assertEquals("§9", 关键词颜色.魔法.获取传统颜色代码());
        assertEquals("§7", 关键词颜色.物理.获取传统颜色代码());
        assertEquals("§b§l", 关键词颜色.蓄力.获取传统颜色代码());
        assertEquals("§4§l", 关键词颜色.混乱.获取传统颜色代码());
    }

    @Test
    void 格式化传统代码应前缀拼接() {
        关键词颜色 颜色 = 关键词颜色.魔法;
        assertEquals("§9魔法", 颜色.格式化传统代码("魔法"));
    }

    @Test
    void 显示文本默认为枚举名() {
        assertEquals("魔法", 关键词颜色.魔法.获取显示文本());
        assertEquals("物理", 关键词颜色.物理.获取显示文本());
    }

    @Test
    void 自定义显示文本应正确返回() {
        assertEquals("[技能日志]", 关键词颜色.技能日志前缀.获取显示文本());
        assertEquals("[错误]", 关键词颜色.错误.获取显示文本());
    }

    @Test
    void 所有枚举值应可遍历() {
        int 数量 = 关键词颜色.values().length;
        assertNotNull(关键词颜色.values());
        assertEquals(true, 数量 > 50);
    }

    @Test
    void 技能槽位颜色应正确() {
        assertEquals("<red>", 关键词颜色.第一技能.获取MiniMessage开标签());
        assertEquals("<gold>", 关键词颜色.第二技能.获取MiniMessage开标签());
        assertEquals("<white>", 关键词颜色.第九技能.获取MiniMessage开标签());
    }
}
