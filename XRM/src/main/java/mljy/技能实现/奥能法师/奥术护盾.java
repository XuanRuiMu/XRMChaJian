package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.调试日志器;
import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数读取器;
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

import java.util.Optional;
import java.util.UUID;

@技能定义(
        技能标识 = "1_3",
        名称翻译键 = "skill.奥能法师.奥术护盾.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 10.0,
        公共冷却 = 1.5
)
public class 奥术护盾 extends 奥能法师技能基础 implements 技能执行器 {
    private static final java.util.logging.Logger 日志 = java.util.logging.Logger.getLogger(奥术护盾.class.getName());
    private static final double 护盾系数 = 0.4;
    private static final long 默认持续 = 20000L;
    private static final double 护盾高度偏移 = 1.0;
    private static final Color 护盾主颜色 = Color.fromRGB(80, 150, 255);
    private static final Color 护盾修饰颜色 = Color.fromRGB(160, 210, 255);
    private static final Color 护盾核心颜色 = Color.fromRGB(200, 240, 255);
    private static final double 护盾环半径 = 1.2;

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        final double 当前护盾系数 = 获取参数("护盾系数", 护盾系数);
        final long 当前持续 = 获取参数("持续时间", 默认持续);
        记录技能阶段("奥术护盾", 请求施法者, "1_3", 根事件标识, 施法标识, null,
                "释放请求", "进入", "instant=瞬发 shield_coef=%.2f", 当前护盾系数);
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("奥术护盾", "释放跳过：技能上下文或施法者为空");
            记录技能阶段("奥术护盾", null, "1_3", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        调试日志器.调试("奥术护盾", "执行开始：施法者=%s 基础智力=%.1f", 施法者.名称(), 施法者.属性().基础智力());

        if (效果调度服务 == null) {
            日志.warning("[奥术护盾] 效果调度服务未注入，技能中断");
            记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=效果调度服务_未注入");
            return 技能执行结果.执行异常;
        }
        if (资源变更服务 == null) {
            日志.warning("[奥术护盾] 资源变更服务未注入，技能中断");
            记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=资源变更服务_未注入");
            return 技能执行结果.执行异常;
        }
        if (效果注册服务 == null) {
            日志.warning("[奥术护盾] 效果注册服务未注入，技能中断");
            记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=效果注册服务_未注入");
            return 技能执行结果.执行异常;
        }
        记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                "前置状态", "通过", "服务校验=通过");

        double 护盾值 = 施法者.属性().基础智力() * 当前护盾系数;
        long 到期时间 = 计算效果到期时间(奥术护盾效果标识, 当前持续);
        调试日志器.调试("奥术护盾", "护盾计算：护盾值=%.1f 到期时间=%d", 护盾值, 到期时间);

        Optional<效果实例> 现有护盾 = 效果调度服务.获取(施法者.唯一标识(), 奥术护盾效果标识);
        double 现有护盾值 = 现有护盾
                .map(e -> e.获取参数覆盖().获取双精度("护盾吸收量", e.获取层数()))
                .orElse(0.0D);

        if (现有护盾.isPresent() && 护盾值 < 现有护盾值) {
            调试日志器.调试("奥术护盾", "护盾大盖小保留：新值=%.2f 现有值=%.2f 保留现有不刷新", 护盾值, 现有护盾值);
        } else {
            if (现有护盾.isPresent()) {
                效果调度服务.移除(施法者.唯一标识(), 奥术护盾效果标识);
            }
            参数读取器 护盾参数覆盖 = new 参数读取器(java.util.Map.of("护盾吸收量", String.format("%.4f", 护盾值)));
            效果调度服务.添加(施法者.唯一标识(),
                    new 效果实例(奥术护盾效果标识, "1_3", 施法者.唯一标识(),
                            Math.max(1, (int) Math.floor(护盾值)), 到期时间, 护盾参数覆盖));
        }

