package mljy.业务层;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("技能冷却服务重置方法")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能冷却服务重置测试 {
    @Mock
    private JavaPlugin 插件;
    @Mock
    private Server 服务器;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 任务;

    private 技能冷却服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        服务 = new 技能冷却服务实现(插件);
        玩家标识 = UUID.randomUUID();
        when(插件.getServer()).thenReturn(服务器);
        when(服务器.getScheduler()).thenReturn(调度器);
        when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(任务);
    }

    @Nested
    @DisplayName("重置冷却（单个技能）")
    class 重置单个技能冷却 {
        @Test
        @DisplayName("开始冷却后重置 - 剩余冷却应为0")
        void 开始冷却后重置_剩余应为零() {
            服务.开始冷却(玩家标识, "技能A", 5.0);
            assertEquals(5.0, 服务.获取剩余冷却(玩家标识, "技能A"), 0.1);

            服务.重置冷却(玩家标识, "技能A");

            assertEquals(0.0, 服务.获取剩余冷却(玩家标识, "技能A"), 0.0001);
            assertFalse(服务.是否冷却中(玩家标识, "技能A"));
        }

        @Test
        @DisplayName("重置正在冷却的技能 - 应取消已调度的独立CD完成任务")
        void 重置冷却_应取消已调度任务() {
            服务.开始冷却(玩家标识, "技能A", 5.0);

            服务.重置冷却(玩家标识, "技能A");

            verify(任务).cancel();
        }

        @Test
        @DisplayName("重置正在冷却的技能 - 应触发独立CD完成回调")
        void 重置正在冷却的技能_应触发完成回调() {
            BiConsumer<UUID, String> 回调 = mock();
            服务.设置独立CD完成回调(回调);
            服务.开始冷却(玩家标识, "技能A", 5.0);

            服务.重置冷却(玩家标识, "技能A");

            verify(回调).accept(玩家标识, "技能A");
        }

        @Test
        @DisplayName("重置不存在的冷却记录 - 不应抛异常且不触发回调")
        void 重置不存在的冷却_不应抛异常且不触发回调() {
            BiConsumer<UUID, String> 回调 = mock();
            服务.设置独立CD完成回调(回调);

            assertDoesNotThrow(() -> 服务.重置冷却(玩家标识, "不存在的技能"));

            verify(回调, never()).accept(any(), any());
        }
    }

    @Nested
    @DisplayName("重置公共冷却")
    class 重置公共冷却 {
        @Test
        @DisplayName("开始公CD后重置 - 公共冷却剩余应为0")
        void 开始公CD后重置_剩余应为零() {
            服务.开始公共冷却(玩家标识, 1.5);
            assertEquals(1.5, 服务.获取公共冷却剩余(玩家标识), 0.1);

            服务.重置公共冷却(玩家标识);

            assertEquals(0.0, 服务.获取公共冷却剩余(玩家标识), 0.0001);
            assertFalse(服务.是否公共冷却中(玩家标识));
        }

        @Test
        @DisplayName("重置正在冷却的公CD - 应取消已调度的公CD完成任务")
        void 重置公CD_应取消已调度任务() {
            服务.开始公共冷却(玩家标识, 1.5);

            服务.重置公共冷却(玩家标识);

            verify(任务).cancel();
        }

        @Test
        @DisplayName("重置正在冷却的公CD - 应触发公CD完成回调")
        void 重置正在冷却的公CD_应触发完成回调() {
            Consumer<UUID> 回调 = mock();
            服务.设置公CD完成回调(回调);
            服务.开始公共冷却(玩家标识, 1.5);

            服务.重置公共冷却(玩家标识);

            verify(回调).accept(玩家标识);
        }

        @Test
        @DisplayName("重置不存在的公CD - 不应抛异常且不触发回调")
        void 重置不存在的公CD_不应抛异常且不触发回调() {
            Consumer<UUID> 回调 = mock();
            服务.设置公CD完成回调(回调);

            assertDoesNotThrow(() -> 服务.重置公共冷却(玩家标识));

            verify(回调, never()).accept(any());
        }
    }

    @Nested
    @DisplayName("重置所有冷却")
    class 重置所有冷却 {
        @Test
        @DisplayName("重置所有 - 应清除所有独立冷却和公共冷却")
        void 重置所有_应清除所有冷却() {
            服务.开始冷却(玩家标识, "技能A", 5.0);
            服务.开始冷却(玩家标识, "技能B", 3.0);
            服务.开始公共冷却(玩家标识, 1.0);

            服务.重置所有冷却(玩家标识);

            assertEquals(0.0, 服务.获取剩余冷却(玩家标识, "技能A"), 0.0001);
            assertEquals(0.0, 服务.获取剩余冷却(玩家标识, "技能B"), 0.0001);
            assertEquals(0.0, 服务.获取公共冷却剩余(玩家标识), 0.0001);
            assertFalse(服务.是否冷却中(玩家标识, "技能A"));
            assertFalse(服务.是否冷却中(玩家标识, "技能B"));
            assertFalse(服务.是否公共冷却中(玩家标识));
        }

        @Test
        @DisplayName("重置所有 - 应对每个正在冷却的技能触发独立CD完成回调并触发公CD完成回调")
        void 重置所有_应触发所有完成回调() {
            BiConsumer<UUID, String> 独立回调 = mock();
            Consumer<UUID> 公CD回调 = mock();
            服务.设置独立CD完成回调(独立回调);
            服务.设置公CD完成回调(公CD回调);
            服务.开始冷却(玩家标识, "技能A", 5.0);
            服务.开始冷却(玩家标识, "技能B", 3.0);
            服务.开始公共冷却(玩家标识, 1.0);

            服务.重置所有冷却(玩家标识);

            verify(独立回调).accept(玩家标识, "技能A");
            verify(独立回调).accept(玩家标识, "技能B");
            verify(公CD回调).accept(玩家标识);
        }

        @Test
        @DisplayName("重置所有 - 应取消所有已调度任务（独立CD+公CD）")
        void 重置所有_应取消所有任务() {
            服务.开始冷却(玩家标识, "技能A", 5.0);
            服务.开始冷却(玩家标识, "技能B", 3.0);
            服务.开始公共冷却(玩家标识, 1.0);

            服务.重置所有冷却(玩家标识);

            verify(任务, times(3)).cancel();
        }

        @Test
        @DisplayName("重置所有 - 不存在的玩家不应抛异常且不触发回调")
        void 重置所有_不存在的玩家不应抛异常() {
            BiConsumer<UUID, String> 独立回调 = mock();
            Consumer<UUID> 公CD回调 = mock();
            服务.设置独立CD完成回调(独立回调);
            服务.设置公CD完成回调(公CD回调);

            assertDoesNotThrow(() -> 服务.重置所有冷却(玩家标识));

            verify(独立回调, never()).accept(any(), any());
            verify(公CD回调, never()).accept(any());
        }
    }
}
