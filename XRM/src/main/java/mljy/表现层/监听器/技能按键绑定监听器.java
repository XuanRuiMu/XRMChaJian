package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.驭空术服务;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.事件日志上下文;
import mljy.领域层.技能.技能按键类型;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.驭空术.飞行状态;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能按键绑定监听器。
 * 监听PlayerInteractEvent与PlayerAnimationEvent，左键点击触发对应槽位的技能。
 * 9个技能槽位对应9种工具类型：剑/斧/镐/铲/锄/矛/重锤/书/自定义。
 * 释放技能后进入战斗状态。
 * 左键空气、左键方块、攻击实体三个动作都触发技能，且不取消原事件。
 * PlayerAnimationEvent 作为路径A的补充触发源，覆盖虚弱0伤害挥动实体场景：
 * 此时PlayerInteractEvent不派发（攻击实体非空气/方块），EntityDamageByEntityEvent也不派发（0伤害），
 * 仅PlayerAnimationEvent可靠触发。用200ms时间窗口按 玩家+按键槽位 维度去重，避免同一挥剑动作
 * 同时触发 PlayerAnimationEvent 与 EntityDamageByEntityEvent 导致重复释放技能。
 */
public class 技能按键绑定监听器 implements Listener {
    private static final String 配置根键 = "技能.专精编号映射";
    private static final String 技能标识分隔符 = "_";
    private static final long 触发去重毫秒 = 200L;

    private final 玩家服务 玩家服务;
    private final 技能释放服务 技能释放服务;
    private final 战斗状态服务 战斗状态服务;
    private final 技能注册服务 技能注册服务;
    private final 驭空术服务 驭空术服务;
    private final JavaPlugin 插件;
    private final Map<String, Integer> 专精编号映射 = new HashMap<>();
    private final Map<String, Long> 玩家最近触发时间表 = new ConcurrentHashMap<>();
    private NamespacedKey 技能伤害键;

    @Inject
    public 技能按键绑定监听器(玩家服务 玩家服务, 技能释放服务 技能释放服务,
                             战斗状态服务 战斗状态服务, 技能注册服务 技能注册服务,
                             驭空术服务 驭空术服务, JavaPlugin 插件) {
        this.玩家服务 = 玩家服务;
        this.技能释放服务 = 技能释放服务;
        this.战斗状态服务 = 战斗状态服务;
        this.技能注册服务 = 技能注册服务;
        this.驭空术服务 = 驭空术服务;
        this.插件 = 插件;
        加载专精编号映射();
    }

