package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.仇恨服务;
import mljy.业务层.吸血处理服务;
import mljy.业务层.消息服务;
import mljy.业务层.消息.伤害日志格式化服务;
import mljy.业务层.消息.受击修正上下文;
import mljy.基础设施层.颜色码工具;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.内存战斗服务;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.效果.效果实例;
import mljy.领域层.玩家.玩家快照;
import mljy.技能实现.奥能法师.奥术护盾效果处理器;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 战斗监听器。
 * 监听伤害事件、治疗事件。
 * 进入战斗条件：造成伤害、受到伤害、治疗、释放技能。
 * 装备切换（盔甲槽）由装备切换监听器负责，职责不重叠。
 * 仇恨记录：玩家造成伤害时记录仇恨。
 * 战斗日志：普通攻击（非XRM技能伤害）发送普通攻击日志和受到伤害日志。
 * XRM技能伤害由技能代码自行发送日志，此处通过PersistentDataContainer识别并跳过。
 */
public class 战斗监听器 implements Listener {
    private static final String 普通攻击日志键 = "战斗日志.普通攻击";
    private static final String 受到伤害日志键 = "战斗日志.受到伤害";
    private static final String 受到伤害暴击日志键 = "战斗日志.受到伤害暴击";
    private static final String 受到伤害带护盾抵挡日志键 = "战斗日志.受到伤害带护盾抵挡";
    private static final String 空手键 = "战斗日志.空手";
    private static final String 护盾效果标识 = "1_3_1";
    private static final String 奥术护盾来源键 = "effect.奥能法师.奥术护盾.name";
    private static final String 数值格式 = "%.1f";

    private final 战斗状态服务 战斗状态服务;
    private final 仇恨服务 仇恨服务;
    private final 消息服务 消息服务;
    private final 玩家服务 玩家服务;
    private final 效果调度服务 效果调度服务;
    private final 奥术护盾效果处理器 护盾处理器;
    private final 伤害日志格式化服务 伤害日志格式化服务;
    private final 吸血处理服务 吸血处理服务;
    private final NamespacedKey 技能伤害键;

    @Inject
    public 战斗监听器(战斗状态服务 战斗状态服务, 仇恨服务 仇恨服务,
                     消息服务 消息服务, 玩家服务 玩家服务,
                     效果调度服务 效果调度服务,
                     伤害日志格式化服务 伤害日志格式化服务,
                     JavaPlugin 插件,
                     奥术护盾效果处理器 护盾处理器,
                     吸血处理服务 吸血处理服务) {
        this.战斗状态服务 = 战斗状态服务;
        this.仇恨服务 = 仇恨服务;
        this.消息服务 = 消息服务;
        this.玩家服务 = 玩家服务;
        this.效果调度服务 = 效果调度服务;
        this.护盾处理器 = 护盾处理器;
        this.伤害日志格式化服务 = 伤害日志格式化服务;
        this.吸血处理服务 = 吸血处理服务;
        this.技能伤害键 = new NamespacedKey(插件, 内存战斗服务.技能伤害标记键);
    }

