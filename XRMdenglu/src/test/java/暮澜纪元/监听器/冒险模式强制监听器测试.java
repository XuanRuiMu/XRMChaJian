package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import org.bukkit.GameMode;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("冒险模式强制监听器")
@SuppressWarnings("null")
class 冒险模式强制监听器测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;
    private 冒险模式强制监听器 监听器;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "监听器/冒险模式强制监听器");
        when(mock插件.获取消息管理器()).thenReturn(消息);
        when(mock插件.获取JavaPlugin()).thenReturn(null);
        监听器 = new 冒险模式强制监听器(mock插件);
    }

    @Nested
    @DisplayName("游戏模式变更")
    class 游戏模式变更 {

        @Test
        @DisplayName("聊天栏应输出：你的游戏模式已被锁定为冒险模式。")
        void 非OP玩家切换生存模式_应取消事件并发送提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.SURVIVAL);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            verify(mock事件).setCancelled(true);
            捕获器.断言包含("冒险模式", "非OP玩家切换生存模式时");
        }

        @Test
        @DisplayName("非OP玩家切换创造模式 → 聊天栏应输出：你的游戏模式已被锁定为冒险模式。")
        void 非OP玩家切换创造模式_应取消事件并发送提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.CREATIVE);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            捕获器.断言包含("冒险模式", "非OP玩家切换创造模式时");
        }

        @Test
        @DisplayName("非OP玩家切换旁观模式 → 聊天栏应输出：你的游戏模式已被锁定为冒险模式。")
        void 非OP玩家切换旁观模式_应取消事件并发送提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.SPECTATOR);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            捕获器.断言包含("冒险模式", "非OP玩家切换旁观模式时");
        }

        @Test
        @DisplayName("OP玩家切换模式不应收到消息")
        void OP玩家切换生存模式_不应取消事件(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置OP(true);
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.SURVIVAL);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            verify(mock事件, never()).setCancelled(anyBoolean());
            捕获器.断言无消息("OP玩家切换模式时");
        }

        @Test
        @DisplayName("非OP玩家切换冒险模式不应收到消息")
        void 非OP玩家切换冒险模式_不应取消事件(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.ADVENTURE);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            捕获器.断言无消息("非OP玩家切换冒险模式时");
        }

        @Test
        @DisplayName("英文玩家应看到：Your game mode is locked to Adventure mode.")
        void 英文玩家_应看到英文提示(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            PlayerGameModeChangeEvent mock事件 = mock(PlayerGameModeChangeEvent.class);
            when(mock事件.getPlayer()).thenReturn(捕获器.获取玩家());
            when(mock事件.getNewGameMode()).thenReturn(GameMode.SURVIVAL);

            监听器.onGameModeChange(mock事件);

            捕获器.发布到报告(报告);
            捕获器.断言包含("Adventure mode", "英文玩家游戏模式被阻止时");
        }
    }
}
