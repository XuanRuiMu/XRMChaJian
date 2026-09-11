package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class 冒险模式强制监听器 implements Listener {

    private final 登录服插件 插件;
    private final 消息管理器 消息;

    public 冒险模式强制监听器(登录服插件 插件) {
        this.插件 = 插件;
        this.消息 = 插件.获取消息管理器();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        Bukkit.getScheduler().runTaskLater(
            插件.获取JavaPlugin(),
            () -> {
                if (玩家.isOnline() && 玩家.getGameMode() != GameMode.ADVENTURE) {
                    玩家.setGameMode(GameMode.ADVENTURE);
                }
            },
            1L
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent 事件) {
        Player 玩家 = 事件.getPlayer();

        if (玩家.isOp()) {
            return;
        }

        GameMode 新模式 = 事件.getNewGameMode();
        if (新模式 != GameMode.ADVENTURE) {
            事件.setCancelled(true);
            消息.发送类翻译(玩家, 冒险模式强制监听器.class, "游戏模式被阻止");
        }
    }
}
