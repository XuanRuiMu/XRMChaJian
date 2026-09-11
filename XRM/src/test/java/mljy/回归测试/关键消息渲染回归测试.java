package mljy.回归测试;

import mljy.基础设施层.XRM技能名解析器;
import mljy.翻译服务;
import mljy.玩家服务;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import 暮澜纪元.通用.文本.关键词解析器;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@DisplayName("FP-08 XRM 关键消息渲染回归测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 关键消息渲染回归测试 {

    @Mock
    private 玩家服务 玩家服务;

    private 关键词解析器 关键词解析器;
    private YamlConfiguration zh配置;
    private YamlConfiguration en配置;

    static Stream<Arguments> 提供双语() {
        return Stream.of(Arguments.of("zh"), Arguments.of("en"));
    }

    @BeforeEach
    void setUp() throws Exception {
        zh配置 = new YamlConfiguration();
        加载翻译文件(zh配置, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(zh配置, "src/main/resources/文本消息/战斗日志/zh.yml");

        en配置 = new YamlConfiguration();
        加载翻译文件(en配置, "src/main/resources/文本消息/common/en.yml");
        加载翻译文件(en配置, "src/main/resources/文本消息/战斗日志/en.yml");

        测试翻译服务 zh服务 = new 测试翻译服务(zh配置);
        测试翻译服务 en服务 = new 测试翻译服务(en配置);
        // XRM 实际链路：关键词解析器注入 XRM技能名解析器
        // 用 zh 服务作为基准，en 链路单独构造验证
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(zh服务, 玩家服务));
        lenient().when(玩家服务.获取会话(ArgumentMatchers.any(UUID.class))).thenReturn(java.util.Optional.empty());
    }

    private void 加载翻译文件(YamlConfiguration 目标, String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                目标.set(键, 配置.get(键));
            }
        }
    }

    private String 解析并序列化(YamlConfiguration 配置, String 键, Object... 参数) {
        测试翻译服务 服务 = new 测试翻译服务(配置);
        关键词解析器 解析器 = new 关键词解析器(new XRM技能名解析器(服务, 玩家服务));
        String 文本 = 配置.getString(键);
        String 解析后 = 解析器.解析(文本, UUID.randomUUID(), 参数);
        return MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));
    }

    private String 获取可见文本(String 序列化) {
        return 序列化.replaceAll("<[^>]+>", "");
    }

    @Nested
    @DisplayName("zh 关键消息渲染")
    class zh关键消息渲染 {

        @Test
        @DisplayName("zh普通攻击消息应正确着色[伤害数值]关键词")
        void zh普通攻击消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");
            assertTrue(序列化.contains("你用"), "应包含'你用'，实际: " + 序列化);
            assertTrue(序列化.contains("铁剑"), "应包含'铁剑'，实际: " + 序列化);
            assertTrue(序列化.contains("100.0"), "应包含伤害数值'100.0'，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_red>"), "[伤害数值]应着色<dark_red>，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh技能伤害消息应正确着色[伤害数值]关键词")
        void zh技能伤害消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.技能伤害", "奥术冲击", "僵尸", "250.0");
            assertTrue(序列化.contains("奥术冲击"), "应包含技能名，实际: " + 序列化);
            assertTrue(序列化.contains("250.0"), "应包含伤害数值'250.0'，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_red>"), "[伤害数值]应着色<dark_red>，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh普通攻击暴击消息应正确着色[暴击]关键词")
        void zh普通攻击暴击消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.普通攻击暴击", "铁剑", "僵尸", "100.0");
            assertTrue(序列化.contains("<gold>") && 序列化.contains("<bold>") && 序列化.contains("暴击"),
                    "[暴击]应着色<gold><bold>，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh受到伤害消息应为无裸括号的纯文本格式（FP-A点9）")
        void zh受到伤害消息应为纯文本格式() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.受到伤害", "僵尸", "50.0");
            assertTrue(序列化.contains("<dark_red>50.0</dark_red>"),
                    "伤害数值应着色<dark_red>，实际: " + 序列化);
            assertFalse(序列化.contains("[伤害数值:"), "不应包含裸括号'[伤害数值:'，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh奥术护盾吸收日志应渲染为指定单行文本")
        void zh奥术护盾吸收日志应为指定文本() {
            String 序列化 = 解析并序列化(
                    zh配置, "战斗日志.吸收", "奥术护盾", "2.0");

            assertEquals(
                    "被奥术护盾抵挡了2.0点伤害。",
                    获取可见文本(序列化));
        }

        @Test
        @DisplayName("zh吸血日志应正确嵌套着色三个关键词")
        void zh吸血日志应正确嵌套着色() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.吸血", "30.0");
            assertTrue(序列化.contains("<dark_red>吸血</dark_red>"),
                    "[吸血]应着色<dark_red>，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_green>") && 序列化.contains("30.0"),
                    "[治疗数值:30.0]应着色<dark_green>，实际: " + 序列化);
            assertTrue(序列化.contains("生命值"),
                    "应包含'生命值'文本，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh脱战回血消息应正确着色[治疗数值]与[生命值]关键词")
        void zh脱战回血消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "战斗日志.脱战回血", "50.0");
            assertTrue(序列化.contains("<dark_green>") && 序列化.contains("50.0"),
                    "[治疗数值:50.0]应着色<dark_green>，实际: " + 序列化);
            assertTrue(序列化.contains("生命值"),
                    "应包含'生命值'文本，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh通用错误参数不足消息应正确着色[错误]关键词")
        void zh通用错误参数不足消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "通用.错误.参数不足");
            assertTrue(序列化.contains("<red>[错误]</red>"),
                    "[错误]应着色<red>，实际: " + 序列化);
            assertTrue(序列化.contains("参数不足"), "应包含'参数不足'文本，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh通用提示操作成功消息应正确着色[成功]关键词")
        void zh通用提示操作成功消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "通用.提示.操作成功");
            assertTrue(序列化.contains("<green>[成功]</green>"),
                    "[成功]应着色<green>，实际: " + 序列化);
        }

        @Test
        @DisplayName("zh通用用法前缀消息应正确着色[警告]关键词")
        void zh通用用法前缀消息应正确着色() {
            String 序列化 = 解析并序列化(zh配置, "通用.用法.通用前缀", "/xrm help");
            assertTrue(序列化.contains("<yellow>[警告]</yellow>"),
                    "[警告]应着色<yellow>，实际: " + 序列化);
            assertTrue(序列化.contains("/xrm help"), "应包含参数'/xrm help'，实际: " + 序列化);
        }
    }

    @Nested
    @DisplayName("en 关键消息渲染")
    class en关键消息渲染 {

        @Test
        @DisplayName("en普通攻击消息应正确着色[damage value]关键词")
        void en普通攻击消息应正确着色() {
            String 序列化 = 解析并序列化(en配置, "战斗日志.普通攻击", "Iron Sword", "Zombie", "100.0");
            assertTrue(序列化.contains("100.0"), "应包含伤害数值'100.0'，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_red>"), "[伤害数值]应着色<dark_red>，实际: " + 序列化);
        }

        @Test
        @DisplayName("en技能伤害消息应正确着色[damage value]关键词")
        void en技能伤害消息应正确着色() {
            String 序列化 = 解析并序列化(en配置, "战斗日志.技能伤害", "Arcane Blast", "Zombie", "250.0");
            assertTrue(序列化.contains("250.0"), "应包含伤害数值'250.0'，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_red>"), "[伤害数值]应着色<dark_red>，实际: " + 序列化);
            assertTrue(序列化.contains("Arcane Blast"), "应包含技能名，实际: " + 序列化);
        }

        @Test
        @DisplayName("en奥术护盾吸收日志应使用英文回退键")
        void en奥术护盾吸收日志应使用英文键() {
            String 序列化 = 解析并序列化(
                    en配置, "战斗日志.吸收", "Arcane Shield", "2.0");

            assertEquals(
                    "[Arcane Shield] blocked 2.0 damage.",
                    获取可见文本(序列化));
        }

        @Test
        @DisplayName("en吸血日志应正确嵌套着色三个关键词")
        void en吸血日志应正确嵌套着色() {
            String 序列化 = 解析并序列化(en配置, "战斗日志.吸血", "30.0");
            assertTrue(序列化.contains("<dark_red>") && 序列化.contains("吸血"),
                    "[吸血]应着色<dark_red>，实际: " + 序列化);
            assertTrue(序列化.contains("<dark_green>") && 序列化.contains("30.0"),
                    "[治疗数值:30.0]应着色<dark_green>，实际: " + 序列化);
            assertTrue(序列化.contains("生命值"),
                    "应包含'生命值'文本，实际: " + 序列化);
        }

        @Test
        @DisplayName("en通用错误参数不足消息应正确着色[错误]关键词")
        void en通用错误参数不足消息应正确着色() {
            String 序列化 = 解析并序列化(en配置, "通用.错误.参数不足");
            assertTrue(序列化.contains("<red>[错误]</red>"),
                    "[错误]应着色<red>，实际: " + 序列化);
            assertTrue(序列化.contains("Insufficient"), "应包含英文'Insufficient'文本，实际: " + 序列化);
        }

        @Test
        @DisplayName("en通用提示操作成功消息应正确着色[成功]关键词")
        void en通用提示操作成功消息应正确着色() {
            String 序列化 = 解析并序列化(en配置, "通用.提示.操作成功");
            assertTrue(序列化.contains("<green>[成功]</green>"),
                    "[成功]应着色<green>，实际: " + 序列化);
        }
    }

    @Nested
    @DisplayName("zh/en 渲染一致性回归")
    class 渲染一致性 {

        @ParameterizedTest(name = "语言={0}")
        @MethodSource("mljy.回归测试.关键消息渲染回归测试#提供双语")
        @DisplayName("zh/en下同一关键词[物理]应产生相同MiniMessage标签")
        void zhEn下同一关键词应产生相同标签(String 语言) {
            关键词解析器 解析器 = 暮澜纪元.通用.文本.关键词解析器.创建纯文本();
            String 结果 = 解析器.解析("[物理]");
            assertTrue(结果.contains("<gray>物理</gray>"),
                    "语言=" + 语言 + " [物理] 应着色为 <gray>物理</gray>，实际: " + 结果);
        }

        @Test
        @DisplayName("zh/en下[伤害数值:X]应产生相同dark_red标签(回归基准)")
        void zhEn下伤害数值应产生相同标签() {
            关键词解析器 解析器 = 暮澜纪元.通用.文本.关键词解析器.创建纯文本();
            String zh结果 = 解析器.解析("[伤害数值:100.0]");
            String en结果 = 解析器.解析("[伤害数值:100.0]");
            assertTrue(zh结果.contains("<dark_red>100.0</dark_red>"),
                    "zh [伤害数值:100.0] 应着色为 <dark_red>，实际: " + zh结果);
            assertTrue(en结果.contains("<dark_red>100.0</dark_red>"),
                    "en [伤害数值:100.0] 应着色为 <dark_red>，实际: " + en结果);
        }

        @Test
        @DisplayName("zh/en下[吸血][治疗数值:X][生命值]三关键词嵌套应产生相同标签")
        void zhEn下吸血三关键词嵌套应产生相同标签() {
            关键词解析器 解析器 = 暮澜纪元.通用.文本.关键词解析器.创建纯文本();
            String zh结果 = 解析器.解析("通过[吸血]恢复了[治疗数值:30.0]点[生命值]");
            String en结果 = 解析器.解析("recovered [治疗数值:30.0] [生命值] via [吸血]");
            // 两语言下三个关键词应分别着色一致
            assertTrue(zh结果.contains("<dark_red>吸血</dark_red>"), "zh [吸血]应着色，实际: " + zh结果);
            assertTrue(en结果.contains("<dark_red>吸血</dark_red>"), "en [吸血]应着色，实际: " + en结果);
            assertTrue(zh结果.contains("<dark_green>30.0</dark_green>"), "zh [治疗数值:30.0]应着色，实际: " + zh结果);
            assertTrue(en结果.contains("<dark_green>30.0</dark_green>"), "en [治疗数值:30.0]应着色，实际: " + en结果);
            assertTrue(zh结果.contains("<dark_green>") && zh结果.contains("生命值"), "zh [生命值]应着色，实际: " + zh结果);
            assertTrue(en结果.contains("<dark_green>") && en结果.contains("生命值"), "en [生命值]应着色，实际: " + en结果);
        }
    }

    private static class 测试翻译服务 implements 翻译服务 {
        private final YamlConfiguration 配置;

        测试翻译服务(YamlConfiguration 配置) {
            this.配置 = 配置;
        }

        @Override
        public String 获取(String 键, Locale 语言, Object... 参数) {
            return 获取(键, 参数);
        }

        @Override
        public String 获取(String 键, Object... 参数) {
            String 文本 = 配置.getString(键);
            return 文本 != null ? 文本 : 键;
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
