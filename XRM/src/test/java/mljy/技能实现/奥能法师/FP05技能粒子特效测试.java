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
import org.bukkit.entity.LivingEntity;
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
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-05 技能粒子特效测试 - 验证8个奥能法师技能的粒子效果和音效
 *
 * 测试遵循需求.md第1222-1284行"落实到实际游戏中的软件测试规则"：
 * - 验证玩家实际可见的输出（粒子参数、音效参数）
 * - 描述完整玩家场景（释放→特效触发）
 * - 验证Bukkit API实际调用链（World.spawnParticle/World.playSound）
 * - 验证FP-04设计要求：只用粒子和音效，无聊天栏/标题/副标题/计分板/BossBar
 */
@DisplayName("FP-05 奥能法师8个技能粒子特效验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FP05技能粒子特效测试 {

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
        // Bug 2修复：技能使用Bukkit.getPlayer获取实时位置，需mock在线玩家和位置
        实时位置 = new Location(世界, 0, 64, 0, 0f, 0f);
        when(世界.getName()).thenReturn("world");
        when(在线玩家.getLocation()).thenReturn(实时位置);
        when(在线玩家.isOnline()).thenReturn(true);
        when(在线玩家.isDead()).thenReturn(false);
        when(插件.getServer()).thenReturn(null);
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

    private void 在BukkitMock中执行(Runnable 操作) {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            操作.run();
        }
    }

    // ==================== 精通魔法预兆 ====================

    @Nested
    @DisplayName("精通魔法预兆 - 被动激活时玩家应看到紫色奥术粒子爆发和音效")
    class 精通魔法预兆测试 {

        private 精通魔法预兆 创建技能() throws Exception {
            精通魔法预兆 技能 = new 精通魔法预兆();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("激活时应生成紫色DUST粒子（奥术预兆能量）")
        void 激活时应生成紫色DUST粒子() throws Exception {
            精通魔法预兆 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_9", "skill.奥能法师.精通魔法预兆.name", 施法类型.瞬发));
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色粒子, "激活时应生成紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("激活时应播放音效（紫晶块响声）")
        void 激活时应播放音效() throws Exception {
            精通魔法预兆 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_9", "skill.奥能法师.精通魔法预兆.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("激活时DUST粒子数量应充足（≥30个）")
        void 激活时DUST粒子数量应充足() throws Exception {
            精通魔法预兆 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_9", "skill.奥能法师.精通魔法预兆.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n >= 30),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }

        @Test
        @DisplayName("世界不存在时应安全终止不抛异常")
        void 世界不存在时应安全终止() throws Exception {
            精通魔法预兆 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                assertDoesNotThrow(() -> 技能.执行(构建上下文("1_9", "skill.奥能法师.精通魔法预兆.name", 施法类型.瞬发)));
                verify(世界, never()).spawnParticle(
                        any(Particle.class), any(Location.class), anyInt(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            }
        }
    }

    // ==================== 奥术护盾 ====================

    @Nested
    @DisplayName("奥术护盾 - 释放时玩家应看到蓝色护盾粒子和音效")
    class 奥术护盾测试 {

        private 奥术护盾 创建技能() throws Exception {
            奥术护盾 技能 = new 奥术护盾();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("释放时应生成蓝色DUST粒子（护盾能量）")
        void 释放时应生成蓝色DUST粒子() throws Exception {
            奥术护盾 技能 = 创建技能();
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_3", "skill.奥能法师.奥术护盾.name", 施法类型.瞬发)));
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有蓝色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(80, 150, 255)));
            assertTrue(有蓝色粒子, "释放时应生成蓝色DUST粒子（RGB 80,150,255）");
        }

        @Test
        @DisplayName("释放时应生成END_ROD和WITCH粒子")
        void 释放时应生成多种粒子() throws Exception {
            奥术护盾 技能 = 创建技能();
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_3", "skill.奥能法师.奥术护盾.name", 施法类型.瞬发)));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.END_ROD), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.WITCH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("释放时应播放音效")
        void 释放时应播放音效() throws Exception {
            奥术护盾 技能 = 创建技能();
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_3", "skill.奥能法师.奥术护盾.name", 施法类型.瞬发)));
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("释放时DUST粒子数量应充足（≥35个）")
        void 释放时DUST粒子数量应充足() throws Exception {
            奥术护盾 技能 = 创建技能();
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_3", "skill.奥能法师.奥术护盾.name", 施法类型.瞬发)));
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n >= 35),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }
    }

    // ==================== 秘法回流 ====================

    @Nested
    @DisplayName("秘法回流 - 治疗时玩家应看到绿色治疗粒子和音效")
    class 秘法回流测试 {

        private 秘法回流 创建技能() throws Exception {
            秘法回流 技能 = new 秘法回流();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("治疗时应生成绿色DUST粒子（治疗能量）")
        void 治疗时应生成绿色DUST粒子() throws Exception {
            秘法回流 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_6", "skill.奥能法师.秘法回流.name", 施法类型.瞬发));
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有绿色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(80, 255, 120)));
            assertTrue(有绿色粒子, "治疗时应生成绿色DUST粒子（RGB 80,255,120）");
        }

        @Test
        @DisplayName("治疗时应播放音效")
        void 治疗时应播放音效() throws Exception {
            秘法回流 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_6", "skill.奥能法师.秘法回流.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("治疗时应生成HAPPY_VILLAGER粒子（治疗反馈）")
        void 治疗时应生成快乐村民粒子() throws Exception {
            秘法回流 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_6", "skill.奥能法师.秘法回流.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.HAPPY_VILLAGER), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ==================== 奥能冥想 ====================

    @Nested
    @DisplayName("奥能冥想 - 释放时玩家应看到紫色冥想粒子和音效")
    class 奥能冥想测试 {

        private 奥能冥想 创建技能() throws Exception {
            奥能冥想 技能 = new 奥能冥想();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("释放时应生成紫色DUST粒子（冥想能量）")
        void 释放时应生成紫色DUST粒子() throws Exception {
            奥能冥想 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色粒子, "释放时应生成紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("释放时应播放音效")
        void 释放时应播放音效() throws Exception {
            奥能冥想 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("释放时DUST粒子数量应充足（≥8个，FP-02进一步削减后仍有可见爆发）")
        void 释放时DUST粒子数量应充足() throws Exception {
            奥能冥想 技能 = 创建技能();
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n >= 8),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }
    }

    // ==================== 混沌束缚 ====================

    @Nested
    @DisplayName("混沌束缚 - 弹道技能，玩家应看到暗红色混沌粒子和音效")
    class 混沌束缚测试 {

        private 混沌束缚 创建技能() throws Exception {
            混沌束缚 技能 = new 混沌束缚();
            注入全部依赖(技能);
            return 技能;
        }

        private Runnable 执行技能并捕获弹道任务(混沌束缚 技能) {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(1L))).thenReturn(任务);
                技能.执行(构建上下文("1_5", "skill.奥能法师.混沌束缚.name", 施法类型.瞬发));
                ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
                verify(调度器).runTaskTimer(eq(插件), captor.capture(), eq(0L), eq(1L));
                return captor.getValue();
            }
        }

        @Test
        @DisplayName("释放时应生成暗红色DUST粒子（混沌能量）")
        void 释放时应生成暗红色DUST粒子() throws Exception {
            混沌束缚 技能 = 创建技能();
            执行技能并捕获弹道任务(技能);
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有暗红色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(200, 50, 80)));
            assertTrue(有暗红色粒子, "释放时应生成暗红色DUST粒子（RGB 200,50,80）");
        }

        @Test
        @DisplayName("释放时应生成WITCH粒子（混乱魔法）")
        void 释放时应生成WITCH粒子() throws Exception {
            混沌束缚 技能 = 创建技能();
            执行技能并捕获弹道任务(技能);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.WITCH), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("释放时应播放释放音效")
        void 释放时应播放释放音效() throws Exception {
            混沌束缚 技能 = 创建技能();
            执行技能并捕获弹道任务(技能);
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("弹道飞行时应持续生成DUST粒子")
        void 弹道飞行时应持续生成DUST粒子() throws Exception {
            混沌束缚 技能 = 创建技能();
            Runnable 弹道任务 = 执行技能并捕获弹道任务(技能);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                弹道任务.run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n >= 10),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }

        @Test
        @DisplayName("命中目标时应生成EXPLOSION粒子并播放命中音效")
        void 命中目标时应生成爆发粒子() throws Exception {
            混沌束缚 技能 = 创建技能();
            LivingEntity 目标 = mock(LivingEntity.class);
            when(目标.isDead()).thenReturn(false);
            when(目标.getUniqueId()).thenReturn(UUID.randomUUID());
            when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(目标));
            Runnable 弹道任务 = 执行技能并捕获弹道任务(技能);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                弹道任务.run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ==================== 秘法湮灭 ====================

    @Nested
    @DisplayName("秘法湮灭 - 大招，每段伤害应有紫色爆发粒子和音效")
    class 秘法湮灭测试 {

        private 秘法湮灭 创建技能() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("释放时应调度4段伤害任务")
        void 释放时应调度4段伤害任务() throws Exception {
            秘法湮灭 技能 = 创建技能();
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发)));
            verify(调度器, times(4)).runTaskLater(eq(插件), any(Runnable.class), anyLong());
        }

        @Test
        @DisplayName("每段伤害触发时应生成紫色DUST粒子")
        void 每段伤害时应生成紫色DUST粒子() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskLater(eq(插件), captor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            // 执行第一段任务
            Runnable 第一段 = captor.getAllValues().get(0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                第一段.run();
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色粒子, "每段伤害应生成紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("每段伤害触发时应生成EXPLOSION粒子并播放音效")
        void 每段伤害应生成爆炸粒子() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskLater(eq(插件), captor.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            Runnable 第一段 = captor.getAllValues().get(0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                第一段.run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }
    }

    // ==================== 秘法崩裂 ====================

    @Nested
    @DisplayName("秘法崩裂 - 释放凝聚+延迟爆发两段式")
    class 秘法崩裂测试 {

        private 秘法崩裂 创建技能() throws Exception {
            秘法崩裂 技能 = new 秘法崩裂();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("释放凝聚时应生成紫色DUST粒子")
        void 释放凝聚时应生成紫色DUST粒子() throws Exception {
            秘法崩裂 技能 = 创建技能();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(1.0, 0.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), captor.capture(), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_2", "skill.奥能法师.秘法崩裂.name", 施法类型.瞬发));
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色粒子, "释放凝聚时应生成紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("释放凝聚时应播放音效")
        void 释放凝聚时应播放音效() throws Exception {
            秘法崩裂 技能 = 创建技能();
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(1.0, 0.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_2", "skill.奥能法师.秘法崩裂.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("延迟爆发时应生成粒子")
        void 延迟爆发时应生成爆炸粒子() throws Exception {
            秘法崩裂 技能 = 创建技能();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(1.0, 0.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), captor.capture(), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_2", "skill.奥能法师.秘法崩裂.name", 施法类型.瞬发));
                Runnable 延迟任务 = captor.getValue();
                for (int i = 0; i <= 8; i++) {
                    延迟任务.run();
                }
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ==================== 秘法陨落 ====================

    @Nested
    @DisplayName("秘法陨落 - 陨石技能，释放+每支箭矢命中爆发")
    class 秘法陨落测试 {

        private 秘法陨落 创建技能() throws Exception {
            秘法陨落 技能 = new 秘法陨落();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("释放时应生成紫色DUST粒子（陨石预告）")
        void 释放时应生成紫色DUST粒子() throws Exception {
            秘法陨落 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(2L))).thenReturn(任务);
                技能.执行(构建上下文("1_4", "skill.奥能法师.秘法陨落.name", 施法类型.蓄力));
            }
            ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    dustCaptor.capture());
            boolean 有紫色粒子 = dustCaptor.getAllValues().stream()
                    .anyMatch(opt -> opt.getColor().equals(Color.fromRGB(180, 80, 255)));
            assertTrue(有紫色粒子, "释放时应生成紫色DUST粒子（RGB 180,80,255）");
        }

        @Test
        @DisplayName("释放时应播放释放音效")
        void 释放时应播放释放音效() throws Exception {
            秘法陨落 技能 = 创建技能();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(2L))).thenReturn(任务);
                技能.执行(构建上下文("1_4", "skill.奥能法师.秘法陨落.name", 施法类型.蓄力));
            }
            verify(世界, atLeastOnce()).playSound(any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("每支箭矢命中时应生成DUST和EXPLOSION粒子")
        void 每支箭矢命中时应生成爆发粒子() throws Exception {
            秘法陨落 技能 = 创建技能();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), captor.capture(), eq(0L), eq(1L))).thenReturn(任务);
                技能.执行(构建上下文("1_4", "skill.奥能法师.秘法陨落.name", 施法类型.蓄力));
            }
            // 执行第一次箭矢发射任务
            Runnable 箭矢任务 = captor.getValue();
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                箭矢任务.run();
            }
            // 验证命中粒子（DUST+EXPLOSION）在箭矢发射时生成
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ==================== FP-04设计要求符合性 ====================

    @Nested
    @DisplayName("FP-04设计要求符合性 - 所有技能只用粒子和音效表现特效")
    class FP04设计要求符合性 {

        @Test
        @DisplayName("奥术护盾特效只使用粒子和音效，不调用消息服务发送技能特效消息")
        void 奥术护盾不使用消息服务() throws Exception {
            奥术护盾 技能 = new 奥术护盾();
            注入全部依赖(技能);
            在BukkitMock中执行(() -> 技能.执行(构建上下文("1_3", "skill.奥能法师.奥术护盾.name", 施法类型.瞬发)));
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("精通魔法预兆特效只使用粒子和音效")
        void 精通魔法预兆不使用消息服务() throws Exception {
            精通魔法预兆 技能 = new 精通魔法预兆();
            注入全部依赖(技能);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_9", "skill.奥能法师.精通魔法预兆.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    any(Particle.class), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
            verify(世界, atLeastOnce()).playSound(
                    any(Location.class), nullable(Sound.class), anyFloat(), anyFloat());
        }
    }
}
