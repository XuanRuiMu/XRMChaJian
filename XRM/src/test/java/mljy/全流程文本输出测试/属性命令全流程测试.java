package mljy.全流程文本输出测试;

import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.属性计算服务;
import mljy.业务层.属性计算服务实现;
import mljy.玩家服务;
import mljy.属性服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.表现层.命令.属性指令.属性命令处理器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家会话;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-05 属性命令全流程文本输出测试。
 *
 * 验证玩家执行 /sx（属性查看）命令时，属性命令处理器实际发送给 CommandSender 的
 * 文本内容符合翻译文件 + 关键词解析器规范。
 *
 * 使用 ArgumentCaptor 捕获 CommandSender.sendMessage 的 Component 参数，
 * 用 PlainTextComponentSerializer 验证文本片段，用 MiniMessage.serialize 验证颜色标签。
 *
 * 注意（基于翻译文件实际内容）：
 * - 属性名.力量 = "[力量]"（着色为<red>）
 * - 属性名.躲闪几率 = "[躲闪]几率"（[躲闪]着色为<light_purple>，"几率"不着色）
 * - 属性名.法术暴击几率 = "[法术暴击几率]"（着色为<yellow>）
 * - 属性名.吸血 = "[吸血]"（着色为<dark_red>）
 * - 属性名.公共冷却时间 = "[公共冷却时间]"（着色为<dark_gray>，时间数值不单独着色）
 */
