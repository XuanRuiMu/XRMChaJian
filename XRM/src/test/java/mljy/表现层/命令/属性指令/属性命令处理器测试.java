package mljy.表现层.命令.属性指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.属性计算服务;
import mljy.属性服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家会话;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("属性命令处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 属性命令处理器测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性服务 属性服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private 修饰器管理器 修饰器管理器;
    @Mock
    private Player 玩家;
    @Mock
    private Player 目标玩家;
    @Mock
    private ConsoleCommandSender 控制台;
    @Mock
    private Command 命令;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 属性命令处理器 处理器;
    private YamlConfiguration 翻译文件;

    private UUID 玩家标识;
    private UUID 目标标识;
    private 玩家会话 会话;
    private 属性快照 属性;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/命令/属性指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        处理器 = new 属性命令处理器(翻译服务, 关键词解析器, 玩家服务, 属性服务, 属性计算服务, 修饰器管理器);
        when(属性计算服务.计算实际公共冷却(anyDouble(), anyDouble())).thenAnswer(调用 -> (double) 调用.getArgument(0));
        when(修饰器管理器.查询修饰器(any(), anyString())).thenReturn(java.util.List.of());

        玩家标识 = UUID.randomUUID();
        目标标识 = UUID.randomUUID();
        会话 = new 玩家会话(玩家标识, "测试玩家");
        属性 = 创建属性快照();

        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("测试玩家");
        when(玩家.getHealth()).thenReturn(20.0);
        when(目标玩家.getUniqueId()).thenReturn(目标标识);
        when(目标玩家.getName()).thenReturn("目标玩家");
        when(目标玩家.getHealth()).thenReturn(15.0);
        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
        when(属性服务.计算属性(玩家标识)).thenReturn(属性);
        属性快照 基础属性 = 创建基础属性快照();
        when(属性服务.获取基础属性(玩家标识)).thenReturn(基础属性);
        when(属性服务.获取基础属性(目标标识)).thenReturn(基础属性);
    }

    private 属性快照 创建属性快照() {
        return 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                20.0, 100.0,
                15.0, 5.0, 8.0, 12.0,
                2.0, 1.0
        );
    }

    private 属性快照 创建基础属性快照() {
        return 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                20.0, 100.0,
                15.0, 5.0, 8.0, 12.0,
                2.0, 0.5
        );
    }

    private void 加载翻译文件(String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                翻译文件.set(键, 配置.get(键));
            }
        }
    }

    private List<String> 捕获所有消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
    }

    @Nested
    @DisplayName("正常流程 - 查看自己属性")
    class 查看自己属性 {

        @Test
        @DisplayName("玩家执行/属性 - 应显示完整属性界面")
        void 玩家执行属性命令_应显示完整属性界面() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "属性", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(玩家);
            // 应包含分隔线、标题行、各属性分类标题和属性行
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("主属性")),
                    "应包含'主属性'分类，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("生存属性")),
                    "应包含'生存属性'分类，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("战斗属性")),
                    "应包含'战斗属性'分类，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("属性界面 - 应包含力量/敏捷/智力主属性")
        void 属性界面_应包含主属性() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("力量")),
                    "应包含'力量'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("敏捷")),
                    "应包含'敏捷'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("智力")),
                    "应包含'智力'属性，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("属性界面 - 应包含生命值/生命恢复/躲闪几率")
        void 属性界面_应包含生存属性() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("生命值")),
                    "应包含'生命值'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("生命恢复")),
                    "应包含'生命恢复'属性，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("属性界面 - 应包含急速/暴击/精通/全能/吸血/移速")
        void 属性界面_应包含战斗属性() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("急速")),
                    "应包含'急速'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("暴击")),
                    "应包含'暴击'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("精通")),
                    "应包含'精通'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("全能")),
                    "应包含'全能'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("吸血")),
                    "应包含'吸血'属性，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("移速")),
                    "应包含'移速'属性，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("属性界面 - 应包含等级和专精信息")
        void 属性界面_应包含等级和专精() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("级")),
                    "应包含等级后缀'级'，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("奥能法师")),
                    "应包含专精'奥能法师'，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("的属性")),
                    "应包含'的属性'，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("属性界面 - 应包含属性构成分解")
        void 属性界面_应包含属性构成() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("基础")),
                    "应包含'基础'构成，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("装备")),
                    "应包含'装备'构成，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("天赋")),
                    "应包含'天赋'构成，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("临时")),
                    "应包含'临时'构成，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("正常流程 - 查看他人属性")
    class 查看他人属性 {

        @Test
        @DisplayName("玩家执行/属性 <玩家名> - 应显示目标玩家属性")
        void 玩家执行属性命令带参数_应显示目标玩家属性() {
            when(玩家服务.获取会话(目标标识)).thenReturn(Optional.of(new 玩家会话(目标标识, "目标玩家")));
            when(属性服务.计算属性(目标标识)).thenReturn(属性);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(玩家, 命令, "属性", new String[]{"目标玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(玩家);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("目标玩家")),
                        "应包含目标玩家名，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("异常输入 - 玩家不在线")
    class 玩家不在线 {

        @Test
        @DisplayName("指定不在线玩家 - 应发送玩家不在线错误")
        void 指定不在线玩家_应发送玩家不在线错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("不在线玩家")).thenReturn(null);

                boolean 结果 = 处理器.onCommand(玩家, 命令, "属性", new String[]{"不在线玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(玩家);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("不在线")),
                        "应包含玩家不在线错误，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("异常输入 - 会话不存在")
    class 会话不存在 {

        @Test
        @DisplayName("玩家会话不存在 - 应发送未知错误")
        void 玩家会话不存在_应发送未知错误() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "属性", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]")),
                    "应包含错误前缀，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("异常输入 - 非玩家发送者")
    class 非玩家发送者 {

        @Test
        @DisplayName("控制台执行/属性 - 应发送仅玩家可用错误")
        void 控制台执行属性命令_应发送仅玩家可用错误() {
            boolean 结果 = 处理器.onCommand(控制台, 命令, "属性", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(控制台);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("仅玩家可用")),
                    "应包含仅玩家可用错误，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("玩家可见输出 - 验证最终中文文本")
    class 玩家可见输出 {

        @Test
        @DisplayName("属性数值 - 应显示具体数值")
        void 属性数值_应显示具体数值() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            // 力量=50.0，应显示在某个消息中
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("50.0")),
                    "应包含力量数值'50.0'，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("公共冷却时间 - 应通过属性计算服务计算实际GCD")
        void 公共冷却时间_应使用计算服务() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            verify(属性计算服务).计算实际公共冷却(1.5, 10.0);
        }

        @Test
        @DisplayName("属性界面 - 不应包含翻译键")
        void 属性界面_不应包含翻译键() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            for (String 消息 : 消息列表) {
                assertFalse(消息.contains("属性指令."),
                        "不应包含翻译键前缀'属性指令.'，实际消息: " + 消息);
            }
        }

        @Test
        @DisplayName("移速行 - 基础值应使用基础属性而非最终值")
        void 移速行_基础值应使用基础属性() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            // 最终移速=1.0，基础移速=0.5；移速行应同时包含最终值与基础值
            String 移速行 = 消息列表.stream()
                    .filter(m -> m.contains("移速"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(移速行, "应包含移速行，实际消息: " + 消息列表);
            assertTrue(移速行.contains("1.0"), "移速行应包含最终值'1.0'，实际: " + 移速行);
            assertTrue(移速行.contains("0.5"), "移速行应包含基础值'0.5'，实际: " + 移速行);
        }

        @Test
        @DisplayName("FP-03: 移速行 - 不应输出移速的绝对值提示文案")
        void 移速行_不应输出移速的绝对值提示() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertFalse(消息列表.stream().anyMatch(m -> m.contains("绝对值")),
                    "不应输出移速的绝对值提示文案'绝对值'，实际消息: " + 消息列表);
            assertFalse(消息列表.stream().anyMatch(m -> m.contains("0.2=+0.2")),
                    "不应输出移速的绝对值提示输入示例'0.2=+0.2'，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("公共冷却构成 - 值为0.0时不显示符号")
        void 公共冷却构成_值为0时不显示符号() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            // 构成分解中急速减少=0.0和其他修正=0.0都不应有+0.0或-0.0
            for (String 消息 : 消息列表) {
                assertFalse(消息.contains("+0.0"), "不应包含'+0.0'，实际消息: " + 消息);
                assertFalse(消息.contains("-0.0"), "不应包含'-0.0'，实际消息: " + 消息);
            }
        }

        @Test
        @DisplayName("获取命令名 - 应返回'属性'")
        void 获取命令名_应返回属性() {
            assertEquals("属性", 处理器.获取命令名());
        }
    }

    @Nested
    @DisplayName("临时修饰器加成显示")
    class 临时修饰器加成显示 {

        @Test
        @DisplayName("无修饰器时 - 临时构成应显示0.0")
        void 无修饰器_临时显示零() {
            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 智力行 = 消息列表.stream()
                    .filter(m -> m.contains("智力"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(智力行, "应包含智力行，实际消息: " + 消息列表);
            assertTrue(智力行.contains("临时"), "智力行应包含'临时'构成，实际: " + 智力行);
        }

        @Test
        @DisplayName("有智力修饰器时 - 临时构成应显示实际加成值")
        void 有智力修饰器_临时显示实际值() {
            修饰器 智力修饰器 = new 修饰器("智力", "奥能冥想", 修饰方式.相加, 40.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "智力"))
                    .thenReturn(java.util.List.of(智力修饰器));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 智力行 = 消息列表.stream()
                    .filter(m -> m.contains("智力"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(智力行, "应包含智力行，实际消息: " + 消息列表);
            assertTrue(智力行.contains("40.0"), "智力行临时构成应包含加成值'40.0'，实际: " + 智力行);
        }

        @Test
        @DisplayName("有多个智力修饰器时 - 临时构成应显示累加值")
        void 有多个智力修饰器_临时显示累加值() {
            修饰器 修饰器1 = new 修饰器("智力", "奥能冥想", 修饰方式.相加, 40.0, 100);
            修饰器 修饰器2 = new 修饰器("智力", "天赋_智力", 修饰方式.相加, 15.0, 50);
            when(修饰器管理器.查询修饰器(玩家标识, "智力"))
                    .thenReturn(java.util.List.of(修饰器1, 修饰器2));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 智力行 = 消息列表.stream()
                    .filter(m -> m.contains("智力"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(智力行, "应包含智力行，实际消息: " + 消息列表);
            assertTrue(智力行.contains("55.0"), "智力行临时构成应包含累加值'55.0'，实际: " + 智力行);
        }

        @Test
        @DisplayName("智力修饰器不影响其他属性 - 力量临时仍为0.0")
        void 智力修饰器不影响力量() {
            修饰器 智力修饰器 = new 修饰器("智力", "奥能冥想", 修饰方式.相加, 40.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "智力"))
                    .thenReturn(java.util.List.of(智力修饰器));
            when(修饰器管理器.查询修饰器(玩家标识, "力量"))
                    .thenReturn(java.util.List.of());

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            verify(修饰器管理器).查询修饰器(玩家标识, "智力");
            verify(修饰器管理器).查询修饰器(玩家标识, "力量");
        }
    }

    @Nested
    @DisplayName("Tab补全")
    class Tab补全 {

        @Test
        @DisplayName("Tab补全 - 应返回空列表")
        void Tab补全_应返回空列表() {
            var 结果 = 处理器.onTabComplete(玩家, 命令, "属性", new String[]{});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }
    }

    @Nested
    @DisplayName("FP-04: 属性双重计算修复 - 基础显示未修饰值、临时单独显示")
    class FP04属性双重计算修复 {

        private 属性快照 创建临时加成属性快照(double 加成) {
            return 属性快照.创建(
                    100.0 + 加成, 1.5, 10.0 + 加成,
                    50.0, 40.0, 30.0,
                    50.0, 40.0, 30.0,
                    20.0 + 加成, 100.0 + 加成,
                    15.0 + 加成, 5.0 + 加成, 8.0 + 加成, 12.0 + 加成,
                    2.0 + 加成, 1.0
            );
        }

        @Test
        @DisplayName("精通 - 临时修饰器下：数值=基础+临时，基础栏不含临时")
        void 精通_基础与临时分离() {
            属性快照 修改后 = 创建临时加成属性快照(100.0);
            when(属性服务.计算属性(玩家标识)).thenReturn(修改后);
            修饰器 精通修饰器 = new 修饰器("精通", "奥能冥想", 修饰方式.相加, 100.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "精通")).thenReturn(java.util.List.of(精通修饰器));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 行 = 消息列表.stream().filter(m -> m.contains("精通")).findFirst().orElse(null);
            assertNotNull(行, "应包含精通行，实际: " + 消息列表);
            assertTrue(行.contains("115.0"), "精通数值应为基础15+临时100=115.0，实际: " + 行);
            assertTrue(行.contains("基础：15.0"), "精通基础栏应显示未修饰基础15.0，实际: " + 行);
            assertTrue(行.contains("临时：100.0"), "精通临时栏应显示100.0，实际: " + 行);
            assertFalse(行.contains("基础：115.0"), "基础栏不得显示含临时的总值115.0，实际: " + 行);
        }

        @Test
        @DisplayName("生命值上限 - 临时修饰器下：最大值=基础+临时，基础栏不含临时")
        void 生命值上限_基础与临时分离() {
            属性快照 修改后 = 创建临时加成属性快照(100.0);
            when(属性服务.计算属性(玩家标识)).thenReturn(修改后);
            修饰器 修饰器1 = new 修饰器("生命值上限", "生命祝福", 修饰方式.相加, 100.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "生命值上限")).thenReturn(java.util.List.of(修饰器1));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 行 = 消息列表.stream().filter(m -> m.contains("生命值")).findFirst().orElse(null);
            assertNotNull(行, "应包含生命值行，实际: " + 消息列表);
            assertTrue(行.contains("200.0"), "生命值最大值应为基础100+临时100=200.0，实际: " + 行);
            assertTrue(行.contains("基础：100.0"), "生命值基础栏应显示未修饰基础100.0，实际: " + 行);
            assertTrue(行.contains("临时：100.0"), "生命值临时栏应显示100.0，实际: " + 行);
            assertFalse(行.contains("基础：200.0"), "基础栏不得显示含临时的总值200.0，实际: " + 行);
        }

        @Test
        @DisplayName("躲闪 - 临时修饰器下：数值=基础+临时，基础栏不含临时")
        void 躲闪_基础与临时分离() {
            属性快照 修改后 = 创建临时加成属性快照(100.0);
            when(属性服务.计算属性(玩家标识)).thenReturn(修改后);
            修饰器 修饰器1 = new 修饰器("躲闪", "幻影步", 修饰方式.相加, 100.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "躲闪")).thenReturn(java.util.List.of(修饰器1));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 行 = 消息列表.stream().filter(m -> m.contains("躲闪")).findFirst().orElse(null);
            assertNotNull(行, "应包含躲闪行，实际: " + 消息列表);
            assertTrue(行.contains("112.0"), "躲闪数值应为基础12+临时100=112.0，实际: " + 行);
            assertTrue(行.contains("基础：12.0"), "躲闪基础栏应显示未修饰基础12.0，实际: " + 行);
            assertTrue(行.contains("临时：100.0"), "躲闪临时栏应显示100.0，实际: " + 行);
            assertFalse(行.contains("基础：112.0"), "基础栏不得显示含临时的总值112.0，实际: " + 行);
        }

        @Test
        @DisplayName("公共冷却 - 临时修饰器下：基础栏=未修饰、其他栏=临时、数学自洽")
        void 公共冷却_临时分离且自洽() {
            属性快照 修改后 = 属性快照.创建(
                    100.0, 2.5, 10.0,
                    50.0, 40.0, 30.0,
                    50.0, 40.0, 30.0,
                    20.0, 100.0,
                    15.0, 5.0, 8.0, 12.0,
                    2.0, 1.0
            );
            when(属性服务.计算属性(玩家标识)).thenReturn(修改后);
            修饰器 公CD修饰器 = new 修饰器("公共冷却时间", "公CD修正", 修饰方式.相加, 1.0, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "公共冷却时间")).thenReturn(java.util.List.of(公CD修饰器));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 行 = 消息列表.stream().filter(m -> m.contains("公共冷却时间")).findFirst().orElse(null);
            assertNotNull(行, "应包含公共冷却行，实际: " + 消息列表);
            assertTrue(行.contains("2.5"), "实际公共冷却应显示2.5，实际: " + 行);
            assertTrue(行.contains("基础：1.5"), "基础栏应显示未修饰基础1.5，实际: " + 行);
            assertTrue(行.contains("其他：+1.0"), "其他(临时)栏应显示+1.0，实际: " + 行);
            assertFalse(行.contains("基础：2.5"), "基础栏不得显示含临时的总值2.5，实际: " + 行);
        }

        @Test
        @DisplayName("移速 - 临时修饰器下：临时栏应显示临时值（确认无 bug）")
        void 移速_临时显示() {
            修饰器 移速修饰器 = new 修饰器("移速", "疾风术", 修饰方式.相加, 0.3, 100);
            when(修饰器管理器.查询修饰器(玩家标识, "移速")).thenReturn(java.util.List.of(移速修饰器));

            处理器.onCommand(玩家, 命令, "属性", new String[]{});

            List<String> 消息列表 = 捕获所有消息(玩家);
            String 行 = 消息列表.stream().filter(m -> m.contains("移速")).findFirst().orElse(null);
            assertNotNull(行, "应包含移速行，实际: " + 消息列表);
            assertTrue(行.contains("基础：0.5"), "移速基础栏应显示基础0.5，实际: " + 行);
            assertTrue(行.contains("临时：0.3"), "移速临时栏应显示0.3，实际: " + 行);
            assertTrue(行.contains("1.0"), "移速数值应显示最终值1.0，实际: " + 行);
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
