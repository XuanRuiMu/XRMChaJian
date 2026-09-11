package mljy.技能实现.奥能法师;

import mljy.业务层.效果调度服务;
import mljy.业务层.效果调度服务实现;
import mljy.业务层.效果注册服务;
import mljy.基础设施层.内存效果服务;
import mljy.领域层.效果.效果定义;
import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数读取器;
import com.google.inject.Injector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("奥术护盾效果处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 奥术护盾效果处理器测试 {
    private static final String 奥术护盾效果标识 = "1_3_1";

    @Mock
    private JavaPlugin 插件;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 旧任务;
    @Mock
    private BukkitTask 新任务;
    @Mock
    private 效果调度服务 效果调度服务;

    private 奥术护盾效果处理器 处理器;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        处理器 = new 奥术护盾效果处理器(效果调度服务);
        玩家标识 = UUID.randomUUID();
        注入字段(处理器, "插件", 插件);
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
    @DisplayName("护盾处理器不得实现Bukkit伤害监听器")
    void 护盾处理器_不得实现伤害监听器() {
        assertFalse(Listener.class.isAssignableFrom(奥术护盾效果处理器.class));
    }

    @Test
    @DisplayName("on添加应启动护盾粒子任务")
    void on添加_启动粒子任务() {
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(新任务);

            处理器.on添加(玩家标识, 效果);

            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L));
        }
    }

    @Test
    @DisplayName("on移除应取消已启动的粒子任务")
    void on移除_取消粒子任务() throws Exception {
        注入活跃任务(旧任务);
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);

        处理器.on移除(玩家标识, 效果);

        verify(旧任务).cancel();
        assertFalse(获取活跃任务表().containsKey(玩家标识));
    }

    @Test
    @DisplayName("小数伤害应精确扣盾且不得取整")
    void 小数伤害_精确扣盾() {
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);

        奥术护盾效果处理器.护盾吸收结果 第一次 = 处理器.吸收伤害(玩家标识, 效果, 0.5D, UUID.randomUUID());
        奥术护盾效果处理器.护盾吸收结果 第二次 = 处理器.吸收伤害(玩家标识, 效果, 1.5D, UUID.randomUUID());

        assertEquals(0.5D, 第一次.吸收量(), 0.000001D);
        assertEquals(1.5D, 第一次.剩余护盾(), 0.000001D);
        assertFalse(第一次.已耗尽());
        assertEquals(1.5D, 第二次.吸收量(), 0.000001D);
        assertEquals(0.0D, 第二次.剩余护盾(), 0.000001D);
        assertTrue(第二次.已耗尽());
        assertEquals(0.0D, 处理器.获取精确护盾值(玩家标识), 0.000001D);
        verify(效果调度服务).移除(玩家标识, 奥术护盾效果标识);
    }

    @Test
    @DisplayName("FP-7: 效果调度服务未注入时护盾耗尽不得抛NPE")
    void 效果调度服务未注入_护盾耗尽不抛NPE() throws Exception {
        奥术护盾效果处理器 无服务处理器 = new 奥术护盾效果处理器();
        注入字段(无服务处理器, "插件", 插件);
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);

        奥术护盾效果处理器.护盾吸收结果 结果 =
                无服务处理器.吸收伤害(玩家标识, 效果, 2.0D, UUID.randomUUID());

        assertTrue(结果.已耗尽());
        assertEquals(2.0D, 结果.吸收量(), 0.000001D);
        assertEquals(0.0D, 无服务处理器.获取精确护盾值(玩家标识), 0.000001D);
    }

    @Test
    @DisplayName("FP-7: 通过字段注入的效果调度服务在护盾耗尽时应正确调用移除")
    void 字段注入效果调度服务_护盾耗尽应调用移除() throws Exception {
        奥术护盾效果处理器 字段注入处理器 = new 奥术护盾效果处理器();
        注入字段(字段注入处理器, "插件", 插件);
        注入字段(字段注入处理器, "效果调度服务", 效果调度服务);
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);

        奥术护盾效果处理器.护盾吸收结果 结果 =
                字段注入处理器.吸收伤害(玩家标识, 效果, 2.0D, UUID.randomUUID());

        assertTrue(结果.已耗尽());
        assertEquals(2.0D, 结果.吸收量(), 0.000001D);
        verify(效果调度服务).移除(玩家标识, 奥术护盾效果标识);
    }

    @Test
    @DisplayName("小数消费应更新展示层但保留精确剩余护盾")
    void 小数消费_更新展示层并保留精确剩余护盾() {
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 3, System.currentTimeMillis() + 20000);

        奥术护盾效果处理器.护盾吸收结果 结果 =
                处理器.吸收伤害(玩家标识, 效果, 1.25D, UUID.randomUUID());

        assertEquals(1.25D, 结果.吸收量(), 0.000001D);
        assertEquals(1.75D, 结果.剩余护盾(), 0.000001D);
        assertEquals(1.75D, 处理器.获取精确护盾值(玩家标识), 0.000001D);
        verify(效果调度服务).设置层数(玩家标识, 奥术护盾效果标识, 2);
    }

    @Test
    @DisplayName("参数覆盖护盾吸收量应覆盖默认层数")
    void 参数覆盖_护盾吸收量_覆盖默认层数() {
        参数读取器 覆盖 = new 参数读取器(Map.of("护盾吸收量", "500"));
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", null, 2, System.currentTimeMillis() + 20000, 覆盖);

        奥术护盾效果处理器.护盾吸收结果 结果 =
                处理器.吸收伤害(玩家标识, 效果, 100.0D, UUID.randomUUID());

        assertEquals(100.0D, 结果.吸收量(), 0.000001D);
        assertEquals(400.0D, 结果.剩余护盾(), 0.000001D);
        assertEquals(400.0D, 处理器.获取精确护盾值(玩家标识), 0.000001D);
    }

    @Test
    @DisplayName("非正伤害不得触发护盾扣减")
    void 非正伤害_不扣盾() {
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);

        奥术护盾效果处理器.护盾吸收结果 结果 = 处理器.吸收伤害(玩家标识, 效果, 0.0D, UUID.randomUUID());

        assertEquals(0.0D, 结果.吸收量(), 0.000001D);
        assertEquals(0.0D, 结果.剩余伤害(), 0.000001D);
        assertFalse(结果.已耗尽());
    }

    @Test
    @DisplayName("移除效果应同时清除精确护盾值和粒子任务登记")
    void on移除_原子清理护盾状态和任务() throws Exception {
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000);
        处理器.吸收伤害(玩家标识, 效果, 0.5D, UUID.randomUUID());
        注入活跃任务(旧任务);

        处理器.on移除(玩家标识, 效果);

        verify(旧任务).cancel();
        assertFalse(获取活跃任务表().containsKey(玩家标识));
        assertEquals(0.0D, 处理器.获取精确护盾值(玩家标识), 0.000001D);
    }

    @Test
    @DisplayName("on层数变化应取消旧任务并按新层数重启")
    void on层数变化_取消旧任务并重启() throws Exception {
        注入活跃任务(旧任务);
        效果实例 效果 = new 效果实例(
                奥术护盾效果标识, "1_3", 3, System.currentTimeMillis() + 20000);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(新任务);

            处理器.on层数变化(玩家标识, 效果, 5);

            verify(旧任务).cancel();
            verify(调度器).runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L));
        }
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
        when(位置.add(0, 1.0, 0)).thenReturn(位置);
        when(位置.getWorld()).thenReturn(null);
        when(新任务.getTaskId()).thenReturn(7);
        ArgumentCaptor<Runnable> 任务体捕获器 = ArgumentCaptor.forClass(Runnable.class);
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
            when(调度器.runTaskTimer(eq(插件), 任务体捕获器.capture(), eq(0L), eq(4L)))
                    .thenReturn(新任务);

            处理器.on添加(玩家标识, new 效果实例(
                    奥术护盾效果标识, "1_3", 2, System.currentTimeMillis() + 20000));
            任务体捕获器.getValue().run();

            verify(调度器).cancelTask(anyInt());
            assertFalse(获取活跃任务表().containsKey(玩家标识));
        }
    }

    @Test
    @DisplayName("FP-03 复现与修复契约：重铸相等层数提前返回致护盾未刷新；移除重添加应复位满值")
    void FP03_重铸刷新_复现与修复契约() throws Exception {
        JavaPlugin 插件 = mock(JavaPlugin.class);
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        BukkitTask 任务 = mock(BukkitTask.class);
        奥术护盾效果处理器 护盾处理器 = new 奥术护盾效果处理器();
        注入字段(护盾处理器, "插件", 插件);
        效果注册服务 注册服务 = mock(效果注册服务.class);
        Injector 注入器 = mock(Injector.class);
        效果定义 定义 = new 效果定义(
                "1_3_1", "奥术护盾", "", "", 奥术护盾效果处理器.class.getName(),
                20000L, 效果定义.无层数上限, false, false);
        when(注册服务.获取定义("1_3_1")).thenReturn(Optional.of(定义));
        when(注入器.getInstance(奥术护盾效果处理器.class)).thenReturn(护盾处理器);
        效果调度服务实现 护盾调度 = new 效果调度服务实现(new 内存效果服务(), 注册服务, 注入器);

        UUID 玩家 = UUID.randomUUID();
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L))).thenReturn(任务);

            // 第一次施放：精确护盾值=2.0（经效果参数覆盖写入 on添加）
            参数读取器 覆盖 = new 参数读取器(Map.of("护盾吸收量", "2.0"));
            护盾调度.添加(玩家, new 效果实例("1_3_1", "1_3", null, 2, System.currentTimeMillis() + 20000, 覆盖));
            assertEquals(2.0, 护盾处理器.获取精确护盾值(玩家), 0.000001D);

            // 受击扣减 0.8 -> 精确剩余 1.2
            效果实例 效果 = 护盾调度.获取(玩家, "1_3_1").orElseThrow();
            护盾处理器.吸收伤害(玩家, 效果, 0.8, UUID.randomUUID());
            assertEquals(1.2, 护盾处理器.获取精确护盾值(玩家), 0.000001D);

            // 复现旧逻辑：重铸 新层数(2) <= 现有层数(2)，调度服务提前返回，不调用 on添加，精确值未刷新
            护盾调度.添加(玩家, new 效果实例("1_3_1", "1_3", null, 2, System.currentTimeMillis() + 20000, 覆盖));
            效果 = 护盾调度.获取(玩家, "1_3_1").orElseThrow();
            奥术护盾效果处理器.护盾吸收结果 旧逻辑结果 = 护盾处理器.吸收伤害(玩家, 效果, 9.0, UUID.randomUUID());
            assertEquals(1.2, 旧逻辑结果.吸收量(), 0.000001D); // 复现：仅吸收剩余 1.2 而非满 2.0

            // 修复契约：奥术护盾.java 修复路径=移除旧效果后重新添加，应复位精确值为满 2.0
            护盾调度.移除(玩家, "1_3_1");
            护盾调度.添加(玩家, new 效果实例("1_3_1", "1_3", null, 2, System.currentTimeMillis() + 20000, 覆盖));
            效果 = 护盾调度.获取(玩家, "1_3_1").orElseThrow();
            奥术护盾效果处理器.护盾吸收结果 修复结果 = 护盾处理器.吸收伤害(玩家, 效果, 9.0, UUID.randomUUID());
            assertEquals(2.0, 修复结果.吸收量(), 0.000001D); // 修复后应吸收满 2.0
            assertEquals(0.0, 修复结果.剩余护盾(), 0.000001D);
        }
    }

    @Test
    @DisplayName("FP-5 复现：非单例时精确护盾值写入不可见于伤害路径，回退全量参数值；单例化后跨实例共享精确值")
    void FP5_单例化_精确护盾值跨实例共享() throws Exception {
        JavaPlugin 插件 = mock(JavaPlugin.class);
        BukkitScheduler 调度器 = mock(BukkitScheduler.class);
        BukkitTask 任务 = mock(BukkitTask.class);
        // 修复后（单例化）：效果调度服务与伤害计算/战斗监听路径共享同一实例，精确护盾值表全局可见
        奥术护盾效果处理器 调度实例 = new 奥术护盾效果处理器();
        奥术护盾效果处理器 伤害实例 = 调度实例;
        注入字段(调度实例, "插件", 插件);
        效果注册服务 注册服务 = mock(效果注册服务.class);
        Injector 注入器 = mock(Injector.class);
        效果定义 定义 = new 效果定义(
                "1_3_1", "奥术护盾", "", "", 奥术护盾效果处理器.class.getName(),
                20000L, 效果定义.无层数上限, false, false);
        when(注册服务.获取定义("1_3_1")).thenReturn(Optional.of(定义));
        when(注入器.getInstance(奥术护盾效果处理器.class)).thenReturn(调度实例);
        效果调度服务实现 护盾调度 = new 效果调度服务实现(new 内存效果服务(), 注册服务, 注入器);

        UUID 玩家 = UUID.randomUUID();
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L))).thenReturn(任务);

            // 非整数精确护盾值 3.7（经效果参数覆盖写入，模拟奥术护盾.java）
            参数读取器 覆盖 = new 参数读取器(Map.of("护盾吸收量", "3.7"));
            护盾调度.添加(玩家, new 效果实例("1_3_1", "1_3", null, 3, System.currentTimeMillis() + 20000, 覆盖));
            assertEquals(3.7, 调度实例.获取精确护盾值(玩家), 0.000001D);

            // 伤害路径取出的效果不含覆盖（模拟回退全量参数值场景）：吸收应来自“共享精确护盾值表”
            效果实例 护盾效果 = new 效果实例("1_3_1", "1_3", 3, System.currentTimeMillis() + 20000);
            奥术护盾效果处理器.护盾吸收结果 结果 = 伤害实例.吸收伤害(玩家, 护盾效果, 2.0, UUID.randomUUID());
            // 修复后（单例共享）：精确扣减 3.7，剩余 1.7
            assertEquals(2.0, 结果.吸收量(), 0.000001D);
            assertEquals(1.7, 结果.剩余护盾(), 0.000001D);
            assertEquals(1.7, 伤害实例.获取精确护盾值(玩家), 0.000001D);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BukkitTask> 获取活跃任务表() throws Exception {
        Field 字段 = 处理器.getClass().getDeclaredField("活跃任务表");
        字段.setAccessible(true);
        return (Map<UUID, BukkitTask>) 字段.get(处理器);
    }
}