    private void 加载专精编号映射() {
        FileConfiguration 配置 = 插件.getConfig();
        ConfigurationSection 区段 = 配置.getConfigurationSection(配置根键);
        if (区段 == null) {
            return;
        }
        for (String 专精 : 区段.getKeys(false)) {
            int 编号 = 区段.getInt(专精, 0);
            if (编号 > 0) {
                专精编号映射.put(专精, 编号);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void 玩家交互(PlayerInteractEvent 事件) {
        if (事件.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Action 动作 = 事件.getAction();
        if (动作 != Action.LEFT_CLICK_AIR && 动作 != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player 玩家 = 事件.getPlayer();
        UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        try {
            事件日志上下文.在事件中执行(事件标识, () -> {
                调试日志器.调试("技能按键监听器", "玩家交互：玩家=%s 动作=%s 事件=%s", 玩家.getName(), 动作.name(), 事件标识);
                ItemStack 主手物品 = 玩家.getInventory().getItemInMainHand();
                if (主手物品 == null || 主手物品.getType() == Material.AIR) {
                    调试日志器.调试("技能按键监听器", "空手左键，转入驭空术处理：玩家=%s 事件=%s", 玩家.getName(), 事件标识);
                    处理空手左键(玩家);
                    return;
                }
                技能按键类型 按键类型 = 技能按键类型.从物品查询(主手物品.getType());
                if (按键类型 == null) {
                    调试日志器.调试("技能按键监听器", "主手物品无技能绑定：玩家=%s 物品=%s 事件=%s",
                            玩家.getName(), 主手物品.getType(), 事件标识);
                    return;
                }
                调试日志器.调试("技能按键监听器", "触发技能入口：玩家=%s 物品=%s → 按键类型=%s 槽位=%d 事件=%s",
                        玩家.getName(), 主手物品.getType(), 按键类型, 按键类型.获取槽位编号(), 事件标识);
                boolean 技能已触发 = 触发技能(玩家, 按键类型, 事件标识);
                if (动作 == Action.LEFT_CLICK_AIR && 技能已触发) {
                    事件日志上下文.创建玩家目标因果凭证(玩家.getUniqueId(), 事件标识, 事件);
                }
            });
        } finally {
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    private void 处理空手左键(Player 玩家) {
        飞行状态 状态 = 驭空术服务.获取飞行状态(玩家.getUniqueId());
        if (状态 == 飞行状态.飞行中 || 状态 == 飞行状态.准驭空) {
            调试日志器.调试("技能按键监听器", "空手左键触发驭空术冲刺：玩家=%s 状态=%s", 玩家.getName(), 状态.name());
            驭空术服务.冲刺(玩家.getUniqueId());
        }
    }

    /**
     * 玩家挥动手臂事件监听器。
     * PlayerAnimationEvent 在玩家左键空气、左键方块、左键实体时都触发，不依赖伤害值。
     * 虚弱0伤害挥动实体场景下，PlayerInteractEvent 与 EntityDamageByEntityEvent 均不派发，
     * 此监听器作为路径A的补充触发源，确保技能可正常释放。
     * 优先级 LOWEST 早于路径A（LOW）和路径B（LOW），首次触发后由 触发技能 内部50ms去重窗口拦截后续重复触发。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void 玩家挥动(PlayerAnimationEvent 事件) {
        Player 玩家 = 事件.getPlayer();
        UUID 事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        try {
            事件日志上下文.在事件中执行(事件标识, () -> {
                调试日志器.调试("技能按键监听器", "玩家挥动：玩家=%s 事件=%s", 玩家.getName(), 事件标识);
                ItemStack 主手物品 = 玩家.getInventory().getItemInMainHand();
                if (主手物品 == null || 主手物品.getType() == Material.AIR) {
                    调试日志器.调试("技能按键监听器", "空手挥动，转入驭空术处理：玩家=%s 事件=%s", 玩家.getName(), 事件标识);
                    处理空手左键(玩家);
                    return;
                }
                技能按键类型 按键类型 = 技能按键类型.从物品查询(主手物品.getType());
                if (按键类型 == null) {
                    调试日志器.调试("技能按键监听器", "挥动主手物品无技能绑定：玩家=%s 物品=%s 事件=%s",
                            玩家.getName(), 主手物品.getType(), 事件标识);
                    return;
                }
                调试日志器.调试("技能按键监听器", "挥动触发技能入口：玩家=%s 物品=%s → 按键类型=%s 槽位=%d 事件=%s",
                        玩家.getName(), 主手物品.getType(), 按键类型, 按键类型.获取槽位编号(), 事件标识);
                boolean 技能已触发 = 触发技能(玩家, 按键类型, 事件标识);
                if (技能已触发) {
                    事件日志上下文.创建玩家目标因果凭证(玩家.getUniqueId(), 事件标识, 事件);
                }
            });
        } finally {
            事件日志上下文.结束事件对象(事件);
            事件日志上下文.清除();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void 实体被攻击(EntityDamageByEntityEvent 事件) {
        if (!(事件.getDamager() instanceof Player 玩家)) {
            return;
        }
        UUID 事件标识 = 事件日志上下文.消费玩家目标因果凭证(
                玩家.getUniqueId(), 事件.getEntity(), 事件);
        if (事件标识 == null) {
            事件标识 = 事件日志上下文.获取或创建事件标识(事件);
        }
        UUID 根事件标识 = 事件标识;
        try {
            事件日志上下文.在事件中执行(根事件标识, () -> 处理实体被攻击(事件, 玩家, 根事件标识));
        } finally {
            // 战斗监听器在 MONITOR 阶段结束根事件；这里仅清除当前线程，保留事件对象映射。
            事件日志上下文.清除();
        }
    }

    private void 处理实体被攻击(EntityDamageByEntityEvent 事件, Player 玩家, UUID 事件标识) {
        if (事件.getEntity() instanceof org.bukkit.entity.LivingEntity 生物
                && 获取技能伤害键() != null
                && 生物.getPersistentDataContainer().has(获取技能伤害键(), PersistentDataType.DOUBLE)) {
            调试日志器.调试("技能按键监听器", "跳过XRM技能伤害事件：攻击者=%s 目标=%s 事件=%s",
                    玩家.getName(), 事件.getEntity().getName(), 事件标识);
            return;
        }
        ItemStack 主手物品 = 玩家.getInventory().getItemInMainHand();
        if (主手物品 == null || 主手物品.getType() == Material.AIR) {
            return;
        }
        技能按键类型 按键类型 = 技能按键类型.从物品查询(主手物品.getType());
        if (按键类型 == null) {
            return;
        }
        调试日志器.调试("技能按键监听器", "实体被攻击触发技能：玩家=%s 目标=%s 按键类型=%s 事件=%s",
                玩家.getName(), 事件.getEntity().getClass().getSimpleName(), 按键类型, 事件标识);
        触发技能(玩家, 按键类型, 事件标识);
    }

    private boolean 触发技能(Player 玩家, 技能按键类型 按键类型, UUID 事件标识) {
        UUID 玩家标识 = 玩家.getUniqueId();
        long 当前时间 = System.currentTimeMillis();
        String 去重键 = 玩家标识 + "_" + 按键类型.获取槽位编号();
        Long 最近触发时间 = 玩家最近触发时间表.get(去重键);
        if (最近触发时间 != null && 当前时间 - 最近触发时间 < 触发去重毫秒) {
            调试日志器.调试("技能按键监听器", "跳过重复触发：玩家=%s 按键类型=%s 距上次=%dms 事件=%s",
                    玩家.getName(), 按键类型, 当前时间 - 最近触发时间, 事件标识);
            return false;
        }
        玩家最近触发时间表.put(去重键, 当前时间);
        String 技能标识 = 构建技能标识(玩家标识, 按键类型);
        if (技能标识 == null) {
            调试日志器.调试("技能按键监听器", "技能标识构建失败：玩家=%s 按键类型=%s 槽位=%d", 玩家.getName(), 按键类型, 按键类型.获取槽位编号());
            return false;
        }
        调试日志器.调试("技能按键监听器", "构建技能标识成功：玩家=%s 专精编号_槽位=%s 事件=%s", 玩家.getName(), 技能标识, 事件标识);
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        if (快照可选.isEmpty()) {
            调试日志器.调试("技能按键监听器", "玩家快照不存在：玩家=%s 技能标识=%s 事件=%s", 玩家.getName(), 技能标识, 事件标识);
            return false;
        }
        调试日志器.调试("技能按键监听器", "玩家快照获取成功：玩家=%s 等级=%d 智力=%.1f 事件=%s", 玩家.getName(), 快照可选.get().等级(), 快照可选.get().属性().基础智力(), 事件标识);
        Optional<技能定义> 定义可选 = 技能注册服务.获取定义(技能标识);
        if (定义可选.isEmpty()) {
            调试日志器.调试("技能按键监听器", "技能定义未注册：玩家=%s 技能标识=%s 事件=%s", 玩家.getName(), 技能标识, 事件标识);
            return false;
        }
        技能定义 定义 = 定义可选.get();
        调试日志器.调试("技能按键监听器", "释放技能：玩家=%s 技能标识=%s 施法类型=%s 冷却=%.1f 公共冷却=%.1f 事件=%s",
                玩家.getName(), 技能标识, 定义.施法类型(), 定义.冷却时间(), 定义.公共冷却(), 事件标识);
        技能上下文 上下文 = new 技能上下文(
                快照可选.get(),
                技能标识,
                定义,
                null,
                当前时间
        );
        技能执行结果 结果 = 技能释放服务.释放(上下文);
        调试日志器.调试("技能按键监听器", "释放结果：玩家=%s 技能标识=%s 结果=%s 事件=%s", 玩家.getName(), 技能标识, 结果, 事件标识);
        if (结果 == 技能执行结果.成功) {
            战斗状态服务.进入战斗(玩家标识, 事件标识);
            return true;
        }
        return false;
    }

    private String 构建技能标识(UUID 玩家标识, 技能按键类型 按键类型) {
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        if (会话可选.isEmpty()) {
            调试日志器.调试("技能按键监听器", "构建技能标识失败：玩家会话不存在 玩家=%s", 玩家标识);
            return null;
        }
        String 专精 = 会话可选.get().获取专精();
        调试日志器.调试("技能按键监听器", "构建技能标识：专精=%s 槽位=%d", 专精, 按键类型.获取槽位编号());
        Integer 编号 = 专精编号映射.get(专精);
        if (编号 == null) {
            调试日志器.调试("技能按键监听器", "构建技能标识失败：专精=%s 无编号映射 映射表=%s", 专精, 专精编号映射);
            return null;
        }
        String 技能标识 = 编号 + 技能标识分隔符 + 按键类型.获取槽位编号();
        调试日志器.调试("技能按键监听器", "构建技能标识完成：%s", 技能标识);
        return 技能标识;
    }

    private NamespacedKey 获取技能伤害键() {
        if (技能伤害键 == null) {
            try {
                技能伤害键 = new NamespacedKey(插件, mljy.基础设施层.内存战斗服务.技能伤害标记键);
            } catch (Exception e) {
                调试日志器.调试("技能按键监听器", "创建技能伤害键失败：%s", e.getMessage());
                return null;
            }
        }
        return 技能伤害键;
    }
}
