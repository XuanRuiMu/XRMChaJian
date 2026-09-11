package mljy.表现层.命令.总菜单指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.玩家服务;
import mljy.表现层.菜单.菜单服务;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FP-09 总菜单命令处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 总菜单命令处理器测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 菜单服务 菜单服务;
    @Mock
    private Player 玩家;
    @Mock
    private ConsoleCommandSender 控制台;
    @Mock
    private Command 命令;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 总菜单命令处理器 处理器;
    private YamlConfiguration 翻译文件;

    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        处理器 = new 总菜单命令处理器(翻译服务, 关键词解析器, 菜单服务);

        玩家标识 = UUID.randomUUID();
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("测试玩家");
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

    private List<String> 捕获所有消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
    }

    @Nested
    @DisplayName("正常流程 - 玩家执行/cd")
    class 玩家执行命令 {

        @Test
        @DisplayName("玩家执行/cd - 应调用菜单服务打开总菜单")
        void 玩家执行cd_应调用打开总菜单() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "cd", new String[]{});

            assertTrue(结果);
            verify(菜单服务).打开菜单(玩家, "总菜单");
        }

        @Test
        @DisplayName("获取命令名 - 应返回'cd'")
        void 获取命令名_应返回cd() {
            assertEquals("cd", 处理器.获取命令名());
        }

        @Test
        @DisplayName("玩家执行/菜单别名 - 应同样打开总菜单")
        void 玩家执行别名_应同样打开总菜单() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "菜单", new String[]{});

            assertTrue(结果);
            verify(菜单服务).打开菜单(玩家, "总菜单");
        }

        @Test
        @DisplayName("玩家执行/caidan别名 - 应同样打开总菜单")
        void 玩家执行caidan别名_应同样打开总菜单() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "caidan", new String[]{});

            assertTrue(结果);
            verify(菜单服务).打开菜单(玩家, "总菜单");
        }
    }

    @Nested
    @DisplayName("异常输入 - 非玩家发送者")
    class 非玩家发送者 {

        @Test
        @DisplayName("控制台执行/cd - 应发送仅玩家可用错误，不打开菜单")
        void 控制台执行cd_应发送仅玩家可用错误() {
            boolean 结果 = 处理器.onCommand(控制台, 命令, "cd", new String[]{});

            assertTrue(结果);
            List<String> 消息列表 = 捕获所有消息(控制台);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("[错误]") && m.contains("仅玩家可用")),
                    "应包含仅玩家可用错误，实际消息: " + 消息列表);
            verify(菜单服务, never()).打开菜单(any(), anyString());
        }
    }

    @Nested
    @DisplayName("Tab补全")
    class Tab补全 {

        @Test
        @DisplayName("Tab补全 - 应返回空列表")
        void Tab补全_应返回空列表() {
            var 结果 = 处理器.onTabComplete(玩家, 命令, "cd", new String[]{});

            assertNotNull(结果);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("Tab补全带参数 - 仍应返回空列表")
        void Tab补全带参数_应返回空列表() {
            var 结果 = 处理器.onTabComplete(玩家, 命令, "cd", new String[]{"任意"});

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
