package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class 击退抗性监听器 implements Listener {
    private final 额外功能配置服务 配置服务;
    private final JavaPlugin 插件;
    private final Attribute 击退抗性属性;

    @Inject
    public 击退抗性监听器(额外功能配置服务 配置服务, JavaPlugin 插件) {
        this.配置服务 = 配置服务;
        this.插件 = 插件;
        this.击退抗性属性 = 解析击退抗性属性();
        启动定时任务();
    }

    private static Attribute 解析击退抗性属性() {
        Attribute 属性 = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("knockback_resistance"));
        if (属性 == null) {
            属性 = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.knockback_resistance"));
        }
        return 属性;
    }

    private void 启动定时任务() {
        if (!配置服务.总开关() || !配置服务.击退抗性开启()) {
            return;
        }
        Bukkit.getScheduler().runTask(插件, this::应用所有实体击退抗性);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void 生物生成(CreatureSpawnEvent 事件) {
        if (!配置服务.总开关() || !配置服务.击退抗性开启()) {
            return;
        }
        应用击退抗性(事件.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void 玩家加入(PlayerJoinEvent 事件) {
        if (!配置服务.总开关() || !配置服务.击退抗性开启()) {
            return;
        }
        应用击退抗性(事件.getPlayer());
    }

    private void 应用所有实体击退抗性() {
        for (World 世界 : Bukkit.getWorlds()) {
            for (LivingEntity 实体 : 世界.getLivingEntities()) {
                应用击退抗性(实体);
            }
        }
    }

    private void 应用击退抗性(LivingEntity 实体) {
        if (击退抗性属性 == null) {
            return;
        }
        AttributeInstance 属性 = 实体.getAttribute(击退抗性属性);
        if (属性 != null) {
            属性.setBaseValue(配置服务.击退抗性值());
        }
    }
}
