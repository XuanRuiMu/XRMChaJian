package 暮澜纪元.监听器;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.重生点配置;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

/**
 * 重生点监听器。
 * 玩家加入游戏时传送到登录服重生点。
 */
public class 重生点监听器 implements Listener {

    private final 登录服插件 插件;

    public 重生点监听器(登录服插件 插件) {
        this.插件 = 插件;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent 事件) {
        Optional<Location> 位置可选 = 获取重生点位置();
        if (位置可选.isEmpty()) {
            return;
        }

        Location 位置 = 位置可选.get();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (事件.getPlayer().isOnline()) {
                    事件.getPlayer().teleport(位置);
                }
            }
        }.runTaskLater(插件.获取JavaPlugin(), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent 事件) {
        Optional<Location> 位置可选 = 获取重生点位置();
        位置可选.ifPresent(事件::setRespawnLocation);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent 事件) {
        插件.获取数据库().移除缓存(事件.getPlayer().getUniqueId());
    }

    private Optional<Location> 获取重生点位置() {
        重生点配置 重生点 = 插件.获取登录配置().获取重生点();
        if (重生点 == null) {
            return Optional.empty();
        }

        Optional<Location> 位置 = 重生点.构建位置();
        if (位置.isEmpty()) {
            插件.getLogger().warning("找不到重生点世界: " + 重生点.获取世界名());
        }
        return 位置;
    }
}