@DisplayName("FP-TEXT-05 属性命令全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 属性命令全流程测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性服务 属性服务;
    @Mock
    private Player 玩家;
    @Mock
    private Command 命令;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 属性命令处理器 处理器;

    private UUID 玩家标识;
    private 玩家会话 会话;
    private 属性快照 属性;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/命令/属性指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        修饰器管理器 修饰器管理器 = new 修饰器管理器();
        属性计算服务 属性计算服务 = new 属性计算服务实现(修饰器管理器);
        处理器 = new 属性命令处理器(翻译服务, 关键词解析器, 玩家服务, 属性服务, 属性计算服务, 修饰器管理器);

        玩家标识 = UUID.randomUUID();
        会话 = new 玩家会话(玩家标识, "测试玩家");
        属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);

        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("测试玩家");
        when(玩家.getHealth()).thenReturn(80.0);
        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
        when(属性服务.计算属性(玩家标识)).thenReturn(属性);
        when(属性服务.获取基础属性(玩家标识)).thenReturn(属性);
    }

    private void 加载翻译文件(YamlConfiguration 合并配置, String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                合并配置.set(键, 配置.get(键));
            }
        }
    }

    private List<String> 捕获纯文本消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        org.mockito.Mockito.verify(发送者, org.mockito.Mockito.atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
    }

    private List<String> 捕获MiniMessage消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        org.mockito.Mockito.verify(发送者, org.mockito.Mockito.atLeastOnce()).sendMessage(捕获器.capture());
        MiniMessage mm = MiniMessage.miniMessage();
        return 捕获器.getAllValues().stream()
                .map(mm::serialize)
                .toList();
    }

    private String 查找包含行(List<String> 消息列表, String 文本) {
        return 消息列表.stream()
                .filter(消息 -> 消息.contains(文本))
                .findFirst()
                .orElse("");
    }

    @Nested
    @DisplayName("主属性全流程：力量/敏捷/智性行")
    class 主属性全流程 {

        @Test
        @DisplayName("力量行：捕获实际输出应包含'力量'文本和数值（力量翻译值无方括号不着色）")
        void 力量行_应输出力量文本和数值() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 消息列表 = 捕获纯文本消息(玩家);
            String 力量行 = 查找包含行(消息列表, "力量");
            assertTrue(力量行.contains("力量"), "应包含'力量'文本，实际：" + 力量行);
            assertTrue(力量行.contains("50.0"), "应包含力量数值'50.0'，实际：" + 力量行);
        }

        @Test
        @DisplayName("力量行：捕获实际MiniMessage输出应包含[力量]颜色标签（red）")
        void 力量行_应包含力量颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 力量翻译值是"[力量]"，着色为 <red>力量</red>
            assertTrue(全部消息.contains("<red>力量</red>"),
                    "[力量]应着色为<red>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("敏捷行：捕获实际MiniMessage输出应包含[敏捷]颜色标签（green）")
        void 敏捷行_应包含敏捷颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 敏捷翻译值是"[敏捷]"，着色为 <green>敏捷</green>
            assertTrue(全部消息.contains("<green>敏捷</green>"),
                    "[敏捷]应着色为<green>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("智力行：捕获实际MiniMessage输出应包含[智力]颜色标签（blue）")
        void 智力行_应包含智力颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 智力翻译值是"[智力]"，着色为 <blue>智力</blue>
            assertTrue(全部消息.contains("<blue>智力</blue>"),
                    "[智力]应着色为<blue>，实际：" + 全部消息);
        }
    }

    @Nested
    @DisplayName("生存属性全流程：生命值/躲闪几率行")
    class 生存属性全流程 {

        @Test
        @DisplayName("生命值行：捕获实际输出应包含当前/最大生命值")
        void 生命值行_应输出当前和最大生命值() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 消息列表 = 捕获纯文本消息(玩家);
            String 生命值行 = 查找包含行(消息列表, "生命值");
            assertTrue(生命值行.contains("80.0"), "应包含当前生命值'80.0'，实际：" + 生命值行);
            assertTrue(生命值行.contains("100.0"), "应包含最大生命值'100.0'，实际：" + 生命值行);
        }

        @Test
        @DisplayName("躲闪几率行：捕获实际MiniMessage输出应包含[躲闪]颜色标签（light_purple）")
        void 躲闪几率行_应包含躲闪颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 躲闪行 = 查找包含行(mm消息, "躲闪");
            // 躲闪行整体包裹在 <light_purple> 中，[躲闪]关键词颜色与外层相同，序列化后不会保留内层标签
            assertTrue(躲闪行.startsWith("<light_purple>躲闪"),
                    "躲闪行应以<light_purple>躲闪开头，实际：" + 躲闪行);
        }

        @Test
        @DisplayName("躲闪几率行：整条属性应包裹在light_purple中（FP-SX验收标准）")
        void 躲闪几率行_整条应为粉色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 躲闪行 = 查找包含行(mm消息, "躲闪");
            assertTrue(躲闪行.startsWith("<light_purple>"),
                    "躲闪行应以<light_purple>开头，实际：" + 躲闪行);
            assertTrue(躲闪行.contains("躲闪几率"),
                    "躲闪行应包含'躲闪几率'，实际：" + 躲闪行);
        }

        @Test
        @DisplayName("生命值行：斜杠应使用白色（FP-SX验收标准）")
        void 生命值行_斜杠应为白色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 生命值行 = 查找包含行(mm消息, "生命值");
            assertTrue(生命值行.contains("<white>: 80.0/100.0"),
                    "生命值斜杠应在白色跨度内，实际：" + 生命值行);
            assertFalse(生命值行.contains("<dark_gray>/"),
                    "生命值斜杠不应为深灰色，实际：" + 生命值行);
        }
    }

    @Nested
    @DisplayName("战斗属性全流程：公共冷却/暴击/吸血行")
    class 战斗属性全流程 {

        @Test
        @DisplayName("公共冷却时间行：捕获实际MiniMessage输出应包含[公共冷却时间]颜色标签（dark_gray）")
        void 公共冷却行_应包含公共冷却颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 公共冷却时间翻译值是"[公共冷却时间]"，整体着色为 <dark_gray>公共冷却时间</dark_gray>
            assertTrue(全部消息.contains("<dark_gray>公共冷却时间</dark_gray>"),
                    "[公共冷却时间]应着色为<dark_gray>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("公共冷却时间行：时间数值应使用白色显示（FP-SX验收标准）")
        void 公共冷却行_时间数值应使用白色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 公共冷却行 = 查找包含行(mm消息, "公共冷却时间");
            // FP-SX：公共冷却时间的数值"1.0秒"用白色显示，不随"公共冷却时间"关键词颜色
            // 公共冷却行模板: {0}<white>: <white>{1}{2} <aqua>{3}
            // 属性快照公共冷却时间=1.5、急速=10.0，实际GCD=1.5/(1+10*0.01)=1.36→1.4秒
            assertTrue(公共冷却行.contains("<white>: 1.4秒"),
                    "公共冷却时间数值应使用<white>并显示实际GCD（FP-REAL），实际：" + 公共冷却行);
            assertFalse(公共冷却行.contains("<dark_gray>1.4秒"),
                    "公共冷却时间数值不应随关键词使用dark_gray，实际：" + 公共冷却行);
        }

        @Test
        @DisplayName("公共冷却时间行：显示数值应为属性计算服务输出的实际GCD（非原始配置值）")
        void 公共冷却行_应显示实际GCD而非原始配置值() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 纯文本 = 捕获纯文本消息(玩家);
            String 公共冷却行 = 查找包含行(纯文本, "公共冷却时间");
            assertTrue(公共冷却行.contains("1.4秒"),
                    "应显示计算后的实际GCD 1.4秒，实际：" + 公共冷却行);
            assertFalse(公共冷却行.startsWith("<white>公共冷却时间</white><white>: 1.5秒"),
                    "显示值不应为原始配置GCD 1.5秒，实际：" + 公共冷却行);
        }

        @Test
        @DisplayName("暴击行：应使用新版'法术暴击几率/法术暴击伤害'关键词（非旧版'暴击几率/暴击伤害'）")
        void 暴击行_应使用新版法术暴击关键词() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 纯文本 = 捕获纯文本消息(玩家);
            String 全部纯文本 = String.join("\n", 纯文本);
            // 新版关键词：法术暴击几率/法术暴击伤害（非旧版暴击几率/暴击伤害，也非简称法术暴击率）
            assertTrue(全部纯文本.contains("法术暴击几率"),
                    "应包含新版'法术暴击几率'，实际：" + 全部纯文本);
            assertTrue(全部纯文本.contains("法术暴击伤害"),
                    "应包含新版'法术暴击伤害'，实际：" + 全部纯文本);
            assertFalse(全部纯文本.contains("法术暴击率"),
                    "不应包含旧版简称'法术暴击率'，实际：" + 全部纯文本);
        }

        @Test
        @DisplayName("暴击行：法术暴击几率前缀应使用<yellow>颜色（规范第十五章）")
        void 暴击行_法术暴击几率前缀应使用黄色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 规范第十五章：法术暴击几率关键词用 <yellow>
            // 翻译文件 属性指令.属性名.法术暴击几率="[法术暴击几率]" 着色为 <yellow>法术暴击几率</yellow>
            assertTrue(全部消息.contains("<yellow>法术暴击几率</yellow>"),
                    "'法术暴击几率'应着色为<yellow>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("暴击行：法术暴击伤害前缀应使用<gold>颜色（规范第十五章）")
        void 暴击行_法术暴击伤害前缀应使用金色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 规范第十五章：法术暴击伤害关键词用 <gold>
            // 翻译文件 属性指令.属性名.法术暴击伤害="[法术暴击伤害]" 着色为 <gold>法术暴击伤害</gold>
            assertTrue(全部消息.contains("<gold>法术暴击伤害</gold>"),
                    "'法术暴击伤害'应着色为<gold>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("吸血行：捕获实际MiniMessage输出应包含[吸血]颜色标签（dark_red）")
        void 吸血行_应包含吸血颜色标签() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 吸血翻译值是"[吸血]"，着色为 <dark_red>吸血</dark_red>
            assertTrue(全部消息.contains("<dark_red>吸血</dark_red>"),
                    "[吸血]应着色为<dark_red>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("全能行：捕获实际输出应包含百分比格式（%）")
        void 全能行_应包含百分比格式() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 消息列表 = 捕获纯文本消息(玩家);
            String 全部消息 = String.join("\n", 消息列表);
            assertTrue(全部消息.contains("8.0%"), "全能应显示8.0%，实际：" + 全部消息);
        }

        @Test
        @DisplayName("百分号：应跟随前面数字使用白色（FP-SX验收标准）")
        void 百分号_应为白色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全能行 = 查找包含行(mm消息, "全能");
            // 模板中 % 使用 <white>%，序列化后数值与%处于同一白色跨度内
            assertTrue(全能行.contains("<white>: 8.0% "),
                    "百分号应跟随数字为白色，实际：" + 全能行);
            assertFalse(全能行.contains("<red>%"),
                    "百分号不应使用红色，实际：" + 全能行);
        }

        @Test
        @DisplayName("暴击：应显示为两条独立属性行（FP-SX验收标准）")
        void 暴击_应为两条独立属性() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 纯文本 = 捕获纯文本消息(玩家);
            long 法术暴击行数 = 纯文本.stream()
                    .filter(行 -> 行.contains("法术暴击"))
                    .count();
            assertTrue(法术暴击行数 >= 2,
                    "应至少有两行包含'法术暴击'（几率+伤害），实际行数：" + 法术暴击行数);
            assertFalse(纯文本.stream().anyMatch(行 -> 行.contains("暴击") && !行.contains("法术暴击")),
                    "不应再包含旧版'暴击'属性行");
        }
    }

    @Nested
    @DisplayName("构成文本全流程：基础/装备/天赋/临时")
    class 构成文本全流程 {

        @Test
        @DisplayName("属性行：捕获实际输出应包含基础/装备/天赋/临时构成文本")
        void 属性行_应包含所有构成文本() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 消息列表 = 捕获纯文本消息(玩家);
            String 全部消息 = String.join("\n", 消息列表);
            assertTrue(全部消息.contains("基础"), "应包含'基础'构成，实际：" + 全部消息);
            assertTrue(全部消息.contains("装备"), "应包含'装备'构成，实际：" + 全部消息);
            assertTrue(全部消息.contains("天赋"), "应包含'天赋'构成，实际：" + 全部消息);
            assertTrue(全部消息.contains("临时"), "应包含'临时'构成，实际：" + 全部消息);
        }

        @Test
        @DisplayName("属性行：捕获实际输出应包含分隔符和括号")
        void 属性行_应包含分隔符和括号() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> 消息列表 = 捕获纯文本消息(玩家);
            String 全部消息 = String.join("\n", 消息列表);
            assertTrue(全部消息.contains("（"), "应包含左括号'（'，实际：" + 全部消息);
            assertTrue(全部消息.contains("）"), "应包含右括号'）'，实际：" + 全部消息);
        }

        @Test
        @DisplayName("属性行：构成文本（基础...）应使用淡蓝色（aqua）")
        void 属性行_构成文本应使用淡蓝色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 全部消息 = String.join("\n", mm消息);
            // 构成文本"（基础：...）"应使用 <aqua> 颜色（规范：括号注释用淡蓝色）
            assertTrue(全部消息.contains("<aqua>（基础"),
                    "构成文本应使用<aqua>，实际：" + 全部消息);
        }

        @Test
        @DisplayName("属性行：数值应使用白色，不应使用淡蓝色（aqua）")
        void 属性行_数值应使用白色非淡蓝色() {
            处理器.onCommand(玩家, 命令, "sx", new String[]{});

            List<String> mm消息 = 捕获MiniMessage消息(玩家);
            String 力量行 = 查找包含行(mm消息, "力量");
            // 数值50.0不应出现在<aqua>标签紧后（旧颜色是aqua，新颜色是white）
            // 构成文本中的50.0在<aqua>（基础：50.0 内是正常的，不算
            assertFalse(力量行.contains("<aqua>50.0"),
                    "数值50.0不应使用<aqua>颜色，实际：" + 力量行);
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
