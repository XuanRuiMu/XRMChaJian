package 暮澜纪元.命令;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.配置.登录服配置;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.logging.Logger;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("传送管理命令")
@SuppressWarnings("null")
class 传送管理命令测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;
    private 传送管理命令 命令;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, List.of("命令/传送管理命令", "配置/消息管理器"));
        when(mock插件.获取消息管理器()).thenReturn(消息);
        lenient().when(mock插件.getLogger()).thenReturn(Logger.getLogger("XRMdenglu测试"));
        命令 = new 传送管理命令(mock插件);
    }

    @Nested
    @DisplayName("权限与基础验证")
    class 权限与基础验证 {

        @Test
        @DisplayName("非玩家执行 → 聊天栏应输出：此命令仅玩家可用")
        void 非玩家执行_应提示仅玩家可用(TestReporter 报告) {
            CommandSender mock发送者 = mock(CommandSender.class);
            Command mock命令 = mock(Command.class);

            命令.onCommand(mock发送者, mock命令, "传送管理", new String[]{});

            报告.publishEntry("说明", "非玩家执行命令时，通过CommandSender.sendMessage(Component)发送提示，无法通过玩家输出捕获器捕获");
        }

        @Test
        @DisplayName("聊天栏应输出：你没有权限执行此操作。")
        void 无权限玩家_应提示无权限(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            Command mock命令 = mock(Command.class);

            命令.onCommand(捕获器.获取玩家(), mock命令, "传送管理", new String[]{});

            捕获器.发布到报告(报告);
            捕获器.断言包含("没有权限", "无权限玩家执行命令时");
        }

        @Test
        @DisplayName("有权限玩家无参数 → 聊天栏应输出帮助信息")
        void 有权限玩家无参数_应输出帮助(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            when(捕获器.获取玩家().hasPermission("xrmdenglu.admin")).thenReturn(true);
            Command mock命令 = mock(Command.class);

            命令.onCommand(捕获器.获取玩家(), mock命令, "传送管理", new String[]{});

            捕获器.发布到报告(报告);
            捕获器.断言任意消息包含("传送管理命令帮助", "有权限玩家无参数时");
        }

        @Test
        @DisplayName("有权限玩家重载 → 聊天栏应输出：配置文件已重新加载")
        void 有权限玩家重载_应输出重载成功(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            when(捕获器.获取玩家().hasPermission("xrmdenglu.admin")).thenReturn(true);
            登录服配置 mock登录配置 = mock(登录服配置.class);
            when(mock插件.获取登录配置()).thenReturn(mock登录配置);
            Command mock命令 = mock(Command.class);

            命令.onCommand(捕获器.获取玩家(), mock命令, "传送管理", new String[]{"重载"});

            捕获器.发布到报告(报告);
            捕获器.断言包含("重新加载", "重载配置时");
        }

        @Test
        @DisplayName("有权限玩家无效服务器名 → 聊天栏应输出含'无效'的提示")
        void 有权限玩家无效服务器名_应输出提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            when(捕获器.获取玩家().hasPermission("xrmdenglu.admin")).thenReturn(true);
            Command mock命令 = mock(Command.class);

            命令.onCommand(捕获器.获取玩家(), mock命令, "传送管理", new String[]{"选点一", "不存在"});

            捕获器.发布到报告(报告);
            捕获器.断言包含("无效的服务器名", "无效服务器名时");
        }
    }
}
