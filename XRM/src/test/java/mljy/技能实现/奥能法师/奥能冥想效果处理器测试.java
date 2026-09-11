package mljy.技能实现.奥能法师;

import mljy.玩家服务;
import mljy.属性服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数读取器;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("奥能冥想效果处理器")
@ExtendWith(MockitoExtension.class)
class 奥能冥想效果处理器测试 {
    @Mock
    private JavaPlugin 插件;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;
    @Mock
    private 修饰器管理器 修饰器管理器;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性服务 属性服务;

    private 奥能冥想效果处理器 处理器;
    private UUID 玩家标识;
    private 效果实例 效果;

    @BeforeEach
    void setUp() throws Exception {
        处理器 = new 奥能冥想效果处理器();
        玩家标识 = UUID.randomUUID();
        效果 = new 效果实例("1_7_2", "1_7", 4, 100L);
        注入字段(处理器, "插件", 插件);
        注入字段(处理器, "修饰器管理器", 修饰器管理器);
        注入字段(处理器, "玩家服务", 玩家服务);
        注入字段(处理器, "属性服务", 属性服务);
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
    private void 注入活跃任务() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        ((Map<UUID, BukkitTask>) 字段.get(处理器)).put(玩家标识, 任务);
    }

    @Test
    @DisplayName("on添加应启动冥想粒子任务")
    void on添加_启动粒子任务() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L));
        }
    }

    @Test
    @DisplayName("on添加应注册智力修饰器到修饰器管理器")
    void on添加_注册智力修饰器() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            ArgumentCaptor<修饰器> 捕获器 = ArgumentCaptor.forClass(修饰器.class);
            verify(修饰器管理器).注册修饰器(eq(玩家标识), 捕获器.capture());
            修饰器 注册的 = 捕获器.getValue();
            assertEquals("智力", 注册的.类别());
            assertEquals("奥能冥想", 注册的.标签());
            assertEquals(修饰方式.相加, 注册的.方式());
            assertEquals(40.0, 注册的.数值());
            assertEquals(100, 注册的.优先级());
        }
    }

    @Test
    @DisplayName("on添加在玩家快照缺失时跳过注册且不抛异常")
    void on添加_玩家快照缺失_跳过注册() {
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(修饰器管理器, never()).注册修饰器(eq(玩家标识), any(修饰器.class));
        }
    }

    @Test
    @DisplayName("重复添加应先取消旧粒子任务并刷新修饰器")
    void 重复添加_先取消旧任务() throws Exception {
        注入活跃任务();
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(任务).cancel();
            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L));
        }
    }

    @Test
    @DisplayName("重复添加应注销旧修饰器后再注册新修饰器避免叠加")
    void 重复添加_注销旧修饰器再注册新() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 效果);

            verify(修饰器管理器).注销修饰器(玩家标识, "智力", "奥能冥想");
            verify(修饰器管理器, times(1)).注册修饰器(eq(玩家标识), any(修饰器.class));
        }
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
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        ArgumentCaptor<Runnable> 任务体捕获器 = ArgumentCaptor.forClass(Runnable.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
            when(调度器.runTaskTimer(eq(插件), 任务体捕获器.capture(), eq(0L), eq(4L))).thenReturn(任务);

            处理器.on添加(玩家标识, 效果);
            任务体捕获器.getValue().run();

            verify(调度器).cancelTask(anyInt());
            assertFalse(获取活跃任务表().containsKey(玩家标识));
        }
    }

    @Test
    @DisplayName("on到期应取消冥想粒子任务并注销智力修饰器")
    void on到期_取消粒子任务并注销修饰器() throws Exception {
        注入活跃任务();

        处理器.on到期(玩家标识, 效果);

        verify(任务).cancel();
        verify(修饰器管理器).注销修饰器(玩家标识, "智力", "奥能冥想");
    }

    @Test
    @DisplayName("on移除应取消冥想粒子任务并注销智力修饰器")
    void on移除_取消粒子任务并注销修饰器() throws Exception {
        注入活跃任务();

        处理器.on移除(玩家标识, 效果);

        verify(任务).cancel();
        verify(修饰器管理器).注销修饰器(玩家标识, "智力", "奥能冥想");
    }

    @Test
    @DisplayName("on层数变化应重新注册修饰器以反映新层数")
    void on层数变化_重新注册修饰器() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        效果实例 新效果 = new 效果实例("1_7_2", "1_7", 玩家标识, 6, 100L);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on层数变化(玩家标识, 新效果, 4);

            ArgumentCaptor<修饰器> 捕获器 = ArgumentCaptor.forClass(修饰器.class);
            verify(修饰器管理器).注册修饰器(eq(玩家标识), 捕获器.capture());
            修饰器 注册的 = 捕获器.getValue();
            assertEquals(60.0, 注册的.数值(), "层数为6等级10时智力加成应为60");
        }
    }

    @Test
    @DisplayName("注册智力修饰器数值与奥能冥想日志公式一致")
    void 注册智力修饰器_数值公式一致() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(15);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        效果实例 三层效果 = new 效果实例("1_7_2", "1_7", 玩家标识, 3, 100L);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 三层效果);

            ArgumentCaptor<修饰器> 捕获器 = ArgumentCaptor.forClass(修饰器.class);
            verify(修饰器管理器).注册修饰器(eq(玩家标识), 捕获器.capture());
            assertEquals(45.0, 捕获器.getValue().数值(), "等级15 * 层数3 = 45");
        }
    }

    @Test
    @DisplayName("参数覆盖智力加成应覆盖默认等级乘层数公式")
    void 参数覆盖_智力加成_覆盖默认公式() {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        参数读取器 覆盖 = new 参数读取器(Map.of("智力加成", "999"));
        效果实例 覆盖效果 = new 效果实例("1_7_2", "1_7", 玩家标识, 4, 100L, 覆盖);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            处理器.on添加(玩家标识, 覆盖效果);

            ArgumentCaptor<修饰器> 捕获器 = ArgumentCaptor.forClass(修饰器.class);
            verify(修饰器管理器).注册修饰器(eq(玩家标识), 捕获器.capture());
            assertEquals(999.0, 捕获器.getValue().数值(), "参数覆盖智力加成=999应覆盖默认公式10*4=40");
        }
    }

    @Nested
    @DisplayName("属性缓存刷新")
    class 属性缓存刷新 {

        @Test
        @DisplayName("on添加注册修饰器后应刷新属性缓存")
        void on添加_刷新属性缓存() {
            玩家快照 快照 = mock(玩家快照.class);
            when(快照.等级()).thenReturn(10);
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                        .thenReturn(任务);

                处理器.on添加(玩家标识, 效果);

                verify(属性服务).刷新属性(玩家标识);
            }
        }

        @Test
        @DisplayName("on添加玩家快照缺失时也应刷新属性缓存")
        void on添加_快照缺失_仍刷新属性缓存() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
                when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                        .thenReturn(任务);

                处理器.on添加(玩家标识, 效果);

                verify(属性服务).刷新属性(玩家标识);
            }
        }

        @Test
        @DisplayName("on到期注销修饰器后应刷新属性缓存")
        void on到期_刷新属性缓存() throws Exception {
            注入活跃任务();

            处理器.on到期(玩家标识, 效果);

            verify(属性服务).刷新属性(玩家标识);
        }

        @Test
        @DisplayName("on移除注销修饰器后应刷新属性缓存")
        void on移除_刷新属性缓存() throws Exception {
            注入活跃任务();

            处理器.on移除(玩家标识, 效果);

            verify(属性服务).刷新属性(玩家标识);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BukkitTask> 获取活跃任务表() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        return (Map<UUID, BukkitTask>) 字段.get(处理器);
    }

    @Test
    @DisplayName("FP-02 四层(秘能≥4)粒子光柱最高Y钳制到≤脚底+2格，不超玩家身高")
    void 四层粒子_高度钳制到2格() throws Exception {
        玩家快照 快照 = mock(玩家快照.class);
        when(快照.等级()).thenReturn(10);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
        World 世界 = mock(World.class);
        when(世界.getName()).thenReturn("world");
        Location 真实位置 = new Location(世界, 0, 64, 0);
        Player 玩家 = mock(Player.class);
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isDead()).thenReturn(false);
        when(玩家.getLocation()).thenReturn(真实位置);
        效果实例 四层效果 = new 效果实例("1_7_2", "1_7", 玩家标识, 4, 100L);
        ArgumentCaptor<Runnable> 任务体捕获器 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Location> 位置捕获 = ArgumentCaptor.forClass(Location.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
            when(调度器.runTaskTimer(eq(插件), 任务体捕获器.capture(), eq(0L), eq(4L))).thenReturn(任务);

            处理器.on添加(玩家标识, 四层效果);
            任务体捕获器.getValue().run();
        }
        verify(世界, atLeastOnce()).spawnParticle(any(Particle.class), 位置捕获.capture(),
                anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
        for (Location 粒子位置 : 位置捕获.getAllValues()) {
            assertTrue(粒子位置.getY() <= 66.0,
                    "四层粒子最高Y=" + 粒子位置.getY() + " 必须≤脚底(64)+2=66");
        }
    }
}
