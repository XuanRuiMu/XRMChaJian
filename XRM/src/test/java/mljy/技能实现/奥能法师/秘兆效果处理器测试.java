package mljy.技能实现.奥能法师;

import mljy.领域层.效果.效果实例;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 秘兆效果处理器测试 - FP-B 点2 秘兆粒子清除验证
 *
 * 测试目标：验证秘兆效果添加时启动粒子任务，移除/到期/手动停止时取消粒子任务，
 * 使周身粒子特效随效果消失而消失。无活跃任务时移除应安全不抛异常。
 */
@DisplayName("秘兆效果处理器 - FP-B 粒子清除验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 秘兆效果处理器测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;

    private 秘兆效果处理器 处理器;
    private UUID 玩家标识;
    private 效果实例 效果;

    @BeforeEach
    void setUp() throws Exception {
        处理器 = new 秘兆效果处理器();
        玩家标识 = UUID.randomUUID();
        注入字段(处理器, "插件", 插件);
        效果 = new 效果实例("1_9_1", "1_9", 1, System.currentTimeMillis() + 5000);
    }

    private void 注入字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 实例.getClass().getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    @Test
    @DisplayName("旧粒子任务失效回调不得清除同玩家新任务登记")
    void 旧任务失效回调_保留新任务登记() throws Exception {
        BukkitTask 旧任务 = mock(BukkitTask.class);
        BukkitTask 新任务 = mock(BukkitTask.class);
        获取活跃任务表().put(玩家标识, 新任务);
        BukkitRunnable 旧任务体 = mock(BukkitRunnable.class);
        Method 方法 = 处理器.getClass().getDeclaredMethod(
                "清理失效任务", BukkitRunnable.class, BukkitTask.class, UUID.class, String.class);
        方法.setAccessible(true);

        方法.invoke(处理器, 旧任务体, 旧任务, 玩家标识, "玩家离线");

        assertSame(新任务, 获取活跃任务表().get(玩家标识));
    }

    @SuppressWarnings("unchecked")
    private void 注入活跃任务(BukkitTask 任务句柄) throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        ((Map<UUID, BukkitTask>) 字段.get(处理器)).put(玩家标识, 任务句柄);
    }

    @Test
    @DisplayName("on添加应启动秘兆粒子任务")
    void on添加应启动秘兆粒子任务() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L));
        }
    }

    @Test
    @DisplayName("重复添加应先取消旧粒子任务")
    void 重复添加_先取消旧任务() throws Exception {
        注入活跃任务(任务);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(任务).cancel();
            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L));
        }
    }

    @Test
    @DisplayName("on移除应取消已启动的粒子任务")
    void on移除应取消粒子任务() throws Exception {
        注入活跃任务(任务);

        处理器.on移除(玩家标识, 效果);

        verify(任务).cancel();
        assertFalse(获取活跃任务表().containsKey(玩家标识));
    }

    @Test
    @DisplayName("on到期应取消已启动的粒子任务")
    void on到期应取消粒子任务() throws Exception {
        注入活跃任务(任务);

        处理器.on到期(玩家标识, 效果);

        verify(任务).cancel();
    }

    @Test
    @DisplayName("层数变化不得重复启动或取消秘兆粒子任务")
    void 层数变化不得重启粒子任务() throws Exception {
        注入活跃任务(任务);
        效果实例 新层数效果 = new 效果实例("1_9_1", "1_9", 2, System.currentTimeMillis() + 5000);

        处理器.on层数变化(玩家标识, 新层数效果, 1);

        verify(任务, never()).cancel();
        assertSame(任务, 获取活跃任务表().get(玩家标识));
    }

    @Test
    @DisplayName("层数归零应立即停止并注销秘兆粒子任务")
    void 层数归零_停止并注销任务() throws Exception {
        注入活跃任务(任务);
        效果实例 零层效果 = new 效果实例("1_9_1", "1_9", 0, System.currentTimeMillis() + 5000);

        处理器.on层数变化(玩家标识, 零层效果, 1);

        verify(任务).cancel();
        assertFalse(获取活跃任务表().containsKey(玩家标识));
    }

    @Test
    @DisplayName("停止特效应取消已启动的粒子任务")
    void 停止特效应取消粒子任务() throws Exception {
        注入活跃任务(任务);

        处理器.停止特效(玩家标识);

        verify(任务).cancel();
        assertFalse(获取活跃任务表().containsKey(玩家标识));
    }

    @Test
    @DisplayName("无活跃任务时on移除应安全不抛异常")
    void 无活跃任务时移除应安全() {
        assertDoesNotThrow(() -> 处理器.on移除(玩家标识, 效果));
    }

    @Test
    @DisplayName("粒子任务发现玩家世界为空时应取消并清除登记")
    void 粒子任务_玩家世界为空_取消并清除登记() throws Exception {
        Player 玩家 = mock(Player.class);
        Location 位置 = mock(Location.class);
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isValid()).thenReturn(true);
        when(玩家.getLocation()).thenReturn(位置);
        when(位置.clone()).thenReturn(位置);
        when(位置.add(0, 1.5, 0)).thenReturn(位置);
        when(位置.getWorld()).thenReturn(null);
        when(任务.getTaskId()).thenReturn(9);
        ArgumentCaptor<Runnable> 任务体捕获器 = ArgumentCaptor.forClass(Runnable.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
            when(调度器.runTaskTimer(eq(插件), 任务体捕获器.capture(), eq(0L), eq(3L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 效果);
            任务体捕获器.getValue().run();

            verify(调度器).cancelTask(anyInt());
            assertFalse(获取活跃任务表().containsKey(玩家标识));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BukkitTask> 获取活跃任务表() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        return (Map<UUID, BukkitTask>) 字段.get(处理器);
    }
}
