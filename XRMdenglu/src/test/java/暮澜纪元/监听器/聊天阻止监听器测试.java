package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("聊天阻止监听器")
@SuppressWarnings("null")
class 聊天阻止监听器测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;
    private 聊天阻止监听器 监听器;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "监听器/聊天阻止监听器");
        when(mock插件.获取消息管理器()).thenReturn(消息);
        监听器 = new 聊天阻止监听器(mock插件);
    }

    @Nested
    @DisplayName("聊天事件")
    class 聊天事件 {

        @Test
        @DisplayName("聊天栏应输出：登录服禁止聊天。")
        void 聊天事件_应取消并发送提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            AsyncChatEvent mock事件 = mock(AsyncChatEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());

            监听器.onAsyncChat(mock事件);

            捕获器.发布到报告(报告);
            verify(mock事件).setCancelled(true);
            捕获器.断言包含("登录服禁止聊天", "聊天被阻止时");
        }

        @Test
        @DisplayName("多次聊天 → 聊天栏应输出：登录服禁止聊天。")
        void 多次聊天_均应被取消(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            AsyncChatEvent mock事件1 = mock(AsyncChatEvent.class);
            when(mock事件1.getPlayer()).thenReturn(捕获器.获取玩家());
            AsyncChatEvent mock事件2 = mock(AsyncChatEvent.class);
            when(mock事件2.getPlayer()).thenReturn(捕获器.获取玩家());

            监听器.onAsyncChat(mock事件1);
            监听器.onAsyncChat(mock事件2);

            捕获器.发布到报告(报告);
            verify(mock事件1).setCancelled(true);
            verify(mock事件2).setCancelled(true);
            捕获器.断言消息数量(2, "两次聊天事件");
        }

        @Test
        @DisplayName("英文玩家聊天栏应输出：Chat is disabled on the login server.")
        void 英文玩家_应看到英文提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            AsyncChatEvent mock事件 = mock(AsyncChatEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());

            监听器.onAsyncChat(mock事件);

            捕获器.发布到报告(报告);
            捕获器.断言包含("Chat is disabled", "英文玩家聊天被阻止时");
        }
    }
}