    public 战斗监听器(战斗状态服务 战斗状态服务, 仇恨服务 仇恨服务,
                     消息服务 消息服务, 玩家服务 玩家服务,
                     效果调度服务 效果调度服务,
                     mljy.翻译服务 翻译服务, JavaPlugin 插件,
                     吸血处理服务 吸血处理服务) {
        this(战斗状态服务, 仇恨服务, 消息服务, 玩家服务, 效果调度服务,
                new mljy.业务层.消息.伤害日志格式化服务实现(翻译服务),
                插件, new 奥术护盾效果处理器(效果调度服务), 吸血处理服务);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void 原生实体伤害护盾吸收(EntityDamageEvent 事件) {
        UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        try {
            事件日志上下文.在事件中执行(事件标识, () -> {
                Entity 目标实体 = 事件.getEntity();
                调试日志器.调试("战斗监听器",
                        "原生护盾入口：目标=%s 伤害原因=%s 取消=%s 事件=%s",
                        目标实体 == null ? "null" : 目标实体.getClass().getSimpleName(),
                        事件.getCause(), 事件.isCancelled(), 事件标识);
                if (!(目标实体 instanceof Player 受击者)) {
                    调试日志器.调试("战斗监听器", "原生护盾跳过：目标不是玩家 事件=%s", 事件标识);
                    return;
                }
                boolean xrm技能伤害 = 是XRM技能伤害(受击者);
                调试日志器.调试("战斗监听器",
                        "原生护盾XRM标记：目标=%s 是XRM技能伤害=%s 事件=%s",
                        受击者.getUniqueId(), xrm技能伤害, 事件标识);
                if (事件.isCancelled() || xrm技能伤害) {
                    调试日志器.调试("战斗监听器",
                            "原生护盾跳过：取消=%s XRM技能伤害=%s 事件=%s",
                            事件.isCancelled(), xrm技能伤害, 事件标识);
                    return;
                }
                处理原生护盾吸收(受击者, 事件, 事件标识);
            });
        } catch (RuntimeException 异常) {
            调试日志器.调试("战斗监听器",
                    "原生护盾处理异常：事件=%s 异常=%s", 事件标识, 异常);
        } finally {
            事件日志上下文.清除();
        }
    }

    private void 处理原生护盾吸收(Player 受击者, EntityDamageEvent 事件, UUID 事件标识) {
        UUID 受击者标识 = 受击者.getUniqueId();
        List<效果实例> 效果列表 = 效果调度服务.获取列表(受击者标识);
        if (效果列表 == null) {
            效果列表 = List.of();
        }
        Optional<效果实例> 护盾效果可选 = 效果列表.stream()
                .filter(效果 -> 效果 != null && 护盾效果标识.equals(效果.获取效果标识()))
                .findFirst();
        调试日志器.调试("战斗监听器",
                "原生护盾效果查找：目标=%s 效果数量=%d 命中=%s 事件=%s",
                受击者标识, 效果列表.size(), 护盾效果可选.isPresent(), 事件标识);
        if (护盾效果可选.isEmpty()) {
            调试日志器.调试("战斗监听器",
                    "原生护盾完成：目标=%s 抵挡量=0.0 剩余量=未修改 最终效果状态=无护盾 事件=%s",
                    受击者标识, 事件标识);
            return;
        }

        效果实例 护盾效果 = 护盾效果可选.get();
        double 输入伤害 = 事件.getDamage();
        调试日志器.调试("战斗监听器",
                "原生护盾输入：目标=%s 输入伤害=%.6f 事件=%s",
                受击者标识, 输入伤害, 事件标识);
        if (!Double.isFinite(输入伤害) || 输入伤害 <= 0) {
            调试日志器.调试("战斗监听器",
                    "原生护盾跳过：输入伤害非法或非正 输入伤害=%.6f 最终效果状态=保持 事件=%s",
                    输入伤害, 事件标识);
            return;
        }
        奥术护盾效果处理器.护盾吸收结果 处理结果 = 护盾处理器.吸收伤害(
                受击者标识, 护盾效果, 输入伤害, 事件标识);
        if (处理结果.吸收量() > 0.0D) {
            受击修正上下文.记录吸收(
                    受击者标识, 事件标识, 处理结果.吸收量(), 奥术护盾来源键);
            事件.setDamage(处理结果.剩余伤害());
        }
        调试日志器.调试("战斗监听器",
                "原生护盾完成：目标=%s 输入伤害=%.6f 抵挡量=%.6f 剩余量=%.6f 剩余护盾=%.6f 最终效果状态=%s 事件=%s",
                受击者标识, 输入伤害, 处理结果.吸收量(), 处理结果.剩余伤害(),
                处理结果.剩余护盾(), 处理结果.已耗尽() ? "已移除" : "保留精确值", 事件标识);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void 实体被攻击(EntityDamageEvent 事件) {
        UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        try {
            事件日志上下文.在事件中执行(事件标识, () -> {
                调试日志器.调试("战斗监听器", "实体受伤 来源=%s 伤害=%.1f 目标=%s 取消=%s 事件=%s",
                        获取伤害来源名(事件), 事件.getFinalDamage(), 事件.getEntity().getName(),
                        事件.isCancelled(), 事件标识);
                if (事件.isCancelled()) {
                    return;
                }
                boolean xrm技能伤害 = 是XRM技能伤害(事件.getEntity());
                String 伤害来源名 = 获取伤害来源名(事件);
                if (事件 instanceof EntityDamageByEntityEvent 实体伤害事件
                        && 实体伤害事件.getDamager() instanceof Player 攻击者) {
                    伤害来源名 = 伤害日志格式化服务.获取实体名称(攻击者);
                    处理玩家造成伤害(攻击者, 实体伤害事件, 事件标识, xrm技能伤害);
                }
                if (事件.getEntity() instanceof Player 受击者) {
                    战斗状态服务.进入战斗(受击者.getUniqueId(), 事件标识);
                    if (!xrm技能伤害) {
                        发送受到伤害日志(受击者, 事件, 事件标识, 伤害来源名);
                    }
                }
            });
        } finally {
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    private void 处理玩家造成伤害(Player 攻击者, EntityDamageByEntityEvent 事件,
                              UUID 事件标识, boolean xrm技能伤害) {
        UUID 玩家标识 = 攻击者.getUniqueId();
        战斗状态服务.进入战斗(玩家标识, 事件标识);
        if (事件.getEntity() instanceof LivingEntity 目标 && !(目标 instanceof Player)) {
            仇恨服务.记录伤害仇恨(目标.getUniqueId(), 玩家标识, 事件.getFinalDamage());
        }
        调试日志器.调试("战斗监听器", "处理玩家造成伤害 攻击者=%s 伤害=%.1f 是XRM技能伤害=%s",
                攻击者.getName(), 事件.getFinalDamage(), xrm技能伤害);
        if (吸血处理服务 != null) {
            Optional<玩家快照> 攻击者快照可选 = 玩家服务.获取快照(玩家标识);
            if (攻击者快照可选.isPresent()) {
                double 权威伤害值 = xrm技能伤害
                        ? 读取技能权威伤害值(事件.getEntity())
                        : 事件.getFinalDamage();
                吸血处理服务.处理伤害吸血(攻击者快照可选.get(), 权威伤害值);
            }
        }
        if (xrm技能伤害) {
            return;
        }
        发送普通攻击日志(攻击者, 事件, 事件标识);
    }

    private double 读取技能权威伤害值(org.bukkit.entity.Entity 实体) {
        if (!(实体 instanceof LivingEntity 生物)) {
            return 0.0;
        }
        PersistentDataContainer pdc = 生物.getPersistentDataContainer();
        if (pdc == null || !pdc.has(技能伤害键, PersistentDataType.DOUBLE)) {
            return 0.0;
        }
        Double 值 = pdc.get(技能伤害键, PersistentDataType.DOUBLE);
        return 值 != null ? 值 : 0.0;
    }

    private void 发送普通攻击日志(Player 攻击者, EntityDamageByEntityEvent 事件, UUID 事件标识) {
        double 伤害数值 = 事件.getFinalDamage();
        String 伤害数值文本 = String.format(数值格式, 伤害数值);
        String 武器名 = 获取武器名(攻击者);
        String 目标名 = 伤害日志格式化服务.获取实体名称(事件.getEntity());
        Optional<玩家快照> 攻击者快照可选 = 玩家服务.获取快照(攻击者.getUniqueId());
        攻击者快照可选.ifPresent(快照 -> 消息服务.发送战斗日志(
                快照, 事件标识, 普通攻击日志键, 武器名, 目标名, 伤害数值文本));
    }

    private void 发送受到伤害日志(Player 受击者, EntityDamageEvent 事件,
                              UUID 事件标识, String 伤害来源名) {
        double 伤害数值 = 事件.getFinalDamage();
        String 伤害数值文本 = String.format(数值格式, 伤害数值);
        List<受击修正上下文.修正信息> 修正列表 = 受击修正上下文.获取并清除(受击者.getUniqueId(), 事件标识);
        Optional<受击修正上下文.修正信息> 护盾吸收可选 = 修正列表.stream()
                .filter(修正 -> 修正 != null
                        && 修正.类型() == 受击修正上下文.修正类型.吸收
                        && 修正.修正量() > 0)
                .findFirst();
        Optional<玩家快照> 受击者快照可选 = 玩家服务.获取快照(受击者.getUniqueId());
        受击者快照可选.ifPresent(快照 -> {
            if (护盾吸收可选.isPresent()) {
                String 抵挡数值文本 = String.format(数值格式, 护盾吸收可选.get().修正量());
                消息服务.发送战斗日志(快照, 事件标识, 受到伤害带护盾抵挡日志键,
                        伤害来源名, 伤害数值文本, 抵挡数值文本);
            } else {
                消息服务.发送战斗日志(快照, 事件标识, 受到伤害日志键,
                        伤害来源名, 伤害数值文本);
            }
        });
        调试日志器.调试("战斗监听器",
                "受到伤害日志：目标=%s 来源=%s 最终伤害=%s 护盾吸收=%s 事件=%s",
                受击者.getUniqueId(), 伤害来源名, 伤害数值文本,
                护盾吸收可选.map(修正 -> String.format(数值格式, 修正.修正量())).orElse("无"),
                事件标识);
        附加上非吸收修正日志(受击者, 事件标识, 修正列表);
    }

    private String 获取伤害来源名(EntityDamageEvent 事件) {
        if (事件 instanceof EntityDamageByEntityEvent 实体伤害事件) {
            return 伤害日志格式化服务.获取实体来源名(实体伤害事件.getDamager());
        }
        String 原因名 = 事件.getCause() == null ? "UNKNOWN" : 事件.getCause().name();
        return 伤害日志格式化服务.获取来源翻译("战斗日志.伤害来源." + 原因名);
    }

    private void 附加上非吸收修正日志(Player 受击者, UUID 事件标识,
                                    List<受击修正上下文.修正信息> 修正列表) {
        if (修正列表 == null || 修正列表.isEmpty()) {
            return;
        }
        Optional<玩家快照> 受击者快照可选 = 玩家服务.获取快照(受击者.getUniqueId());
        受击者快照可选.ifPresent(快照 -> {
            for (受击修正上下文.修正信息 修正 : 修正列表) {
                if (修正 == null) {
                    continue;
                }
                if (修正.类型() == 受击修正上下文.修正类型.吸收) {
                    continue;
                }
                if (修正.修正量() > 0) {
                    String 修正数值 = String.format(数值格式, 修正.修正量());
                    String 来源名 = 伤害日志格式化服务.获取安全翻译(修正.来源名称(), 伤害日志格式化服务.获取未知来源名());
                    消息服务.发送战斗日志(快照, 事件标识, 修正.获取翻译键(),
                            来源名, 修正数值);
                    调试日志器.调试("战斗监听器",
                            "受到伤害修正日志：目标=%s 来源=%s 修正类型=%s 修正量=%s 事件=%s",
                            受击者.getUniqueId(), 来源名, 修正.类型(), 修正数值,
                            事件标识);
                }
            }
        });
    }

    private String 获取武器名(Player 攻击者) {
        ItemStack 主手物品 = 攻击者.getInventory().getItemInMainHand();
        if (主手物品 == null || 主手物品.getType().isAir()) {
            return 伤害日志格式化服务.获取安全翻译(空手键, "");
        }
        ItemMeta 元数据 = 主手物品.getItemMeta();
        if (元数据 != null && 元数据.hasDisplayName()) {
            Component 显示名组件 = 元数据.displayName();
            String 显示名 = 显示名组件 == null ? "" : LegacyComponentSerializer.legacySection().serialize(显示名组件);
            return 颜色码工具.转换颜色码(显示名);
        }
        String 翻译键 = 主手物品.getType().translationKey();
        if (翻译键 == null || 翻译键.isBlank()) {
            调试日志器.调试("战斗监听器", "武器客户端翻译键为空：玩家=%s 物品=%s",
                    攻击者.getName(), 主手物品.getType());
            return 伤害日志格式化服务.获取安全翻译(空手键, "");
        }
        调试日志器.调试("战斗监听器", "武器名称使用客户端翻译：玩家=%s 物品=%s 翻译键=%s",
                攻击者.getName(), 主手物品.getType(), 翻译键);
        return "<lang:" + 翻译键 + ">";
    }

    private boolean 是XRM技能伤害(org.bukkit.entity.Entity 实体) {
        if (!(实体 instanceof LivingEntity 生物)) {
            return false;
        }
        PersistentDataContainer pdc = 生物.getPersistentDataContainer();
        return pdc != null && pdc.has(技能伤害键, PersistentDataType.DOUBLE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void 实体恢复生命(EntityRegainHealthEvent 事件) {
        if (!(事件.getEntity() instanceof Player 玩家)) {
            return;
        }
        调试日志器.调试("战斗监听器", "实体恢复生命 玩家=%s 恢复原因=%s",
                玩家.getName(), 事件.getRegainReason());
        if (事件.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }
        战斗状态服务.进入战斗(玩家.getUniqueId());
    }
}
