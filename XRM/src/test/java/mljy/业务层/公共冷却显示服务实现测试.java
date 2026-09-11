package mljy.业务层;

import mljy.玩家服务;
import mljy.翻译服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("公共冷却显示服务实现")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 公共冷却显示服务实现测试 {
    @Mock
    private JavaPlugin 插件;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 关键词解析器 关键词解析器;
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

    private 公共冷却显示服务实现 服务;
    private UUID 玩家标识;

    private static final String 红色键 = "公CD显示管理器.标题.红色阶段";
    private static final String 黄色键 = "公CD显示管理器.标题.黄色阶段";
    private static final String 绿色键 = "公CD显示管理器.标题.绿色阶段";
    private static final String 空闲键 = "公CD显示管理器.标题.空闲阶段";

    @BeforeEach
    void setUp() {
        服务 = new 公共冷却显示服务实现(插件, 翻译服务, 关键词解析器, 玩家服务, 技能冷却服务, 属性计算服务);
        玩家标识 = UUID.randomUUID();
        when(玩家.isOnline()).thenReturn(true);
        when(翻译服务.获取(红色键)).thenReturn("<dark_gray>GCD</dark_gray> <white>-</white> <red>%.1fs</red><white>/</white><dark_gray>%.1fs</dark_gray>");
        when(翻译服务.获取(黄色键)).thenReturn("<dark_gray>GCD</dark_gray> <white>-</white> <yellow>%.1fs</yellow><white>/</white><dark_gray>%.1fs</dark_gray>");
        when(翻译服务.获取(绿色键)).thenReturn("<dark_gray>GCD</dark_gray> <white>-</white> <green>%.1fs</green><white>/</white><dark_gray>%.1fs</dark_gray>");
        when(翻译服务.获取(空闲键)).thenReturn("<dark_gray>GCD</dark_gray> <white>-</white> <white>%.1fs</white><white>/</white><dark_gray>%.1fs</dark_gray>");
        when(关键词解析器.解析(anyString(), eq(玩家标识))).thenAnswer(调用 -> (String) 调用.getArgument(0));
        when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
        when(属性计算服务.计算实际公共冷却(anyDouble(), anyDouble())).thenAnswer(调用 -> (double) 调用.getArgument(0));
    }

    private 属性快照 创建属性快照(double 公共冷却时间) {
        return 属性快照.创建(
                100.0, 公共冷却时间, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private 属性快照 创建属性快照(double 公共冷却时间, double 急速) {
        return 属性快照.创建(
                100.0, 公共冷却时间, 急速,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private 玩家快照 创建玩家快照(double 公共冷却时间) {
        return new 玩家快照(玩家标识, "测试玩家", 1, null, 创建属性快照(公共冷却时间), Map.of(), null);
    }

    private 玩家快照 创建玩家快照(double 公共冷却时间, double 急速) {
        return new 玩家快照(玩家标识, "测试玩家", 1, null, 创建属性快照(公共冷却时间, 急速), Map.of(), null);
    }

    private Runnable 捕获刷新任务() {
        ArgumentCaptor<Runnable> 捕获器 = ArgumentCaptor.forClass(Runnable.class);
        verify(调度器).runTaskTimer(eq(插件), 捕获器.capture(), eq(0L), eq(1L));
        return 捕获器.getValue();
    }

    private String 捕获最终标题() {
        ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
        verify(bossBar, atLeastOnce()).setTitle(捕获器.capture());
        return 捕获器.getValue();
    }

    @Nested
    @DisplayName("启动恒定显示")
    class 启动恒定显示 {
        @Test
        @DisplayName("在线玩家启动 - 应创建BossBar并启动每tick刷新任务")
        void 在线玩家启动_应创建BossBar并启动刷新任务() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));

                服务.启动恒定显示(玩家标识);

                verify(bossBar).setProgress(0.0);
                verify(bossBar).addPlayer(玩家);
                verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(1L));
            }
        }

        @Test
        @DisplayName("玩家标识为空 - 不应创建BossBar")
        void 玩家标识为空_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                服务.启动恒定显示(null);
                bukkit.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("玩家离线 - 不应创建BossBar")
        void 玩家离线_不应创建BossBar() {
            when(玩家.isOnline()).thenReturn(false);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.启动恒定显示(玩家标识);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)), never());
                verify(调度器, never()).runTaskTimer(any(JavaPlugin.class), any(Runnable.class), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("总秒数为0 - 不应创建BossBar")
        void 总秒数为0_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(0.0)));

                服务.启动恒定显示(玩家标识);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)), never());
                verify(调度器, never()).runTaskTimer(any(JavaPlugin.class), any(Runnable.class), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("玩家快照不存在 - 不应创建BossBar")
        void 玩家快照不存在_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());

                服务.启动恒定显示(玩家标识);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)), never());
            }
        }

        @Test
        @DisplayName("重复启动 - 不应重建BossBar")
        void 重复启动_不应重建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));

                服务.启动恒定显示(玩家标识);
                服务.启动恒定显示(玩家标识);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)),
                        times(1));
            }
        }
    }

    @Nested
    @DisplayName("显示（触发倒计时）")
    class 显示触发倒计时 {
        @Test
        @DisplayName("未启动恒定显示时显示 - 应自动启动恒定显示")
        void 未启动恒定显示时显示_应自动启动恒定显示() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));

                服务.显示(玩家标识, 1.0);

                verify(bossBar).addPlayer(玩家);
                verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(1L));
            }
        }

        @Test
        @DisplayName("玩家标识为空 - 不应创建BossBar")
        void 玩家标识为空_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                服务.显示(null, 1.0);
                bukkit.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("公共冷却秒数小于等于零 - 不应创建BossBar")
        void 公共冷却秒数小于等于零_不应创建BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.显示(玩家标识, 0.0);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)), never());
            }
        }

        @Test
        @DisplayName("玩家离线 - 不应创建BossBar")
        void 玩家离线_不应创建BossBar() {
            when(玩家.isOnline()).thenReturn(false);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.显示(玩家标识, 1.0);

                bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)), never());
            }
        }
    }

    @Nested
    @DisplayName("清理")
    class 清理 {
        @Test
        @DisplayName("已启动的BossBar - 应取消任务并移除BossBar")
        void 已启动的BossBar_应取消任务并移除BossBar() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));

                服务.启动恒定显示(玩家标识);
                服务.清理(玩家标识);

                verify(任务).cancel();
                verify(bossBar).removeAll();
            }
        }

        @Test
        @DisplayName("未启动的BossBar - 不应抛出异常")
        void 未启动的BossBar_不应抛出异常() {
            assertDoesNotThrow(() -> 服务.清理(玩家标识));
            verify(任务, never()).cancel();
        }
    }

    @Nested
    @DisplayName("示例场景验证（含模拟渲染输出）")
    class 示例场景验证 {
        private String 启动并捕获标题(double 总秒数, double 剩余秒数) {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(总秒数)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(剩余秒数);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
            return 捕获最终标题();
        }

        @Test
        @DisplayName("示例1: 公CD 1.0s 剩余0s - 空闲态 白色 渲染 §8GCD §f- §f0.0s§f/§81.0s")
        void 示例1_空闲态_白色() {
            String 标题 = 启动并捕获标题(1.0, 0.0);
            assertEquals("§8GCD §f- §f0.0s§f/§81.0s", 标题);
            verify(bossBar).setColor(BarColor.WHITE);
            verify(bossBar, atLeastOnce()).setProgress(0.0);
        }

        @Test
        @DisplayName("示例2: 公CD 1.0s 剩余0.67s 进度0.67>2/3 - 红色 渲染 §8GCD §f- §c0.7s§f/§81.0s")
        void 示例2_红色阶段_0点67秒() {
            String 标题 = 启动并捕获标题(1.0, 0.67);
            assertEquals("§8GCD §f- §c0.7s§f/§81.0s", 标题);
            verify(bossBar).setColor(BarColor.RED);
            verify(bossBar).setProgress(0.67);
        }

        @Test
        @DisplayName("示例3: 公CD 1.0s 剩余0.66s 进度0.66<2/3 - 黄色 渲染 §8GCD §f- §e0.7s§f/§81.0s")
        void 示例3_黄色阶段_0点66秒() {
            String 标题 = 启动并捕获标题(1.0, 0.66);
            assertEquals("§8GCD §f- §e0.7s§f/§81.0s", 标题);
            verify(bossBar).setColor(BarColor.YELLOW);
            verify(bossBar).setProgress(0.66);
        }

        @Test
        @DisplayName("示例4: 公CD 1.0s 剩余0.34s 进度0.34>1/3 - 黄色 渲染 §8GCD §f- §e0.4s§f/§81.0s")
        void 示例4_黄色阶段_0点34秒() {
            String 标题 = 启动并捕获标题(1.0, 0.34);
            assertEquals("§8GCD §f- §e0.4s§f/§81.0s", 标题);
            verify(bossBar).setColor(BarColor.YELLOW);
            verify(bossBar).setProgress(0.34);
        }

        @Test
        @DisplayName("示例5: 公CD 1.0s 剩余0.33s 进度0.33<1/3 - 绿色 渲染 §8GCD §f- §a0.4s§f/§81.0s")
        void 示例5_绿色阶段_0点33秒() {
            String 标题 = 启动并捕获标题(1.0, 0.33);
            assertEquals("§8GCD §f- §a0.4s§f/§81.0s", 标题);
            verify(bossBar).setColor(BarColor.GREEN);
            verify(bossBar).setProgress(0.33);
        }

        @Test
        @DisplayName("示例6: 公CD 0.9s 剩余0.87s 进度0.967>2/3 - 红色 渲染 §8GCD §f- §c0.9s§f/§80.9s")
        void 示例6_红色阶段_0点87秒() {
            String 标题 = 启动并捕获标题(0.9, 0.87);
            assertEquals("§8GCD §f- §c0.9s§f/§80.9s", 标题);
            verify(bossBar).setColor(BarColor.RED);
            ArgumentCaptor<Double> 进度捕获器 = ArgumentCaptor.forClass(Double.class);
            verify(bossBar, atLeastOnce()).setProgress(进度捕获器.capture());
            assertEquals(0.87 / 0.9, 进度捕获器.getValue(), 0.0001);
        }

        @Test
        @DisplayName("示例7: 公CD 0.9s 剩余0.45s 进度0.5 - 黄色 渲染 §8GCD §f- §e0.5s§f/§80.9s")
        void 示例7_黄色阶段_0点45秒() {
            String 标题 = 启动并捕获标题(0.9, 0.45);
            assertEquals("§8GCD §f- §e0.5s§f/§80.9s", 标题);
            verify(bossBar).setColor(BarColor.YELLOW);
            verify(bossBar).setProgress(0.5);
        }

        @Test
        @DisplayName("示例8: 公CD 0.9s 剩余0.15s 进度0.167<1/3 - 绿色 渲染 §8GCD §f- §a0.2s§f/§80.9s")
        void 示例8_绿色阶段_0点15秒() {
            String 标题 = 启动并捕获标题(0.9, 0.15);
            assertEquals("§8GCD §f- §a0.2s§f/§80.9s", 标题);
            verify(bossBar).setColor(BarColor.GREEN);
            ArgumentCaptor<Double> 进度捕获器 = ArgumentCaptor.forClass(Double.class);
            verify(bossBar, atLeastOnce()).setProgress(进度捕获器.capture());
            assertEquals(0.15 / 0.9, 进度捕获器.getValue(), 0.0001);
        }
    }

    @Nested
    @DisplayName("边界值与阶段切换")
    class 边界值与阶段切换 {
        private void 启动并触发更新(double 总秒数, double 剩余秒数) {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(总秒数)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(剩余秒数);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
        }

        @Test
        @DisplayName("剩余=0 - 应显示空闲态（白色空格）")
        void 剩余为零_应显示空闲态() {
            启动并触发更新(1.0, 0.0);
            verify(bossBar).setColor(BarColor.WHITE);
            verify(bossBar, atLeastOnce()).setProgress(0.0);
        }

        @Test
        @DisplayName("公CD 1.0秒 剩余0.95秒（进度0.95>2/3）- 红色阶段")
        void 公CD1秒剩余0点95秒_应显示红色阶段() {
            启动并触发更新(1.0, 0.95);
            verify(bossBar).setColor(BarColor.RED);
            verify(bossBar).setProgress(0.95);
        }

        @Test
        @DisplayName("2/3边界: 公CD 1.0秒 剩余0.67秒（进度0.67>2/3≈0.6667）- 红色阶段")
        void 边界2分之3_0点67秒_应显示红色阶段() {
            启动并触发更新(1.0, 0.67);
            verify(bossBar).setColor(BarColor.RED);
        }

        @Test
        @DisplayName("2/3边界: 公CD 1.0秒 剩余0.66秒（进度0.66<2/3≈0.6667）- 黄色阶段")
        void 边界2分之3_0点66秒_应显示黄色阶段() {
            启动并触发更新(1.0, 0.66);
            verify(bossBar).setColor(BarColor.YELLOW);
        }

        @Test
        @DisplayName("1/3边界: 公CD 1.0秒 剩余0.34秒（进度0.34>1/3≈0.3333）- 黄色阶段")
        void 边界1分之3_0点34秒_应显示黄色阶段() {
            启动并触发更新(1.0, 0.34);
            verify(bossBar).setColor(BarColor.YELLOW);
        }

        @Test
        @DisplayName("1/3边界: 公CD 1.0秒 剩余0.33秒（进度0.33<1/3≈0.3333）- 绿色阶段")
        void 边界1分之3_0点33秒_应显示绿色阶段() {
            启动并触发更新(1.0, 0.33);
            verify(bossBar).setColor(BarColor.GREEN);
        }

        @Test
        @DisplayName("公CD 0.9秒 剩余0.87秒 - 红色阶段，显示文本应为0.9s/0.9s")
        void 公CD0点9秒剩余0点87秒_应显示红色阶段且显示0点9秒() {
            启动并触发更新(0.9, 0.87);
            verify(bossBar).setColor(BarColor.RED);
            assertTrue(捕获最终标题().contains("§c0.9s"));
        }

        @Test
        @DisplayName("更新时总秒数变为0 - 应清理BossBar")
        void 更新时总秒数变为0_应清理BossBar() {
            when(玩家服务.获取快照(玩家标识))
                    .thenReturn(Optional.of(创建玩家快照(1.0)))
                    .thenReturn(Optional.of(创建玩家快照(0.0)));
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);

                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();

                verify(bossBar).removeAll();
            }
        }

        @Test
        @DisplayName("更新时玩家离线 - 应清理BossBar")
        void 更新时玩家离线_应清理BossBar() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);

                服务.启动恒定显示(玩家标识);
                when(玩家.isOnline()).thenReturn(false);
                捕获刷新任务().run();

                verify(bossBar).removeAll();
            }
        }
    }

    @Nested
    @DisplayName("格式化显示秒数（ceil 取整）")
    class 格式化显示秒数 {
        @Test
        @DisplayName("实际0.05秒 - 应显示0.1s（绿色阶段）")
        void 实际0点05秒_应显示0点1秒() {
            启动并触发更新(1.0, 0.05);
            verify(bossBar).setColor(BarColor.GREEN);
            assertEquals("§8GCD §f- §a0.1s§f/§81.0s", 捕获最终标题());
        }

        @Test
        @DisplayName("实际0.10秒 - 应显示0.1s（绿色阶段）")
        void 实际0点10秒_应显示0点1秒() {
            启动并触发更新(1.0, 0.10);
            verify(bossBar).setColor(BarColor.GREEN);
            assertEquals("§8GCD §f- §a0.1s§f/§81.0s", 捕获最终标题());
        }

        @Test
        @DisplayName("实际0.11秒 - 应显示0.2s（绿色阶段）")
        void 实际0点11秒_应显示0点2秒() {
            启动并触发更新(1.0, 0.11);
            verify(bossBar).setColor(BarColor.GREEN);
            assertEquals("§8GCD §f- §a0.2s§f/§81.0s", 捕获最终标题());
        }

        private void 启动并触发更新(double 总秒数, double 剩余秒数) {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(总秒数)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(剩余秒数);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
        }
    }

    @Nested
    @DisplayName("实时刷新总秒数")
    class 实时刷新总秒数 {
        @Test
        @DisplayName("总秒数从1.0变为0.8 - 应在下一tick使用新总秒数计算进度")
        void 总秒数变化_应在下一tick使用新总秒数() {
            when(玩家服务.获取快照(玩家标识))
                    .thenReturn(Optional.of(创建玩家快照(1.0)))
                    .thenReturn(Optional.of(创建玩家快照(1.0)))
                    .thenReturn(Optional.of(创建玩家快照(0.8)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.4);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);

                服务.启动恒定显示(玩家标识);
                Runnable 刷新任务 = 捕获刷新任务();

                刷新任务.run();
                verify(bossBar).setColor(BarColor.YELLOW);
                verify(bossBar).setProgress(0.4);

                刷新任务.run();
                verify(bossBar, times(2)).setColor(BarColor.YELLOW);
                verify(bossBar).setProgress(0.5);
            }
        }
    }

    @Nested
    @DisplayName("急速计算验证（bug1修复：致命风险#8）")
    class 急速计算验证 {
        @Test
        @DisplayName("急速20%基础1.5秒 - 实际GCD应为1.25秒，BossBar总秒数使用计算值")
        void 急速20_基础1点5_实际GCD应为1点25秒() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.5, 20.0)));
            when(属性计算服务.计算实际公共冷却(eq(1.5), eq(20.0))).thenReturn(1.25);
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(1.25);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
            verify(属性计算服务, atLeastOnce()).计算实际公共冷却(eq(1.5), eq(20.0));
            verify(bossBar).setColor(BarColor.RED);
            String 标题 = 捕获最终标题();
            assertTrue(标题.contains("§c1.3s"));
            assertTrue(标题.contains("§81.3s"));
        }

        @Test
        @DisplayName("急速影响进度计算 - 剩余0.5秒实际GCD1.25秒进度0.4应黄色阶段")
        void 急速影响进度计算_剩余0点5秒应黄色阶段() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.5, 20.0)));
            when(属性计算服务.计算实际公共冷却(eq(1.5), eq(20.0))).thenReturn(1.25);
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.5);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
            verify(bossBar).setColor(BarColor.YELLOW);
            verify(bossBar).setProgress(0.4);
        }

        @Test
        @DisplayName("零急速 - 实际GCD应等于基础GCD")
        void 零急速_实际GCD应等于基础GCD() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0, 0.0)));
            when(属性计算服务.计算实际公共冷却(eq(1.0), eq(0.0))).thenReturn(1.0);
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(1.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
            verify(属性计算服务, atLeastOnce()).计算实际公共冷却(eq(1.0), eq(0.0));
            verify(bossBar).setProgress(1.0);
        }
    }

    @Nested
    @DisplayName("tick优化验证（bug2修复：重大风险#7）")
    class tick优化验证 {
        @Test
        @DisplayName("进度变化小于阈值 - 应跳过setProgress调用")
        void 进度变化小于阈值_应跳过setProgress调用() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识))
                    .thenReturn(0.5)
                    .thenReturn(0.495);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                Runnable 刷新任务 = 捕获刷新任务();
                刷新任务.run();
                刷新任务.run();
            }
            verify(bossBar, times(2)).setProgress(anyDouble());
            verify(bossBar, never()).setProgress(0.495);
        }

        @Test
        @DisplayName("进度变化超过阈值 - 应调用setProgress")
        void 进度变化超过阈值_应调用setProgress() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识))
                    .thenReturn(0.5)
                    .thenReturn(0.48);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                Runnable 刷新任务 = 捕获刷新任务();
                刷新任务.run();
                刷新任务.run();
            }
            verify(bossBar, times(3)).setProgress(anyDouble());
            verify(bossBar).setProgress(0.5);
            verify(bossBar).setProgress(0.48);
        }

        @Test
        @DisplayName("空闲态保持 - 进度0.0不应重复调用setProgress")
        void 空闲态保持_进度0点0不应重复调用setProgress() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                Runnable 刷新任务 = 捕获刷新任务();
                刷新任务.run();
                刷新任务.run();
            }
            verify(bossBar, times(1)).setProgress(0.0);
        }
    }

    @Nested
    @DisplayName("String.format异常防护验证（bug3修复：重大风险#8）")
    class 翻译格式化异常防护 {
        @Test
        @DisplayName("翻译模板含百分号 - 应回退为模板原文不抛异常")
        void 翻译模板含百分号_应回退为模板原文不抛异常() {
            String 含百分号模板 = "公CD%显示管理器";
            when(翻译服务.获取(空闲键)).thenReturn(含百分号模板);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                assertDoesNotThrow(() -> {
                    服务.启动恒定显示(玩家标识);
                    捕获刷新任务().run();
                });
            }
            assertTrue(捕获最终标题().contains("公CD%显示管理器"));
        }

        @Test
        @DisplayName("翻译模板格式参数过多 - 应回退为模板原文不抛异常")
        void 翻译模板格式参数过多_应回退为模板原文不抛异常() {
            String 多参数模板 = "%.1fs %.1fs %.1fs";
            when(翻译服务.获取(空闲键)).thenReturn(多参数模板);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                assertDoesNotThrow(() -> {
                    服务.启动恒定显示(玩家标识);
                    捕获刷新任务().run();
                });
            }
            assertTrue(捕获最终标题().contains(多参数模板));
        }

        @Test
        @DisplayName("正常翻译模板 - 应正常格式化不回退")
        void 正常翻译模板_应正常格式化不回退() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(创建玩家快照(1.0)));
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.WHITE), eq(BarStyle.SOLID)))
                        .thenReturn(bossBar);
                bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
                服务.启动恒定显示(玩家标识);
                捕获刷新任务().run();
            }
            assertEquals("§8GCD §f- §f0.0s§f/§81.0s", 捕获最终标题());
        }
    }
}
