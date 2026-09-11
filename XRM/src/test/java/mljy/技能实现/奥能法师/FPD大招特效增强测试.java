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
 * FP-D 大招特效增强测试 - 验证奥能冥想/秘法湮灭两个大招（60s/90s冷却）的特效增强
 *
 * 增强点：颜色渐变（紫→蓝→白 DUST_COLOR_TRANSITION）、多层旋转环、扩大光柱/爆发半径、
 * 终极段全屏闪光（FLASH）、大范围碎裂、续燃调度、性能边界、空值安全。
 *
 * 遵循需求.md「落实到实际游戏中的软件测试规则」与 FP05 测试结构：
 * - 验证 Bukkit API 实际调用（World.spawnParticle / World.playSound）
 * - 验证粒子参数（颜色、数量边界）
 * - 验证空值/异常场景不抛异常
 */
@DisplayName("FP-D 奥能法师大招特效增强验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FPD大招特效增强测试 {

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

    private 技能上下文 构建上下文(String 技能标识, String 翻译键, 施法类型 类型) {
        技能定义 定义 = new 技能定义(
                技能标识, 翻译键, 类型,
                0.0, 0.0, 10.0, 1.5, "", 0.0, 5.0, 1.0, 1, true, false, false);
        return new 技能上下文(施法者快照, 技能标识, 定义, null, System.currentTimeMillis());
    }

    // ==================== 奥能冥想（1_7，60s大招） ====================

    @Nested
    @DisplayName("奥能冥想 - 大招特效增强：颜色渐变紫→蓝→白 + 多层旋转环 + 扩大光柱")
    class 奥能冥想测试 {

        private 奥能冥想 创建技能() throws Exception {
            奥能冥想 技能 = new 奥能冥想();
            注入全部依赖(技能);
            return 技能;
        }

        private void 执行奥能冥想(奥能冥想 技能) {
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
        }

        @Test
        @DisplayName("一次性爆发应生成颜色渐变DUST_COLOR_TRANSITION粒子（紫→白）")
        void 一次性爆发应生成颜色渐变粒子() throws Exception {
            奥能冥想 技能 = 创建技能();
            执行奥能冥想(技能);
            ArgumentCaptor<Particle.DustTransition> 渐变Captor = ArgumentCaptor.forClass(Particle.DustTransition.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST_COLOR_TRANSITION), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    渐变Captor.capture());
            boolean 有紫白渐变 = 渐变Captor.getAllValues().stream()
                    .anyMatch(d -> d.getColor().equals(Color.fromRGB(180, 80, 255))
                            && d.getToColor().equals(Color.fromRGB(255, 255, 255)));
            assertTrue(有紫白渐变, "冥想开始爆发应含紫(180,80,255)→白(255,255,255)渐变粒子");
        }

        @Test
        @DisplayName("一次性爆发应保留紫色DUST粒子（冥想主色，回归不破坏）")
        void 一次性爆发应保留紫色DUST粒子() throws Exception {
            奥能冥想 技能 = 创建技能();
            执行奥能冥想(技能);
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色 = dustCaptor.getAllValues().stream()
                    .anyMatch(o -> o.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色, "冥想爆发应保留紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("冥想完成特效应生成大爆发DUST（≥15，FP-02进一步削减至约1/4后）与颜色渐变粒子")
        void 冥想完成特效应生成大爆发与渐变() throws Exception {
            奥能冥想 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            // 执行蓄能完成回调（触发 播放冥想完成特效 一次性大爆发）
            assertFalse(laterCaptor.getAllValues().isEmpty(), "应调度蓄能完成回调");
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                laterCaptor.getValue().run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n >= 15),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST_COLOR_TRANSITION), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustTransition.class));
        }

        @Test
        @DisplayName("冥想完成动画每刻应生成冲击波粒子（FP-02已削减层数）")
        void 冥想完成动画应生成冲击波粒子() throws Exception {
            奥能冥想 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
                laterCaptor.getValue().run();
                // timerCaptor 第0项=开始动画, 第1项=完成动画；在 Bukkit 静态模拟范围内执行完成动画一刻
                assertEquals(2, timerCaptor.getAllValues().size(), "应注册开始与完成两个动画任务");
                timerCaptor.getAllValues().get(1).run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }

        @Test
        @DisplayName("释放应调度开始动画定时任务与蓄能完成延迟任务")
        void 释放应调度动画与蓄能任务() throws Exception {
            奥能冥想 技能 = 创建技能();
            执行奥能冥想(技能);
            verify(调度器, times(1)).runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong());
            verify(调度器, times(1)).runTaskLater(eq(插件), any(Runnable.class), anyLong());
        }

        @Test
        @DisplayName("释放时单次DUST粒子数量不超过250（性能边界）")
        void 单次DUST粒子数量不超过上限() throws Exception {
            奥能冥想 技能 = 创建技能();
            执行奥能冥想(技能);
            ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), countCaptor.capture(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            assertTrue(countCaptor.getAllValues().stream().allMatch(n -> n <= 250),
                    "单次DUST粒子数量不应超过250，避免卡服");
        }

        @Test
        @DisplayName("世界不存在时应安全终止不抛异常")
        void 世界不存在时应安全终止() throws Exception {
            奥能冥想 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                assertDoesNotThrow(() -> 技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发)));
                verify(世界, never()).spawnParticle(
                        any(Particle.class), any(Location.class), anyInt(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            }
        }

        @Test
        @DisplayName("玩家不在线时应安全终止不抛异常")
        void 玩家不在线时应安全终止() throws Exception {
            奥能冥想 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(null);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                assertDoesNotThrow(() -> 技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发)));
                verify(世界, never()).spawnParticle(
                        any(Particle.class), any(Location.class), anyInt(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            }
        }
    }

    // ==================== 秘法湮灭（1_8，90s大招） ====================

    @Nested
    @DisplayName("秘法湮灭 - 大招特效增强：扩大爆发半径 + 多能量柱 + 全屏闪光 + 续燃")
    class 秘法湮灭测试 {

        private 秘法湮灭 创建技能() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            return 技能;
        }

        private void 执行秘法湮灭(秘法湮灭 技能) {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
        }

        @Test
        @DisplayName("释放时应调度4段伤害任务（回归不破坏）")
        void 释放时应调度4段伤害任务() throws Exception {
            秘法湮灭 技能 = 创建技能();
            执行秘法湮灭(技能);
            verify(调度器, times(4)).runTaskLater(eq(插件), any(Runnable.class), anyLong());
        }

        @Test
        @DisplayName("释放时应启动充能旋转环与第四段蓄力定时任务（FP-2：第四段蓄力前置铺垫）")
        void 释放时应启动充能旋转环任务() throws Exception {
            秘法湮灭 技能 = 创建技能();
            执行秘法湮灭(技能);
            // FP-2：第四段爆发前 0.5 秒新增蓄力特效任务，与原充能旋转环任务共同启动
            verify(调度器, times(2)).runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong());
        }

        @Test
        @DisplayName("充能开始应生成加大的天空裂缝与颜色渐变粒子")
        void 充能开始应生成加大天空裂缝() throws Exception {
            秘法湮灭 技能 = 创建技能();
            执行秘法湮灭(技能);
            // 加大天空裂缝：DUST调用次数充足（裂缝110 + 爆发60 + 射线裂痕72）
            verify(世界, atLeast(100)).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST_COLOR_TRANSITION), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustTransition.class));
        }

        @Test
        @DisplayName("终极段爆发应生成全屏FLASH粒子（白光闪屏）")
        void 终极段应生成全屏闪光粒子() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            assertEquals(4, laterCaptor.getAllValues().size(), "应调度4段伤害任务");
            Runnable 终极段 = laterCaptor.getAllValues().get(3);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                终极段.run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.FLASH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Color.class));
        }

        @Test
        @DisplayName("终极段爆发应生成DRAGON_BREATH并调度续燃任务（持续时长）")
        void 终极段应调度续燃任务() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            Runnable 终极段 = laterCaptor.getAllValues().get(3);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                终极段.run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DRAGON_BREATH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyFloat());
            // 充能旋转环(1) + 续燃(1) = 至少2次runTaskTimer
            verify(调度器, atLeast(2)).runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong());
        }

        @Test
        @DisplayName("段序号0爆发应保留紫色DUST粒子（回归不破坏）")
        void 段序号0应保留紫色DUST粒子() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            Runnable 第一段 = laterCaptor.getAllValues().get(0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                第一段.run();
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色 = dustCaptor.getAllValues().stream()
                    .anyMatch(o -> o.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色, "段序号0爆发应保留紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("终极段单次DUST粒子数量不超过250（性能边界）")
        void 终极段单次DUST粒子数量不超过上限() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> laterCaptor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), laterCaptor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            Runnable 终极段 = laterCaptor.getAllValues().get(3);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                终极段.run();
            }
            ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), countCaptor.capture(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            assertTrue(countCaptor.getAllValues().stream().allMatch(n -> n <= 250),
                    "终极段单次DUST粒子数量不应超过250，避免卡服");
        }

        @Test
        @DisplayName("世界不存在时应安全终止不抛异常")
        void 世界不存在时应安全终止() throws Exception {
            秘法湮灭 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                assertDoesNotThrow(() -> 技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发)));
                verify(世界, never()).spawnParticle(
                        any(Particle.class), any(Location.class), anyInt(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            }
        }
    }
}
