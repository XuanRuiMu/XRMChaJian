package mljy.表现层.命令.乐器指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.业务层.乐器服务;
import mljy.业务层.乐器服务.乐谱保存结果;
import mljy.业务层.乐器.公共乐谱服务.上传结果;
import mljy.业务层.乐器.公共乐谱服务.下载结果;
import mljy.业务层.乐器.公共乐谱服务.评分结果;
import mljy.业务层.乐器.公共乐谱服务.审核结果;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.量化档位;
import mljy.领域层.乐器.延音模式;
import mljy.领域层.乐器.和弦类型;
import mljy.领域层.乐器.合奏状态;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FP-16 乐器命令处理器测试。
 * 覆盖 19 个一级子命令的正常流程、参数校验、权限检查、服务调用验证、Tab 补全。
 */
@DisplayName("乐器命令处理器")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 乐器命令处理器测试 {

    @Mock
    private 乐器服务 乐器服务mock;
    @Mock
    private 乐器注册表 乐器注册表mock;
    @Mock
    private Player 玩家;
    @Mock
    private ConsoleCommandSender 控制台;
    @Mock
    private Command 命令;
    @Mock
    private 乐器定义 乐器定义mock;
    @Mock
    private 乐谱 乐谱mock;
    @Mock
    private 公共乐谱 公共乐谱mock;
    @Mock
    private 合奏状态 合奏状态mock;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器实例;
    private 乐器命令处理器 处理器;
    private YamlConfiguration 翻译文件;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/乐器/乐器命令/zh.yml");
        加载翻译文件("src/main/resources/文本消息/乐器/乐器服务/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器实例 = 关键词解析器.创建纯文本();
        处理器 = new 乐器命令处理器(翻译服务, 关键词解析器实例, 乐器服务mock, 乐器注册表mock);

        玩家标识 = UUID.randomUUID();
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.hasPermission(anyString())).thenReturn(true);
        when(乐器定义mock.获取显示名称()).thenReturn("钢琴");
    }

    private void 加载翻译文件(String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        if (!文件.exists()) {
            return;
        }
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
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        return PlainTextComponentSerializer.plainText().serialize(捕获器.getValue());
    }

    private List<String> 捕获所有消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        List<String> 结果 = new ArrayList<>();
        for (Component 组件 : 捕获器.getAllValues()) {
            结果.add(PlainTextComponentSerializer.plainText().serialize(组件));
        }
        return 结果;
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

    @Nested
    @DisplayName("基础功能")
    class 基础功能 {

        @Test
        @DisplayName("获取命令名 - 应返回'乐器'")
        void 获取命令名_应返回乐器() {
            assertEquals("乐器", 处理器.获取命令名());
        }

        @Test
        @DisplayName("控制台执行 - 应发送仅玩家可用错误")
        void 控制台执行_应发送仅玩家可用错误() {
            boolean 结果 = 处理器.onCommand(控制台, 命令, "乐器", new String[]{});

            assertTrue(结果);
            String 文本 = 捕获消息(控制台);
            assertTrue(文本.contains("仅玩家可用"), "应包含'仅玩家可用'，实际: " + 文本);
        }

        @Test
        @DisplayName("无参数 - 应发送缺少参数错误")
        void 无参数_应发送缺少参数错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("参数") || 文本.contains("缺少"), "应包含参数错误，实际: " + 文本);
        }

        @Test
        @DisplayName("未知子命令 - 应发送未知子命令错误")
        void 未知子命令_应发送未知子命令错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"不存在"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("不存在") || 文本.contains("未知"), "应包含未知子命令，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("演奏命令")
    class 演奏命令 {

        @Test
        @DisplayName("无玩家乐器且无默认乐器 - 应发送默认乐器未配置错误")
        void 无玩家乐器且无默认乐器_应发送音色不存在错误() {
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.empty());
            when(乐器注册表mock.默认乐器()).thenReturn(Optional.empty());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"演奏"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("默认乐器") || 文本.contains("未配置"), "应包含默认乐器未配置，实际: " + 文本);
        }

        @Test
        @DisplayName("有默认乐器 - 应调用打开演奏界面")
        void 有默认乐器_应调用打开演奏界面() {
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.empty());
            when(乐器注册表mock.默认乐器()).thenReturn(Optional.of(乐器定义mock));

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"演奏"});

            assertTrue(结果);
            verify(乐器服务mock).打开演奏界面(玩家, 乐器定义mock);
        }
    }

    @Nested
    @DisplayName("切换命令")
    class 切换命令 {

        @Test
        @DisplayName("缺少参数 - 应发送缺少参数错误")
        void 缺少参数_应发送缺少参数错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"切换"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("参数") || 文本.contains("缺少"), "应包含参数错误，实际: " + 文本);
        }

        @Test
        @DisplayName("乐器不存在 - 应发送音色不存在错误")
        void 乐器不存在_应发送音色不存在错误() {
            when(乐器注册表mock.按名称查找("不存在的乐器")).thenReturn(Optional.empty());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"切换", "不存在的乐器"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("音色") || 文本.contains("不存在"), "应包含音色不存在，实际: " + 文本);
        }

        @Test
        @DisplayName("正常切换 - 应调用切换乐器")
        void 正常切换_应调用切换乐器() {
            when(乐器注册表mock.按名称查找("钢琴")).thenReturn(Optional.of(乐器定义mock));

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"切换", "钢琴"});

            assertTrue(结果);
            verify(乐器服务mock).切换乐器(玩家, 乐器定义mock);
        }
    }

    @Nested
    @DisplayName("乐谱命令")
    class 乐谱命令 {

        @Test
        @DisplayName("列表为空 - 应发送列表为空消息")
        void 列表为空_应发送列表为空消息() {
            when(乐器服务mock.获取乐谱列表(玩家标识)).thenReturn(Collections.emptyList());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"乐谱", "列表"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("乐谱") || 文本.contains("没有"), "应包含乐谱列表为空，实际: " + 文本);
        }

        @Test
        @DisplayName("播放缺乐谱名 - 应发送缺少参数错误")
        void 播放缺乐谱名_应发送缺少参数错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"乐谱", "播放"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("参数") || 文本.contains("缺少"), "应包含参数错误，实际: " + 文本);
        }

        @Test
        @DisplayName("播放不存在的乐谱 - 应发送乐谱不存在错误")
        void 播放不存在的乐谱_应发送乐谱不存在错误() {
            when(乐器服务mock.加载乐谱(玩家标识, "不存在")).thenReturn(Optional.empty());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"乐谱", "播放", "不存在"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("不存在"), "应包含乐谱不存在，实际: " + 文本);
        }

        @Test
        @DisplayName("删除成功 - 应发送删除成功消息")
        void 删除成功_应发送删除成功消息() {
            when(乐器服务mock.删除乐谱(玩家标识, "test")).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"乐谱", "删除", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("删除") || 文本.contains("test"), "应包含删除成功，实际: " + 文本);
        }

        @Test
        @DisplayName("删除不存在的乐谱 - 应发送乐谱不存在错误")
        void 删除不存在的乐谱_应发送乐谱不存在错误() {
            when(乐器服务mock.删除乐谱(玩家标识, "不存在")).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"乐谱", "删除", "不存在"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("不存在"), "应包含乐谱不存在，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("节拍器命令")
    class 节拍器命令 {

        @Test
        @DisplayName("开启 - 应调用设置节拍器开关并发送开启消息")
        void 开启_应调用设置节拍器开关并发送开启消息() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"节拍器", "开"});

            assertTrue(结果);
            verify(乐器服务mock).设置节拍器开关(玩家标识, true);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("开启") || 文本.contains("节拍器"), "应包含开启，实际: " + 文本);
        }

        @Test
        @DisplayName("关闭 - 应调用设置节拍器开关并发送关闭消息")
        void 关闭_应调用设置节拍器开关并发送关闭消息() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"节拍器", "关"});

            assertTrue(结果);
            verify(乐器服务mock).设置节拍器开关(玩家标识, false);
        }

        @Test
        @DisplayName("count-in 合法值 - 应调用设置CountIn小节数")
        void countIn合法值_应调用设置CountIn小节数() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"节拍器", "count-in", "1"});

            assertTrue(结果);
            verify(乐器服务mock).设置CountIn小节数(1);
        }

        @Test
        @DisplayName("count-in 非法值 3 - 应发送非法值错误")
        void countIn非法值3_应发送非法值错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"节拍器", "count-in", "3"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("count-in") || 文本.contains("非法") || 文本.contains("错误"),
                    "应包含非法值错误，实际: " + 文本);
        }

        @Test
        @DisplayName("count-in 非数字 - 应发送非法值错误")
        void countIn非数字_应发送非法值错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"节拍器", "count-in", "abc"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("count-in") || 文本.contains("非法") || 文本.contains("错误"),
                    "应包含非法值错误，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("力度命令 - FP-16 新增")
    class 力度命令 {

        @Test
        @DisplayName("设置 mf 档 - 应调用设置玩家力度档")
        void 设置MF档_应调用设置玩家力度档() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "mf"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家力度档(玩家标识, 力度档.MF);
        }

        @Test
        @DisplayName("设置 pp 档 - 应调用设置玩家力度档")
        void 设置PP档_应调用设置玩家力度档() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "pp"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家力度档(玩家标识, 力度档.PP);
        }

        @Test
        @DisplayName("真实力度开 - 应调用设置玩家真实力度开关")
        void 真实力度开_应调用设置玩家真实力度开关() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "真实", "开"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家真实力度开关(玩家标识, true);
        }

        @Test
        @DisplayName("真实力度关 - 应调用设置玩家真实力度开关")
        void 真实力度关_应调用设置玩家真实力度开关() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "真实", "关"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家真实力度开关(玩家标识, false);
        }

        @Test
        @DisplayName("力度音域 2 - 应调用设置玩家音域")
        void 力度音域2_应调用设置玩家音域() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "音域", "2"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家音域(玩家标识, 2);
        }

        @Test
        @DisplayName("无效档位 - 应发送档位无效错误")
        void 无效档位_应发送档位无效错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"力度", "invalid"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("档位") || 文本.contains("无效") || 文本.contains("错误"),
                    "应包含档位无效，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("音域命令 - FP-16 新增")
    class 音域命令 {

        @Test
        @DisplayName("设置音域 3 - 应调用设置玩家音域")
        void 设置音域3_应调用设置玩家音域() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"音域", "3"});

            assertTrue(结果);
            verify(乐器服务mock).设置玩家音域(玩家标识, 3);
        }

        @Test
        @DisplayName("音域越界 5 - 应发送越界错误")
        void 音域越界5_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"音域", "5"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }

        @Test
        @DisplayName("音域非数字 - 应发送越界错误")
        void 音域非数字_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"音域", "abc"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("量化命令 - FP-16 新增")
    class 量化命令 {

        @Test
        @DisplayName("量化 1/16 - 应调用量化乐谱")
        void 量化一分之十六_应调用量化乐谱() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"量化", "1/16"});

            assertTrue(结果);
            verify(乐器服务mock).量化乐谱(玩家标识, 量化档位.一分之十六);
        }

        @Test
        @DisplayName("撤销量化成功 - 应发送量化已撤销消息")
        void 撤销量化成功_应发送量化已撤销消息() {
            when(乐器服务mock.撤销量化(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"量化", "撤销"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("撤销") || 文本.contains("量化"),
                    "应包含量化撤销，实际: " + 文本);
        }

        @Test
        @DisplayName("撤销量化失败 - 应发送无可量化乐谱错误")
        void 撤销量化失败_应发送无可量化乐谱错误() {
            when(乐器服务mock.撤销量化(玩家标识)).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"量化", "撤销"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("量化") || 文本.contains("错误"),
                    "应包含错误，实际: " + 文本);
        }

        @Test
        @DisplayName("预览已量化 - 应发送已量化消息")
        void 预览已量化_应发送已量化消息() {
            when(乐器服务mock.是否已量化(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"量化", "预览"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertNotNull(文本);
        }

        @Test
        @DisplayName("无效量化档位 - 应发送档位无效错误")
        void 无效量化档位_应发送档位无效错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"量化", "invalid"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("档位") || 文本.contains("无效") || 文本.contains("错误"),
                    "应包含档位无效，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("swing 命令 - FP-16 新增")
    class Swing命令 {

        @Test
        @DisplayName("开启 - 应调用设置Swing")
        void 开启_应调用设置Swing() {
            when(乐器服务mock.获取Swing(玩家标识)).thenReturn(0.50);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"swing", "开"});

            assertTrue(结果);
            verify(乐器服务mock).设置Swing(玩家标识, 0.55);
        }

        @Test
        @DisplayName("关闭 - 应调用设置Swing为0.50")
        void 关闭_应调用设置Swing为050() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"swing", "关"});

            assertTrue(结果);
            verify(乐器服务mock).设置Swing(玩家标识, 0.50);
        }

        @Test
        @DisplayName("百分比 60 - 应调用设置Swing为0.60")
        void 百分比60_应调用设置Swing为060() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"swing", "60"});

            assertTrue(结果);
            verify(乐器服务mock).设置Swing(玩家标识, 0.60);
        }

        @Test
        @DisplayName("百分比越界 80 - 应发送越界错误")
        void 百分比越界80_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"swing", "80"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("延音命令 - FP-16 新增")
    class 延音命令 {

        @Test
        @DisplayName("按住模式 - 应调用设置延音模式")
        void 按住模式_应调用设置延音模式() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"延音", "按住"});

            assertTrue(结果);
            verify(乐器服务mock).设置延音模式(玩家标识, 延音模式.按住);
        }

        @Test
        @DisplayName("切换模式 - 应调用设置延音模式")
        void 切换模式_应调用设置延音模式() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"延音", "切换"});

            assertTrue(结果);
            verify(乐器服务mock).设置延音模式(玩家标识, 延音模式.切换);
        }

        @Test
        @DisplayName("无效模式 - 应发送模式无效错误")
        void 无效模式_应发送模式无效错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"延音", "invalid"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("模式") || 文本.contains("无效") || 文本.contains("错误"),
                    "应包含模式无效，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("和弦命令 - FP-16 新增")
    class 和弦命令 {

        @Test
        @DisplayName("列表 - 应发送和弦列表标题")
        void 列表_应发送和弦列表标题() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "列表"});

            assertTrue(结果);
            verify(玩家, atLeastOnce()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("清除 - 应调用停止和弦记忆")
        void 清除_应调用停止和弦记忆() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "清除"});

            assertTrue(结果);
            verify(乐器服务mock).停止和弦记忆(玩家标识);
        }

        @Test
        @DisplayName("播放和弦 - 应调用播放和弦")
        void 播放和弦_应调用播放和弦() {
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.of(乐器定义mock));
            when(乐器服务mock.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "50", "maj"});

            assertTrue(结果);
            verify(乐器服务mock).播放和弦(玩家, 乐器定义mock, 50, 和弦类型.大三, 力度档.MF);
        }

        @Test
        @DisplayName("和弦记忆 - 应调用启动和弦记忆")
        void 和弦记忆_应调用启动和弦记忆() {
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.of(乐器定义mock));
            when(乐器服务mock.获取玩家力度档(玩家标识)).thenReturn(力度档.MF);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "50", "maj", "记忆"});

            assertTrue(结果);
            verify(乐器服务mock).启动和弦记忆(eq(玩家), eq(乐器定义mock), eq(50), eq(和弦类型.大三), eq(力度档.MF), anyInt());
        }

        @Test
        @DisplayName("根音越界 - 应发送根音无效错误")
        void 根音越界_应发送根音无效错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "100", "maj"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("根音") || 文本.contains("无效") || 文本.contains("错误"),
                    "应包含根音无效，实际: " + 文本);
        }

        @Test
        @DisplayName("和弦类型无效 - 应发送类型无效错误")
        void 和弦类型无效_应发送类型无效错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"和弦", "60", "invalid"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("类型") || 文本.contains("无效") || 文本.contains("错误"),
                    "应包含类型无效，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("录音命令 - FP-16 新增")
    class 录音命令 {

        @Test
        @DisplayName("开始录音 - 应调用开始录制")
        void 开始录音_应调用开始录制() {
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.of(乐器定义mock));

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "开始"});

            assertTrue(结果);
            verify(乐器服务mock).开始录制(玩家标识, 乐器定义mock);
        }

        @Test
        @DisplayName("停止录音 - 应调用停止录制")
        void 停止录音_应调用停止录制() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "停止"});

            assertTrue(结果);
            verify(乐器服务mock).停止录制(玩家标识);
        }

        @Test
        @DisplayName("保存成功 - 应发送保存成功消息")
        void 保存成功_应发送保存成功消息() {
            when(乐器服务mock.保存乐谱带结果(玩家标识, "test")).thenReturn(乐谱保存结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "保存", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("保存") || 文本.contains("成功"),
                    "应包含保存成功，实际: " + 文本);
        }

        @Test
        @DisplayName("保存重名 - 应发送乐谱已存在消息")
        void 保存重名_应发送乐谱已存在消息() {
            when(乐器服务mock.保存乐谱带结果(玩家标识, "test")).thenReturn(乐谱保存结果.重名);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "保存", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("存在") || 文本.contains("重名"),
                    "应包含乐谱已存在，实际: " + 文本);
        }

        @Test
        @DisplayName("预览进行中 - 应发送进行中消息")
        void 预览进行中_应发送进行中消息() {
            when(乐器服务mock.是否录制中(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "预览"});

            assertTrue(结果);
            verify(乐器服务mock).是否录制中(玩家标识);
        }
    }

    @Nested
    @DisplayName("公共库命令 - FP-16 新增")
    class 公共库命令 {

        @Test
        @DisplayName("列表为空 - 应发送列表为空消息")
        void 列表为空_应发送列表为空消息() {
            when(乐器服务mock.列出公共乐谱()).thenReturn(Collections.emptyList());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "列表"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("空") || 文本.contains("公共"),
                    "应包含列表为空，实际: " + 文本);
        }

        @Test
        @DisplayName("上传成功 - 应发送上传成功消息")
        void 上传成功_应发送上传成功消息() {
            when(乐器服务mock.上传公共乐谱(玩家, "local", "public")).thenReturn(上传结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "上传", "local", "public"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("上传") || 文本.contains("成功"),
                    "应包含上传成功，实际: " + 文本);
        }

        @Test
        @DisplayName("下载成功 - 应发送下载成功消息")
        void 下载成功_应发送下载成功消息() {
            when(乐器服务mock.下载公共乐谱(玩家, "id1")).thenReturn(下载结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "下载", "id1"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("下载") || 文本.contains("成功"),
                    "应包含下载成功，实际: " + 文本);
        }

        @Test
        @DisplayName("评分成功 - 应发送评分成功消息")
        void 评分成功_应发送评分成功消息() {
            when(乐器服务mock.评分公共乐谱(玩家, "id1", 5)).thenReturn(评分结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "评分", "id1", "5"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("评分") || 文本.contains("成功"),
                    "应包含评分成功，实际: " + 文本);
        }

        @Test
        @DisplayName("审核通过 - 应调用审核公共乐谱")
        void 审核通过_应调用审核公共乐谱() {
            when(乐器服务mock.审核公共乐谱(玩家, "id1", 审核状态.已通过)).thenReturn(审核结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "审核", "id1", "通过"});

            assertTrue(结果);
            verify(乐器服务mock).审核公共乐谱(玩家, "id1", 审核状态.已通过);
        }

        @Test
        @DisplayName("审核拒绝 - 应调用审核公共乐谱")
        void 审核拒绝_应调用审核公共乐谱() {
            when(乐器服务mock.审核公共乐谱(玩家, "id1", 审核状态.已拒绝)).thenReturn(审核结果.成功);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"公共库", "审核", "id1", "拒绝"});

            assertTrue(结果);
            verify(乐器服务mock).审核公共乐谱(玩家, "id1", 审核状态.已拒绝);
        }
    }

    @Nested
    @DisplayName("分层命令 - FP-16 新增")
    class 分层命令 {

        @Test
        @DisplayName("开启 - 应调用设置分层模式")
        void 开启_应调用设置分层模式() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "开"});

            assertTrue(结果);
            verify(乐器服务mock).设置分层模式(玩家标识, true);
        }

        @Test
        @DisplayName("关闭 - 应调用设置分层模式")
        void 关闭_应调用设置分层模式() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "关"});

            assertTrue(结果);
            verify(乐器服务mock).设置分层模式(玩家标识, false);
        }

        @Test
        @DisplayName("切换 - 应调用切换分层模式")
        void 切换_应调用切换分层模式() {
            when(乐器服务mock.切换分层模式(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "切换"});

            assertTrue(结果);
            verify(乐器服务mock).切换分层模式(玩家标识);
        }

        @Test
        @DisplayName("设置分界点 - 应调用设置分界点")
        void 设置分界点_应调用设置分界点() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "分界", "12"});

            assertTrue(结果);
            verify(乐器服务mock).设置分界点(玩家标识, 12);
        }

        @Test
        @DisplayName("分界点越界 - 应发送越界错误")
        void 分界点越界_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "分界", "100"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }

        @Test
        @DisplayName("设置左乐器 - 应调用设置分层左乐器")
        void 设置左乐器_应调用设置分层左乐器() {
            when(乐器注册表mock.按名称查找("钢琴")).thenReturn(Optional.of(乐器定义mock));

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "乐器", "左", "钢琴"});

            assertTrue(结果);
            verify(乐器服务mock).设置分层左乐器(玩家标识, 乐器定义mock);
        }

        @Test
        @DisplayName("设置右乐器 - 应调用设置分层右乐器")
        void 设置右乐器_应调用设置分层右乐器() {
            when(乐器注册表mock.按名称查找("钢琴")).thenReturn(Optional.of(乐器定义mock));

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"分层", "乐器", "右", "钢琴"});

            assertTrue(结果);
            verify(乐器服务mock).设置分层右乐器(玩家标识, 乐器定义mock);
        }
    }

    @Nested
    @DisplayName("钢琴卷帘命令 - FP-16 新增")
    class 钢琴卷帘命令 {

        @Test
        @DisplayName("打开成功 - 应发送已打开消息")
        void 打开成功_应发送已打开消息() {
            when(乐器服务mock.打开PianoRoll编辑器(玩家, "test")).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"钢琴卷帘", "打开", "test"});

            assertTrue(结果);
            verify(乐器服务mock).打开PianoRoll编辑器(玩家, "test");
        }

        @Test
        @DisplayName("打开失败 - 应发送乐谱不存在错误")
        void 打开失败_应发送乐谱不存在错误() {
            when(乐器服务mock.打开PianoRoll编辑器(玩家, "test")).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"钢琴卷帘", "打开", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("不存在") || 文本.contains("错误"),
                    "应包含乐谱不存在，实际: " + 文本);
        }

        @Test
        @DisplayName("缺乐谱名 - 应发送用法消息")
        void 缺乐谱名_应发送用法消息() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"钢琴卷帘"});

            assertTrue(结果);
            verify(玩家, atLeastOnce()).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("资源包命令 - FP-16 新增")
    class 资源包命令 {

        @Test
        @DisplayName("应用成功 - 应发送应用提示消息")
        void 应用成功_应发送应用提示消息() {
            when(乐器服务mock.应用资源包(玩家)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"资源包", "应用"});

            assertTrue(结果);
            verify(乐器服务mock).应用资源包(玩家);
        }

        @Test
        @DisplayName("应用失败 - 应发送应用失败错误")
        void 应用失败_应发送应用失败错误() {
            when(乐器服务mock.应用资源包(玩家)).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"资源包", "应用"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("失败") || 文本.contains("错误"),
                    "应包含应用失败，实际: " + 文本);
        }

        @Test
        @DisplayName("生成成功 - 应发送生成成功消息")
        void 生成成功_应发送生成成功消息() {
            when(乐器服务mock.生成资源包()).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"资源包", "生成"});

            assertTrue(结果);
            verify(乐器服务mock).生成资源包();
        }

        @Test
        @DisplayName("生成失败 - 应发送生成失败错误")
        void 生成失败_应发送生成失败错误() {
            when(乐器服务mock.生成资源包()).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"资源包", "生成"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("失败") || 文本.contains("错误"),
                    "应包含生成失败，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("弯音命令 - FP-16 新增")
    class 弯音命令 {

        @Test
        @DisplayName("设置灵敏度 2 - 应调用设置弯音范围")
        void 设置灵敏度2_应调用设置弯音范围() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"弯音", "灵敏度", "2"});

            assertTrue(结果);
            verify(乐器服务mock).设置弯音范围(玩家标识, 2);
        }

        @Test
        @DisplayName("灵敏度越界 15 - 应发送越界错误")
        void 灵敏度越界15_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"弯音", "灵敏度", "15"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }

        @Test
        @DisplayName("灵敏度非数字 - 应发送越界错误")
        void 灵敏度非数字_应发送越界错误() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"弯音", "灵敏度", "abc"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("越界") || 文本.contains("错误"),
                    "应包含越界错误，实际: " + 文本);
        }
    }

    @Nested
    @DisplayName("重载命令")
    class 重载命令 {

        @Test
        @DisplayName("正常重载 - 应调用重载配置")
        void 正常重载_应调用重载配置() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"重载"});

            assertTrue(结果);
            verify(乐器服务mock).重载配置();
        }
    }

    @Nested
    @DisplayName("合奏命令")
    class 合奏命令 {

        @Test
        @DisplayName("缺参数 - 应发送用法消息")
        void 缺参数_应发送用法消息() {
            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏"});

            assertTrue(结果);
            verify(玩家, atLeastOnce()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("开始成功 - 应发送准备检查已发送消息")
        void 开始成功_应发送准备检查已发送消息() {
            when(乐器服务mock.开始合奏(玩家标识, "test")).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏", "开始", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertNotNull(文本);
        }

        @Test
        @DisplayName("开始失败 - 应发送无法开始错误")
        void 开始失败_应发送无法开始错误() {
            when(乐器服务mock.开始合奏(玩家标识, "test")).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏", "开始", "test"});

            assertTrue(结果);
            String 文本 = 捕获消息(玩家);
            assertTrue(文本.contains("错误") || 文本.contains("无法"),
                    "应包含无法开始错误，实际: " + 文本);
        }

        @Test
        @DisplayName("确认成功 - 应发送已确认消息")
        void 确认成功_应发送已确认消息() {
            when(乐器服务mock.确认合奏(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏", "确认"});

            assertTrue(结果);
            verify(乐器服务mock).确认合奏(玩家标识);
        }

        @Test
        @DisplayName("停止成功 - 应发送已停止消息")
        void 停止成功_应发送已停止消息() {
            when(乐器服务mock.停止合奏(玩家标识)).thenReturn(true);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏", "停止"});

            assertTrue(结果);
            verify(乐器服务mock).停止合奏(玩家标识);
        }

        @Test
        @DisplayName("状态无进行中 - 应发送无进行中消息")
        void 状态无进行中_应发送无进行中消息() {
            when(乐器服务mock.获取合奏状态(玩家标识)).thenReturn(Optional.empty());

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"合奏", "状态"});

            assertTrue(结果);
            verify(乐器服务mock).获取合奏状态(玩家标识);
        }
    }

    @Nested
    @DisplayName("Tab 补全")
    class Tab补全 {

        @Test
        @DisplayName("一级补全 - 应返回 19 个子命令")
        void 一级补全_应返回19个子命令() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{""});

            assertNotNull(结果);
            assertTrue(结果.contains("演奏"));
            assertTrue(结果.contains("切换"));
            assertTrue(结果.contains("乐谱"));
            assertTrue(结果.contains("合奏"));
            assertTrue(结果.contains("给予"));
            assertTrue(结果.contains("重载"));
            assertTrue(结果.contains("节拍器"));
            assertTrue(结果.contains("力度"));
            assertTrue(结果.contains("音域"));
            assertTrue(结果.contains("量化"));
            assertTrue(结果.contains("swing"));
            assertTrue(结果.contains("延音"));
            assertTrue(结果.contains("和弦"));
            assertTrue(结果.contains("录音"));
            assertTrue(结果.contains("公共库"));
            assertTrue(结果.contains("分层"));
            assertTrue(结果.contains("钢琴卷帘"));
            assertTrue(结果.contains("资源包"));
            assertTrue(结果.contains("弯音"));
        }

        @Test
        @DisplayName("乐谱二级补全 - 应返回 7 个乐谱子命令")
        void 乐谱二级补全_应返回7个乐谱子命令() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"乐谱", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("列表"));
            assertTrue(结果.contains("播放"));
            assertTrue(结果.contains("删除"));
            assertTrue(结果.contains("导入"));
            assertTrue(结果.contains("导出"));
            assertTrue(结果.contains("midi导入"));
            assertTrue(结果.contains("midi导出"));
        }

        @Test
        @DisplayName("力度二级补全 - 应返回 5 档+真实+音域")
        void 力度二级补全_应返回5档真实音域() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"力度", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("pp"));
            assertTrue(结果.contains("p"));
            assertTrue(结果.contains("mf"));
            assertTrue(结果.contains("f"));
            assertTrue(结果.contains("ff"));
            assertTrue(结果.contains("真实"));
            assertTrue(结果.contains("音域"));
        }

        @Test
        @DisplayName("节拍器 count-in 三级补全 - 应返回 0/1/2")
        void 节拍器CountIn三级补全_应返回012() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"节拍器", "count-in", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("0"));
            assertTrue(结果.contains("1"));
            assertTrue(结果.contains("2"));
        }

        @Test
        @DisplayName("力度真实三级补全 - 应返回 开/关")
        void 力度真实三级补全_应返回开关() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"力度", "真实", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("开"));
            assertTrue(结果.contains("关"));
        }

        @Test
        @DisplayName("乐谱播放三级补全 - 应返回玩家乐谱列表")
        void 乐谱播放三级补全_应返回玩家乐谱列表() {
            List<String> 乐谱列表 = Arrays.asList("song1", "song2");
            when(乐器服务mock.获取乐谱列表(玩家标识)).thenReturn(乐谱列表);

            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"乐谱", "播放", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("song1"));
            assertTrue(结果.contains("song2"));
        }

        @Test
        @DisplayName("分层乐器三级补全 - 应返回 左/右")
        void 分层乐器三级补全_应返回左右() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"分层", "乐器", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("左"));
            assertTrue(结果.contains("右"));
        }

        @Test
        @DisplayName("资源包二级补全 - 应返回 应用/生成")
        void 资源包二级补全_应返回应用生成() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"资源包", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("应用"));
            assertTrue(结果.contains("生成"));
        }

        @Test
        @DisplayName("弯音二级补全 - 应返回 灵敏度")
        void 弯音二级补全_应返回灵敏度() {
            List<String> 结果 = 处理器.onTabComplete(玩家, 命令, "乐器", new String[]{"弯音", ""});

            assertNotNull(结果);
            assertTrue(结果.contains("灵敏度"));
        }
    }

    @Nested
    @DisplayName("权限检查")
    class 权限检查 {

        @Test
        @DisplayName("演奏无权限 - 应发送无权限错误并不调用打开演奏界面")
        void 演奏无权限_应发送无权限错误并不调用打开演奏界面() {
            when(玩家.hasPermission("xrm.乐器.演奏")).thenReturn(false);
            when(玩家.hasPermission("xrmm.乐器.管理")).thenReturn(false);
            when(玩家.hasPermission(anyString())).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"演奏"});

            assertTrue(结果);
            verify(乐器服务mock, never()).打开演奏界面(any(), any());
        }

        @Test
        @DisplayName("录音无权限 - 应发送无权限错误并不调用开始录制")
        void 录音无权限_应发送无权限错误并不调用开始录制() {
            when(玩家.hasPermission(anyString())).thenReturn(false);

            boolean 结果 = 处理器.onCommand(玩家, 命令, "乐器", new String[]{"录音", "开始"});

            assertTrue(结果);
            verify(乐器服务mock, never()).开始录制(any(), any());
        }
    }
}
