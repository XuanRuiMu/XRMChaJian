package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.调试日志器;
import mljy.业务层.效果调度服务;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.UUID;

public class 效果生命周期监听器 implements Listener {
    private final 效果调度服务 效果调度服务;

    @Inject
    public 效果生命周期监听器(效果调度服务 效果调度服务) {
        this.效果调度服务 = 效果调度服务;
    }

    @EventHandler
    public void 玩家死亡(PlayerDeathEvent 事件) {
        清理效果(事件.getEntity(), "玩家死亡");
    }

    @EventHandler
    public void 玩家切换世界(PlayerChangedWorldEvent 事件) {
        清理效果(事件.getPlayer(), "世界切换");
    }

    private void 清理效果(Player 玩家, String 清理原因) {
        UUID 玩家标识 = 玩家.getUniqueId();
        调试日志器.调试("效果生命周期监听器", "生命周期事件清理效果：玩家=%s 原因=%s",
                玩家标识, 清理原因);
        效果调度服务.清空(玩家标识, 清理原因);
    }
}
