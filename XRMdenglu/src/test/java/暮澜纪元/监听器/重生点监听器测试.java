package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.登录服配置;
import 暮澜纪元.配置.重生点配置;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("重生点监听器")
@SuppressWarnings("null")
class 重生点监听器测试 {

    private 登录服插件 mock插件;
    private 登录服配置 mock登录配置;
    private Logger mock日志;
    private 重生点监听器 监听器;

    @BeforeEach
    void 准备() {
        mock插件 = mock(登录服插件.class);
        mock登录配置 = mock(登录服配置.class);
        mock日志 = mock(Logger.class);
        when(mock插件.获取登录配置()).thenReturn(mock登录配置);
        when(mock插件.getLogger()).thenReturn(mock日志);
        监听器 = new 重生点监听器(mock插件);
    }

    @Nested
    @DisplayName("玩家加入事件")
    class 玩家加入事件 {

        @Test
        @DisplayName("重生点为null时不应传送")
        void 重生点为null_不应传送() {
            PlayerJoinEvent mock事件 = mock(PlayerJoinEvent.class);
            when(mock登录配置.获取重生点()).thenReturn(null);

            监听器.onPlayerJoin(mock事件);

            verify(mock事件, never()).getPlayer();
        }

        @Test
        @DisplayName("重生点有效且世界存在时应调度延迟传送")
        void 重生点有效且世界存在_应调度传送() {
            PlayerJoinEvent mock事件 = mock(PlayerJoinEvent.class);
            Player mock玩家 = mock(Player.class);
            World mock世界 = mock(World.class);
            重生点配置 mock重生点 = new 重生点配置("test_world", 100.0, 64.0, 200.0, 90.0f, 45.0f);
            JavaPlugin mockJavaPlugin = mock(JavaPlugin.class);
            BukkitScheduler mock调度器 = mock(BukkitScheduler.class);
            BukkitTask mock任务 = mock(BukkitTask.class);
            when(mock事件.getPlayer()).thenReturn(mock玩家);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);
            when(mock插件.获取JavaPlugin()).thenReturn(mockJavaPlugin);
            when(mock调度器.runTaskLater(eq(mockJavaPlugin), any(Runnable.class), eq(1L))).thenReturn(mock任务);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(mock世界);
                mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                监听器.onPlayerJoin(mock事件);

