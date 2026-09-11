package mljy.业务层;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.领域层.战斗.角色类型;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仇恨服务实现。
 * 管理怪物的仇恨列表，仇恨系数按角色类型区分（坦克3倍/治疗0.5倍/输出1倍）。
 * 40格距离检测，不需要视线也可以维持仇恨。
 * 仇恨自然消失条件：玩家已死亡、玩家距离超过40格、玩家不在线、怪物已死亡。
 * 注意：只要玩家不死且距离在40格内，仇恨不会随时间衰减。
 */
public class 仇恨服务实现 implements 仇恨服务 {
    private static final String 配置根键 = "战斗";
    private static final String 键仇恨系数 = "仇恨系数";
    private static final String 键坦克系数 = "坦克";
    private static final String 键治疗系数 = "治疗";
    private static final String 键输出系数 = "输出";
    private static final String 键仇恨最大距离 = "仇恨最大距离";
    private static final String 键仇恨清空范围 = "仇恨清空范围";

    private static final double 默认坦克系数 = 3.0;
    private static final double 默认治疗系数 = 0.5;
    private static final double 默认输出系数 = 1.0;
    private static final double 默认仇恨最大距离 = 40.0;
    private static final double 默认仇恨清空范围 = 40.0;

    private final 职业类型服务 职业类型服务;
    private final 玩家服务 玩家服务;

    private final Map<UUID, Map<UUID, Double>> 仇恨表 = new ConcurrentHashMap<>();
    private volatile boolean 调试模式 = false;

    private final double 坦克系数;
    private final double 治疗系数;
    private final double 输出系数;
    private final double 仇恨最大距离;
    private final double 仇恨清空范围;

    @Inject
    public 仇恨服务实现(职业类型服务 职业类型服务, 玩家服务 玩家服务, JavaPlugin 插件) {
        this.职业类型服务 = 职业类型服务;
        this.玩家服务 = 玩家服务;
        FileConfiguration 配置 = 插件.getConfig();
        String 系数根 = 配置根键 + "." + 键仇恨系数;
        this.坦克系数 = 配置.getDouble(系数根 + "." + 键坦克系数, 默认坦克系数);
        this.治疗系数 = 配置.getDouble(系数根 + "." + 键治疗系数, 默认治疗系数);
        this.输出系数 = 配置.getDouble(系数根 + "." + 键输出系数, 默认输出系数);
        this.仇恨最大距离 = 配置.getDouble(配置根键 + "." + 键仇恨最大距离, 默认仇恨最大距离);
        this.仇恨清空范围 = 配置.getDouble(配置根键 + "." + 键仇恨清空范围, 默认仇恨清空范围);
    }

    @Override
    public void 记录伤害仇恨(UUID 怪物标识, UUID 玩家标识, double 伤害值) {
        角色类型 类型 = 获取玩家角色类型(玩家标识);
        记录伤害仇恨(怪物标识, 玩家标识, 伤害值, 类型);
    }

    @Override
    public void 记录伤害仇恨(UUID 怪物标识, UUID 玩家标识, double 伤害值, 角色类型 施法者类型) {
        if (伤害值 <= 0) {
            return;
        }
        double 系数 = 获取仇恨系数(施法者类型);
        double 仇恨值 = 伤害值 * 系数;
        仇恨表.computeIfAbsent(怪物标识, k -> new ConcurrentHashMap<>())
                .merge(玩家标识, 仇恨值, (Double a, Double b) -> a + b);
    }

    @Override
    public void 记录治疗仇恨(UUID 被治疗者标识, UUID 治疗者标识, double 治疗值) {
        if (治疗值 <= 0) {
            return;
        }
        角色类型 治疗者类型 = 获取玩家角色类型(治疗者标识);
        double 系数 = 获取仇恨系数(治疗者类型);
        double 仇恨值 = 治疗值 * 系数;
        for (Map.Entry<UUID, Map<UUID, Double>> 条目 : 仇恨表.entrySet()) {
            Map<UUID, Double> 怪物仇恨表 = 条目.getValue();
            if (怪物仇恨表.containsKey(被治疗者标识)) {
                怪物仇恨表.merge(治疗者标识, 仇恨值, (Double a, Double b) -> a + b);
            }
        }
    }

    @Override
    public Map<UUID, Double> 获取仇恨列表(UUID 怪物标识) {
        Map<UUID, Double> 列表 = 仇恨表.get(怪物标识);
        if (列表 == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(列表));
    }

    @Override
    public Optional<UUID> 获取仇恨最高目标(UUID 怪物标识) {
        Map<UUID, Double> 列表 = 仇恨表.get(怪物标识);
        if (列表 == null || 列表.isEmpty()) {
            return Optional.empty();
        }
        UUID 最高目标 = null;
        double 最高仇恨 = -1;
        for (Map.Entry<UUID, Double> 条目 : 列表.entrySet()) {
            if (条目.getValue() > 最高仇恨) {
                最高仇恨 = 条目.getValue();
                最高目标 = 条目.getKey();
            }
        }
        return Optional.ofNullable(最高目标);
    }

    @Override
    public Optional<UUID> 获取当前目标(UUID 怪物标识) {
        清除怪物无效仇恨(怪物标识);
        return 获取仇恨最高目标(怪物标识);
    }

    @Override
    public void 清除怪物仇恨(UUID 怪物标识) {
        仇恨表.remove(怪物标识);
    }

