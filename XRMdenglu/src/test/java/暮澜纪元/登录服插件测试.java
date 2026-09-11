package 暮澜纪元;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import 暮澜纪元.数据.数据库连接管理器;
import 暮澜纪元.配置.消息管理器;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("登录服插件")
@SuppressWarnings("null")
class 登录服插件测试 {

    @AfterEach
    void 清理() throws Exception {
        设置静态字段("实例", null);
        设置静态字段("外部插件实例", null);
    }

    @Nested
    @DisplayName("静态方法")
    class 静态方法 {

        @Test
        @DisplayName("获取实例_启动前应为null")
        void 获取实例_启动前应为null() {
            assertNull(登录服插件.获取实例());
        }

        @Test
        @DisplayName("设置外部实例_应设置静态引用")
        void 设置外部实例_应设置静态引用() {
            JavaPlugin mock插件 = mock(JavaPlugin.class);
            登录服插件.设置外部实例(mock插件);
            登录服插件 插件 = new 登录服插件();
            assertSame(mock插件, 插件.获取JavaPlugin());
        }

        @Test
        @DisplayName("获取JavaPlugin_应返回外部实例")
        void 获取JavaPlugin_应返回外部实例() {
            JavaPlugin mock插件 = mock(JavaPlugin.class);
            登录服插件.设置外部实例(mock插件);
            登录服插件 插件 = new 登录服插件();
            assertSame(mock插件, 插件.获取JavaPlugin());
        }
    }

    @Nested
    @DisplayName("启动流程")
    class 启动流程 {

        @Test
        @DisplayName("启动_数据库初始化失败_应禁用插件")
        void 启动_数据库初始化失败_应禁用插件() {
            MockedConstruction<数据库连接管理器> mock构造 = mockConstruction(数据库连接管理器.class,
                    (mock, context) -> when(mock.初始化()).thenReturn(false));
            try {
                JavaPlugin mock插件 = 创建MockJava插件();
                登录服插件 插件 = new 登录服插件();
                插件.启动(mock插件);
                verify(mock插件.getServer().getPluginManager()).disablePlugin(mock插件);
            } finally {
                mock构造.close();
            }
        }
    }

    @Nested
    @DisplayName("关闭流程")
    class 关闭流程 {

        @Test
        @DisplayName("关闭_应清空单例实例")
        void 关闭_应清空单例实例() throws Exception {
            登录服插件 插件 = new 登录服插件();
            设置实例字段(插件, "消息", mock(消息管理器.class));
            设置静态字段("实例", 插件);
            插件.关闭();
            assertNull(登录服插件.获取实例());
        }

        @Test
        @DisplayName("关闭_外部插件实例为null_不应抛异常")
        void 关闭_外部插件实例为null_不应抛异常() throws Exception {
            登录服插件 插件 = new 登录服插件();
            设置实例字段(插件, "消息", mock(消息管理器.class));
            assertDoesNotThrow(() -> 插件.关闭());
        }

        @Test
        @DisplayName("关闭_数据库为null_不应抛异常")
        void 关闭_数据库为null_不应抛异常() throws Exception {
            JavaPlugin mock插件 = mock(JavaPlugin.class);
            Server mockServer = mock(Server.class);
            Messenger mockMessenger = mock(Messenger.class);
            when(mock插件.getServer()).thenReturn(mockServer);
            when(mockServer.getMessenger()).thenReturn(mockMessenger);
            登录服插件 插件 = new 登录服插件();
            设置实例字段(插件, "消息", mock(消息管理器.class));
            设置实例字段(插件, "外部插件实例", mock插件);
            assertDoesNotThrow(() -> 插件.关闭());
        }

