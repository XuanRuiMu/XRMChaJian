package mljy.技能实现.奥能法师;

import mljy.技能定义;
import mljy.技能执行器;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.实体;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.伤害结果;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@技能定义(
        技能标识 = "1_4",
        名称翻译键 = "skill.奥能法师.秘法陨落.name",
        施法类型 = 施法类型.蓄力,
        蓄力时间 = 1.5,
        冷却时间 = 6.0,
        公共冷却 = 1.5,
        射程 = 6.0
)
public class 秘法陨落 extends 奥能法师技能基础 implements 技能执行器 {
    private static final double 智力系数 = 0.09;
    private static final int 箭矢数量 = 12;
    private static final double 水平距离 = 6.0;
    private static final double 垂直高度 = 4.0;
    private static final double 半径 = 4.0;
    private static final double 秘能产生概率 = 0.3;
    private static final double 释放高度偏移 = 1.5;
    private static final Color 陨落主颜色 = Color.fromRGB(180, 80, 255);
    private static final Color 陨落浅色 = Color.fromRGB(200, 150, 255);
    private static final Color 陨落深色 = Color.fromRGB(80, 0, 120);
    private static final Color 陨落核心颜色 = Color.fromRGB(255, 200, 255);
    private static final Color 紫色气泡颜色 = Color.fromRGB(160, 80, 255);
    private static final int 气泡环点数 = 8;
    private static final double 气泡环半径 = 0.4;
    private static final long 发射间隔刻 = 2L;
    private static final long 最大生命周期刻 = 80L;
    private static final long 全部消失延迟刻 = 10L;
    private static final double 扫掠查询半径 = 1.5;
    private static final double 扫掠命中半径 = 1.0;

