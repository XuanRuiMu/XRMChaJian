package mljy.基础设施层;

import org.bukkit.configuration.file.FileConfiguration;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * FP-BUG3 复现与回归测试：副属性全为0问题。
 *
 * 根因：部署目录 plugins/XRM/ 下缺少 专精属性.yml，saveResource 失败时
 * YamlConfiguration.loadConfiguration(不存在的文件) 返回空配置不抛异常，
 * 内存属性服务.加载专精基础属性() 中 根==null 静默 return，
 * 导致专精基础属性表为空，构建基础属性回退到兜底属性（副属性全0）。
 *
 * 修复：Yaml配置加载器.加载() 在外部文件不存在/为空时，从 jar 内资源回退加载。
 *
 * 本测试通过 mock JavaPlugin 模拟部署目录文件缺失场景，验证：
 * 1. 加载资源() 能从 jar 内资源（InputStream）加载配置
 * 2. 加载() 在外部文件不存在时能从 jar 内资源回退加载
 * 3. 回退加载的配置包含正确的专精属性（副属性非0）
 */
@DisplayName("FP-BUG3 Yaml配置加载器回退测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Yaml配置加载器测试 {

    @Mock
    private JavaPlugin 插件;

    @TempDir
    private Path 临时目录;

    private Path 源码资源路径;
    private Path 部署目录;

    @BeforeEach
    void setUp() throws Exception {
        源码资源路径 = new File("src/main/resources/专精属性.yml").toPath();
        部署目录 = 临时目录.resolve("XRM");
        Files.createDirectories(部署目录);

        // 模拟 JavaPlugin.getDataFolder() 返回部署目录
        when(插件.getDataFolder()).thenReturn(部署目录.toFile());
    }

    private InputStream 打开源码资源() throws Exception {
        return new FileInputStream(源码资源路径.toFile());
    }

    @Nested
    @DisplayName("加载资源：从 jar 内资源加载配置")
    class 加载资源测试 {

        @Test
        @DisplayName("getResource 返回 InputStream 时应正确加载配置")
        void getResource返回流_应正确加载配置() throws Exception {
            when(插件.getResource("专精属性.yml")).thenReturn(打开源码资源());

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            FileConfiguration 配置 = 加载器.加载资源("专精属性.yml");

            // 验证配置非空
            assertFalse(配置.getKeys(false).isEmpty(), "配置不应为空");
            // 验证包含专精属性
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.智力"),
                    "智力应为5.0");
            assertEquals(10.0, 配置.getDouble("专精.奥能法师.基础属性.法术暴击几率"),
                    "法术暴击几率应为10.0");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.全能"),
                    "全能应为5.0");
        }

        @Test
        @DisplayName("getResource 返回 null 时应返回空配置（不抛异常）")
        void getResource返回null_应返回空配置() {
            when(插件.getResource("不存在.yml")).thenReturn(null);

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            FileConfiguration 配置 = 加载器.加载资源("不存在.yml");

            assertTrue(配置.getKeys(false).isEmpty(), "资源不存在时应返回空配置");
        }
    }

    @Nested
    @DisplayName("加载：外部文件不存在时从 jar 内资源回退加载")
    class 加载回退测试 {

        @Test
        @DisplayName("外部文件不存在且 saveResource 失败时应从 jar 内资源回退加载")
        void 外部文件不存在_应从jar内资源回退加载() throws Exception {
            // 模拟 saveResource 失败（Mockito mock final 方法返回默认 void，不创建文件）
            // 文件不存在，getResource 返回源码资源 InputStream
            when(插件.getResource("专精属性.yml")).thenReturn(打开源码资源());

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            FileConfiguration 配置 = 加载器.加载("专精属性.yml");

            // 验证回退加载成功
            assertFalse(配置.getKeys(false).isEmpty(), "应从 jar 内资源回退加载配置");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.智力"),
                    "智力应为5.0");
            assertEquals(10.0, 配置.getDouble("专精.奥能法师.基础属性.法术暴击几率"),
                    "法术暴击几率应为10.0（非0）");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.法术暴击伤害"),
                    "法术暴击伤害应为5.0（非0）");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.全能"),
                    "全能应为5.0（非0）");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.吸血"),
                    "吸血应为5.0（非0）");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.躲闪"),
                    "躲闪应为5.0（非0）");
            assertEquals(10.0, 配置.getDouble("专精.奥能法师.基础属性.生命恢复"),
                    "生命恢复应为10.0（非0）");
            assertEquals(0.1, 配置.getDouble("专精.奥能法师.基础属性.移速"),
                    "移速应为0.1（非0）");
        }

        @Test
        @DisplayName("外部文件存在且非空时应直接加载外部文件")
        void 外部文件存在_应直接加载外部文件() throws Exception {
            // 复制源码资源到部署目录
            Path 目标文件 = 部署目录.resolve("专精属性.yml");
            Files.copy(源码资源路径, 目标文件, StandardCopyOption.REPLACE_EXISTING);

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            FileConfiguration 配置 = 加载器.加载("专精属性.yml");

            assertFalse(配置.getKeys(false).isEmpty(), "应加载外部文件");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.智力"),
                    "智力应为5.0");
        }

        @Test
        @DisplayName("外部文件存在但为空时应从 jar 内资源回退加载")
        void 外部文件为空_应从jar内资源回退加载() throws Exception {
            // 创建空文件
            Path 目标文件 = 部署目录.resolve("专精属性.yml");
            Files.createFile(目标文件);

            // getResource 返回源码资源
            when(插件.getResource("专精属性.yml")).thenReturn(打开源码资源());

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            FileConfiguration 配置 = 加载器.加载("专精属性.yml");

            // 验证回退加载成功
            assertFalse(配置.getKeys(false).isEmpty(), "空文件应触发从 jar 内资源回退加载");
            assertEquals(5.0, 配置.getDouble("专精.奥能法师.基础属性.智力"),
                    "智力应为5.0");
        }
    }

    @Nested
    @DisplayName("端到端：Yaml配置加载器 + 内存属性服务 回退加载验证")
    class 端到端回退加载测试 {

        @Test
        @DisplayName("外部文件缺失时内存属性服务应通过回退加载获得正确专精属性")
        void 外部文件缺失_内存属性服务应通过回退加载获得正确专精属性() throws Exception {
            // 模拟部署目录无 专精属性.yml，saveResource 失败
            when(插件.getResource("专精属性.yml")).thenReturn(打开源码资源());

            Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
            mljy.业务层.属性.修饰器管理器 修饰器管理器 = new mljy.业务层.属性.修饰器管理器();
            内存属性服务 属性服务 = new 内存属性服务(修饰器管理器, 加载器);

            // 验证专精基础属性表非空
            assertTrue(属性服务.获取专精基础属性("奥能法师").isPresent(),
                    "通过回退加载，奥能法师专精基础属性应存在");

            // 验证副属性非0
            mljy.领域层.属性.属性快照 属性 = 属性服务.获取专精基础属性("奥能法师").get();
            assertEquals(10.0, 属性.法术暴击几率(), "法术暴击几率应为10.0（非0）");
            assertEquals(5.0, 属性.法术暴击伤害(), "法术暴击伤害应为5.0（非0）");
            assertEquals(5.0, 属性.全能(), "全能应为5.0（非0）");
            assertEquals(5.0, 属性.吸血(), "吸血应为5.0（非0）");
            assertEquals(5.0, 属性.躲闪(), "躲闪应为5.0（非0）");
            assertEquals(10.0, 属性.生命恢复(), "生命恢复应为10.0（非0）");
            assertEquals(0.1, 属性.移速(), "移速应为0.1（非0）");
        }
    }
}
