package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("传送门监听器")
class 传送门监听器测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "监听器/传送门监听器");
        when(mock插件.获取消息管理器()).thenReturn(消息);
    }

    @Nested
    @DisplayName("翻译输出验证")
    class 翻译输出验证 {

        @Test
        @DisplayName("聊天栏应输出：请先选择你所扮演的角色。")
        void 无职业提示_应显示正确中文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 传送门监听器.class, "无职业");

            捕获器.发布到报告(报告);
            捕获器.断言包含("请先选择你所扮演的角色", "无职业玩家进入传送门时");
        }

        @Test
        @DisplayName("聊天栏应输出：正在进入暮澜纪元MMORPG服务器...")
        void 服务器欢迎_应显示正确中文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 传送门监听器.class, "服务器欢迎");

            捕获器.发布到报告(报告);
            捕获器.断言包含("暮澜纪元", "有职业玩家进入传送门时");
            捕获器.断言包含("MMORPG", "有职业玩家进入传送门时");
        }

        @Test
        @DisplayName("英文玩家应看到：please select your character first.")
        void 英文无职业提示_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 传送门监听器.class, "无职业");

            捕获器.发布到报告(报告);
            捕获器.断言包含("select your character", "英文玩家无职业提示");
        }

        @Test
        @DisplayName("英文玩家应看到：Entering Mulan Epoch MMORPG Server...")
        void 英文服务器欢迎_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 传送门监听器.class, "服务器欢迎");

            捕获器.发布到报告(报告);
            捕获器.断言包含("MMORPG Server", "英文玩家服务器欢迎");
        }
    }
}
