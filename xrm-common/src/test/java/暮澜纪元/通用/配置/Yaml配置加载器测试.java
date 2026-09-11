package 暮澜纪元.通用.配置;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Yaml配置加载器")
class Yaml配置加载器测试 {

    private JavaPlugin 插件;
    private Yaml配置加载器 加载器;

    @BeforeEach
    void setUp() {
        插件 = mock(JavaPlugin.class);
        加载器 = new Yaml配置加载器(插件);
    }

    @Nested
    @DisplayName("Guice依赖注入回归")
    class Guice依赖注入回归 {

        @Test
        @DisplayName("构造器应有@Inject注解(防止XRM启动失败回归)")
        void 构造器_应有Inject注解() throws NoSuchMethodException {
            Constructor<Yaml配置加载器> 构造器 = Yaml配置加载器.class.getDeclaredConstructor(JavaPlugin.class);
            assertTrue(构造器.isAnnotationPresent(Inject.class),
                    "Yaml配置加载器构造器必须有@Inject注解，否则Guice无法创建实例导致XRM启动失败");
        }

        @Test
        @DisplayName("构造器参数应为JavaPlugin")
        void 构造器_参数应为JavaPlugin() {
            Constructor<?>[] 构造器列表 = Yaml配置加载器.class.getDeclaredConstructors();
            assertEquals(1, 构造器列表.length, "应只有1个构造器");
            Class<?>[] 参数类型 = 构造器列表[0].getParameterTypes();
            assertEquals(1, 参数类型.length, "构造器应有1个参数");
            assertEquals(JavaPlugin.class, 参数类型[0], "构造器参数应为JavaPlugin");
        }
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("加载已存在的文件应返回配置内容")
        void 加载已存在文件_应返回配置(@TempDir Path 临时目录) throws Exception {
            File 数据目录 = 临时目录.toFile();
            File 配置文件 = new File(数据目录, "test.yml");
            java.nio.file.Files.write(配置文件.toPath(),
                    "key: value".getBytes(StandardCharsets.UTF_8));
            when(插件.getDataFolder()).thenReturn(数据目录);

            FileConfiguration 配置 = 加载器.加载("test.yml");

            assertEquals("value", 配置.getString("key"));
            verify(插件, never()).saveResource(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("加载资源应返回配置内容")
        void 加载资源_应返回配置() {
            String yaml内容 = "key: 资源值";
            InputStream 流 = new ByteArrayInputStream(yaml内容.getBytes(StandardCharsets.UTF_8));
            when(插件.getResource("resource.yml")).thenReturn(流);

            FileConfiguration 配置 = 加载器.加载资源("resource.yml");

            assertEquals("资源值", 配置.getString("key"));
        }

        @Test
        @DisplayName("获取文件应返回数据目录下的文件")
        void 获取文件_应返回正确路径(@TempDir Path 临时目录) {
            File 数据目录 = 临时目录.toFile();
            when(插件.getDataFolder()).thenReturn(数据目录);

            File 文件 = 加载器.获取文件("config.yml");

            assertEquals(new File(数据目录, "config.yml"), 文件);
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("加载不存在的文件应触发保存默认文件")
        void 加载不存在文件_应触发保存默认(@TempDir Path 临时目录) {
            File 数据目录 = 临时目录.toFile();
            when(插件.getDataFolder()).thenReturn(数据目录);

            FileConfiguration 配置 = 加载器.加载("missing.yml");

            verify(插件).saveResource("missing.yml", false);
            assertNotNull(配置);
        }

        @Test
        @DisplayName("加载资源时资源不存在应返回空配置")
        void 加载资源_资源不存在_应返回空配置(@TempDir Path 临时目录) {
            File 数据目录 = 临时目录.toFile();
            when(插件.getDataFolder()).thenReturn(数据目录);
            when(插件.getResource("null.yml")).thenReturn(null);

            FileConfiguration 配置 = 加载器.加载资源("null.yml");

            assertNotNull(配置);
            assertNull(配置.getString("anyKey"));
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("保存默认文件时父目录不存在应创建")
        void 保存默认文件_父目录不存在_应创建(@TempDir Path 临时目录) {
            File 数据目录 = 临时目录.toFile();
            when(插件.getDataFolder()).thenReturn(数据目录);

            加载器.保存默认文件("subdir/file.yml");

            assertTrue(new File(数据目录, "subdir").exists());
            verify(插件).saveResource("subdir/file.yml", false);
        }
    }
}
