package mljy.基础设施层.乐器;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FP-14 资源包管理器测试（保底方案）。
 * 验证资源包生成、应用、配置加载与玩家状态管理。
 * <p>
 * 注意：调试日志器在未初始化时为 no-op，无需 mock。
 */
@DisplayName("FP-14: 资源包管理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 资源包管理器测试 {

    @Mock
    private JavaPlugin 插件mock;

    @Mock
    private Player 玩家mock;

    @TempDir
    private Path 临时目录;

    private 资源包管理器 管理器;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        when(插件mock.getDataFolder()).thenReturn(临时目录.toFile());
        管理器 = new 资源包管理器(插件mock);
        玩家标识 = UUID.randomUUID();
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
    }

    @Nested
    @DisplayName("初始状态")
    class 初始状态测试 {

        @Test
        @DisplayName("新构造管理器应未启用")
        void 新管理器未启用() {
            assertFalse(管理器.是否启用());
        }

        @Test
        @DisplayName("新构造管理器在空目录应未配置")
        void 新管理器未配置() {
            assertFalse(管理器.资源包已配置());
        }

        @Test
        @DisplayName("新管理器玩家未应用资源包")
        void 新管理器玩家未应用() {
            assertFalse(管理器.玩家已应用(玩家标识));
        }

        @Test
        @DisplayName("获取资源包根目录应非 null")
        void 获取根目录非Null() {
            File 根 = 管理器.获取资源包根目录();
            assertNotNull(根);
            assertTrue(根.getName().equals(资源包管理器.资源包目录名));
        }

        @Test
        @DisplayName("未生成时获取资源包文件应返回 null")
        void 未生成时获取文件Null() {
            assertNull(管理器.获取资源包文件());
        }
    }

    @Nested
    @DisplayName("配置加载")
    class 配置加载测试 {

        @Test
        @DisplayName("从配置加载 true 应启用资源包")
        void 加载true启用() {
            管理器.从配置加载(true);
            assertTrue(管理器.是否启用());
        }

        @Test
        @DisplayName("从配置加载 false 应禁用资源包")
        void 加载false禁用() {
            管理器.从配置加载(true);
            管理器.从配置加载(false);
            assertFalse(管理器.是否启用());
        }

        @Test
        @DisplayName("仅启用但未生成时资源包未配置")
        void 仅启用未生成未配置() {
            管理器.从配置加载(true);
            assertTrue(管理器.是否启用());
            assertFalse(管理器.资源包已配置());
        }
    }

    @Nested
    @DisplayName("生成资源包")
    class 生成资源包测试 {

        @Test
        @DisplayName("首次生成应返回 true 且创建目录结构")
        void 首次生成成功() {
            boolean 结果 = 管理器.生成资源包();
            assertTrue(结果);
            File 根 = 管理器.获取资源包根目录();
            assertTrue(根.exists());
            assertTrue(new File(根, 资源包管理器.pack元数据文件名).exists());
        }

        @Test
        @DisplayName("重复生成应返回 false（已存在）")
        void 重复生成返回false() {
            assertTrue(管理器.生成资源包());
            assertFalse(管理器.生成资源包(), "已存在时再次生成应返回 false");
        }

        @Test
        @DisplayName("生成后获取资源包文件应非 null")
        void 生成后获取文件非Null() {
            管理器.生成资源包();
            File 文件 = 管理器.获取资源包文件();
            assertNotNull(文件);
            assertTrue(文件.exists());
        }

        @Test
        @DisplayName("生成后 pack.mcmeta 应包含 pack_format 26")
        void 生成后pack元数据正确() throws IOException {
            管理器.生成资源包();
            File 根 = 管理器.获取资源包根目录();
            File 元数据 = new File(根, 资源包管理器.pack元数据文件名);
            String 内容 = java.nio.file.Files.readString(元数据.toPath());
            assertTrue(内容.contains("\"pack_format\": " + 资源包管理器.pack格式版本));
        }

        @Test
        @DisplayName("生成后 sounds.json 应存在")
        void 生成后soundsJson存在() {
            管理器.生成资源包();
            File 根 = 管理器.获取资源包根目录();
            File sounds定义 = new File(根, 资源包管理器.assets目录名 + File.separator
                    + 资源包管理器.minecraft目录名 + File.separator + 资源包管理器.sounds定义文件名);
            assertTrue(sounds定义.exists());
        }

        @Test
        @DisplayName("生成后 sounds/xrm 目录应存在")
        void 生成后soundsXrm目录存在() {
            管理器.生成资源包();
            File 根 = 管理器.获取资源包根目录();
            File xrm目录 = new File(根, 资源包管理器.assets目录名 + File.separator
                    + 资源包管理器.minecraft目录名 + File.separator
                    + 资源包管理器.sounds目录名 + File.separator
                    + 资源包管理器.xrm音色目录名);
            assertTrue(xrm目录.exists());
            assertTrue(xrm目录.isDirectory());
        }
    }

    @Nested
    @DisplayName("应用资源包")
    class 应用资源包测试 {

        @Test
        @DisplayName("null 玩家应返回 false")
        void null玩家返回false() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            assertFalse(管理器.应用资源包(null));
        }

        @Test
        @DisplayName("未启用资源包应返回 false")
        void 未启用返回false() {
            when(玩家mock.isOnline()).thenReturn(true);
            // 未调用 从配置加载(true)，默认未启用
            assertFalse(管理器.应用资源包(玩家mock));
        }

        @Test
        @DisplayName("离线玩家应返回 false")
        void 离线玩家返回false() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            when(玩家mock.isOnline()).thenReturn(false);
            assertFalse(管理器.应用资源包(玩家mock));
        }

        @Test
        @DisplayName("启用且在线且已生成应返回 true 并标记玩家已应用")
        void 正常应用成功() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            when(玩家mock.isOnline()).thenReturn(true);

            boolean 结果 = 管理器.应用资源包(玩家mock);
            assertTrue(结果);
            assertTrue(管理器.玩家已应用(玩家标识));
        }

        @Test
        @DisplayName("应用资源包应调用 setResourcePack")
        void 应用调用setResourcePack() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            when(玩家mock.isOnline()).thenReturn(true);

            管理器.应用资源包(玩家mock);
            verify(玩家mock).setResourcePack(anyString(), anyString());
        }

        @Test
        @DisplayName("启用但未生成时应自动生成后应用")
        void 未生成自动生成() {
            管理器.从配置加载(true);
            when(玩家mock.isOnline()).thenReturn(true);
            // 未手动生成，应用时应自动生成
            boolean 结果 = 管理器.应用资源包(玩家mock);
            assertTrue(结果);
            assertTrue(管理器.玩家已应用(玩家标识));
        }
    }

    @Nested
    @DisplayName("玩家状态管理")
    class 玩家状态测试 {

        @Test
        @DisplayName("玩家退出清理应清除应用状态")
        void 玩家退出清理() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            when(玩家mock.isOnline()).thenReturn(true);
            管理器.应用资源包(玩家mock);
            assertTrue(管理器.玩家已应用(玩家标识));

            管理器.玩家退出清理(玩家标识);
            assertFalse(管理器.玩家已应用(玩家标识));
        }

        @Test
        @DisplayName("null 玩家标识退出清理应无异常")
        void null标识清理无异常() {
            assertDoesNotThrow(() -> 管理器.玩家退出清理(null));
        }

        @Test
        @DisplayName("未应用玩家退出清理应无异常")
        void 未应用玩家清理无异常() {
            assertDoesNotThrow(() -> 管理器.玩家退出清理(UUID.randomUUID()));
        }

        @Test
        @DisplayName("null 玩家标识查询应返回 false")
        void null标识查询返回false() {
            assertFalse(管理器.玩家已应用(null));
        }
    }

    @Nested
    @DisplayName("资源包已配置")
    class 资源包已配置测试 {

        @Test
        @DisplayName("未启用且未生成应未配置")
        void 未启用未生成未配置() {
            assertFalse(管理器.资源包已配置());
        }

        @Test
        @DisplayName("启用但未生成应未配置")
        void 启用未生成未配置() {
            管理器.从配置加载(true);
            assertFalse(管理器.资源包已配置());
        }

        @Test
        @DisplayName("未启用但已生成应未配置")
        void 未启用已生成未配置() {
            管理器.生成资源包();
            assertFalse(管理器.资源包已配置());
        }

        @Test
        @DisplayName("启用且已生成应已配置")
        void 启用已生成已配置() {
            管理器.从配置加载(true);
            管理器.生成资源包();
            assertTrue(管理器.资源包已配置());
        }
    }

    @Nested
    @DisplayName("常量")
    class 常量测试 {

        @Test
        @DisplayName("资源包目录名应为 资源包")
        void 目录名() {
            assertEquals("资源包", 资源包管理器.资源包目录名);
        }

        @Test
        @DisplayName("pack 格式版本应为 26")
        void pack格式版本() {
            assertEquals(26, 资源包管理器.pack格式版本);
        }

        @Test
        @DisplayName("资源包文件名应为 xrm-resource-pack.zip")
        void 资源包文件名() {
            assertEquals("xrm-resource-pack.zip", 资源包管理器.资源包文件名);
        }
    }
}
