package mljy.表现层.计分板;

import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 计分板监听器。
 * 监听玩家加入和退出事件，创建/删除计分板。
 * 使用MONITOR优先级确保在玩家会话和资源初始化之后执行。
 */
public class 计分板监听器 implements Listener {
    private final 计分板服务 计分板服务;

    @Inject
    public 计分板监听器(计分板服务 计分板服务) {
        this.计分板服务 = 计分板服务;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void 玩家加入(PlayerJoinEvent 事件) {
        计分板服务.创建计分板(事件.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void 玩家退出(PlayerQuitEvent 事件) {
        计分板服务.删除计分板(事件.getPlayer());
    }
}
