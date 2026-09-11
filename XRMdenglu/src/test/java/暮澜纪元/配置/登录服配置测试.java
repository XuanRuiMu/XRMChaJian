package 暮澜纪元.配置;

import 暮澜纪元.传送门区域;
import 暮澜纪元.登录服插件;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("登录服配置")
class 登录服配置测试 {

    private 登录服插件 mock插件;
    private FileConfiguration mock配置;
    private 登录服配置 登录配置;

    @BeforeEach
    void 准备() {
        mock插件 = mock(登录服插件.class);
        mock配置 = mock(FileConfiguration.class);
        when(mock插件.getConfig()).thenReturn(mock配置);
        when(mock插件.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        登录配置 = new 登录服配置(mock插件);
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("加载配置后目标服务器应正确")
        void 加载配置_目标服务器应正确() {
            when(mock配置.contains("传送门")).thenReturn(false);
            when(mock配置.contains("重生点")).thenReturn(false);
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("survival");
            when(mock配置.getBoolean("调试模式", false)).thenReturn(false);
            登录配置.加载配置();
            assertEquals("survival", 登录配置.获取目标服务器());
        }

        @Test
        @DisplayName("调试模式应返回配置值")
        void 调试模式_应返回配置值() {
            when(mock配置.contains("传送门")).thenReturn(false);
            when(mock配置.contains("重生点")).thenReturn(false);
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("mmorpg");
            when(mock配置.getBoolean("调试模式", false)).thenReturn(true);
            登录配置.加载配置();
            assertTrue(登录配置.是否调试模式());
        }

        @Test
        @DisplayName("无传送门配置时传送门列表应为空")
        void 无传送门配置_传送门列表应为空() {
            when(mock配置.contains("传送门")).thenReturn(false);
            when(mock配置.contains("重生点")).thenReturn(false);
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("mmorpg");
            when(mock配置.getBoolean("调试模式", false)).thenReturn(false);
            登录配置.加载配置();
            assertTrue(登录配置.获取传送门列表().isEmpty());
        }

        @Test
        @DisplayName("无重生点配置时获取重生点应返回null")
        void 无重生点配置_获取重生点应返回null() {
            when(mock配置.contains("传送门")).thenReturn(false);
            when(mock配置.contains("重生点")).thenReturn(false);
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("mmorpg");
            when(mock配置.getBoolean("调试模式", false)).thenReturn(false);
            登录配置.加载配置();
            assertNull(登录配置.获取重生点());
        }

        @Test
        @DisplayName("设置传送门区域应存入列表")
        void 设置传送门区域_应存入列表() {
            传送门区域 区域 = new 传送门区域("test", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            登录配置.设置传送门区域("test", 区域);
            assertNotNull(登录配置.获取传送门区域("test"));
            assertEquals("test", 登录配置.获取传送门区域("test").获取传送门ID());
        }

        @Test
        @DisplayName("移除传送门区域应从列表删除")
        void 移除传送门区域_应从列表删除() {
            传送门区域 区域 = new 传送门区域("test", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            登录配置.设置传送门区域("test", 区域);
            登录配置.移除传送门区域("test");
            assertNull(登录配置.获取传送门区域("test"));
        }

        @Test
        @DisplayName("设置重生点应正确存储")
        void 设置重生点_应正确存储() {
            登录配置.设置重生点("world", 100.0, 64.0, -50.0, 90.0f, 45.0f);
            重生点配置 重生点 = 登录配置.获取重生点();
            assertNotNull(重生点);
            assertEquals("world", 重生点.获取世界名());
            assertEquals(100.0, 重生点.获取坐标X(), 0.001);
            assertEquals(64.0, 重生点.获取坐标Y(), 0.001);
            assertEquals(-50.0, 重生点.获取坐标Z(), 0.001);
        }

        @Test
        @DisplayName("保存配置应调用插件saveConfig")
        void 保存配置_应调用插件saveConfig() {
            登录配置.保存配置();
            verify(mock插件).saveConfig();
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("获取不存在的传送门区域应返回null")
        void 获取不存在的传送门区域_应返回null() {
            assertNull(登录配置.获取传送门区域("不存在"));
        }

        @Test
        @DisplayName("默认目标服务器应为mmorpg")
        void 默认目标服务器_应为mmorpg() {
            when(mock配置.contains("传送门")).thenReturn(false);
            when(mock配置.contains("重生点")).thenReturn(false);
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("mmorpg");
            when(mock配置.getBoolean("调试模式", false)).thenReturn(false);
            登录配置.加载配置();
            assertEquals("mmorpg", 登录配置.获取目标服务器());
        }
    }
}
