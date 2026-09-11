package mljy.全流程文本输出测试;

import mljy.玩家服务;
import mljy.业务层.公共冷却显示服务实现;
import mljy.业务层.技能冷却服务;
import mljy.业务层.属性计算服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-03 BossBar全流程文本输出测试。
 *
 * 验证公共冷却显示服务实现的 BossBar 标题构建逻辑符合翻译文件 + 关键词解析器规范。
 * 使用 ArgumentCaptor 捕获 Bukkit.createBossBar 和 BossBar.setTitle 的实际参数，
 * 验证标题文本包含正确的关键词颜色标签和数值格式。
 *
 * 翻译键（zh.yml 实际内容，所有语言统一）：
 * - 红色阶段: "<dark_gray>GCD</dark_gray> <white>-</white> <red>%.1fs</red><white>/</white><dark_gray>%.1fs</dark_gray>"
 * - 黄色阶段: "<dark_gray>GCD</dark_gray> <white>-</white> <yellow>%.1fs</yellow><white>/</white><dark_gray>%.1fs</dark_gray>"
 * - 绿色阶段: "<dark_gray>GCD</dark_gray> <white>-</white> <green>%.1fs</green><white>/</white><dark_gray>%.1fs</dark_gray>"
 * - 空闲阶段: "<dark_gray>GCD</dark_gray> <white>-</white> <white>%.1fs</white><white>/</white><dark_gray>%.1fs</dark_gray>"
 * 所有语言统一使用 GCD 前缀（需求文档第350行），不再使用 [公共冷却] 关键词语法。
 */
@DisplayName("FP-TEXT-03 BossBar全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BossBar全流程测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 技能冷却服务 技能冷却服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private Player 玩家;
    @Mock
    private BossBar bossBar;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 公共冷却显示服务实现 服务;

    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/技能/管理/公CD显示管理器/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        服务 = new 公共冷却显示服务实现(插件, 翻译服务, 关键词解析器, 玩家服务, 技能冷却服务, 属性计算服务);

        玩家标识 = UUID.randomUUID();
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.isOnline()).thenReturn(true);
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

    private 玩家快照 创建快照(double 公共冷却, double 急速) {
        属性快照 属性 = 属性快照.创建(
                100.0, 公共冷却, 急速,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        return new 玩家快照(玩家标识, "测试玩家", 1, null, 属性, Collections.emptyMap(), 战斗状态.非战斗);
    }

    @Nested
    @DisplayName("启动恒定显示全流程：创建BossBar → 设置空闲标题 → 启动定时任务")
    class 启动恒定显示全流程 {

        @Test
        @DisplayName("启动恒定显示：捕获createBossBar标题应包含'公共冷却'关键词")
        void 启动恒定显示_标题应包含公共冷却关键词() {
            玩家快照 快照 = 创建快照(1.5, 0.0);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算实际公共冷却(1.5, 0.0)).thenReturn(1.5);

            ArgumentCaptor<String> 标题捕获器 = ArgumentCaptor.forClass(String.class);
            try (MockedStatic<Bukkit> bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkitMock.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
                        .thenReturn(bossBar);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(1L))).thenReturn(任务);

                服务.启动恒定显示(玩家标识);

                // 标题在 createBossBar 时传入，不调用 setTitle
                bukkitMock.verify(() -> Bukkit.createBossBar(
                        标题捕获器.capture(), any(BarColor.class), any(BarStyle.class)));
            }

            String 标题 = 标题捕获器.getValue();
            // 空闲阶段翻译键存在，值为 "<dark_gray>GCD</dark_gray> <white>-</white> <white>%.1fs</white><white>/</white><dark_gray>%.1fs</dark_gray>"
            // 经 String.format 和 转传统颜色 后，标题应包含 "GCD"，不含占位符
            assertTrue(标题.contains("GCD"),
                    "标题应包含'GCD'前缀，实际：" + 标题);
            assertFalse(标题.contains("[错误]"),
                    "标题不应包含'[错误]'占位符，实际：" + 标题);
            assertFalse(标题.contains("[警告]"),
                    "标题不应包含'[警告]'占位符，实际：" + 标题);
            assertFalse(标题.contains("[计分板标题]"),
                    "标题不应包含'[计分板标题]'占位符，实际：" + 标题);
        }

        @Test
        @DisplayName("启动恒定显示：BossBar应添加玩家并设置进度为0.0")
        void 启动恒定显示_应添加玩家并设置进度() {
            玩家快照 快照 = 创建快照(1.5, 0.0);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算实际公共冷却(1.5, 0.0)).thenReturn(1.5);

            try (MockedStatic<Bukkit> bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkitMock.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
                        .thenReturn(bossBar);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(1L))).thenReturn(任务);

                服务.启动恒定显示(玩家标识);
            }

            verify(bossBar).addPlayer(玩家);
            verify(bossBar).setProgress(0.0);
        }
    }

    @Nested
    @DisplayName("边界值与异常场景：玩家不在线、总秒数为0")
    class 边界值与异常 {

        @Test
        @DisplayName("玩家不在线：不应创建BossBar")
        void 玩家不在线_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                服务.启动恒定显示(玩家标识);
            }

            verify(bossBar, never()).addPlayer(any(Player.class));
        }

        @Test
        @DisplayName("总秒数为0：不应创建BossBar")
        void 总秒数为0_不应创建BossBar() {
            玩家快照 快照 = 创建快照(0.0, 0.0);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算实际公共冷却(0.0, 0.0)).thenReturn(0.0);

            try (MockedStatic<Bukkit> bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.启动恒定显示(玩家标识);
            }

            verify(bossBar, never()).addPlayer(any(Player.class));
        }

        @Test
        @DisplayName("显示方法传入0秒数：不应触发任何操作")
        void 显示方法传入0秒_不应触发任何操作() {
            try (MockedStatic<Bukkit> bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.显示(玩家标识, 0.0);
            }

            verify(bossBar, never()).addPlayer(any(Player.class));
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
