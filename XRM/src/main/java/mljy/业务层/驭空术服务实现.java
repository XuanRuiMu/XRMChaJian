package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.驭空术.飞行状态;
import mljy.领域层.驭空术.驭空术配置;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 驭空术服务实现。
 * 管理玩家飞行状态、活力系统、4个技能、BossBar显示和飞行物理。
 */
@Singleton
public class 驭空术服务实现 implements 驭空术服务 {
    private static final int 每秒Tick数 = 20;
    private static final double 活力百分比绿色阈值 = 0.5;
    private static final double 活力百分比黄色阈值 = 0.2;
    private static final double 俯仰角阈值 = 20.0;
    private static final double 爆发Y分量 = 0.5;
    private static final double 俯冲Y分量 = -0.5;
    private static final String 翻译键活力不足 = "驭空术服务.活力不足";
    private static final String 翻译键活力耗尽 = "驭空术服务.活力耗尽";
    private static final String 翻译键系统未启用 = "驭空术服务.系统未启用";
    private static final String 翻译键战斗中禁用 = "驭空术服务.战斗中禁用";
    private static final String 翻译键需要骑乘 = "驭空术服务.需要骑乘";
    private static final String 翻译键冲刺成功 = "驭空术服务.冲刺成功";
    private static final String 翻译键冲刺冷却中 = "驭空术服务.冲刺冷却中";
    private static final String 翻译键爆发成功 = "驭空术服务.爆发成功";
    private static final String 翻译键爆发冷却中 = "驭空术服务.爆发冷却中";
    private static final String 翻译键俯冲成功 = "驭空术服务.俯冲成功";
    private static final String 翻译键俯冲冷却中 = "驭空术服务.俯冲冷却中";
    private static final String 翻译键起飞成功 = "驭空术服务.起飞成功";
    private static final String 翻译键进入准驭空 = "驭空术服务.进入准驭空";
    private static final String 翻译键非飞行状态 = "驭空术服务.非飞行状态";

    private final 驭空术配置 配置;
    private final 翻译服务 翻译服务;
    private final 战斗状态服务 战斗状态服务;
    private final JavaPlugin 插件;
    private final MiniMessage 迷你消息 = MiniMessage.miniMessage();
    private final Map<UUID, 玩家飞行数据> 玩家数据表 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> 最后切换飞行时间 = new ConcurrentHashMap<>();

    @Inject
    public 驭空术服务实现(驭空术配置 配置, 翻译服务 翻译服务,
                       战斗状态服务 战斗状态服务, JavaPlugin 插件) {
        this.配置 = 配置;
        this.翻译服务 = 翻译服务;
        this.战斗状态服务 = 战斗状态服务;
        this.插件 = 插件;
    }

    @Override
    public boolean 起飞(UUID 玩家标识) {
        if (!配置.总开关()) {
            发送消息(玩家标识, 翻译键系统未启用);
            return false;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return false;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家标识);
        if (数据.状态 != 飞行状态.地面 && 数据.状态 != 飞行状态.准驭空) {
            return false;
        }
        if (!检查骑乘(玩家)) {
            发送消息(玩家标识, 翻译键需要骑乘);
            return false;
        }
        if (配置.战斗中禁用() && 战斗状态服务.是否战斗中(玩家标识)) {
            发送消息(玩家标识, 翻译键战斗中禁用);
            return false;
        }
        if (数据.活力 < 配置.起飞消耗()) {
            发送消息(玩家标识, 翻译键活力不足);
            return false;
        }
        数据.活力 -= 配置.起飞消耗();
        数据.状态 = 飞行状态.起飞中;
        数据.起飞开始Tick = System.currentTimeMillis();
        玩家.setAllowFlight(true);
        玩家.setFlying(true);
        播放音效(玩家, Sound.ENTITY_ENDER_DRAGON_FLAP);
        生成粒子(玩家, Particle.CLOUD, 20);
        生成粒子(玩家, Particle.EXPLOSION, 5);
        生成粒子(玩家, Particle.END_ROD, 10);
        开始起飞任务(玩家, 数据);
        发送消息(玩家标识, 翻译键起飞成功);
        调试日志器.调试("驭空术", "起飞成功：玩家=%s", 玩家标识);
        return true;
    }