        @Test
        @DisplayName("关闭_应注销BungeeCord通道")
        void 关闭_应注销BungeeCord通道() throws Exception {
            JavaPlugin mock插件 = mock(JavaPlugin.class);
            Messenger mockMessenger = mock(Messenger.class);
            Server mockServer = mock(Server.class);
            when(mock插件.getServer()).thenReturn(mockServer);
            when(mockServer.getMessenger()).thenReturn(mockMessenger);
            登录服插件 插件 = new 登录服插件();
            设置实例字段(插件, "消息", mock(消息管理器.class));
            设置实例字段(插件, "外部插件实例", mock插件);
            插件.关闭();
            verify(mockMessenger).unregisterOutgoingPluginChannel(mock插件);
        }
    }

    @Nested
    @DisplayName("FP-05 配置同步白盒")
    class 配置同步白盒 {

        @Test
        @DisplayName("FP-05 登录服插件应包含同步数据库配置方法")
        void 登录服插件_应包含同步数据库配置方法() throws Exception {
            String 源码 = Files.readString(Path.of("src/main/java/暮澜纪元/登录服插件.java"), StandardCharsets.UTF_8);
            assertTrue(源码.contains("private void 同步数据库配置到运行时config()"), "应定义同步方法");
            assertTrue(源码.contains("String 源码数据库名 = \"燃烧之陨\""), "应包含源码数据库名默认值");
            assertTrue(源码.contains("String 源码数据库密码 = \"BXYXblupz542284\""), "应包含源码数据库密码默认值");
            assertTrue(源码.contains("同步数据库配置到运行时config();"), "应在 onEnable 中调用同步方法");
            assertTrue(源码.contains("[XRMdenglu-Config同步]"), "日志前缀应为 [XRMdenglu-Config同步]");
        }

        @Test
        @DisplayName("FP-05 同步方法应在数据库连接管理器初始化之前调用")
        void 同步方法_应在数据库初始化之前调用() throws Exception {
            String 源码 = Files.readString(Path.of("src/main/java/暮澜纪元/登录服插件.java"), StandardCharsets.UTF_8);
            int 同步方法位置 = 源码.indexOf("同步数据库配置到运行时config();");
            int 数据库初始化位置 = 源码.indexOf("new 数据库连接管理器");
            if (数据库初始化位置 < 0) {
                数据库初始化位置 = 源码.indexOf("数据库连接管理器(");
            }
            assertTrue(同步方法位置 >= 0, "应包含同步方法调用");
            assertTrue(数据库初始化位置 >= 0, "应包含数据库连接管理器初始化");
            assertTrue(同步方法位置 < 数据库初始化位置, "同步方法必须在数据库连接管理器初始化之前调用");
        }
    }

    private JavaPlugin 创建MockJava插件() {
        JavaPlugin mock插件 = mock(JavaPlugin.class);
        Server mockServer = mock(Server.class);
        PluginManager mockPluginManager = mock(PluginManager.class);
        Messenger mockMessenger = mock(Messenger.class);
        PluginMeta mockMeta = mock(PluginMeta.class);
        FileConfiguration mockConfig = mock(FileConfiguration.class);
        when(mock插件.getServer()).thenReturn(mockServer);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        when(mockServer.getMessenger()).thenReturn(mockMessenger);
        when(mock插件.getPluginMeta()).thenReturn(mockMeta);
        when(mockMeta.getVersion()).thenReturn("1.0");
        when(mock插件.getDataFolder()).thenReturn(new File("test"));
        when(mock插件.getConfig()).thenReturn(mockConfig);
        when(mock插件.getLogger()).thenReturn(Logger.getLogger("test"));
        when(mockConfig.getBoolean("调试模式", false)).thenReturn(false);
        when(mockConfig.contains("传送门")).thenReturn(false);
        when(mockConfig.contains("重生点")).thenReturn(false);
        when(mockConfig.getString("目标服务器", "mmorpg")).thenReturn("mmorpg");
        return mock插件;
    }

    private void 设置实例字段(Object 目标, String 字段名, Object 值) throws Exception {
        Field 字段 = 目标.getClass().getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(目标, 值);
    }

    private void 设置静态字段(String 字段名, Object 值) throws Exception {
        Field 字段 = 登录服插件.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(null, 值);
    }
}