    @Override
    public void 清除玩家仇恨(UUID 玩家标识) {
        for (Map<UUID, Double> 怪物仇恨表 : 仇恨表.values()) {
            怪物仇恨表.remove(玩家标识);
        }
    }

    @Override
    public int 清除范围仇恨(UUID 中心玩家标识, double 范围) {
        Player 中心玩家 = Bukkit.getPlayer(中心玩家标识);
        if (中心玩家 == null) {
            return 0;
        }
        Location 中心位置 = 中心玩家.getLocation();
        double 实际范围 = 范围 > 0 ? 范围 : 仇恨清空范围;
        double 范围平方 = 实际范围 * 实际范围;
        int 清除数量 = 0;
        List<UUID> 待清除 = new ArrayList<>();
        for (UUID 怪物标识 : 仇恨表.keySet()) {
            Entity 怪物 = Bukkit.getEntity(怪物标识);
            if (怪物 == null) {
                待清除.add(怪物标识);
                continue;
            }
            if (怪物.getWorld().equals(中心玩家.getWorld())
                    && 怪物.getLocation().distanceSquared(中心位置) <= 范围平方) {
                待清除.add(怪物标识);
            }
        }
        for (UUID 标识 : 待清除) {
            仇恨表.remove(标识);
            清除数量++;
        }
        return 清除数量;
    }

    @Override
    public void 清除无效仇恨() {
        List<UUID> 待清除怪物 = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Double>> 条目 : 仇恨表.entrySet()) {
            UUID 怪物标识 = 条目.getKey();
            Entity 怪物 = Bukkit.getEntity(怪物标识);
            if (怪物 == null || 怪物.isDead() || !(怪物 instanceof LivingEntity)) {
                待清除怪物.add(怪物标识);
                continue;
            }
            清除怪物无效仇恨(怪物标识, 怪物);
        }
        for (UUID 标识 : 待清除怪物) {
            仇恨表.remove(标识);
        }
    }

    private void 清除怪物无效仇恨(UUID 怪物标识) {
        Entity 怪物 = Bukkit.getEntity(怪物标识);
        if (怪物 == null || 怪物.isDead() || !(怪物 instanceof LivingEntity)) {
            仇恨表.remove(怪物标识);
            return;
        }
        清除怪物无效仇恨(怪物标识, 怪物);
    }

    private void 清除怪物无效仇恨(UUID 怪物标识, Entity 怪物) {
        Map<UUID, Double> 怪物仇恨表 = 仇恨表.get(怪物标识);
        if (怪物仇恨表 == null) {
            return;
        }
        Location 怪物位置 = 怪物.getLocation();
        double 距离平方 = 仇恨最大距离 * 仇恨最大距离;
        List<UUID> 待清除玩家 = new ArrayList<>();
        for (UUID 玩家标识 : 怪物仇恨表.keySet()) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
                待清除玩家.add(玩家标识);
                continue;
            }
            if (!玩家.getWorld().equals(怪物.getWorld())) {
                待清除玩家.add(玩家标识);
                continue;
            }
            if (玩家.getLocation().distanceSquared(怪物位置) > 距离平方) {
                待清除玩家.add(玩家标识);
            }
        }
        for (UUID 标识 : 待清除玩家) {
            怪物仇恨表.remove(标识);
        }
        if (怪物仇恨表.isEmpty()) {
            仇恨表.remove(怪物标识);
        }
    }

    @Override
    public List<仇恨信息条目> 获取附近仇恨信息(UUID 中心玩家标识, double 范围) {
        Player 中心玩家 = Bukkit.getPlayer(中心玩家标识);
        if (中心玩家 == null) {
            return Collections.emptyList();
        }
        Location 中心位置 = 中心玩家.getLocation();
        double 实际范围 = 范围 > 0 ? 范围 : 仇恨清空范围;
        double 范围平方 = 实际范围 * 实际范围;
        List<仇恨信息条目> 信息 = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Double>> 条目 : 仇恨表.entrySet()) {
            Entity 怪物 = Bukkit.getEntity(条目.getKey());
            if (怪物 == null) {
                continue;
            }
            if (!怪物.getWorld().equals(中心玩家.getWorld())
                    || 怪物.getLocation().distanceSquared(中心位置) > 范围平方) {
                continue;
            }
            String 怪物名 = 怪物.getName();
            Map<String, Double> 玩家仇恨表 = new LinkedHashMap<>();
            for (Map.Entry<UUID, Double> 仇恨条目 : 条目.getValue().entrySet()) {
                Player 玩家 = Bukkit.getPlayer(仇恨条目.getKey());
                String 玩家名 = 玩家 != null ? 玩家.getName() : 仇恨条目.getKey().toString();
                玩家仇恨表.put(玩家名, 仇恨条目.getValue());
            }
            信息.add(new 仇恨信息条目(怪物名, 条目.getKey(), 玩家仇恨表));
        }
        return 信息;
    }

    @Override
    public void 设置调试模式(boolean 开启) {
        this.调试模式 = 开启;
    }

    @Override
    public boolean 是否调试模式() {
        return 调试模式;
    }

    @Override
    public double 获取仇恨系数(角色类型 类型) {
        return switch (类型) {
            case 坦克 -> 坦克系数;
            case 治疗 -> 治疗系数;
            case 输出 -> 输出系数;
        };
    }

    private 角色类型 获取玩家角色类型(UUID 玩家标识) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家标识);
        if (会话.isEmpty()) {
            return 角色类型.输出;
        }
        return 职业类型服务.获取角色类型(会话.get().获取专精())
                .orElse(角色类型.输出);
    }
}
