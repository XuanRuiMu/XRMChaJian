package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 伤害无敌帧监听器 implements Listener {
    private static final long 毫秒每秒 = 1000L;
    private static final int 最小间隔 = 1;

    private final 额外功能配置服务 配置服务;
    private final JavaPlugin 插件;
    private final ConcurrentHashMap<UUID, Long> 伤害时间记录 = new ConcurrentHashMap<>();

    @Inject
    public 伤害无敌帧监听器(额外功能配置服务 配置服务, JavaPlugin 插件) {
        this.配置服务 = 配置服务;
        this.插件 = 插件;
        启动定时任务();
    }

    private void 启动定时任务() {
        if (!配置服务.总开关()) {
            return;
        }
        if (配置服务.取消玩家无敌帧() || 配置服务.取消生物无敌帧()) {
            int 间隔 = Math.max(最小间隔, 配置服务.监控间隔Tick());
            Bukkit.getScheduler().runTaskTimer(插件, this::监控无敌帧, 间隔, 间隔);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void 实体受伤(EntityDamageEvent 事件) {
        if (!配置服务.总开关()) {
            return;
        }
        记录伤害时间(事件.getEntity());
    }

    private void 记录伤害时间(Entity 实体) {
        if (!(实体 instanceof LivingEntity)) {
            return;
        }
        boolean 应记录 = false;
        if (实体 instanceof Player && 配置服务.取消玩家无敌帧()) {
            应记录 = true;
        } else if (!(实体 instanceof Player) && 配置服务.取消生物无敌帧()) {
            应记录 = true;
        }
        if (应记录) {
            伤害时间记录.put(实体.getUniqueId(), System.currentTimeMillis());
        }
    }

    private void 监控无敌帧() {
        long 当前时间 = System.currentTimeMillis();
        long 窗口 = 配置服务.监控持续秒() * 毫秒每秒;
        伤害时间记录.entrySet().removeIf(条目 -> 当前时间 - 条目.getValue() > 窗口);
        for (UUID 标识 : 伤害时间记录.keySet()) {
            Entity 实体 = Bukkit.getEntity(标识);
            if (实体 instanceof LivingEntity 生物) {
                生物.setNoDamageTicks(0);
            }
        }
    }
}
