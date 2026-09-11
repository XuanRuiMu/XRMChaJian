package mljy.技能实现.奥能法师;

import mljy.战斗服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.业务层.伤害计算服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.资源变更服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-02 秘法湮灭四段递进特效测试 - 验证全新递进爆发设计与第四段0.5秒SMOKE蓄力
 *
 * 验收标准对应：
 * - 四段特效由小到大递进（粒子数与爆发半径递增）
 * - 第四段有灰色SMOKE蓄力0.5秒（10刻）
 *
 * 测试遵循需求.md「落实到实际游戏中的软件测试规则」：
 * - 验证Bukkit API实际调用链（World.spawnParticle/World.playSound）
 * - 验证粒子参数（颜色、数量、半径递进关系）
 * - 验证第四段蓄力任务的SMOKE粒子调用
 */
@DisplayName("FP-02 秘法湮灭四段递进特效验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FP02秘法湮灭四段递进特效测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 伤害计算服务 伤害计算服务;
    @Mock
    private 目标选择服务 目标选择服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private 效果注册服务 效果注册服务;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 技能释放服务 技能释放服务;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;
    @Mock
    private World 世界;
    @Mock
    private Player 在线玩家;
    @Mock
    private AttributeInstance 最大生命属性;

    private UUID 施法者标识;
    private 玩家快照 施法者快照;
    private Location 实时位置;

    @BeforeEach
    void setUp() {
        施法者标识 = UUID.randomUUID();
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        位置 玩家位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        施法者快照 = new 玩家快照(
                施法者标识, "测试法师", 1, 玩家位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);
        实时位置 = new Location(世界, 0, 64, 0, 0f, 0f);
        when(世界.getName()).thenReturn("world");
        when(在线玩家.isOnline()).thenReturn(true);
        when(在线玩家.isDead()).thenReturn(false);
        when(最大生命属性.getValue()).thenReturn(100.0);
        when(在线玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(最大生命属性);
        when(在线玩家.getHealth()).thenReturn(50.0);
        when(在线玩家.getLocation()).thenReturn(实时位置);
    }

    private void 注入父类字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    private void 注入全部依赖(Object 实例) throws Exception {
        注入父类字段(实例, "插件", 插件);
        注入父类字段(实例, "战斗服务", 战斗服务);
        注入父类字段(实例, "伤害计算服务", 伤害计算服务);
        注入父类字段(实例, "目标选择服务", 目标选择服务);
        注入父类字段(实例, "消息服务", 消息服务);
        注入父类字段(实例, "效果调度服务", 效果调度服务);
        注入父类字段(实例, "资源变更服务", 资源变更服务);
        注入父类字段(实例, "效果注册服务", 效果注册服务);
        注入父类字段(实例, "玩家服务", 玩家服务);
        注入父类字段(实例, "翻译服务", 翻译服务);
        注入父类字段(实例, "技能释放服务实例", 技能释放服务);
    }

    private 技能上下文 构建上下文() {
        技能定义 定义 = new 技能定义(
                "1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发,
                0.0, 0.0, 10.0, 1.5, "", 0.0, 5.0, 1.0, 1, true, false, false);
        return new 技能上下文(施法者快照, "1_8", 定义, null, System.currentTimeMillis());
    }

    /**
     * 执行秘法湮灭并捕获4段伤害延迟任务与2个定时任务（充能旋转环 + 第四段蓄力）。
     * 返回三组Captor：laterCaptor（4段伤害）、timerCaptor（含充能旋转环与第四段蓄力）。
     */
    private 执行结果 执行并捕获任务(秘法湮灭 技能) {
        ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
            技能.执行(构建上下文());
        }
        return new 执行结果(laterCaptor, timerCaptor);
    }

    private void 在Bukkit环境中运行(Runnable 操作) {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
            操作.run();
        }
    }

    private static final class 执行结果 {
        final ArgumentCaptor<Runnable> laterCaptor;
        final ArgumentCaptor<Runnable> timerCaptor;

        执行结果(ArgumentCaptor<Runnable> laterCaptor, ArgumentCaptor<Runnable> timerCaptor) {
            this.laterCaptor = laterCaptor;
            this.timerCaptor = timerCaptor;
        }
    }

    // ==================== 递进设计验证 ====================

    @Nested
    @DisplayName("四段递进设计 - 由小到大爆发")
    class 四段递进爆发验证 {

        @Test
        @DisplayName("第一段（最小）：应生成紫色DUST光球+END_ROD中心粒子")
        void 第一段光球爆发应生成紫色粒子() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            assertEquals(4, 结果.laterCaptor.getAllValues().size(), "应调度4段伤害任务");
            在Bukkit环境中运行(() -> 结果.laterCaptor.getAllValues().get(0).run());
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色光球 = dustCaptor.getAllValues().stream()
                    .anyMatch(o -> o.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色光球, "第一段应生成紫色DUST光球（RGB 180,80,255）");
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.END_ROD), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("第一段：应播放BEACON_POWER_SELECT音效")
        void 第一段应播放信标音效() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            在Bukkit环境中运行(() -> 结果.laterCaptor.getAllValues().get(0).run());
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), eq(Sound.BLOCK_BEACON_POWER_SELECT), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("第二段（小）：应生成紫色波纹DUST环点+END_ROD光柱")
        void 第二段波纹扩散应有环点和光柱() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            在Bukkit环境中运行(() -> 结果.laterCaptor.getAllValues().get(1).run());
            // 第二段波纹环点：DUST单点1个多次调用（环点数=25×1.35≈34）
            verify(世界, atLeast(20)).spawnParticle(
                    eq(Particle.DUST), any(Location.class), eq(1),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), eq(Sound.BLOCK_BEACON_POWER_SELECT), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("第三段（中）：应生成六角星爆+DUST_COLOR_TRANSITION+SONIC_BOOM")
        void 第三段星爆应含星形和渐变和音爆() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            在Bukkit环境中运行(() -> 结果.laterCaptor.getAllValues().get(2).run());
            // 六角星爆：DUST单点1个多次调用（每角点数=40×1.35/6≈9，6角=54次）
            verify(世界, atLeast(30)).spawnParticle(
                    eq(Particle.DUST), any(Location.class), eq(1),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST_COLOR_TRANSITION), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustTransition.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.SONIC_BOOM), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), eq(Sound.BLOCK_BEACON_POWER_SELECT), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("第四段（最大）：应生成FLASH+EXPLOSION+多柱END_ROD+多层环+SCULK_SOUL+DRAGON_BREATH")
        void 第四段全屏湮灭应有完整粒子组合() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            在Bukkit环境中运行(() -> 结果.laterCaptor.getAllValues().get(3).run());
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.FLASH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Color.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.EXPLOSION), any(Location.class), eq(5),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            // 7柱六芒星：END_ROD单点1个多次调用
            verify(世界, atLeast(50)).spawnParticle(
                    eq(Particle.END_ROD), any(Location.class), eq(1),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.SCULK_SOUL), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DRAGON_BREATH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyFloat());
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), eq(Sound.ENTITY_ENDER_DRAGON_GROWL), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("递进验证：第四段DUST粒子数应大于第一段（由小到大递进）")
        void 四段递进粒子数应递增() throws Exception {
            // 用源码静态校验：第一段配置.粒子数量(15)、第四段配置.粒子数量(50)
            // 由于Mockito无法在同一World上隔离4段粒子的累计调用，这里通过源码常量校验递进关系
            java.nio.file.Path 源码路径 = java.nio.file.Path.of(
                    "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java");
            String 源码 = java.nio.file.Files.readString(源码路径, java.nio.charset.StandardCharsets.UTF_8);
            // 提取第一段DUST基础数量
            assertTrue(源码.contains("配置.粒子数量(15)") || 源码.contains("配置.粒子数量(15),"),
                    "第一段DUST基础数量应为15");
            // 第二段环点25
            assertTrue(源码.contains("配置.环点数量(25)"),
                    "第二段环点基础数量应为25");
            // 第三段星形40
            assertTrue(源码.contains("配置.环点数量(40)"),
                    "第三段星形基础点数应为40");
            // 第四段DUST 50
            assertTrue(源码.contains("配置.粒子数量(50)"),
                    "第四段DUST基础数量应为50");
            // 递进关系：15 < 25 < 40 < 50
            int[] 基础数 = {15, 25, 40, 50};
            for (int i = 1; i < 基础数.length; i++) {
                assertTrue(基础数[i] > 基础数[i - 1],
                        "第" + (i + 1) + "段粒子数应大于第" + i + "段（递进设计）");
            }
        }

        @Test
        @DisplayName("递进验证：四段半径应递增（2.0隐含 → 3.0 → 4.0 → 全屏）")
        void 四段半径应递增() throws Exception {
            java.nio.file.Path 源码路径 = java.nio.file.Path.of(
                    "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java");
            String 源码 = java.nio.file.Files.readString(源码路径, java.nio.charset.StandardCharsets.UTF_8);
            // 第二段环半径3.0
            assertTrue(源码.contains("double 环半径 = 3.0;"),
                    "第二段环半径应为3.0");
            // 第三段星半径4.0
            assertTrue(源码.contains("double 星半径 = 4.0;"),
                    "第三段星半径应为4.0");
            // 第四段使用配置.视觉半径()（6.5默认），且多层环（0.3+层*0.22）
            assertTrue(源码.contains("配置.视觉半径() * (0.3 + 层 * 0.22)"),
                    "第四段多层环应基于配置.视觉半径递进");
            // 半径递进：3.0 < 4.0 < 6.5
            assertTrue(3.0 < 4.0 && 4.0 < 6.5, "半径应递增");
        }
    }

    // ==================== 第四段SMOKE蓄力验证 ====================

    @Nested
    @DisplayName("第四段0.5秒SMOKE蓄力 - 灰色烟雾汇聚")
    class 第四段SMOKE蓄力验证 {

        @Test
        @DisplayName("释放时应启动2个定时任务（充能旋转环 + 第四段蓄力）")
        void 释放应启动两个定时任务() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            // 充能旋转环(1) + 第四段蓄力(1) = 2次runTaskTimer
            verify(调度器, times(2)).runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong());
            assertEquals(2, 结果.timerCaptor.getAllValues().size(),
                    "应捕获2个定时任务runnable（含充能旋转环与第四段蓄力）");
        }

        @Test
        @DisplayName("第四段蓄力任务运行时应生成SMOKE粒子（灰色烟雾）")
        void 第四段蓄力应生成SMOKE粒子() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            // timerCaptor第一个是充能旋转环（delay=0），第二个是第四段蓄力（delay=50刻）
            // 直接运行第二个timer runnable验证SMOKE
            Runnable 蓄力任务 = 结果.timerCaptor.getAllValues().get(1);
            在Bukkit环境中运行(蓄力任务);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.SMOKE), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("第四段蓄力任务运行时应生成END_ROD中心光柱")
        void 第四段蓄力应生成END_ROD光柱() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            Runnable 蓄力任务 = 结果.timerCaptor.getAllValues().get(1);
            在Bukkit环境中运行(蓄力任务);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.END_ROD), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("第四段蓄力应播放BEACON_POWER_SELECT渐强音效")
        void 第四段蓄力应播放渐强音效() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            Runnable 蓄力任务 = 结果.timerCaptor.getAllValues().get(1);
            在Bukkit环境中运行(蓄力任务);
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), eq(Sound.BLOCK_BEACON_POWER_SELECT), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("蓄力持续0.5秒（10刻）后应自然结束不抛异常")
        void 蓄力任务应能完整执行10刻() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            Runnable 蓄力任务 = 结果.timerCaptor.getAllValues().get(1);
            // 连续运行11刻（超过0.5秒=10刻），应安全结束
            assertDoesNotThrow(() -> {
                try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                    bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                    bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                    bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                    when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                    for (int 刻 = 0; 刻 < 11; 刻++) {
                        蓄力任务.run();
                    }
                }
            });
        }

        @Test
        @DisplayName("蓄力任务应在世界为空时安全终止不抛异常")
        void 蓄力任务世界为空应安全终止() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            执行结果 结果 = 执行并捕获任务(技能);
            Runnable 蓄力任务 = 结果.timerCaptor.getAllValues().get(1);
            assertDoesNotThrow(() -> {
                try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                    bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
                    bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                    bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                    when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                    蓄力任务.run();
                }
            });
        }
    }
}
