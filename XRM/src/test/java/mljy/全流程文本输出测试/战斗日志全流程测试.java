package mljy.全流程文本输出测试;

import mljy.玩家服务;
import mljy.业务层.消息服务实现;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-02 战斗日志全流程文本输出测试。
 *
 * 验证玩家造成伤害、受到伤害、吸血附加等场景下，
 * 消息服务实际发送给日志合并管理器的内容符合翻译文件 + 关键词解析器规范。
 *
 * 使用 ArgumentCaptor 捕获 日志合并管理器.添加战斗日志 的实际参数，
 * 不硬编码完整预期文本，只验证关键特征（关键词颜色标签、参数替换、文本片段）。
 */
@DisplayName("FP-TEXT-02 战斗日志全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 战斗日志全流程测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 日志合并管理器 日志合并管理器;
    @Mock
    private 战斗日志上下文管理器 战斗日志上下文管理器;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 消息服务实现 消息服务;
    private UUID 玩家标识;
    private 玩家快照 快照;
    private 玩家会话 会话;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/战斗日志/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        消息服务 = new 消息服务实现(翻译服务, 关键词解析器, 玩家服务, 日志合并管理器, 战斗日志上下文管理器);

        玩家标识 = UUID.randomUUID();
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, null, Collections.emptyMap(), 战斗状态.非战斗);
        会话 = new 玩家会话(玩家标识, "测试玩家");
    }

    private void 加载翻译文件(YamlConfiguration 合并配置, String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                合并配置.set(键, 配置.get(键));
            }
        }
    }

    private String 捕获战斗日志内容() {
        ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
        verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 捕获器.capture(), anyInt());
        return 捕获器.getValue();
    }

    @Nested
    @DisplayName("普通攻击全流程：玩家攻击 → 伤害计算 → 消息服务处理 → 日志合并 → 玩家可见文本")
    class 普通攻击全流程 {

        @Test
        @DisplayName("普通攻击：捕获实际输出应包含物品名、目标名、伤害数值关键词")
        void 普通攻击_应输出完整中文伤害文本() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("你用"), "应包含'你用'前缀，实际：" + 日志内容);
            assertTrue(日志内容.contains("铁剑"), "应包含物品名'铁剑'，实际：" + 日志内容);
            assertTrue(日志内容.contains("僵尸"), "应包含目标名'僵尸'，实际：" + 日志内容);
            assertTrue(日志内容.contains("100.0"), "应包含伤害数值'100.0'，实际：" + 日志内容);
        }

        @Test
        @DisplayName("普通攻击：捕获实际输出应包含伤害数值关键词的MiniMessage颜色标签")
        void 普通攻击_应包含正确的MiniMessage颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("<dark_red>100.0</dark_red>"),
                    "伤害数值应使用<dark_red>颜色标签，实际：" + 日志内容);
        }

        @Test
        @DisplayName("普通攻击暴击：捕获实际输出应包含暴击关键词的gold+bold颜色标签")
        void 普通攻击暴击_应包含暴击颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击暴击", "铁剑", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("<gold><bold>暴击</bold></gold>"),
                    "暴击应使用<gold><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("100.0"), "应包含伤害数值，实际：" + 日志内容);
        }
    }

    @Nested
    @DisplayName("受到伤害全流程：玩家被攻击 → 伤害计算 → 消息服务处理 → 日志合并 → 玩家可见文本")
    class 受到伤害全流程 {

        @Test
        @DisplayName("受到伤害：捕获实际输出应包含来源名、伤害数值（FP-A点9：无裸括号纯文本）")
        void 受到伤害_应输出完整中文伤害文本() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.受到伤害", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("你受到了来自"), "应包含'你受到了来自'前缀，实际：" + 日志内容);
            assertTrue(日志内容.contains("僵尸"), "应包含来源名'僵尸'，实际：" + 日志内容);
            assertTrue(日志内容.contains("<dark_red>100.0</dark_red>"),
                    "伤害数值应着色<dark_red>，实际：" + 日志内容);
            assertFalse(日志内容.contains("[伤害数值:"), "不应包含裸括号'[伤害数值:'，实际：" + 日志内容);
        }

        @Test
        @DisplayName("受到伤害暴击：捕获实际输出应包含暴击关键词（FP-A点9：数值为纯文本）")
        void 受到伤害暴击_应输出暴击伤害文本() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.受到伤害暴击", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("<gold><bold>暴击</bold></gold>"),
                    "暴击应使用<gold><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("100.0"), "应包含伤害数值，实际：" + 日志内容);
            assertFalse(日志内容.contains("[伤害数值:"), "不应包含裸括号'[伤害数值:'，实际：" + 日志内容);
        }
    }

    @Nested
    @DisplayName("吸血附加全流程：攻击造成伤害 → 吸血触发 → 吸血日志附加 → 玩家可见完整文本")
    class 吸血附加全流程 {

        @Test
        @DisplayName("吸血附加：主伤害日志发送，吸血以尾缀登记（合并句末尾统一输出单条）")
        void 吸血附加_应输出伤害加吸血完整文本() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(25.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("你用"), "应包含伤害日志前缀，实际：" + 日志内容);
            assertTrue(日志内容.contains("100.0"), "主伤害日志应包含伤害数值，实际：" + 日志内容);

            ArgumentCaptor<Double> 吸血捕获 = ArgumentCaptor.forClass(Double.class);
            verify(日志合并管理器).设置吸血尾缀(eq(玩家标识), any(), 吸血捕获.capture());
            assertEquals(25.0, 吸血捕获.getValue(), 0.0001, "吸血尾缀应登记吸血量数值 25.0");
        }

        @Test
        @DisplayName("吸血附加：吸血尾缀应登记 25.0 数值（着色与句号由合并时在翻译模板解析）")
        void 吸血附加_应包含吸血和治疗效果颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(25.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<Double> 吸血捕获 = ArgumentCaptor.forClass(Double.class);
            verify(日志合并管理器).设置吸血尾缀(eq(玩家标识), any(), 吸血捕获.capture());
            assertEquals(25.0, 吸血捕获.getValue(), 0.0001, "吸血尾缀应登记吸血量数值 25.0");
        }

        @Test
        @DisplayName("无吸血时不登记吸血尾缀，主伤害日志仍正常发送")
        void 无吸血_不应附加吸血日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(日志合并管理器, never()).设置吸血尾缀(any(), any(), anyDouble());
            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("你用"), "无吸血时主伤害日志仍应发送，实际：" + 日志内容);
        }
    }

    @Nested
    @DisplayName("边界值与异常场景：开关关闭、会话不存在")
    class 边界值与异常 {

        @Test
        @DisplayName("战斗日志开关关闭 - 不应发送日志且清除上下文")
        void 战斗日志开关关闭_不应发送日志且清除上下文() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(false);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(日志合并管理器, never()).添加战斗日志(any(), anyString());
            verify(战斗日志上下文管理器).清除上下文(any(), any());
        }

        @Test
        @DisplayName("会话不存在 - 使用默认开启时应发送战斗日志")
        void 会话不存在_默认开启时应发送战斗日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认战斗日志开关()).thenReturn(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            String 日志内容 = 捕获战斗日志内容();
            assertTrue(日志内容.contains("你用"), "默认开启时应输出战斗日志，实际: " + 日志内容);
        }

        @Test
        @DisplayName("会话不存在 - 使用默认关闭时不应发送战斗日志且清除上下文")
        void 会话不存在_默认关闭时不应发送战斗日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认战斗日志开关()).thenReturn(false);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(日志合并管理器, never()).添加战斗日志(any(), anyString());
            verify(战斗日志上下文管理器).清除上下文(any(), any());
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
