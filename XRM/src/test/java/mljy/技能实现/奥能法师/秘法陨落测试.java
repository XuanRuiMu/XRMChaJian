package mljy.技能实现.奥能法师;

import mljy.玩家服务;
import mljy.翻译服务;
import mljy.战斗服务;
import mljy.业务层.伤害计算服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.资源变更服务;
import mljy.领域层.实体;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import mljy.基础设施层.事件日志上下文;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 秘法陨落(1_4) 重写测试 — FP-05（问题1）
 *
 * 逐条对齐奥能法师.md L23 与用户 5 条要求：
 *  1) 伤害在「触发伤害那一刻」按玩家实时属性计算（0.09×当时总智力值），暴击/暴伤亦实时；
 *  2) 每次结算独立计算暴击（每箭各自 roll，互不影响）；
 *  3) 每次结算 30% 概率产 1 点秘能（不设限），走通用秘能入口（应用单体伤害并记录命中，
 *     消耗秘兆=false），秘兆与「获得了秘兆效果」日志由通用链路自动追加，本类不出现秘兆判定；
 *  4) 同一支箭矢不重复伤害同单位，但箭矢之间互相独立（每箭维护自己的已命中集合）；
 *  5) 弹道参数对齐文档：面前水平6格+垂直4格、环形半径1.2~4.0、12支、间隔0.1秒、中心高度垂直下落。
 *
 * 测试通过覆盖受控子类（覆盖应用单体伤害/取实时施法者/概率触发）验证上述行为。
 */
