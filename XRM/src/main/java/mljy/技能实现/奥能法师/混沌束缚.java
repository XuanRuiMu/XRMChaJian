package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.实体;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

@技能定义(
        技能标识 = "1_5",
        名称翻译键 = "skill.奥能法师.混沌束缚.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 15.0,
        公共冷却 = 1.5,
        射程 = 8.0,
        宽度 = 1.0
)
public class 混沌束缚 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 主伤害系数 = 0.4;
    private static final double 副伤害系数 = 0.15;
    private static final double 弹道总长度 = 8.0;
    private static final double 弹道宽度 = 1.0;
    private static final double 检测步长 = 0.6;
    private static final double 二次半径 = 3.0;
    private static final int 缓慢等级 = 4;
    private static final int 缓慢持续刻 = 100;
    private static final double 眼睛高度偏移 = 1.5;
    private static final Color 混沌主颜色 = Color.fromRGB(200, 50, 80);
    private static final Color 混沌副颜色 = Color.fromRGB(60, 180, 70);
    private static final Color 混沌核心颜色 = Color.fromRGB(255, 120, 60);
    private static final String 混沌束缚AOE范围命中日志键 = "技能日志.混沌束缚AOE范围命中";

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识源 = 获取调试根事件标识();
        final UUID 根事件标识 = 根事件标识源 != null ? 根事件标识源 : UUID.randomUUID();
        UUID 施法标识 = 创建施法标识();
        UUID 弹道标识 = UUID.randomUUID();
        玩家快照 施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, null,
                "释放请求", "进入", "cooldown_gate=delegated resource_gate=none projectile_uuid=%s", 弹道标识);
        if (施法者 == null) {
            记录技能阶段("混沌束缚", null, "1_5", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_caster projectile_uuid=%s", 弹道标识);
            return 技能执行结果.上下文为空;
        }
        位置 起点 = 获取实时位置(施法者);
        if (起点 == null) {
            记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=position_unavailable projectile_uuid=%s", 弹道标识);
            return 技能执行结果.位置无效;
        }
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=world_unavailable projectile_uuid=%s", 弹道标识);
            return 技能执行结果.位置无效;
        }
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, null,
                "前置状态", "通过", "cooldown_gate=delegated projectile_uuid=%s", 弹道标识);
        final double 当前弹道总长度 = 获取参数("弹道总长度", 弹道总长度);
        final double 当前弹道宽度 = 获取参数("弹道宽度", 弹道宽度);
        final double 当前检测步长 = 获取参数("检测步长", 检测步长);
        Vector 方向 = new Vector(
                -Math.sin(Math.toRadians(起点.偏航角())) * Math.cos(Math.toRadians(起点.俯仰角())),
                -Math.sin(Math.toRadians(起点.俯仰角())),
                Math.cos(Math.toRadians(起点.偏航角())) * Math.cos(Math.toRadians(起点.俯仰角()))
        ).normalize();
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 眼睛高度偏移, 0);

        播放释放特效(世界, 施法位置);
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, null,
                "弹道阶段", "开始", "projectile_uuid=%s length=%.1f width=%.1f speed=%.1f_blocks_per_second",
                弹道标识, 当前弹道总长度, 当前弹道宽度, 当前检测步长 * 20.0);

        UUID 事件标识 = 根事件标识;
        boolean 创建事件 = 根事件标识源 == null;
        事件日志上下文.异步事件租约 异步租约 = 事件日志上下文.获取异步事件租约(事件标识);
        if (异步租约 == null) {
            记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                    "任务启动", "失败", "reason=event_lease_unavailable projectile_uuid=%s", 弹道标识);
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
            return 技能执行结果.执行异常;
        }
        try {
            BukkitTask 弹道任务 = new BukkitRunnable() {
            private double 已飞行距离 = 0;
            private boolean 已命中 = false;
            private boolean 已结束 = false;

            @Override
            public void run() {
                try {
                    事件日志上下文.在事件中执行(事件标识, () -> {
                        if (已命中) {
                            完成();
                            return;
                        }
                        double 本次距离 = Math.min(当前检测步长, 当前弹道总长度 - 已飞行距离);
                        if (本次距离 <= 0) {
                            完成();
                            return;
                        }
                        已飞行距离 += 本次距离;
                        Location 当前位置 = 施法位置.clone().add(方向.clone().multiply(已飞行距离));
                        播放弹道粒子(世界, 当前位置, 方向, 已飞行距离);
                        for (Entity 实体 : 世界.getNearbyEntities(当前位置, 当前弹道宽度, 当前弹道宽度, 当前弹道宽度)) {
                            if (!(实体 instanceof LivingEntity 生物) || 生物.isDead()) {
                                continue;
                            }
                            if (实体 instanceof Player 玩家 && 玩家.getUniqueId().equals(施法者.唯一标识())) {
                                continue;
                            }
                            命中(施法者, 生物, 当前位置, 世界, 事件标识, 施法标识, 弹道标识);
                            已命中 = true;
                            完成();
                            return;
                        }
                        if (已飞行距离 >= 当前弹道总长度) {
                            记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                                    "伤害结算", "未命中", "projectile_uuid=%s target_uuid=null", 弹道标识);
                            完成();
                        }
                    });
                } catch (RuntimeException | Error 异常) {
                    完成();
                    throw 异常;
                }
            }

            private void 完成() {
                if (已结束) {
                    return;
                }
                已结束 = true;
                cancel();
                异步租约.释放();
                记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                        "任务停止", "成功", "projectile_uuid=%s reason=cast_finished", 弹道标识);
            }
        }.runTaskTimer(插件, 0, 1);
            记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                    "任务启动", "成功", "projectile_uuid=%s task_id=%s", 弹道标识,
                    弹道任务 == null ? "null" : 弹道任务.getTaskId());
            return 技能执行结果.成功;
        } catch (RuntimeException | Error 异常) {
            记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                    "取消原因", "异常", "projectile_uuid=%s exception=%s", 弹道标识, 异常.getClass().getSimpleName());
            异步租约.释放();
            throw 异常;
        } finally {
            记录技能阶段("混沌束缚", 施法者, "1_5", 事件标识, 施法标识, null,
                    "最终状态", "结束", "projectile_uuid=%s", 弹道标识);
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_5", "主伤害系数", "0.4", "主伤害属性系数");
        注册参数("1_5", "副伤害系数", "0.15", "二次伤害智力系数");
        注册参数("1_5", "弹道总长度", "8.0", "弹道最大飞行距离");
        注册参数("1_5", "弹道宽度", "1.0", "弹道碰撞检测宽度");
        注册参数("1_5", "检测步长", "0.6", "弹道每tick飞行距离");
        注册参数("1_5", "二次半径", "3.0", "二次伤害范围半径");
        注册参数("1_5", "缓慢等级", "4", "缓慢效果等级");
        注册参数("1_5", "缓慢持续刻", "100", "缓慢效果持续tick数");
        注册参数("1_5", "暴击倍率", "1.5", "暴击伤害倍率");
    }

    private void 播放释放特效(World 世界, Location 施法位置) {
        世界.spawnParticle(Particle.DUST, 施法位置, 30,
                0.6, 0.6, 0.6, 0.15,
                new Particle.DustOptions(混沌主颜色, 1.8f));
        世界.spawnParticle(Particle.DUST, 施法位置, 15,
                0.3, 0.3, 0.3, 0.25,
                new Particle.DustOptions(混沌核心颜色, 1.0f));
        世界.spawnParticle(Particle.WITCH, 施法位置, 20, 0.6, 0.6, 0.6, 0.1);
        世界.playSound(施法位置, Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.6f);
        世界.playSound(施法位置, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.2f);
    }

    private void 播放弹道粒子(World 世界, Location 当前位置, Vector 方向, double 已飞行距离) {
        Location 弹道中心 = 当前位置.clone();
        世界.spawnParticle(Particle.DUST, 弹道中心, 8,
                0.35, 0.35, 0.35, 0.0,
                new Particle.DustOptions(混沌主颜色, 1.6f));
        世界.spawnParticle(Particle.DUST, 弹道中心, 4,
                0.2, 0.2, 0.2, 0.0,
                new Particle.DustOptions(混沌核心颜色, 1.0f));
        世界.spawnParticle(Particle.WITCH, 弹道中心, 6, 0.25, 0.25, 0.25, 0.02);

        double 混沌角 = 已飞行距离 * Math.PI * 3;
        Vector 垂直 = new Vector(0, 1, 0);
        if (Math.abs(方向.dot(垂直)) > 0.99) {
            垂直 = new Vector(1, 0, 0);
        }
        Vector 右 = 方向.clone().crossProduct(垂直).normalize().multiply(1.2);
        Vector 上 = 方向.clone().crossProduct(右).normalize();
        for (int i = 0; i < 3; i++) {
            double 角 = 混沌角 + i * Math.PI * 2 / 3 + Math.sin(已飞行距离 * 2.0) * 0.5;
            double 半径偏移 = 0.7 + Math.sin(已飞行距离 * 4.0 + i) * 0.3;
            double xOff = 右.getX() * Math.cos(角) * 半径偏移 + 上.getX() * Math.sin(角) * 半径偏移;
            double yOff = 右.getY() * Math.cos(角) * 半径偏移 + 上.getY() * Math.sin(角) * 半径偏移;
            double zOff = 右.getZ() * Math.cos(角) * 半径偏移 + 上.getZ() * Math.sin(角) * 半径偏移;
            Location 混沌点 = 弹道中心.clone().add(xOff, yOff, zOff);
            世界.spawnParticle(Particle.DUST, 混沌点, 1,
                    0.05, 0.05, 0.05, 0.0,
                    new Particle.DustOptions(混沌副颜色, 0.8f));
        }
    }

    private void 命中(玩家快照 施法者, LivingEntity 目标, Location 命中位置, World 世界,
                    UUID 根事件标识, UUID 施法标识, UUID 弹道标识) {
        double 当前主伤害系数 = 获取参数("主伤害系数", 主伤害系数);
        int 当前缓慢等级 = 获取参数("缓慢等级", 缓慢等级);
        int 当前缓慢持续刻 = 获取参数("缓慢持续刻", 缓慢持续刻);
        double 主伤害 = (施法者.属性().力量() + 施法者.属性().敏捷() + 施法者.属性().智力()) * 当前主伤害系数;
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, 目标.getUniqueId(),
                "伤害结算", "开始", "projectile_uuid=%s base_damage=%.2f", 弹道标识, 主伤害);
        事件日志上下文.在事件中执行(根事件标识, () -> 应用单体伤害并记录命中(施法者, new mljy.基础设施层.Bukkit适配.实体适配器(目标), 主伤害, false, "skill.奥能法师.混沌束缚.name", 2, 根事件标识));
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, 目标.getUniqueId(),
                "伤害结算", "完成", "projectile_uuid=%s base_damage=%.2f", 弹道标识, 主伤害);

        世界.spawnParticle(Particle.DUST, 命中位置, 30,
                0.6, 0.6, 0.6, 0.15,
                new Particle.DustOptions(混沌主颜色, 2.0f));
        世界.spawnParticle(Particle.DUST, 命中位置, 15,
                0.3, 0.3, 0.3, 0.3,
                new Particle.DustOptions(混沌核心颜色, 1.2f));
        世界.spawnParticle(Particle.WITCH, 命中位置, 20, 0.6, 0.6, 0.6, 0.1);
        世界.spawnParticle(Particle.EXPLOSION, 命中位置, 2, 0.3, 0.3, 0.3, 0.0);
        世界.spawnParticle(Particle.SCULK_SOUL, 命中位置, 8, 0.4, 0.4, 0.4, 0.02);
        世界.playSound(命中位置, Sound.ENTITY_WITCH_HURT, 1.0f, 1.1f);
        世界.playSound(命中位置, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.6f, 0.8f);

        try {
            目标.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 当前缓慢持续刻, 当前缓慢等级));
        } catch (ExceptionInInitializerError | IllegalArgumentException 异常) {
            调试日志器.调试("混沌束缚", "应用缓慢效果失败: %s", 异常.toString());
        }
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, 目标.getUniqueId(),
                "效果变化", "完成", "effect=缓慢 amplifier=%d duration_ticks=%d", 当前缓慢等级, 当前缓慢持续刻);
        事件日志上下文.在事件中执行(根事件标识, () -> 触发二次伤害(施法者, 目标, 根事件标识, 施法标识, 弹道标识));
    }

    private void 触发二次伤害(玩家快照 施法者, LivingEntity 主目标,
                            UUID 根事件标识, UUID 施法标识, UUID 弹道标识) {
        位置 中心 = 位置适配器.转换(主目标.getLocation());
        double 当前副伤害系数 = 获取参数("副伤害系数", 副伤害系数);
        double 当前二次半径 = 获取参数("二次半径", 二次半径);
        double 副伤害 = 施法者.属性().智力() * 当前副伤害系数;
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, 主目标.getUniqueId(),
                "范围伤害", "开始", "projectile_uuid=%s radius=%.1f base_damage=%.2f", 弹道标识, 当前二次半径, 副伤害);
        应用范围伤害并记录日志(施法者, 中心, 当前二次半径, 副伤害, Aoe衰减阈值, "skill.奥能法师.混沌束缚.name", 0, false, 根事件标识, 混沌束缚AOE范围命中日志键);
        记录技能阶段("混沌束缚", 施法者, "1_5", 根事件标识, 施法标识, 主目标.getUniqueId(),
                "范围伤害", "完成", "projectile_uuid=%s radius=%.1f", 弹道标识, 当前二次半径);
    }
}
