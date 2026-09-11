package mljy.回归测试;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import 暮澜纪元.通用.文本.关键词解析器;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FP-08 XRM 18 语言回归测试")
class XRM18语言回归测试 {

    private static final String 消息管理器目录 = "src/main/resources/文本消息/配置/消息管理器/";

    static Stream<Arguments> 提供18语言() {
        return Stream.of(
                Arguments.of("zh"), Arguments.of("zh_tw"), Arguments.of("en"),
                Arguments.of("ja"), Arguments.of("ko"), Arguments.of("fr"),
                Arguments.of("de"), Arguments.of("es"), Arguments.of("it"),
                Arguments.of("pt"), Arguments.of("ru"), Arguments.of("th"),
                Arguments.of("vi"), Arguments.of("id"), Arguments.of("ms"),
                Arguments.of("nl"), Arguments.of("pl"), Arguments.of("tr")
        );
    }

    static Stream<Arguments> 提供双语战斗日志() {
        return Stream.of(Arguments.of("zh"), Arguments.of("en"));
    }

    private YamlConfiguration 加载语言文件(String 语言, String 目录路径) throws Exception {
        File 文件 = new File(目录路径 + 语言 + ".yml");
        assertTrue(文件.exists(), "语言文件应存在: " + 文件.getPath());
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
    }

