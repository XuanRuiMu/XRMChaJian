package mljy.业务层.消息;

import mljy.翻译服务;
import mljy.玩家服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.基础设施层.事件日志上下文;
import mljy.基础设施层.调试日志器;
import 暮澜纪元.通用.文本.关键词解析器;
import net.kyori.adventure.text.Component;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-A 日志合并管理器测试。
 *
 * 验证 FP-A 重构后的固定延迟窗口合并机制：
 * - 第一条日志到达后调度 1 tick 延迟任务，任务执行时发送合并日志
 * - 延迟窗口内到达的同键片段追加合并，最终一次性发送
 * - 不再依赖事件完成回调，未命中场景下日志仍正常发送
 * - 战斗日志和技能日志按前缀分别输出物理单行消息
 * - 不同因果事件即使属于同一玩家也不合并
 * - 空内容/null 内容不触发调度
 * - 玩家不在线/不存在时通过翻译文件记录警告并丢弃条目
 */
@DisplayName("FP-A 日志合并管理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 日志合并管理器测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private Player 玩家;
    @Mock
    private 玩家服务 玩家服务;

    private 日志合并管理器 日志合并管理器;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        Logger 日志器 = mock(Logger.class);
        when(插件.getLogger()).thenReturn(日志器);
        翻译服务 翻译 = new 测试翻译服务();
        关键词解析器 解析器 = new 关键词解析器(new XRM技能名解析器(翻译, 玩家服务));
        日志合并管理器 = new 日志合并管理器(插件, 翻译, 解析器);
        玩家标识 = UUID.randomUUID();
    }

    @AfterEach
    void 清理事件上下文() {
        事件日志上下文.清理全部玩家目标因果凭证();
        事件日志上下文.清除();
    }

    /**
     * 配置调度器：让 runTaskLater 立即同步执行 Runnable。
     * 用于模拟"1 tick 后执行"在测试中的等价立即执行。
     */
    private BukkitScheduler 配置立即执行调度器() {
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            Runnable 任务 = invocation.getArgument(1);
            任务.run();
            return null;
        }).when(调度器).runTaskLater(any(), any(Runnable.class), anyLong());
        return 调度器;
    }

    /**
     * 配置调度器：让 runTaskLater 不立即执行，将 Runnable 收集到列表中。
     * 用于"多片段合并发送"测试，需要在添加完所有片段后手动触发执行。
     */
    private BukkitScheduler 配置延迟执行调度器(List<Runnable> 待执行任务) {
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            Runnable 任务 = invocation.getArgument(1);
            待执行任务.add(任务);
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

    private String 捕获消息文本() {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(玩家).sendMessage(捕获器.capture());
        return PlainTextComponentSerializer.plainText().serialize(捕获器.getValue());
    }

    private List<String> 捕获所有消息文本() {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(玩家, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
    }

    @Nested
    @DisplayName("FP-A-1 单片段立即调度发送 - 第一条日志触发 1 tick 延迟任务")
    class 单片段调度发送 {

        @Test
        @DisplayName("添加技能日志 - 玩家应收到包含技能日志前缀和内容的消息")
        void 添加技能日志_玩家应收到消息() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("[技能日志]"), "应包含技能日志前缀，实际: " + 文本);
                assertTrue(文本.contains("释放了奥术冲击"), "应包含技能日志内容，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("添加战斗日志 - 玩家应收到包含战斗日志前缀和内容的消息")
        void 添加战斗日志_玩家应收到消息() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加战斗日志(玩家标识, "对僵尸造成了100点伤害");

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("[战斗日志]"), "应包含战斗日志前缀，实际: " + 文本);
                assertTrue(文本.contains("对僵尸造成了100点伤害"), "应包含战斗日志内容，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("调度发送 - 应调用 Bukkit 调度器 runTaskLater 且延迟为 1 tick")
        void 调度发送_应调用调度器延迟1tick() {
            when(玩家.isOnline()).thenReturn(true);
            BukkitScheduler 调度器 = mock(BukkitScheduler.class);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                verify(调度器, times(1)).runTaskLater(any(JavaPlugin.class), any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L));
            }
        }
    }

    @Nested
    @DisplayName("FP-A-2 多片段合并发送 - 同事件后续片段追加合并")
    class 多片段合并发送 {

        @Test
        @DisplayName("同事件多条技能日志 - 延迟任务执行时合并为一条消息且无空格分隔")
        void 同事件多条技能日志_合并为一条() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "获得秘能");

                assertEquals(1, 待执行任务.size(), "第一条片段应触发一次调度，后续片段应追加合并");

                待执行任务.forEach(Runnable::run);

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("[技能日志]"), "应包含技能日志前缀，实际: " + 文本);
                assertTrue(文本.contains("释放了奥术冲击"), "应包含第一条技能日志，实际: " + 文本);
                assertTrue(文本.contains("获得秘能"), "应包含第二条技能日志，实际: " + 文本);
                assertTrue(文本.contains("释放了奥术冲击获得秘能"),
                        "同频道条目应直接拼接无空格，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("同事件多条战斗日志 - 延迟任务执行时合并为一条消息且无空格分隔")
        void 同事件多条战斗日志_合并为一条() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "吸血恢复25点生命值");

                assertEquals(1, 待执行任务.size(), "第一条片段应触发一次调度，后续片段应追加合并");

                待执行任务.forEach(Runnable::run);

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("[战斗日志]"), "应包含战斗日志前缀，实际: " + 文本);
                assertTrue(文本.contains("对僵尸造成了100点伤害"), "应包含第一条战斗日志，实际: " + 文本);
                assertTrue(文本.contains("吸血恢复25点生命值"), "应包含第二条战斗日志，实际: " + 文本);
                assertTrue(文本.contains("对僵尸造成了100点伤害吸血恢复25点生命值"),
                        "同频道条目应直接拼接无空格，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("同事件战斗+技能日志 - 合并为单行，只保留战斗日志前缀")
        void 同事件混合日志_合并为单行输出() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "造成100点物理伤害");
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "奥术冲击造成50点奥术伤害");

                assertEquals(1, 待执行任务.size(), "第一条片段应触发一次调度，后续片段应追加合并");

                待执行任务.forEach(Runnable::run);

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(1, 文本列表.size(), "战斗和技能日志应合并为单行消息，实际: " + 文本列表);
                String 文本 = 文本列表.get(0);
                assertTrue(文本.contains("[战斗日志]"), "应以战斗日志前缀开头，实际: " + 文本);
                assertFalse(文本.contains("[技能日志]"), "不得包含技能日志前缀，实际: " + 文本);
                assertTrue(文本.contains("造成100点物理伤害"), "应包含战斗日志内容，实际: " + 文本);
                assertTrue(文本.contains("奥术冲击造成50点奥术伤害"), "应包含技能日志内容，实际: " + 文本);
                assertFalse(文本.startsWith("[战斗日志] "), "战斗日志前缀后不应有空格（避免触发自动换行），实际: " + 文本);
            }
        }

        @Test
        @DisplayName("吸血尾缀应在合并句末尾以单条输出，不混入中间片段（合并时解析着色）")
        void 吸血尾缀_合并句末尾单条输出() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");
                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "触发了暴击");
                日志合并管理器.设置吸血尾缀(玩家标识, 事件标识, 25.0);

                assertEquals(1, 待执行任务.size(), "第一条片段应触发一次调度");

                待执行任务.forEach(Runnable::run);

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("对僵尸造成了100点伤害"), "应包含主伤害，实际: " + 文本);
                assertTrue(文本.contains("触发了暴击"), "应包含暴击片段，实际: " + 文本);
                assertTrue(文本.contains("通过"), "吸血尾缀应包含'通过'，实际: " + 文本);
                assertTrue(文本.contains("25.0"), "吸血尾缀应包含吸血量'25.0'，实际: " + 文本);
                assertTrue(文本.contains("生命值"), "吸血尾缀应包含'生命值'，实际: " + 文本);
                int 主伤害位置 = 文本.indexOf("对僵尸造成了100点伤害");
                int 吸血位置 = 文本.indexOf("25.0");
                assertTrue(吸血位置 > 主伤害位置, "吸血尾缀应在合并句末尾，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("同事件多次吸血尾缀应数值求和并输出唯一一条（问题5 bug3）")
        void 吸血尾缀_多次累加应求和() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "第一击造成伤害");
                日志合并管理器.设置吸血尾缀(玩家标识, 事件标识, 10.0);
                日志合并管理器.设置吸血尾缀(玩家标识, 事件标识, 15.0);

                assertEquals(1, 待执行任务.size(), "第一条片段应触发一次调度");

                待执行任务.forEach(Runnable::run);

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("25.0"), "多次吸血应求和=25.0，实际: " + 文本);
                // 唯一一条：文本中应只出现一次'通过'
                assertEquals(1, 文本.split("通过", -1).length - 1, "吸血尾缀应唯一一条，实际: " + 文本);
                int 伤害位置 = 文本.indexOf("第一击造成伤害");
                int 吸血位置 = 文本.indexOf("25.0");
                assertTrue(吸血位置 > 伤害位置, "求和吸血应在合并句末尾，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("奥术冲击未命中 - 根事件未结束时延迟任务仍触发发送，日志不被吞")
        void 奥术冲击未命中_延迟任务触发发送() {
            when(玩家.isOnline()).thenReturn(true);
            List<Runnable> 待执行任务 = new ArrayList<>();
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置延迟执行调度器(待执行任务));

                // 模拟释放奥术冲击未命中：仅添加技能日志，不调用结束事件
                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");

                // 不调用事件日志上下文.结束事件
                待执行任务.forEach(Runnable::run);

                String 文本 = 捕获消息文本();
                assertTrue(文本.contains("[技能日志]"), "未命中场景下应仍发送技能日志，实际: " + 文本);
                assertTrue(文本.contains("释放了奥术冲击"), "应包含技能日志内容，实际: " + 文本);
            }
        }
    }

    @Nested
    @DisplayName("FP-A-3 不同事件标识分别独立调度发送")
    class 不同事件独立调度 {

        @Test
        @DisplayName("不同事件标识 - 各自调度独立发送，互不合并")
        void 不同事件标识_各自独立发送() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加战斗日志(玩家标识, UUID.randomUUID(), "第一根事件");
                日志合并管理器.添加战斗日志(玩家标识, UUID.randomUUID(), "第二根事件");

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(2, 文本列表.size(), "不同因果事件不得合并，实际: " + 文本列表);
                assertTrue(文本列表.stream().anyMatch(t -> t.contains("第一根事件")));
                assertTrue(文本列表.stream().anyMatch(t -> t.contains("第二根事件")));
            }
        }

        @Test
        @DisplayName("自然DOT每跳 - 应使用独立根事件且分别发送")
        void 自然DOT每跳_使用独立根事件() {
            when(玩家.isOnline()).thenReturn(true);
            UUID 第一跳根事件 = UUID.randomUUID();
            UUID 第二跳根事件 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, 第一跳根事件, "自然DOT第1跳");
                日志合并管理器.添加技能日志(玩家标识, 第二跳根事件, "自然DOT第2跳");

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(2, 文本列表.size(), "每次自然DOT跳必须独立发送，实际: " + 文本列表);
                assertTrue(文本列表.stream().anyMatch(文本 -> 文本.contains("自然DOT第1跳")));
                assertTrue(文本列表.stream().anyMatch(文本 -> 文本.contains("自然DOT第2跳")));
            }
        }

        @Test
        @DisplayName("独立事件延迟任务并行调度 - 各事件调度一次，互不阻塞")
        void 独立事件并行调度_各调度一次() {
            when(玩家.isOnline()).thenReturn(true);
            BukkitScheduler 调度器 = mock(BukkitScheduler.class);
            List<Runnable> 触发任务 = new ArrayList<>();
            doAnswer(invocation -> {
                Runnable 任务 = invocation.getArgument(1);
                触发任务.add(任务);
                return null;
            }).when(调度器).runTaskLater(any(), any(Runnable.class), anyLong());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

                日志合并管理器.添加技能日志(玩家标识, UUID.randomUUID(), "事件一");
                日志合并管理器.添加技能日志(玩家标识, UUID.randomUUID(), "事件二");

                verify(调度器, times(2)).runTaskLater(any(JavaPlugin.class), any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L));
                assertEquals(2, 触发任务.size(), "两个独立事件应各自调度一次");

                触发任务.forEach(Runnable::run);

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(2, 文本列表.size(), "两个独立事件应分别发送，实际: " + 文本列表);
            }
        }
    }

    @Nested
    @DisplayName("FP-A-4 单行化处理换行符")
    class 单行化处理 {

        @Test
        @DisplayName("日志内容包含换行 - 应转换为空格")
        void 日志内容包含换行_应转换为空格() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识,
                        "第一\r\n第二\r第三\n第四\u000B第五\u000C第六\u0085第七\u2028第八\u2029第九"
                                + "<newline>第十<br>第十一<BR />第十二");

                String 文本 = 捕获消息文本();
                assertEquals(0, 文本.chars().filter(c -> c == '\r').count());
                assertEquals(0, 文本.chars().filter(c -> c == '\n').count());
                assertTrue(文本.contains("第一 第二 第三 第四 第五 第六 第七 第八 第九 第十 第十一 第十二"),
                        "所有物理换行、Unicode换行和MiniMessage换行标签都应转换为空格，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("翻译前缀包含换行 - 最终发送仍为单行")
        void 翻译前缀包含换行_最终仍为单行() {
            when(玩家.isOnline()).thenReturn(true);
            日志合并管理器 带换行前缀 = new 日志合并管理器(插件, new 换行前缀翻译服务(),
                    new 关键词解析器(new XRM技能名解析器(new 换行前缀翻译服务(), 玩家服务)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                带换行前缀.添加技能日志(玩家标识, "释放了奥术冲击");

                String 文本 = 捕获消息文本();
                assertFalse(文本.contains("\r"));
                assertFalse(文本.contains("\n"));
                assertTrue(文本.contains("[技能 日志]"), "翻译前缀换行应折叠为空格，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("客户端物品翻译标签 - 应保留为客户端可解析的语言组件")
        void 客户端物品翻译标签_应保留() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                UUID 事件标识 = UUID.randomUUID();
                日志合并管理器.添加战斗日志(
                        玩家标识,
                        事件标识,
                        "你用<lang:item.minecraft.stone_axe>对血缚恶魔造成了1.0点物理伤害。"
                );

                ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
                verify(玩家).sendMessage(捕获器.capture());
                String MiniMessage文本 = MiniMessage.miniMessage().serialize(捕获器.getValue());
                assertTrue(
                        MiniMessage文本.contains("<lang:item.minecraft.stone_axe>"),
                        "客户端物品翻译标签不得在消息管线中丢失，实际: " + MiniMessage文本
                );
            }
        }
    }

    @Nested
    @DisplayName("边界值 - 空内容和null内容")
    class 空内容处理 {

        @Test
        @DisplayName("空字符串技能日志 - 不应调度发送")
        void 空字符串技能日志_不应发送() {
            BukkitScheduler 调度器 = mock(BukkitScheduler.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

                日志合并管理器.添加技能日志(玩家标识, "");

                verify(调度器, never()).runTaskLater(any(), any(Runnable.class), anyLong());
            }
        }

        @Test
        @DisplayName("null技能日志 - 不应调度发送")
        void null技能日志_不应发送() {
            BukkitScheduler 调度器 = mock(BukkitScheduler.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

                日志合并管理器.添加技能日志(玩家标识, (String) null);

                verify(调度器, never()).runTaskLater(any(), any(Runnable.class), anyLong());
            }
        }

        @Test
        @DisplayName("空字符串战斗日志 - 不应调度发送")
        void 空字符串战斗日志_不应发送() {
            BukkitScheduler 调度器 = mock(BukkitScheduler.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

                日志合并管理器.添加战斗日志(玩家标识, "");

                verify(调度器, never()).runTaskLater(any(), any(Runnable.class), anyLong());
            }
        }
    }

    @Nested
    @DisplayName("边界值 - 玩家在线状态")
    class 玩家在线状态 {

        @Test
        @DisplayName("玩家不在线 - 不应发送消息但应通过翻译文件记录警告")
        void 玩家不在线_不应发送但应记录警告() {
            when(玩家.isOnline()).thenReturn(false);
            Logger 日志器 = 插件.getLogger();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                verify(玩家, never()).sendMessage(any(Component.class));
                verify(日志器, atLeastOnce()).warning(org.mockito.ArgumentMatchers.contains("玩家不在线"));
            }
        }

        @Test
        @DisplayName("玩家不存在 - 不应发送消息但应通过翻译文件记录警告")
        void 玩家不存在_不应发送但应记录警告() {
            Logger 日志器 = 插件.getLogger();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                verify(玩家, never()).sendMessage(any(Component.class));
                verify(日志器, atLeastOnce()).warning(org.mockito.ArgumentMatchers.contains("玩家不在线"));
            }
        }
    }

    @Nested
    @DisplayName("前缀格式 - 单频道前缀输出")
    class 前缀格式 {

        @Test
        @DisplayName("只添加技能日志 - 应使用技能日志前缀")
        void 只添加技能日志_应使用技能日志前缀() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, "释放了奥术冲击");

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(1, 文本列表.size(), "只有技能日志时应发送一行，实际: " + 文本列表);
                String 文本 = 文本列表.get(0);
                assertTrue(文本.contains("[技能日志]"), "应使用技能日志前缀，实际: " + 文本);
                assertFalse(文本.contains("[战斗日志]"), "不应出现战斗日志前缀，实际: " + 文本);
            }
        }

        @Test
        @DisplayName("只添加战斗日志 - 应使用战斗日志前缀且无技能日志前缀")
        void 只添加战斗日志_应使用战斗日志前缀() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加战斗日志(玩家标识, "对僵尸造成了100点伤害");

                List<String> 文本列表 = 捕获所有消息文本();
                assertEquals(1, 文本列表.size(), "只有战斗日志时应发送一行，实际: " + 文本列表);
                String 文本 = 文本列表.get(0);
                assertTrue(文本.contains("[战斗日志]"), "应使用战斗日志前缀，实际: " + 文本);
                assertFalse(文本.contains("[技能日志]"), "不应出现技能日志前缀，实际: " + 文本);
            }
        }
    }

    @Nested
    @DisplayName("FP-F Debug记录实际输出")
    class Debug记录实际输出 {

        @Test
        @DisplayName("发送合并日志 - 应调用调试日志器记录玩家、事件、频道、文本")
        void 发送合并日志_应记录最终输出文本() {
            when(玩家.isOnline()).thenReturn(true);
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
                 MockedStatic<调试日志器> 调试日志器Mock = mockStatic(调试日志器.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害");

                // 验证调用了 调试日志器.调试 记录最终输出，包含玩家、事件、频道、文本
                调试日志器Mock.verify(() -> 调试日志器.调试(
                        eq("日志合并管理器"),
                        eq("发送合并日志：玩家=%s 事件=%s 频道=%s 文本=%s"),
                        eq(玩家标识),
                        eq(事件标识),
                        eq("战斗日志"),
                        contains("对僵尸造成了100点伤害")
                ));
            }
        }

        @Test
        @DisplayName("发送技能日志合并 - 应记录频道为技能日志")
        void 发送技能日志合并_应记录技能频道() {
            when(玩家.isOnline()).thenReturn(true);
            UUID 事件标识 = UUID.randomUUID();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
                 MockedStatic<调试日志器> 调试日志器Mock = mockStatic(调试日志器.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加技能日志(玩家标识, 事件标识, "释放了奥术冲击");

                // 验证调用了 调试日志器.调试 记录技能日志最终输出
                调试日志器Mock.verify(() -> 调试日志器.调试(
                        eq("日志合并管理器"),
                        eq("发送合并日志：玩家=%s 事件=%s 频道=%s 文本=%s"),
                        eq(玩家标识),
                        eq(事件标识),
                        eq("技能日志"),
                        contains("释放了奥术冲击")
                ));
            }
        }

        @Test
        @DisplayName("添加战斗日志片段 - 应调用调试日志器记录玩家、事件、优先级、内容")
        void 添加战斗日志片段_应记录接收内容() {
            when(玩家.isOnline()).thenReturn(true);
            UUID 事件标识 = UUID.randomUUID();
            int 优先级 = 10;

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
                 MockedStatic<调试日志器> 调试日志器Mock = mockStatic(调试日志器.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                应用调度器(bukkitMock, 配置立即执行调度器());

                日志合并管理器.添加战斗日志(玩家标识, 事件标识, "对僵尸造成了100点伤害", 优先级);

                // 验证调用了 调试日志器.调试 记录接收的战斗日志片段，包含玩家、事件、内容、优先级
                调试日志器Mock.verify(() -> 调试日志器.调试(
                        eq("日志合并管理器"),
                        eq("已记录战斗日志：玩家=%s 事件=%s 类型=战斗日志 内容=%s 优先级=%d"),
                        eq(玩家标识),
                        eq(事件标识),
                        contains("对僵尸造成了100点伤害"),
                        eq(优先级)
                ));
            }
        }
    }

    private static class 测试翻译服务 implements 翻译服务 {
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
                case "日志合并管理器.警告.玩家不在线":
                    return "玩家不在线，已丢弃合并日志：玩家=" + (参数.length > 0 ? 参数[0] : "");
                case "日志合并管理器.错误.发送失败":
                    return "日志合并发送失败：键=" + (参数.length > 0 ? 参数[0] : "")
                            + " 原因=" + (参数.length > 1 ? 参数[1] : "");
                case "战斗日志.吸血":
                    return "通过[吸血]恢复了[治疗数值:{0}]点[生命值]。";
                default:
                    return 键;
            }
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }

    private static class 换行前缀翻译服务 extends 测试翻译服务 {
        @Override
        public String 获取(String 键, Object... 参数) {
            if ("日志合并管理器.前缀.技能日志".equals(键)) {
                return "<aqua>[技能\r\n日志]</aqua>";
            }
            return super.获取(键, 参数);
        }
    }
}
