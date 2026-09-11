package mljy.表现层.命令.技能指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("技能命令处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能命令处理器测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private Player 玩家;
    @Mock
    private ConsoleCommandSender 控制台;
    @Mock
    private Command 命令;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 技能命令处理器 处理器;
    private YamlConfiguration 翻译文件;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/命令/技能指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        处理器 = new 技能命令处理器(翻译服务, 关键词解析器);

        when(玩家.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private void 加载翻译文件(String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                翻译文件.set(键, 配置.get(键));
            }
        }
    }

    private String 捕获消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者).sendMessage(捕获器.capture());
        return PlainTextComponentSerializer.plainText().serialize(捕获器.getValue());
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("玩家执行/技能 - 应发送菜单未配置消息")
        void 玩家执行技能命令_应发送菜单未配置消息() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "技能", new String[]{});

            assertTrue(结果);
            String 最终文本 = 捕获消息(玩家);
            assertTrue(最终文本.contains("技能菜单未配置"),
                    "应包含'技能菜单未配置'，实际: " + 最终文本);
        }

        @Test
        @DisplayName("获取命令名 - 应返回'技能'")
        void 获取命令名_应返回技能() {
            assertEquals("技能", 处理器.获取命令名());
        }
    }

    @Nested
    @DisplayName("异常输入 - 非玩家发送者")
    class 非玩家发送者 {

        @Test
        @DisplayName("控制台执行/技能 - 应发送仅玩家可用错误")
        void 控制台执行技能命令_应发送仅玩家可用错误() {
            boolean 结果 = 处理器.onCommand(控制台, 命令, "技能", new String[]{});

            assertTrue(结果);
            String 最终文本 = 捕获消息(控制台);
            assertTrue(最终文本.contains("[错误]"),
                    "应包含'[错误]'前缀，实际: " + 最终文本);
            assertTrue(最终文本.contains("仅玩家可用"),
                    "应包含'仅玩家可用'，实际: " + 最终文本);
        }
    }

    @Nested
    @DisplayName("玩家可见输出 - 验证最终中文文本")
    class 玩家可见输出 {

        @Test
        @DisplayName("玩家收到消息 - 应为中文文本而非翻译键")
        void 玩家收到消息_应为中文文本() {
            处理器.onCommand(玩家, 命令, "技能", new String[]{});

            String 最终文本 = 捕获消息(玩家);
            // 不应包含翻译键
            assertFalse(最终文本.contains("技能指令.菜单.未配置"),
                    "不应包含翻译键，实际: " + 最终文本);
            // 应包含中文文本
            assertTrue(最终文本.contains("技能菜单未配置"),
                    "应包含中文文本'技能菜单未配置'，实际: " + 最终文本);
        }

        @Test
        @DisplayName("玩家收到消息 - 应包含错误前缀")
        void 玩家收到消息_应包含错误前缀() {
            处理器.onCommand(玩家, 命令, "技能", new String[]{});

            String 最终文本 = 捕获消息(玩家);
            assertTrue(最终文本.contains("[错误]"),
                    "应包含'[错误]'前缀，实际: " + 最终文本);
        }
    }

    @Nested
    @DisplayName("Tab补全")
    class Tab补全 {

        @Test
        @DisplayName("Tab补全 - 应返回空列表")
        void Tab补全_应返回空列表() {
            var 结果 = 处理器.onTabComplete(玩家, 命令, "技能", new String[]{});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }
    }

    private static class 测试翻译服务 implements 翻译服务 {
        private final YamlConfiguration 配置;

        测试翻译服务(YamlConfiguration 配置) {
            this.配置 = 配置;
        }

        @Override
        public String 获取(String 键, Locale 语言, Object... 参数) {
            return 获取(键, 参数);
        }

        @Override
        public String 获取(String 键, Object... 参数) {
            String 文本 = 配置.getString(键);
            return 文本 != null ? 文本 : 键;
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
