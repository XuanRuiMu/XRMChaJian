package mljy.业务层.消息;

import mljy.翻译服务;
import mljy.玩家服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.基础设施层.事件日志上下文;
import 暮澜纪元.通用.文本.关键词解析器;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-03 日志颜色修复测试。
 *
 * 验证：
 * - 前缀颜色标签正确闭合（<red>[战斗日志]</red> / <aqua>[技能日志]</aqua>）
 * - 普通文字默认白色，不继承前缀的 <red>/<aqua> 颜色
 * - 同一事件的战斗日志与技能日志分别输出物理单行消息，颜色互不污染
 * - 合并逻辑：战斗行在前、技能行在后，频道前缀不交叉，行内无换行
 *
 * 验证方式：
 * - 通过 MiniMessage.serialize 将捕获的 Component 重新序列化为 MiniMessage 字符串，
 *   检查前缀闭合标签存在
 * - 通过遍历 Component 树，检查纯文本部分的颜色不是 red/aqua
 */
@DisplayName("FP-03 日志颜色修复")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 日志颜色修复测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private Player 玩家;
    @Mock
    private 玩家服务 玩家服务;

    private 日志合并管理器 日志合并管理器;
    private UUID 玩家标识;
    private final MiniMessage 迷你消息 = MiniMessage.miniMessage();

    @BeforeEach
    void setUp() {
        Logger 日志器 = mock(Logger.class);
        when(插件.getLogger()).thenReturn(日志器);
        翻译服务 翻译 = new 闭合前缀翻译服务();
        关键词解析器 解析器 = new 关键词解析器(new XRM技能名解析器(翻译, 玩家服务));
        日志合并管理器 = new 日志合并管理器(插件, 翻译, 解析器);
        玩家标识 = UUID.randomUUID();
    }

    @AfterEach
    void 清理事件上下文() {
        事件日志上下文.清除();
    }

    /**
     * 配置 Bukkit 调度器 mock：让 runTaskLater 立即同步执行 Runnable。
     * 适配 FP-A 重构后的固定延迟窗口合并机制。
     */
    private BukkitScheduler 创建立即执行调度器() {
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            Runnable 任务 = invocation.getArgument(1);
            任务.run();
            return null;
        }).when(调度器).runTaskLater(any(), any(Runnable.class), anyLong());
        return 调度器;
    }

    /**
     * 应用调度器到 Bukkit 静态 mock，避免在 bukkitMock.when() 调用内部进行 stubbing。
     */
    private void 应用调度器(MockedStatic<Bukkit> bukkitMock, BukkitScheduler 调度器) {
        bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
    }

    /**
     * 配置 Bukkit 调度器 mock：让 runTaskLater 立即同步执行 Runnable。
     * 适配 FP-A 重构后的固定延迟窗口合并机制。
     */
    private void 配置立即执行调度器(MockedStatic<Bukkit> bukkitMock) {
        应用调度器(bukkitMock, 创建立即执行调度器());
    }

    /**
     * 配置 Bukkit 调度器 mock：让 runTaskLater 不立即执行，将 Runnable 收集到列表中。
     * 用于多片段合并测试，需要在添加完所有片段后手动触发执行。
     */
    private BukkitScheduler 配置延迟执行调度器(MockedStatic<Bukkit> bukkitMock, List<Runnable> 待执行任务) {
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            Runnable 任务 = invocation.getArgument(1);
            待执行任务.add(任务);
            return null;
        }).when(调度器).runTaskLater(any(), any(Runnable.class), anyLong());
        bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
        return 调度器;
    }

    private Component 捕获消息组件() {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(玩家).sendMessage(捕获器.capture());
        return 捕获器.getValue();
    }

    private List<Component> 捕获所有消息组件() {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(玩家, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues();
    }

    private Component 查找包含前缀的组件(List<Component> 组件列表, String 前缀文本) {
        return 组件列表.stream()
                .filter(c -> PlainTextComponentSerializer.plainText().serialize(c).contains(前缀文本))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到包含[" + 前缀文本 + "]的行，实际: "
                        + 组件列表.stream().map(c -> PlainTextComponentSerializer.plainText().serialize(c)).toList()));
    }

    private String 捕获纯文本() {
        return PlainTextComponentSerializer.plainText().serialize(捕获消息组件());
    }

    /**
     * 递归查找包含指定文本片段的叶子组件，返回其颜色。
     * 返回 null 表示该文本片段未设置颜色（继承默认色，即白色）。
     */
    private net.kyori.adventure.text.format.NamedTextColor 查找文本颜色(Component 组件, String 文本片段) {
        if (组件 instanceof TextComponent) {
            TextComponent 文本组件 = (TextComponent) 组件;
            if (文本组件.content().contains(文本片段)) {
                net.kyori.adventure.text.format.TextColor 颜色 = 组件.style().color();
                if (颜色 instanceof net.kyori.adventure.text.format.NamedTextColor) {
                    return (net.kyori.adventure.text.format.NamedTextColor) 颜色;
                }
                return null;
            }
        }
        for (Component 子组件 : 组件.children()) {
            net.kyori.adventure.text.format.NamedTextColor 颜色 = 查找文本颜色(子组件, 文本片段);
            if (颜色 != null || containsText(子组件, 文本片段)) {
                return 颜色;
            }
        }
        return null;
    }

    private boolean containsText(Component 组件, String 文本片段) {
        if (组件 instanceof TextComponent) {
            return ((TextComponent) 组件).content().contains(文本片段);
        }
        for (Component 子组件 : 组件.children()) {
            if (containsText(子组件, 文本片段)) {
                return true;
            }
        }
        return false;
    }

    @Nested
    @DisplayName("前缀颜色标签闭合")
    class 前缀闭合 {

        @Test
        @DisplayName("战斗日志前缀应包含闭合的 </red> 标签")
        void 战斗日志前缀应闭合() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加战斗日志(玩家标识, "对僵尸造成了100点伤害");

                String 序列化 = 迷你消息.serialize(捕获消息组件());
                assertTrue(序列化.contains("</red>"),
                        "战斗日志前缀应闭合 </red>，序列化结果: " + 序列化);
            }
        }

        @Test
        @DisplayName("技能日志前缀应包含闭合的 </aqua> 标签")
        void 技能日志前缀应闭合() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                String 序列化 = 迷你消息.serialize(捕获消息组件());
                assertTrue(序列化.contains("</aqua>"),
                        "技能日志前缀应闭合 </aqua>，序列化结果: " + 序列化);
            }
        }
    }

    @Nested
    @DisplayName("普通文字默认白色 - 不继承前缀颜色")
    class 普通文字白色 {

        @Test
        @DisplayName("战斗日志纯文本部分不应继承红色")
        void 战斗日志纯文本不应继承红色() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加战斗日志(玩家标识, "对僵尸造成了100点伤害");

                Component 组件 = 捕获消息组件();
                net.kyori.adventure.text.format.NamedTextColor 文本颜色 = 查找文本颜色(组件, "对僵尸");
                assertFalse(文本颜色 == net.kyori.adventure.text.format.NamedTextColor.RED,
                        "纯文本'对僵尸'不应是红色，实际颜色: " + 文本颜色);
            }
        }

        @Test
        @DisplayName("技能日志纯文本部分不应继承淡紫色")
        void 技能日志纯文本不应继承淡紫色() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                Component 组件 = 捕获消息组件();
                net.kyori.adventure.text.format.NamedTextColor 文本颜色 = 查找文本颜色(组件, "释放了");
                assertFalse(文本颜色 == net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE,
                        "纯文本'释放了'不应是淡紫色，实际颜色: " + 文本颜色);
            }
        }

        @Test
        @DisplayName("战斗日志前缀部分应为红色")
        void 战斗日志前缀应为红色() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加战斗日志(玩家标识, "对僵尸造成了100点伤害");

                Component 组件 = 捕获消息组件();
                net.kyori.adventure.text.format.NamedTextColor 前缀颜色 = 查找文本颜色(组件, "[战斗日志]");
                assertEquals(net.kyori.adventure.text.format.NamedTextColor.RED, 前缀颜色,
                        "前缀'[战斗日志]'应为红色，实际颜色: " + 前缀颜色);
            }
        }

        @Test
        @DisplayName("技能日志前缀部分应为青色")
        void 技能日志前缀应为青色() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                Component 组件 = 捕获消息组件();
                net.kyori.adventure.text.format.NamedTextColor 前缀颜色 = 查找文本颜色(组件, "[技能日志]");
                assertEquals(net.kyori.adventure.text.format.NamedTextColor.AQUA, 前缀颜色,
                        "前缀'[技能日志]'应为青色，实际颜色: " + 前缀颜色);
            }
        }
    }

    @Nested
    @DisplayName("分频道颜色不污染 - 同一事件战斗+技能单行")
    class 合并颜色不污染 {

        @Test
        @DisplayName("技能日志行内容不应被战斗日志前缀的红色污染")
        void 合并时技能日志不应被红色污染() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                UUID 事件标识 = UUID.randomUUID();
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");

                Component 技能组件 = 查找包含前缀的组件(捕获所有消息组件(), "[技能日志]");

                net.kyori.adventure.text.format.NamedTextColor 技能文本颜色 = 查找文本颜色(技能组件, "释放了");
                assertFalse(技能文本颜色 == net.kyori.adventure.text.format.NamedTextColor.RED,
                        "技能日志行'释放了'不应被红色污染，实际颜色: " + 技能文本颜色);
            }
        }

        @Test
        @DisplayName("战斗日志行纯文本不应为红色")
        void 合并时战斗日志纯文本不应为红色() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                UUID 事件标识 = UUID.randomUUID();
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");

                Component 战斗组件 = 查找包含前缀的组件(捕获所有消息组件(), "[战斗日志]");

                net.kyori.adventure.text.format.NamedTextColor 战斗文本颜色 = 查找文本颜色(战斗组件, "对僵尸");
                assertFalse(战斗文本颜色 == net.kyori.adventure.text.format.NamedTextColor.RED,
                        "战斗日志行'对僵尸'不应为红色，实际颜色: " + 战斗文本颜色);
            }
        }

        @Test
        @DisplayName("同一事件合并顺序应为战斗日志段在前、技能日志段在后")
        void 合并顺序应为战斗在前技能在后() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                UUID 事件标识 = UUID.randomUUID();
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");

                List<String> 文本列表 = 捕获所有消息组件().stream()
                        .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                        .toList();
                assertEquals(2, 文本列表.size(), "同一事件的战斗与技能日志应分别发送两行，实际: " + 文本列表);
                String 战斗行 = 文本列表.stream()
                        .filter(文本 -> 文本.contains("[战斗日志]"))
                        .findFirst()
                        .orElseThrow();
                String 技能行 = 文本列表.stream()
                        .filter(文本 -> 文本.contains("[技能日志]"))
                        .findFirst()
                        .orElseThrow();
                assertFalse(战斗行.contains("[技能日志]"), "战斗行不得包含技能前缀，实际: " + 战斗行);
                assertFalse(技能行.contains("[战斗日志]"), "技能行不得包含战斗前缀，实际: " + 技能行);
                assertTrue(战斗行.contains("对僵尸造成了100点伤害"), "应包含战斗内容，实际: " + 战斗行);
                assertTrue(技能行.contains("释放了奥术冲击"), "应包含技能内容，实际: " + 技能行);
                assertEquals("[战斗日志]", 战斗行.substring(0, "[战斗日志]".length()),
                        "战斗行应先以战斗前缀开始，实际: " + 战斗行);
                assertEquals("[技能日志]", 技能行.substring(0, "[技能日志]".length()),
                        "技能行应先以技能前缀开始，实际: " + 技能行);
                文本列表.forEach(文本 ->
                        assertFalse(文本.contains("\r") || 文本.contains("\n"),
                                "物理日志行不应包含换行，实际: " + 文本));
            }
        }
    }

    @Nested
    @DisplayName("同一事件合并")
    class 事件事务合并 {

        @Test
        @DisplayName("同一事件内多条战斗日志和技能日志合并为单行，只保留战斗日志前缀")
        void 同一事件多条日志应合并为单行() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                List<Runnable> 待执行任务 = new java.util.ArrayList<>();
                配置延迟执行调度器(bukkitMock, 待执行任务);

                UUID 事件标识 = UUID.randomUUID();
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "吸血恢复25点生命值");
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");
                待执行任务.forEach(Runnable::run);

                List<String> 文本列表 = 捕获所有消息组件().stream()
                        .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                        .toList();
                assertEquals(1, 文本列表.size(), "同一事件战斗+技能日志应合并为单行消息，实际: " + 文本列表);
                String 文本 = 文本列表.get(0);
                assertTrue(文本.contains("[战斗日志]"), "应以战斗日志前缀开头，实际: " + 文本);
                assertFalse(文本.contains("[技能日志]"), "不得包含技能日志前缀，实际: " + 文本);
                assertTrue(文本.contains("对僵尸造成了100点伤害"), "应包含第一条战斗日志，实际: " + 文本);
                assertTrue(文本.contains("吸血恢复25点生命值"), "应包含第二条战斗日志，实际: " + 文本);
                assertTrue(文本.contains("释放了奥术冲击"), "应包含技能日志，实际: " + 文本);
                assertFalse(文本.contains("\r") || 文本.contains("\n"),
                        "合并日志不应包含换行，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("只技能日志时应使用技能日志前缀且无战斗日志前缀")
        void 只技能日志时应用技能日志前缀() {
            when(玩家.isOnline()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                配置立即执行调度器(bukkitMock);

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                String 纯文本 = 捕获纯文本();
                assertTrue(纯文本.contains("[技能日志]"), "应使用技能日志前缀，实际: " + 纯文本);
                assertFalse(纯文本.contains("[战斗日志]"), "不应出现战斗日志前缀，实际: " + 纯文本);
            }
        }
    }

    /**
     * 闭合前缀翻译服务 - 反映修复后的真实翻译文件行为。
     * 前缀颜色标签已闭合，确保普通文字默认白色。
     */
    private static class 闭合前缀翻译服务 implements 翻译服务 {
        @Override
        public String 获取(String 键, Locale 语言, Object... 参数) {
            return 获取(键, 参数);
        }

        @Override
        public String 获取(String 键, Object... 参数) {
            switch (键) {
                case "日志合并管理器.前缀.技能日志":
                    return "<aqua>[技能日志]</aqua>";
                case "日志合并管理器.前缀.战斗日志":
                    return "<red>[战斗日志]</red>";
                case "错误前缀":
                    return "<red>[错误]</red>";
                default:
                    return 键;
            }
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