        调试日志器.调试("奥术护盾", "护盾效果已添加：施法者=%s 层数=%d 护盾值=%.1f", 施法者.名称(), (int) Math.floor(护盾值), 护盾值);
        记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                "效果变化", "成功", "effect_id=%s 层数=%d 到期=%d", 奥术护盾效果标识, (int) Math.floor(护盾值), 到期时间);
        double 资源前 = 获取秘能层数(施法者);
        增加秘能(施法者, 1);
        double 资源后 = 获取秘能层数(施法者);
        记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                "资源变化", "成功", "resource=秘能 before=%.1f after=%.1f delta=1", 资源前, 资源后);
        调试日志器.调试("奥术护盾", "执行完成：施法者=%s", 施法者.名称());

        技能释放服务实例.抑制通用释放日志(施法者.唯一标识(), 上下文.技能标识());
        String 槽位中文数字 = 查找槽位中文数字("skill.奥能法师.奥术护盾.name");
        消息服务.发送技能日志(施法者, "技能日志.护盾释放", 槽位中文数字, String.format(数值格式, 护盾值));

        try {
            播放护盾激活特效(施法者);
            记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                    "任务启动", "成功", "task=护盾激活特效");
        } catch (Exception e) {
            日志.warning("[奥术护盾] 播放特效异常：" + e.getClass().getSimpleName() + ":" + e.getMessage());
            记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                    "任务启动", "失败", "reason=特效异常 exception=%s", e.getClass().getSimpleName());
        }
        记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                "任务停止", "完成", "task=护盾激活特效 reason=fire_and_forget");
        记录技能阶段("奥术护盾", 施法者, "1_3", 根事件标识, 施法标识, null,
                "释放后状态", "成功", "shield=%.1f 秘能=%.1f", 护盾值, 资源后);
        return 技能执行结果.成功;
    }

    @Override
    public void 声明参数() {
        注册参数("1_3", "护盾系数", "0.4", "护盾值智力系数");
        注册参数("1_3", "持续时间", "20000", "护盾持续毫秒数");
        注册参数("1_3", "暴击倍率", "1.5", "暴击伤害倍率");
        注册参数("1_3_1", "护盾系数", "0.4", "护盾值智力系数");
        注册参数("1_3_1", "持续时间", "20000", "护盾持续毫秒数");
    }

    private void 播放护盾激活特效(玩家快照 施法者) {
        if (插件 == null) {
            日志.warning("[奥术护盾] 插件未注入，跳过粒子特效");
            return;
        }
        位置 起点 = 获取实时位置(施法者);
        if (起点 == null) {
            return;
        }
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 护盾高度偏移, 0);

        世界.spawnParticle(Particle.DUST, 施法位置, 50,
                1.2, 1.2, 1.2, 0.15,
                new Particle.DustOptions(护盾主颜色, 2.0f));
        世界.spawnParticle(Particle.DUST, 施法位置, 25,
                0.6, 0.6, 0.6, 0.25,
                new Particle.DustOptions(护盾核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 25, 1.0, 1.0, 1.0, 0.08);
        世界.spawnParticle(Particle.WITCH, 施法位置, 30, 1.0, 1.0, 1.0, 0.15);
        世界.spawnParticle(Particle.SOUL_FIRE_FLAME, 施法位置, 10, 0.8, 0.8, 0.8, 0.02);

        for (int 层 = 0; 层 < 3; 层++) {
            double y偏移 = (层 - 1) * 1.0;
            for (int i = 0; i < 16; i++) {
                double 角 = i * Math.PI * 2 / 16;
                double x = 施法位置.getX() + Math.cos(角) * 护盾环半径;
                double z = 施法位置.getZ() + Math.sin(角) * 护盾环半径;
                Location 环点 = new Location(施法位置.getWorld(), x, 施法位置.getY() + y偏移, z);
                世界.spawnParticle(Particle.DUST, 环点, 1,
                        0.0, 0.0, 0.0, 0.0,
                        new Particle.DustOptions(护盾修饰颜色, 1.2f));
            }
        }

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 8) {
                    cancel();
                    return;
                }
                tick++;
                for (int i = 0; i < 8; i++) {
                    double 角 = i * Math.PI * 2 / 8 + tick * 0.3;
                    double x = 施法位置.getX() + Math.cos(角) * 护盾环半径;
                    double z = 施法位置.getZ() + Math.sin(角) * 护盾环半径;
                    Location 点 = new Location(施法位置.getWorld(), x, 施法位置.getY() + Math.sin(tick * 0.5) * 0.5, z);
                    世界.spawnParticle(Particle.DUST, 点, 1,
                            0.03, 0.03, 0.03, 0.0,
                            new Particle.DustOptions(护盾核心颜色, 0.6f));
                }
            }
        }.runTaskTimer(插件, 0, 2);

        世界.playSound(施法位置, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        世界.playSound(施法位置, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);
        世界.playSound(施法位置, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.5f, 0.8f);
    }
}
