package mljy.业务层;

import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.领域层.实体;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class 目标选择服务实现 implements 目标选择服务 {
    @Override
    public List<实体> 选择敌人(玩家快照 施法者, 位置 中心, double 半径) {
        return 选择范围内(中心, 半径, 实体 -> {
            if (!(实体 instanceof 实体适配器 适配实体)) {
                return false;
            }
            Entity 原始 = 适配实体.获取原始实体();
            if (!(原始 instanceof LivingEntity)) {
                return false;
            }
            if (原始 instanceof Player 玩家 && 玩家.getUniqueId().equals(施法者.唯一标识())) {
                return false;
            }
            return true;
        });
    }

    @Override
    public List<实体> 选择友方(玩家快照 施法者, 位置 中心, double 半径) {
        return 选择范围内(中心, 半径, 实体 -> {
            if (!(实体 instanceof 实体适配器 适配实体)) {
                return false;
            }
            Entity 原始 = 适配实体.获取原始实体();
            if (原始 instanceof Player 玩家) {
                return 玩家.getUniqueId().equals(施法者.唯一标识());
            }
            return false;
        });
    }

    @Override
    public 实体 选择最近敌人(玩家快照 施法者, 位置 中心, double 半径) {
        return 选择敌人(施法者, 中心, 半径).stream()
                .min(Comparator.comparingDouble(实体 -> 实体.获取位置().距离平方(中心)))
                .orElse(null);
    }

    @Override
    public List<实体> 选择范围内(位置 中心, double 半径, Predicate<实体> 过滤条件) {
        List<实体> 结果 = new ArrayList<>();
        World 世界 = org.bukkit.Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            return 结果;
        }
        // FP-01：球形判定——先AABB候选再过滤球形距离
        double 半径平方 = 半径 * 半径;
        org.bukkit.Location 中心位置 = 位置适配器.转换(中心);
        for (Entity 原始实体 : 世界.getNearbyEntities(中心位置, 半径, 半径, 半径)) {
            实体适配器 适配实体 = new 实体适配器(原始实体);
            if (过滤条件.test(适配实体)) {
                org.bukkit.Location 实体位置 = 原始实体.getLocation();
                if (实体位置 != null && 中心位置.distanceSquared(实体位置) <= 半径平方) {
                    结果.add(适配实体);
                }
            }
        }
        return 结果;
    }
}
