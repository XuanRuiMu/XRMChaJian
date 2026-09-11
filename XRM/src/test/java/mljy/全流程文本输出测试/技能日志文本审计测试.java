package mljy.全流程文本输出测试;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import 暮澜纪元.通用.文本.关键词解析器;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("技能日志翻译文本审计")
class 技能日志文本审计测试 {

    private static final Path 技能日志目录 = Path.of("src/main/resources/文本消息/技能日志");
    // FP-1：汇总日志模板改为仅传 伤害数值+秘能数值（不再传目标数量）
    private static final String 秘法陨落中文聚合模板 =
            "[第四技能]造成了[伤害数值:{0}]点伤害，获得了{1}点[秘能]。";
    private static final String 秘法陨落英文聚合模板 =
            "Arcane Meteor dealt {0} damage. Gained {1} Arcane Power.";

    @Test
    @DisplayName("所有语言文件不得残留CR/LF注入文本")
    void 所有语言文件不得残留CRLF注入文本() throws Exception {
        for (Path 文件 : 获取语言文件()) {
            YamlConfiguration 配置 = 加载(文件);
            for (String 键 : 配置.getKeys(true)) {
                if (配置.isConfigurationSection(键) || !(配置.get(键) instanceof String 文本)) {
                    continue;
                }
                assertFalse(文本.contains("\r"), 文件 + "." + 键 + " 不得包含 CR");
                assertFalse(文本.contains("\n"), 文件 + "." + 键 + " 不得包含 LF");
                assertFalse(文本.contains("\\r"), 文件 + "." + 键 + " 不得包含 \\r");
                assertFalse(文本.contains("\\n"), 文件 + "." + 键 + " 不得包含 \\n");
            }
        }
    }

    @Test
    @DisplayName("秘能关键词格式保持现有规范")
    void 秘能关键词格式保持现有规范() throws Exception {
        for (Path 文件 : 获取语言文件()) {
            YamlConfiguration 配置 = 加载(文件);
            assertTrue(配置.getString("技能日志.命中仅资源").contains("[秘能]"),
                    文件 + " 的命中仅资源必须保留 [秘能]");
            assertTrue(配置.getString("技能日志.命中仅资源带秘兆").contains("[秘能]"),
                    文件 + " 的命中仅资源带秘兆必须保留 [秘能]");
        }
    }

    @Test
    @DisplayName("秘法陨落聚合文案与玩家可见秘兆格式准确")
    void 秘法陨落聚合文案与玩家可见秘兆格式准确() throws Exception {
        YamlConfiguration 中文 = 加载(技能日志目录.resolve("zh.yml"));
        YamlConfiguration 英文 = 加载(技能日志目录.resolve("en.yml"));

        assertEquals(秘法陨落中文聚合模板, 中文.getString("技能日志.秘法陨落范围命中"));
        assertEquals(秘法陨落英文聚合模板, 英文.getString("技能日志.秘法陨落范围命中"));

        String 中文秘兆日志 = 关键词解析器.创建纯文本()
                .解析(中文.getString("技能日志.命中仅资源带秘兆"), "2");
        assertTrue(中文秘兆日志.contains("获得了<light_purple><bold>秘兆</bold></light_purple>效果。"));
        assertFalse(中文秘兆日志.contains("[秘兆]"));

        String 中文聚合日志 = 关键词解析器.创建纯文本()
                .解析(中文.getString("技能日志.秘法陨落范围命中"), "42.5", "2");
        assertEquals("<green>第四技能</green>造成了<dark_red>42.5</dark_red>点伤害，获得了2点<dark_purple><bold>秘能</bold></dark_purple>。", 中文聚合日志);
    }

    @Test
    @DisplayName("问题4：秘法陨落带秘兆汇总键文案与物理消息规范（秘兆拼入同一条）")
    void 秘法陨落带秘兆汇总键文案与物理消息规范() throws Exception {
        YamlConfiguration 中文 = 加载(技能日志目录.resolve("zh.yml"));
        YamlConfiguration 英文 = 加载(技能日志目录.resolve("en.yml"));

        String 中文模板 = 中文.getString("技能日志.秘法陨落范围命中带秘兆");
        String 英文模板 = 英文.getString("技能日志.秘法陨落范围命中带秘兆");
        assertEquals("[第四技能]造成了[伤害数值:{0}]点伤害，获得了{1}点[秘能]，获得了<light_purple><bold>秘兆</bold></light_purple>效果。", 中文模板);
        assertEquals("Arcane Meteor dealt {0} damage. Gained {1} Arcane Power, gained <light_purple><bold>秘兆</bold></light_purple> effect.", 英文模板);

        关键词解析器 纯文本解析器 = 关键词解析器.创建纯文本();
        String 物理消息 = 纯文本解析器.解析(中文模板, "42.5", "2");
        assertTrue(物理消息.contains("获得了<light_purple><bold>秘兆</bold></light_purple>效果。"),
                "带秘兆汇总必须含秘兆效果，实际：" + 物理消息);
        assertFalse(物理消息.contains("[秘兆]"), "不得残留[秘兆]方括号，实际：" + 物理消息);
        assertFalse(物理消息.contains("\r"), "不得含CR");
        assertFalse(物理消息.contains("\n"), "不得含LF");
    }

    @Test
    @DisplayName("带秘兆日志的最终物理消息不得输出方括号或换行")
    void 带秘兆日志最终物理消息不得输出方括号或换行() throws Exception {
        YamlConfiguration 中文 = 加载(技能日志目录.resolve("zh.yml"));
        关键词解析器 纯文本解析器 = 关键词解析器.创建纯文本();

        String 单体日志 = 纯文本解析器.解析(
                中文.getString("技能日志.命中仅资源带秘兆"), "2");
        String 范围日志 = 纯文本解析器.解析(
                中文.getString("技能日志.范围命中仅资源带秘兆"), "三", "3", "42.5", "奥术", "2");
        String 汇总日志 = 纯文本解析器.解析(
                中文.getString("技能日志.秘法陨落范围命中"), "42.5", "2");
        String 最终物理消息 = String.join(" ", 单体日志, 范围日志, 汇总日志);

        assertTrue(单体日志.contains("获得了<light_purple><bold>秘兆</bold></light_purple>效果。"));
        assertTrue(范围日志.contains("获得了<light_purple><bold>秘兆</bold></light_purple>效果。"));
        assertFalse(最终物理消息.contains("[秘兆]"));
        assertFalse(最终物理消息.contains("\r"));
        assertFalse(最终物理消息.contains("\n"));
    }

    private List<Path> 获取语言文件() throws Exception {
        try (var 文件流 = Files.list(技能日志目录)) {
            return 文件流.filter(文件 -> 文件.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        }
    }

    private YamlConfiguration 加载(Path 文件) throws Exception {
        try (var 输入流 = Files.newInputStream(文件)) {
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(输入流, StandardCharsets.UTF_8));
        }
    }
}
