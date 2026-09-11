package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.调试日志器;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@技能定义(
        技能标识 = "1_2",
        名称翻译键 = "skill.奥能法师.秘法崩裂.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 4.0,
        公共冷却 = 1.5,
        射程 = 5.0
)
public class 秘法崩裂 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 智力系数 = 0.37;
    private static final double 基础系数 = 0.15;
    private static final double 延迟秒 = 0.4;
    private static final double 半径 = 5.0;
    private static final double 凝聚高度偏移 = 1.5;
    private static final double 爆发高度偏移 = 1.0;
    private static final Color 崩裂主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 崩裂修饰颜色 = Color.fromRGB(140, 50, 220);
    private static final Color 崩裂核心颜色 = Color.fromRGB(255, 200, 255);
    private static final long 延迟刻 = (long) (延迟秒 * 20);
    private static final double 资源比较容差 = 0.000001;

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        final double 当前延迟秒 = 获取参数("延迟秒", 延迟秒);
        final double 当前半径 = 获取参数("半径", 半径);
        final double 当前智力系数 = 获取参数("智力系数", 智力系数);
        final double 当前基础系数 = 获取参数("基础系数", 基础系数);
        final long 当前延迟刻 = (long) (当前延迟秒 * 20);
        记录技能阶段("秘法崩裂", 请求施法者, "1_2", 根事件标识, 施法标识, null,
                "释放请求", "进入", "instant=瞬发 delay=%.1f", 当前延迟秒);
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("秘法崩裂", "释放跳过：技能上下文或施法者为空");
            记录技能阶段("秘法崩裂", null, "1_2", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        调试日志器.调试("秘法崩裂", "请求 root_event_id=%s cast_id=%s target_uuid=null", 根事件标识, 施法标识);
        位置 起点 = 获取实时位置(施法者);
        if (起点 == null) {
            调试日志器.调试("秘法崩裂", "释放跳过：施法者位置无效 玩家=%s", 施法者.唯一标识());
            记录技能阶段("秘法崩裂", 施法者, "1_2", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=位置无效");
            return 技能执行结果.位置无效;
        }
        if (Bukkit.getWorld(起点.世界()) == null) {
            调试日志器.调试("秘法崩裂", "释放跳过：施法世界无效 玩家=%s 世界=%s", 施法者.唯一标识(), 起点.世界());
            记录技能阶段("秘法崩裂", 施法者, "1_2", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=世界无效 world=%s", 起点.世界());
            return 技能执行结果.位置无效;
        }
        调试日志器.调试("秘法崩裂", "前置通过 root_event_id=%s cast_id=%s 施法者=%s", 根事件标识, 施法标识, 施法者.名称());
        记录技能阶段("秘法崩裂", 施法者, "1_2", 根事件标识, 施法标识, null,
                "前置状态", "通过", "服务校验=通过");
        资源消费快照 消费快照 = 原子消费全部秘能(施法者, 根事件标识, 施法标识);
        if (!消费快照.成功()) {
            调试日志器.调试("秘法崩裂", "失败 root_event_id=%s cast_id=%s target_uuid=null 资源before=%.1f 原因=零秘能无副作用",
                    根事件标识, 施法标识, 消费快照.消费前());
            记录技能阶段("秘法崩裂", 施法者, "1_2", 根事件标识, 施法标识, null,
                    "资源变化", "失败", "resource=秘能 before=%.1f reason=零秘能无副作用", 消费快照.消费前());
            return 技能执行结果.资源不足;
        }
        记录技能阶段("秘法崩裂", 施法者, "1_2", 根事件标识, 施法标识, null,
                "资源变化", "成功", "resource=秘能 before=%.1f after=%.1f consumed=%.1f",
                消费快照.消费前(), 消费快照.消费后(), 消费快照.实际消费量());
        double 秘能层数 = 消费快照.消费前();
        double 实际消费量 = 消费快照.实际消费量();
        AtomicBoolean 已结算 = new AtomicBoolean(false);
        Runnable 提交资源 = () -> {
            if (已结算.compareAndSet(false, true)) {
                调试日志器.调试("秘法崩裂", "资源提交 root_event_id=%s cast_id=%s target_uuid=null consumed=%.1f",
                        根事件标识, 施法标识, 实际消费量);
            }
        };
        Runnable 回滚资源 = () -> {
            if (已结算.compareAndSet(false, true)) {
                尝试恢复资源(施法者, 实际消费量, 根事件标识, 施法标识);
                调试日志器.调试("秘法崩裂", "资源回滚 root_event_id=%s cast_id=%s target_uuid=null 恢复=%.1f",
                        根事件标识, 施法标识, 实际消费量);
            }
        };
        调试日志器.调试("秘法崩裂", "资源快照 root_event_id=%s cast_id=%s target_uuid=null before=%.1f after=%.1f consumed=%.1f snapshot=%.1f",
                根事件标识, 施法标识, 消费快照.消费前(), 消费快照.消费后(), 实际消费量, 秘能层数);

        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        事件日志上下文.异步事件租约 异步租约 = 事件日志上下文.获取异步事件租约(事件标识);
        if (异步租约 == null) {
            回滚资源.run();
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
            return 技能执行结果.执行异常;
        }
        BukkitRunnable 延迟任务 = new BukkitRunnable() {
            private int 预警刻 = 0;
            private boolean 已结束;

            @Override
            public void run() {
                try {
                    事件日志上下文.在事件中执行(事件标识, () -> {
                        if (已结束) {
                            return;
                        }
                        if (预警刻 < 当前延迟刻) {
                            位置 预警中心 = 获取实时位置(施法者);
                            if (预警中心 == null || Bukkit.getWorld(预警中心.世界()) == null) {
                                结束失败("预警阶段位置或世界无效");
                                return;
                            }
                            播放预警粒子(预警中心, (double) 预警刻 / 当前延迟刻);
                            预警刻++;
                            return;
                        }
                        位置 中心 = 获取实时位置(施法者);
                        if (中心 == null || Bukkit.getWorld(中心.世界()) == null) {
                            结束失败("爆发阶段位置或世界无效");
                            return;
                        }
                        try {
                            if (!播放爆发特效(中心)) {
                                结束失败("爆发世界无效");
                                return;
                            }
                            调试日志器.调试("秘法崩裂", "伤害before root_event_id=%s cast_id=%s target_uuid=null snapshot=%.1f",
                                    事件标识, 施法标识, 秘能层数);
                            List<实体> 目标列表 = 目标选择服务.选择敌人(施法者, 中心, 当前半径);
                            if (目标列表 == null || 目标列表.isEmpty()) {
                                记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                                        "伤害结算", "未命中", "reason=目标列表为空");
                                结束失败("目标列表为空");
                                return;
                            }
                            double 基础伤害 = 施法者.属性().智力() * 当前基础系数;
                            double 秘能伤害 = 秘能层数 * 当前智力系数 * 施法者.属性().智力();
                            double 总伤害 = 基础伤害 + 秘能伤害;
                            double 单目标基础伤害 = Math.floor(总伤害 / 目标列表.size());
                            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                                    "伤害结算", "开始", "target_count=%d radius=%.1f base_damage=%.2f secret_damage=%.2f per_target_base=%.2f snapshot=%.1f",
                                    目标列表.size(), 当前半径, 基础伤害, 秘能伤害, 单目标基础伤害, 秘能层数);
                            boolean 命中成功 = 应用范围伤害并记录日志(施法者, 中心, 当前半径, 单目标基础伤害, 获取参数("Aoe衰减阈值", Aoe衰减阈值), "skill.奥能法师.秘法崩裂.name", 0, true);
                            if (!命中成功) {
                                记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                                        "伤害结算", "未命中", "reason=目标执行未命中");
                                结束失败("目标执行未命中");
                                return;
                            }
                            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                                    "伤害结算", "完成", "target_count=%d per_target_base=%.2f total_damage=%.2f",
                                    目标列表.size(), 单目标基础伤害, 单目标基础伤害 * 目标列表.size());
                            调试日志器.调试("秘法崩裂", "伤害after root_event_id=%s cast_id=%s target_uuid=null 命中目标=%d 快照=%.1f 新秘能保留",
                                    事件标识, 施法标识, 目标列表.size(), 秘能层数);
                        } catch (RuntimeException | Error 异常) {
                            调试日志器.调试("秘法崩裂", "执行异常 root_event_id=%s cast_id=%s target_uuid=null 异常=%s 资源将回滚",
                                    事件标识, 施法标识, 异常.getClass().getSimpleName());
                            结束失败("执行异常");
                        } finally {
                            if (!已结束) {
                                完成();
                            }
                        }
                    });
                } catch (RuntimeException | Error 异常) {
                    调试日志器.调试("秘法崩裂", "异步执行异常 root_event_id=%s cast_id=%s target_uuid=null 异常=%s 资源将回滚",
                            事件标识, 施法标识, 异常.getClass().getSimpleName());
                    结束失败("异步执行异常");
                }
            }

            private void 完成() {
                if (已结束) {
                    return;
                }
                已结束 = true;
                提交资源.run();
                try {
                    cancel();
                } catch (RuntimeException | Error 异常) {
                    调试日志器.调试("秘法崩裂", "成功清理任务异常 root_event_id=%s cast_id=%s target_uuid=null 异常=%s",
                            事件标识, 施法标识, 异常.getClass().getSimpleName());
                } finally {
                    异步租约.释放();
                }
                调试日志器.调试("秘法崩裂", "完成 root_event_id=%s cast_id=%s target_uuid=null snapshot=%.1f",
                        事件标识, 施法标识, 秘能层数);
                记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                        "任务停止", "完成", "task=延迟爆发 snapshot=%.1f", 秘能层数);
            }

            private void 结束失败(String 原因) {
                if (已结束) {
                    return;
                }
                已结束 = true;
                调试日志器.调试("秘法崩裂", "释放结束 root_event_id=%s cast_id=%s target_uuid=null 施法者=%s 原因=%s 资源回滚",
                        事件标识, 施法标识, 施法者.名称(), 原因);
                记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                        "取消原因", "失败", "reason=%s", 原因);
                回滚资源.run();
                try {
                    cancel();
                } catch (RuntimeException | Error 异常) {
                    调试日志器.调试("秘法崩裂", "失败清理任务异常 root_event_id=%s cast_id=%s target_uuid=null 异常=%s",
                            事件标识, 施法标识, 异常.getClass().getSimpleName());
                } finally {
                    异步租约.释放();
                }
            }
        };
        try {
            BukkitTask 已注册任务 = 延迟任务.runTaskTimer(插件, 0, 1);
            if (已注册任务 == null) {
                throw new IllegalStateException("调度器返回空任务");
            }
            播放凝聚特效(起点);
            调试日志器.调试("秘法崩裂", "初始化完成 root_event_id=%s cast_id=%s target_uuid=null snapshot=%.1f",
                    事件标识, 施法标识, 秘能层数);
            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=延迟爆发 delay_ticks=%d", 当前延迟刻);
            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                    "释放后状态", "成功", "snapshot=%.1f async_event=%s", 秘能层数, 事件标识);
            技能释放服务实例.抑制通用释放日志(施法者.唯一标识(), 上下文.技能标识());
            String 槽位中文数字 = 查找槽位中文数字("skill.奥能法师.秘法崩裂.name");
            消息服务.发送技能日志(施法者, "技能日志.延迟释放", String.format(数值格式, 当前延迟秒), 槽位中文数字);
            return 技能执行结果.成功;
        } catch (RuntimeException | Error 异常) {
            try {
                延迟任务.cancel();
            } catch (RuntimeException | Error ignored) {
            }
            回滚资源.run();
            try {
                异步租约.释放();
            } catch (RuntimeException | Error 租约异常) {
                调试日志器.调试("秘法崩裂", "初始化释放租约异常 root_event_id=%s cast_id=%s target_uuid=null 异常=%s",
                        事件标识, 施法标识, 租约异常.getClass().getSimpleName());
            }
            调试日志器.调试("秘法崩裂", "初始化失败 root_event_id=%s cast_id=%s target_uuid=null 异常=%s 资源已回滚",
                    事件标识, 施法标识, 异常.getClass().getSimpleName());
            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                    "任务启动", "失败", "reason=调度异常 exception=%s", 异常.getClass().getSimpleName());
            return 技能执行结果.执行异常;
        } finally {
            记录技能阶段("秘法崩裂", 施法者, "1_2", 事件标识, 施法标识, null,
                    "最终状态", "结束", "task=延迟爆发 snapshot=%.1f", 秘能层数);
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_2", "智力系数", "0.37", "秘能层数智力伤害系数");
        注册参数("1_2", "基础系数", "0.15", "基础智力伤害系数");
        注册参数("1_2", "延迟秒", "0.4", "爆发延迟秒数");
        注册参数("1_2", "半径", "5.0", "伤害范围半径");
        注册参数("1_2", "Aoe衰减阈值", "5", "AOE伤害衰减阈值");
        注册参数("1_2", "暴击倍率", "1.5", "暴击伤害倍率");
    }

    private 资源消费快照 原子消费全部秘能(玩家快照 施法者, UUID 根事件标识, UUID 施法标识) {
        if (施法者 == null || 施法者.唯一标识() == null || 资源变更服务 == null) {
            return 资源消费快照.失败(0.0, 0.0, 0.0);
        }
        synchronized (资源变更服务) {
            double 消费前 = 获取秘能层数(施法者);
            if (!Double.isFinite(消费前) || 消费前 <= 资源比较容差) {
                return 资源消费快照.失败(消费前, 消费前, 0.0);
            }
            try {
                资源变更服务.清空(施法者.唯一标识(), 秘能资源标识);
            } catch (RuntimeException | Error 异常) {
                double 异常后 = 消费前;
                try {
                    异常后 = 获取秘能层数(施法者);
                } catch (RuntimeException | Error 读取异常) {
                    调试日志器.调试("秘法崩裂", "资源消费后读失败 root_event_id=%s cast_id=%s target_uuid=null 异常=%s",
                            根事件标识, 施法标识, 读取异常.getClass().getSimpleName());
                }
                double 实际消费量 = 计算实际消费量(消费前, 异常后);
                尝试恢复资源(施法者, 实际消费量, 根事件标识, 施法标识);
                调试日志器.调试("秘法崩裂", "资源消费失败 root_event_id=%s cast_id=%s target_uuid=null before=%.1f after=%.1f consumed=%.1f 异常=%s",
                        根事件标识, 施法标识, 消费前, 异常后, 实际消费量, 异常.getClass().getSimpleName());
                return 资源消费快照.失败(消费前, 异常后, 实际消费量);
            }
            double 消费后 = 获取秘能层数(施法者);
            double 实际消费量 = 计算实际消费量(消费前, 消费后);
            if (!Double.isFinite(消费后) || 消费后 > 资源比较容差 || 实际消费量 <= 资源比较容差) {
                尝试恢复资源(施法者, 实际消费量, 根事件标识, 施法标识);
                调试日志器.调试("秘法崩裂", "资源消费未完成 root_event_id=%s cast_id=%s target_uuid=null before=%.1f after=%.1f consumed=%.1f 已回滚",
                        根事件标识, 施法标识, 消费前, 消费后, 实际消费量);
                return 资源消费快照.失败(消费前, 消费后, 实际消费量);
            }
            return 资源消费快照.成功(消费前, 消费后, 实际消费量);
        }
    }

    private void 尝试恢复资源(玩家快照 施法者, double 恢复量, UUID 根事件标识, UUID 施法标识) {
        if (!Double.isFinite(恢复量) || 恢复量 <= 资源比较容差) {
            return;
        }
        try {
            资源变更服务.增加(施法者.唯一标识(), 秘能资源标识, 恢复量);
        } catch (RuntimeException | Error 异常) {
            调试日志器.调试("秘法崩裂", "资源回滚异常 root_event_id=%s cast_id=%s target_uuid=null 恢复=%.1f 异常=%s",
                    根事件标识, 施法标识, 恢复量, 异常.getClass().getSimpleName());
        }
    }

    private double 计算实际消费量(double 消费前, double 消费后) {
        if (!Double.isFinite(消费前) || !Double.isFinite(消费后)) {
            return 0.0;
        }
        return Math.max(0.0, 消费前 - 消费后);
    }

    private record 资源消费快照(boolean 成功, double 消费前, double 消费后, double 实际消费量) {
        private static 资源消费快照 成功(double 消费前, double 消费后, double 实际消费量) {
            return new 资源消费快照(true, 消费前, 消费后, 实际消费量);
        }

        private static 资源消费快照 失败(double 消费前, double 消费后, double 实际消费量) {
            return new 资源消费快照(false, 消费前, 消费后, 实际消费量);
        }
    }

    private void 播放凝聚特效(位置 起点) {
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 凝聚高度偏移, 0);
        世界.spawnParticle(Particle.DUST, 施法位置, 35,
                0.5, 0.5, 0.5, 0.1,
                new Particle.DustOptions(崩裂主颜色, 2.0f));
        世界.spawnParticle(Particle.DUST, 施法位置, 15,
                0.2, 0.2, 0.2, 0.2,
                new Particle.DustOptions(崩裂核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 15, 0.4, 0.4, 0.4, 0.05);
        世界.playSound(施法位置, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
        世界.playSound(施法位置, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.5f);
    }

    private void 播放预警粒子(位置 中心, double 进度) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            return;
        }
        Location 预警中心 = 位置适配器.转换(中心).clone();
        int 圆上点数 = 20;
        double 当前半径 = 获取参数("半径", 半径) * 进度;
        for (int i = 0; i < 圆上点数; i++) {
            double 角 = i * Math.PI * 2 / 圆上点数;
            double x = 预警中心.getX() + Math.cos(角) * 当前半径;
            double z = 预警中心.getZ() + Math.sin(角) * 当前半径;
            Location 圆点 = new Location(预警中心.getWorld(), x, 预警中心.getY(), z);
            世界.spawnParticle(Particle.DUST, 圆点, 1,
                    0.0, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(崩裂修饰颜色, 1.4f));
        }
        Location 地面中心 = 预警中心.clone().add(0, -1.5, 0);
        世界.spawnParticle(Particle.DUST, 地面中心, 5,
                当前半径 * 0.5, 0.1, 当前半径 * 0.5, 0.0,
                new Particle.DustOptions(崩裂主颜色, 1.2f));
        世界.spawnParticle(Particle.END_ROD, 地面中心, 3,
                当前半径 * 0.3, 0.1, 当前半径 * 0.3, 0.0);
    }

    private boolean 播放爆发特效(位置 中心) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            return false;
        }
        Location 爆发位置 = 位置适配器.转换(中心).clone().add(0, 爆发高度偏移, 0);
        世界.spawnParticle(Particle.DUST, 爆发位置, 50,
                1.0, 1.0, 1.0, 0.15,
                new Particle.DustOptions(崩裂主颜色, 2.2f));
        世界.spawnParticle(Particle.DUST, 爆发位置, 25,
                0.5, 0.5, 0.5, 0.25,
                new Particle.DustOptions(崩裂核心颜色, 1.2f));
        世界.spawnParticle(Particle.END_ROD, 爆发位置, 25, 0.8, 0.8, 0.8, 0.1);
        世界.spawnParticle(Particle.EXPLOSION, 爆发位置, 3, 0.6, 0.6, 0.6, 0.0);
        世界.spawnParticle(Particle.SONIC_BOOM, 爆发位置, 1, 0.1, 0.1, 0.1, 0.0);
        世界.playSound(爆发位置, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.1f);
        世界.playSound(爆发位置, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.8f);

        for (int i = 0; i < 3; i++) {
            double 环半径 = 获取参数("半径", 半径) * (0.5 + i * 0.2);
            for (int j = 0; j < 16; j++) {
                double 角 = j * Math.PI * 2 / 16;
                double x = 爆发位置.getX() + Math.cos(角) * 环半径;
                double z = 爆发位置.getZ() + Math.sin(角) * 环半径;
                Location 环点 = new Location(爆发位置.getWorld(), x, 爆发位置.getY() + (i - 1) * 0.5, z);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.05, 0.05, 0.05, 0.0,
                        new Particle.DustOptions(崩裂修饰颜色, 1.0f));
            }
        }
        return true;
    }
}
