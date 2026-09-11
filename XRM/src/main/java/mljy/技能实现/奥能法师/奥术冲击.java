package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

@技能定义(
        技能标识 = "1_1",
        名称翻译键 = "skill.奥能法师.奥术冲击.name",
        施法类型 = 施法类型.蓄力,
        蓄力时间 = 0.5,
        冷却时间 = 3.0,
        公共冷却 = 1.5,
        射程 = 10.0,
        宽度 = 1.0
)
public class 奥术冲击 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 智力系数 = 0.15;
    private static final double 弹道总长度 = 10.0;
    private static final double 弹道宽度 = 1.0;
    private static final double 检测步长 = 1.0;
    private static final double 眼睛高度偏移 = 1.5;
    private static final Color 奥术主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 奥术修饰颜色 = Color.fromRGB(220, 160, 255);
    private static final Color 奥术核心颜色 = Color.fromRGB(255, 220, 255);

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, null,
                "释放请求", "进入", "cooldown_gate=delegated resource_gate=none");
        if (施法者 == null) {
            记录技能阶段("奥术冲击", null, "1_1", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_caster");
            return 技能执行结果.上下文为空;
        }
        位置 起点 = 获取实时位置(施法者);
        if (起点 == null) {
            记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=position_unavailable");
            return 技能执行结果.位置无效;
        }
        World 世界 = org.bukkit.Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            调试日志器.调试("奥术冲击", "世界不存在，技能终止：世界=%s", 起点.世界());
            记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=world_unavailable world=%s", 起点.世界());
            return 技能执行结果.位置无效;
        }
        记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, null,
                "蓄力结束", "通过", "position=resolved");
        final double 当前眼睛高度偏移 = 获取参数("眼睛高度偏移", 眼睛高度偏移);
        final double 当前弹道总长度 = 获取参数("弹道总长度", 弹道总长度);
        final double 当前弹道宽度 = 获取参数("弹道宽度", 弹道宽度);
        final double 当前检测步长 = 获取参数("检测步长", 检测步长);
        Vector 方向 = new Vector(
                -Math.sin(Math.toRadians(起点.偏航角())) * Math.cos(Math.toRadians(起点.俯仰角())),
                -Math.sin(Math.toRadians(起点.俯仰角())),
                Math.cos(Math.toRadians(起点.偏航角())) * Math.cos(Math.toRadians(起点.俯仰角()))
        ).normalize();
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 当前眼睛高度偏移, 0);
        调试日志器.调试("奥术冲击", "开始释放：施法者=%s 起点=(%.1f,%.1f,%.1f) 方向=(%.2f,%.2f,%.2f)",
                施法者.唯一标识(), 起点.x(), 起点.y(), 起点.z(), 方向.getX(), 方向.getY(), 方向.getZ());

        播放释放特效(世界, 施法位置, 方向);

        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        UUID 弹道标识 = UUID.randomUUID();
        记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                "弹道阶段", "开始", "projectile_uuid=%s length=%.1f width=%.1f speed=%.1f_blocks_per_second",
                弹道标识, 当前弹道总长度, 当前弹道宽度, 当前检测步长 * 20.0);
        调试日志器.调试("奥术冲击", "建立因果链：根事件=%s 施法=%s 弹道=%s",
                事件标识, 施法标识, 弹道标识);
        事件日志上下文.异步事件租约 异步租约 = 事件日志上下文.获取异步事件租约(事件标识);
        if (异步租约 == null) {
            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                    "任务启动", "失败", "reason=event_lease_unavailable projectile_uuid=%s", 弹道标识);
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
            return 技能执行结果.执行异常;
        }
        记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                "租约获取", "成功", "projectile_uuid=%s", 弹道标识);
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
                            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                                    "弹道阶段", "结束", "projectile_uuid=%s reason=range_exhausted", 弹道标识);
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
                            调试日志器.调试("奥术冲击", "弹道飞完全程未命中：施法者=%s", 施法者.唯一标识());
                            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
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
                记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                        "任务停止", "成功", "projectile_uuid=%s reason=cast_finished", 弹道标识);
            }
        }.runTaskTimer(插件, 0, 1);
            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                    "任务启动", "成功", "projectile_uuid=%s task_id=%s", 弹道标识,
                    弹道任务 == null ? "null" : 弹道任务.getTaskId());
            return 技能执行结果.成功;
        } catch (RuntimeException | Error 异常) {
            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                    "取消原因", "异常", "projectile_uuid=%s exception=%s", 弹道标识, 异常.getClass().getSimpleName());
            异步租约.释放();
            throw 异常;
        } finally {
            记录技能阶段("奥术冲击", 施法者, "1_1", 事件标识, 施法标识, null,
                    "最终状态", "结束", "projectile_uuid=%s", 弹道标识);
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_1", "智力系数", "0.15", "智力伤害系数");
        注册参数("1_1", "弹道总长度", "10.0", "弹道最大飞行距离");
        注册参数("1_1", "弹道宽度", "1.0", "弹道碰撞检测宽度");
        注册参数("1_1", "检测步长", "1.0", "弹道每tick飞行距离");
        注册参数("1_1", "眼睛高度偏移", "1.5", "粒子起点眼睛高度偏移");
    }

    private void 播放释放特效(World 世界, Location 施法位置, Vector 方向) {
        世界.spawnParticle(Particle.DUST, 施法位置, 35,
                0.6, 0.6, 0.6, 0.15,
                new Particle.DustOptions(奥术主颜色, 2.0f));
        世界.spawnParticle(Particle.DUST, 施法位置, 20,
                0.3, 0.3, 0.3, 0.3,
                new Particle.DustOptions(奥术核心颜色, 1.2f));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 20, 0.5, 0.5, 0.5, 0.08);
        世界.spawnParticle(Particle.SONIC_BOOM, 施法位置, 1, 0.1, 0.1, 0.1, 0.0);
        世界.playSound(施法位置, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 0.6f);
        世界.playSound(施法位置, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
    }

    private void 播放弹道粒子(World 世界, Location 当前位置, Vector 方向, double 已飞行距离) {
        Location 弹道中心 = 当前位置.clone();
        世界.spawnParticle(Particle.DUST, 弹道中心, 8,
                0.35, 0.35, 0.35, 0.0,
                new Particle.DustOptions(奥术主颜色, 1.6f));
        世界.spawnParticle(Particle.DUST, 弹道中心, 4,
                0.2, 0.2, 0.2, 0.0,
                new Particle.DustOptions(奥术核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 弹道中心, 4, 0.15, 0.15, 0.15, 0.02);

        double 螺旋角度 = 已飞行距离 * Math.PI * 2;
        Vector 垂直 = new Vector(0, 1, 0);
        if (Math.abs(方向.dot(垂直)) > 0.99) {
            垂直 = new Vector(1, 0, 0);
        }
        Vector 右 = 方向.clone().crossProduct(垂直).normalize().multiply(1.5);
        Vector 上 = 方向.clone().crossProduct(右).normalize();
        double 螺旋半径 = 0.8;
        for (int i = 0; i < 3; i++) {
            double 角 = 螺旋角度 + i * Math.PI * 2 / 3;
            double 偏移x = 右.getX() * Math.cos(角) * 螺旋半径 + 上.getX() * Math.sin(角) * 螺旋半径;
            double 偏移y = 右.getY() * Math.cos(角) * 螺旋半径 + 上.getY() * Math.sin(角) * 螺旋半径;
            double 偏移z = 右.getZ() * Math.cos(角) * 螺旋半径 + 上.getZ() * Math.sin(角) * 螺旋半径;
            Location 螺旋点 = 弹道中心.clone().add(偏移x, 偏移y, 偏移z);
            世界.spawnParticle(Particle.DUST, 螺旋点, 1,
                    0.05, 0.05, 0.05, 0.0,
                    new Particle.DustOptions(奥术修饰颜色, 0.7f));
        }
    }

    private void 命中(玩家快照 施法者, LivingEntity 目标, Location 命中位置, World 世界,
                    UUID 根事件标识, UUID 施法标识, UUID 弹道标识) {
        double 当前智力系数 = 获取参数("智力系数", 智力系数);
        double 基础伤害 = 施法者.属性().智力() * 当前智力系数;
        记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, 目标.getUniqueId(),
                "伤害结算", "开始", "projectile_uuid=%s base_damage=%.2f", 弹道标识, 基础伤害);
        调试日志器.调试("奥术冲击", "弹道命中：施法者=%s 目标=%s 基础伤害=%.2f 根事件=%s 施法=%s 弹道=%s",
                施法者.唯一标识(), 目标.getUniqueId(), 基础伤害, 根事件标识, 施法标识, 弹道标识);
        事件日志上下文.在显式子事件中执行(根事件标识, 施法标识, 弹道标识,
                () -> 应用单体伤害并记录命中(施法者,
                        new mljy.基础设施层.Bukkit适配.实体适配器(目标), 基础伤害,
                        false,
                         "skill.奥能法师.奥术冲击.name", 1, false));
        记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, 目标.getUniqueId(),
                "伤害结算", "完成", "projectile_uuid=%s base_damage=%.2f", 弹道标识, 基础伤害);
        记录技能阶段("奥术冲击", 施法者, "1_1", 根事件标识, 施法标识, 目标.getUniqueId(),
                "释放后状态", "命中", "projectile_uuid=%s resource_change=hit_grants_1", 弹道标识);

        世界.spawnParticle(Particle.DUST, 命中位置, 35,
                0.6, 0.6, 0.6, 0.15,
                new Particle.DustOptions(奥术主颜色, 2.2f));
        世界.spawnParticle(Particle.DUST, 命中位置, 15,
                0.3, 0.3, 0.3, 0.25,
                new Particle.DustOptions(奥术核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 命中位置, 15, 0.5, 0.5, 0.5, 0.1);
        世界.spawnParticle(Particle.EXPLOSION, 命中位置, 2, 0.3, 0.3, 0.3, 0.0);
        世界.playSound(命中位置, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        世界.playSound(命中位置, Sound.ENTITY_ELDER_GUARDIAN_HURT, 0.4f, 0.6f);
    }
}
