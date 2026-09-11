package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.业务层.伤害计算服务;
import mljy.战斗服务;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class 内存战斗服务 implements 战斗服务 {
    private static final String 模块名 = "内存战斗服务";
    public static final String 技能伤害标记键 = "xrm_skill_damage";
    private final 伤害计算服务 伤害计算服务;
    private final NamespacedKey 技能伤害键;

    @Inject
    public 内存战斗服务(伤害计算服务 伤害计算服务, Plugin 插件) {
        this.伤害计算服务 = 伤害计算服务;
        this.技能伤害键 = new NamespacedKey(插件, 技能伤害标记键);
    }

    @Override
    public 伤害结果 应用伤害(伤害上下文 上下文) {
        伤害结果 结果 = 伤害计算服务.计算(上下文);
        if (上下文.目标() instanceof 实体适配器 适配实体) {
            Entity 原始 = 适配实体.获取原始实体();
            if (原始 instanceof LivingEntity 生物 && 结果.最终数值() > 0) {
                if (上下文.施法者() == null) {
                    return 结果;
                }
                Entity 攻击者 = Bukkit.getEntity(上下文.施法者().唯一标识());
                if (攻击者 == null) {
                    return 结果;
                }
                调试日志器.调试(模块名,
                        "应用伤害前：目标=%s 目标类型=%s 目标名称=%s 传入伤害=%.2f 攻击者=%s",
                        生物.getUniqueId(), 生物.getType(), 生物.getName(),
                        结果.最终数值(), 攻击者.getUniqueId());
                生物.getPersistentDataContainer().set(技能伤害键, PersistentDataType.DOUBLE, 结果.最终数值());
                try {
                    生物.damage(结果.最终数值(), 攻击者);
                    调试日志器.调试(模块名,
                            "应用伤害后：目标=%s 目标当前血量=%.2f",
                            生物.getUniqueId(), 生物.getHealth());
                } finally {
                    生物.getPersistentDataContainer().remove(技能伤害键);
                }
            }
        }
        return 结果;
    }
}
