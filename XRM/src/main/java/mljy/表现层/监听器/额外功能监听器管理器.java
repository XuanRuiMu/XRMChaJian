package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.额外功能.额外功能配置服务;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class 额外功能监听器管理器 {
    private final 额外功能配置服务 配置服务;
    private final JavaPlugin 插件;
    private final PluginManager 插件管理器;

    private final 伤害无敌帧监听器 伤害无敌帧监听器;
    private final 岩浆块免疫监听器 岩浆块免疫监听器;
    private final 末影人控制监听器 末影人控制监听器;
    private final 猪灵控制监听器 猪灵控制监听器;
    private final 蜘蛛控制监听器 蜘蛛控制监听器;
    private final 击退抗性监听器 击退抗性监听器;

    @Inject
    public 额外功能监听器管理器(
        额外功能配置服务 配置服务,
        JavaPlugin 插件,
        伤害无敌帧监听器 伤害无敌帧监听器,
        岩浆块免疫监听器 岩浆块免疫监听器,
        末影人控制监听器 末影人控制监听器,
        猪灵控制监听器 猪灵控制监听器,
        蜘蛛控制监听器 蜘蛛控制监听器,
        击退抗性监听器 击退抗性监听器
    ) {
        this.配置服务 = 配置服务;
        this.插件 = 插件;
        this.插件管理器 = 插件.getServer().getPluginManager();
        this.伤害无敌帧监听器 = 伤害无敌帧监听器;
        this.岩浆块免疫监听器 = 岩浆块免疫监听器;
        this.末影人控制监听器 = 末影人控制监听器;
        this.猪灵控制监听器 = 猪灵控制监听器;
        this.蜘蛛控制监听器 = 蜘蛛控制监听器;
        this.击退抗性监听器 = 击退抗性监听器;
    }

    public void 注册所有监听器() {
        if (!配置服务.总开关()) {
            return;
        }

        if (配置服务.取消玩家无敌帧() || 配置服务.取消生物无敌帧()) {
            插件管理器.registerEvents(伤害无敌帧监听器, 插件);
        }

        if (配置服务.玩家岩浆块免疫() || 配置服务.狗岩浆块免疫()) {
            插件管理器.registerEvents(岩浆块免疫监听器, 插件);
        }

        if (配置服务.末影人免疫水雨伤害() || 配置服务.末影人禁止传送()) {
            插件管理器.registerEvents(末影人控制监听器, 插件);
        }

        if (配置服务.猪灵取消以物易物() || 配置服务.猪灵永久敌对() || 配置服务.猪灵阻止僵尸化()) {
            插件管理器.registerEvents(猪灵控制监听器, 插件);
        }

        if (配置服务.蜘蛛永久敌对()) {
            插件管理器.registerEvents(蜘蛛控制监听器, 插件);
        }

        if (配置服务.击退抗性开启()) {
            插件管理器.registerEvents(击退抗性监听器, 插件);
        }
    }
}
