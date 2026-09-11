package mljy.业务层;

import com.google.inject.Injector;
import mljy.基础设施层.内存效果服务;
import mljy.技能实现.奥能法师.奥术护盾效果处理器;
import mljy.领域层.效果.效果定义;
import mljy.领域层.效果.效果处理器;
import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数读取器;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("效果调度服务实现")
class 效果调度服务实现测试 {
    private static final String 效果标识 = "fp_02";

    private 内存效果服务 效果存储;
    private 效果调度服务实现 服务;
    private 记录效果处理器 处理器;
    private Injector 注入器;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        效果存储 = new 内存效果服务();
        效果注册服务 注册服务 = mock(效果注册服务.class);
        注入器 = mock(Injector.class);
        处理器 = new 记录效果处理器();
        效果定义 定义 = new 效果定义(
                效果标识, "FP-02", "", "", 记录效果处理器.class.getName(),
                1000L, 效果定义.无层数上限, false, false);
        when(注册服务.获取定义(效果标识)).thenReturn(Optional.of(定义));
        when(注入器.getInstance(记录效果处理器.class)).thenReturn(处理器);
        服务 = new 效果调度服务实现(效果存储, 注册服务, 注入器);
        玩家标识 = UUID.randomUUID();
    }

    @Test
    @DisplayName("刷新到期效果应依次触发到期和移除并删除效果")
    void 刷新_效果到期_触发到期和移除并删除效果() {
        服务.添加(玩家标识, new 效果实例(效果标识, "skill", 3, 100L));
        处理器.事件.clear();

        服务.刷新(玩家标识, 100L);

        assertEquals(List.of("到期:3", "移除:3"), 处理器.事件);
        assertFalse(服务.拥有(玩家标识, 效果标识));
    }

    @Test
    @DisplayName("清空玩家效果应逐个触发一次移除并删除效果")
    void 清空_玩家有效果_触发一次移除并删除效果() {
        服务.添加(玩家标识, new 效果实例(效果标识, "skill", 3, -1L));
        处理器.事件.clear();

        服务.清空(玩家标识);

        assertEquals(List.of("移除:3"), 处理器.事件);
        assertFalse(服务.拥有(玩家标识, 效果标识));
    }

    @Test
    @DisplayName("不可叠加效果的新层数更大时应替换并重启处理器")
    void 添加_不可叠加且新层数更大_替换并重启处理器() {
        服务.添加(玩家标识, new 效果实例(效果标识, "old", 5, 1000L));
        处理器.事件.clear();

        服务.添加(玩家标识, new 效果实例(效果标识, "new", 8, 800L));

        效果实例 当前 = 服务.获取(玩家标识, 效果标识).orElseThrow();
        assertEquals(8, 当前.获取层数());
        assertEquals(800L, 当前.获取到期时间());
        assertEquals(List.of("移除:5", "添加:8"), 处理器.事件);
    }

    @Test
    @DisplayName("不可叠加效果新层数更小时应保留现有效果")
    void 添加_不可叠加且新层数更小_保留现有效果() {
        服务.添加(玩家标识, new 效果实例(效果标识, "old", 8, 1000L));
        处理器.事件.clear();

        服务.添加(玩家标识, new 效果实例(效果标识, "new", 5, 2000L));

        效果实例 当前 = 服务.获取(玩家标识, 效果标识).orElseThrow();
        assertEquals(8, 当前.获取层数());
        assertEquals(1000L, 当前.获取到期时间());
        assertTrue(处理器.事件.isEmpty());
    }

    @Test
    @DisplayName("不可叠加效果新层数相等时应保留现有效果")
    void 添加_不可叠加且新层数相等_保留现有效果() {
        服务.添加(玩家标识, new 效果实例(效果标识, "old", 8, 1000L));
        处理器.事件.clear();

        服务.添加(玩家标识, new 效果实例(效果标识, "new", 8, 2000L));

        效果实例 当前 = 服务.获取(玩家标识, 效果标识).orElseThrow();
        assertEquals(8, 当前.获取层数());
        assertEquals(1000L, 当前.获取到期时间());
        assertTrue(处理器.事件.isEmpty());
    }

    @Test
    @DisplayName("效果重复刷新到期后不得重复调用回调或删除")
    void 刷新_重复刷新已到期效果_只处理一次() {
        服务.添加(玩家标识, new 效果实例(效果标识, "skill", 3, 100L));
        处理器.事件.clear();

        服务.刷新(玩家标识, 100L);
        服务.刷新(玩家标识, 200L);

        assertEquals(List.of("到期:3", "移除:3"), 处理器.事件);
        assertFalse(服务.拥有(玩家标识, 效果标识));
    }

    @Test
    @DisplayName("带原因清空效果应保留清理语义并删除效果")
    void 清空_带原因_删除效果() {
        服务.添加(玩家标识, new 效果实例(效果标识, "skill", 3, -1L));
        处理器.事件.clear();

        服务.清空(玩家标识, "玩家死亡");

        assertEquals(List.of("移除:3"), 处理器.事件);
        assertFalse(服务.拥有(玩家标识, 效果标识));
    }

    @Test
    @DisplayName("处理器首次加载失败后下一次添加应重新加载")
    void 添加_处理器首次加载失败_下一次重试加载() {
        when(注入器.getInstance(记录效果处理器.class))
                .thenThrow(new IllegalStateException("首次加载失败"))
                .thenReturn(处理器);

        服务.添加(UUID.randomUUID(), new 效果实例(效果标识, "first", 1, 1000L));
        服务.添加(UUID.randomUUID(), new 效果实例(效果标识, "second", 2, 2000L));

        assertEquals(List.of("添加:2"), 处理器.事件);
        verify(注入器, times(2)).getInstance(记录效果处理器.class);
    }

    @Test
    @DisplayName("清空全部应清理仓库中在线与离线玩家的效果")
    void 清空全部_仓库存在多个玩家_全部清理() {
        UUID 离线玩家 = UUID.randomUUID();
        服务.添加(玩家标识, new 效果实例(效果标识, "online", 1, -1L));
        服务.添加(离线玩家, new 效果实例(效果标识, "offline", 2, -1L));
        处理器.事件.clear();

        服务.清空全部();

        assertFalse(服务.拥有(玩家标识, 效果标识));
        assertFalse(服务.拥有(离线玩家, 效果标识));
        assertEquals(2, 处理器.事件.size());
        assertTrue(处理器.事件.containsAll(List.of("移除:1", "移除:2")));
    }

    @Test
    @DisplayName("护盾到期应通过on移除只取消一次粒子任务")
    void 刷新_护盾到期_只取消一次粒子任务() throws Exception {
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
        效果调度服务实现 护盾调度 = new 效果调度服务实现(
                new 内存效果服务(), 注册服务, 注入器);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), eq(0L), eq(4L)))
                    .thenReturn(任务);

            护盾调度.添加(玩家标识, new 效果实例("1_3_1", "1_3", 5, 100L));
            护盾调度.刷新(玩家标识, 100L);

            verify(任务, times(1)).cancel();
            assertFalse(护盾调度.拥有(玩家标识, "1_3_1"));
        }
    }

    @Test
    @DisplayName("添加效果带覆盖负持续时间应创建永久效果并保留覆盖表")
    void 添加效果带覆盖_负持续时间_永久效果带覆盖() {
        参数读取器 覆盖 = new 参数读取器(Map.of("智力加成", "888"));
        处理器.事件.clear();

        服务.添加效果带覆盖(玩家标识, 效果标识, "skill", 3, -1L, 覆盖);

        效果实例 效果 = 服务.获取(玩家标识, 效果标识).orElseThrow();
        assertEquals(3, 效果.获取层数());
        assertTrue(效果.是否永久());
        assertTrue(效果.获取参数覆盖().是否有覆盖("智力加成"));
        assertEquals(888, 效果.获取参数覆盖().获取整数("智力加成", 0));
        assertEquals(List.of("添加:3"), 处理器.事件);
    }

    @Test
    @DisplayName("添加效果带覆盖正持续时间应按当前时间计算到期时间")
    void 添加效果带覆盖_正持续时间_计算到期() {
        long 调用前 = System.currentTimeMillis();
        服务.添加效果带覆盖(玩家标识, 效果标识, "skill", 1, 5000L, new 参数读取器());
        long 调用后 = System.currentTimeMillis();

        效果实例 效果 = 服务.获取(玩家标识, 效果标识).orElseThrow();
        assertFalse(效果.是否永久());
        assertTrue(效果.获取到期时间() >= 调用前 + 5000L);
        assertTrue(效果.获取到期时间() <= 调用后 + 5000L);
    }

    private void 注入字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 实例.getClass().getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    public static final class 记录效果处理器 implements 效果处理器 {
        private final List<String> 事件 = new ArrayList<>();

        @Override
        public void on添加(UUID 目标标识, 效果实例 效果) {
            事件.add("添加:" + 效果.获取层数());
        }

        @Override
        public void on移除(UUID 目标标识, 效果实例 效果) {
            事件.add("移除:" + 效果.获取层数());
        }

        @Override
        public void on到期(UUID 目标标识, 效果实例 效果) {
            事件.add("到期:" + 效果.获取层数());
        }

        @Override
        public void on层数变化(UUID 目标标识, 效果实例 效果, int 旧层数) {
            事件.add("层数:" + 旧层数 + ">" + 效果.获取层数());
        }
    }
}
