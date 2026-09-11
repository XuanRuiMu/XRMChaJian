package 暮澜纪元.配置;

import 暮澜纪元.登录服插件;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("消息管理器")
class 消息管理器测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "配置/消息管理器");
    }

    @Nested
    @DisplayName("翻译输出验证")
    class 翻译输出验证 {

        @Test
        @DisplayName("获取类翻译(中文) → 聊天栏应输出：语言系统初始化完成")
        void 获取类翻译_中文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            String 结果 = 消息.获取类翻译(消息管理器.class, "消息管理器.初始化.完成", 捕获器.获取玩家(), 5, 3, "zh, en");

            报告.publishEntry("翻译结果", 结果);
            assertTrue(结果.contains("语言系统初始化完成"), "中文翻译应包含'语言系统初始化完成'，实际: " + 结果);
            assertTrue(结果.contains("5"), "参数{0}应被替换为5");
        }

        @Test
        @DisplayName("获取类翻译(英文) → 聊天栏应输出：Language system initialized")
        void 获取类翻译_英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 结果 = 消息.获取类翻译(消息管理器.class, "消息管理器.初始化.完成", 捕获器.获取玩家(), 5, 3, "zh, en");

            报告.publishEntry("翻译结果", 结果);
            assertTrue(结果.contains("Language system initialized"), "英文翻译应包含'Language system initialized'，实际: " + 结果);
        }

        @Test
        @DisplayName("聊天栏应输出：语言系统初始化完成")
        void 发送类翻译_应输出到玩家聊天栏(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.初始化.完成", 5, 3, "zh, en");

            捕获器.发布到报告(报告);
            捕获器.断言包含("语言系统初始化完成", "发送初始化完成消息时");
        }

        @Test
        @DisplayName("参数替换 → 聊天栏应输出含替换后文本的消息")
        void 参数替换(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.初始化.完成", 10, 5, "zh, en, ja");

            捕获器.发布到报告(报告);
            捕获器.断言包含("10", "参数{0}替换");
            捕获器.断言包含("5", "参数{1}替换");
        }

        @Test
        @DisplayName("不存在的翻译键 → 聊天栏应输出键名本身")
        void 不存在的翻译键(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            String 结果 = 消息.获取类翻译(消息管理器.class, "不存在的键", 捕获器.获取玩家());

            报告.publishEntry("翻译结果", 结果);
            assertEquals("不存在的键", 结果, "不存在的键应返回键名本身");
        }

        @Test
        @DisplayName("聊天栏应输出：消息文件已重新加载")
        void 重载完成_应输出到玩家聊天栏(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.重载.完成", 8, 4);

            捕获器.发布到报告(报告);
            捕获器.断言包含("重新加载", "发送重载完成消息时");
        }

        @Test
        @DisplayName("英文玩家重载完成 → 聊天栏应输出：Message files reloaded")
        void 英文重载完成(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.重载.完成", 8, 4);

            捕获器.发布到报告(报告);
            捕获器.断言包含("reloaded", "英文玩家重载完成消息");
        }

        @Test
        @DisplayName("聊天栏应输出：无法创建日志文件")
        void 日志文件创建失败(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.日志.文件创建失败", "test.log");

            捕获器.发布到报告(报告);
            捕获器.断言包含("无法创建日志文件", "日志文件创建失败时");
        }

        @Test
        @DisplayName("聊天栏应输出：找不到语言文件")
        void 找不到语言文件(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 消息管理器.class, "消息管理器.日志.找不到语言文件", "xx.yml");

            捕获器.发布到报告(报告);
            捕获器.断言包含("找不到语言文件", "找不到语言文件时");
        }
    }
}