    @Override
    public 技能执行结果 执行(技能上下文 上下文) {
        当前参数读取器 = 上下文 == null ? null : 上下文.获取参数覆盖();
        UUID 根事件标识 = 获取调试根事件标识();
        UUID 施法标识 = 创建施法标识();
        玩家快照 请求施法者 = 上下文 == null ? null : 上下文.施法者();
        final double 当前水平距离 = 获取参数("水平距离", 水平距离);
        final double 当前垂直高度 = 获取参数("垂直高度", 垂直高度);
        final int 当前箭矢数量 = 获取参数("箭矢数量", 箭矢数量);
        final double 当前半径 = 获取参数("半径", 半径);
        记录技能阶段("秘法陨落", 请求施法者, "1_4", 根事件标识, 施法标识, null,
                "释放请求", "进入", "charge=蓄力 projectile=箭矢 horizontal_distance=%.1f vertical_height=%.1f",
                当前水平距离, 当前垂直高度);
        if (上下文 == null || 上下文.施法者() == null) {
            调试日志器.调试("秘法陨落", "请求失败 root_event_id=null cast_id=null projectile_uuid=null target_uuid=null 原因=上下文或施法者为空");
            记录技能阶段("秘法陨落", null, "1_4", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=missing_context_or_caster");
            return 技能执行结果.上下文为空;
        }
        玩家快照 施法者 = 上下文.施法者();
        调试日志器.调试("秘法陨落", "请求 root_event_id=%s cast_id=%s projectile_uuid=null target_uuid=null 施法者=%s",
                根事件标识, 施法标识, 施法者.名称());
        位置 玩家位置 = 获取实时位置(施法者);
        if (玩家位置 == null) {
            记录技能阶段("秘法陨落", 施法者, "1_4", 根事件标识, 施法标识, null,
                    "前置状态", "失败", "reason=位置无效");
            return 技能执行结果.位置无效;
        }
        记录技能阶段("秘法陨落", 施法者, "1_4", 根事件标识, 施法标识, null,
                "前置状态", "通过", "服务校验=通过");

        位置 中心 = 计算中心(玩家位置);
        播放释放特效(中心);
        double 起始角度 = 计算圆环起始角度(玩家位置, 中心);
        UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
        boolean 创建事件 = 当前事件标识 == null;
        UUID 事件标识 = 创建事件 ? 事件日志上下文.开始新事件() : 当前事件标识;
        try {
            记录范围内实体诊断(中心, 施法者);
            new 施法状态(施法者, 玩家位置.世界(), 生成箭矢点位(中心), 事件标识, 施法标识, 起始角度, 中心).启动();
            记录技能阶段("秘法陨落", 施法者, "1_4", 事件标识, 施法标识, null,
                    "任务启动", "成功", "task=延迟施法 arrows=%d", 当前箭矢数量);
            记录技能阶段("秘法陨落", 施法者, "1_4", 事件标识, 施法标识, null,
                    "释放后状态", "成功", "arrows=%d radius=%.1f", 当前箭矢数量, 当前半径);
            return 技能执行结果.成功;
        } finally {
            记录技能阶段("秘法陨落", 施法者, "1_4", 事件标识, 施法标识, null,
                    "最终状态", "结束", "task=延迟施法");
            if (创建事件) {
                事件日志上下文.结束事件(事件标识);
                事件日志上下文.清除();
            }
        }
    }

    @Override
    public void 声明参数() {
        注册参数("1_4", "智力系数", "0.09", "箭矢命中智力伤害系数");
        注册参数("1_4", "箭矢数量", "12", "发射箭矢总数");
        注册参数("1_4", "水平距离", "6.0", "落点中心水平距离");
        注册参数("1_4", "垂直高度", "4.0", "落点中心垂直高度");
        注册参数("1_4", "半径", "4.0", "落点散布半径");
        注册参数("1_4", "秘能产生概率", "0.3", "命中产生秘能概率");
        注册参数("1_4", "暴击倍率", "1.5", "暴击伤害倍率");
    }

    /**
     * 取施法者实时快照：用于「触发伤害那一刻」按玩家当时属性计算伤害/暴击/暴伤，
     * 使箭矢下落期间属性变化（如奥能冥想临时智力生效/失效）反映到后续箭矢结算。
     * 仅在实时快照不可得时回退到施法时缓存快照，避免凭空创建属性。
     */
    protected 玩家快照 取实时施法者(玩家快照 缓存施法者) {
        if (缓存施法者 == null || 缓存施法者.唯一标识() == null) {
            return 缓存施法者;
        }
        return 玩家服务.获取快照(缓存施法者.唯一标识()).orElse(缓存施法者);
    }

    private 位置 计算中心(位置 玩家位置) {
        double 当前水平距离 = 获取参数("水平距离", 水平距离);
        double 当前垂直高度 = 获取参数("垂直高度", 垂直高度);
        位置 前方位置 = 获取前方位置(玩家位置, 当前水平距离);
        if (Double.isNaN(前方位置.x()) || Double.isNaN(前方位置.z())) {
            前方位置 = 获取随机方向位置(玩家位置, 当前水平距离);
        }
        位置 中心 = 上方位置(前方位置, 当前垂直高度);
        调试日志器.调试("秘法陨落", "计算中心 root_event_id=%s 玩家位置=(%.1f,%.1f,%.1f) 偏航角=%.2f 中心位置=(%.1f,%.1f,%.1f) 水平距离=%.1f 垂直高度=%.1f",
                事件日志上下文.获取当前事件标识或空(),
                玩家位置.x(), 玩家位置.y(), 玩家位置.z(), 玩家位置.偏航角(),
                中心.x(), 中心.y(), 中心.z(), 当前水平距离, 当前垂直高度);
        return 中心;
    }

    private void 记录范围内实体诊断(位置 中心, 玩家快照 施法者) {
        World 世界 = Bukkit.getWorld(中心.世界());
        if (世界 == null) {
            调试日志器.调试("秘法陨落", "范围内实体诊断：世界为空 root_event_id=%s 中心世界=%s",
                    事件日志上下文.获取当前事件标识或空(), 中心.世界());
            return;
        }
        Location 中心位置 = 位置适配器.转换(中心);
        double 当前半径 = 获取参数("半径", 半径);
        double 当前垂直高度 = 获取参数("垂直高度", 垂直高度);
        double 搜索半径x = 当前半径 + 1.0;
        double 搜索半径y = 当前垂直高度 + 2.0;
        double 搜索半径z = 当前半径 + 1.0;
        Collection<Entity> 实体集合 = null;
        try {
            实体集合 = 世界.getNearbyEntities(中心位置, 搜索半径x, 搜索半径y, 搜索半径z);
        } catch (RuntimeException 异常) {
            调试日志器.调试("秘法陨落", "范围内实体诊断异常 root_event_id=%s 异常=%s",
                    事件日志上下文.获取当前事件标识或空(), 异常.getClass().getSimpleName());
            return;
        }
        if (实体集合 == null) {
            调试日志器.调试("秘法陨落", "范围内实体诊断：getNearbyEntities返回null root_event_id=%s",
                    事件日志上下文.获取当前事件标识或空());
            return;
        }
        UUID 施法者标识 = 施法者 == null ? null : 施法者.唯一标识();
        int 怪物计数 = 0;
        for (Entity 实体 : 实体集合) {
            if (!(实体 instanceof LivingEntity)) {
                continue;
            }
            if (施法者标识 != null && 施法者标识.equals(实体.getUniqueId())) {
                continue;
            }
            怪物计数++;
            LivingEntity 生物 = (LivingEntity) 实体;
            Location 实体位置 = null;
            BoundingBox 碰撞箱 = null;
            try {
                实体位置 = 生物.getLocation();
                碰撞箱 = 生物.getBoundingBox();
            } catch (RuntimeException 异常) {
                调试日志器.调试("秘法陨落", "范围内怪物[%d]信息读取异常 uuid=%s 异常=%s",
                        怪物计数, 实体.getUniqueId(), 异常.getClass().getSimpleName());
                continue;
            }
            String 位置描述 = 实体位置 == null ? "null"
                    : String.format("(%.2f,%.2f,%.2f)", 实体位置.getX(), 实体位置.getY(), 实体位置.getZ());
            String 碰撞箱描述 = 碰撞箱 == null ? "null"
                    : String.format("min=(%.2f,%.2f,%.2f) max=(%.2f,%.2f,%.2f)",
                            碰撞箱.getMinX(), 碰撞箱.getMinY(), 碰撞箱.getMinZ(),
                            碰撞箱.getMaxX(), 碰撞箱.getMaxY(), 碰撞箱.getMaxZ());
            调试日志器.调试("秘法陨落", "范围内怪物[%d] uuid=%s 类型=%s 坐标=%s 碰撞箱%s",
                    怪物计数, 实体.getUniqueId(), 实体.getType(), 位置描述, 碰撞箱描述);
        }
        调试日志器.调试("秘法陨落", "范围内实体诊断完成 root_event_id=%s 怪物数=%d 中心=(%.2f,%.2f,%.2f) 搜索范围=x±%.1f y±%.1f z±%.1f 半径=%.1f",
                事件日志上下文.获取当前事件标识或空(), 怪物计数,
                中心.x(), 中心.y(), 中心.z(), 搜索半径x, 搜索半径y, 搜索半径z, 当前半径);
    }

    private List<位置> 生成箭矢点位(位置 中心) {
        List<位置> 点位列表 = new ArrayList<>();
        int 当前箭矢数量 = 获取参数("箭矢数量", 箭矢数量);
        double 当前半径 = 获取参数("半径", 半径);
        for (int i = 0; i < 当前箭矢数量; i++) {
            double 偏移角 = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            // 环形随机半径：0.3~1.0 倍半径 ⇒ 1.2~4.0 格（对应文档「半径1.2~4.0格的环形范围」）
            double 偏移距离 = ThreadLocalRandom.current().nextDouble(0.3, 1.0) * 当前半径;
            double 点位x = 中心.x() + Math.cos(偏移角) * 偏移距离;
            double 点位z = 中心.z() + Math.sin(偏移角) * 偏移距离;
            点位列表.add(new 位置(
                    中心.世界(),
                    点位x,
                    中心.y(),
                    点位z,
                    中心.偏航角(),
                    中心.俯仰角()));
            调试日志器.调试("秘法陨落", "箭矢点位[%d] root_event_id=%s 坐标=(%.2f,%.2f,%.2f) 偏移角=%.4f 偏移距离=%.2f",
                    i + 1, 事件日志上下文.获取当前事件标识或空(),
                    点位x, 中心.y(), 点位z, 偏移角, 偏移距离);
        }
        调试日志器.调试("秘法陨落", "箭矢点位生成完成 root_event_id=%s 数量=%d 中心=(%.2f,%.2f,%.2f) 半径=%.1f",
                事件日志上下文.获取当前事件标识或空(), 当前箭矢数量, 中心.x(), 中心.y(), 中心.z(), 当前半径);
        return 点位列表;
    }

    private final class 施法状态 implements Listener {
        private final 玩家快照 施法者;
        private final String 施法世界;
        private final List<位置> 箭矢目标点位;
        private final UUID 事件标识;
        private final UUID 施法标识;
        private final Map<UUID, 箭矢记录> 箭矢列表 = new HashMap<>();
        private final double 起始角度;
        private final 位置 圆环中心位置;
        private int 已发射;
        private int 已绘制圆环进度;
        private int 触地箭矢计数;
        private long 已运行刻数;
        private long 所有箭矢触地时刻 = -1L;
        private BukkitTask 任务;
        private boolean 已结束;
        private boolean 异步租约已持有;

        private 施法状态(玩家快照 施法者, String 施法世界, List<位置> 箭矢目标点位, UUID 事件标识, UUID 施法标识,
                         double 起始角度, 位置 圆环中心位置) {
            this.施法者 = 施法者;
            this.施法世界 = 施法世界;
            this.箭矢目标点位 = 箭矢目标点位;
            this.事件标识 = 事件标识;
            this.施法标识 = 施法标识;
            this.起始角度 = 起始角度;
            this.圆环中心位置 = 圆环中心位置;
        }

        private void 启动() {
            异步租约已持有 = 事件日志上下文.持有异步子事件(事件标识);
            调试日志器.调试("秘法陨落", "启动延迟施法：施法者=%s 事件=%s 异步租约=%s",
                    施法者.名称(), 事件标识, 异步租约已持有);
            PluginManager 插件管理器 = Bukkit.getPluginManager();
            if (插件管理器 != null) {
                插件管理器.registerEvents(this, 插件);
            }
            try {
                任务 = Bukkit.getScheduler().runTaskTimer(
                        插件, () -> 事件日志上下文.在事件中执行(事件标识, this::运行一刻), 0L, 1L);
                if (任务 == null) {
                    调试日志器.调试("秘法陨落", "启动延迟施法失败：调度器返回空任务，施法者=%s 事件=%s",
                            施法者.名称(), 事件标识);
                    清理(false);
                }
            } catch (RuntimeException 异常) {
                调试日志器.调试("秘法陨落", "启动延迟施法失败：施法者=%s 事件=%s 异常=%s",
                        施法者.名称(), 事件标识, 异常.getClass().getSimpleName());
                清理(false);
            }
        }

        private void 运行一刻() {
            if (已结束) {
                return;
            }
            Player 玩家 = Bukkit.getPlayer(施法者.唯一标识());
            if (玩家 == null || !施法世界.equals(玩家.getWorld().getName())) {
                清理(false);
                return;
            }
            if (已运行刻数 % 发射间隔刻 == 0 && 已发射 < 箭矢目标点位.size()) {
                发射箭矢(施法者, 箭矢目标点位.get(已发射), this);
                已发射++;
                绘制圆环批次();
            }

            for (java.util.Iterator<Map.Entry<UUID, 箭矢记录>> it = 箭矢列表.entrySet().iterator(); it.hasNext();) {
                Map.Entry<UUID, 箭矢记录> entry = it.next();
                箭矢记录 记录 = entry.getValue();
                Arrow 箭矢 = 记录.箭矢();
                if (!箭矢.isValid() || 箭矢.isDead()) {
                    调试日志器.调试("秘法陨落", "箭矢移除（无效或死亡）root_event_id=%s cast_id=%s projectile_uuid=%s tick=%d isValid=%b isDead=%b",
                            事件标识, 施法标识, 箭矢.getUniqueId(), 已运行刻数, 箭矢.isValid(), 箭矢.isDead());
                    it.remove();
                    continue;
                }
                Location 箭矢位置 = 安全获取箭矢位置(箭矢);
                if (已运行刻数 % 5 == 0 && 箭矢位置 != null) {
                    调试日志器.调试("秘法陨落", "箭矢飞行轨迹 root_event_id=%s cast_id=%s projectile_uuid=%s tick=%d 坐标=(%.2f,%.2f,%.2f) 触地=%b",
                            事件标识, 施法标识, 箭矢.getUniqueId(), 已运行刻数,
                            箭矢位置.getX(), 箭矢位置.getY(), 箭矢位置.getZ(),
                            箭矢.isInBlock() || 箭矢.isOnGround() || 记录.待处理落点());
                }
                boolean 触地 = 箭矢.isInBlock() || 箭矢.isOnGround() || 记录.待处理落点();
                if (触地 && 记录.触地时刻() < 0) {
                    调试日志器.调试("秘法陨落", "箭矢触地停止 root_event_id=%s cast_id=%s projectile_uuid=%s tick=%d 坐标=%s 原因=isInBlock=%b isOnGround=%b 待处理落点=%b",
                            事件标识, 施法标识, 箭矢.getUniqueId(), 已运行刻数,
                            箭矢位置 == null ? "null" : String.format("(%.2f,%.2f,%.2f)", 箭矢位置.getX(), 箭矢位置.getY(), 箭矢位置.getZ()),
                            箭矢.isInBlock(), 箭矢.isOnGround(), 记录.待处理落点());
                    try {
                        箭矢.setVelocity(new Vector(0, 0, 0));
                        箭矢.setGravity(false);
                    } catch (RuntimeException | Error 异常) {
                        调试日志器.调试("秘法陨落", "箭矢触地停止异常 root_event_id=%s cast_id=%s projectile_uuid=%s 异常=%s",
                                事件标识, 施法标识, 箭矢.getUniqueId(), 异常.getClass().getSimpleName());
                    }
                    记录 = new 箭矢记录(箭矢, 已运行刻数, 记录.待处理落点(), 记录.已命中集());
                    entry.setValue(记录);
                    触地箭矢计数++;
                    if (触地箭矢计数 == 箭矢目标点位.size() && 所有箭矢触地时刻 < 0) {
                        所有箭矢触地时刻 = 已运行刻数;
                    }
                }
                // 飞行全程扫掠判定——每支未触地箭矢对当前位置1格内敌方单位结算伤害
                // 去重复用「每支箭矢自己的已命中集合」（每箭仅命中同一单位一次），箭矢之间互相独立
                if (箭矢位置 != null && !触地) {
                    World 箭矢世界 = 箭矢.getWorld();
                    if (箭矢世界 != null) {
                        Collection<Entity> 邻近实体集合;
                        try {
                            邻近实体集合 = 箭矢世界.getNearbyEntities(箭矢位置, 扫掠查询半径, 扫掠查询半径, 扫掠查询半径);
                        } catch (RuntimeException 异常) {
                            调试日志器.调试("秘法陨落", "扫掠查询异常 root_event_id=%s cast_id=%s projectile_uuid=%s 异常=%s",
                                    事件标识, 施法标识, 箭矢.getUniqueId(), 异常.getClass().getSimpleName());
                            邻近实体集合 = null;
                        }
                        if (邻近实体集合 != null) {
                            for (Entity 邻近 : 邻近实体集合) {
                                if (!(邻近 instanceof LivingEntity 邻近生物)) {
                                    continue;
                                }
                                if (邻近生物.getUniqueId().equals(施法者.唯一标识())) {
                                    continue;
                                }
                                BoundingBox 邻近碰撞箱;
                                try {
                                    邻近碰撞箱 = 邻近生物.getBoundingBox();
                                } catch (RuntimeException 异常) {
                                    调试日志器.调试("秘法陨落", "扫掠碰撞箱读取异常 root_event_id=%s cast_id=%s projectile_uuid=%s 异常=%s",
                                            事件标识, 施法标识, 箭矢.getUniqueId(), 异常.getClass().getSimpleName());
                                    continue;
                                }
                                if (邻近碰撞箱 == null || !邻近碰撞箱.expand(扫掠命中半径).contains(箭矢位置.toVector())) {
                                    continue;
                                }
                                处理箭矢命中目标(邻近生物, 箭矢.getUniqueId());
                            }
                        }
                        调试日志器.调试("秘法陨落", "扫掠判定 root_event_id=%s cast_id=%s projectile_uuid=%s 坐标=(%.2f,%.2f,%.2f) 扫掠半径=%.1f 邻近实体数=%d",
                                事件标识, 施法标识, 箭矢.getUniqueId(),
                                箭矢位置.getX(), 箭矢位置.getY(), 箭矢位置.getZ(),
                                扫掠命中半径,
                                邻近实体集合 == null ? 0 : 邻近实体集合.size());
                    }
                }
                if (箭矢位置 != null) {
                    播放箭矢下落气泡特效(箭矢.getWorld(), 箭矢位置);
                }
            }

            已运行刻数++;
            if (已运行刻数 >= 最大生命周期刻) {
                清理(true);
            } else if (所有箭矢触地时刻 >= 0 && 已运行刻数 - 所有箭矢触地时刻 >= 全部消失延迟刻) {
                清理(true);
            }
        }

        private void 绘制圆环批次() {
            绘制圆环批次(false);
        }

        private void 绘制圆环批次(boolean 强制绘制全部) {
            int 当前箭矢数量 = 获取参数("箭矢数量", 箭矢数量);
            if (已绘制圆环进度 >= 当前箭矢数量) {
                return;
            }
            World 世界 = Bukkit.getWorld(圆环中心位置.世界());
            if (世界 == null) {
                return;
            }
            Location 预警中心 = 位置适配器.转换(圆环中心位置);
            int 本批进度数 = 强制绘制全部 ? 当前箭矢数量 - 已绘制圆环进度 : 1;
            // 半圆圆弧180°，左右两线从近点同步推进到远点
            // 12进度点对应12支箭矢，每进度绘制左线(逆时针)+右线(顺时针)各1个位置
            // 进度0=起点(近点，浅色)，进度11=终点(远点，深色)
            for (int i = 0; i < 本批进度数; i++) {
                int 进度 = 已绘制圆环进度;
                double 进度比例 = 当前箭矢数量 <= 1 ? 0.0 : (double) 进度 / (当前箭矢数量 - 1);
                double 左线角度 = 起始角度 + 进度比例 * Math.PI;
                double 右线角度 = 起始角度 - 进度比例 * Math.PI;
                绘制圆环点(世界, 预警中心, 左线角度, 进度比例);
                绘制圆环点(世界, 预警中心, 右线角度, 进度比例);
                已绘制圆环进度++;
            }
        }

        @EventHandler
        public void 箭矢命中(ProjectileHitEvent 事件) {
            调试日志器.调试("秘法陨落", "ProjectileHitEvent触发 root_event_id=%s cast_id=%s event_entity=%s hit_entity=%s hit_block=%s",
                    事件标识, 施法标识,
                    事件.getEntity() == null ? "null" : 事件.getEntity().getType(),
                    事件.getHitEntity() == null ? "null" : 事件.getHitEntity().getType(),
                    事件.getHitBlock());
            事件日志上下文.在事件中执行(事件标识, () -> 处理箭矢命中(事件));
        }

        private void 处理箭矢命中(ProjectileHitEvent 事件) {
            if (!(事件.getEntity() instanceof Arrow 箭矢)) {
                return;
            }
            标记待处理落点(箭矢);
            org.bukkit.entity.Entity 命中实体 = 事件.getHitEntity();
            调试日志器.调试("秘法陨落", "箭矢命中 root_event_id=%s cast_id=%s projectile_uuid=%s hit_entity=%s hit_block=%s",
                    事件标识, 施法标识, 箭矢.getUniqueId(),
                    命中实体 == null ? "null" : 命中实体.getType(),
                    事件.getHitBlock());
            if (命中实体 != null) {
                try {
                    Location 命中坐标 = 命中实体.getLocation();
                    BoundingBox 命中碰撞箱 = null;
                    try {
                        命中碰撞箱 = 命中实体.getBoundingBox();
                    } catch (RuntimeException ignored) {
                    }
                    调试日志器.调试("秘法陨落", "命中实体详情 root_event_id=%s cast_id=%s projectile_uuid=%s target_uuid=%s 类型=%s 坐标=%s 碰撞箱=%s",
                            事件标识, 施法标识, 箭矢.getUniqueId(),
                            命中实体.getUniqueId(), 命中实体.getType(),
                            命中坐标 == null ? "null" : String.format("(%.2f,%.2f,%.2f)", 命中坐标.getX(), 命中坐标.getY(), 命中坐标.getZ()),
                            命中碰撞箱 == null ? "null" : String.format("min=(%.2f,%.2f,%.2f) max=(%.2f,%.2f,%.2f)",
                                    命中碰撞箱.getMinX(), 命中碰撞箱.getMinY(), 命中碰撞箱.getMinZ(),
                                    命中碰撞箱.getMaxX(), 命中碰撞箱.getMaxY(), 命中碰撞箱.getMaxZ()));
                } catch (RuntimeException 异常) {
                    调试日志器.调试("秘法陨落", "命中实体详情读取异常 root_event_id=%s projectile_uuid=%s 异常=%s",
                            事件标识, 箭矢.getUniqueId(), 异常.getClass().getSimpleName());
                }
            }
            // 每根箭矢触碰到敌方单位造成1次伤害，已命中目标不反复造成伤害（按每箭自己的集合去重）
            if (命中实体 instanceof LivingEntity 生物实体
                    && !(命中实体 instanceof Player 命中玩家 && 命中玩家.getUniqueId().equals(施法者.唯一标识()))) {
                处理箭矢命中目标(生物实体, 箭矢.getUniqueId());
            } else {
                String 跳过原因 = 命中实体 == null ? "未命中实体"
                        : (!(命中实体 instanceof LivingEntity) ? "命中非生物实体" : "命中施法者自身");
                调试日志器.调试("秘法陨落", "箭矢命中不造成伤害 root_event_id=%s cast_id=%s projectile_uuid=%s 原因=%s",
                        事件标识, 施法标识, 箭矢.getUniqueId(), 跳过原因);
            }
        }

        /**
         * 单支箭矢命中结算：
         * 1) 按「触发伤害那一刻」的玩家实时属性计算伤害值（0.09×当时总智力值）；
         *    暴击与暴伤亦由战斗服务按实时施法者属性独立判定（每箭独立 roll，互不影响）。
         * 2) 每次结算独立按 30% 概率通过通用秘能入口产生 1 点秘能（受上限4约束，触发次数不设限）；
         *    秘兆的判定与「获得了秘兆效果」日志由通用链路（应用单体伤害并记录命中）自动完成，
         *    本方法不出现任何秘兆逻辑；秘法陨落消耗 0 秘能，传 消耗秘兆=false。
         * 3) 去重按「每支箭矢自己的已命中集合」：同一箭矢不重复伤害同单位，箭矢之间互相独立。
         */
        private void 处理箭矢命中目标(LivingEntity 命中生物, UUID 箭矢标识) {
            UUID 目标标识 = 命中生物.getUniqueId();
            箭矢记录 记录 = 箭矢列表.get(箭矢标识);
            if (记录 != null && 记录.已命中(目标标识)) {
                调试日志器.调试("秘法陨落", "箭矢命中已命中目标跳过 root_event_id=%s cast_id=%s projectile_uuid=%s target_uuid=%s",
                        事件标识, 施法标识, 箭矢标识, 目标标识);
                return;
            }
            实体 目标 = new 实体适配器(命中生物);
            玩家快照 实时施法者 = 取实时施法者(施法者);
            double 当前智力系数 = 获取参数("智力系数", 智力系数);
            double 基础伤害 = 实时施法者.属性().智力() * 当前智力系数;
            double 当前秘能产生概率 = 获取参数("秘能产生概率", 秘能产生概率);
            int 秘能量 = 概率触发(当前秘能产生概率) ? 1 : 0;
            UUID 当前事件标识 = 事件日志上下文.获取当前事件标识或空();
            记录技能阶段("秘法陨落", 实时施法者, "1_4", 当前事件标识, null, 目标标识,
                    "伤害结算", "开始", "projectile_uuid=%s base_damage=%.2f target_uuid=%s 实时智力=%.1f 秘能量=%d",
                    箭矢标识, 基础伤害, 目标标识, 实时施法者.属性().智力(), 秘能量);
            伤害结果 结果 = 应用单体伤害并记录命中(实时施法者, 目标, 基础伤害, false,
                    "skill.奥能法师.秘法陨落.name", 秘能量, false);
            if (记录 != null) {
                记录.记录命中(目标标识);
            }
            调试日志器.调试("秘法陨落", "箭矢命中造成伤害 projectile_uuid=%s target_uuid=%s 实际伤害=%s 是否暴击=%s 秘能量=%d 消耗秘兆=false",
                    箭矢标识, 目标标识, 结果 == null ? "null" : String.format("%.1f", 结果.最终数值()),
                    结果 != null && 结果.是否暴击(), 秘能量);
            记录技能阶段("秘法陨落", 实时施法者, "1_4", 当前事件标识, null, 目标标识,
                    "伤害结算", "完成", "projectile_uuid=%s target_uuid=%s base_damage=%.2f",
                    箭矢标识, 目标标识, 基础伤害);
        }

        private void 标记待处理落点(Arrow 箭矢) {
            箭矢记录 旧记录 = 箭矢列表.get(箭矢.getUniqueId());
            if (旧记录 == null || 旧记录.待处理落点()) {
                return;
            }
            箭矢列表.put(箭矢.getUniqueId(), new 箭矢记录(旧记录.箭矢(), 旧记录.触地时刻(), true, 旧记录.已命中集()));
        }

        @EventHandler
        public void 玩家退出(PlayerQuitEvent 事件) {
            if (施法者.唯一标识().equals(事件.getPlayer().getUniqueId())) {
                清理(false);
            }
        }

        @EventHandler
        public void 玩家换世界(PlayerChangedWorldEvent 事件) {
            if (施法者.唯一标识().equals(事件.getPlayer().getUniqueId())) {
                清理(false);
            }
        }

        private void 清理(boolean 发送汇总) {
            if (已结束) {
                return;
            }
            已结束 = true;
            // 兜底：清理时强制绘制剩余圆环进度点，避免箭矢发射未完成时圆弧不完整
            绘制圆环批次(true);
            记录技能阶段("秘法陨落", 施法者, "1_4", 事件标识, 施法标识, null,
                    "任务停止", "完成", "task=延迟施法 send_summary=%s arrows_fired=%d", 发送汇总, 已发射);
            if (任务 != null) {
                任务.cancel();
            }
            for (箭矢记录 记录 : 箭矢列表.values()) {
                if (记录.箭矢().isValid()) {
                    记录.箭矢().remove();
                }
            }
            箭矢列表.clear();
            HandlerList.unregisterAll(this);
            try {
                if (异步租约已持有) {
                    异步租约已持有 = false;
                    事件日志上下文.释放异步子事件(事件标识);
                    调试日志器.调试("秘法陨落", "释放延迟施法事件租约：施法者=%s 事件=%s",
                            施法者.名称(), 事件标识);
                }
            } finally {
                // 清理结束
            }
        }
    }

    private record 箭矢记录(Arrow 箭矢, long 触地时刻, boolean 待处理落点, Set<UUID> 已命中集) {
        boolean 已命中(UUID 目标标识) {
            return 目标标识 != null && 已命中集.contains(目标标识);
        }

        void 记录命中(UUID 目标标识) {
            if (目标标识 != null) {
                已命中集.add(目标标识);
            }
        }
    }

    private void 发射箭矢(玩家快照 施法者, 位置 目标点, 施法状态 状态) {
        World 世界 = Bukkit.getWorld(目标点.世界());
        if (世界 == null) {
            return;
        }
        Location 释放位置 = 位置适配器.转换(目标点).clone();
        Arrow 箭矢 = 世界.spawnArrow(释放位置, new Vector(0, -1, 0), 3.0f, 0.0f);
        if (箭矢 == null) {
            调试日志器.调试("秘法陨落", "Arrow生成失败 root_event_id=%s cast_id=%s projectile_uuid=null target_uuid=null",
                    状态.事件标识, 状态.施法标识);
            return;
        }
        箭矢.setGravity(true);
        箭矢.setDamage(0.0);
        箭矢.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        箭矢.setCritical(false);
        箭矢.setPierceLevel((byte) 127);
        状态.箭矢列表.putIfAbsent(箭矢.getUniqueId(), new 箭矢记录(箭矢, -1L, false, new HashSet<>()));
        调试日志器.调试("秘法陨落", "Arrow生成 root_event_id=%s cast_id=%s projectile_uuid=%s target_uuid=null 原版伤害=0 位置=%s",
                状态.事件标识, 状态.施法标识, 箭矢.getUniqueId(), 箭矢.getLocation());
        调试日志器.调试("秘法陨落", "箭矢预测轨迹 root_event_id=%s cast_id=%s projectile_uuid=%s 起点=(%.2f,%.2f,%.2f) 方向=(0,-1,0) 预测落点y=%.2f",
                状态.事件标识, 状态.施法标识, 箭矢.getUniqueId(),
                释放位置.getX(), 释放位置.getY(), 释放位置.getZ(),
                释放位置.getY() - 获取参数("垂直高度", 垂直高度));
        世界.playSound(释放位置, Sound.ITEM_CROSSBOW_SHOOT, 0.5f, 1.6f);
        调试日志器.调试("秘法陨落", "箭矢发射：施法者=%s 第%d支", 施法者.名称(), 状态.已发射 + 1);
    }

    private Location 安全获取箭矢位置(Arrow 箭矢) {
        try {
            return 箭矢 == null ? null : 箭矢.getLocation();
        } catch (RuntimeException | Error 异常) {
            调试日志器.调试("秘法陨落", "读取Arrow位置异常 projectile_uuid=%s 异常=%s",
                    箭矢 == null ? null : 箭矢.getUniqueId(), 异常.getClass().getSimpleName());
            return null;
        }
    }

    private void 播放释放特效(位置 起点) {
        World 世界 = Bukkit.getWorld(起点.世界());
        if (世界 == null) {
            return;
        }
        Location 施法位置 = 位置适配器.转换(起点).clone().add(0, 释放高度偏移, 0);
        世界.spawnParticle(Particle.DUST, 施法位置, 35, 0.7, 0.7, 0.7, 0.15,
                new Particle.DustOptions(陨落主颜色, 2.0f));
        世界.spawnParticle(Particle.DUST, 施法位置, 20, 0.3, 0.3, 0.3, 0.2,
                new Particle.DustOptions(陨落核心颜色, 1.0f));
        世界.spawnParticle(Particle.END_ROD, 施法位置, 20, 0.6, 0.6, 0.6, 0.08);
        世界.spawnParticle(Particle.SONIC_BOOM, 施法位置, 2, 0.1, 0.1, 0.1, 0.0);
        世界.playSound(施法位置, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 0.5f);
        世界.playSound(施法位置, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.7f);
    }

    private double 计算圆环起始角度(位置 玩家位置, 位置 中心) {
        double 方向x = 玩家位置.x() - 中心.x();
        double 方向z = 玩家位置.z() - 中心.z();
        double 方向长度平方 = 方向x * 方向x + 方向z * 方向z;
        double 起始角度 = 方向长度平方 < 1e-12 ? 0.0 : Math.atan2(方向z, 方向x);
        调试日志器.调试("秘法陨落", "圆环起始角度：玩家=(%.1f,%.1f) 中心=(%.1f,%.1f) 方向长度平方=%.6f 起始角度=%.4f",
                玩家位置.x(), 玩家位置.z(), 中心.x(), 中心.z(), 方向长度平方, 起始角度);
        return 起始角度;
    }

    private void 绘制圆环点(World 世界, Location 预警中心, double 角度, double 渐变比例) {
        double 当前半径 = 获取参数("半径", 半径);
        double 当前垂直高度 = 获取参数("垂直高度", 垂直高度);
        Location 圆点 = new Location(
                预警中心.getWorld(),
                预警中心.getX() + Math.cos(角度) * 当前半径,
                预警中心.getY() - 当前垂直高度,
                预警中心.getZ() + Math.sin(角度) * 当前半径);
        // 圆环粒子颜色从离玩家最近点(浅色)单调渐变到最远点(深色)
        Color 当前颜色 = 插值颜色(陨落浅色, 陨落深色, 渐变比例);
        世界.spawnParticle(Particle.DUST, 圆点, 2, 0.1, 0.1, 0.1, 0.0,
                new Particle.DustOptions(当前颜色, 1.5f));
        世界.spawnParticle(Particle.END_ROD, 圆点, 1, 0.05, 0.05, 0.05, 0.0);
    }

    private Color 插值颜色(Color 起始色, Color 终止色, double 比例) {
        if (比例 <= 0.0) {
            return 起始色;
        }
        if (比例 >= 1.0) {
            return 终止色;
        }
        int 红 = (int) Math.round(起始色.getRed() + (终止色.getRed() - 起始色.getRed()) * 比例);
        int 绿 = (int) Math.round(起始色.getGreen() + (终止色.getGreen() - 起始色.getGreen()) * 比例);
        int 蓝 = (int) Math.round(起始色.getBlue() + (终止色.getBlue() - 起始色.getBlue()) * 比例);
        return Color.fromRGB(红, 绿, 蓝);
    }

    private void 播放箭矢下落气泡特效(World 世界, Location 箭矢位置) {
        if (世界 == null || 箭矢位置 == null) {
            return;
        }
        世界.spawnParticle(Particle.BUBBLE_POP, 箭矢位置, 气泡环点数,
                0.25, 0.25, 0.25, 0.02);
        for (int i = 0; i < 气泡环点数; i++) {
            double 角 = i * Math.PI * 2 / 气泡环点数;
            Location 环点 = 箭矢位置.clone().add(
                    Math.cos(角) * 气泡环半径, 0, Math.sin(角) * 气泡环半径);
            世界.spawnParticle(Particle.DUST, 环点, 1, 0.02, 0.02, 0.02, 0.0,
                    new Particle.DustOptions(紫色气泡颜色, 0.8f));
        }
    }
}
