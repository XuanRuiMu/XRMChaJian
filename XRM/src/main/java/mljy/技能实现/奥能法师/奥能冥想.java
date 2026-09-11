package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.效果.效果实例;
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

import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@技能定义(
        技能标识 = "1_7",
        名称翻译键 = "skill.奥能法师.奥能冥想.name",
        施法类型 = 施法类型.瞬发,
        冷却时间 = 60.0,
        公共冷却 = 1.5
)
public class 奥能冥想 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 生命恢复比例 = 0.2;
    private static final long 默认蓄能持续 = 2000L;
    private static final long 默认冥想持续 = 30000L;
    private static final double 冥想高度偏移 = 1.5;
    private static final double 光柱高度 = 0.5;
    private static final double 天空光柱高度 = 0.5;
    private static final double 外层光柱半径 = 0.7;
    private static final double 符文外半径 = 1.3;
    private static final double 符文内半径 = 0.5;
    private static final double 第二环半径 = 1.9;
    private static final double 第三环半径 = 2.7;
    private static final int 开始动画总刻 = 20;
    private static final int 完成动画总刻 = 26;
    private static final int 冲击波层数 = 2;
    private static final int 脉冲间隔 = 4;
    private static final Color 冥想主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 冥想蓝色 = Color.fromRGB(90, 150, 255);
    private static final Color 冥想白色 = Color.fromRGB(255, 255, 255);
    private static final Color 冥想修饰颜色 = Color.fromRGB(100, 40, 200);
    private static final Color 冥想核心颜色 = Color.fromRGB(255, 200, 255);
    private static final Color 冥想金色 = Color.fromRGB(255, 220, 80);
    private final 奥能法师大招任务组 任务组 = new 奥能法师大招任务组();
    private final Set<UUID> 活跃玩家 = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Set<事件日志上下文.异步事件租约>> 异步租约表 = new ConcurrentHashMap<>();

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识源 = 获取调试根事件标识();
        final UUID 根事件标识 = 根事件标识源 != null ? 根事件标识源 : UUID.randomUUID();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        记录技能阶段("奥能冥想", 请求施法者, "1_7", 根事件标识, 施法标识, null,
                "释放请求", "进入", "cooldown_gate=delegated resource_gate=秘能全部消耗");
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("奥能冥想", "释放跳过：技能上下文或施法者为空");
            记录技能阶段("奥能冥想", null, "1_7", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
        if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
            调试日志器.调试("奥能冥想", "释放跳过：施法者不在线或已死亡 玩家=%s", 施法者.唯一标识());
            记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=player_unavailable");
            return 技能执行结果.执行异常;
        }
        位置 中心 = 获取实时位置(施法者);
        if (中心 == null || 中心.世界() == null || 中心.世界().isBlank() || Bukkit.getWorld(中心.世界()) == null) {
            调试日志器.调试("奥能冥想", "释放跳过：施法者世界无效 玩家=%s", 施法者.唯一标识());
            记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=world_unavailable");
            return 技能执行结果.位置无效;
        }
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "前置状态", "通过", "cooldown_gate=delegated player_online=true world=%s", 中心.世界());
        final double 当前生命恢复比例 = 获取参数("生命恢复比例", 生命恢复比例);
        final long 当前蓄能持续 = 获取参数("蓄能持续", 默认蓄能持续);
        final long 当前冥想持续 = 获取参数("冥想持续", 默认冥想持续);
        奥能法师大招特效配置 配置 = 奥能法师大招特效配置.加载(插件);
        UUID 玩家标识 = 施法者.唯一标识();
        取消玩家任务(玩家标识, "重复施法");
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "任务停止", "完成", "reason=replace_previous_cast");
        效果调度服务.移除(玩家标识, 奥能冥想蓄能效果标识);
        调试日志器.调试("奥能冥想", "释放：施法者=%s 层数配置强度=%.2f 粒子上限=%d",
                施法者.名称(), 配置.强度(), 配置.粒子数量(250));
        double 秘能层数 = 获取秘能层数(施法者);
        if (秘能层数 <= 0) {
            记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                    "取消原因", "失败", "reason=insufficient_resource resource=秘能 value=%.1f", 秘能层数);
            return 技能执行结果.资源不足;
        }
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "资源变化", "开始", "resource=秘能 before=%.1f action=consume_all", 秘能层数);
        消耗所有秘能(施法者);
        double 消耗后秘能 = 获取秘能层数(施法者);
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "资源变化", "完成", "resource=秘能 before=%.1f after=%.1f delta=%.1f consume_flow=true", 秘能层数, 消耗后秘能, 消耗后秘能 - 秘能层数);

        AttributeInstance 最大生命属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        double 最大生命值 = 最大生命属性 != null ? 最大生命属性.getValue() : 20.0;
        double 生命前 = 玩家.getHealth();
        玩家.setHealth(Math.min(玩家.getHealth() + 施法者.属性().生命值上限() * 当前生命恢复比例, 最大生命值));
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "治疗结算", "完成", "health_before=%.1f health_after=%.1f", 生命前, 玩家.getHealth());
        技能释放服务实例.抑制通用释放日志(施法者.唯一标识(), 上下文.技能标识());
        消息服务.发送技能日志(施法者, 根事件标识, "奥能冥想.施放",
                String.valueOf((int) 秘能层数),
                String.format(数值格式, 玩家.getHealth() - 生命前));

        播放冥想开始特效(中心, 配置, 玩家标识);
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "任务启动", "成功", "task=meditation_start_animation");

        long 蓄能到期 = 计算效果到期时间(奥能冥想蓄能效果标识, 当前蓄能持续);
        效果调度服务.添加(施法者.唯一标识(), new 效果实例(奥能冥想蓄能效果标识, "1_7", 施法者.唯一标识(), (int) 秘能层数, 蓄能到期));
        记录技能阶段("奥能冥想", 施法者, "1_7", 根事件标识, 施法标识, null,
                "效果变化", "应用", "effect_id=%s stacks=%d expires_at=%d", 奥能冥想蓄能效果标识, (int) 秘能层数, 蓄能到期);
        UUID 事件标识 = 根事件标识;
        记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                "蓄力开始", "成功", "duration_ms=%d", 当前蓄能持续);
        事件日志上下文.异步事件租约 异步租约 = 事件日志上下文.获取异步事件租约(事件标识);
        if (异步租约 == null) {
            记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                    "任务启动", "失败", "reason=event_lease_unavailable");
            return 技能执行结果.执行异常;
        }

        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 蓄力任务 = new BukkitRunnable() {
            private boolean 已结束;

            @Override
            public void run() {
                try {
                    事件日志上下文.在事件中执行(事件标识, () -> {
                        if (已结束) {
                            return;
                        }
                        Player 当前玩家 = Bukkit.getPlayer(施法者.唯一标识());
                        if (!玩家状态有效(当前玩家, 中心.世界(), 位置适配器.转换(中心), 配置, true)) {
                            记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                                    "取消原因", "失败", "reason=charging_player_state_invalid");
                            取消玩家任务(玩家标识, "蓄力阶段玩家状态失效");
                            效果调度服务.移除(玩家标识, 奥能冥想蓄能效果标识);
                            完成();
                            return;
                        }
                        Location 当前位置 = 当前玩家.getLocation();
                        调试日志器.调试("奥能冥想", "蓄力完成：施法者=%s", 施法者.名称());
                        记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                                "蓄力结束", "成功", "effect_id=%s", 奥能冥想蓄能效果标识);
                        效果调度服务.移除(玩家标识, 奥能冥想蓄能效果标识);
                        long 冥想到期 = 计算效果到期时间(奥能冥想效果标识, 当前冥想持续);
                        效果调度服务.添加(玩家标识, new 效果实例(奥能冥想效果标识, "1_7", 玩家标识, (int) 秘能层数, 冥想到期));
                        记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                                "效果变化", "完成", "effect_id=%s stacks=%d expires_at=%d intelligence_granted=%d",
                                奥能冥想效果标识, (int) 秘能层数, 冥想到期, 施法者.等级() * (int) 秘能层数);
                        消息服务.发送技能日志(施法者, 事件标识, "奥能冥想.效果结束", String.format(数值格式, (double) 施法者.等级() * 秘能层数));
                        播放冥想完成特效(位置适配器.转换(当前位置), 配置, 玩家标识);
                        记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                                "任务启动", "成功", "task=meditation_complete_animation");
                        移除任务(玩家标识, 任务句柄[0]);
                        记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                                "释放后状态", "成功", "meditation_effect_active=true");
                        完成();
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
                释放异步租约(玩家标识, 异步租约);
                记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                        "任务停止", "成功", "task=charging reason=cast_finished");
            }
        };
        try {
            BukkitTask 任务 = 蓄力任务.runTaskLater(插件, 当前蓄能持续 / 50);
            任务句柄[0] = 任务;
            注册任务(玩家标识, 任务);
            注册异步租约(玩家标识, 异步租约);
            记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=charging task_id=%s", 任务 == null ? "null" : 任务.getTaskId());
            return 技能执行结果.成功;
        } catch (RuntimeException | Error 异常) {
            记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                    "取消原因", "异常", "reason=charging_task_start_failed exception=%s", 异常.getClass().getSimpleName());
            异步租约.释放();
            效果调度服务.移除(玩家标识, 奥能冥想蓄能效果标识);
            throw 异常;
        } finally {
            记录技能阶段("奥能冥想", 施法者, "1_7", 事件标识, 施法标识, null,
                    "最终状态", "结束", "charging_task_registered=true");
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_7", "生命恢复比例", "0.2", "生命恢复占上限比例");
        注册参数("1_7", "蓄能持续", "2000", "蓄能效果持续毫秒数");
        注册参数("1_7", "冥想持续", "30000", "冥想效果持续毫秒数");
        注册参数("1_7", "暴击倍率", "1.5", "暴击伤害倍率");
        注册参数("1_7_1", "蓄能持续", "2000", "蓄能效果持续毫秒数");
        注册参数("1_7_2", "冥想持续", "30000", "冥想效果持续毫秒数");
    }

    private boolean 玩家状态有效(Player 玩家, String 世界名称, Location 起点,
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

    private int 粒子数量(奥能法师大招特效配置 配置, int 基础数量) {
        return 配置.粒子数量(基础数量);
    }

    private Color 插值颜色(Color 起, Color 止, double t) {
        double tt = Math.max(0.0, Math.min(1.0, t));
        return Color.fromRGB(
                (int) Math.round(起.getRed() + (止.getRed() - 起.getRed()) * tt),
                (int) Math.round(起.getGreen() + (止.getGreen() - 起.getGreen()) * tt),
                (int) Math.round(起.getBlue() + (止.getBlue() - 起.getBlue()) * tt));
    }

    private Color 冥想渐变色(double 进度) {
        double p = Math.max(0.0, Math.min(1.0, 进度));
        if (p < 0.5) {
            return 插值颜色(冥想主颜色, 冥想蓝色, p / 0.5);
        }
        return 插值颜色(冥想蓝色, 冥想白色, (p - 0.5) / 0.5);
    }

    private void 播放冥想开始特效(位置 起点, 奥能法师大招特效配置 配置, UUID 玩家标识) {
        if (起点 == null) {
            return;
        }
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 冥想高度偏移, 0);

        // === 一次性初始爆发（精简：删除SOUL_FIRE_FLAME，仅保留核心紫白粒子） ===
        世界.spawnParticle(Particle.DUST, 施法位置, 粒子数量(配置, 8),
                1.0, 1.0, 1.0, 0.15,
                new Particle.DustOptions(冥想主颜色, 配置.粒子尺寸(2.8f)));
        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 施法位置, 粒子数量(配置, 3),
                0.9, 1.4, 0.9, 0.2,
                new Particle.DustTransition(冥想主颜色, 冥想白色, 配置.粒子尺寸(2.2f)));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 粒子数量(配置, 2), 0.8, 1.5, 0.8, 0.06);

        // === 精简音效（仅保留信标选择，删除BLOCK_BEACON_ACTIVATE避免音效混乱） ===
        世界.playSound(施法位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(1.0f), 0.7f);

        // === 持续动画：天空光柱 + 地面符文 + 多层旋转环 + 能量汇聚 + 双层旋转光柱 + 脉冲爆发 + 递进音效 ===
        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 动画任务 = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 配置.冥想开始持续刻()) {
                    cancel();
                    移除任务(玩家标识, 任务句柄[0]);
                    return;
                }
                Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (!玩家状态有效(玩家, 起点.世界(), 位置适配器.转换(起点), 配置, true)) {
                    cancel();
                    取消玩家任务(玩家标识, "开始动画玩家状态失效");
                    效果调度服务.移除(玩家标识, 奥能冥想蓄能效果标识);
                    return;
                }
                double 进度 = (double) tick / 配置.冥想开始持续刻();
                Color 渐变 = 冥想渐变色(进度);

                // 2. 地面符文 —— 八角星 + 第二环（精简：删除第三环与原符文外环，减少重复环；删除超过2格的天空光柱）
                int 顶点数 = 配置.环点数量(4);
                for (int i = 0; i < 顶点数; i++) {
                    double angleOuter = (2 * Math.PI * i / 顶点数) + tick * 0.12;
                    double angleInner = angleOuter + Math.PI / 顶点数;
                    double xOuter = 施法位置.getX() + Math.cos(angleOuter) * 符文外半径;
                    double zOuter = 施法位置.getZ() + Math.sin(angleOuter) * 符文外半径;
                    double xInner = 施法位置.getX() + Math.cos(angleInner) * 符文内半径;
                    double zInner = 施法位置.getZ() + Math.sin(angleInner) * 符文内半径;
                    Location 外点 = new Location(施法位置.getWorld(), xOuter, 施法位置.getY(), zOuter);
                    Location 内点 = new Location(施法位置.getWorld(), xInner, 施法位置.getY(), zInner);
                    世界.spawnParticle(Particle.DUST, 外点, 1, 0.02, 0.02, 0.02, 0.0,
                            new Particle.DustOptions(冥想核心颜色, 0.8f));
                    世界.spawnParticle(Particle.DUST, 内点, 1, 0.02, 0.02, 0.02, 0.0,
                            new Particle.DustOptions(冥想修饰颜色, 0.6f));
                }
                // 第二环（反向旋转，渐变色）
                int 第二环点数 = 配置.环点数量(5);
                for (int i = 0; i < 第二环点数; i++) {
                    double angle = 2 * Math.PI * i / 第二环点数 - tick * 0.15;
                    double x = 施法位置.getX() + Math.cos(angle) * 第二环半径;
                    double z = 施法位置.getZ() + Math.sin(angle) * 第二环半径;
                    Location 环点 = new Location(施法位置.getWorld(), x, 施法位置.getY(), z);
                    世界.spawnParticle(Particle.DUST, 环点, 1, 0.02, 0.02, 0.02, 0.0,
                            new Particle.DustOptions(渐变, 0.6f));
                }

                // 4. 能量汇聚 —— 粒子从周围流向中心（渐变拖尾）
                double 汇聚半径 = 3.5 - tick * 0.12;
                if (汇聚半径 < 0.5) {
                    汇聚半径 = 0.5;
                }
                int 流数 = 配置.环点数量(2);
                for (int i = 0; i < 流数; i++) {
                    double angle = 2 * Math.PI * i / 流数 + tick * 0.25;
                    double r = 汇聚半径;
                    double x = 施法位置.getX() + Math.cos(angle) * r;
                    double z = 施法位置.getZ() + Math.sin(angle) * r;
                    double y = 施法位置.getY() + 0.3 + Math.sin(tick * 0.4 + i) * 0.2;
                    Location 汇聚点 = new Location(施法位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 汇聚点, 1,
                            0.02, 0.02, 0.02, 0.0,
                            new Particle.DustTransition(渐变, 冥想白色, 0.7f));
                    double rInner = r * 0.7;
                    double xInner = 施法位置.getX() + Math.cos(angle) * rInner;
                    double zInner = 施法位置.getZ() + Math.sin(angle) * rInner;
                    Location 内汇聚点 = new Location(施法位置.getWorld(), xInner, y * 0.8 + 施法位置.getY() * 0.2, zInner);
                    世界.spawnParticle(Particle.DUST, 内汇聚点, 1, 0.02, 0.02, 0.02, 0.0,
                            new Particle.DustOptions(冥想核心颜色, 0.4f));
                }

                if (tick % 3 == 0) {
                    for (int i = 0; i < 1; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = 2.0 + Math.random() * 2.5;
                        double x = 施法位置.getX() + Math.cos(angle) * r;
                        double z = 施法位置.getZ() + Math.sin(angle) * r;
                        double y = 施法位置.getY() + Math.random() * 0.5;
                        Location 火点 = new Location(施法位置.getWorld(), x, y, z);
                        世界.spawnParticle(Particle.DUST, 火点, 1, 0.1, 0.1, 0.1, 0.01,
                                new Particle.DustOptions(冥想核心颜色, 0.7f));
                    }
                }

                // 5. 旋转光柱（钳制高度≤2格：仅单柱，并限制yOffset上限）
                for (int i = 0; i < 1; i++) {
                    double 角 = i * Math.PI * 2 / 3 + tick * 0.3;
                    double 半径 = 0.8 + Math.sin(tick * 0.5) * 0.2;
                    double x = 施法位置.getX() + Math.cos(角) * 半径;
                    double z = 施法位置.getZ() + Math.sin(角) * 半径;
                    for (double yOff = 0; yOff < 光柱高度; yOff += 2.0) {
                        Location 光柱点 = new Location(施法位置.getWorld(), x, 施法位置.getY() + yOff, z);
                        世界.spawnParticle(Particle.DUST, 光柱点, 1,
                                0.02, 0.02, 0.02, 0.0,
                                new Particle.DustOptions(渐变, 0.5f));
                    }
                }

                // 6. 持续脉冲爆发（每4刻一次扩散环，删除SONIC_BOOM爆炸特效）
                if (tick % 脉冲间隔 == 0) {
                    double 脉冲半径 = 0.6 + tick * 0.35;
                    int 脉冲点数 = 配置.环点数量(4);
                    for (int i = 0; i < 脉冲点数; i++) {
                        double angle = 2 * Math.PI * i / 脉冲点数;
                        double x = 施法位置.getX() + Math.cos(angle) * 脉冲半径;
                        double z = 施法位置.getZ() + Math.sin(angle) * 脉冲半径;
                        Location 脉冲点 = new Location(施法位置.getWorld(), x, 施法位置.getY() + 0.1, z);
                        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 脉冲点, 1,
                                0.05, 0.0, 0.05, 0.0,
                                new Particle.DustTransition(渐变, 冥想白色, 1.0f));
                    }
                }

                // 7. 递进音效（每5刻一次，音高随进度上升，精简为单一音效）
                if (tick % 5 == 0) {
                    float 音高 = (float) (0.5 + 进度 * 1.0);
                    世界.playSound(施法位置, Sound.BLOCK_BEACON_POWER_SELECT,
                            配置.音量(0.5f + (float) (进度 * 0.4)), 音高);
                }

                tick++;
            }
        };
        BukkitTask 任务 = 动画任务.runTaskTimer(插件, 0, 配置.冥想开始间隔刻());
        任务句柄[0] = 任务;
        注册任务(玩家标识, 任务);
    }

    private void 播放冥想完成特效(位置 起点, 奥能法师大招特效配置 配置, UUID 玩家标识) {
        if (起点 == null) {
            return;
        }
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 冥想高度偏移, 0);

        // === 一次性大爆发（精简 + 颜色渐变收束至白，DUST≥100保留） ===
        世界.spawnParticle(Particle.DUST, 施法位置, 粒子数量(配置, 14),
                1.2, 1.5, 1.2, 0.25,
                new Particle.DustOptions(冥想主颜色, 配置.粒子尺寸(2.8f)));
        世界.spawnParticle(Particle.DUST, 施法位置, 粒子数量(配置, 2),
                0.7, 1.2, 0.7, 0.3,
                new Particle.DustOptions(冥想核心颜色, 配置.粒子尺寸(1.6f)));
        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 施法位置, 粒子数量(配置, 5),
                1.0, 1.4, 1.0, 0.25,
                new Particle.DustTransition(冥想主颜色, 冥想白色, 配置.粒子尺寸(2.4f)));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 粒子数量(配置, 2), 1.0, 1.5, 1.0, 0.1);
        世界.spawnParticle(Particle.SOUL_FIRE_FLAME, 施法位置, 粒子数量(配置, 1), 0.8, 1.5, 0.8, 0.06);

        // === 精简音效（仅保留挑战完成+信标选择，避免音效混乱） ===
        世界.playSound(施法位置, Sound.UI_TOAST_CHALLENGE_COMPLETE, 配置.音量(1.0f), 1.0f);
        世界.playSound(施法位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(1.0f), 1.2f);

        // === 持续动画：五层冲击波 + 粒子漩涡 + 光柱爆发 + 脉冲 + 递进音效 ===
        BukkitTask[] 任务句柄 = new BukkitTask[1];
        BukkitRunnable 动画任务 = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 配置.冥想完成持续刻()) {
                    cancel();
                    移除任务(玩家标识, 任务句柄[0]);
                    return;
                }
                Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (!玩家状态有效(玩家, 起点.世界(), 位置适配器.转换(起点), 配置, true)) {
                    cancel();
                    取消玩家任务(玩家标识, "完成动画玩家状态失效");
                    return;
                }
                double 进度 = (double) tick / 配置.冥想完成持续刻();
                Color 渐变 = 冥想渐变色(进度);

                // 1. 五层同心冲击波扩散（加冲击波层数，颜色递进 紫→蓝→白→金→紫）
                for (int wave = 0; wave < 冲击波层数; wave++) {
                    double waveRadius = 0.5 + tick * 0.5 + wave * 1.6;
                    double waveOpacity = Math.max(0, 1.0 - tick / (double) 配置.冥想完成持续刻() - wave * 0.15);
                    if (waveOpacity <= 0) {
                        continue;
                    }
                    Color waveColor;
                    if (wave == 0) {
                        waveColor = 冥想渐变色(Math.min(1.0, 进度 + 0.2));
                    } else if (wave == 1) {
                        waveColor = 冥想蓝色;
                    } else if (wave == 2) {
                        waveColor = 冥想白色;
                    } else if (wave == 3) {
                        waveColor = 冥想金色;
                    } else {
                        waveColor = 冥想主颜色;
                    }
                    float waveSize = 0.6f + wave * 0.2f;
                    float 实际尺寸 = Math.max(0.01f, waveSize * (float) waveOpacity);
                    int 环点数 = 配置.环点数量(4);
                    for (int i = 0; i < 环点数; i++) {
                        double angle = 2 * Math.PI * i / 环点数;
                        double x = 施法位置.getX() + Math.cos(angle) * waveRadius;
                        double z = 施法位置.getZ() + Math.sin(angle) * waveRadius;
                        double y = 施法位置.getY() + wave * 0.1;
                        Location 波点 = new Location(施法位置.getWorld(), x, y, z);
                        世界.spawnParticle(Particle.DUST, 波点, 1,
                                0.03, 0.03, 0.03, 0.0,
                                new Particle.DustOptions(waveColor, 实际尺寸));
                    }
                }

                // 2. 粒子漩涡 —— 旋转上升（渐变色，删除SOUL_FIRE_FLAME减少粒子）
                int 漩涡粒子数 = 配置.环点数量(2);
                for (int i = 0; i < 漩涡粒子数; i++) {
                    double angle = 2 * Math.PI * i / 漩涡粒子数 + tick * 0.35;
                    double heightProgress = (tick + i * 0.3) % 配置.冥想完成持续刻() / (double) 配置.冥想完成持续刻();
                    double y = 施法位置.getY() + heightProgress * 0.5;
                    double r = 1.8 * (1.0 - heightProgress * 0.7);
                    double x = 施法位置.getX() + Math.cos(angle) * r;
                    double z = 施法位置.getZ() + Math.sin(angle) * r;
                    Location 涡点 = new Location(施法位置.getWorld(), x, y, z);
                    世界.spawnParticle(Particle.END_ROD, 涡点, 1, 0.05, 0.05, 0.05, 0.02);
                    世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 涡点, 1,
                            0.05, 0.05, 0.05, 0.0,
                            new Particle.DustTransition(渐变, 冥想白色, 0.45f));
                }

                // 3. 光柱爆发 —— 竖直光柱向外扩散（保留END_ROD，删除DUST_COLOR_TRANSITION减少粒子）
                if (tick < 14) {
                    double 光柱半径 = 0.3 + tick * 0.22;
                    for (double yOff = 0; yOff <= 天空光柱高度; yOff += 1.0) {
                        Location 光点 = 施法位置.clone().add(
                                Math.sin(yOff * 0.5 + tick) * 光柱半径,
                                yOff,
                                Math.cos(yOff * 0.5 + tick) * 光柱半径
                        );
                        世界.spawnParticle(Particle.END_ROD, 光点, 1, 0.05, 0.0, 0.05, 0.0);
                    }
                }

                // 4. 持续脉冲爆发（每3刻一次，中心向外冲击，删除SONIC_BOOM爆炸特效）
                if (tick % 3 == 0) {
                    double 脉冲半径 = 0.8 + tick * 0.4;
                    for (int i = 0; i < 3; i++) {
                        double angle = 2 * Math.PI * i / 3;
                        double x = 施法位置.getX() + Math.cos(angle) * 脉冲半径;
                        double z = 施法位置.getZ() + Math.sin(angle) * 脉冲半径;
                        Location 脉冲点 = new Location(施法位置.getWorld(), x, 施法位置.getY() + 0.1, z);
                        世界.spawnParticle(Particle.DUST_COLOR_TRANSITION, 脉冲点, 1,
                                0.05, 0.0, 0.05, 0.0,
                                new Particle.DustTransition(渐变, 冥想白色, 0.9f));
                    }
                }

                // 5. 递进音效（随动画进度音高上升）
                if (tick % 4 == 0) {
                    float 音高 = (float) (0.6 + 进度 * 0.8);
                    世界.playSound(施法位置, Sound.BLOCK_BEACON_POWER_SELECT, 配置.音量(0.4f), 音高);
                }

                tick++;
            }
        };
        BukkitTask 任务 = 动画任务.runTaskTimer(插件, 0, 配置.冥想完成间隔刻());
        任务句柄[0] = 任务;
        注册任务(玩家标识, 任务);
    }

    public void 取消玩家任务(UUID 玩家标识, String 原因) {
        if (玩家标识 == null) {
            return;
        }
        Set<事件日志上下文.异步事件租约> 租约集合 = 异步租约表.remove(玩家标识);
        if (租约集合 != null) {
            租约集合.forEach(事件日志上下文.异步事件租约::释放);
        }
        任务组.取消(玩家标识, "奥能冥想", 原因);
        活跃玩家.remove(玩家标识);
        调试日志器.调试("奥能冥想", "生命周期结束：玩家=%s 原因=%s", 玩家标识, 原因);
    }

    public void 取消全部任务(String 原因) {
        Set.copyOf(活跃玩家).forEach(玩家标识 -> 取消玩家任务(玩家标识, 原因));
    }

    private void 注册任务(UUID 玩家标识, BukkitTask 任务) {
        活跃玩家.add(玩家标识);
        任务组.注册(玩家标识, 任务, "奥能冥想");
    }

    private void 移除任务(UUID 玩家标识, BukkitTask 任务) {
        任务组.移除(玩家标识, 任务, "奥能冥想");
        if (任务组.获取数量(玩家标识) == 0) {
            活跃玩家.remove(玩家标识);
            调试日志器.调试("奥能冥想", "生命周期结束：玩家=%s 原因=任务自然完成", 玩家标识);
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