@DisplayName("秘法陨落(1_4)重写验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 秘法陨落测试 {

    @Mock private JavaPlugin 插件;
    @Mock private 战斗服务 战斗服务;
    @Mock private 伤害计算服务 伤害计算服务;
    @Mock private 目标选择服务 目标选择服务;
    @Mock private 消息服务 消息服务;
    @Mock private 效果调度服务 效果调度服务;
    @Mock private 资源变更服务 资源变更服务;
    @Mock private 效果注册服务 效果注册服务;
    @Mock private 玩家服务 玩家服务;
    @Mock private 翻译服务 翻译服务;
    @Mock private 技能释放服务 技能释放服务;
    @Mock private BukkitScheduler 调度器;
    @Mock private BukkitTask 任务;
    @Mock private PluginManager 插件管理器;
    @Mock private Arrow 箭矢;
    @Mock private PlayerQuitEvent 退出事件;
    @Mock private World 世界;
    @Mock private Player 在线玩家;

    private UUID 施法者标识;
    private 玩家快照 施法者快照;
    private Location 实时位置;

    /**
     * 覆盖受控子类：捕获每次伤害结算的 基础伤害/是否暴击/实时施法者/秘能资源量/消耗秘兆，
     * 并允许注入实时智力序列与 30% 概率序列，用于验证需求1/2/3/4。
     */
    private static final class 序列测试技能 extends 秘法陨落 {
        final List<Double> 基础伤害序列 = new ArrayList<>();
        final List<Boolean> 是否暴击序列 = new ArrayList<>();
        final List<Integer> 资源序列 = new ArrayList<>();
        final List<Boolean> 消耗秘兆序列 = new ArrayList<>();
        final List<玩家快照> 施法者序列 = new ArrayList<>();
        int 伤害次数;
        final Queue<Double> 实时智力队列 = new ArrayDeque<>();
        final Queue<Boolean> 概率队列 = new ArrayDeque<>();
        // 模拟通用秘能链路返回「是否获得秘兆」：资源>0 时按队列（默认true）判定，使带秘兆/非秘兆两条资源日志均可在测试中确定性出现
        final Queue<Boolean> 秘兆结果队列 = new ArrayDeque<>();

        @Override
        protected 玩家快照 取实时施法者(玩家快照 缓存施法者) {
            double 智力 = 实时智力队列.isEmpty() ? 缓存施法者.属性().智力() : 实时智力队列.poll();
            // 属性快照.创建 第6个参数为智力（1:生命值上限 2:公共冷却 3:急速 4:力量 5:敏捷 6:智力）
            属性快照 新属性 = 属性快照.创建(100.0, 1.5, 10.0, 50.0, 40.0, 智力,
                    50.0, 40.0, 30.0, 15.0, 100.0, 5.0, 8.0, 12.0, 2.0, 1.0, 1.0);
            return new 玩家快照(缓存施法者.唯一标识(), 缓存施法者.名称(), 缓存施法者.等级(),
                    缓存施法者.位置(), 新属性, 缓存施法者.资源表(), 缓存施法者.战斗状态());
        }

        @Override
        protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害,
                                      boolean 是否暴击, boolean 消耗秘兆) {
            基础伤害序列.add(基础伤害);
            是否暴击序列.add(是否暴击);
            施法者序列.add(施法者);
            伤害次数++;
            return new 伤害结果(7.0, false, false);
        }

        @Override
        protected 伤害结果 应用单体伤害并记录命中(玩家快照 施法者, 实体 目标, double 基础伤害,
                                            boolean 是否暴击, String 技能名翻译键,
                                            int 资源获取量, boolean 消耗秘兆) {
            资源序列.add(资源获取量);
            消耗秘兆序列.add(消耗秘兆);
            return super.应用单体伤害并记录命中(施法者, 目标, 基础伤害, 是否暴击, 技能名翻译键, 资源获取量, 消耗秘兆);
        }

        @Override
        protected boolean 概率触发(double 概率) {
            return 概率队列.isEmpty() ? false : 概率队列.poll();
        }

        @Override
        protected boolean 增加秘能(玩家快照 施法者, double 量, boolean 发射日志) {
            // 模拟通用秘能链路：资源>0 时按队列（默认true）决定「是否获得秘兆」，
            // 证明秘兆日志由通用链路自动追加，而秘法陨落本身不含任何秘兆判定。
            if (量 <= 0) {
                return false;
            }
            return 秘兆结果队列.isEmpty() ? true : 秘兆结果队列.poll();
        }
    }

    private static final class 碰撞测试技能 extends 秘法陨落 {
        UUID 最后命中目标;

        @Override
        protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害,
                                      boolean 是否暴击, boolean 消耗秘兆) {
            最后命中目标 = 目标 == null ? null : 目标.获取唯一标识();
            return new 伤害结果(7.0, false, false);
        }

        @Override
        protected boolean 概率触发(double 概率) {
            return false;
        }
    }

    private static final class 去重测试技能 extends 秘法陨落 {
        int 应用伤害次数;

        @Override
        protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害,
                                      boolean 是否暴击, boolean 消耗秘兆) {
            应用伤害次数++;
            return new 伤害结果(7.0, false, false);
        }

        @Override
        protected boolean 概率触发(double 概率) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        施法者标识 = UUID.randomUUID();
        // 属性快照.创建 第6个参数为智力（1:生命值上限 2:公共冷却 3:急速 4:力量 5:敏捷 6:智力）
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 100.0,
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
                "1_4", "skill.奥能法师.秘法陨落.name", 施法类型.蓄力,
                1.5, 6.0, 6.0, 1.5, "", 0.0, 5.0, 0.0, 1, true, false, false);
        return new 技能上下文(施法者快照, "1_4", 定义, null, System.currentTimeMillis());
    }

    // ==================== FP-05 需求1：伤害在触发那一刻按实时属性计算 ====================

    @Test
    @DisplayName("FP-05 需求1：后续箭矢反映下落期间属性变化（智力100→200 ⇒ 伤害9.0→18.0）")
    void 需求1_实时属性_后续箭矢反映属性变化() throws Exception {
        序列测试技能 技能 = new 序列测试技能();
        注入全部依赖(技能);
        技能.实时智力队列.add(100.0);
        技能.实时智力队列.add(200.0);

        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));

        Arrow 箭矢1 = mock(Arrow.class);
        UUID 箭矢1标识 = UUID.randomUUID();
        Arrow 箭矢2 = mock(Arrow.class);
        UUID 箭矢2标识 = UUID.randomUUID();
        when(箭矢1.getUniqueId()).thenReturn(箭矢1标识);
        when(箭矢2.getUniqueId()).thenReturn(箭矢2标识);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);

        ProjectileHitEvent 事件1 = mock(ProjectileHitEvent.class);
        when(事件1.getEntity()).thenReturn(箭矢1);
        when(事件1.getHitEntity()).thenReturn(命中实体);
        when(事件1.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件2 = mock(ProjectileHitEvent.class);
        when(事件2.getEntity()).thenReturn(箭矢2);
        when(事件2.getHitEntity()).thenReturn(命中实体);
        when(事件2.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢1, 箭矢2);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            Runnable 任务 = 任务捕获器.getValue();
            任务.run();
            Method 命中方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            命中方法.setAccessible(true);
            命中方法.invoke(监听器捕获器.getValue(), 事件1);
            Field 已发射字段 = 监听器捕获器.getValue().getClass().getDeclaredField("已发射");
            已发射字段.setAccessible(true);
            已发射字段.setInt(监听器捕获器.getValue(), 12);
            命中方法.invoke(监听器捕获器.getValue(), 事件2);
        }

        assertEquals(2, 技能.伤害次数, "两支不同箭矢应各自结算一次");
        assertEquals(9.0, 技能.基础伤害序列.get(0), 0.0001, "第1箭应按当时智力100 ⇒ 0.09×100=9.0");
        assertEquals(18.0, 技能.基础伤害序列.get(1), 0.0001, "第2箭应按当时智力200 ⇒ 0.09×200=18.0");
    }

    // ==================== FP-05 需求2：每次结算独立计算暴击 ====================

    @Test
    @DisplayName("FP-05 需求2：每支箭矢结算独立把暴击交给战斗服务判定（是否暴击=false，由引擎按实时属性各自roll）")
    void 需求2_独立暴击_每箭委托引擎判定() throws Exception {
        序列测试技能 技能 = new 序列测试技能();
        注入全部依赖(技能);

        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);

        // 三支不同箭矢，同一目标
        Arrow 箭矢1 = mock(Arrow.class); UUID 箭矢1标识 = UUID.randomUUID();
        Arrow 箭矢2 = mock(Arrow.class); UUID 箭矢2标识 = UUID.randomUUID();
        Arrow 箭矢3 = mock(Arrow.class); UUID 箭矢3标识 = UUID.randomUUID();
        when(箭矢1.getUniqueId()).thenReturn(箭矢1标识);
        when(箭矢2.getUniqueId()).thenReturn(箭矢2标识);
        when(箭矢3.getUniqueId()).thenReturn(箭矢3标识);
        ProjectileHitEvent 事件1 = mock(ProjectileHitEvent.class);
        when(事件1.getEntity()).thenReturn(箭矢1);
        when(事件1.getHitEntity()).thenReturn(命中实体);
        when(事件1.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件2 = mock(ProjectileHitEvent.class);
        when(事件2.getEntity()).thenReturn(箭矢2);
        when(事件2.getHitEntity()).thenReturn(命中实体);
        when(事件2.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件3 = mock(ProjectileHitEvent.class);
        when(事件3.getEntity()).thenReturn(箭矢3);
        when(事件3.getHitEntity()).thenReturn(命中实体);
        when(事件3.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢1, 箭矢2, 箭矢3);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            Runnable 任务 = 任务捕获器.getValue();
            任务.run();
            Method 命中方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            命中方法.setAccessible(true);
            for (ProjectileHitEvent 事件 : new ProjectileHitEvent[]{事件1, 事件2, 事件3}) {
                Field 已发射字段 = 监听器捕获器.getValue().getClass().getDeclaredField("已发射");
                已发射字段.setAccessible(true);
                已发射字段.setInt(监听器捕获器.getValue(), 12);
                命中方法.invoke(监听器捕获器.getValue(), 事件);
            }
        }

        assertEquals(3, 技能.伤害次数, "三支不同箭矢应各结算一次");
        // 每箭都把暴击判定交给引擎（是否暴击=false），引擎按实时属性各自独立 roll
        for (boolean 是否暴击 : 技能.是否暴击序列) {
            assertFalse(是否暴击, "每支箭矢不应强制暴击，暴击由战斗服务按实时属性独立判定");
        }
        assertEquals(3, 技能.是否暴击序列.size());
    }

    // ==================== FP-05 需求3：每次结算 30% 概率产秘能，走通用链路，无秘兆判定 ====================

    @Test
    @DisplayName("FP-05 需求3：30% 概率产1秘能(不设限)，消耗秘兆=false，秘兆日志由通用链路(命中仅资源带秘兆)自动追加")
    void 需求3_秘能通用链路_无秘兆判定() throws Exception {
        序列测试技能 技能 = new 序列测试技能();
        注入全部依赖(技能);
        // 四次命中（事件1~4）的30%触发序列：命中1/2/4获得秘能，命中3不获得 ⇒ 资源序列 [1,1,0,1]
        技能.概率队列.add(true);
        技能.概率队列.add(true);
        技能.概率队列.add(false);
        技能.概率队列.add(true);
        // 资源>0 的三次命中（事件1/2/4）分别：获得秘兆、未获得秘兆、获得秘兆
        // ⇒ 通用链路发出「命中仅资源带秘兆」与「命中仅资源」两条日志，证明秘兆日志由通用链路自动追加
        技能.秘兆结果队列.add(true);
        技能.秘兆结果队列.add(false);
        技能.秘兆结果队列.add(true);

        // 让通用秘能入口可成功获得秘能并触发秘兆
        when(资源变更服务.获取上限(any(), eq(奥能法师技能基础.秘能资源标识))).thenReturn(4.0);
        when(资源变更服务.获取当前值(any(), eq(奥能法师技能基础.秘能资源标识))).thenReturn(0.0);
        when(资源变更服务.增加(any(), eq(奥能法师技能基础.秘能资源标识), anyDouble())).thenReturn(true);

        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);

        Arrow 箭矢1 = mock(Arrow.class); UUID 箭矢1标识 = UUID.randomUUID();
        Arrow 箭矢2 = mock(Arrow.class); UUID 箭矢2标识 = UUID.randomUUID();
        Arrow 箭矢3 = mock(Arrow.class); UUID 箭矢3标识 = UUID.randomUUID();
        Arrow 箭矢4 = mock(Arrow.class); UUID 箭矢4标识 = UUID.randomUUID();
        when(箭矢1.getUniqueId()).thenReturn(箭矢1标识);
        when(箭矢2.getUniqueId()).thenReturn(箭矢2标识);
        when(箭矢3.getUniqueId()).thenReturn(箭矢3标识);
        when(箭矢4.getUniqueId()).thenReturn(箭矢4标识);
        ProjectileHitEvent 事件1 = mock(ProjectileHitEvent.class);
        when(事件1.getEntity()).thenReturn(箭矢1);
        when(事件1.getHitEntity()).thenReturn(命中实体);
        when(事件1.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件2 = mock(ProjectileHitEvent.class);
        when(事件2.getEntity()).thenReturn(箭矢2);
        when(事件2.getHitEntity()).thenReturn(命中实体);
        when(事件2.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件3 = mock(ProjectileHitEvent.class);
        when(事件3.getEntity()).thenReturn(箭矢3);
        when(事件3.getHitEntity()).thenReturn(命中实体);
        when(事件3.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件4 = mock(ProjectileHitEvent.class);
        when(事件4.getEntity()).thenReturn(箭矢4);
        when(事件4.getHitEntity()).thenReturn(命中实体);
        when(事件4.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢1, 箭矢2, 箭矢3, 箭矢4);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            Runnable 任务 = 任务捕获器.getValue();
            任务.run();
            Method 命中方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            命中方法.setAccessible(true);
            for (ProjectileHitEvent 事件 : new ProjectileHitEvent[]{事件1, 事件2, 事件3, 事件4}) {
                Field 已发射字段 = 监听器捕获器.getValue().getClass().getDeclaredField("已发射");
                已发射字段.setAccessible(true);
                已发射字段.setInt(监听器捕获器.getValue(), 12);
                命中方法.invoke(监听器捕获器.getValue(), 事件);
            }
        }

        assertEquals(4, 技能.伤害次数);
        // 三次触发(30%成功)⇒资源量1，一次不触发⇒0；不设限多次触发
        assertEquals(List.of(1, 1, 0, 1), 技能.资源序列, "30%触发则秘能+1，否则0，多次触发不设限");
        // 秘法陨落消耗0秘能，必须传 消耗秘兆=false（不得被秘兆增益/消费）
        for (boolean 消耗 : 技能.消耗秘兆序列) {
            assertFalse(消耗, "秘法陨落必须传 消耗秘兆=false");
        }
        // 秘兆与「获得了秘兆效果」日志由通用链路自动追加（命中仅资源带秘兆），本类不出现秘兆判定
        verify(消息服务, atLeastOnce()).发送技能日志(
                any(玩家快照.class), any(UUID.class), eq(奥能法师技能基础.命中仅资源带秘兆日志键), anyString());
        // 不触发秘能的那次只发命中仅资源（非带秘兆）
        verify(消息服务, atLeastOnce()).发送技能日志(
                any(玩家快照.class), any(UUID.class), eq(奥能法师技能基础.命中仅资源日志键), anyString());
    }

    // ==================== FP-05 需求4：同箭不重复、异箭可独立 ====================

    @Test
    @DisplayName("FP-05 需求4a：同一支箭矢命中同一单位仅造成1次伤害（每箭独立已命中集合）")
    void 需求4a_同箭不重复() throws Exception {
        去重测试技能 技能 = new 去重测试技能();
        注入全部依赖(技能);
        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 20, 68, 20));
        ProjectileHitEvent 命中事件 = mock(ProjectileHitEvent.class);
        when(命中事件.getEntity()).thenReturn(箭矢);
        when(命中事件.getHitEntity()).thenReturn(命中实体);
        when(命中事件.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
            Method 命中方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            命中方法.setAccessible(true);
            命中方法.invoke(监听器捕获器.getValue(), 命中事件);
            命中方法.invoke(监听器捕获器.getValue(), 命中事件);
            命中方法.invoke(监听器捕获器.getValue(), 命中事件);
        }
        assertEquals(1, 技能.应用伤害次数, "同一支箭矢命中同一单位仅1次伤害（每箭独立去重）");
    }

    @Test
    @DisplayName("FP-05 需求4b：不同箭矢命中同一单位可分别造成伤害（箭矢之间互相独立）")
    void 需求4b_异箭独立() throws Exception {
        序列测试技能 技能 = new 序列测试技能();
        注入全部依赖(技能);
        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);

        Arrow 箭矢1 = mock(Arrow.class); UUID 箭矢1标识 = UUID.randomUUID();
        Arrow 箭矢2 = mock(Arrow.class); UUID 箭矢2标识 = UUID.randomUUID();
        when(箭矢1.getUniqueId()).thenReturn(箭矢1标识);
        when(箭矢2.getUniqueId()).thenReturn(箭矢2标识);
        ProjectileHitEvent 事件1 = mock(ProjectileHitEvent.class);
        when(事件1.getEntity()).thenReturn(箭矢1);
        when(事件1.getHitEntity()).thenReturn(命中实体);
        when(事件1.getHitBlock()).thenReturn(null);
        ProjectileHitEvent 事件2 = mock(ProjectileHitEvent.class);
        when(事件2.getEntity()).thenReturn(箭矢2);
        when(事件2.getHitEntity()).thenReturn(命中实体);
        when(事件2.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢1, 箭矢2);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            Runnable 任务 = 任务捕获器.getValue();
            任务.run();
            Method 命中方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            命中方法.setAccessible(true);
            for (ProjectileHitEvent 事件 : new ProjectileHitEvent[]{事件1, 事件2}) {
                Field 已发射字段 = 监听器捕获器.getValue().getClass().getDeclaredField("已发射");
                已发射字段.setAccessible(true);
                已发射字段.setInt(监听器捕获器.getValue(), 12);
                命中方法.invoke(监听器捕获器.getValue(), 事件);
            }
        }
        assertEquals(2, 技能.伤害次数, "不同箭矢命中同一单位应分别造成伤害（箭矢之间独立）");
    }

    // ==================== FP-05 需求5：弹道/生成参数对齐文档 ====================

    @Test
    @DisplayName("FP-05 需求5：箭矢数量12、水平距离6、垂直高度4、发射间隔0.1秒(2刻)、环形半径1.2~4.0")
    void 需求5_生成参数对齐文档() throws Exception {
        Field 箭矢数量字段 = 秘法陨落.class.getDeclaredField("箭矢数量");
        箭矢数量字段.setAccessible(true);
        assertEquals(12, 箭矢数量字段.get(null), "箭矢数量应为12");

        Field 水平距离字段 = 秘法陨落.class.getDeclaredField("水平距离");
        水平距离字段.setAccessible(true);
        assertEquals(6.0, 水平距离字段.get(null), "落点中心水平距离应为6格");

        Field 垂直高度字段 = 秘法陨落.class.getDeclaredField("垂直高度");
        垂直高度字段.setAccessible(true);
        assertEquals(4.0, 垂直高度字段.get(null), "落点中心垂直高度应为4格");

        Field 发射间隔字段 = 秘法陨落.class.getDeclaredField("发射间隔刻");
        发射间隔字段.setAccessible(true);
        assertEquals(2L, 发射间隔字段.get(null), "发射间隔应为2刻=0.1秒");

        Field 半径字段 = 秘法陨落.class.getDeclaredField("半径");
        半径字段.setAccessible(true);
        assertEquals(4.0, 半径字段.get(null), "环形半径上限应为4.0");
    }

    @Test
    @DisplayName("FP-05 需求5：生成箭矢点位散布在距中心1.2~4.0格的环形范围")
    void 需求5_环形半径范围() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        位置 中心 = new 位置("world", 0, 64, 0, 0f, 0f);
        Method 方法 = 秘法陨落.class.getDeclaredMethod("生成箭矢点位", 位置.class);
        方法.setAccessible(true);
        double 半径值 = 4.0;
        double 最小允许 = 1.2;
        double 最大允许 = 4.0;
        for (int i = 0; i < 200; i++) {
            @SuppressWarnings("unchecked")
            List<位置> 点位列表 = (List<位置>) 方法.invoke(技能, 中心);
            for (位置 点位 : 点位列表) {
                double 距离 = Math.sqrt(点位.x() * 点位.x() + 点位.z() * 点位.z());
                assertTrue(距离 >= 最小允许 - 0.0001, "距中心应≥1.2格，实际=" + 距离);
                assertTrue(距离 <= 最大允许 + 0.0001, "距中心应≤4.0格，实际=" + 距离);
            }
        }
    }

    @Test
    @DisplayName("FP-05 需求5：12支箭矢按0.1秒(2刻)间隔依次发射，且总数封顶12")
    void 需求5_发射间隔与封顶() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(false);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 68, 0));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            技能.执行(构建上下文());
            Runnable 任务 = 捕获任务();
            // tick0 发射第1支；tick1（1%2!=0）不发射；tick2 发射第2支
            任务.run();
            verify(世界, times(1)).spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat());
            任务.run();
            verify(世界, times(1)).spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat());
            任务.run();
            verify(世界, times(2)).spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat());
            // 运行足够多刻，发射应封顶12支（不再新增）
            for (int i = 0; i < 30; i++) {
                任务.run();
            }
            verify(世界, times(12)).spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat());
        }
    }

    @SuppressWarnings("unchecked")
    private Runnable 捕获任务() throws Exception {
        // 重新执行以获取任务（简化：直接通过调度器捕获）
        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        verify(调度器).runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong());
        return 任务捕获器.getValue();
    }

    // ==================== 既有特效/边界/扫掠测试（保持 FP-01/FP-02 行为） ====================

    @Test
    @DisplayName("箭矢下落期间应生成紫色气泡DUST粒子（颜色160,80,255）")
    void 箭矢下落期间生成紫色气泡粒子() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        UUID 箭矢标识 = UUID.randomUUID();
        Location 箭矢位置 = new Location(世界, 0, 68, 0);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(箭矢位置);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            技能.执行(构建上下文());
            timerCaptor.getValue().run();
        }
        ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
        verify(世界, atLeast(8)).spawnParticle(
                eq(Particle.DUST), any(Location.class), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                dustCaptor.capture());
        boolean 有紫色气泡 = dustCaptor.getAllValues().stream()
                .anyMatch(o -> o.getColor().equals(Color.fromRGB(160, 80, 255)));
        assertTrue(有紫色气泡, "箭矢下落应生成紫色气泡DUST粒子（RGB 160,80,255）");
        verify(世界, atLeastOnce()).spawnParticle(
                eq(Particle.BUBBLE_POP), any(Location.class), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("箭矢参数应使用真实下落方向并禁止拾取")
    void 箭矢发射_参数应为真实下落箭矢() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Location> 位置捕获器 = ArgumentCaptor.forClass(Location.class);
        ArgumentCaptor<Vector> 方向捕获器 = ArgumentCaptor.forClass(Vector.class);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 68, 0));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(位置捕获器.capture(), 方向捕获器.capture(), eq(3.0f), eq(0.0f))).thenReturn(箭矢);
            技能.执行(构建上下文());
            timerCaptor.getValue().run();
        }
        assertEquals(new Vector(0, -1, 0), 方向捕获器.getValue());
        assertEquals(68.0, 位置捕获器.getValue().getY(), 0.0001,
                "箭矢生成Y应为玩家Y+4（中心.y）");
        verify(箭矢).setGravity(true);
        verify(箭矢).setDamage(0.0);
        verify(箭矢).setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        verify(箭矢).setCritical(false);
        verify(箭矢).setPierceLevel((byte) 127);
    }

    @Test
    @DisplayName("玩家退出时应取消任务、移除箭矢并注销监听器")
    void 玩家退出_应清理施法生命周期() throws Exception {
        碰撞测试技能 技能 = new 碰撞测试技能();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        UUID 箭矢标识 = UUID.randomUUID();
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 72, 0));
        when(退出事件.getPlayer()).thenReturn(在线玩家);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
             MockedStatic<HandlerList> handlerListMock = mockStatic(HandlerList.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            timerCaptor.getValue().run();
            Listener 监听器 = 监听器捕获器.getValue();
            Method 方法 = 监听器.getClass().getDeclaredMethod("玩家退出", PlayerQuitEvent.class);
            方法.setAccessible(true);
            方法.invoke(监听器, 退出事件);
            方法.invoke(监听器, 退出事件);
            verify(任务, times(1)).cancel();
            verify(箭矢, times(1)).remove();
            handlerListMock.verify(() -> HandlerList.unregisterAll(监听器), times(1));
            verifyNoInteractions(消息服务);
        }
    }

    @Test
    @DisplayName("FP-01: 箭矢命中敌方LivingEntity时造成1次伤害并记录目标（碰撞触发，不再用AOE）")
    void 命中实体_使用实际碰撞实体() throws Exception {
        碰撞测试技能 技能 = new 碰撞测试技能();
        注入全部依赖(技能);
        LivingEntity 命中实体 = mock(LivingEntity.class);
        UUID 目标标识 = UUID.randomUUID();
        when(命中实体.getUniqueId()).thenReturn(目标标识);
        when(命中实体.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(命中实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 20, 68, 20));
        ProjectileHitEvent 命中事件 = mock(ProjectileHitEvent.class);
        when(命中事件.getEntity()).thenReturn(箭矢);
        when(命中事件.getHitEntity()).thenReturn(命中实体);
        when(命中事件.getHitBlock()).thenReturn(null);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
            Method 方法 = 监听器捕获器.getValue().getClass().getDeclaredMethod("箭矢命中", ProjectileHitEvent.class);
            方法.setAccessible(true);
            方法.invoke(监听器捕获器.getValue(), 命中事件);
        }

        assertEquals(目标标识, 技能.最后命中目标, "FP-01: 箭矢命中敌方应造成伤害并记录目标");
        verify(箭矢, never()).remove();
        verify(目标选择服务, never()).选择敌人(any(), any(), anyDouble());
    }

    private LivingEntity 构建邻近怪物(UUID 标识, BoundingBox 碰撞箱) {
        LivingEntity 怪物 = mock(LivingEntity.class);
        when(怪物.getUniqueId()).thenReturn(标识);
        when(怪物.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(怪物.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(怪物.getBoundingBox()).thenReturn(碰撞箱);
        when(怪物.getLocation()).thenReturn(new Location(世界, 0, 70, 0));
        return 怪物;
    }

    @Test
    @DisplayName("FP-01扫掠：怪物碰撞箱包含箭矢当前位置时能够结算伤害")
    void 扫掠_碰撞箱包含箭矢位置_结算伤害() throws Exception {
        碰撞测试技能 技能 = new 碰撞测试技能();
        注入全部依赖(技能);
        UUID 箭矢标识 = UUID.randomUUID();
        UUID 目标标识 = UUID.randomUUID();
        Location 箭矢位置 = new Location(世界, 0, 70, 0);
        BoundingBox 碰撞箱 = new BoundingBox(-0.4, 69.5, -0.4, 0.4, 70.5, 0.4);
        LivingEntity 怪物 = 构建邻近怪物(目标标识, 碰撞箱);
        Collection<Entity> 邻近 = new ArrayList<>();
        邻近.add(怪物);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(箭矢位置);
        when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(邻近);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
        }
        assertEquals(目标标识, 技能.最后命中目标, "FP-01扫掠：碰撞箱包含箭矢位置应结算伤害");
        verify(目标选择服务, never()).选择敌人(any(), any(), anyDouble());
        verify(箭矢, never()).remove();
    }

    @Test
    @DisplayName("FP-01扫掠：同一目标多tick扫掠只结算1次（每箭独立去重）")
    void 扫掠_同一目标多tick只结算一次() throws Exception {
        去重测试技能 技能 = new 去重测试技能();
        注入全部依赖(技能);
        UUID 箭矢标识 = UUID.randomUUID();
        UUID 目标标识 = UUID.randomUUID();
        Location 箭矢位置 = new Location(世界, 0, 70, 0);
        BoundingBox 碰撞箱 = new BoundingBox(-0.4, 69.5, -0.4, 0.4, 70.5, 0.4);
        LivingEntity 怪物 = 构建邻近怪物(目标标识, 碰撞箱);
        Collection<Entity> 邻近 = new ArrayList<>();
        邻近.add(怪物);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(箭矢位置);
        when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(邻近);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            Runnable 任务 = 任务捕获器.getValue();
            任务.run();
            任务.run();
            任务.run();
        }
        assertEquals(1, 技能.应用伤害次数, "FP-01扫掠：同一目标多tick只结算1次（每箭独立去重）");
    }

    @Test
    @DisplayName("FP-01扫掠：施法者自身在范围内不受伤害")
    void 扫掠_施法者自身不受伤害() throws Exception {
        去重测试技能 技能 = new 去重测试技能();
        注入全部依赖(技能);
        UUID 箭矢标识 = UUID.randomUUID();
        Location 箭矢位置 = new Location(世界, 0, 64, 0);
        BoundingBox 碰撞箱 = new BoundingBox(-0.4, 63.5, -0.4, 0.4, 64.5, 0.4);
        Collection<Entity> 邻近 = new ArrayList<>();
        邻近.add(在线玩家);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(在线玩家.getBoundingBox()).thenReturn(碰撞箱);
        when(在线玩家.getLocation()).thenReturn(箭矢位置);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(箭矢位置);
        when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(邻近);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
        }
        assertEquals(0, 技能.应用伤害次数, "FP-01扫掠：施法者自身在范围内不受伤害");
    }

    @Test
    @DisplayName("FP-01扫掠：箭矢已触地则不再扫掠")
    void 扫掠_触地箭矢不再扫掠() throws Exception {
        去重测试技能 技能 = new 去重测试技能();
        注入全部依赖(技能);
        UUID 箭矢标识 = UUID.randomUUID();
        UUID 目标标识 = UUID.randomUUID();
        Location 箭矢位置 = new Location(世界, 0, 64, 0);
        BoundingBox 碰撞箱 = new BoundingBox(-0.4, 63.5, -0.4, 0.4, 64.5, 0.4);
        LivingEntity 怪物 = 构建邻近怪物(目标标识, 碰撞箱);
        Collection<Entity> 邻近 = new ArrayList<>();
        邻近.add(怪物);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(箭矢标识);
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(true);
        when(箭矢.isOnGround()).thenReturn(true);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(箭矢位置);
        when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(邻近);

        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Listener> 监听器捕获器 = ArgumentCaptor.forClass(Listener.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(监听器捕获器.capture(), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
        }
        assertEquals(0, 技能.应用伤害次数, "FP-01扫掠：箭矢触地后不再扫掠结算伤害");
    }

    @Test
    @DisplayName("FP-01: 箭矢数量应为12")
    void 箭矢数量_应为12() throws Exception {
        Field 字段 = 秘法陨落.class.getDeclaredField("箭矢数量");
        字段.setAccessible(true);
        int 值 = (int) 字段.get(null);
        assertEquals(12, 值, "FP-01: 箭矢数量应为12");
    }

    @Test
    @DisplayName("FP-01: 半圆圆弧左右两线同步推进，12进度点24粒子，起止点重合")
    void 圆环粒子_半圆圆弧左右两线同步推进() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);

        位置 中心 = new 位置("world", 0, 68, 6, 0f, 0f);
        List<位置> 箭矢目标点位 = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            箭矢目标点位.add(中心);
        }

        Class<?> 施法状态类 = Class.forName("mljy.技能实现.奥能法师.秘法陨落$施法状态");
        java.lang.reflect.Constructor<?> 构造器 = 施法状态类.getDeclaredConstructor(
                秘法陨落.class, 玩家快照.class, String.class, List.class, UUID.class, UUID.class,
                double.class, 位置.class);
        构造器.setAccessible(true);

        double 起始角度 = -Math.PI / 2;
        Object 监听器 = 构造器.newInstance(技能, 施法者快照, "world", 箭矢目标点位,
                 UUID.randomUUID(), UUID.randomUUID(), 起始角度, 中心);

        ArgumentCaptor<Location> 位置捕获器 = ArgumentCaptor.forClass(Location.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);

            Method 方法 = 施法状态类.getDeclaredMethod("绘制圆环批次", boolean.class);
            方法.setAccessible(true);
            方法.invoke(监听器, true);
        }

        ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
        verify(世界, atLeast(24)).spawnParticle(
                eq(Particle.DUST), 位置捕获器.capture(), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                dustCaptor.capture());

        List<Location> 圆环位置 = 位置捕获器.getAllValues();
        assertEquals(24, 圆环位置.size(), "FP-01: 圆弧应有24个DUST粒子（12进度×左右两线）");
        assertEquals(24, dustCaptor.getAllValues().size(), "FP-01: 圆弧DUST粒子颜色数应=24");

        double 起始对距离 = 圆环位置.get(0).distance(圆环位置.get(1));
        assertTrue(起始对距离 < 0.1, "FP-01: 第1对点（进度0左右线近点）位置应接近");
        double 终止对距离 = 圆环位置.get(22).distance(圆环位置.get(23));
        assertTrue(终止对距离 < 0.1, "FP-01: 最后1对点（进度11左右线远点）位置应接近");
        Location 起始点 = 圆环位置.get(0);
        Location 终点 = 圆环位置.get(22);
        double 起始终点距离 = 起始点.distance(终点);
        assertEquals(2 * 4.0, 起始终点距离, 0.5, "FP-01: 起始点和终点距离应接近直径（8.0）");
        assertTrue(起始终点距离 > 1.0, "FP-01: 起始点和终点不应重合");
    }

    @Test
    @DisplayName("FP-01: 半圆圆弧粒子颜色从浅到深单调渐变（近点浅色→远点深色）")
    void 圆环粒子_颜色从浅到深单调渐变() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);

        位置 中心 = new 位置("world", 0, 68, 6, 0f, 0f);
        List<位置> 箭矢目标点位 = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            箭矢目标点位.add(中心);
        }

        Class<?> 施法状态类 = Class.forName("mljy.技能实现.奥能法师.秘法陨落$施法状态");
        java.lang.reflect.Constructor<?> 构造器 = 施法状态类.getDeclaredConstructor(
                秘法陨落.class, 玩家快照.class, String.class, List.class, UUID.class, UUID.class,
                double.class, 位置.class);
        构造器.setAccessible(true);

        double 起始角度 = -Math.PI / 2;
        Object 监听器 = 构造器.newInstance(技能, 施法者快照, "world", 箭矢目标点位,
                UUID.randomUUID(), UUID.randomUUID(), 起始角度, 中心);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);

            Method 方法 = 施法状态类.getDeclaredMethod("绘制圆环批次", boolean.class);
            方法.setAccessible(true);
            方法.invoke(监听器, true);
        }

        ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
        verify(世界, atLeast(24)).spawnParticle(
                eq(Particle.DUST), any(Location.class), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                dustCaptor.capture());

        List<Color> 圆环颜色 = new ArrayList<>();
        for (Particle.DustOptions 选项 : dustCaptor.getAllValues()) {
            圆环颜色.add(选项.getColor());
        }
        assertEquals(24, 圆环颜色.size(), "FP-01: 圆弧应有24个DUST粒子");

        Color 浅色 = Color.fromRGB(200, 150, 255);
        Color 深色 = Color.fromRGB(80, 0, 120);
        assertEquals(浅色, 圆环颜色.get(0), "FP-01: 第1个点（进度0左线近点）应为浅色");
        assertEquals(浅色, 圆环颜色.get(1), "FP-01: 第2个点（进度0右线近点）应为浅色");
        assertEquals(深色, 圆环颜色.get(22), "FP-01: 第23个点（进度11左线远点）应为深色");
        assertEquals(深色, 圆环颜色.get(23), "FP-01: 第24个点（进度11右线远点）应为深色");

        for (int i = 2; i < 24; i += 2) {
            Color 前一对颜色 = 圆环颜色.get(i - 2);
            Color 当前对颜色 = 圆环颜色.get(i);
            int 前一对总和 = 前一对颜色.getRed() + 前一对颜色.getGreen() + 前一对颜色.getBlue();
            int 当前对总和 = 当前对颜色.getRed() + 当前对颜色.getGreen() + 当前对颜色.getBlue();
            assertTrue(当前对总和 <= 前一对总和,
                    "FP-01: 颜色应单调从浅到深变化（第" + (i / 2) + "对应不浅于前一对）");
        }
    }

    @Test
    @DisplayName("FP-01: 清理时强制绘制剩余圆弧进度点（兜底处理箭矢发射未完成的边界）")
    void 清理时_强制绘制剩余圆环点() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);

        位置 中心 = new 位置("world", 0, 68, 6, 0f, 0f);
        List<位置> 箭矢目标点位 = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            箭矢目标点位.add(中心);
        }

        Class<?> 施法状态类 = Class.forName("mljy.技能实现.奥能法师.秘法陨落$施法状态");
        java.lang.reflect.Constructor<?> 构造器 = 施法状态类.getDeclaredConstructor(
                秘法陨落.class, 玩家快照.class, String.class, List.class, UUID.class, UUID.class,
                double.class, 位置.class);
        构造器.setAccessible(true);

        double 起始角度 = -Math.PI / 2;
        Object 监听器 = 构造器.newInstance(技能, 施法者快照, "world", 箭矢目标点位,
                UUID.randomUUID(), UUID.randomUUID(), 起始角度, 中心);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);

            Method 方法 = 施法状态类.getDeclaredMethod("清理", boolean.class);
            方法.setAccessible(true);
            方法.invoke(监听器, true);
        }

        ArgumentCaptor<Particle.DustOptions> dustCaptor = ArgumentCaptor.forClass(Particle.DustOptions.class);
        verify(世界, atLeast(24)).spawnParticle(
                eq(Particle.DUST), any(Location.class), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                dustCaptor.capture());

        assertEquals(24, dustCaptor.getAllValues().size(), "FP-01: 清理兜底应强制绘制全部24个圆弧DUST粒子");
    }

    @Test
    @DisplayName("FP-02: 箭矢生成位置Y应为玩家Y+4（中心.y）")
    void 箭矢生成位置_应为玩家Y加4() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Location> 位置捕获器 = ArgumentCaptor.forClass(Location.class);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 68, 0));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(位置捕获器.capture(), any(Vector.class), eq(3.0f), eq(0.0f))).thenReturn(箭矢);
            技能.执行(构建上下文());
            timerCaptor.getValue().run();
        }
        assertEquals(68.0, 位置捕获器.getValue().getY(), 0.0001,
                "FP-2修复：箭矢生成位置Y应为玩家Y+4（68）");
    }

    @Test
    @DisplayName("FP-02: 箭矢触地后应停止运动（setVelocity零向量 + setGravity false）")
    void 箭矢触地_应停止运动() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Vector> 速度捕获器 = ArgumentCaptor.forClass(Vector.class);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(true);
        when(箭矢.isOnGround()).thenReturn(true);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 5, 64, 5));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), 任务捕获器.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            doNothing().when(插件管理器).registerEvents(any(Listener.class), eq(插件));
            技能.执行(构建上下文());
            任务捕获器.getValue().run();
        }
        verify(箭矢).setVelocity(速度捕获器.capture());
        assertEquals(new Vector(0, 0, 0), 速度捕获器.getValue());
        verify(箭矢).setGravity(false);
    }

    @Test
    @DisplayName("FP-6: 箭矢setPierceLevel应为127（最大穿透）")
    void 箭矢穿透级别_应为127() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 68, 0));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), timerCaptor.capture(), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            技能.执行(构建上下文());
            timerCaptor.getValue().run();
        }
        verify(箭矢).setPierceLevel((byte) 127);
    }

    @Test
    @DisplayName("世界不存在时应安全终止不抛异常")
    void 世界不存在时应安全终止() {
        秘法陨落 技能 = new 秘法陨落();
        assertDoesNotThrow(() -> {
            注入全部依赖(技能);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文());
            }
        });
    }

    @Test
    @DisplayName("玩家不在线时应安全终止不抛异常")
    void 玩家不在线时应安全终止() {
        秘法陨落 技能 = new 秘法陨落();
        assertDoesNotThrow(() -> {
            注入全部依赖(技能);
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(null);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
                技能.执行(构建上下文());
            }
        });
    }

    @Test
    @DisplayName("FP-01 debug增强：记录范围内实体诊断在getNearbyEntities返回null时安全处理")
    void 记录范围内实体诊断_getNearbyEntities返回null时安全处理() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        位置 中心 = new 位置("world", 0, 68, 0, 0f, 0f);
        Method 方法 = 秘法陨落.class.getDeclaredMethod("记录范围内实体诊断", 位置.class, 玩家快照.class);
        方法.setAccessible(true);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            assertDoesNotThrow(() -> 方法.invoke(技能, 中心, 施法者快照));
        }
    }

    @Test
    @DisplayName("FP-01 debug增强：记录范围内实体诊断正确处理范围内LivingEntity")
    void 记录范围内实体诊断_正确处理范围内实体() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        位置 中心 = new 位置("world", 0, 68, 0, 0f, 0f);
        LivingEntity 怪物 = mock(LivingEntity.class);
        UUID 怪物标识 = UUID.randomUUID();
        when(怪物.getUniqueId()).thenReturn(怪物标识);
        when(怪物.getType()).thenReturn(org.bukkit.entity.EntityType.ZOMBIE);
        // 自定义名非空时 实体适配器.获取安全名称 直接返回，跳过 EntityType.translationKey()（mock 环境无真实服务端会抛异常）
        when(怪物.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("测试魔物"));
        when(怪物.getLocation()).thenReturn(new Location(世界, 1, 64, 1));
        BoundingBox 碰撞箱 = new BoundingBox(0.6, 64.0, 0.6, 1.4, 65.8, 1.4);
        when(怪物.getBoundingBox()).thenReturn(碰撞箱);
        Collection<org.bukkit.entity.Entity> 实体集合 = new ArrayList<>();
        实体集合.add(怪物);
        Method 方法 = 秘法陨落.class.getDeclaredMethod("记录范围内实体诊断", 位置.class, 玩家快照.class);
        方法.setAccessible(true);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            when(世界.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble())).thenReturn(实体集合);
            assertDoesNotThrow(() -> 方法.invoke(技能, 中心, 施法者快照));
        }
    }

    @Test
    @DisplayName("FP-01 debug增强：记录范围内实体诊断在世界为空时安全处理")
    void 记录范围内实体诊断_世界为空时安全处理() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        位置 中心 = new 位置("nonexistent", 0, 68, 0, 0f, 0f);
        Method 方法 = 秘法陨落.class.getDeclaredMethod("记录范围内实体诊断", 位置.class, 玩家快照.class);
        方法.setAccessible(true);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("nonexistent")).thenReturn(null);
            assertDoesNotThrow(() -> 方法.invoke(技能, 中心, 施法者快照));
        }
    }

    @Test
    @DisplayName("FP-01 debug增强：施法流程含记录范围内实体诊断时不抛异常")
    void 施法流程_含记录范围内实体诊断_不抛异常() throws Exception {
        秘法陨落 技能 = new 秘法陨落();
        注入全部依赖(技能);
        when(在线玩家.getWorld()).thenReturn(世界);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(箭矢.getUniqueId()).thenReturn(UUID.randomUUID());
        when(箭矢.isValid()).thenReturn(true);
        when(箭矢.isDead()).thenReturn(false);
        when(箭矢.isInBlock()).thenReturn(false);
        when(箭矢.isOnGround()).thenReturn(false);
        when(箭矢.getWorld()).thenReturn(世界);
        when(箭矢.getLocation()).thenReturn(new Location(世界, 0, 68, 0));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(插件管理器);
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(任务);
            when(世界.spawnArrow(any(Location.class), any(Vector.class), anyFloat(), anyFloat())).thenReturn(箭矢);
            assertDoesNotThrow(() -> 技能.执行(构建上下文()));
        }
    }
}
