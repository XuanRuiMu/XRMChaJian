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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("奥能冥想蓄能效果处理器")
@ExtendWith(MockitoExtension.class)
class 奥能冥想蓄能效果处理器测试 {
    @Mock
    private JavaPlugin 插件;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;

    private 奥能冥想蓄能效果处理器 处理器;
    private UUID 玩家标识;
    private 效果实例 效果;

    @BeforeEach
    void setUp() throws Exception {
        处理器 = new 奥能冥想蓄能效果处理器();
        玩家标识 = UUID.randomUUID();
        效果 = new 效果实例("1_7_1", "1_7", 2, System.currentTimeMillis() + 2000);
        注入字段(处理器, "插件", 插件);
    }

    @Test
    @DisplayName("on添加应启动蓄能粒子任务")
    void on添加_启动粒子任务() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(3L));
        }
    }

    @Test
    @DisplayName("on移除应取消蓄能粒子任务")
    void on移除_取消粒子任务() throws Exception {
        注入活跃任务();

        处理器.on移除(玩家标识, 效果);

        verify(任务).cancel();
    }

    @Test
    @DisplayName("粒子任务发现玩家世界为空时应取消并清除登记")
    void 粒子任务_玩家世界为空_取消并清除登记() throws Exception {
        Player 玩家 = mock(Player.class);
        Location 位置 = mock(Location.class);
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isDead()).thenReturn(false);
        when(玩家.getLocation()).thenReturn(位置);
        when(位置.getWorld()).thenReturn(null);
        ArgumentCaptor<Runnable> 任务体捕获器 = ArgumentCaptor.forClass(Runnable.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
            when(调度器.runTaskTimer(eq(插件), 任务体捕获器.capture(), eq(0L), eq(3L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);
            任务体捕获器.getValue().run();

            verify(调度器).cancelTask(anyInt());
            assertFalse(获取活跃任务表().containsKey(玩家标识));
        }
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

    private void 注入字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 实例.getClass().getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    @SuppressWarnings("unchecked")
    private void 注入活跃任务() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        ((Map<UUID, BukkitTask>) 字段.get(处理器)).put(玩家标识, 任务);
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BukkitTask> 获取活跃任务表() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        return (Map<UUID, BukkitTask>) 字段.get(处理器);
    }
}