    private String 解析并序列化(String 文本) {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 解析后 = 解析器.解析(文本);
        return MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言消息管理器文件应存在且包含5个核心消息键")
    void 语言文件应存在且包含核心消息键(String 语言) throws Exception {
        YamlConfiguration 配置 = 加载语言文件(语言, 消息管理器目录);
        assertNotNull(配置.get("消息管理器.初始化.完成"), "语言=" + 语言 + " 缺少 初始化.完成");
        assertNotNull(配置.get("消息管理器.重载.完成"), "语言=" + 语言 + " 缺少 重载.完成");
        assertNotNull(配置.get("消息管理器.日志.文件创建失败"), "语言=" + 语言 + " 缺少 日志.文件创建失败");
        assertNotNull(配置.get("消息管理器.日志.写入失败"), "语言=" + 语言 + " 缺少 日志.写入失败");
        assertNotNull(配置.get("消息管理器.日志.找不到语言文件"), "语言=" + 语言 + " 缺少 日志.找不到语言文件");
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言初始化完成消息应将[成功]关键词着色为绿色")
    void 初始化完成消息应将成功关键词着色为绿色(String 语言) throws Exception {
        YamlConfiguration 配置 = 加载语言文件(语言, 消息管理器目录);
        String 文本 = 配置.getString("消息管理器.初始化.完成");

        String 序列化 = 解析并序列化(文本);

        assertTrue(序列化.contains("<green>"), "语言=" + 语言 + " [成功] 应着色为 <green>，实际: " + 序列化);
        assertTrue(序列化.contains("[成功]"), "语言=" + 语言 + " 应保留 [成功] 显示文本，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言文件创建失败消息应将[错误]关键词着色为红色且参数{0}被替换")
    void 文件创建失败消息应将错误关键词着色为红色且参数被替换(String 语言) throws Exception {
        YamlConfiguration 配置 = 加载语言文件(语言, 消息管理器目录);
        String 文本 = 配置.getString("消息管理器.日志.文件创建失败");

        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 解析后 = 解析器.解析(文本, "test.log");
        String 序列化 = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));

        assertTrue(序列化.contains("<red>"), "语言=" + 语言 + " [错误] 应着色为 <red>，实际: " + 序列化);
        assertTrue(序列化.contains("test.log"), "语言=" + 语言 + " {0} 应被替换为 test.log，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言找不到语言文件消息应将[警告]关键词着色为黄色")
    void 找不到语言文件消息应将警告关键词着色为黄色(String 语言) throws Exception {
        YamlConfiguration 配置 = 加载语言文件(语言, 消息管理器目录);
        String 文本 = 配置.getString("消息管理器.日志.找不到语言文件");

        String 序列化 = 解析并序列化(文本);

        assertTrue(序列化.contains("<yellow>"), "语言=" + 语言 + " [警告] 应着色为 <yellow>，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言关键词着色输出应与中文基准一致([成功][错误][警告]颜色一致)")
    void 关键词着色输出应与中文基准一致(String 语言) throws Exception {
        YamlConfiguration 配置 = 加载语言文件(语言, 消息管理器目录);

        String 成功文本 = 配置.getString("消息管理器.初始化.完成");
        String 错误文本 = 配置.getString("消息管理器.日志.文件创建失败");
        String 警告文本 = 配置.getString("消息管理器.日志.找不到语言文件");

        String 成功序列化 = 解析并序列化(成功文本);
        String 错误序列化 = 解析并序列化(错误文本);
        String 警告序列化 = 解析并序列化(警告文本);

        assertTrue(成功序列化.contains("<green>[成功]</green>"),
                "语言=" + 语言 + " [成功] 着色应为 <green>[成功]</green>，实际: " + 成功序列化);
        assertTrue(错误序列化.contains("<red>[错误]</red>"),
                "语言=" + 语言 + " [错误] 着色应为 <red>[错误]</red>，实际: " + 错误序列化);
        assertTrue(警告序列化.contains("<yellow>[警告]</yellow>"),
                "语言=" + 语言 + " [警告] 着色应为 <yellow>[警告]</yellow>，实际: " + 警告序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供双语战斗日志")
    @DisplayName("zh/en战斗日志普通攻击消息应正确着色[伤害数值]关键词")
    void 战斗日志普通攻击消息应正确着色伤害数值关键词(String 语言) throws Exception {
        String 目录 = "src/main/resources/文本消息/战斗日志/";
        YamlConfiguration 配置 = 加载语言文件(语言, 目录);
        String 文本 = 配置.getString("战斗日志.普通攻击");

        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 解析后 = 解析器.解析(文本, "铁剑", "僵尸", "100.0");
        String 序列化 = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));

        assertTrue(序列化.contains("100.0"), "语言=" + 语言 + " 应包含伤害数值 100.0，实际: " + 序列化);
        assertTrue(序列化.contains("<dark_red>"), "语言=" + 语言 + " [伤害数值] 应着色为 <dark_red>，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供双语战斗日志")
    @DisplayName("zh/en战斗日志技能伤害消息应正确着色技能名和[伤害数值]关键词")
    void 战斗日志技能伤害消息应正确着色技能名和伤害数值关键词(String 语言) throws Exception {
        String 目录 = "src/main/resources/文本消息/战斗日志/";
        YamlConfiguration 配置 = 加载语言文件(语言, 目录);
        String 文本 = 配置.getString("战斗日志.技能伤害");

        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 解析后 = 解析器.解析(文本, "奥术冲击", "僵尸", "250.0");
        String 序列化 = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));

        assertTrue(序列化.contains("250.0"), "语言=" + 语言 + " 应包含伤害数值 250.0，实际: " + 序列化);
        assertTrue(序列化.contains("<dark_red>"), "语言=" + 语言 + " [伤害数值] 应着色为 <dark_red>，实际: " + 序列化);
        assertTrue(序列化.contains("奥术冲击"), "语言=" + 语言 + " 应包含技能名 奥术冲击，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供双语战斗日志")
    @DisplayName("zh/en战斗日志吸血消息应正确嵌套着色[吸血][治疗数值][生命值]三个关键词")
    void 战斗日志吸血消息应正确嵌套着色三个关键词(String 语言) throws Exception {
        String 目录 = "src/main/resources/文本消息/战斗日志/";
        YamlConfiguration 配置 = 加载语言文件(语言, 目录);
        String 文本 = 配置.getString("战斗日志.吸血");

        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 解析后 = 解析器.解析(文本, "30.0");
        String 序列化 = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(解析后));

        assertTrue(序列化.contains("<dark_red>") && 序列化.contains("吸血"),
                "语言=" + 语言 + " [吸血] 应着色为 <dark_red>，实际: " + 序列化);
        assertTrue(序列化.contains("<dark_green>") && 序列化.contains("30.0"),
                "语言=" + 语言 + " [治疗数值:30.0] 应着色为 <dark_green>，实际: " + 序列化);
        assertTrue(序列化.contains("<dark_green>") && 序列化.contains("生命值"),
                "语言=" + 语言 + " [生命值] 应着色为 <dark_green>，实际: " + 序列化);
    }

    @ParameterizedTest(name = "语言={0}")
    @MethodSource("提供18语言")
    @DisplayName("18语言下同一关键词[物理]应产生相同MiniMessage标签(回归基准)")
    void 同一关键词在18语言下应产生相同MiniMessage标签(String 语言) throws Exception {
        String 期望 = "<gray>物理</gray>";

        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 结果 = 解析器.解析("[物理]");

        assertTrue(结果.contains(期望),
                "语言=" + 语言 + " [物理] 应着色为 " + 期望 + "，实际: " + 结果);
    }
}
