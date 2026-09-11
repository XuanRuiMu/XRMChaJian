package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.调试日志器;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

@技能定义(
        技能标识 = "1_6",
        名称翻译键 = "skill.奥能法师.秘法回流.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 20.0,
        公共冷却 = 1.5
)
public class 秘法回流 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 治疗系数 = 0.6;
    private static final double 移动判定延迟 = 1.0;
    private static final double 移动判定阈值 = 0.1;
    private static final double 治疗高度偏移 = 1.0;
    private static final Color 治疗主颜色 = Color.fromRGB(80, 255, 120);
    private static final Color 治疗副颜色 = Color.fromRGB(255, 220, 60);
    private static final Color 治疗核心颜色 = Color.fromRGB(180, 255, 200);
    private static final Color 回流主颜色 = Color.fromRGB(100, 140, 255);
    private static final Color 回流副颜色 = Color.fromRGB(180, 100, 255);
    private static final Color 回流核心颜色 = Color.fromRGB(220, 200, 255);
    private static final double 回流环半径 = 0.8;

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识源 = 获取调试根事件标识();
        final UUID 根事件标识 = 根事件标识源 != null ? 根事件标识源 : UUID.randomUUID();
        UUID 施法标识 = 创建施法标识();
        玩家快照 施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "释放请求", "进入", "cooldown_gate=delegated resource_gate=none");
        if (施法者 == null) {
            记录技能阶段("秘法回流", null, "1_6", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_caster");
            return 技能执行结果.上下文为空;
        }
        double 秘能前 = 获取秘能层数(施法者);
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "前置状态", "通过", "resource=秘能 before=%.1f movement_gate=after_1s", 秘能前);
        调试日志器.调试("秘法回流", "释放：施法者=%s 秘能变化前=%.1f", 施法者.名称(), 秘能前);
        技能释放服务实例.抑制通用释放日志(施法者.唯一标识(), 上下文.技能标识());
        治疗(施法者, 根事件标识, 施法标识, "秘法回流.治疗");
        增加秘能(施法者, 1);
        double 首段秘能后 = 获取秘能层数(施法者);
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "资源变化", "完成", "resource=秘能 before=%.1f after=%.1f delta=%.1f", 秘能前, 首段秘能后, 首段秘能后 - 秘能前);

        Player 在线玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (在线玩家 == null) {
            记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                    "最终状态", "失败", "reason=player_offline after_first_stage=true");
            return 技能执行结果.执行异常;
        }
        org.bukkit.Location 首次位置 = 在线玩家.getLocation();
        final double 当前移动判定延迟 = 获取参数("移动判定延迟", 移动判定延迟);

        // 秘能回流环绕粒子（蓝色/紫色流动环，持续1秒）
        BukkitTask 粒子任务 = new BukkitRunnable() {
            int particleTick = 0;

            @Override
            public void run() {
                Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
                if (玩家 == null || !玩家.isOnline()) {
                    cancel();
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "任务停止", "成功", "task=resource_flow_particles reason=player_unavailable");
                    return;
                }
                Location 位置 = 玩家.getLocation().clone().add(0, 1.0, 0);
                World 世界 = 位置.getWorld();
                if (世界 == null) {
                    cancel();
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "任务停止", "成功", "task=resource_flow_particles reason=world_unavailable");
                    return;
                }
                播放秘能回流粒子(世界, 位置, particleTick);
                particleTick++;
            }
        }.runTaskTimer(插件, 0, 3);
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=resource_flow_particles task_id=%s", 粒子任务 == null ? "null" : 粒子任务.getTaskId());

        BukkitTask 二段任务 = new BukkitRunnable() {
            @Override
            public void run() {
                粒子任务.cancel();
                记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                        "任务停止", "成功", "task=resource_flow_particles reason=movement_check");
                Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
                if (玩家 == null || !玩家.isOnline()) {
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "取消原因", "失败", "reason=player_offline before_second_stage=true");
                    return;
                }
                Location 当前位置 = 玩家.getLocation();
                boolean 跨世界 = 当前位置.getWorld() == null
                        || 首次位置.getWorld() == null
                        || !当前位置.getWorld().equals(首次位置.getWorld());
                double 移动距离 = 跨世界 ? Double.MAX_VALUE : 当前位置.distance(首次位置);
                double 当前移动判定阈值 = 获取参数("移动判定阈值", 移动判定阈值);
                boolean 触发二段 = 移动距离 > 当前移动判定阈值;
                记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                        "移动判定", "完成", "distance=%.3f threshold=%.3f crossed_world=%b triggered=%b",
                        移动距离, 当前移动判定阈值, 跨世界, 触发二段);
                调试日志器.调试("秘法回流", "二段移动判定：施法者=%s 当前位置=%s 首次位置=%s 移动距离=%.3f 阈值=%.3f 触发=%b",
                        施法者.名称(), 当前位置, 首次位置, 移动距离, 当前移动判定阈值, 触发二段);
                if (触发二段) {
                    double 二段前 = 获取秘能层数(施法者);
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "二段释放", "开始", "resource=秘能 before=%.1f distance=%.3f", 二段前, 移动距离);
                    治疗(施法者, 根事件标识, 施法标识, "秘法回流.移动额外治疗");
                    增加秘能(施法者, 1);
                    double 二段后 = 获取秘能层数(施法者);
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "二段释放", "完成", "resource=秘能 before=%.1f after=%.1f delta=%.1f", 二段前, 二段后, 二段后 - 二段前);
                } else {
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "取消原因", "跳过", "reason=player_did_not_move distance=%.3f threshold=%.3f",
                            移动距离, 当前移动判定阈值);
                }
                记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                        "最终状态", "结束", "second_stage=true");
            }
        }.runTaskLater(插件, (long) (当前移动判定延迟 * 20));
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=movement_check task_id=%s delay_seconds=%.1f",
                二段任务 == null ? "null" : 二段任务.getTaskId(), 当前移动判定延迟);
        return 技能执行结果.成功;
    }

    @Override
    public void 声明参数() {
        注册参数("1_6", "治疗系数", "0.6", "治疗量智力系数");
        注册参数("1_6", "移动判定延迟", "1.0", "二段移动判定延迟秒数");
        注册参数("1_6", "移动判定阈值", "0.1", "触发二段所需移动距离");
        注册参数("1_6", "暴击倍率", "1.5", "暴击伤害倍率");
    }

    private void 治疗(玩家快照 施法者, UUID 根事件标识, UUID 施法标识, String 日志键) {
        Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (玩家 == null || !玩家.isOnline()) {
            记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                    "伤害结算", "失败", "reason=player_offline action=heal");
            return;
        }
        double 生命前 = 玩家.getHealth();
        播放治疗特效(位置适配器.转换(玩家.getLocation()), 施法者, 根事件标识, 施法标识);
        double 治疗量 = 施法者.属性().智力() * 获取参数("治疗系数", 治疗系数);
        AttributeInstance 最大生命属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        double 最大生命值 = 最大生命属性 != null ? 最大生命属性.getValue() : 20.0;
        玩家.setHealth(Math.min(玩家.getHealth() + 治疗量, 最大生命值));
        double 实际回复 = 玩家.getHealth() - 生命前;
        if (实际回复 < 0) {
            实际回复 = 0;
        }
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "治疗结算", "完成", "health_before=%.1f requested=%.1f health_after=%.1f actual=%.1f", 生命前, 治疗量, 玩家.getHealth(), 实际回复);
        消息服务.发送技能日志(施法者, 根事件标识, 日志键, String.format(数值格式, 实际回复), 1);
    }

    private void 播放治疗特效(位置 起点, 玩家快照 施法者, UUID 根事件标识, UUID 施法标识) {
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                    "任务启动", "失败", "task=heal_particles reason=world_unavailable");
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 治疗高度偏移, 0);

        世界.spawnParticle(Particle.DUST, 施法位置, 35,
                0.8, 1.5, 0.8, 0.1,
                new Particle.DustOptions(治疗主颜色, 1.8f));
        世界.spawnParticle(Particle.DUST, 施法位置, 20,
                0.4, 0.8, 0.4, 0.2,
                new Particle.DustOptions(治疗核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 12, 0.4, 0.8, 0.4, 0.05);
        世界.spawnParticle(Particle.HAPPY_VILLAGER, 施法位置, 15, 0.6, 0.8, 0.6, 0.15);
        世界.spawnParticle(Particle.SOUL, 施法位置, 10, 0.5, 1.0, 0.5, 0.05);

        BukkitTask 治疗动画任务 = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 6) {
                    cancel();
                    记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                            "任务停止", "成功", "task=heal_particles reason=animation_complete");
                    return;
                }
                for (int i = 0; i < 4; i++) {
                    double 角 = i * Math.PI / 2 + tick * 0.8;
                    double 半径 = 0.6;
                    double x = 施法位置.getX() + Math.cos(角) * 半径;
                    double z = 施法位置.getZ() + Math.sin(角) * 半径;
                    double y = 施法位置.getY() + tick * 0.4;
                    Location 螺旋点 = new Location(施法位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.DUST, 螺旋点, 1,
                            0.03, 0.03, 0.03, 0.0,
                            new Particle.DustOptions(治疗副颜色, 0.7f));
                }
                tick++;
            }
        }.runTaskTimer(插件, 0, 1);
        记录技能阶段("秘法回流", 施法者, "1_6", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=heal_particles task_id=%s", 治疗动画任务 == null ? "null" : 治疗动画任务.getTaskId());

        世界.playSound(施法位置, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
        世界.playSound(施法位置, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
    }

    private void 播放秘能回流粒子(World 世界, Location 位置, int tick) {
        for (int 环层 = 0; 环层 < 3; 环层++) {
            double y偏移 = (环层 - 1) * 0.6;
            double 相位 = 环层 * Math.PI * 2 / 3;
            int 每环粒子 = 8;
            double 环径 = 回流环半径 + 环层 * 0.2;
            for (int i = 0; i < 每环粒子; i++) {
                double 角 = i * Math.PI * 2 / 每环粒子 + tick * 0.2 + 相位;
                double x = 位置.getX() + Math.cos(角) * 环径;
                double z = 位置.getZ() + Math.sin(角) * 环径;
                Location 环点 = new Location(位置.getWorld(), x, 位置.getY() + y偏移, z);
                Color 颜色 = 环层 == 0 ? 回流核心颜色 : (环层 == 1 ? 回流主颜色 : 回流副颜色);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.02, 0.02, 0.02, 0.0,
                        new Particle.DustOptions(颜色, 0.7f));
            }
        }
        // 中心光晕
        if (tick % 5 == 0) {
            世界.spawnParticle(Particle.END_ROD, 位置, 3, 0.3, 0.3, 0.3, 0.02);
            世界.spawnParticle(Particle.SOUL, 位置, 5, 0.4, 0.6, 0.4, 0.03);
        }
    }
}
