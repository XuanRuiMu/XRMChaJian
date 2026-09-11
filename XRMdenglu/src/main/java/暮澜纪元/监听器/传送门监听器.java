package 暮澜纪元.监听器;

import 暮澜纪元.传送门区域;
import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.菜单.职业选择GUI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送门监听器。
 * 检测玩家移动并处理传送门传送逻辑。
 */
public class 传送门监听器 implements Listener {

    private final 登录服插件 插件;
    private final 职业选择GUI 职业选择GUI实例;
    private final 消息管理器 消息;
    private final Map<UUID, Long> 冷却列表 = new ConcurrentHashMap<>();
    private static final long 冷却时间 = 3000L;

    public 传送门监听器(登录服插件 插件, 职业选择GUI 职业选择GUI实例) {
        this.插件 = 插件;
        this.职业选择GUI实例 = 职业选择GUI实例;
        this.消息 = 插件.获取消息管理器();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent 事件) {
        if (事件.getFrom().getBlockX() == 事件.getTo().getBlockX() &&
            事件.getFrom().getBlockY() == 事件.getTo().getBlockY() &&
            事件.getFrom().getBlockZ() == 事件.getTo().getBlockZ()) {
            return;
        }

        Player 玩家 = 事件.getPlayer();
        Location 位置 = 事件.getTo();

        for (传送门区域 传送门 : 插件.获取登录配置().获取传送门列表().values()) {
            if (传送门.是否在区域内(位置.getWorld().getName(),
                位置.getBlockX(), 位置.getBlockY(), 位置.getBlockZ())) {
                检查并传送(玩家, 传送门);
                break;
            }
        }
    }

    private void 检查并传送(Player 玩家, 传送门区域 传送门) {
        UUID 玩家UUID = 玩家.getUniqueId();
        long 当前时间 = System.currentTimeMillis();

        if (冷却列表.containsKey(玩家UUID) &&
            当前时间 - 冷却列表.get(玩家UUID) < 冷却时间) {
            return;
        }

        冷却列表.put(玩家UUID, 当前时间);

        var 玩家数据 = 职业选择GUI实例.获取玩家数据(玩家);

        if (!玩家数据.是否有职业()) {
            消息.发送类翻译(玩家, 传送门监听器.class, "无职业");
            职业选择GUI实例.打开职业选择菜单(玩家);
            return;
        }

        消息.发送类翻译(玩家, 传送门监听器.class, "服务器欢迎");
        传送玩家到服务器(玩家, 传送门.获取目标服务器());
    }

    private void 传送玩家到服务器(Player 玩家, String 服务器名) {
        插件.传送玩家到服务器(玩家, 服务器名);
    }
}
