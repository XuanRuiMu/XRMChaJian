package mljy.表现层.命令.管理员指令;

import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.仇恨服务;
import mljy.业务层.技能冷却服务;
import mljy.业务层.公共冷却显示服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.战斗状态服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.数据服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.基础设施层.Yaml配置加载器;
import mljy.属性服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.技能.参数读取器;
import mljy.领域层.技能.参数注册表;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.效果.效果定义;
import mljy.技能实现.奥能法师.奥能法师技能基础;
import mljy.技能实现.奥能法师.奥术冲击;
import mljy.技能实现.奥能法师.奥术护盾;
import mljy.领域层.技能.技能定义;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("管理命令处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 管理命令处理器测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 数据服务 数据服务;
    @Mock
    private 属性服务 属性服务;
    @Mock
    private 修饰器管理器 修饰器管理器;
    @Mock
    private 仇恨服务 仇恨服务;
    @Mock
    private 战斗状态服务 战斗状态服务;
    @Mock
    private 技能注册服务 技能注册服务;
    @Mock
    private 效果注册服务 效果注册服务;
    @Mock
    private 技能冷却服务 技能冷却服务;
    @Mock
    private 公共冷却显示服务 公共冷却显示服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 技能释放服务 技能释放服务;
    @Mock
    private 参数注册表 参数注册表;
    @Mock
    private JavaPlugin 插件;
    @Mock
    private Yaml配置加载器 配置加载器;
    @Mock
    private Player 管理员;
    @Mock
    private Player 普通玩家;
    @Mock
    private Player 目标玩家;
    @Mock
    private ConsoleCommandSender 控制台;
    @Mock
    private Command 命令;
    @Mock
    private Logger 日志器;
    @Mock
    private BukkitScheduler 调度器;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 管理命令处理器 处理器;
    private YamlConfiguration 翻译文件;

    private UUID 管理员标识;
    private UUID 目标标识;
    private 玩家会话 目标会话;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/命令/管理员指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));

        when(插件.getLogger()).thenReturn(日志器);
        处理器 = new 管理命令处理器(翻译服务, 关键词解析器,
                玩家服务, 数据服务, 属性服务,
                修饰器管理器, 仇恨服务, 战斗状态服务,
                插件, 配置加载器, new 属性注册表(),
                技能注册服务, 效果注册服务,
                技能冷却服务, 公共冷却显示服务, 效果调度服务,
                技能释放服务, 参数注册表);

        管理员标识 = UUID.randomUUID();
        目标标识 = UUID.randomUUID();
        目标会话 = new 玩家会话(目标标识, "目标玩家");

        when(管理员.getUniqueId()).thenReturn(管理员标识);
        when(管理员.getName()).thenReturn("管理员");
        when(管理员.hasPermission("xrm.op")).thenReturn(true);
        when(普通玩家.getUniqueId()).thenReturn(UUID.randomUUID());
        when(普通玩家.getName()).thenReturn("普通玩家");
        when(普通玩家.hasPermission("xrm.op")).thenReturn(false);
        when(目标玩家.getUniqueId()).thenReturn(目标标识);
        when(目标玩家.getName()).thenReturn("目标玩家");
        when(玩家服务.获取会话(目标标识)).thenReturn(Optional.of(目标会话));
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

    private List<String> 捕获MiniMessage消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        MiniMessage mm = MiniMessage.miniMessage();
        return 捕获器.getAllValues().stream()
                .map(mm::serialize)
                .toList();
    }

    @Nested
    @DisplayName("权限检查")
    class 权限检查 {

        @Test
        @DisplayName("无权限玩家执行/管理 - 应发送无权限错误")
        void 无权限玩家执行管理命令_应发送无权限错误() {
            boolean 结果 = 处理器.onCommand(普通玩家, 命令, "管理", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(普通玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("权限")),
                    "应包含无权限错误，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("有权限管理员执行/管理 - 不应发送无权限错误")
        void 有权限管理员执行管理命令_不应发送无权限错误() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertFalse(消息列表.stream().anyMatch(m -> m.contains("没有权限")),
                    "不应包含无权限错误，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("参数验证 - 无参数显示帮助")
    class 无参数显示帮助 {

        @Test
        @DisplayName("执行/管理无参数 - 应显示帮助信息")
        void 执行管理无参数_应显示帮助信息() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.size() > 5, "应显示多条帮助信息，实际数量: " + 消息列表.size());
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("管理员命令")),
                    "应包含帮助标题，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("帮助信息 - 应包含所有子命令说明")
        void 帮助信息_应包含所有子命令说明() {
            处理器.onCommand(管理员, 命令, "管理", new String[]{});

            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("仇恨")),
                    "应包含'仇恨'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("战斗")),
                    "应包含'战斗'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("等级")),
                    "应包含'等级'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("职业")),
                    "应包含'职业'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("属性")),
                    "应包含'属性'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("重载")),
                    "应包含'重载'子命令说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("刷新")),
                    "应包含'刷新'子命令说明，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("正常流程 - 重载子命令")
    class 重载子命令 {

        @Test
        @DisplayName("执行/管理 重载 - 应重载配置并发送成功消息")
        void 执行管理重载_应重载配置并发送成功消息() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{"重载"});

            assertTrue(结果);
            verify(插件).reloadConfig();
            verify(日志器).info("插件配置已由管理员重载");
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("配置") && m.contains("重新加载")),
                    "应包含重载成功消息，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("正常流程 - 等级子命令")
    class 等级子命令 {

        @Test
        @DisplayName("执行/管理 等级 <玩家> - 应显示玩家当前等级")
        void 执行管理等级查看_应显示玩家当前等级() {
            目标会话.设置等级(15);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{"等级", "目标玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("15")),
                        "应包含等级'15'，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("执行/管理 等级 <玩家> <等级> - 应设置玩家等级")
        void 执行管理等级设置_应设置玩家等级() {
            目标会话.设置等级(10);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"等级", "目标玩家", "20"});

                assertTrue(结果);
                assertEquals(20, 目标会话.获取等级());
                verify(数据服务).保存(目标会话);
                verify(属性服务).刷新属性(目标标识);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("20")),
                        "应包含新等级'20'，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("异常输入 - 等级子命令")
    class 等级子命令异常 {

        @Test
        @DisplayName("等级参数不足 - 应发送用法提示")
        void 等级参数不足_应发送用法提示() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{"等级"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("用法")),
                    "应包含用法错误，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("等级非数字 - 应发送错误")
        void 等级非数字_应发送错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"等级", "目标玩家", "abc"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("数字")),
                        "应包含数字错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("等级为零 - 应发送必须大于0错误")
        void 等级为零_应发送必须大于0错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"等级", "目标玩家", "0"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("大于0")),
                        "应包含必须大于0错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("目标玩家不在线 - 应发送不在线错误")
        void 目标玩家不在线_应发送不在线错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("不在线玩家")).thenReturn(null);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"等级", "不在线玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("不在线")),
                        "应包含不在线错误，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("正常流程 - 刷新子命令")
    class 刷新子命令 {

        @Test
        @DisplayName("执行/管理 刷新 <玩家> - 应刷新玩家属性")
        void 执行管理刷新_应刷新玩家属性() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"刷新", "目标玩家"});

                assertTrue(结果);
                verify(属性服务).刷新属性(目标标识);
                verify(技能冷却服务).重置所有冷却(目标标识);
                verify(技能冷却服务).重置公共冷却(目标标识);
                verify(公共冷却显示服务).清理(目标标识);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("刷新") && m.contains("目标玩家")),
                        "应包含刷新成功消息，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("FP-02 刷新子命令重置冷却")
    class 刷新子命令重置冷却FP02 {

        @Test
        @DisplayName("刷新应重置独立CD与公CD并清理GCD显示")
        void 刷新_应重置独立CD与公CD并清理GCD显示() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"刷新", "目标玩家"});

                assertTrue(结果);
                verify(技能冷却服务).重置所有冷却(目标标识);
                verify(技能冷却服务).重置公共冷却(目标标识);
                verify(公共冷却显示服务).清理(目标标识);
                verify(技能冷却服务, never()).是否冷却中(any(), any());
            }
        }

        @Test
        @DisplayName("刷新参数不足 - 不应调用任何冷却重置")
        void 刷新参数不足_不应调用冷却重置() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"刷新"});

            assertTrue(结果);
            verify(技能冷却服务, never()).重置所有冷却(any());
            verify(技能冷却服务, never()).重置公共冷却(any());
            verify(公共冷却显示服务, never()).清理(any());
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("用法")),
                    "应包含用法错误，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("FP-1 属性子命令（详情三参数+公CD修正改名）")
    class 属性子命令FP1 {
        @Mock
        private Server mock服务器;
        @Mock
        private BukkitScheduler mock调度器;
        @Mock
        private BukkitTask mock任务;

        @BeforeEach
        void setUpScheduler() {
            when(插件.getServer()).thenReturn(mock服务器);
            when(mock服务器.getScheduler()).thenReturn(mock调度器);
            when(mock调度器.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mock任务);
        }

        @Test
        @DisplayName("FP-1修改1: /管理 属性 <玩家> 精通 100 10000 第三行详情格式正确")
        void 属性添加临时_详情格式应为3参数() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "10000"});

                assertTrue(结果);
                verify(修饰器管理器).注册修饰器(eq(目标标识), any());
                verify(属性服务).刷新属性(目标标识);
                List<String> 消息列表 = 捕获所有消息(管理员);
                String 详情消息 = 消息列表.stream()
                        .filter(m -> m.contains("实际获得了"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(详情消息, "应包含'实际获得了'详情消息，实际消息: " + 消息列表);
                assertTrue(详情消息.contains("精通"), "详情应包含属性名'精通'，实际: " + 详情消息);
                assertTrue(详情消息.contains("100点"), "详情应包含数值'100点'，实际: " + 详情消息);
                assertTrue(详情消息.contains("10000秒"), "详情应包含秒数'10000秒'，实际: " + 详情消息);
                assertEquals("实际获得了：精通，100点，持续：10000秒", 详情消息,
                        "详情消息完整文本应严格匹配需求格式");
            }
        }

        @Test
        @DisplayName("FP-1修改2: /管理 属性 输入'公CD修正'应正确识别为公共冷却时间属性")
        void 属性添加临时_公CD修正应被识别() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "公CD修正", "100", "10000"});

                assertTrue(结果);
                ArgumentCaptor<修饰器> 修饰器捕获 = ArgumentCaptor.forClass(修饰器.class);
                verify(修饰器管理器).注册修饰器(eq(目标标识), 修饰器捕获.capture());
                assertEquals("公共冷却时间", 修饰器捕获.getValue().类别(),
                        "应注册到'公共冷却时间'修饰器类别");
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertFalse(消息列表.stream().anyMatch(m -> m.contains("无效的属性名") && m.contains("公CD修正")),
                        "不应触发无效属性名错误，实际消息: " + 消息列表);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("实际获得了") && m.contains("公CD修正")),
                        "应发送添加成功详情，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("FP-1修改2: /管理 属性 输入旧名'公CD减少'应触发无效属性名错误（已改名）")
        void 属性添加临时_公CD减少应失效() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "公CD减少", "100", "10000"});

                assertTrue(结果);
                verify(修饰器管理器, never()).注册修饰器(any(), any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("无效的属性名") && m.contains("公CD减少")),
                        "应触发无效属性名错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("FP-03: /管理 属性 <玩家> 移速 0.2 30 添加成功不应输出移速的绝对值提示文案")
        void 属性子命令_添加移速修饰器成功_不应输出移速的绝对值提示() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "移速", "0.2", "30"});

                assertTrue(结果);
                verify(修饰器管理器).注册修饰器(eq(目标标识), any());
                verify(属性服务).刷新属性(目标标识);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("已为") && m.contains("目标玩家")),
                        "应包含添加成功消息，实际消息: " + 消息列表);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("实际获得了") && m.contains("移速")),
                        "应包含详情消息含移速，实际消息: " + 消息列表);
                assertFalse(消息列表.stream().anyMatch(m -> m.contains("绝对值")),
                        "不应输出移速的绝对值提示文案'绝对值'，实际消息: " + 消息列表);
                assertFalse(消息列表.stream().anyMatch(m -> m.contains("0.2=+0.2")),
                        "不应输出移速的绝对值提示输入示例'0.2=+0.2'，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("FP-03: /管理 属性 <玩家> 移速 0.01 10000 详情应显示0.01点（非0点）")
        void 属性添加临时_移速0_01详情应显示0_01点() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "移速", "0.01", "10000"});

                assertTrue(结果);
                verify(修饰器管理器).注册修饰器(eq(目标标识), any());
                verify(属性服务).刷新属性(目标标识);
                List<String> 消息列表 = 捕获所有消息(管理员);
                String 详情消息 = 消息列表.stream()
                        .filter(m -> m.contains("实际获得了"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(详情消息, "应包含'实际获得了'详情消息，实际消息: " + 消息列表);
                assertTrue(详情消息.contains("0.01点"), "移速0.01应显示为0.01点，不能为0点，实际: " + 详情消息);
                assertTrue(详情消息.contains("10000秒"), "持续秒数应显示10000秒，实际: " + 详情消息);
            }
        }

        @Test
        @DisplayName("FP-03: /管理 属性 <玩家> 急速 100 30 添加非移速修饰器不应输出移速的绝对值提示")
        void 属性子命令_添加非移速属性修饰器_不应输出移速的绝对值提示() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "急速", "100", "30"});

                assertTrue(结果);
                verify(修饰器管理器).注册修饰器(eq(目标标识), any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("实际获得了") && m.contains("急速")),
                        "应包含急速详情消息，实际消息: " + 消息列表);
                assertFalse(消息列表.stream().anyMatch(m -> m.contains("绝对值")),
                        "不应输出移速的绝对值提示文案，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("FP-11 属性指令反馈关键词颜色格式")
    class 属性子命令FP11 {
        @Mock
        private Server mock服务器;
        @Mock
        private BukkitScheduler mock调度器;
        @Mock
        private BukkitTask mock任务;

        @BeforeEach
        void setUpScheduler() {
            when(插件.getServer()).thenReturn(mock服务器);
            when(mock服务器.getScheduler()).thenReturn(mock调度器);
            when(mock调度器.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mock任务);
        }

        @Test
        @DisplayName("成功-添加成功消息：成功前缀后缀应着色<green>，玩家名应着色<white>")
        void 属性添加临时_添加成功消息_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "10000"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 添加成功消息 = mm消息.stream()
                        .filter(m -> m.contains("已为") && m.contains("添加临时属性效果"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(添加成功消息, "应包含'添加成功'消息，实际：" + mm消息);
                // 模板: [成功:已为 ][玩家名:{0}][成功: 添加临时属性效果]
                // 期望: <green>已为 </green><white>目标玩家</white><green> 添加临时属性效果
                // 注意：MiniMessage序列化器对字符串末尾的彩色文本会省略闭合标签
                assertTrue(添加成功消息.contains("<green>已为 </green>"),
                        "已为前缀应着色为<green>，实际：" + 添加成功消息);
                assertTrue(添加成功消息.contains("<white>目标玩家</white>"),
                        "玩家名应着色为<white>，实际：" + 添加成功消息);
                assertTrue(添加成功消息.contains("<green> 添加临时属性效果"),
                        "添加临时属性效果后缀应着色为<green>，实际：" + 添加成功消息);
            }
        }

        @Test
        @DisplayName("成功-效果ID标签消息：标签前缀应着色<gray>，效果ID值应着色<aqua>")
        void 属性添加临时_效果ID标签消息_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "10000"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 效果ID消息 = mm消息.stream()
                        .filter(m -> m.contains("效果ID"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(效果ID消息, "应包含'效果ID'消息，实际：" + mm消息);
                // 模板: [物理:效果ID：][信息:{0}]
                // 期望: <gray>效果ID：</gray><aqua>精通_xrm_1</aqua>
                assertTrue(效果ID消息.contains("<gray>效果ID：</gray>"),
                        "效果ID前缀应着色为<gray>，实际：" + 效果ID消息);
                assertTrue(效果ID消息.contains("<aqua>"),
                        "效果ID值应着色为<aqua>，实际：" + 效果ID消息);
            }
        }

        @Test
        @DisplayName("成功-详情消息：精通应着色<dark_purple>，点数应着色<white>，秒数应着色<aqua>")
        void 属性添加临时_详情消息_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);
                bukkitMock.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "10000"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 详情消息 = mm消息.stream()
                        .filter(m -> m.contains("实际获得了"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(详情消息, "应包含'实际获得了'详情消息，实际：" + mm消息);
                // 模板: 实际获得了：{0}，[点数:{1}]点，持续：[秒数:{2}]秒
                // {0} = "[精通]"（来自属性名翻译值，静态关键词）
                // 期望: 实际获得了：<dark_purple>精通</dark_purple>，<white>100</white>点，持续：<aqua>10000</aqua>秒
                assertTrue(详情消息.contains("<dark_purple>精通</dark_purple>"),
                        "精通应着色为<dark_purple>，实际：" + 详情消息);
                assertTrue(详情消息.contains("<white>100</white>"),
                        "点数100应着色为<white>，实际：" + 详情消息);
                assertTrue(详情消息.contains("<aqua>10000</aqua>"),
                        "秒数10000应着色为<aqua>，实际：" + 详情消息);
            }
        }

        @Test
        @DisplayName("失败-无效属性名：错误前缀应着色<red>，无效属性名应着色<aqua>(信息)")
        void 属性添加临时_无效属性名_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "无效属性名XXX", "100", "10000"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 错误消息 = mm消息.stream()
                        .filter(m -> m.contains("无效的属性名"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(错误消息, "应包含'无效的属性名'错误消息，实际：" + mm消息);
                // 模板: [错误:无效的属性名: ][信息:{0}]
                // 错误前缀键"<red>[错误]</red>" + " " + 解析后
                // 期望: <red>[错误]</red> <red>无效的属性名: </red><aqua>无效属性名XXX
                // 注意：末尾彩色文本的闭合标签被MiniMessage序列化器省略
                assertTrue(错误消息.contains("<red>[错误]</red>"),
                        "错误前缀[错误]应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<red>无效的属性名: </red>"),
                        "错误文本应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<aqua>无效属性名XXX"),
                        "无效属性名应着色为<aqua>(信息)，实际：" + 错误消息);
            }
        }

        @Test
        @DisplayName("失败-数值非数字：错误前缀应着色<red>，数值应着色<aqua>(信息)")
        void 属性添加临时_数值非数字_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "abc", "10000"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 错误消息 = mm消息.stream()
                        .filter(m -> m.contains("数值必须是数字"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(错误消息, "应包含'数值必须是数字'错误消息，实际：" + mm消息);
                // 模板: [错误:数值必须是数字: ][信息:{0}]
                // 期望: <red>[错误]</red> <red>数值必须是数字: </red><aqua>abc
                assertTrue(错误消息.contains("<red>[错误]</red>"),
                        "错误前缀[错误]应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<red>数值必须是数字: </red>"),
                        "错误文本应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<aqua>abc"),
                        "数值'abc'应着色为<aqua>(信息)，实际：" + 错误消息);
            }
        }

        @Test
        @DisplayName("失败-秒数非数字：错误前缀应着色<red>，秒数应着色<aqua>(信息)")
        void 属性添加临时_秒数非数字_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "abc"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 错误消息 = mm消息.stream()
                        .filter(m -> m.contains("秒数必须是数字"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(错误消息, "应包含'秒数必须是数字'错误消息，实际：" + mm消息);
                // 模板: [错误:秒数必须是数字: ][信息:{0}]
                // 期望: <red>[错误]</red> <red>秒数必须是数字: </red><aqua>abc
                assertTrue(错误消息.contains("<red>[错误]</red>"),
                        "错误前缀[错误]应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<red>秒数必须是数字: </red>"),
                        "错误文本应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<aqua>abc"),
                        "秒数'abc'应着色为<aqua>(信息)，实际：" + 错误消息);
            }
        }

        @Test
        @DisplayName("失败-持续时间必须大于0：错误前缀应着色<red>，错误文本应着色<red>")
        void 属性添加临时_持续时间必须大于0_应包含关键词颜色标签() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"属性", "目标玩家", "精通", "100", "0"});

                assertTrue(结果);
                List<String> mm消息 = 捕获MiniMessage消息(管理员);
                String 错误消息 = mm消息.stream()
                        .filter(m -> m.contains("持续时间必须大于0"))
                        .findFirst()
                        .orElse(null);
                assertNotNull(错误消息, "应包含'持续时间必须大于0'错误消息，实际：" + mm消息);
                // 模板: [错误:持续时间必须大于0]
                // 期望: <red>[错误]</red> <red>持续时间必须大于0
                // 注意：末尾红色文本的闭合标签被MiniMessage序列化器省略
                assertTrue(错误消息.contains("<red>[错误]</red>"),
                        "错误前缀[错误]应着色为<red>，实际：" + 错误消息);
                assertTrue(错误消息.contains("<red>持续时间必须大于0"),
                        "错误文本应着色为<red>，实际：" + 错误消息);
            }
        }
    }

    @Nested
    @DisplayName("异常输入 - 未知子命令")
    class 未知子命令 {

        @Test
        @DisplayName("执行/管理 未知子命令 - 应显示帮助信息")
        void 执行管理未知子命令_应显示帮助信息() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{"未知命令"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("管理员命令")),
                    "应显示帮助标题，实际消息: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("玩家可见输出 - 验证最终中文文本")
    class 玩家可见输出 {

        @Test
        @DisplayName("帮助信息 - 应为中文文本而非翻译键")
        void 帮助信息_应为中文文本() {
            处理器.onCommand(管理员, 命令, "管理", new String[]{});

            List<String> 消息列表 = 捕获所有消息(管理员);
            for (String 消息 : 消息列表) {
                assertFalse(消息.contains("管理员指令.帮助."),
                        "不应包含翻译键前缀'管理员指令.帮助.'，实际消息: " + 消息);
            }
        }

        @Test
        @DisplayName("获取命令名 - 应返回'管理'")
        void 获取命令名_应返回管理() {
            assertEquals("管理", 处理器.获取命令名());
        }
    }

    @Nested
    @DisplayName("Tab补全")
    class Tab补全 {

        @Test
        @DisplayName("无权限玩家Tab补全 - 应返回空列表")
        void 无权限玩家Tab补全_应返回空列表() {
            var 结果 = 处理器.onTabComplete(普通玩家, 命令, "管理", new String[]{""});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("一级Tab补全 - 应返回所有子命令")
        void 一级Tab补全_应返回所有子命令() {
            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{""});

            assertNotNull(结果);
            assertTrue(结果.contains("仇恨"));
            assertTrue(结果.contains("战斗"));
            assertTrue(结果.contains("等级"));
            assertTrue(结果.contains("职业"));
            assertTrue(结果.contains("属性"));
            assertTrue(结果.contains("重载"));
            assertTrue(结果.contains("刷新"));
            assertTrue(结果.contains("技能"));
        }

        @Test
        @DisplayName("仇恨子命令二级Tab补全 - 应返回操作类型")
        void 仇恨子命令二级Tab补全_应返回操作类型() {
            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"仇恨", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("调试"));
            assertTrue(结果.contains("清空"));
            assertTrue(结果.contains("信息"));
        }

        @Test
        @DisplayName("技能子命令二级Tab补全 - 应返回冷却和参数关键词")
        void 技能子命令二级Tab补全_应返回冷却和参数关键词() {
            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"技能", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("冷却"));
            assertTrue(结果.contains("参数"));
        }

        @Test
        @DisplayName("技能冷却二级Tab补全 - 应返回重置关键词")
        void 技能冷却二级Tab补全_应返回重置关键词() {
            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"技能", "冷却", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("重置"));
        }

        @Test
        @DisplayName("FP-05 技能二级Tab补全 - 应同时含关键词与所有技能效果ID")
        void 技能二级Tab补全_应含关键词与所有ID() {
            when(技能注册服务.获取所有技能标识()).thenReturn(java.util.Set.of("1_1", "1_3"));
            when(效果注册服务.获取所有定义()).thenReturn(List.of(
                    new 效果定义("1_3_1", "n", "nt", "dt", "h", 1000L, 1, true, false),
                    new 效果定义("2_0", "n", "nt", "dt", "h", 1000L, 1, true, false)));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"技能", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("冷却"));
            assertTrue(结果.contains("参数"));
            assertTrue(结果.contains("1_1"));
            assertTrue(结果.contains("1_3_1"));
            assertTrue(结果.contains("2_0"));
        }

        @Test
        @DisplayName("FP-05 技能参数子命令三级Tab补全 - 应补全技能效果ID")
        void 技能参数三级Tab补全_应补全ID() {
            when(技能注册服务.获取所有技能标识()).thenReturn(java.util.Set.of("1_1", "1_3"));
            when(效果注册服务.获取所有定义()).thenReturn(List.of(
                    new 效果定义("1_3_1", "n", "nt", "dt", "h", 1000L, 1, true, false)));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"技能", "参数", "1"});

            assertNotNull(结果);
            assertTrue(结果.contains("1_1"));
            assertTrue(结果.contains("1_3"));
            assertTrue(结果.contains("1_3_1"));
            assertFalse(结果.contains("冷却"));
        }

        @Test
        @DisplayName("FP-05 技能释放形式四级Tab补全 - 应补全参数名=形式")
        void 技能释放四级Tab补全_应补全参数名等号形式() {
            参数注册表.参数条目 条目1 = new 参数注册表.参数条目("智力系数", "0.15", "智力伤害系数");
            参数注册表.参数条目 条目2 = new 参数注册表.参数条目("暴击倍率", "1.5", "暴击伤害倍率");
            when(参数注册表.获取参数列表("1_1")).thenReturn(List.of(条目1, 条目2));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理",
                    new String[]{"技能", "1_1", "目标玩家", "暴"});

            assertNotNull(结果);
            assertTrue(结果.contains("暴击倍率=<1.5 暴击伤害倍率>"));
            assertFalse(结果.contains("智力系数="));
        }

        @Test
        @DisplayName("FP-05 技能释放形式四级Tab补全 - 未知ID应返回空")
        void 技能释放四级Tab补全_未知ID应返回空() {
            when(参数注册表.获取参数列表("不存在")).thenReturn(List.of());

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理",
                    new String[]{"技能", "不存在", "目标玩家", "暴"});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }
    }

    @Nested
    @DisplayName("FP-05 参数声明启动即注册")
    class FP05参数声明启动即注册 {

        @Test
        @DisplayName("声明参数 - 不执行技能也应将参数注册到参数注册表")
        void 声明参数_不执行技能_应注册参数到注册表() throws Exception {
            参数注册表 真实注册表 = new 参数注册表();
            奥术冲击 技能 = new 奥术冲击();
            var 字段 = 奥能法师技能基础.class.getDeclaredField("参数注册表实例");
            字段.setAccessible(true);
            字段.set(技能, 真实注册表);

            技能.声明参数();

            List<参数注册表.参数条目> 列表 = 真实注册表.获取参数列表("1_1");
            assertFalse(列表.isEmpty(), "1_1 参数不应为空");
            assertTrue(列表.stream().anyMatch(e -> e.参数名().equals("智力系数") && e.默认值().equals("0.15")));
        }

        @Test
        @DisplayName("声明参数 - 启动即声明效果ID 1_3_1 的参数")
        void 声明参数_启动即声明效果ID参数() throws Exception {
            参数注册表 真实注册表 = new 参数注册表();
            奥术护盾 技能 = new 奥术护盾();
            var 字段 = 奥能法师技能基础.class.getDeclaredField("参数注册表实例");
            字段.setAccessible(true);
            字段.set(技能, 真实注册表);

            技能.声明参数();

            List<参数注册表.参数条目> 列表 = 真实注册表.获取参数列表("1_3_1");
            assertFalse(列表.isEmpty(), "1_3_1 参数不应为空");
            assertTrue(列表.stream().anyMatch(e -> e.参数名().equals("护盾系数") && e.默认值().equals("0.4")));
        }
    }

    @Nested
    @DisplayName("FP-05 技能子命令")
    class FP05技能子命令 {

        @Test
        @DisplayName("技能子命令无参数 - 应显示帮助")
        void 技能子命令无参数_应显示帮助() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理", new String[]{"技能"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("技能子命令")),
                    "应包含帮助标题，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("冷却") && m.contains("重置")),
                    "应包含冷却重置说明，实际消息: " + 消息列表);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("参数") && m.contains("查询")),
                    "应包含参数查询说明，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("技能冷却重置全部 - 应调用重置所有冷却")
        void 技能冷却重置全部_应调用重置所有冷却() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "冷却", "重置", "目标玩家"});

                assertTrue(结果);
                verify(技能冷却服务).重置所有冷却(目标标识);
                verify(技能冷却服务, never()).重置冷却(any(), any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("重置") && m.contains("目标玩家") && m.contains("所有冷却")),
                        "应发送重置全部成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("技能冷却重置单个 - 应调用重置冷却+重置公共冷却")
        void 技能冷却重置单个_应调用重置冷却和重置公共冷却() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "冷却", "重置", "目标玩家", "1_1"});

                assertTrue(结果);
                verify(技能冷却服务).重置冷却(目标标识, "1_1");
                verify(技能冷却服务).重置公共冷却(目标标识);
                verify(技能冷却服务, never()).重置所有冷却(any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("重置") && m.contains("1_1")),
                        "应发送重置单个成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("技能冷却重置 - 缺少参数应发送用法错误")
        void 技能冷却重置_缺少参数应发送用法错误() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"技能", "冷却"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("用法")),
                    "应包含用法错误，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("技能冷却重置 - 玩家不在线应发送不在线错误")
        void 技能冷却重置_玩家不在线应发送不在线错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("不在线玩家")).thenReturn(null);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "冷却", "重置", "不在线玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("不在线")),
                        "应包含不在线错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("技能参数查询 - 无参数列表应发送无参数消息")
        void 技能参数查询_无参数列表应发送无参数消息() {
            when(参数注册表.获取参数列表("1_1")).thenReturn(List.of());

            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"技能", "参数", "1_1"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("无可覆盖参数")),
                    "应发送无可覆盖参数消息，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("技能参数查询 - 有参数列表应发送参数列表")
        void 技能参数查询_有参数列表应发送参数列表() {
            参数注册表.参数条目 条目 = new 参数注册表.参数条目("伤害", "100", "技能伤害数值");
            when(参数注册表.获取参数列表("1_1")).thenReturn(List.of(条目));

            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"技能", "参数", "1_1"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("伤害") && m.contains("100")),
                    "应包含参数行，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("技能参数查询 - 缺少参数应发送用法错误")
        void 技能参数查询_缺少参数应发送用法错误() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"技能", "参数"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("用法")),
                    "应包含用法错误，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("释放技能 - 应重置冷却+调用释放服务并返回成功")
        void 释放技能_应重置冷却并调用释放服务() {
            技能定义 定义 = mock(技能定义.class);
            玩家快照 快照 = mock(玩家快照.class);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(玩家服务.获取快照(目标标识)).thenReturn(Optional.of(快照));
            when(技能释放服务.释放(any())).thenReturn(技能执行结果.成功);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_1", "目标玩家"});

                assertTrue(结果);
                verify(技能冷却服务).重置冷却(目标标识, "1_1");
                verify(技能冷却服务).重置公共冷却(目标标识);
                verify(技能释放服务).释放(any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("释放") && m.contains("1_1")),
                        "应发送释放成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("释放技能 - 释放失败应发送失败消息")
        void 释放技能_释放失败应发送失败消息() {
            技能定义 定义 = mock(技能定义.class);
            玩家快照 快照 = mock(玩家快照.class);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(玩家服务.获取快照(目标标识)).thenReturn(Optional.of(快照));
            when(技能释放服务.释放(any())).thenReturn(技能执行结果.资源不足);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_1", "目标玩家"});

                assertTrue(结果);
                verify(技能释放服务).释放(any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("释放") && m.contains("失败")),
                        "应发送释放失败消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("施加效果 - 应调用添加效果带覆盖")
        void 施加效果_应调用添加效果带覆盖() {
            效果定义 定义 = mock(效果定义.class);
            when(定义.持续时间()).thenReturn(5000L);
            when(技能注册服务.获取定义("1_3_1")).thenReturn(Optional.empty());
            when(效果注册服务.获取定义("1_3_1")).thenReturn(Optional.of(定义));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_3_1", "目标玩家"});

                assertTrue(结果);
                verify(效果调度服务).添加效果带覆盖(eq(目标标识), eq("1_3_1"), eq(""), eq(1), eq(5000L), any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("施加") && m.contains("1_3_1")),
                        "应发送施加效果成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("未知ID - 应发送未知ID错误")
        void 未知ID_应发送未知ID错误() {
            when(技能注册服务.获取定义("不存在")).thenReturn(Optional.empty());
            when(效果注册服务.获取定义("不存在")).thenReturn(Optional.empty());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "不存在", "目标玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("未找到") && m.contains("不存在")),
                        "应发送未找到错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("释放或施加 - 玩家不在线应发送不在线错误")
        void 释放或施加_玩家不在线应发送不在线错误() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("不在线玩家")).thenReturn(null);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_1", "不在线玩家"});

                assertTrue(结果);
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("不在线")),
                        "应包含不在线错误，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("释放或施加 - 参数不足应发送用法错误")
        void 释放或施加_参数不足应发送用法错误() {
            boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                    new String[]{"技能", "1_1"});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(管理员);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("用法")),
                    "应包含用法错误，实际消息: " + 消息列表);
        }

        @Test
        @DisplayName("释放技能（带参数覆盖） - 应将覆盖值传递给释放服务")
        void 释放技能_带参数覆盖_应将覆盖值传递给释放服务() {
            技能定义 定义 = mock(技能定义.class);
            玩家快照 快照 = mock(玩家快照.class);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(玩家服务.获取快照(目标标识)).thenReturn(Optional.of(快照));
            when(技能释放服务.释放(any())).thenReturn(技能执行结果.成功);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_1", "目标玩家", "--伤害系数=0.5"});

                assertTrue(结果);
                ArgumentCaptor<技能上下文> 上下文捕获 = ArgumentCaptor.forClass(技能上下文.class);
                verify(技能释放服务).释放(上下文捕获.capture());
                技能上下文 捕获上下文 = 上下文捕获.getValue();
                参数读取器 覆盖 = 捕获上下文.获取参数覆盖();
                assertNotNull(覆盖, "技能上下文应包含参数覆盖读取器");
                assertTrue(覆盖.是否有覆盖("伤害系数"),
                        "参数覆盖应包含'伤害系数'键，实际覆盖表: " + 覆盖.获取覆盖表());
                assertEquals(0.5, 覆盖.获取双精度("伤害系数", 1.0),
                        "参数覆盖'伤害系数'应解析为0.5");
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("释放") && m.contains("1_1") && m.contains("已为")),
                        "应发送释放成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("施加效果（带参数覆盖） - 应将覆盖值传递给添加效果带覆盖")
        void 施加效果_带参数覆盖_应将覆盖值传递给添加效果带覆盖() {
            效果定义 定义 = mock(效果定义.class);
            when(定义.持续时间()).thenReturn(5000L);
            when(技能注册服务.获取定义("1_3_1")).thenReturn(Optional.empty());
            when(效果注册服务.获取定义("1_3_1")).thenReturn(Optional.of(定义));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_3_1", "目标玩家", "--数值=100"});

                assertTrue(结果);
                ArgumentCaptor<参数读取器> 覆盖捕获 = ArgumentCaptor.forClass(参数读取器.class);
                verify(效果调度服务).添加效果带覆盖(eq(目标标识), eq("1_3_1"), eq(""), eq(1), eq(5000L), 覆盖捕获.capture());
                参数读取器 覆盖 = 覆盖捕获.getValue();
                assertNotNull(覆盖, "应传递非null的参数覆盖读取器");
                assertTrue(覆盖.是否有覆盖("数值"),
                        "参数覆盖应包含'数值'键，实际覆盖表: " + 覆盖.获取覆盖表());
                assertEquals(100, 覆盖.获取整数("数值", 0),
                        "参数覆盖'数值'应解析为100");
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("施加") && m.contains("1_3_1")),
                        "应发送施加效果成功消息，实际消息: " + 消息列表);
            }
        }

        @Test
        @DisplayName("释放技能 - 会话不存在应发送会话不存在错误")
        void 释放技能_会话不存在_应发送会话不存在错误() {
            技能定义 定义 = mock(技能定义.class);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(玩家服务.获取快照(目标标识)).thenReturn(Optional.empty());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer("目标玩家")).thenReturn(目标玩家);

                boolean 结果 = 处理器.onCommand(管理员, 命令, "管理",
                        new String[]{"技能", "1_1", "目标玩家"});

                assertTrue(结果);
                verify(技能释放服务, never()).释放(any());
                List<String> 消息列表 = 捕获所有消息(管理员);
                assertTrue(消息列表.stream().anyMatch(m -> m.contains("会话不存在")),
                        "应发送会话不存在错误，实际消息: " + 消息列表);
            }
        }
    }

    @Nested
    @DisplayName("FP-04 技能参数Tab补全增强")
    class FP04技能参数Tab补全增强 {

        @Test
        @DisplayName("技能二级Tab补全 - 应返回运行时真实注册技能/效果ID而非硬编码")
        void 技能二级Tab补全_应返回真实注册技能效果ID() {
            when(技能注册服务.获取所有技能标识()).thenReturn(java.util.Set.of("1_9_1"));
            when(效果注册服务.获取所有定义()).thenReturn(List.of(
                    new 效果定义("2_0", "n", "nt", "dt", "h", 1000L, 1, true, false)));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理", new String[]{"技能", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("冷却"));
            assertTrue(结果.contains("参数"));
            assertTrue(结果.contains("1_9_1"), "应包含真实注册的技能ID 1_9_1，实际: " + 结果);
            assertTrue(结果.contains("2_0"), "应包含真实注册的效果ID 2_0，实际: " + 结果);
            assertFalse(结果.contains("3_3"), "不应包含未注册的ID，实际: " + 结果);
        }

        @Test
        @DisplayName("技能释放四级Tab补全完成 - 参数项应带默认值与说明")
        void 技能释放四级Tab补全_参数应带默认值与说明() {
            参数注册表.参数条目 条目 = new 参数注册表.参数条目("秘兆触发概率", "0.5", "秘兆触发概率");
            when(参数注册表.获取参数列表("1_9_1")).thenReturn(List.of(条目));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理",
                    new String[]{"技能", "1_9_1", "目标玩家", "秘兆"});

            assertNotNull(结果);
            assertTrue(结果.contains("秘兆触发概率=<0.5 秘兆触发概率>"),
                    "参数项应带默认值与说明，实际: " + 结果);
        }

        @Test
        @DisplayName("技能释放四级Tab补全 - 参数说明翻译文件优先于注册说明")
        void 技能释放四级Tab补全_参数说明翻译优先() {
            翻译文件.set("管理员指令.技能.参数说明.1_9_1.秘兆触发概率", "秘兆触发概率(0~1)");
            参数注册表.参数条目 条目 = new 参数注册表.参数条目("秘兆触发概率", "0.5", "秘兆触发概率");
            when(参数注册表.获取参数列表("1_9_1")).thenReturn(List.of(条目));

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理",
                    new String[]{"技能", "1_9_1", "目标玩家", "秘兆"});

            assertNotNull(结果);
            assertTrue(结果.contains("秘兆触发概率=<0.5 秘兆触发概率(0~1)>"),
                    "应使用翻译文件中的参数说明，实际: " + 结果);
        }

        @Test
        @DisplayName("技能释放四级Tab补全 - 未知ID应返回空")
        void 技能释放四级Tab补全_未知ID应返回空() {
            when(参数注册表.获取参数列表("不存在")).thenReturn(List.of());

            var 结果 = 处理器.onTabComplete(管理员, 命令, "管理",
                    new String[]{"技能", "不存在", "目标玩家", "秘"});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
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
