package mljy.回归测试;

import mljy.基础设施层.Yaml配置加载器;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * 回归测试：Bug 3 修复 - 副属性全为0。
 *
 * Bug 3 根因：
 * 1. 部署目录缺 专精属性.yml
 * 2. saveResource 静默失败
 * 3. 三个静默点导致问题难以定位
 *
 * 修复：
 * 1. Yaml配置加载器.加载() 增加 jar 内资源回退加载
 * 2. 三处调试日志
 * 3. 删除旧版 jar
 *
 * 本测试验证 jar 内资源回退逻辑：当外部文件不存在时，应从 jar 内资源加载配置。
 */
@DisplayName("回归测试 - Bug 3：副属性全为0（Yaml配置加载器 jar 内资源回退）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 副属性非零回归测试 {

    @Mock
    private JavaPlugin 插件;

    @TempDir
    private File 临时目录;

    @Test
    @DisplayName("外部文件不存在时：应从 jar 内资源回退加载并返回非空配置")
    void 外部文件不存在_应从Jar内资源回退加载() {
        // 模拟外部文件不存在：getDataFolder 返回空临时目录
        when(插件.getDataFolder()).thenReturn(临时目录);
        // saveResource 静默失败（什么都不做）
        doNothing().when(插件).saveResource(anyString(), anyBoolean());
        // getResource 返回 jar 内资源（源码 resources/专精属性.yml）
        when(插件.getResource("专精属性.yml")).thenReturn(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("专精属性.yml")));

        Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
        FileConfiguration 配置 = 加载器.加载("专精属性.yml");

        assertFalse(配置.getKeys(false).isEmpty(),
                "jar 内资源回退加载应返回非空配置，实际：" + 配置.getKeys(false));
    }

    @Test
    @DisplayName("jar 内资源加载：奥能法师副属性应非0（除精通/急速按需求文档为0）")
    void Jar内资源加载_奥能法师副属性应非0() {
        when(插件.getDataFolder()).thenReturn(临时目录);
        doNothing().when(插件).saveResource(anyString(), anyBoolean());
        when(插件.getResource("专精属性.yml")).thenReturn(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("专精属性.yml")));

        Yaml配置加载器 加载器 = new Yaml配置加载器(插件);
        FileConfiguration 配置 = 加载器.加载("专精属性.yml");

        // 验证奥能法师的副属性非0（配置路径：专精.奥能法师.基础属性.xxx）
        double 法术暴击几率 = 配置.getDouble("专精.奥能法师.基础属性.法术暴击几率", -1);
        double 全能 = 配置.getDouble("专精.奥能法师.基础属性.全能", -1);
        double 吸血 = 配置.getDouble("专精.奥能法师.基础属性.吸血", -1);
        double 躲闪 = 配置.getDouble("专精.奥能法师.基础属性.躲闪", -1);

        assertTrue(法术暴击几率 > 0, "法术暴击几率应大于0，实际：" + 法术暴击几率);
        assertTrue(全能 > 0, "全能应大于0，实际：" + 全能);
        assertTrue(吸血 > 0, "吸血应大于0，实际：" + 吸血);
        assertTrue(躲闪 > 0, "躲闪应大于0，实际：" + 躲闪);
    }

    @Test
    @DisplayName("源码 resources/专精属性.yml 应存在且可加载")
    void 源码资源应存在且可加载() throws Exception {
        File 源文件 = new File("src/main/resources/专精属性.yml");
        assertTrue(源文件.exists(), "源码 resources/专精属性.yml 应存在");

        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(源文件.toPath()), StandardCharsets.UTF_8));

        assertFalse(配置.getKeys(false).isEmpty(),
                "源码 专精属性.yml 应可加载且非空，实际：" + 配置.getKeys(false));
        assertTrue(配置.contains("专精"),
                "应包含专精配置，实际：" + 配置.getKeys(false));
    }
}