    @Override
    public boolean 冲刺(UUID 玩家标识) {
        if (!配置.总开关()) {
            发送消息(玩家标识, 翻译键系统未启用);
            return false;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return false;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家标识);
        if (数据.状态 != 飞行状态.飞行中) {
            发送消息(玩家标识, 翻译键非飞行状态);
            return false;
        }
        long 当前时间 = System.currentTimeMillis();
        if (当前时间 < 数据.冲刺冷却到期) {
            long 剩余秒 = (数据.冲刺冷却到期 - 当前时间) / 1000;
            发送消息(玩家标识, 翻译键冲刺冷却中, 剩余秒);
            return false;
        }
        if (数据.活力 < 配置.冲刺消耗()) {
            发送消息(玩家标识, 翻译键活力不足);
            return false;
        }
        数据.活力 -= 配置.冲刺消耗();
        数据.冲刺冷却到期 = 当前时间 + 配置.冲刺冷却() * 50L;
        Vector 方向 = 玩家.getLocation().getDirection().normalize();
        应用推力(玩家, 方向, 配置.冲刺推力(), 配置.冲刺持续Tick());
        播放音效(玩家, Sound.ENTITY_BREEZE_WIND_BURST);
        生成粒子(玩家, Particle.END_ROD, 10);
        生成粒子(玩家, Particle.CLOUD, 15);
        发送消息(玩家标识, 翻译键冲刺成功);
        调试日志器.调试("驭空术", "冲刺成功：玩家=%s", 玩家标识);
        return true;
    }

    @Override
    public boolean 爆发(UUID 玩家标识) {
        if (!配置.总开关()) {
            发送消息(玩家标识, 翻译键系统未启用);
            return false;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return false;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家标识);
        if (数据.状态 != 飞行状态.飞行中) {
            发送消息(玩家标识, 翻译键非飞行状态);
            return false;
        }
        long 当前时间 = System.currentTimeMillis();
        if (当前时间 < 数据.爆发冷却到期) {
            long 剩余秒 = (数据.爆发冷却到期 - 当前时间) / 1000;
            发送消息(玩家标识, 翻译键爆发冷却中, 剩余秒);
            return false;
        }
        if (数据.活力 < 配置.爆发消耗()) {
            发送消息(玩家标识, 翻译键活力不足);
            return false;
        }
        数据.活力 -= 配置.爆发消耗();
        数据.爆发冷却到期 = 当前时间 + 配置.爆发冷却() * 50L;
        Vector 方向 = 玩家.getLocation().getDirection().normalize();
        方向.setY(Math.max(方向.getY(), 爆发Y分量));
        方向.normalize();
        应用推力(玩家, 方向, 配置.爆发推力(), 配置.爆发持续Tick());
        播放音效(玩家, Sound.ENTITY_GENERIC_EXPLODE);
        生成粒子(玩家, Particle.FIREWORK, 15);
        生成粒子(玩家, Particle.FLAME, 10);
        生成粒子(玩家, Particle.CLOUD, 10);
        发送消息(玩家标识, 翻译键爆发成功);
        调试日志器.调试("驭空术", "爆发成功：玩家=%s", 玩家标识);
        return true;
    }

    @Override
    public boolean 俯冲(UUID 玩家标识) {
        if (!配置.总开关()) {
            发送消息(玩家标识, 翻译键系统未启用);
            return false;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return false;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家标识);
        if (数据.状态 != 飞行状态.飞行中) {
            发送消息(玩家标识, 翻译键非飞行状态);
            return false;
        }
        long 当前时间 = System.currentTimeMillis();
        if (当前时间 < 数据.俯冲冷却到期) {
            long 剩余秒 = (数据.俯冲冷却到期 - 当前时间) / 1000;
            发送消息(玩家标识, 翻译键俯冲冷却中, 剩余秒);
            return false;
        }
        if (数据.活力 < 配置.俯冲消耗()) {
            发送消息(玩家标识, 翻译键活力不足);
            return false;
        }
        数据.活力 -= 配置.俯冲消耗();
        数据.俯冲冷却到期 = 当前时间 + 配置.俯冲冷却() * 50L;
        Vector 方向 = 玩家.getLocation().getDirection().normalize();
        方向.setY(Math.min(方向.getY(), 俯冲Y分量));
        方向.normalize();
        应用推力(玩家, 方向, 配置.俯冲推力(), 配置.俯冲持续Tick());
        播放音效(玩家, Sound.ENTITY_PHANTOM_SWOOP);
        生成粒子(玩家, Particle.SWEEP_ATTACK, 5);
        生成粒子(玩家, Particle.CRIT, 15);
        发送消息(玩家标识, 翻译键俯冲成功);
        调试日志器.调试("驭空术", "俯冲成功：玩家=%s", 玩家标识);
        return true;
    }

