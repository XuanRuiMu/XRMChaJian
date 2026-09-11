package 暮澜纪元.配置;

import 暮澜纪元.登录服插件;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("配置管理器")
@SuppressWarnings("null")
class 配置管理器测试 {

    private 登录服插件 mock插件;
    private FileConfiguration mock配置;
    private 配置管理器 管理器;

    @BeforeEach
    void 准备() {
        mock插件 = mock(登录服插件.class);
        mock配置 = mock(FileConfiguration.class);
        when(mock插件.getConfig()).thenReturn(mock配置);
        管理器 = new 配置管理器(mock插件);
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("获取配置应返回插件配置")
        void 获取配置_应返回插件配置() {
            assertEquals(mock配置, 管理器.获取配置());
        }

        @Test
        @DisplayName("刷新缓存应重新获取插件配置")
        void 刷新缓存_应重新获取插件配置() {
            FileConfiguration 新配置 = mock(FileConfiguration.class);
            when(mock插件.getConfig()).thenReturn(新配置);
            管理器.刷新缓存();
            verify(mock插件, atLeast(2)).getConfig();
        }

        @Test
        @DisplayName("调试模式应返回配置值")
        void 调试模式_应返回配置值() {
            when(mock配置.getBoolean("debug", false)).thenReturn(true);
            assertTrue(管理器.调试模式开启());
        }

        @Test
        @DisplayName("目标服务器应返回配置值")
        void 目标服务器_应返回配置值() {
            when(mock配置.getString("目标服务器", "mmorpg")).thenReturn("survival");
            assertEquals("survival", 管理器.目标服务器());
        }

        @Test
        @DisplayName("数据库主机应返回配置值")
        void 数据库主机_应返回配置值() {
            when(mock配置.getString("数据库.主机", "localhost")).thenReturn("192.168.1.1");
            assertEquals("192.168.1.1", 管理器.数据库主机());
        }

        @Test
        @DisplayName("数据库端口应返回配置值")
        void 数据库端口_应返回配置值() {
            when(mock配置.getInt("数据库.端口", 3306)).thenReturn(3307);
            assertEquals(3307, 管理器.数据库端口());
        }

        @Test
        @DisplayName("数据库名称应返回配置值")
        void 数据库名称_应返回配置值() {
            when(mock配置.getString("数据库.数据库名", "燃烧之陨")).thenReturn("测试库");
            assertEquals("测试库", 管理器.数据库名称());
        }

        @Test
        @DisplayName("数据库用户名应返回配置值")
        void 数据库用户名_应返回配置值() {
            when(mock配置.getString("数据库.用户名", "root")).thenReturn("admin");
            assertEquals("admin", 管理器.数据库用户名());
        }

        @Test
        @DisplayName("数据库密码应返回配置值")
        void 数据库密码_应返回配置值() {
            when(mock配置.getString("数据库.密码", "BXYXblupz542284")).thenReturn("secret");
            assertEquals("secret", 管理器.数据库密码());
        }

        @Test
        @DisplayName("数据库连接字符串应正确拼接")
        void 数据库连接字符串_应正确拼接() {
            when(mock配置.getString("数据库.主机", "localhost")).thenReturn("db.example.com");
            when(mock配置.getInt("数据库.端口", 3306)).thenReturn(3307);
            when(mock配置.getString("数据库.数据库名", "燃烧之陨")).thenReturn("mydb");
            String 连接串 = 管理器.数据库连接字符串();
            assertTrue(连接串.contains("jdbc:mysql://db.example.com:3307/mydb"));
            assertTrue(连接串.contains("useSSL=false"));
            assertTrue(连接串.contains("characterEncoding=UTF-8"));
        }

        @Test
        @DisplayName("保存配置应调用插件saveConfig")
        void 保存配置_应调用插件saveConfig() {
            管理器.保存配置();
            verify(mock插件).saveConfig();
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("数据库端口默认值应正确")
        void 数据库端口_默认值应正确() {
            when(mock配置.getInt("数据库.端口", 3306)).thenReturn(3306);
            assertEquals(3306, 管理器.数据库端口());
        }

        @Test
        @DisplayName("数据库连接字符串默认值应正确拼接")
        void 数据库连接字符串_默认值应正确拼接() {
            when(mock配置.getString("数据库.主机", "localhost")).thenReturn("localhost");
            when(mock配置.getInt("数据库.端口", 3306)).thenReturn(3306);
            when(mock配置.getString("数据库.数据库名", "燃烧之陨")).thenReturn("燃烧之陨");
            String 连接串 = 管理器.数据库连接字符串();
            assertTrue(连接串.startsWith("jdbc:mysql://localhost:3306/燃烧之陨"));
        }

        @Test
        @DisplayName("数据库主机空字符串应返回空字符串")
        void 数据库主机_空字符串应返回空字符串() {
            when(mock配置.getString("数据库.主机", "localhost")).thenReturn("");
            assertEquals("", 管理器.数据库主机());
        }

        @Test
        @DisplayName("数据库端口负值应返回负值")
        void 数据库端口_负值应返回负值() {
            when(mock配置.getInt("数据库.端口", 3306)).thenReturn(-1);
            assertEquals(-1, 管理器.数据库端口());
        }
    }
}
