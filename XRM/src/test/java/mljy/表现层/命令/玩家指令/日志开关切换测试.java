package mljy.表现层.命令.玩家指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.数据服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.领域层.玩家.玩家会话;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FP-09 日志开关切换 - bug3 修复验证（直接方法调用）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 日志开关切换测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 数据服务 数据服务;
    @Mock
    private Player 玩家;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 技能日志命令处理器 技能日志处理器;
    private 战斗日志命令处理器 战斗日志处理器;
    private YamlConfiguration 翻译文件;
    private UUID 玩家标识;
    private 玩家会话 会话;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/命令/玩家指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        技能日志处理器 = new 技能日志命令处理器(翻译服务, 关键词解析器, 玩家服务, 数据服务);
        战斗日志处理器 = new 战斗日志命令处理器(翻译服务, 关键词解析器, 玩家服务, 数据服务);

        玩家标识 = UUID.randomUUID();
        会话 = new 玩家会话(玩家标识, "测试玩家");
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("测试玩家");
        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
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

    @Nested
    @DisplayName("bug3修复 - 技能日志直接方法调用")
    class 技能日志切换 {

        @Test
        @DisplayName("切换开关_玩家_true - 应设置会话技能日志开关并保存")
        void 切换开关_玩家_true_应设置会话并保存() {
            会话.设置技能日志开关(false);

            技能日志处理器.切换开关(玩家, true);

            assertTrue(会话.获取技能日志开关(), "会话技能日志开关应为true");
            verify(数据服务).保存(会话);
        }

        @Test
        @DisplayName("切换开关_玩家_false - 应设置会话技能日志开关并保存")
        void 切换开关_玩家_false_应设置会话并保存() {
            会话.设置技能日志开关(true);

            技能日志处理器.切换开关(玩家, false);

            assertFalse(会话.获取技能日志开关(), "会话技能日志开关应为false");
            verify(数据服务).保存(会话);
        }

        @Test
        @DisplayName("切换开关_玩家_true - 应发送设置成功消息")
        void 切换开关_玩家_true_应发送设置成功消息() {
            技能日志处理器.切换开关(玩家, true);

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("技能日志") && m.contains("开启")),
                    "应包含技能日志开启消息，实际: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("bug3修复 - 战斗日志直接方法调用")
    class 战斗日志切换 {

        @Test
        @DisplayName("切换开关_玩家_true - 应设置会话战斗日志开关并保存")
        void 切换开关_玩家_true_应设置会话并保存() {
            会话.设置战斗日志开关(false);

            战斗日志处理器.切换开关(玩家, true);

            assertTrue(会话.获取战斗日志开关(), "会话战斗日志开关应为true");
            verify(数据服务).保存(会话);
        }

        @Test
        @DisplayName("切换开关_玩家_false - 应设置会话战斗日志开关并保存")
        void 切换开关_玩家_false_应设置会话并保存() {
            会话.设置战斗日志开关(true);

            战斗日志处理器.切换开关(玩家, false);

            assertFalse(会话.获取战斗日志开关(), "会话战斗日志开关应为false");
            verify(数据服务).保存(会话);
        }

        @Test
        @DisplayName("切换开关_玩家_true - 应发送设置成功消息")
        void 切换开关_玩家_true_应发送设置成功消息() {
            战斗日志处理器.切换开关(玩家, true);

            List<String> 消息列表 = 捕获所有消息(玩家);
            assertTrue(消息列表.stream().anyMatch(m -> m.contains("战斗日志") && m.contains("开启")),
                    "应包含战斗日志开启消息，实际: " + 消息列表);
        }
    }

    @Nested
    @DisplayName("会话不存在 - 优雅处理")
    class 会话不存在 {

        @Test
        @DisplayName("技能日志切换_会话不存在 - 不应抛出异常，不应保存")
        void 技能日志切换_会话不存在_应优雅处理() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> 技能日志处理器.切换开关(玩家, true));
            verify(数据服务, never()).保存(any());
        }

        @Test
        @DisplayName("战斗日志切换_会话不存在 - 不应抛出异常，不应保存")
        void 战斗日志切换_会话不存在_应优雅处理() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> 战斗日志处理器.切换开关(玩家, true));
            verify(数据服务, never()).保存(any());
        }
    }

    private List<String> 捕获所有消息(Player 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
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