    @Override
    public 飞行状态 获取飞行状态(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        return 数据 != null ? 数据.状态 : 飞行状态.地面;
    }

    @Override
    public double 获取活力(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        return 数据 != null ? 数据.活力 : 0.0;
    }

    @Override
    public double 获取活力上限(UUID 玩家标识) {
        return 配置.活力上限();
    }

    @Override
    public long 获取冲刺剩余冷却(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        if (数据 == null) {
            return 0;
        }
        long 剩余 = 数据.冲刺冷却到期 - System.currentTimeMillis();
        return Math.max(0, 剩余 / 1000);
    }

    @Override
    public long 获取爆发剩余冷却(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        if (数据 == null) {
            return 0;
        }
        long 剩余 = 数据.爆发冷却到期 - System.currentTimeMillis();
        return Math.max(0, 剩余 / 1000);
    }

    @Override
    public long 获取俯冲剩余冷却(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        if (数据 == null) {
            return 0;
        }
        long 剩余 = 数据.俯冲冷却到期 - System.currentTimeMillis();
        return Math.max(0, 剩余 / 1000);
    }

    @Override
    public void 每秒更新() {
        long 当前时间 = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, 玩家飞行数据>> 迭代器 = 玩家数据表.entrySet().iterator();
        while (迭代器.hasNext()) {
            Map.Entry<UUID, 玩家飞行数据> 条目 = 迭代器.next();
            UUID 玩家标识 = 条目.getKey();
            玩家飞行数据 数据 = 条目.getValue();
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                迭代器.remove();
                continue;
            }
            更新活力(玩家标识, 数据, 当前时间);
        }
    }

    private void 更新活力(UUID 玩家标识, 玩家飞行数据 数据, long 当前时间) {
        if (数据.状态 == 飞行状态.飞行中 || 数据.状态 == 飞行状态.起飞中) {
            数据.活力 -= 配置.飞行每秒消耗();
            数据.活力 += 配置.飞行每秒恢复();
            if (数据.活力 <= 0) {
                数据.活力 = 0;
                调试日志器.调试("驭空术", "活力耗尽：玩家=%s 活力=0", 玩家标识);
                发送消息(玩家标识, 翻译键活力耗尽);
                强制结束(玩家标识);
            }
        } else if (数据.状态 == 飞行状态.地面) {
            数据.活力 += 配置.地面每秒恢复();
        }
        数据.活力 = Math.max(0, Math.min(数据.活力, 配置.活力上限()));
    }

    @Override
    public void 每Tick更新() {
        long 当前时间 = System.currentTimeMillis();
        for (Map.Entry<UUID, 玩家飞行数据> 条目 : 玩家数据表.entrySet()) {
            UUID 玩家标识 = 条目.getKey();
            玩家飞行数据 数据 = 条目.getValue();
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                continue;
            }
            if (数据.状态 == 飞行状态.飞行中) {
                更新飞行物理(玩家, 数据);
                更新BossBar(玩家, 数据);
                检查地面接触(玩家, 数据);
                检查骑乘状态(玩家, 数据);
            } else if (数据.状态 == 飞行状态.准驭空) {
                if (当前时间 > 数据.准驭空到期) {
                    数据.状态 = 飞行状态.地面;
                }
            }
            if (数据.摔落保护到期 > 0 && 当前时间 > 数据.摔落保护到期) {
                数据.摔落保护到期 = 0;
            }
        }
    }

    private void 更新飞行物理(Player 玩家, 玩家飞行数据 数据) {
        Vector 速度 = 玩家.getVelocity();
        速度.setY(速度.getY() + 配置.重力加速度());
        double 速度平方 = 速度.lengthSquared();
        if (速度平方 > 0) {
            double 升力 = 配置.升力系数() * 速度平方;
            速度.setY(速度.getY() + 升力);
        }
        速度.multiply(1.0 - 配置.空气阻力系数());
        double 俯仰角 = 玩家.getLocation().getPitch();
        if (俯仰角 < -俯仰角阈值) {
            速度.setY(速度.getY() * 配置.抬头减速倍数());
        } else if (俯仰角 > 俯仰角阈值) {
            速度.multiply(配置.俯冲加速倍数());
        }
        double 最大速度 = 配置.最大速度();
        if (速度.length() > 最大速度) {
            速度.normalize().multiply(最大速度);
        }
        玩家.setVelocity(速度);
    }

    private void 更新BossBar(Player 玩家, 玩家飞行数据 数据) {
        if (数据.bossBar == null) {
            数据.bossBar = Bukkit.createBossBar(
                    构建BossBar标题(数据),
                    计算活力颜色(数据.活力),
                    BarStyle.SOLID);
            数据.bossBar.addPlayer(玩家);
        }
        double 进度 = 数据.活力 / 配置.活力上限();
        数据.bossBar.setProgress(Math.max(0.0, Math.min(1.0, 进度)));
        数据.bossBar.setColor(计算活力颜色(数据.活力));
        数据.bossBar.setTitle(构建BossBar标题(数据));
    }

    private BarColor 计算活力颜色(double 活力) {
        double 百分比 = 活力 / 配置.活力上限();
        if (百分比 > 活力百分比绿色阈值) {
            return BarColor.GREEN;
        }
        if (百分比 > 活力百分比黄色阈值) {
            return BarColor.YELLOW;
        }
        return BarColor.RED;
    }

    private String 构建BossBar标题(玩家飞行数据 数据) {
        return 翻译服务.获取("活力显示管理器.标题",
                String.format("%.1f", 数据.活力),
                String.format("%.1f", 配置.活力上限()));
    }

    private void 检查地面接触(Player 玩家, 玩家飞行数据 数据) {
        Location 脚下 = 玩家.getLocation().clone().subtract(0, 0.1, 0);
        if (脚下.getBlock().getType().isSolid()) {
            long 当前时间 = System.currentTimeMillis();
            if (当前时间 - 数据.起飞开始Tick > 配置.起飞升空时长() * 50L) {
                强制结束(玩家.getUniqueId());
            }
        }
    }

    private void 检查骑乘状态(Player 玩家, 玩家飞行数据 数据) {
        if (!玩家.isInsideVehicle()) {
            强制结束(玩家.getUniqueId());
        }
    }

    @Override
    public void 强制结束(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        if (数据 == null) {
            return;
        }
        if (数据.状态 == 飞行状态.地面) {
            return;
        }
        调试日志器.调试("驭空术", "强制结束：玩家=%s 当前状态=%s", 玩家标识, 数据.状态);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            if (玩家.getGameMode() != GameMode.CREATIVE && 玩家.getGameMode() != GameMode.SPECTATOR) {
                玩家.setFlying(false);
                玩家.setAllowFlight(false);
            }
            数据.摔落保护到期 = System.currentTimeMillis() + 配置.摔落保护秒数() * 1000L;
            玩家.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                    配置.摔落保护秒数() * 每秒Tick数, 0, false, false, false));
        }
        数据.状态 = 飞行状态.地面;
        if (数据.bossBar != null) {
            数据.bossBar.removeAll();
            数据.bossBar = null;
        }
    }

    @Override
    public void 玩家退出清理(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.remove(玩家标识);
        if (数据 != null && 数据.bossBar != null) {
            数据.bossBar.removeAll();
        }
    }

    @Override
    public boolean 是否摔落保护中(UUID 玩家标识) {
        玩家飞行数据 数据 = 玩家数据表.get(玩家标识);
        if (数据 == null || 数据.摔落保护到期 == 0) {
            return false;
        }
        return System.currentTimeMillis() < 数据.摔落保护到期;
    }

    @Override
    public boolean 处理切换飞行(Player 玩家) {
        if (!配置.总开关()) {
            return false;
        }
        if (!玩家.isInsideVehicle()) {
            return true;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家.getUniqueId());
        if (数据.状态 == 飞行状态.地面) {
            long 当前时间 = System.currentTimeMillis();
            Long 上次时间 = 最后切换飞行时间.put(玩家.getUniqueId(), 当前时间);
            if (上次时间 != null && 当前时间 - 上次时间 <= 配置.准驭空双击间隔毫秒()) {
                尝试进入准驭空(玩家);
            }
            return true;
        }
        if (数据.状态 == 飞行状态.准驭空) {
            起飞(玩家.getUniqueId());
            return true;
        }
        return false;
    }

    @Override
    public boolean 尝试进入准驭空(Player 玩家) {
        if (!配置.总开关()) {
            return false;
        }
        if (!玩家.isInsideVehicle()) {
            return false;
        }
        玩家飞行数据 数据 = 获取或创建数据(玩家.getUniqueId());
        if (数据.状态 != 飞行状态.地面) {
            return false;
        }
        数据.状态 = 飞行状态.准驭空;
        数据.准驭空到期 = System.currentTimeMillis() + 配置.准驭空超时秒() * 1000L;
        发送消息(玩家.getUniqueId(), 翻译键进入准驭空);
        return true;
    }

    @Override
    public boolean 处理玩家移动(Player 玩家) {
        if (!配置.总开关()) {
            return false;
        }
        玩家飞行数据 数据 = 玩家数据表.get(玩家.getUniqueId());
        return 数据 != null && (数据.状态 == 飞行状态.飞行中 || 数据.状态 == 飞行状态.起飞中);
    }

    private 玩家飞行数据 获取或创建数据(UUID 玩家标识) {
        return 玩家数据表.computeIfAbsent(玩家标识, 键 -> new 玩家飞行数据(配置.活力上限()));
    }

    private boolean 检查骑乘(Player 玩家) {
        return 玩家.isInsideVehicle();
    }

    private void 开始起飞任务(Player 玩家, 玩家飞行数据 数据) {
        Location 起飞位置 = 玩家.getLocation().clone();
        double 目标Y = 起飞位置.getY() + 配置.起飞升空高度();
        int 总Tick = 配置.起飞升空时长();
        new BukkitRunnable() {
            int 已运行Tick = 0;

            @Override
            public void run() {
                if (已运行Tick >= 总Tick) {
                    数据.状态 = 飞行状态.飞行中;
                    数据.起飞开始Tick = System.currentTimeMillis();
                    cancel();
                    return;
                }
                if (!玩家.isOnline() || !玩家.isInsideVehicle()) {
                    强制结束(玩家.getUniqueId());
                    cancel();
                    return;
                }
                double 进度 = (double) 已运行Tick / 总Tick;
                double 当前Y = 起飞位置.getY() + (目标Y - 起飞位置.getY()) * 进度;
                Location 当前位置 = 玩家.getLocation();
                玩家.teleport(new Location(当前位置.getWorld(), 当前位置.getX(), 当前Y, 当前位置.getZ(),
                        当前位置.getYaw(), 当前位置.getPitch()));
                Vector 方向 = 当前位置.getDirection().clone();
                方向.setY(0);
                if (方向.lengthSquared() > 0) {
                    方向.normalize().multiply(配置.基础速度() * 进度);
                    玩家.setVelocity(方向);
                }
                已运行Tick++;
            }
        }.runTaskTimer(插件, 0, 1);
    }

    private void 应用推力(Player 玩家, Vector 方向, double 推力, int 持续Tick) {
        new BukkitRunnable() {
            int 已运行Tick = 0;

            @Override
            public void run() {
                if (已运行Tick >= 持续Tick) {
                    cancel();
                    return;
                }
                if (!玩家.isOnline()) {
                    cancel();
                    return;
                }
                玩家飞行数据 数据 = 玩家数据表.get(playerUuid(玩家));
                if (数据 == null || 数据.状态 != 飞行状态.飞行中) {
                    cancel();
                    return;
                }
                Vector 速度 = 玩家.getVelocity();
                速度.add(方向.clone().multiply(推力 / 持续Tick));
                玩家.setVelocity(速度);
                已运行Tick++;
            }
        }.runTaskTimer(插件, 0, 1);
    }

    private UUID playerUuid(Player 玩家) {
        return 玩家.getUniqueId();
    }

    private void 播放音效(Player 玩家, Sound 音效) {
        玩家.playSound(玩家.getLocation(), 音效, 1.0f, 1.0f);
    }

    private void 生成粒子(Player 玩家, Particle 粒子, int 数量) {
        Location 位置 = 玩家.getLocation();
        玩家.getWorld().spawnParticle(粒子, 位置, 数量, 0.5, 0.5, 0.5, 0.1);
    }

    private void 发送消息(UUID 玩家标识, String 翻译键, Object... 参数) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return;
        }
        String 消息 = 翻译服务.获取(翻译键, 参数);
        玩家.sendMessage(迷你消息.deserialize(消息));
    }

    private static class 玩家飞行数据 {
        private 飞行状态 状态 = 飞行状态.地面;
        private double 活力;
        private long 冲刺冷却到期 = 0;
        private long 爆发冷却到期 = 0;
        private long 俯冲冷却到期 = 0;
        private long 起飞开始Tick = 0;
        private long 摔落保护到期 = 0;
        private long 准驭空到期 = 0;
        private BossBar bossBar = null;

        玩家飞行数据(double 初始活力) {
            this.活力 = 初始活力;
        }
    }
}
