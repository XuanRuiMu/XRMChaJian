package mljy.技能实现.奥能法师;

import mljy.业务层.资源变更服务;
import mljy.业务层.消息服务;
import mljy.业务层.技能释放服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
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
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

/**
 * FP-5 秘法回流技能日志测试。
 *
 * 覆盖：
 *   - 释放时玩家收到 "秘法回流.治疗" 日志（治疗量、秘能参数正确）
 *   - 1秒后玩家仍移动时收到 "秘法回流.移动额外治疗" 日志
 *   - 玩家离线时不发送任何治疗日志
 */
@DisplayName("秘法回流 - FP-5 技能日志")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 秘法回流测试 {

    @Mock private JavaPlugin 插件;
    @Mock private 资源变更服务 资源变更服务;
    @Mock private 消息服务 消息服务;
    @Mock private 技能释放服务 技能释放服务;
    @Mock private Player 在线玩家;
    @Mock private World 世界;
    @Mock private BukkitScheduler 调度器;
    @Mock private BukkitTask 粒子任务句柄;
    @Mock private BukkitTask 二段任务句柄;
    @Mock private AttributeInstance 最大生命属性;

    private 秘法回流 技能;
    private UUID 施法者标识;
    private 玩家快照 施法者快照;
    private 技能上下文 上下文;

    @BeforeEach
    void setUp() throws Exception {
        技能 = new 秘法回流();
        施法者标识 = UUID.randomUUID();
        注入父类字段(技能, "插件", 插件);
        注入父类字段(技能, "资源变更服务", 资源变更服务);
        注入父类字段(技能, "消息服务", 消息服务);
        注入父类字段(技能, "技能释放服务实例", 技能释放服务);

        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 50.0,
                50.0, 40.0, 50.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        位置 玩家位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        施法者快照 = new 玩家快照(
                施法者标识, "测试法师", 1, 玩家位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);

        技能定义 定义 = new 技能定义(
                "1_6", "skill.奥能法师.秘法回流.name", 施法类型.瞬发,
                20.0, 0.0, 4.0, 1.5, "", 0.0, 5.0, 0.0, 1, true, false, false);
        上下文 = new 技能上下文(施法者快照, "1_6", 定义, null, System.currentTimeMillis());

        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(0.0);
        when(在线玩家.isOnline()).thenReturn(true);
        final double[] 当前生命值 = {20.0};
        when(在线玩家.getHealth()).thenAnswer(inv -> 当前生命值[0]);
        doAnswer(inv -> {
            当前生命值[0] = Math.min((Double) inv.getArgument(0), 100.0);
            return null;
        }).when(在线玩家).setHealth(anyDouble());
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));
        when(在线玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(最大生命属性);
        when(最大生命属性.getValue()).thenReturn(100.0);
        when(世界.getName()).thenReturn("world");
    }

    private void 注入父类字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    @Test
    @DisplayName("释放时玩家收到治疗日志：键=秘法回流.治疗 治疗量=30.0 秘能=1")
    void 释放时发送治疗日志_参数正确() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(粒子任务句柄);
            when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(二段任务句柄);

            assertDoesNotThrow(() -> 技能.执行(上下文));
        }

        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("秘法回流.治疗"),
                eq("30.0"),
                eq(1));
        verify(技能释放服务).抑制通用释放日志(施法者标识, "1_6");
    }

    @Test
    @DisplayName("玩家移动后二段触发：收到移动额外治疗日志")
    void 移动额外治疗触发_发送移动额外治疗日志() {
        Location 首次位置 = new Location(世界, 0, 64, 0);
        Location 移动后位置 = new Location(世界, 5, 64, 5);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

            ArgumentCaptor<Runnable> 粒子任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Runnable> 二段任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            when(调度器.runTaskTimer(eq(插件), 粒子任务捕获.capture(), anyLong(), anyLong())).thenReturn(粒子任务句柄);
            when(调度器.runTaskLater(eq(插件), 二段任务捕获.capture(), anyLong())).thenReturn(二段任务句柄);
            when(在线玩家.getLocation()).thenReturn(首次位置);

            assertDoesNotThrow(() -> 技能.执行(上下文));

            when(在线玩家.getLocation()).thenReturn(移动后位置);
            二段任务捕获.getValue().run();
        }

        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("秘法回流.治疗"),
                eq("30.0"),
                eq(1));
        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("秘法回流.移动额外治疗"),
                eq("30.0"),
                eq(1));
    }

    @Test
    @DisplayName("玩家未移动：不发送移动额外治疗日志")
    void 玩家未移动_不发送移动额外治疗日志() {
        Location 首次位置 = new Location(世界, 0, 64, 0);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

            ArgumentCaptor<Runnable> 二段任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(粒子任务句柄);
            when(调度器.runTaskLater(eq(插件), 二段任务捕获.capture(), anyLong())).thenReturn(二段任务句柄);
            when(在线玩家.getLocation()).thenReturn(首次位置);

            assertDoesNotThrow(() -> 技能.执行(上下文));

            二段任务捕获.getValue().run();
        }

        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("秘法回流.治疗"),
                anyString(),
                any());
        verify(消息服务, never()).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("秘法回流.移动额外治疗"),
                anyString(),
                any());
    }

    @Test
    @DisplayName("玩家离线：不发送任何治疗日志")
    void 玩家离线_不发送治疗日志() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(null);

            assertDoesNotThrow(() -> 技能.执行(上下文));
        }

        verify(消息服务, never()).发送技能日志(
                any(),
                any(UUID.class),
                eq("秘法回流.治疗"),
                any(),
                any());
        verify(消息服务, never()).发送技能日志(
                any(),
                any(UUID.class),
                eq("秘法回流.移动额外治疗"),
                any(),
                any());
    }

    @Test
    @DisplayName("FP-4 首段治疗与二段治疗使用相同事件标识，确保日志可合并")
    void 首段与二段治疗_使用相同事件标识() {
        Location 首次位置 = new Location(世界, 0, 64, 0);
        Location 移动后位置 = new Location(世界, 5, 64, 5);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);

            ArgumentCaptor<Runnable> 粒子任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Runnable> 二段任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            when(调度器.runTaskTimer(eq(插件), 粒子任务捕获.capture(), anyLong(), anyLong())).thenReturn(粒子任务句柄);
            when(调度器.runTaskLater(eq(插件), 二段任务捕获.capture(), anyLong())).thenReturn(二段任务句柄);
            when(在线玩家.getLocation()).thenReturn(首次位置);

            assertDoesNotThrow(() -> 技能.执行(上下文));

            when(在线玩家.getLocation()).thenReturn(移动后位置);
            二段任务捕获.getValue().run();
        }

        ArgumentCaptor<UUID> 首段事件标识 = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> 二段事件标识 = ArgumentCaptor.forClass(UUID.class);
        verify(消息服务).发送技能日志(
                eq(施法者快照),
                首段事件标识.capture(),
                eq("秘法回流.治疗"),
                anyString(),
                any());
        verify(消息服务).发送技能日志(
                eq(施法者快照),
                二段事件标识.capture(),
                eq("秘法回流.移动额外治疗"),
                anyString(),
                any());
        assertEquals(首段事件标识.getValue(), 二段事件标识.getValue(),
                "首段治疗与二段治疗必须使用同一事件标识以保证日志合并");
    }
}
