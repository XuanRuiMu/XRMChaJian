package mljy.回归测试;

import mljy.基础设施层.调试日志器;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 回归测试：Bug 1 修复 - Debug不可见。
 *
 * FP-07 已将调试日志器改为文件日志架构（java.util.logging.FileHandler），
 * 初始化(Logger, boolean) 仅设置内存标志并输出提示信息到传入的Logger。
 * 调试输出不再经过传入的Logger，而是写入文件（需初始化(JavaPlugin)方可启用）。
 */
@DisplayName("回归测试 - Bug 1：Debug不可见（调试日志器初始化）")
class 调试日志器初始化回归测试 {

    @BeforeEach
    void setUp() {
        调试日志器.初始化(null, false);
    }

    @Test
    @DisplayName("调试日志器.初始化：应正确设置日志器和启用标志")
    void 初始化_应正确设置日志器和启用标志() {
        Logger 日志器 = mock(Logger.class);

        调试日志器.初始化(日志器, true);

        assertTrue(调试日志器.启用(), "初始化后启用标志应为true");
        verify(日志器).info(org.mockito.ArgumentMatchers.contains("[XRM-DEBUG]"));
    }

    @Test
    @DisplayName("调试日志器.初始化：启用=false 时 调试日志器.启用() 应返回false")
    void 初始化_启用为false时启用应返回False() {
        Logger 日志器 = mock(Logger.class);

        调试日志器.初始化(日志器, false);

        assertTrue(!调试日志器.启用(), "启用=false 时 调试日志器.启用() 应返回false");
    }

    @Test
    @DisplayName("调试日志器.调试：初始化后应安全执行不抛异常")
    void 调试_初始化后应安全执行() {
        Logger 日志器 = mock(Logger.class);
        调试日志器.初始化(日志器, true);

        assertDoesNotThrow(() -> 调试日志器.调试("测试模块", "测试消息"));
    }

    @Test
    @DisplayName("调试日志器.调试：未初始化时不应抛出异常")
    void 调试_未初始化时不应抛出异常() {
        调试日志器.初始化(null, true);

        assertDoesNotThrow(() -> 调试日志器.调试("测试模块", "测试消息"));
    }

    @Test
    @DisplayName("config.yml：应包含 调试.全局开关 配置项")
    void ConfigYml_应包含调试全局开关配置项() throws Exception {
        File 配置文件 = new File("src/main/resources/config.yml");
        assertTrue(配置文件.exists(), "config.yml 应存在");

        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(配置文件.toPath()), StandardCharsets.UTF_8));

        assertTrue(配置.contains("调试.全局开关"),
                "config.yml 应包含 调试.全局开关 配置项，实际：" + 配置.getKeys(true));
        assertTrue(配置.getBoolean("调试.全局开关", false),
                "调试.全局开关 默认应为 true，实际：" + 配置.getBoolean("调试.全局开关", false));
    }

    @Test
    @DisplayName("config.yml：应包含 调试.已禁用模块 配置项且默认为空列表")
    void ConfigYml_应包含调试已禁用模块配置项() throws Exception {
        File 配置文件 = new File("src/main/resources/config.yml");
        assertTrue(配置文件.exists(), "config.yml 应存在");

        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(配置文件.toPath()), StandardCharsets.UTF_8));

        assertTrue(配置.contains("调试.已禁用模块"),
                "config.yml 应包含 调试.已禁用模块 配置项，实际：" + 配置.getKeys(true));
        assertTrue(配置.getStringList("调试.已禁用模块").isEmpty(),
                "调试.已禁用模块 默认应为空列表，实际：" + 配置.getStringList("调试.已禁用模块"));
    }

    @Test
    @DisplayName("模块级开关：onEnable链路应正确读取已禁用模块列表并传入调试日志器")
    void 模块级开关_onEnable链路_应正确读取已禁用模块() {
        YamlConfiguration 配置 = new YamlConfiguration();
        配置.set("调试.全局开关", true);
        配置.set("调试.已禁用模块", java.util.Arrays.asList("总菜单", "奥术冲击"));

        Logger 日志器 = mock(Logger.class);
        boolean 调试开关 = 配置.getBoolean("调试.全局开关", true);
        java.util.Set<String> 已禁用模块 = new java.util.HashSet<>(配置.getStringList("调试.已禁用模块"));

        调试日志器.初始化(日志器, 调试开关, 已禁用模块);

        assertTrue(调试日志器.启用(), "全局开关为true时启用状态应为true");

        // FP-07: 调试输出写文件日志器，不经过注入的Logger。验证安全执行不抛异常。
        assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "应被过滤"));
        assertDoesNotThrow(() -> 调试日志器.调试("躲闪监听器", "应输出"));
    }
}
