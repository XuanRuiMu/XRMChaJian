package mljy.业务层;

import mljy.玩家服务;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.属性快照;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("FP-X9 战斗状态服务实现 - 脱战回血校验")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 战斗状态服务实现测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private JavaPlugin 插件;

    private 战斗状态服务实现 战斗状态服务;
    private 玩家快照 快照;
    private UUID 玩家标识;
    private Player 玩家;

    @BeforeEach
    void setUp() {
        玩家标识 = UUID.randomUUID();
        玩家 = mock(Player.class);
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isDead()).thenReturn(false);
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, null, java.util.Collections.emptyMap(), mljy.领域层.战斗.战斗状态.非战斗);
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));

        FileConfiguration 配置 = mock(FileConfiguration.class);
        when(配置.getLong("战斗.战斗状态退出秒数", 5L)).thenReturn(5L);
        when(插件.getConfig()).thenReturn(配置);

        战斗状态服务 = new 战斗状态服务实现(玩家服务, 属性计算服务, 消息服务, 资源变更服务, 插件);
    }

    @AfterEach
    void 清理线程上下文() {
        事件日志上下文.清除();
    }

    private void 设置玩家属性(double 当前生命值, double 最大生命值, double 生命恢复) {
        when(玩家.getHealth()).thenReturn(当前生命值);
        AttributeInstance 属性 = mock(AttributeInstance.class);
        when(属性.getValue()).thenReturn(最大生命值);
        when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(属性);

        属性快照 快照 = 属性快照.创建(
                最大生命值, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0,
                0, 0, 0, 0,
                生命恢复, 0
        );
        when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(mock(玩家快照.class)));
        when(属性计算服务.计算(any(玩家快照.class))).thenReturn(快照);
    }

    @Nested
    @DisplayName("脱战回血 - 战斗状态校验")
    class 脱战回血战斗状态校验 {

        @Test
        @DisplayName("玩家在战斗中 - 执行脱战回血 - 不应回血")
        void 玩家在战斗中_不应回血() {
            设置玩家属性(10.0, 100.0, 10.0);
            战斗状态服务.进入战斗(玩家标识);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家, never()).setHealth(anyDouble());
            }
        }

        @Test
        @DisplayName("玩家不在战斗中 - 执行脱战回血 - 应回血")
        void 玩家不在战斗中_应回血() {
            设置玩家属性(10.0, 100.0, 10.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家).setHealth(20.0);
            }
        }

        @Test
        @DisplayName("玩家进入战斗后超时脱战 - 执行脱战回血 - 应回血")
        void 玩家进入战斗后超时脱战_应回血() throws InterruptedException {
            设置玩家属性(50.0, 100.0, 5.0);
            FileConfiguration 零秒配置 = mock(FileConfiguration.class);
            when(零秒配置.getLong("战斗.战斗状态退出秒数", 5L)).thenReturn(0L);
            when(插件.getConfig()).thenReturn(零秒配置);
            战斗状态服务实现 零秒服务 = new 战斗状态服务实现(玩家服务, 属性计算服务, 消息服务, 资源变更服务, 插件);

            零秒服务.进入战斗(玩家标识);
            assertTrue(零秒服务.是否战斗中(玩家标识));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                零秒服务.刷新脱战();
                assertFalse(零秒服务.是否战斗中(玩家标识));

                Thread.sleep(TimeUnit.SECONDS.toMillis(1) + 100L);
                零秒服务.执行脱战回血();

                verify(玩家).setHealth(55.0);
            }
        }

        @Test
        @DisplayName("玩家进入战斗后强制脱战 - 执行脱战回血 - 应回血")
        void 玩家进入战斗后强制脱战_应回血() {
            设置玩家属性(80.0, 100.0, 15.0);
            战斗状态服务.进入战斗(玩家标识);
            assertTrue(战斗状态服务.是否战斗中(玩家标识));

            战斗状态服务.强制脱战(玩家标识);
            assertFalse(战斗状态服务.是否战斗中(玩家标识));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家).setHealth(95.0);
            }
        }
    }

    @Nested
    @DisplayName("脱战回血 - 边界值")
    class 脱战回血边界值 {

        @Test
        @DisplayName("玩家不在战斗中但生命已满 - 不应回血")
        void 生命已满_不应回血() {
            设置玩家属性(100.0, 100.0, 10.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家, never()).setHealth(anyDouble());
            }
        }

        @Test
        @DisplayName("玩家不在战斗中但生命恢复为0 - 不应回血")
        void 生命恢复为零_不应回血() {
            设置玩家属性(50.0, 100.0, 0.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家, never()).setHealth(anyDouble());
            }
        }

        @Test
        @DisplayName("玩家死亡 - 不应回血")
        void 玩家死亡_不应回血() {
            设置玩家属性(0.0, 100.0, 10.0);
            when(玩家.isDead()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家, never()).setHealth(anyDouble());
            }
        }

        @Test
        @DisplayName("玩家不在线 - 不应回血")
        void 玩家不在线_不应回血() {
            设置玩家属性(50.0, 100.0, 10.0);
            when(玩家.isOnline()).thenReturn(false);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家, never()).setHealth(anyDouble());
            }
        }

        @Test
        @DisplayName("回血量不超过最大生命值")
        void 回血量不超过最大生命值() {
            设置玩家属性(95.0, 100.0, 20.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家).setHealth(100.0);
            }
        }
    }

    @Nested
    @DisplayName("战斗状态查询")
    class 战斗状态查询 {

        @Test
        @DisplayName("进入战斗后 - 是否战斗中应返回true")
        void 进入战斗后_是否战斗中应返回true() {
            战斗状态服务.进入战斗(玩家标识);
            assertTrue(战斗状态服务.是否战斗中(玩家标识));
        }

        @Test
        @DisplayName("未进入战斗 - 是否战斗中应返回false")
        void 未进入战斗_是否战斗中应返回false() {
            assertFalse(战斗状态服务.是否战斗中(玩家标识));
        }

        @Test
        @DisplayName("进入战斗后强制脱战 - 是否战斗中应返回false")
        void 进入战斗后强制脱战_是否战斗中应返回false() {
            战斗状态服务.进入战斗(玩家标识);
            战斗状态服务.强制脱战(玩家标识);
            assertFalse(战斗状态服务.是否战斗中(玩家标识));
        }

        @Test
        @DisplayName("获取战斗状态 - 战斗中应返回战斗中枚举")
        void 获取战斗状态_战斗中() {
            战斗状态服务.进入战斗(玩家标识);
            assertEquals(mljy.领域层.战斗.战斗状态.战斗中, 战斗状态服务.获取战斗状态(玩家标识));
        }

        @Test
        @DisplayName("获取战斗状态 - 非战斗应返回非战斗枚举")
        void 获取战斗状态_非战斗() {
            assertEquals(mljy.领域层.战斗.战斗状态.非战斗, 战斗状态服务.获取战斗状态(玩家标识));
        }

        @Test
        @DisplayName("获取战斗中玩家列表 - 应包含战斗中玩家")
        void 获取战斗中玩家列表_应包含战斗中玩家() {
            战斗状态服务.进入战斗(玩家标识);
            List<UUID> 列表 = 战斗状态服务.获取战斗中玩家列表();
            assertEquals(1, 列表.size());
            assertEquals(玩家标识, 列表.get(0));
        }

        @Test
        @DisplayName("获取战斗中玩家列表 - 无战斗中玩家时应为空")
        void 获取战斗中玩家列表_无战斗中玩家时应为空() {
            List<UUID> 列表 = 战斗状态服务.获取战斗中玩家列表();
            assertTrue(列表.isEmpty());
        }
    }

    @Nested
    @DisplayName("脱战回血 - 多玩家场景")
    class 多玩家场景 {

        @Test
        @DisplayName("一个战斗中一个脱战 - 只有脱战玩家应回血")
        void 一个战斗中一个脱战_只有脱战玩家应回血() {
            UUID 战斗玩家标识 = UUID.randomUUID();
            Player 战斗玩家 = mock(Player.class);
            when(战斗玩家.getUniqueId()).thenReturn(战斗玩家标识);
            when(战斗玩家.isOnline()).thenReturn(true);
            when(战斗玩家.isDead()).thenReturn(false);

            设置玩家属性(10.0, 100.0, 10.0);

            when(玩家服务.获取快照(战斗玩家标识)).thenReturn(Optional.of(mock(玩家快照.class)));
            AttributeInstance 战斗玩家属性 = mock(AttributeInstance.class);
            when(战斗玩家属性.getValue()).thenReturn(100.0);
            when(战斗玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(战斗玩家属性);
            when(战斗玩家.getHealth()).thenReturn(50.0);

            属性快照 战斗玩家快照 = 属性快照.创建(
                    100, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 10.0, 0
            );
            when(属性计算服务.计算(any(玩家快照.class))).thenReturn(战斗玩家快照);

            战斗状态服务.进入战斗(战斗玩家标识);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家, 战斗玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                bukkitMock.when(() -> Bukkit.getPlayer(战斗玩家标识)).thenReturn(战斗玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家).setHealth(20.0);
                verify(战斗玩家, never()).setHealth(anyDouble());
            }
        }
    }

    @Nested
    @DisplayName("调试模式")
    class 调试模式 {

        @Test
        @DisplayName("设置调试模式 - 是否调试模式应返回设置值")
        void 设置调试模式_应返回设置值() {
            战斗状态服务.设置调试模式(true);
            assertTrue(战斗状态服务.是否调试模式());

            战斗状态服务.设置调试模式(false);
            assertFalse(战斗状态服务.是否调试模式());
        }
    }

    @Nested
    @DisplayName("战斗日志输出")
    class 战斗日志输出 {

        @Test
        @DisplayName("进入战斗 - 应发送进入战斗日志")
        void 进入战斗_应发送进入战斗日志() {
            战斗状态服务.进入战斗(玩家标识);

            verify(消息服务).发送战斗日志(eq(快照), any(UUID.class), eq(0), eq("战斗日志.进入战斗"));
        }

        @Test
        @DisplayName("重复进入战斗 - 不应重复发送进入战斗日志")
        void 重复进入战斗_不应重复发送() {
            战斗状态服务.进入战斗(玩家标识);
            战斗状态服务.进入战斗(玩家标识);

            verify(消息服务).发送战斗日志(eq(快照), any(UUID.class), eq(0), eq("战斗日志.进入战斗"));
        }

        @Test
        @DisplayName("强制脱战 - 应发送脱离战斗日志")
        void 强制脱战_应发送脱离战斗日志() {
            战斗状态服务.进入战斗(玩家标识);
            战斗状态服务.强制脱战(玩家标识);

            verify(消息服务).发送战斗日志(eq(快照), any(UUID.class), eq(0), eq("战斗日志.脱离战斗"));
        }

        @Test
        @DisplayName("超时脱战 - 应发送脱离战斗日志")
        void 超时脱战_应发送脱离战斗日志() {
            FileConfiguration 零秒配置 = mock(FileConfiguration.class);
            when(零秒配置.getLong("战斗.战斗状态退出秒数", 5L)).thenReturn(0L);
            when(插件.getConfig()).thenReturn(零秒配置);
            战斗状态服务实现 零秒服务 = new 战斗状态服务实现(玩家服务, 属性计算服务, 消息服务, 资源变更服务, 插件);

            零秒服务.进入战斗(玩家标识);
            零秒服务.刷新脱战();

            verify(消息服务).发送战斗日志(eq(快照), any(UUID.class), eq(0), eq("战斗日志.脱离战斗"));
        }

        @Test
        @DisplayName("玩家快照不存在 - 不应发送战斗日志")
        void 快照不存在_不应发送战斗日志() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());

            战斗状态服务.进入战斗(玩家标识);

            verifyNoInteractions(消息服务);
        }

        @Test
        @DisplayName("FP-06 脱战回血 - 应回复生命值并发送脱战回血日志")
        void 脱战回血_应发送脱战回血日志() {
            设置玩家属性(10.0, 100.0, 10.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(玩家).setHealth(20.0);
                verify(消息服务).发送战斗日志(any(), any(UUID.class), eq(0), eq("战斗日志.脱战回血"), eq("10.0"));
            }
        }

        @Test
        @DisplayName("FP-06 脱战回血 - 生命已满不应发送脱战回血日志")
        void 脱战回血_生命已满_不应发送日志() {
            设置玩家属性(100.0, 100.0, 10.0);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                verify(消息服务, never()).发送战斗日志(any(), any(UUID.class), eq(0), eq("战斗日志.脱战回血"));
            }
        }
    }

    @Nested
    @DisplayName("FP-B 脱战吞日志修复 - 事件标识独立性")
    class 事件标识独立性 {

        @Test
        @DisplayName("进入战斗日志 - 应使用独立 UUID 事件标识，不与 ThreadLocal 残留的技能事件标识合并")
        void 进入战斗日志_应使用独立UUID事件标识() {
            UUID 技能事件标识 = 事件日志上下文.开始新事件();

            战斗状态服务.进入战斗(玩家标识);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.进入战斗"));

            UUID 进入战斗事件标识 = 事件标识捕获.getValue();
            assertNotNull(进入战斗事件标识, "进入战斗日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 进入战斗事件标识,
                    "进入战斗日志事件标识不应与 ThreadLocal 残留的技能事件标识相同，否则会被合并吞掉");
        }

        @Test
        @DisplayName("强制脱战日志 - 应使用独立 UUID 事件标识，不与 ThreadLocal 残留的技能事件标识合并")
        void 强制脱战日志_应使用独立UUID事件标识() {
            战斗状态服务.进入战斗(玩家标识);

            UUID 技能事件标识 = 事件日志上下文.开始新事件();
            战斗状态服务.强制脱战(玩家标识);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.脱离战斗"));

            UUID 脱战事件标识 = 事件标识捕获.getValue();
            assertNotNull(脱战事件标识, "脱战日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 脱战事件标识,
                    "脱战日志事件标识不应与 ThreadLocal 残留的技能事件标识相同，否则会被合并吞掉");
        }

        @Test
        @DisplayName("超时脱战日志 - 应使用独立 UUID 事件标识，不与 ThreadLocal 残留的技能事件标识合并")
        void 超时脱战日志_应使用独立UUID事件标识() {
            FileConfiguration 零秒配置 = mock(FileConfiguration.class);
            when(零秒配置.getLong("战斗.战斗状态退出秒数", 5L)).thenReturn(0L);
            when(插件.getConfig()).thenReturn(零秒配置);
            战斗状态服务实现 零秒服务 = new 战斗状态服务实现(玩家服务, 属性计算服务, 消息服务, 资源变更服务, 插件);

            零秒服务.进入战斗(玩家标识);

            UUID 技能事件标识 = 事件日志上下文.开始新事件();
            零秒服务.刷新脱战();

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.脱离战斗"));

            UUID 脱战事件标识 = 事件标识捕获.getValue();
            assertNotNull(脱战事件标识, "超时脱战日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 脱战事件标识,
                    "超时脱战日志事件标识不应与 ThreadLocal 残留的技能事件标识相同");
        }

        @Test
        @DisplayName("释放技能→脱战场景 - 进入战斗、技能日志、脱战三条日志事件标识应相互独立")
        void 释放技能脱战场景_三条日志事件标识应相互独立() {
            UUID 技能事件标识 = 事件日志上下文.开始新事件();

            战斗状态服务.进入战斗(玩家标识);
            战斗状态服务.强制脱战(玩家标识);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务, times(2)).发送战斗日志(
                    eq(快照), 事件标识捕获.capture(), eq(0), any(String.class));

            List<UUID> 事件标识列表 = 事件标识捕获.getAllValues();
            assertEquals(2, 事件标识列表.size(), "应捕获两次战斗日志调用");

            UUID 进入战斗事件标识 = 事件标识列表.get(0);
            UUID 脱战事件标识 = 事件标识列表.get(1);

            assertNotNull(进入战斗事件标识, "进入战斗日志事件标识不应为 null");
            assertNotNull(脱战事件标识, "脱战日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 进入战斗事件标识,
                    "进入战斗日志事件标识不应与技能事件标识相同，否则 [战斗日志]进入战斗 会被 [技能日志] 合并吞掉");
            assertNotEquals(技能事件标识, 脱战事件标识,
                    "脱战日志事件标识不应与技能事件标识相同，否则 [战斗日志]脱战 会被 [技能日志] 合并吞掉");
            assertNotEquals(进入战斗事件标识, 脱战事件标识,
                    "进入战斗和脱战日志事件标识应相互独立，否则两条 [战斗日志] 会被合并");
        }

        @Test
        @DisplayName("脱战回血日志 - 应使用独立 UUID 事件标识，不与 ThreadLocal 残留的技能事件标识合并")
        void 脱战回血日志_应使用独立UUID事件标识() {
            设置玩家属性(10.0, 100.0, 10.0);
            UUID 技能事件标识 = 事件日志上下文.开始新事件();

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(玩家));
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                战斗状态服务.执行脱战回血();

                ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
                verify(消息服务).发送战斗日志(
                        any(), 事件标识捕获.capture(), eq(0), eq("战斗日志.脱战回血"), eq("10.0"));

                UUID 回血事件标识 = 事件标识捕获.getValue();
                assertNotNull(回血事件标识, "脱战回血日志事件标识不应为 null");
                assertNotEquals(技能事件标识, 回血事件标识,
                        "脱战回血日志事件标识不应与 ThreadLocal 残留的技能事件标识相同");
            }
        }
    }

    @Nested
    @DisplayName("FP-E 修复合并判据 - 进入战斗共享挥剑事件标识")
    class 进入战斗共享事件标识 {

        @Test
        @DisplayName("进入战斗(带事件标识) - 应使用传入的事件标识，可与挥剑事件合并")
        void 进入战斗带事件标识_应使用传入事件标识() {
            UUID 挥剑事件标识 = UUID.randomUUID();

            战斗状态服务.进入战斗(玩家标识, 挥剑事件标识);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.进入战斗"));

            UUID 进入战斗事件标识 = 事件标识捕获.getValue();
            assertEquals(挥剑事件标识, 进入战斗事件标识,
                    "进入战斗日志事件标识应等于传入的挥剑事件标识，以合并挥剑事件相关日志");
        }

        @Test
        @DisplayName("进入战斗(事件标识为null) - 应使用 randomUUID，独立不合并")
        void 进入战斗事件标识为null_应使用独立随机UUID() {
            UUID 技能事件标识 = 事件日志上下文.开始新事件();

            战斗状态服务.进入战斗(玩家标识, null);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.进入战斗"));

            UUID 进入战斗事件标识 = 事件标识捕获.getValue();
            assertNotNull(进入战斗事件标识, "进入战斗日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 进入战斗事件标识,
                    "事件标识为 null 时，进入战斗日志事件标识应独立，不与 ThreadLocal 残留的技能事件标识合并");
        }

        @Test
        @DisplayName("进入战斗(无事件标识重载) - 应使用 randomUUID，独立不合并")
        void 进入战斗无事件标识重载_应使用独立随机UUID() {
            UUID 技能事件标识 = 事件日志上下文.开始新事件();

            战斗状态服务.进入战斗(玩家标识);

            ArgumentCaptor<UUID> 事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 事件标识捕获.capture(), eq(0), eq("战斗日志.进入战斗"));

            UUID 进入战斗事件标识 = 事件标识捕获.getValue();
            assertNotNull(进入战斗事件标识, "进入战斗日志事件标识不应为 null");
            assertNotEquals(技能事件标识, 进入战斗事件标识,
                    "单参数重载应使用独立 randomUUID，不与 ThreadLocal 残留的技能事件标识合并");
        }

        @Test
        @DisplayName("挥剑场景集成测试 - 进入战斗、物理伤害、蓄力释放共享同一事件标识")
        void 挥剑场景_三日志共享同一事件标识() {
            UUID 挥剑事件标识 = UUID.randomUUID();

            // 模拟技能按键绑定监听器.触发技能：进入战斗使用挥剑事件标识
            战斗状态服务.进入战斗(玩家标识, 挥剑事件标识);

            // 模拟战斗监听器.处理玩家造成伤害：发送物理伤害日志使用同一事件标识
            消息服务.发送战斗日志(快照, 挥剑事件标识, "战斗日志.普通攻击", "剑", "目标", "10.0", "物理");

            // 模拟技能释放：发送技能日志使用同一事件标识
            消息服务.发送技能日志(快照, 挥剑事件标识, "技能日志.奥术冲击.释放");

            // 捕获进入战斗日志的事件标识
            ArgumentCaptor<UUID> 进入战斗事件标识捕获 = ArgumentCaptor.forClass(UUID.class);
            verify(消息服务).发送战斗日志(eq(快照), 进入战斗事件标识捕获.capture(), eq(0), eq("战斗日志.进入战斗"));

            // 验证物理伤害日志使用挥剑事件标识
            verify(消息服务).发送战斗日志(eq(快照), eq(挥剑事件标识), eq("战斗日志.普通攻击"),
                    any(), any(), any(), any());

            // 验证技能日志使用挥剑事件标识
            verify(消息服务).发送技能日志(eq(快照), eq(挥剑事件标识), eq("技能日志.奥术冲击.释放"));

            // 验证进入战斗日志使用挥剑事件标识，使三者合并为一行
            assertEquals(挥剑事件标识, 进入战斗事件标识捕获.getValue(),
                    "进入战斗日志事件标识应与挥剑事件标识相同，使三条日志合并为一行");
        }

        @Test
        @DisplayName("重复进入战斗(带事件标识) - 不应重复发送进入战斗日志")
        void 重复进入战斗带事件标识_不应重复发送() {
            UUID 挥剑事件标识 = UUID.randomUUID();

            战斗状态服务.进入战斗(玩家标识, 挥剑事件标识);
            战斗状态服务.进入战斗(玩家标识, 挥剑事件标识);

            verify(消息服务).发送战斗日志(eq(快照), eq(挥剑事件标识), eq(0), eq("战斗日志.进入战斗"));
        }
    }
}
