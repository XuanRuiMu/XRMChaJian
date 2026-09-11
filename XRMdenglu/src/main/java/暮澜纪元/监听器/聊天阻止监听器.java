package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class 聊天阻止监听器 implements Listener {

    private final 消息管理器 消息;

    public 聊天阻止监听器(登录服插件 插件) {
        this.消息 = 插件.获取消息管理器();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent 事件) {
        事件.setCancelled(true);
        Player 玩家 = 事件.getPlayer();
        消息.发送类翻译(玩家, 聊天阻止监听器.class, "聊天被阻止");
    }
}
