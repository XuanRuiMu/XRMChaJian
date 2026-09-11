package mljy.全流程文本输出测试;

import com.google.inject.Provider;
import mljy.玩家服务;
import mljy.技能执行器;
import mljy.业务层.消息服务实现;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.业务层.技能释放服务实现;
import mljy.业务层.技能注册服务;
import mljy.业务层.技能冷却服务;
import mljy.业务层.资源变更服务;
import mljy.业务层.属性计算服务;
import mljy.业务层.公共冷却显示服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.表现层.监听器.技能移动打断监听器;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.技能执行结果;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-04 技能消息全流程文本输出测试。
 *
 * 验证玩家释放技能（蓄力/延迟/引导/瞬发）、命中目标、暴击命中场景下，
 * 消息服务实际发送给日志合并管理器的技能日志内容符合翻译文件 + 关键词解析器规范。
 *
 * 使用 ArgumentCaptor 捕获 日志合并管理器.添加技能日志 的实际参数，
 * 不硬编码完整预期文本，只验证关键特征（关键词颜色标签、参数替换、文本片段）。
 */
@DisplayName("FP-TEXT-04 技能消息全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能消息全流程测试 {

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
        加载翻译文件(翻译文件, "src/main/resources/文本消息/技能日志/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        消息服务 = new 消息服务实现(翻译服务, 关键词解析器, 玩家服务, 日志合并管理器, 战斗日志上下文管理器);

        玩家标识 = UUID.randomUUID();
        快照 = new 玩家快照(玩家标识, "测试法师", 1, null, null, Collections.emptyMap(), 战斗状态.非战斗);
        会话 = new 玩家会话(玩家标识, "测试法师");
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

    private String 捕获技能日志内容() {
        ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
        verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 捕获器.capture());
        return 捕获器.getValue();
    }

    @Nested
    @DisplayName("技能释放全流程：蓄力/延迟/引导/瞬发释放日志")
    class 技能释放全流程 {

        @Test
        @DisplayName("蓄力释放：捕获实际输出应包含蓄力关键词、秒数和第一技能")
        void 蓄力释放_应输出完整中文技能日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.蓄力释放", "0.5", "一");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("<aqua><bold>蓄力</bold></aqua>"),
                    "蓄力应使用<aqua><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("<aqua>0.5</aqua>"),
                    "秒数应使用<aqua>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("<red>第一技能</red>"),
                    "第一技能应使用<red>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("秒后，释放"),
                    "应包含'秒后，释放'文本片段，实际：" + 日志内容);
        }

        @Test
        @DisplayName("延迟释放：捕获实际输出应包含延迟关键词（黄色加粗）")
        void 延迟释放_应包含延迟颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.延迟释放", "1.0");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("<yellow><bold>延迟</bold></yellow>"),
                    "延迟应使用<yellow><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("<aqua>1.0</aqua>"),
                    "秒数应使用<aqua>颜色标签，实际：" + 日志内容);
        }

        @Test
        @DisplayName("引导释放：捕获实际输出应包含引导关键词（蓝色加粗）")
        void 引导释放_应包含引导颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.引导释放", "2.0");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("<blue><bold>引导</bold></blue>"),
                    "引导应使用<blue><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("秒，释放"),
                    "引导日志应包含'秒，释放'文本片段，实际：" + 日志内容);
        }

        @Test
        @DisplayName("瞬发释放：捕获实际输出应包含第一技能关键词")
        void 瞬发释放_应包含第一技能() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("释放了"),
                    "瞬发日志应包含'释放了'文本片段，实际：" + 日志内容);
            assertTrue(日志内容.contains("<red>第一技能</red>"),
                    "第一技能应使用<red>颜色标签，实际：" + 日志内容);
        }
    }

    @Nested
    @DisplayName("技能命中全流程：命中/暴击命中日志")
    class 技能命中全流程 {

        @Test
        @DisplayName("技能命中：捕获实际输出应包含目标名、伤害数值和秘能")
        void 技能命中_应输出完整中文命中日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.命中", "僵尸", "100.0", "奥术冲击", "1");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("僵尸"), "应包含目标名'僵尸'，实际：" + 日志内容);
            assertTrue(日志内容.contains("<dark_red>100.0</dark_red>"),
                    "伤害数值应使用<dark_red>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("<dark_purple><bold>秘能</bold></dark_purple>"),
                    "秘能应使用<dark_purple><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("获得"),
                    "应包含'获得'文本片段，实际：" + 日志内容);
        }

        @Test
        @DisplayName("技能暴击命中：捕获实际输出应包含暴击关键词（金色加粗）")
        void 技能暴击命中_应包含暴击颜色标签() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.暴击命中", "僵尸", "100.0", "奥术冲击", "1");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("<gold><bold>暴击</bold></gold>"),
                    "暴击应使用<gold><bold>颜色标签，实际：" + 日志内容);
            assertTrue(日志内容.contains("僵尸"), "应包含目标名，实际：" + 日志内容);
        }

        @Test
        @DisplayName("命中无资源：捕获实际输出不应包含秘能关键词")
        void 命中无资源_不应包含秘能() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.命中无资源", "僵尸", "100.0");

            String 日志内容 = 捕获技能日志内容();
            assertTrue(日志内容.contains("僵尸"), "应包含目标名，实际：" + 日志内容);
            assertFalse(日志内容.contains("秘能"),
                    "无资源命中不应包含秘能关键词，实际：" + 日志内容);
        }
    }

    @Nested
    @DisplayName("边界值与异常场景：开关关闭、会话不存在")
    class 边界值与异常 {

        @Test
        @DisplayName("技能日志开关关闭 - 不应发送日志")
        void 技能日志开关关闭_不应发送日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(false);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            verify(日志合并管理器, never()).添加技能日志(any(), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("会话不存在 - 不应发送技能日志")
        void 会话不存在_不应发送技能日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            verify(日志合并管理器, never()).添加技能日志(any(), any(UUID.class), anyString());
        }
    }

    @Nested
    @DisplayName("FP-C 端到端集成：技能释放服务→消息服务→日志合并管理器")
    class 释放服务到合并管理器端到端集成 {

        @Mock
        private 技能注册服务 技能注册服务;
        @Mock
        private 技能冷却服务 技能冷却服务;
        @Mock
        private 资源变更服务 资源变更服务;
        @Mock
        private 属性计算服务 属性计算服务;
        @Mock
        private 公共冷却显示服务 公共冷却显示服务;
        @Mock
        private JavaPlugin 释放服务插件;
        @Mock
        private Server 释放服务服务器;
        @Mock
        private BukkitScheduler 释放服务调度器;
        @Mock
        private BukkitTask 释放服务任务;
        @Mock
        private Provider<技能移动打断监听器> 移动打断监听器提供者;
        @Mock
        private 技能移动打断监听器 移动打断监听器;
        @Mock
        private 技能执行器 执行器;

        private 技能释放服务实现 释放服务;

        @BeforeEach
        void setUp释放服务() {
            释放服务 = new 技能释放服务实现(技能注册服务, 技能冷却服务, 资源变更服务,
                    属性计算服务, 消息服务, 翻译服务, 公共冷却显示服务, 玩家服务, 释放服务插件);
            when(移动打断监听器提供者.get()).thenReturn(移动打断监听器);
            释放服务.设置移动打断监听器提供者(移动打断监听器提供者);
            when(释放服务插件.getServer()).thenReturn(释放服务服务器);
            when(释放服务服务器.getScheduler()).thenReturn(释放服务调度器);
            when(释放服务调度器.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(释放服务任务);
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);
            会话.设置战斗日志开关(true);
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
            when(技能冷却服务.是否冷却中(eq(玩家标识), anyString())).thenReturn(false);
        }

        private 技能上下文 构造释放上下文(技能定义 定义) {
            属性快照 属性 = 属性快照.创建(100.0, 1.2, 20.0,
                    0.0, 0.0, 10.0, 0.0, 0.0, 10.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            玩家快照 施法者 = new 玩家快照(玩家标识, "测试法师", 1, null, 属性,
                    Collections.emptyMap(), 战斗状态.非战斗);
            return new 技能上下文(施法者, 定义.技能标识(), 定义, null, System.currentTimeMillis());
        }

        private void 准备执行器(技能定义 定义) {
            when(技能注册服务.获取定义(定义.技能标识())).thenReturn(Optional.of(定义));
            when(技能注册服务.获取执行器(定义.技能标识())).thenReturn(Optional.of(执行器));
            when(执行器.执行(any())).thenReturn(技能执行结果.成功);
        }

        @Test
        @DisplayName("瞬发技能释放：仅进入技能日志频道，不进入战斗日志频道")
        void 瞬发释放_仅进入技能日志频道() {
            技能定义 定义 = new 技能定义("1_1", "skill.test", 施法类型.瞬发,
                    0.0, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, true, false, false);
            准备执行器(定义);
            when(属性计算服务.计算实际公共冷却(anyDouble(), anyDouble())).thenReturn(1.2);

            技能执行结果 结果 = 释放服务.释放(构造释放上下文(定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), contains("释放了"));
            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString(), anyInt());
        }

        @Test
        @DisplayName("蓄力技能（奥术冲击）释放：仅进入技能日志频道，不进入战斗日志频道")
        void 蓄力释放_仅进入技能日志频道() {
            技能定义 定义 = new 技能定义("1_1", "skill.test", 施法类型.蓄力,
                    1.0, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            准备执行器(定义);
            when(属性计算服务.计算实际蓄力时间(eq(1.0), eq(20.0))).thenReturn(1.0);

            技能执行结果 结果 = 释放服务.释放(构造释放上下文(定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), contains("蓄力"));
            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString(), anyInt());
        }

        @Test
        @DisplayName("引导技能释放：仅进入技能日志频道，不进入战斗日志频道")
        void 引导释放_仅进入技能日志频道() {
            技能定义 定义 = new 技能定义("1_5", "skill.test", 施法类型.引导,
                    0.0, 3.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            准备执行器(定义);
            when(属性计算服务.计算实际蓄力时间(eq(3.0), eq(20.0))).thenReturn(2.5);

            技能执行结果 结果 = 释放服务.释放(构造释放上下文(定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), contains("引导"));
            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString(), anyInt());
        }

        @Test
        @DisplayName("持续施法技能释放：仅进入技能日志频道，不进入战斗日志频道")
        void 持续施法释放_仅进入技能日志频道() {
            技能定义 定义 = new 技能定义("1_4", "skill.test", 施法类型.持续施法,
                    0.4, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            准备执行器(定义);
            when(属性计算服务.计算实际蓄力时间(eq(0.4), eq(20.0))).thenReturn(0.5);

            技能执行结果 结果 = 释放服务.释放(构造释放上下文(定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), contains("延迟"));
            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString(), anyInt());
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
