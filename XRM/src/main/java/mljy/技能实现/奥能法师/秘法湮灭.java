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
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@技能定义(
        技能标识 = "1_8",
        名称翻译键 = "skill.奥能法师.秘法湮灭.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 90.0,
        公共冷却 = 1.5,
        射程 = 5.0
)
public class 秘法湮灭 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 第一段系数 = 0.08;
    private static final double 第二段系数 = 0.08;
    private static final double 第三段系数 = 0.16;
    private static final double 第四段系数 = 0.24;
    private static final double 半径 = 5.0;
    private static final double 爆发视觉半径 = 6.5;
    private static final double 第一段延迟 = 3.0;
    private static final double 段间隔 = 1.0;
    private static final double 第三段秘能产生概率 = 0.5;
    private static final double 爆发高度偏移 = 1.0;
    private static final double 天空裂缝高度 = 9.0;
    private static final double 天空裂缝半宽 = 3.5;
    private static final double 外柱半径 = 1.5;
    private static final long 充电刻总数 = (long) (第一段延迟 * 20);
    private static final int 续燃总刻 = 6;
    private static final double 第四段蓄力秒数 = 0.5;
    private static final long 第四段蓄力刻数 = (long) (第四段蓄力秒数 * 20);
    private static final Color 湮灭主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 湮灭修饰颜色 = Color.fromRGB(100, 30, 180);
    private static final Color 湮灭核心颜色 = Color.fromRGB(255, 220, 255);
    private static final Color 湮灭暗影颜色 = Color.fromRGB(40, 10, 80);
    private static final Color 湮灭闪光色 = Color.fromRGB(255, 255, 255);
    private static final float 湮灭龙息强度 = 1.0f;
    private static final String 第一段命中日志键 = "技能日志.秘法湮灭第一段命中";
    private static final String 第二段命中日志键 = "技能日志.秘法湮灭第二段命中";
    private static final String 第三段命中日志键 = "技能日志.秘法湮灭第三段命中";
    private static final String 第三段命中带秘能日志键 = "技能日志.秘法湮灭第三段命中带秘能";
    private static final String 第四段命中日志键 = "技能日志.秘法湮灭第四段命中";
    private final 奥能法师大招任务组 任务组 = new 奥能法师大招任务组();
    private final Set<UUID> 活跃玩家 = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Set<事件日志上下文.异步事件租约>> 异步租约表 = new ConcurrentHashMap<>();

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        final double 当前第一段延迟 = 获取参数("第一段延迟", 第一段延迟);
        final double 当前段间隔 = 获取参数("段间隔", 段间隔);
        final double 当前第一段系数 = 获取参数("第一段系数", 第一段系数);
        final double 当前第二段系数 = 获取参数("第二段系数", 第二段系数);
        final double 当前第三段系数 = 获取参数("第三段系数", 第三段系数);
        final double 当前第四段系数 = 获取参数("第四段系数", 第四段系数);
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("秘法湮灭", 请求施法者, "1_8", 根事件标识, 施法标识, null,
                "释放请求", "进入", "cooldown_gate=delegated resource_gate=秘能快照");
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("秘法湮灭", "释放跳过：技能上下文或施法者为空");
            记录技能阶段("秘法湮灭", null, "1_8", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        org.bukkit.entity.Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
            调试日志器.调试("秘法湮灭", "释放跳过：施法者不在线或已死亡 玩家=%s", 施法者.唯一标识());
            记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=player_unavailable");
            return 技能执行结果.执行异常;
        }
        调试日志器.调试("秘法湮灭", "蓄力阶段开始：施法者=%s", 施法者.名称());
        位置 中心 = 获取实时位置(施法者);
        if (中心 == null || 中心.世界() == null || 中心.世界().isBlank() || Bukkit.getWorld(中心.世界()) == null) {
            调试日志器.调试("秘法湮灭", "释放跳过：施法者世界无效 玩家=%s", 施法者.唯一标识());
            记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=world_unavailable");
            return 技能执行结果.位置无效;
        }
        记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                "前置状态", "通过", "cooldown_gate=delegated player_online=true world=%s", 中心.世界());
        UUID 玩家标识 = 施法者.唯一标识();
        奥能法师大招特效配置 配置 = 奥能法师大招特效配置.加载(插件);
        取消玩家任务(玩家标识, "重复施法");
        记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                "任务停止", "完成", "reason=replace_previous_cast");
        double 秘能层数 = 获取秘能层数(施法者);
        double 智力 = 施法者.属性().智力();
        记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                "资源变化", "快照", "resource=秘能 snapshot=%.1f", 秘能层数);
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                "蓄力开始", "成功", "duration_seconds=%.1f", 当前第一段延迟);

        try {
            播放充能开始特效(中心, 配置);
            启动充能特效(中心, 配置, 玩家标识, 事件标识, 施法标识, 施法者);
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=charging_animation");

            调度伤害(施法者, 中心, 配置, 当前第一段延迟, 智力 * 当前第一段系数, false, false, 0, 事件标识, 施法标识,
                    第一段命中日志键, null);
            调度伤害(施法者, 中心, 配置, 当前第一段延迟 + 当前段间隔, 智力 * 当前第二段系数 * 秘能层数, false, false, 1, 事件标识, 施法标识,
                    第二段命中日志键, null);
            调度伤害(施法者, 中心, 配置, 当前第一段延迟 + 2 * 当前段间隔, 智力 * 当前第三段系数 * 秘能层数, true, false, 2, 事件标识, 施法标识,
                    第三段命中日志键, 第三段命中带秘能日志键);
            调度伤害(施法者, 中心, 配置, 当前第一段延迟 + 3 * 当前段间隔, 智力 * 当前第四段系数 * 秘能层数, false, true, 3, 事件标识, 施法标识,
                    null, 第四段命中日志键);
            启动第四段蓄力(中心, 配置, 玩家标识, 事件标识, 施法标识, 施法者);
            return 技能执行结果.成功;
        } catch (RuntimeException | Error 异常) {
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "取消原因", "异常", "reason=task_start_failed exception=%s", 异常.getClass().getSimpleName());
            取消玩家任务(玩家标识, "任务启动失败");
            return 技能执行结果.执行异常;
        } finally {
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "最终状态", "结束", "cast_scheduled=true");
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_8", "第一段系数", "0.08", "第一段伤害智力系数");
        注册参数("1_8", "第二段系数", "0.08", "第二段伤害智力系数");
        注册参数("1_8", "第三段系数", "0.16", "第三段伤害智力系数");
        注册参数("1_8", "第四段系数", "0.24", "第四段伤害智力系数");
        注册参数("1_8", "半径", "5.0", "伤害范围半径");
        注册参数("1_8", "第一段延迟", "3.0", "第一段伤害延迟秒数");
        注册参数("1_8", "段间隔", "1.0", "段间间隔秒数");
        注册参数("1_8", "第三段秘能产生概率", "0.5", "第三段秘能产生概率");
        注册参数("1_8", "暴击倍率", "1.5", "暴击伤害倍率");
    }

    private void 播放充能开始特效(位置 中心, 奥能法师大招特效配置 配置) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            return;
        }
        Location 充能位置 = 位置适配器.转换(中心).clone().add(0, 爆发高度偏移, 0);

        世界.spawnParticle(Particle.DUST, 充能位置, 配置.粒子数量(60),
                1.8, 0.9, 1.8, 0.15,
                new Particle.DustOptions(湮灭主颜色, 配置.粒子尺寸(2.2f)));
        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 充能位置, 配置.粒子数量(40),
                1.4, 0.7, 1.4, 0.2,
                new Particle.DustTransition(湮灭主颜色, 湮灭核心颜色, 配置.粒子尺寸(2.0f)));
        世界.spawnParticle(Particle.END_ROD, 充能位置, 配置.粒子数量(30), 0.9, 0.5, 0.9, 0.05);

        // === 天空裂缝（加大：更宽更密） ===
        Location 裂缝位置 = 充能位置.clone().add(0, 天空裂缝高度, 0);
        for (int i = 0; i < 110; i++) {
            double 裂缝X = (Math.random() - 0.5) * 天空裂缝半宽 * 2;
            double 裂缝Z = (Math.random() - 0.5) * 天空裂缝半宽 * 2;
            Location 裂缝点 = 裂缝位置.clone().add(裂缝X, (Math.random() - 0.5) * 2.4, 裂缝Z);
            世界.spawnParticle(Particle.DUST, 裂缝点, 1,
                    0.06, 0.06, 0.06, 0.0,
                    new Particle.DustOptions(湮灭修饰颜色, 1.4f));
        }
        for (int i = 0; i < 70; i++) {
            double t = i / 69.0;
            double x = (t - 0.5) * (天空裂缝半宽 * 1.8) + (Math.random() - 0.5) * 0.8;
            double z = Math.sin(t * 6) * 1.4 + (Math.random() - 0.5) * 0.6;
            double y = Math.cos(t * 5) * 1.2 + (Math.random() - 0.5) * 0.4;
            Location 边缘点 = 裂缝位置.clone().add(x, y, z);
            世界.spawnParticle(Particle.END_ROD, 边缘点, 1, 0.03, 0.03, 0.03, 0.0);
        }

        // === 12 射线裂痕（扩大长度） ===
        for (int 射线 = 0; 射线 < 12; 射线++) {
            double 角度 = 射线 * Math.PI * 2 / 12 + (Math.random() - 0.5) * 0.3;
            for (int d = 1; d <= 6; d++) {
                double x = 充能位置.getX() + Math.cos(角度) * d * 1.1;
                double z = 充能位置.getZ() + Math.sin(角度) * d * 1.1;
                Location 裂痕点 = new Location(充能位置.getWorld(), x, 充能位置.getY() + 0.05, z);
                世界.spawnParticle(Particle.DUST, 裂痕点, 1,
                        0.04, 0.01, 0.04, 0.0,
                        new Particle.DustOptions(湮灭修饰颜色, 0.9f));
            }
        }

        世界.playSound(充能位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(1.2f), 0.3f);
    }

    private void 启动充能特效(位置 中心, 奥能法师大招特效配置 配置, UUID 玩家标识,
                           UUID 根事件标识, UUID 施法标识, 玩家快照 施法者) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                    "任务启动", "失败", "task=charging_animation reason=world_unavailable");
            return;
        }
        Location 充能位置 = 位置适配器.转换(中心).clone().add(0, 爆发高度偏移, 0);
        final double 当前伤害半径 = 获取参数("半径", 半径);

        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 动画任务 = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 配置.湮灭充能持续刻()) {
                    cancel();
                    移除任务(玩家标识, 任务句柄[0]);
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                            "蓄力结束", "成功", "task=charging_animation");
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                            "任务停止", "成功", "task=charging_animation reason=duration_complete");
                    return;
                }
                org.bukkit.entity.Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (!玩家状态有效(玩家, 中心.世界(), 位置适配器.转换(中心), 配置, true)) {
                    cancel();
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                            "取消原因", "失败", "task=charging_animation reason=player_state_invalid");
                    取消玩家任务(玩家标识, "充能动画玩家状态失效");
                    return;
                }
                double 进度 = (double) tick / 配置.湮灭充能持续刻();
                float 强度 = (float) (0.5 + 进度 * 2.0);

                // 内层16点旋转环
                for (int i = 0; i < 16; i++) {
                    double 角 = i * Math.PI * 2 / 16 + tick * 0.1;
                    double 当前半径 = 当前伤害半径 * (0.3 + 进度 * 0.7);
                    double x = 充能位置.getX() + Math.cos(角) * 当前半径;
                    double z = 充能位置.getZ() + Math.sin(角) * 当前半径;
                    Location 环点 = new Location(充能位置.getWorld(), x, 充能位置.getY(), z);
                    世界.spawnParticle(Particle.DUST, 环点, 1,
                            0.02, 0.02, 0.02, 0.0,
                            new Particle.DustOptions(湮灭修饰颜色, 强度 * 0.7f));
                }

                // 外层24点反向旋转环（增多层旋转环，渐变色）
                for (int i = 0; i < 24; i++) {
                    double 角 = i * Math.PI * 2 / 24 - tick * 0.08;
                    double 当前半径 = 当前伤害半径 * (0.6 + 进度 * 0.6);
                    double x = 充能位置.getX() + Math.cos(角) * 当前半径;
                    double z = 充能位置.getZ() + Math.sin(角) * 当前半径;
                    Location 环点 = new Location(充能位置.getWorld(), x, 充能位置.getY(), z);
                    世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 环点, 1,
                            0.02, 0.02, 0.02, 0.0,
                            new Particle.DustTransition(湮灭主颜色, 湮灭核心颜色, 强度 * 0.6f));
                }

                for (int i = 0; i < (int)(3 + 进度 * 8); i++) {
                    double 角 = Math.random() * Math.PI * 2;
                    double 距 = Math.random() * 当前伤害半径 * (0.3 + 进度 * 0.7);
                    double x = 充能位置.getX() + Math.cos(角) * 距;
                    double z = 充能位置.getZ() + Math.sin(角) * 距;
                    double y = 充能位置.getY() + Math.random() * 0.5;
                    Location 地面点 = new Location(充能位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.DUST, 地面点, 1,
                            0.03, 0.03, 0.03, 0.0,
                            new Particle.DustOptions(湮灭主颜色, 强度 * 0.5f));
                }

                // 持续的天空裂缝（加大，随进度扩张）
                if (tick % 2 == 0) {
                    Location 裂缝位置 = 充能位置.clone().add(0, 天空裂缝高度, 0);
                    for (int i = 0; i < 26; i++) {
                        double t = i / 25.0;
                        double x = (t - 0.5) * (天空裂缝半宽 * 1.8) + (Math.random() - 0.5) * 0.8;
                        double z = Math.sin(t * 6 + tick * 0.02) * 1.4 + (Math.random() - 0.5) * 0.5;
                        double y = Math.cos(t * 5) * (1.0 + 进度 * 1.5) + (Math.random() - 0.5) * 0.5;
                        Location 裂缝点 = 裂缝位置.clone().add(x, y, z);
                        世界.spawnParticle(Particle.DUST, 裂缝点, 1,
                                0.05, 0.05, 0.05, 0.0,
                                new Particle.DustOptions(湮灭修饰颜色, 强度 * 0.8f));
                    }
                    for (int i = 0; i < 10; i++) {
                        double t = Math.random();
                        double x = (t - 0.5) * (天空裂缝半宽 * 1.8) + (Math.random() - 0.5) * 0.3;
                        double z = Math.sin(t * 6 + tick * 0.02) * 1.4 + (Math.random() - 0.5) * 0.3;
                        double y = Math.cos(t * 5) * (1.0 + 进度 * 1.5);
                        Location 边缘点 = 裂缝位置.clone().add(x, y, z);
                        世界.spawnParticle(Particle.END_ROD, 边缘点, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }

                double 吸入半径 = 当前伤害半径 * (1.5 - 进度 * 1.0);
                for (int i = 0; i < (int)(5 + 进度 * 8); i++) {
                    double 角 = Math.random() * Math.PI * 2;
                    double 距 = Math.random() * 吸入半径;
                    double x = 充能位置.getX() + Math.cos(角) * 距;
                    double z = 充能位置.getZ() + Math.sin(角) * 距;
                    double y = 充能位置.getY() + Math.random() * 2.0;
                    Location 吸入点 = new Location(充能位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.DUST, 吸入点, 1,
                            0.05, 0.05, 0.05, 0.0,
                            new Particle.DustOptions(湮灭主颜色, 强度 * 0.4f));
                }

                if (tick % 3 == 0) {
                    int 射线数 = 12;
                    for (int 射线 = 0; 射线 < 射线数; 射线++) {
                        double 角度 = 射线 * Math.PI * 2 / 射线数 + tick * 0.03;
                        double 裂痕长度 = 当前伤害半径 * (0.3 + 进度 * 0.7);
                        int 点数 = (int)(3 + 进度 * 5);
                        for (int d = 1; d <= 点数; d++) {
                            double 距 = d * 裂痕长度 / 点数;
                            double x = 充能位置.getX() + Math.cos(角度) * 距;
                            double z = 充能位置.getZ() + Math.sin(角度) * 距;
                            Location 裂痕点 = new Location(充能位置.getWorld(), x, 充能位置.getY() + 0.03, z);
                            世界.spawnParticle(Particle.DUST, 裂痕点, 1,
                                    0.04, 0.01, 0.04, 0.0,
                                    new Particle.DustOptions(湮灭修饰颜色, 强度 * 0.6f));
                        }
                    }
                }

                // 递进音效（统一为信标充能音效，避免音效混乱）
                if (tick % 8 == 0) {
                    世界.playSound(充能位置, Sound.BLOCK_BEACON_POWER_SELECT,
                            配置.音量((float)(0.4 + 进度 * 0.6)), (float)(0.5 + 进度 * 0.6));
                }
                tick++;
            }
        };
        BukkitTask 任务 = 动画任务.runTaskTimer(插件, 0, 配置.湮灭充能间隔刻());
        任务句柄[0] = 任务;
        注册任务(玩家标识, 任务);
        记录技能阶段("秘法湮灭", 施法者, "1_8", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=charging_animation task_id=%s", 任务 == null ? "null" : 任务.getTaskId());
    }

    private void 调度伤害(玩家快照 施法者, 位置 中心, 奥能法师大招特效配置 配置,
                       double 延迟秒, double 基础伤害, boolean 可产生秘能, boolean 必定产生秘能, int 段序号,
                       UUID 事件标识, UUID 施法标识, String 无秘能日志键, String 带秘能日志键) {
        UUID 玩家标识 = 施法者.唯一标识();
        final double 当前伤害半径 = 获取参数("半径", 半径);
        final double 当前第三段秘能产生概率 = 获取参数("第三段秘能产生概率", 第三段秘能产生概率);
        事件日志上下文.异步事件租约 异步租约 = 事件日志上下文.获取异步事件租约(事件标识);
        if (异步租约 == null) {
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "任务启动", "失败", "task=damage_segment segment=%d reason=event_lease_unavailable", 段序号);
            return;
        }
        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 伤害任务 = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    事件日志上下文.在事件中执行(事件标识, () -> {
                        org.bukkit.entity.Player 玩家 = Bukkit.getPlayer(玩家标识);
                        if (!玩家状态有效(玩家, 中心.世界(), 位置适配器.转换(中心), 配置, false)) {
                            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                                    "取消原因", "失败", "task=damage_segment segment=%d reason=player_state_invalid", 段序号);
                            取消玩家任务(玩家标识, "伤害阶段玩家状态失效");
                            return;
                        }
                        调试日志器.调试("秘法湮灭", "伤害阶段触发：施法者=%s 秘能层数=%.1f 伤害=%.1f 段序号=%d", 施法者.名称(), 获取秘能层数(施法者), 基础伤害, 段序号);
                        double 秘能前 = 获取秘能层数(施法者);
                        记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                                "伤害结算", "开始", "segment=%d target_uuid=null radius=%.1f decay_threshold=%d resource_before=%.1f base_damage=%.1f",
                                段序号, 当前伤害半径, 秘法湮灭衰减阈值, 秘能前, 基础伤害);
                        try {
                            播放湮灭爆发特效(中心, 段序号, 配置, 玩家标识);
                        } catch (RuntimeException | Error 粒子异常) {
                            调试日志器.调试("秘法湮灭", "粒子特效异常：施法者=%s 段序号=%d 异常=%s",
                                    施法者.名称(), 段序号, 粒子异常.getClass().getSimpleName());
                            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                                    "粒子异常", "吞掉", "segment=%d exception=%s", 段序号, 粒子异常.getClass().getSimpleName());
                        }
                        int 秘能获取量;
                        String 秘能判定原因;
                        if (必定产生秘能) {
                            秘能获取量 = 1;
                            秘能判定原因 = "segment_definite_grant";
                        } else if (可产生秘能) {
                            boolean 概率触发结果 = 概率触发(当前第三段秘能产生概率);
                            秘能获取量 = 概率触发结果 ? 1 : 0;
                            秘能判定原因 = String.format("segment_probabilistic_grant probability=%.2f triggered=%b", 当前第三段秘能产生概率, 概率触发结果);
                        } else {
                            秘能获取量 = 0;
                            秘能判定原因 = "segment_no_grant";
                        }
                        调试日志器.调试("秘法湮灭", "段秘能判定：施法者=%s 段序号=%d 判定=%s 秘能获取量=%d",
                                施法者.名称(), 段序号, 秘能判定原因, 秘能获取量);
                        应用范围伤害并记录日志(施法者, 中心, 当前伤害半径, 基础伤害, 秘法湮灭衰减阈值, "skill.奥能法师.秘法湮灭.name", 秘能获取量,
                                false, 事件标识, 无秘能日志键, 带秘能日志键);
                        double 秘能后 = 获取秘能层数(施法者);
                        记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                                "伤害结算", "完成", "segment=%d target_uuid=null resource_before=%.1f resource_after=%.1f resource_delta=%.1f grant_reason=%s", 段序号, 秘能前, 秘能后, 秘能后 - 秘能前, 秘能判定原因);
                        移除任务(玩家标识, 任务句柄[0]);
                    });
                } catch (RuntimeException | Error 异常) {
                    调试日志器.调试("秘法湮灭", "伤害阶段兜底异常：施法者=%s 段序号=%d 异常=%s",
                            施法者.名称(), 段序号, 异常.getClass().getSimpleName());
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                            "伤害结算", "兜底异常", "segment=%d exception=%s", 段序号, 异常.getClass().getSimpleName());
                } finally {
                    释放异步租约(玩家标识, 异步租约);
                }
            }
        };
        try {
            BukkitTask 任务 = 伤害任务.runTaskLater(插件, (long) (延迟秒 * 20));
            任务句柄[0] = 任务;
            注册任务(玩家标识, 任务);
            注册异步租约(玩家标识, 异步租约);
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=damage_segment segment=%d task_id=%s delay_seconds=%.1f",
                    段序号, 任务 == null ? "null" : 任务.getTaskId(), 延迟秒);
        } catch (RuntimeException | Error 异常) {
            异步租约.释放();
            throw 异常;
        }
    }

    private void 播放湮灭爆发特效(位置 中心, int 段序号,
                             奥能法师大招特效配置 配置, UUID 玩家标识) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            return;
        }
        Location 爆发位置 = 位置适配器.转换(中心).clone().add(0, 爆发高度偏移, 0);

        switch (段序号) {
            case 0 -> 播放第一段爆发特效(世界, 爆发位置, 配置);
            case 1 -> 播放第二段爆发特效(世界, 爆发位置, 配置);
            case 2 -> 播放第三段爆发特效(世界, 爆发位置, 配置);
            case 3 -> 播放第四段爆发特效(世界, 爆发位置, 配置, 玩家标识, 中心);
            default -> 播放第一段爆发特效(世界, 爆发位置, 配置);
        }
    }

    // 第一段（最小）：紫色光球爆发——DUST 15个（紫色小球）+ END_ROD 5个（中心向上）
    private void 播放第一段爆发特效(World 世界, Location 爆发位置, 奥能法师大招特效配置 配置) {
        世界.spawnParticle(Particle.DUST, 爆发位置, 配置.粒子数量(15),
                0.4, 0.4, 0.4, 0.1,
                new Particle.DustOptions(湮灭主颜色, 配置.粒子尺寸(1.5f)));
        世界.spawnParticle(Particle.END_ROD, 爆发位置, 配置.粒子数量(5),
                0.1, 0.5, 0.1, 0.02);
        世界.playSound(爆发位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(0.4f), 0.6f);
    }

    // 第二段（小）：紫色波纹扩散——DUST 25个（半径3.0波纹圆环）+ END_ROD 10个（向上光柱）
    private void 播放第二段爆发特效(World 世界, Location 爆发位置, 奥能法师大招特效配置 配置) {
        int 环点数 = 配置.环点数量(25);
        double 环半径 = 3.0;
        for (int i = 0; i < 环点数; i++) {
            double 角 = i * Math.PI * 2 / 环点数;
            double x = 爆发位置.getX() + Math.cos(角) * 环半径;
            double z = 爆发位置.getZ() + Math.sin(角) * 环半径;
            Location 环点 = new Location(爆发位置.getWorld(), x, 爆发位置.getY(), z);
            世界.spawnParticle(Particle.DUST, 环点, 1,
                    0.05, 0.05, 0.05, 0.0,
                    new Particle.DustOptions(湮灭主颜色, 1.0f));
        }
        int 光柱粒子数 = 配置.粒子数量(10);
        for (int i = 0; i < 光柱粒子数; i++) {
            double yOff = i * 0.5;
            Location 柱点 = 爆发位置.clone().add(0, yOff, 0);
            世界.spawnParticle(Particle.END_ROD, 柱点, 1, 0.03, 0.03, 0.03, 0.0);
        }
        世界.playSound(爆发位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(0.7f), 0.6f);
    }

    // 第三段（中）：紫色星爆——DUST 40个（半径4.0星形）+ DUST_COLOR_TRANSITION 15个（外扩）+ END_ROD 15个（3光柱）+ SONIC_BOOM
    private void 播放第三段爆发特效(World 世界, Location 爆发位置, 奥能法师大招特效配置 配置) {
        int 星角数 = 6;
        int 每角点数 = Math.max(1, 配置.环点数量(40) / 星角数);
        double 星半径 = 4.0;
        for (int 角序 = 0; 角序 < 星角数; 角序++) {
            double 角度 = 角序 * Math.PI * 2 / 星角数;
            for (int i = 0; i < 每角点数; i++) {
                double t = (double) i / 每角点数;
                double r = 星半径 * (0.2 + t * 0.8);
                double x = 爆发位置.getX() + Math.cos(角度) * r;
                double z = 爆发位置.getZ() + Math.sin(角度) * r;
                Location 星点 = new Location(爆发位置.getWorld(), x, 爆发位置.getY(), z);
                世界.spawnParticle(Particle.DUST, 星点, 1,
                        0.05, 0.05, 0.05, 0.0,
                        new Particle.DustOptions(湮灭主颜色, 1.2f));
            }
        }
        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 爆发位置, 配置.粒子数量(15),
                1.0, 0.5, 1.0, 0.15,
                new Particle.DustTransition(湮灭主颜色, 湮灭核心颜色, 配置.粒子尺寸(1.5f)));
        int 柱数 = 3;
        int 每柱粒子数 = Math.max(1, 配置.粒子数量(15) / 柱数);
        double 柱高度 = 8.0;
        for (int p = 0; p < 柱数; p++) {
            double 柱角 = p * Math.PI * 2 / 柱数;
            double 柱心x = 爆发位置.getX() + Math.cos(柱角) * 外柱半径;
            double 柱心z = 爆发位置.getZ() + Math.sin(柱角) * 外柱半径;
            for (int i = 0; i < 每柱粒子数; i++) {
                double yOff = i * (柱高度 / 每柱粒子数);
                Location 柱点 = new Location(爆发位置.getWorld(), 柱心x, 爆发位置.getY() + yOff, 柱心z);
                世界.spawnParticle(Particle.END_ROD, 柱点, 1, 0.03, 0.03, 0.03, 0.0);
            }
        }
        世界.spawnParticle(Particle.SONIC_BOOM, 爆发位置, 1, 0.0, 0.0, 0.0, 0.0);
        世界.playSound(爆发位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(1.0f), 0.6f);
    }

    // 第四段（最大）：全屏湮灭——FLASH + EXPLOSION 5 + 7柱六芒星 + 多层环 + SCULK_SOUL + DRAGON_BREATH + 续燃
    private void 播放第四段爆发特效(World 世界, Location 爆发位置, 奥能法师大招特效配置 配置,
                                    UUID 玩家标识, 位置 中心) {
        世界.spawnParticle(Particle.FLASH, 爆发位置, 1,
                0.0, 0.0, 0.0, 0.0, 湮灭闪光色);
        世界.spawnParticle(Particle.EXPLOSION, 爆发位置, 5,
                0.5, 0.3, 0.5, 0.0);
        int 柱数 = 7;
        double 柱高度 = 16.0;
        for (int p = 0; p < 柱数; p++) {
            double 柱心x = 爆发位置.getX();
            double 柱心z = 爆发位置.getZ();
            if (p > 0) {
                double 柱角 = (p - 1) * 2 * Math.PI / (柱数 - 1) + 0.9;
                柱心x += Math.cos(柱角) * 外柱半径;
                柱心z += Math.sin(柱角) * 外柱半径;
            }
            for (double yOff = 0; yOff < 柱高度; yOff += 0.6) {
                Location 柱点 = new Location(爆发位置.getWorld(), 柱心x, 爆发位置.getY() + yOff, 柱心z);
                世界.spawnParticle(Particle.END_ROD, 柱点, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
        世界.spawnParticle(Particle.DUST, 爆发位置, 配置.粒子数量(50),
                1.5, 0.8, 1.5, 0.2,
                new Particle.DustOptions(湮灭主颜色, 配置.粒子尺寸(2.5f)));
        for (int 层 = 0; 层 < 4; 层++) {
            Color 环颜色;
            if (层 == 0) {
                环颜色 = 湮灭核心颜色;
            } else if (层 == 1) {
                环颜色 = 湮灭主颜色;
            } else if (层 == 2) {
                环颜色 = 湮灭修饰颜色;
            } else {
                环颜色 = 湮灭暗影颜色;
            }
            double 环半径 = 配置.视觉半径() * (0.3 + 层 * 0.22);
            int 环点数 = 配置.环点数量(16 + 层 * 4);
            float 环高 = (层 - 1.5f) * 0.6f;
            for (int i = 0; i < 环点数; i++) {
                double 角 = i * Math.PI * 2 / 环点数;
                double x = 爆发位置.getX() + Math.cos(角) * 环半径;
                double z = 爆发位置.getZ() + Math.sin(角) * 环半径;
                Location 环点 = new Location(爆发位置.getWorld(), x, 爆发位置.getY() + 环高, z);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.03, 0.03, 0.03, 0.0,
                        new Particle.DustOptions(环颜色, 1.3f));
            }
        }
        Location 天空位置 = 爆发位置.clone().add(0, 7, 0);
        世界.spawnParticle(Particle.SCULK_SOUL, 天空位置, 配置.粒子数量(40),
                2.4, 1.2, 2.4, 0.02);
        世界.spawnParticle(Particle.DRAGON_BREATH, 天空位置, 配置.粒子数量(80),
                3.0, 1.5, 3.0, 0.01, 湮灭龙息强度);
        BukkitTask[] 续燃任务句柄 = new BukkitTask[1];
        BukkitRunnable 续燃任务 = new BukkitRunnable() {
            int 续燃刻 = 0;

            @Override
            public void run() {
                if (续燃刻 >= 配置.湮灭续燃持续刻()) {
                    cancel();
                    移除任务(玩家标识, 续燃任务句柄[0]);
                    return;
                }
                org.bukkit.entity.Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (!玩家状态有效(玩家, 中心.世界(), 位置适配器.转换(中心), 配置, true)) {
                    cancel();
                    取消玩家任务(玩家标识, "终极续燃玩家状态失效");
                    return;
                }
                世界.spawnParticle(Particle.DRAGON_BREATH, 天空位置, 配置.粒子数量(25),
                        2.4, 1.2, 2.4, 0.01, 湮灭龙息强度);
                续燃刻++;
            }
        };
        BukkitTask 续燃任务句柄值 = 续燃任务.runTaskTimer(插件, 2, 配置.湮灭续燃间隔刻());
        续燃任务句柄[0] = 续燃任务句柄值;
        注册任务(玩家标识, 续燃任务句柄值);
        世界.playSound(爆发位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(1.3f), 0.6f);
    }

    // 第四段爆发前 0.5 秒（10刻）蓄力：灰色SMOKE汇聚（半径5.0→中心收紧）+ END_ROD中心光柱 + 音效渐强
    private void 启动第四段蓄力(位置 中心, 奥能法师大招特效配置 配置, UUID 玩家标识,
                              UUID 事件标识, UUID 施法标识, 玩家快照 施法者) {
        final double 当前第一段延迟 = 获取参数("第一段延迟", 第一段延迟);
        final double 当前段间隔 = 获取参数("段间隔", 段间隔);
        final double 当前伤害半径 = 获取参数("半径", 半径);
        double 蓄力延迟秒 = 当前第一段延迟 + 3 * 当前段间隔 - 第四段蓄力秒数;
        long 蓄力延迟刻 = (long) (蓄力延迟秒 * 20);
        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 蓄力任务 = new BukkitRunnable() {
            int 蓄力刻 = 0;

            @Override
            public void run() {
                if (蓄力刻 >= 第四段蓄力刻数) {
                    cancel();
                    移除任务(玩家标识, 任务句柄[0]);
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                            "蓄力结束", "成功", "task=phase4_charge");
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                            "任务停止", "成功", "task=phase4_charge reason=duration_complete");
                    return;
                }
                World 世界 = Bukkit.getWorld(中心.世界());
                if (世界 == null) {
                    cancel();
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                            "取消原因", "失败", "task=phase4_charge reason=world_unavailable");
                    return;
                }
                org.bukkit.entity.Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (!玩家状态有效(玩家, 中心.世界(), 位置适配器.转换(中心), 配置, true)) {
                    cancel();
                    记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                            "取消原因", "失败", "task=phase4_charge reason=player_state_invalid");
                    取消玩家任务(玩家标识, "第四段蓄力玩家状态失效");
                    return;
                }
                Location 蓄力位置 = 位置适配器.转换(中心).clone().add(0, 爆发高度偏移, 0);
                double 进度 = (double) 蓄力刻 / 第四段蓄力刻数;
                double 吸入半径 = 当前伤害半径 * (1.0 - 进度 * 0.8);
                int 吸入粒子数 = (int) (8 + 进度 * 12);
                for (int i = 0; i < 吸入粒子数; i++) {
                    double 角 = Math.random() * Math.PI * 2;
                    double 距 = Math.random() * 吸入半径;
                    double x = 蓄力位置.getX() + Math.cos(角) * 距;
                    double z = 蓄力位置.getZ() + Math.sin(角) * 距;
                    double y = 蓄力位置.getY() + Math.random() * 1.5;
                    Location 吸入点 = new Location(蓄力位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.SMOKE, 吸入点, 1,
                            0.04, 0.04, 0.04, 0.0);
                }
                double 柱高度 = 2.0 + 进度 * 10.0;
                for (double yOff = 0; yOff < 柱高度; yOff += 0.5) {
                    Location 柱点 = 蓄力位置.clone().add(0, yOff, 0);
                    世界.spawnParticle(Particle.END_ROD, 柱点, 1, 0.02, 0.02, 0.02, 0.01);
                }
                if (蓄力刻 % 3 == 0) {
                    世界.playSound(蓄力位置, Sound.BLOCK_BEACON_POWER_SELECT,
                            配置.音量((float)(0.4 + 进度 * 0.6)), (float)(0.5 + 进度 * 1.0));
                }
                蓄力刻++;
            }
        };
        try {
            BukkitTask 任务 = 蓄力任务.runTaskTimer(插件, 蓄力延迟刻, 1);
            任务句柄[0] = 任务;
            注册任务(玩家标识, 任务);
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=phase4_charge delay_ticks=%d duration_ticks=%d",
                    蓄力延迟刻, 第四段蓄力刻数);
        } catch (RuntimeException | Error 异常) {
            记录技能阶段("秘法湮灭", 施法者, "1_8", 事件标识, 施法标识, null,
                    "任务启动", "失败", "task=phase4_charge reason=%s", 异常.getClass().getSimpleName());
        }
    }

    private boolean 玩家状态有效(org.bukkit.entity.Player 玩家, String 世界名称, Location 起点,
                            奥能法师大招特效配置 配置, boolean 检查距离) {
        if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
            return false;
        }
        Location 当前位置 = 玩家.getLocation();
        if (当前位置 == null || 当前位置.getWorld() == null
                || !当前位置.getWorld().getName().equals(世界名称)) {
            return false;
        }
        return !检查距离 || 起点 == null || 起点.getWorld() == null
                || 起点.distanceSquared(当前位置) <= 配置.最大距离() * 配置.最大距离();
    }

    public void 取消玩家任务(UUID 玩家标识, String 原因) {
        if (玩家标识 == null) {
            return;
        }
        Set<事件日志上下文.异步事件租约> 租约集合 = 异步租约表.remove(玩家标识);
        if (租约集合 != null) {
            租约集合.forEach(事件日志上下文.异步事件租约::释放);
        }
        任务组.取消(玩家标识, "秘法湮灭", 原因);
        活跃玩家.remove(玩家标识);
        调试日志器.调试("秘法湮灭", "生命周期结束：玩家=%s 原因=%s", 玩家标识, 原因);
    }

    public void 取消全部任务(String 原因) {
        Set.copyOf(活跃玩家).forEach(玩家标识 -> 取消玩家任务(玩家标识, 原因));
    }

    private void 注册任务(UUID 玩家标识, BukkitTask 任务) {
        活跃玩家.add(玩家标识);
        任务组.注册(玩家标识, 任务, "秘法湮灭");
    }

    private void 移除任务(UUID 玩家标识, BukkitTask 任务) {
        任务组.移除(玩家标识, 任务, "秘法湮灭");
        if (任务组.获取数量(玩家标识) == 0) {
            活跃玩家.remove(玩家标识);
            调试日志器.调试("秘法湮灭", "生命周期结束：玩家=%s 原因=任务自然完成", 玩家标识);
        }
    }

    private void 注册异步租约(UUID 玩家标识, 事件日志上下文.异步事件租约 租约) {
        异步租约表.computeIfAbsent(玩家标识, 忽略 -> ConcurrentHashMap.newKeySet()).add(租约);
    }

    private void 释放异步租约(UUID 玩家标识, 事件日志上下文.异步事件租约 租约) {
        Set<事件日志上下文.异步事件租约> 租约集合 = 异步租约表.get(玩家标识);
        if (租约集合 != null) {
            租约集合.remove(租约);
            if (租约集合.isEmpty()) {
                异步租约表.remove(玩家标识, 租约集合);
            }
        }
        租约.释放();
    }
}