                verify(mock调度器).runTaskLater(eq(mockJavaPlugin), any(Runnable.class), eq(1L));
            }
        }

        @Test
        @DisplayName("重生点有效但世界不存在时应记录警告")
        void 重生点有效但世界不存在_应记录警告() {
            PlayerJoinEvent mock事件 = mock(PlayerJoinEvent.class);
            重生点配置 mock重生点 = new 重生点配置("missing_world", 100.0, 64.0, 200.0, 90.0f, 45.0f);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("missing_world")).thenReturn(null);

                监听器.onPlayerJoin(mock事件);

                verify(mock日志).warning(contains("missing_world"));
            }
        }

        @Test
        @DisplayName("延迟传送任务中在线玩家应被传送")
        void 延迟传送任务_在线玩家应被传送() {
            PlayerJoinEvent mock事件 = mock(PlayerJoinEvent.class);
            Player mock玩家 = mock(Player.class);
            World mock世界 = mock(World.class);
            重生点配置 mock重生点 = new 重生点配置("test_world", 100.0, 64.0, 200.0, 90.0f, 45.0f);
            JavaPlugin mockJavaPlugin = mock(JavaPlugin.class);
            BukkitScheduler mock调度器 = mock(BukkitScheduler.class);
            BukkitTask mock任务 = mock(BukkitTask.class);
            when(mock事件.getPlayer()).thenReturn(mock玩家);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);
            when(mock插件.获取JavaPlugin()).thenReturn(mockJavaPlugin);
            when(mock玩家.isOnline()).thenReturn(true);

            ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
            when(mock调度器.runTaskLater(eq(mockJavaPlugin), 任务捕获器.capture(), eq(1L))).thenReturn(mock任务);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(mock世界);
                mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                监听器.onPlayerJoin(mock事件);

                Runnable 传送任务 = 任务捕获器.getValue();
                传送任务.run();
                verify(mock玩家).teleport(any(Location.class));
            }
        }

        @Test
        @DisplayName("延迟传送任务中离线玩家不应被传送")
        void 延迟传送任务_离线玩家不应被传送() {
            PlayerJoinEvent mock事件 = mock(PlayerJoinEvent.class);
            Player mock玩家 = mock(Player.class);
            World mock世界 = mock(World.class);
            重生点配置 mock重生点 = new 重生点配置("test_world", 100.0, 64.0, 200.0, 90.0f, 45.0f);
            JavaPlugin mockJavaPlugin = mock(JavaPlugin.class);
            BukkitScheduler mock调度器 = mock(BukkitScheduler.class);
            BukkitTask mock任务 = mock(BukkitTask.class);
            when(mock事件.getPlayer()).thenReturn(mock玩家);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);
            when(mock插件.获取JavaPlugin()).thenReturn(mockJavaPlugin);
            when(mock玩家.isOnline()).thenReturn(false);

            ArgumentCaptor<Runnable> 任务捕获器 = ArgumentCaptor.forClass(Runnable.class);
            when(mock调度器.runTaskLater(eq(mockJavaPlugin), 任务捕获器.capture(), eq(1L))).thenReturn(mock任务);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(mock世界);
                mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(mock调度器);

                监听器.onPlayerJoin(mock事件);

                Runnable 传送任务 = 任务捕获器.getValue();
                传送任务.run();
                verify(mock玩家, never()).teleport(any(Location.class));
            }
        }
    }

    @Nested
    @DisplayName("玩家重生事件")
    class 玩家重生事件 {

        @Test
        @DisplayName("重生点为null时不应设置重生位置")
        void 重生点为null_不应设置重生位置() {
            PlayerRespawnEvent mock事件 = mock(PlayerRespawnEvent.class);
            when(mock登录配置.获取重生点()).thenReturn(null);

            监听器.onPlayerRespawn(mock事件);

            verify(mock事件, never()).setRespawnLocation(any());
        }

        @Test
        @DisplayName("重生点有效时应设置重生位置")
        void 重生点有效_应设置重生位置() {
            PlayerRespawnEvent mock事件 = mock(PlayerRespawnEvent.class);
            World mock世界 = mock(World.class);
            重生点配置 mock重生点 = new 重生点配置("test_world", 100.5, 64.0, -200.5, 90.0f, 45.0f);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(mock世界);

                监听器.onPlayerRespawn(mock事件);

                ArgumentCaptor<Location> 位置捕获器 = ArgumentCaptor.forClass(Location.class);
                verify(mock事件).setRespawnLocation(位置捕获器.capture());
                Location 位置 = 位置捕获器.getValue();
                assertEquals(mock世界, 位置.getWorld());
                assertEquals(100.5, 位置.getX(), 0.001);
                assertEquals(64.0, 位置.getY(), 0.001);
                assertEquals(-200.5, 位置.getZ(), 0.001);
                assertEquals(90.0f, 位置.getYaw(), 0.001);
                assertEquals(45.0f, 位置.getPitch(), 0.001);
            }
        }

        @Test
        @DisplayName("重生点有效但世界不存在时应记录警告且不设置重生位置")
        void 重生点有效但世界不存在_应记录警告且不设置重生位置() {
            PlayerRespawnEvent mock事件 = mock(PlayerRespawnEvent.class);
            重生点配置 mock重生点 = new 重生点配置("missing_world", 100.0, 64.0, 200.0, 90.0f, 45.0f);
            when(mock登录配置.获取重生点()).thenReturn(mock重生点);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                mockedBukkit.when(() -> Bukkit.getWorld("missing_world")).thenReturn(null);

                监听器.onPlayerRespawn(mock事件);

                verify(mock事件, never()).setRespawnLocation(any());
                verify(mock日志).warning(contains("missing_world"));
            }
        }
    }
}
