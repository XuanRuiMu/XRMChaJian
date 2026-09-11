package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class 蜘蛛控制监听器 implements Listener {
    private static final int 最小间隔 = 1;

    private final 额外功能配置服务 配置服务;
    private final JavaPlugin 插件;

    @Inject
    public 蜘蛛控制监听器(额外功能配置服务 配置服务, JavaPlugin 插件) {
        this.配置服务 = 配置服务;
        this.插件 = 插件;
        启动定时任务();
    }

    private void 启动定时任务() {
        if (!配置服务.总开关() || !配置服务.蜘蛛永久敌对()) {
            return;
        }
        int 间隔 = Math.max(最小间隔, 配置服务.蜘蛛检查间隔Tick());
        Bukkit.getScheduler().runTaskTimer(插件, this::强制蜘蛛敌对, 间隔, 间隔);
    }

    private void 强制蜘蛛敌对() {
        for (World 世界 : Bukkit.getWorlds()) {
            for (Spider 蜘蛛 : 世界.getEntitiesByClass(Spider.class)) {
                强制敌对玩家(蜘蛛, 配置服务.蜘蛛索敌范围());
            }
        }
    }

    private void 强制敌对玩家(Mob 怪物, double 范围) {
        if (怪物.getTarget() instanceof Player) {
            return;
        }
        Player 最近 = 找最近玩家(怪物, 范围);
        if (最近 != null) {
            怪物.setTarget(最近);
        }
    }

    private Player 找最近玩家(Entity 实体, double 范围) {
        Player 最近 = null;
        double 最近距离平方 = 范围 * 范围;
        for (Player 玩家 : 实体.getWorld().getPlayers()) {
            if (玩家.getGameMode() == GameMode.SPECTATOR || 玩家.isDead()) {
                continue;
            }
            double 距离平方 = 玩家.getLocation().distanceSquared(实体.getLocation());
            if (距离平方 <= 最近距离平方) {
                最近距离平方 = 距离平方;
                最近 = 玩家;
            }
        }
        return 最近;
    }
}
