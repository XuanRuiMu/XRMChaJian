package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.基础设施层.调试日志器;
import mljy.业务层.技能释放服务;
import mljy.业务层.技能释放服务实现;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 技能移动打断监听器 implements Listener {
    private final 技能释放服务实现 技能释放服务;
    private final Map<UUID, Location> 蓄力起始位置表 = new ConcurrentHashMap<>();

    @Inject
    public 技能移动打断监听器(技能释放服务 技能释放服务) {
        this.技能释放服务 = (技能释放服务实现) 技能释放服务;
    }

    public void 记录蓄力起始位置(UUID 玩家标识, Location 位置) {
        蓄力起始位置表.put(玩家标识, 位置.clone());
        调试日志器.调试("移动打断监听器", "记录蓄力起始位置：玩家=%s 位置=(%.1f,%.1f,%.1f)",
                玩家标识, 位置.getX(), 位置.getY(), 位置.getZ());
    }

    public void 清理蓄力位置(UUID 玩家标识) {
        蓄力起始位置表.remove(玩家标识);
        调试日志器.调试("移动打断监听器", "清理蓄力位置：玩家=%s", 玩家标识);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void 玩家移动(PlayerMoveEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        UUID 玩家标识 = 玩家.getUniqueId();

        if (!技能释放服务.是否正在蓄力(玩家标识)) {
            return;
        }

        Location 起始位置 = 蓄力起始位置表.get(玩家标识);
        if (起始位置 == null) {
            return;
        }

        Location 当前位置 = 事件.getTo();
        if (当前位置 == null) return;

        double 水平移动距离 = Math.sqrt(
            Math.pow(当前位置.getX() - 起始位置.getX(), 2) +
            Math.pow(当前位置.getZ() - 起始位置.getZ(), 2)
        );

        if (水平移动距离 > 0.1) {
            调试日志器.调试("移动打断监听器", "移动打断蓄力：玩家=%s 移动距离=%.3f 起始=(%.1f,%.1f,%.1f) 当前=(%.1f,%.1f,%.1f)",
                    玩家标识, 水平移动距离, 起始位置.getX(), 起始位置.getY(), 起始位置.getZ(),
                    当前位置.getX(), 当前位置.getY(), 当前位置.getZ());
            技能释放服务.打断蓄力(玩家标识, 技能释放服务实现.打断原因.移动);
        }
    }
}
