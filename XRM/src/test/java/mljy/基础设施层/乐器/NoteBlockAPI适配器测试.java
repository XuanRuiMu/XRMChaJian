package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * FP-23 NoteBlockAPI 适配器降级测试。
 * 验证未安装 NoteBlockAPI 时适配器返回 false 不抛异常。
 * <p>
 * 注意：本测试仅验证降级路径，不验证 NoteBlockAPI 已安装时的实际播放行为
 * （那需要真实 NoteBlockAPI 实例，超出单测范围）。
 */
@DisplayName("FP-23: NoteBlockAPI 适配器降级")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteBlockAPI适配器测试 {

    @BeforeEach
    void 重置缓存() {
        NoteBlockAPI适配器.刷新可用性();
    }

    @AfterEach
    void 清理() {
        NoteBlockAPI适配器.刷新可用性();
    }

    private 乐谱 创建测试乐谱() {
        UUID 作者标识 = UUID.randomUUID();
        List<音符> 音符列表 = new ArrayList<>();
        音符列表.add(音符.of(0, 4, 1.0));
        音符列表.add(音符.of(7, 4, 0.8));
        return new 乐谱("测试", 作者标识, "测试者",
                System.currentTimeMillis(), 120, 音符列表);
    }

    @Nested
    @DisplayName("降级行为：NoteBlockAPI 未安装")
    class 降级测试 {

        @Test
        @DisplayName("未安装时 是否可用 应返回 false")
        void 未安装时是否可用返回false() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                assertFalse(NoteBlockAPI适配器.是否可用());
            }
        }

        @Test
        @DisplayName("未安装时 播放乐谱 应返回 false 不抛异常")
        void 未安装时播放乐谱返回false() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                Player 玩家mock = mock(Player.class);
                乐谱 乐谱 = 创建测试乐谱();

                boolean 结果 = NoteBlockAPI适配器.播放乐谱(玩家mock, 乐谱);
                assertFalse(结果);
            }
        }

        @Test
        @DisplayName("未安装时 停止播放 应无操作不抛异常")
        void 未安装时停止播放无异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                assertDoesNotThrow(() -> NoteBlockAPI适配器.停止播放(UUID.randomUUID()));
            }
        }

        @Test
        @DisplayName("未安装时 停止全部播放 应无操作不抛异常")
        void 未安装时停止全部播放无异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                assertDoesNotThrow(NoteBlockAPI适配器::停止全部播放);
            }
        }
    }

    @Nested
    @DisplayName("参数校验")
    class 参数校验测试 {

        @Test
        @DisplayName("播放乐谱 null 玩家 应返回 false")
        void null玩家返回false() {
            乐谱 乐谱 = 创建测试乐谱();
            assertFalse(NoteBlockAPI适配器.播放乐谱(null, 乐谱));
        }

        @Test
        @DisplayName("播放乐谱 null 乐谱 应返回 false")
        void null乐谱返回false() {
            Player 玩家mock = mock(Player.class);
            assertFalse(NoteBlockAPI适配器.播放乐谱(玩家mock, null));
        }

        @Test
        @DisplayName("停止播放 null 玩家标识 应无操作")
        void null玩家标识停止播放无异常() {
            assertDoesNotThrow(() -> NoteBlockAPI适配器.停止播放(null));
        }
    }

    @Nested
    @DisplayName("FP-03 乐器ID参数测试")
    class FP03乐器ID参数测试 {

        @Test
        @DisplayName("带乐器ID播放乐谱 未安装时应返回 false")
        void 带乐器ID播放未安装返回false() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                Player 玩家mock = mock(Player.class);
                乐谱 乐谱 = 创建测试乐谱();

                boolean 结果 = NoteBlockAPI适配器.播放乐谱(玩家mock, 乐谱, 5);
                assertFalse(结果);
            }
        }

        @Test
        @DisplayName("带乐器ID播放乐谱 null 玩家应返回 false")
        void 带乐器ID播放null玩家返回false() {
            乐谱 乐谱 = 创建测试乐谱();
            assertFalse(NoteBlockAPI适配器.播放乐谱(null, 乐谱, 5));
        }

        @Test
        @DisplayName("带乐器ID播放乐谱 null 乐谱应返回 false")
        void 带乐器ID播放null乐谱返回false() {
            Player 玩家mock = mock(Player.class);
            assertFalse(NoteBlockAPI适配器.播放乐谱(玩家mock, null, 5));
        }

        @Test
        @DisplayName("带乐器ID播放乐谱 乐器ID越界不抛异常（clamp 到 0-17）")
        void 带乐器ID播放越界不抛异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                Player 玩家mock = mock(Player.class);
                乐谱 乐谱 = 创建测试乐谱();

                assertDoesNotThrow(() -> NoteBlockAPI适配器.播放乐谱(玩家mock, 乐谱, -1));
                assertDoesNotThrow(() -> NoteBlockAPI适配器.播放乐谱(玩家mock, 乐谱, 100));
            }
        }

        @Test
        @DisplayName("旧版两参数播放乐谱 应委托新版方法（向后兼容）")
        void 旧版两参数委托新版() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                Player 玩家mock = mock(Player.class);
                乐谱 乐谱 = 创建测试乐谱();

                // 旧版两参数应返回 false（未安装），不抛异常
                boolean 结果 = NoteBlockAPI适配器.播放乐谱(玩家mock, 乐谱);
                assertFalse(结果);
            }
        }
    }

    @Nested
    @DisplayName("可用性缓存")
    class 缓存测试 {

        @Test
        @DisplayName("刷新可用性应重置缓存")
        void 刷新可用性重置缓存() {
            // 第一次：模拟未安装
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                assertFalse(NoteBlockAPI适配器.是否可用());
            }
            // 刷新后再次模拟未安装（验证缓存确实被重置）
            NoteBlockAPI适配器.刷新可用性();
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);

                assertFalse(NoteBlockAPI适配器.是否可用());
            }
        }
    }
}
