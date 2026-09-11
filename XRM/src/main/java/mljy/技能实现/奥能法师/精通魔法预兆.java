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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@技能定义(
        技能标识 = "1_9",
        名称翻译键 = "skill.奥能法师.精通魔法预兆.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 0.0,
        公共冷却 = 0.0
)
public class 精通魔法预兆 extends 奥能法师技能基础 implements 技能执行器 {
    private static final long 脱战检查周期 = 100;
    private static final int 激活粒子数量 = 30;
    private static final double 激活粒子扩散 = 0.6;
    private static final Color 奥术粒子颜色 = Color.fromRGB(180, 80, 255);
    private static final float 奥术粒子尺寸 = 1.5f;
    private static final double 眼睛高度偏移 = 1.5;

    private BukkitRunnable 脱战任务;

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("精通魔法预兆", 请求施法者, "1_9", 根事件标识, 施法标识, null,
                "释放请求", "进入", "instant=瞬发 secret_energy_cap=%.1f", 秘能上限);
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("精通魔法预兆", "释放跳过：技能上下文或施法者为空");
            记录技能阶段("精通魔法预兆", null, "1_9", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        调试日志器.调试("精通魔法预兆", "触发：施法者=%s", 施法者.名称());
        记录技能阶段("精通魔法预兆", 施法者, "1_9", 根事件标识, 施法标识, null,
                "前置状态", "通过", "服务校验=通过");
        if (脱战任务 != null) {
            脱战任务.cancel();
            记录技能阶段("精通魔法预兆", 施法者, "1_9", 根事件标识, 施法标识, null,
                    "任务停止", "完成", "task=脱战损失 reason=replace_previous");
        }
        播放预兆激活特效(施法者);
        启动脱战损失(施法者);
        记录技能阶段("精通魔法预兆", 施法者, "1_9", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=脱战损失");
        记录技能阶段("精通魔法预兆", 施法者, "1_9", 根事件标识, 施法标识, null,
                "释放后状态", "成功", "脱战监控=已启动");
        记录技能阶段("精通魔法预兆", 施法者, "1_9", 根事件标识, 施法标识, null,
                "最终状态", "结束", "out_of_combat_monitor=已启动");
        return 技能执行结果.成功;
    }

    @Override
    public void 声明参数() {
        注册参数("1_9", "脱战检查周期", "100", "脱战检查周期刻数");
        注册参数("1_9", "激活粒子数量", "30", "激活特效粒子数量");
        注册参数("1_9", "激活粒子扩散", "0.6", "激活特效粒子扩散范围");
        注册参数("1_9_1", "秘兆触发概率", "0.5", "秘兆触发概率");
        注册参数("1_9_1", "秘兆持续时间", "5000", "秘兆效果持续毫秒数");
    }

    private void 播放预兆激活特效(玩家快照 施法者) {
        位置 起点 = 获取实时位置(施法者);
        if (起点 == null) {
            return;
        }
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 眼睛高度偏移, 0);
        final int 当前激活粒子数量 = 获取参数("激活粒子数量", 激活粒子数量);
        final double 当前激活粒子扩散 = 获取参数("激活粒子扩散", 激活粒子扩散);
        世界.spawnParticle(Particle.DUST, 施法位置, 当前激活粒子数量,
                当前激活粒子扩散, 当前激活粒子扩散, 当前激活粒子扩散, 0.1,
                new Particle.DustOptions(奥术粒子颜色, 奥术粒子尺寸));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 15, 0.5, 0.5, 0.5, 0.05);
        世界.playSound(施法位置, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
    }

    private void 启动脱战损失(玩家快照 施法者) {
        if (脱战任务 != null) {
            脱战任务.cancel();
        }
        final long 当前脱战检查周期 = 获取参数("脱战检查周期", 脱战检查周期);
        脱战任务 = new BukkitRunnable() {
            @Override
            public void run() {
                Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
                if (玩家 == null || !玩家.isOnline()) {
                    调试日志器.调试("精通魔法预兆", "脱战检查跳过：玩家不在线或不存在 施法者=%s", 施法者.名称());
                    cancel();
                    return;
                }
                boolean 战斗中 = 战斗状态服务.是否战斗中(施法者.唯一标识());
                double 当前秘能 = 获取秘能层数(施法者);
                调试日志器.调试("精通魔法预兆", "脱战检查tick：施法者=%s 战斗中=%b 当前秘能=%.1f 周期刻=%d",
                        施法者.名称(), 战斗中, 当前秘能, 当前脱战检查周期);
                if (!战斗中) {
                    if (当前秘能 > 0) {
                        资源变更服务.消耗(施法者.唯一标识(), 秘能资源标识, 1);
                        double 消耗后 = 获取秘能层数(施法者);
                        调试日志器.调试("精通魔法预兆", "脱战损失秘能：施法者=%s before=%.1f after=%.1f delta=-1",
                                施法者.名称(), 当前秘能, 消耗后);
                        记录技能阶段("精通魔法预兆", 施法者, "1_9", null, null, null,
                                "脱战资源变化", "完成", "resource=秘能 before=%.1f after=%.1f delta=-1", 当前秘能, 消耗后);
                    } else {
                        调试日志器.调试("精通魔法预兆", "脱战损失跳过：当前秘能为零 施法者=%s", 施法者.名称());
                    }
                }
            }
        };
        脱战任务.runTaskTimer(插件, 当前脱战检查周期, 当前脱战检查周期);
    }
}
