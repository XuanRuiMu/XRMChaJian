package mljy.技能实现.奥能法师;

import mljy.翻译服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.资源变更服务;
import mljy.战斗服务;
import mljy.玩家服务;
import mljy.属性服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.效果.效果实例;
import mljy.业务层.属性.修饰器管理器;
import org.bukkit.Bukkit;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-02 音效统一与粒子削减测试。
 * 覆盖：秘法湮灭四段爆发音效统一为 BLOCK_BEACON_POWER_SELECT 且音量严格递增；
 * 奥能冥想释放瞬间爆发粒子减半；奥能冥想效果处理器4层档周身环绕粒子减半。
 * 风格遵循 FP05技能粒子特效测试.java。
 */
@DisplayName("FP-02 秘法湮灭音效统一 / 奥能冥想粒子削减")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FP02秘法湮灭与冥想音效粒子测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 战斗服务 战斗服务;
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
    private 修饰器管理器 修饰器管理器;
    @Mock
    private 属性服务 属性服务;

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

    // ==================== 秘法湮灭：四段爆发音效统一 + 音量递增 ====================

    @Nested
    @DisplayName("秘法湮灭 - 四段伤害爆发音效应统一为信标充能且音量由小到大递增")
    class 秘法湮灭音效统一测试 {

        private 秘法湮灭 创建技能() throws Exception {
            秘法湮灭 技能 = new 秘法湮灭();
            注入全部依赖(技能);
            return 技能;
        }

        @Test
        @DisplayName("四段爆发音效枚举全部为 BLOCK_BEACON_POWER_SELECT 且音量严格递增、音调一致")
        void 四段爆发音效统一且音量递增() throws Exception {
            秘法湮灭 技能 = 创建技能();
            ArgumentCaptor<Runnable> 段任务 = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskLater(eq(插件), 段任务.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_8", "skill.奥能法师.秘法湮灭.name", 施法类型.瞬发));
            }
            List<Runnable> 段列表 = 段任务.getAllValues();
            assertEquals(4, 段列表.size(), "应调度4段伤害任务");

            List<Float> 音量列表 = new ArrayList<>();
            List<Float> 音调列表 = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                clearInvocations(世界);
                ArgumentCaptor<Sound> 音效 = ArgumentCaptor.forClass(Sound.class);
                ArgumentCaptor<Float> 音量 = ArgumentCaptor.forClass(Float.class);
                ArgumentCaptor<Float> 音调 = ArgumentCaptor.forClass(Float.class);
                try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                    bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                    bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                    bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                    段列表.get(i).run();
                }
                verify(世界, times(1)).playSound(any(Location.class), 音效.capture(), 音量.capture(), 音调.capture());
                assertEquals(Sound.BLOCK_BEACON_POWER_SELECT, 音效.getValue(),
                        "第" + (i + 1) + "段爆发音效应为 BLOCK_BEACON_POWER_SELECT");
                音量列表.add(音量.getValue());
                音调列表.add(音调.getValue());
            }

            // 音量由小到大严格递增（0.4 < 0.7 < 1.0 < 1.3 倍，实际值乘配置音量倍率1.25）
            for (int i = 1; i < 4; i++) {
                assertTrue(音量列表.get(i) > 音量列表.get(i - 1),
                        String.format("第%d段音量(%.3f)应大于第%d段(%.3f)",
                                i + 1, 音量列表.get(i), i, 音量列表.get(i - 1)));
            }
            // 音调保持一致（统一为0.6f）
            for (int i = 0; i < 4; i++) {
                assertEquals(0.6f, 音调列表.get(i), 1e-6f, "第" + (i + 1) + "段音调应保持一致(0.6)");
            }
            // 首段与末段绝对量级符合建议序列（0.4*1.25=0.5，1.3*1.25=1.625）
            assertEquals(0.5f, 音量列表.get(0), 1e-6f, "首段音量应为0.4*1.25=0.5");
            assertEquals(1.625f, 音量列表.get(3), 1e-6f, "末段音量应为1.3*1.25=1.625");
        }
    }

    // ==================== 奥能冥想：释放瞬间爆发粒子减半 ====================

    @Nested
    @DisplayName("奥能冥想 - 释放瞬间爆发特效粒子数量应减半")
    class 奥能冥想粒子削减测试 {

        private 奥能冥想 创建技能() throws Exception {
            奥能冥想 技能 = new 奥能冥想();
            注入全部依赖(技能);
            return 技能;
        }

        private Player 准备在线玩家() {
            Player 玩家 = mock(Player.class);
            when(玩家.isOnline()).thenReturn(true);
            when(玩家.isDead()).thenReturn(false);
            AttributeInstance 属性实例 = mock(AttributeInstance.class);
            when(属性实例.getValue()).thenReturn(100.0);
            when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性实例);
            when(玩家.getHealth()).thenReturn(50.0);
            when(玩家.getLocation()).thenReturn(实时位置);
            return 玩家;
        }

        @Test
        @DisplayName("释放初始爆发 DUST 粒子数应为11（基础8×强度1.35，较上一轮25进一步削减至约1/4）")
        void 释放初始爆发DUST减半() throws Exception {
            奥能冥想 技能 = 创建技能();
            Player 玩家 = 准备在线玩家();
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n == 11),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }

        @Test
        @DisplayName("释放完成爆发主 DUST 粒子数应为19（基础14×强度1.35，较上一轮55进一步削减至约1/4）")
        void 释放完成爆发主DUST减半() throws Exception {
            奥能冥想 技能 = 创建技能();
            Player 玩家 = 准备在线玩家();
            when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
            ArgumentCaptor<Runnable> 蓄力任务 = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                when(调度器.runTaskLater(eq(插件), 蓄力任务.capture(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文("1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发));
            }
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
                蓄力任务.getValue().run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n == 19),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }
    }

    // ==================== 奥能冥想效果处理器：4层档周身环绕粒子减半 ====================

    @Nested
    @DisplayName("奥能冥想效果处理器 - 4层档周身环绕粒子应减半")
    class 奥能冥想效果处理器粒子削减测试 {

        private 奥能冥想效果处理器 创建处理器() throws Exception {
            奥能冥想效果处理器 处理器 = new 奥能冥想效果处理器();
            Field 插件字段 = 奥能冥想效果处理器.class.getDeclaredField("插件");
            插件字段.setAccessible(true);
            插件字段.set(处理器, 插件);
            Field 修饰器字段 = 奥能冥想效果处理器.class.getDeclaredField("修饰器管理器");
            修饰器字段.setAccessible(true);
            修饰器字段.set(处理器, 修饰器管理器);
            Field 玩家服务字段 = 奥能冥想效果处理器.class.getDeclaredField("玩家服务");
            玩家服务字段.setAccessible(true);
            玩家服务字段.set(处理器, 玩家服务);
            Field 属性服务字段 = 奥能冥想效果处理器.class.getDeclaredField("属性服务");
            属性服务字段.setAccessible(true);
            属性服务字段.set(处理器, 属性服务);
            return 处理器;
        }

        @Test
        @DisplayName("4层档周身环绕 DUST 粒子数应为7（由8减半为4，强度1.35→7）")
        void 四层档周身环绕DUST减半() throws Exception {
            奥能冥想效果处理器 处理器 = 创建处理器();
            when(玩家服务.获取快照(any(UUID.class))).thenReturn(java.util.Optional.empty());
            ArgumentCaptor<Runnable> 任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), 任务捕获.capture(), anyLong(), anyLong())).thenReturn(任务);
                处理器.on添加(施法者标识, new 效果实例("1_7_2", "1_7", 施法者标识, 4, System.currentTimeMillis()));
            }
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                任务捕获.getValue().run();
            }
            verify(世界, atLeastOnce()).spawnParticle(
                    eq(Particle.DUST), any(Location.class), intThat(n -> n == 7),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                    any(Particle.DustOptions.class));
        }
    }
}
