package mljy.表现层.监听器;

import com.google.inject.Inject;
import mljy.业务层.乐器服务;
import mljy.业务层.乐器服务.乐谱保存结果;
import mljy.业务层.乐器服务实现;
import mljy.领域层.乐器.功能位定义;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.延音模式;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 乐器监听器。
 * 处理玩家右键乐器物品打开演奏界面、演奏界面点击事件、界面关闭、玩家退出清理。
 *
 * FP-F 扩展：
 * - 6×9 钢琴布局：9 功能位（槽 0-8）+ 中部控制（槽 9-35）+ 5 黑键（槽 36-44）+ 7 白键（槽 45-53）
 * - 点击处理改为：功能位表查询 + 中部控制槽位比较 + 解析槽位音高 三段分发
 * - 删除旧 根据音高获取材质 方法（硬编码 wool），改用服务端 音高方块映射.音高转方块
 * - 延音触发：点击琴键时根据延音模式（按住/切换）调用 开始延音 / 停止延音
 *
 * FP-E 扩展：
 * - GUI 音符按钮点击改用统一 演奏指令（带玩家力度档），实现三轨输入归一。
 * - 新增聊天命令模式（Apple Musical Typing）：玩家手持乐器物品时输入单字母 a-z 触发音符。
 */
public class 乐器监听器 implements Listener {
    // FP-E: GUI 瞬时点击的默认时值 tick，与 乐器服务实现.默认时值Tick 保持一致。
    private static final int 默认时值Tick = 4;

    private final 乐器服务 乐器服务;
    private final 乐器注册表 乐器注册表;
    private final JavaPlugin 插件;
    // GAP-01: 玩家点击保存遇到重名后，记下待覆盖乐谱名；再次点击保存则确认覆盖
    // TODO: FP-16 命令扩展后可改为聊天输入新乐谱名
    private final Map<UUID, String> 待覆盖乐谱名 = new HashMap<>();

    @Inject
    public 乐器监听器(乐器服务 乐器服务, 乐器注册表 乐器注册表, JavaPlugin 插件) {
        this.乐器服务 = 乐器服务;
        this.乐器注册表 = 乐器注册表;
        this.插件 = 插件;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 玩家右键物品(PlayerInteractEvent 事件) {
        Action 动作 = 事件.getAction();
        if (动作 != Action.RIGHT_CLICK_AIR && 动作 != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (事件.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player 玩家 = 事件.getPlayer();
        ItemStack 物品 = 玩家.getInventory().getItemInMainHand();
        if (物品 == null || 物品.getType() == Material.AIR) {
            return;
        }
        乐器定义 乐器 = 解析乐器物品(物品);
        if (乐器 == null) {
            return;
        }
        事件.setCancelled(true);
        乐器服务.打开演奏界面(玩家, 乐器);
    }

    /**
     * FP-E 聊天命令模式（Apple Musical Typing 流派）。
     * 玩家手持乐器物品时输入单字母 a-z 触发对应音符；
     * z/x 切换音域，c/v 切换力度档。
     * 多字符消息不拦截（保留正常聊天）。
     * <p>
     * 线程安全：AsyncPlayerChatEvent 是异步事件，而 处理聊天按键 内部调用
     * playSound/spawnParticle 等必须主线程执行的 Bukkit API。
     * 异步触发时先取消事件（避免消息广播），再调度到主线程执行演奏。
     * <p>
     * 注意：AsyncPlayerChatEvent 已标记 @Deprecated(forRemoval=true)，但当前 Purpur 服务端仍派发该
     * 异步事件，且本监听器依赖其异步语义（先取消广播再切主线程演奏）。因此保留使用并以
     * @SuppressWarnings 显式抑制；待确认服务端改派 PlayerChatEvent 后再迁移。
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    @SuppressWarnings({"deprecation", "removal"})
    public void 玩家聊天(AsyncPlayerChatEvent 事件) {
        String 消息 = 事件.getMessage();
        if (消息 == null || 消息.length() != 1) {
            return;
        }
        Player 玩家 = 事件.getPlayer();
        ItemStack 物品 = 玩家.getInventory().getItemInMainHand();
        if (物品 == null || 物品.getType() == Material.AIR) {
            return;
        }
        乐器定义 乐器 = 解析乐器物品(物品);
        if (乐器 == null) {
            return;
        }
        // 设置玩家当前乐器（确保后续演奏指令能找到乐器）
        乐器服务.设置玩家当前乐器(玩家.getUniqueId(), 乐器);
        if (Bukkit.isPrimaryThread()) {
            // 主线程（测试环境或同步调用）：直接执行
            boolean 已消费 = 乐器服务.处理聊天按键(玩家, 消息);
            if (已消费) {
                事件.setCancelled(true);
            }
        } else {
            // 异步事件：手持乐器物品 + 单字符消息视为意图触发音符，先取消事件避免消息广播
            事件.setCancelled(true);
            // 调度到主线程执行 处理聊天按键（playSound/spawnParticle 必须主线程）
            Bukkit.getScheduler().runTask(插件, () -> 乐器服务.处理聊天按键(玩家, 消息));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void 界面点击(InventoryClickEvent 事件) {
        Inventory 界面 = 事件.getInventory();
        if (!(界面.getHolder() instanceof 乐器服务实现.演奏界面持有者 持有者)) {
            return;
        }
        事件.setCancelled(true);
        if (!(事件.getWhoClicked() instanceof Player 玩家)) {
            return;
        }
        int 槽位 = 事件.getRawSlot();
        if (槽位 < 0 || 槽位 >= 界面.getSize()) {
            return;
        }
        处理界面点击(玩家, 持有者, 槽位, 界面);
    }

    private void 处理界面点击(Player 玩家, 乐器服务实现.演奏界面持有者 持有者,
                             int 槽位, Inventory 界面) {
        乐器定义 乐器 = 持有者.获取乐器();
        if (!(乐器服务 instanceof 乐器服务实现 实现)) {
            return;
        }
        // 1. 功能位分发（9 功能位槽 0-8）
        Map<Integer, 功能位定义> 功能位表 = 实现.获取功能位槽位表();
        功能位定义 功能位 = 功能位表.get(槽位);
        if (功能位 != null) {
            处理功能位点击(玩家, 乐器, 功能位);
            return;
        }
        // 2. 中部控制分发
        if (槽位 == 实现.获取槽位音域减()) {
            处理音域切换(玩家, 持有者, false, 界面, 乐器);
            return;
        }
        if (槽位 == 实现.获取槽位音域加()) {
            处理音域切换(玩家, 持有者, true, 界面, 乐器);
            return;
        }
        if (槽位 == 实现.获取槽位音域显示()) {
            return;
        }
        if (槽位 == 实现.获取槽位力度减()) {
            乐器服务.切换玩家力度档(玩家.getUniqueId(), false);
            实现.重新填充界面(界面, 乐器, 持有者.获取当前八度());
            return;
        }
        if (槽位 == 实现.获取槽位力度加()) {
            乐器服务.切换玩家力度档(玩家.getUniqueId(), true);
            实现.重新填充界面(界面, 乐器, 持有者.获取当前八度());
            return;
        }
        if (槽位 == 实现.获取槽位力度显示()) {
            return;
        }
        if (槽位 == 实现.获取槽位录制()) {
            处理录制按钮(玩家, 乐器);
            return;
        }
        if (槽位 == 实现.获取槽位停止()) {
            处理停止按钮(玩家);
            return;
        }
        if (槽位 == 实现.获取槽位播放()) {
            处理播放按钮(玩家);
            return;
        }
        if (槽位 == 实现.获取槽位延音模式切换()) {
            乐器服务.切换延音模式(玩家.getUniqueId());
            实现.重新填充界面(界面, 乐器, 持有者.获取当前八度());
            return;
        }
        // 3. 钢琴键分发（黑键 + 白键）
        int 音高 = 实现.解析槽位音高(槽位, 持有者.获取当前八度());
        if (音高 >= 0) {
            处理琴键点击(玩家, 乐器, 音高);
            // FP-10 GUI 高亮：点击琴键后短暂切换为发光态
            触发GUI高亮(玩家, 界面, 槽位, 实现.获取GUI高亮持续tick());
        }
    }

    /**
     * FP-03 功能位点击分发。
     * 前 3 个（保存/清除/关闭）功能已实现；分层切换（FP-19）已接入；其余本轮仅提示"功能未实现"。
     */
    private void 处理功能位点击(Player 玩家, 乐器定义 乐器, 功能位定义 功能位) {
        switch (功能位) {
            case 保存 -> 处理保存按钮(玩家);
            case 清除 -> 处理清除按钮(玩家);
            case 关闭 -> 玩家.closeInventory();
            case 分层切换 -> {
                // FP-19 分层键盘 split：GUI 按钮切换分层模式开关
                乐器服务.切换分层模式(玩家.getUniqueId());
            }
            case NBS导入, NBS导出, 和弦预设, 公共乐谱库, 钢琴卷帘编辑 -> {
                if (乐器服务 instanceof 乐器服务实现 实现) {
                    实现.发送功能未实现提示(玩家);
                }
            }
            default -> {
            }
        }
    }

    /**
     * FP-04 琴键点击：根据延音模式触发 开始延音 / 停止延音。
     * - 按住模式（默认）：始终 开始延音（替换旧延音）；GUI 关闭时停止
     * - 切换模式：再点击同音符 → 停止延音；否则 开始延音
     */
    private void 处理琴键点击(Player 玩家, 乐器定义 乐器, int 音高) {
        UUID 玩家标识 = 玩家.getUniqueId();
        力度档 档 = 乐器服务.获取玩家力度档(玩家标识);
        延音模式 模式 = 乐器服务.获取延音模式(玩家标识);
        if (模式 == 延音模式.切换 && 乐器服务.是否延音中(玩家标识)
                && 乐器服务.获取延音音高(玩家标识) == 音高) {
            // 切换模式：再点击同音符 → 停止延音
            乐器服务.停止延音(玩家标识);
            return;
        }
        // 开始延音（内部调用 演奏指令 完成初始触发 + 录制 + 粒子）
        乐器服务.开始延音(玩家, 乐器, 音高, 档);
        // 录制：瞬时事件，按下后立即松开（与现有 GUI 行为一致）
        if (乐器服务.是否录制中(玩家标识)) {
            乐器服务.记录按键按下(玩家标识, 音高, 档.获取音量());
            乐器服务.记录按键松开(玩家标识, 音高);
        }
    }

    /**
     * FP-10 GUI 高亮：点击琴键后短暂将对应方块切换为发光态（附魔光效 enchant_glint），
     * 持续 GUI高亮持续tick 后恢复原状。使用 ItemMeta.setEnchantmentGlintOverride 实现纯视觉发光。
     * 玩家已关闭界面或槽位已变更时跳过恢复（避免覆盖玩家新操作）。
     *
     * @param 玩家 点击玩家
     * @param 界面 演奏界面
     * @param 槽位 被点击的槽位
     * @param 持续tick 高亮持续 tick 数
     */
    private void 触发GUI高亮(Player 玩家, Inventory 界面, int 槽位, int 持续tick) {
        if (玩家 == null || 界面 == null || 槽位 < 0 || 槽位 >= 界面.getSize()) {
            return;
        }
        ItemStack 原始物品 = 界面.getItem(槽位);
        if (原始物品 == null || 原始物品.getType() == Material.AIR) {
            return;
        }
        // 应用发光态：复制原物品并设置 glint override
        ItemStack 高亮物品 = 原始物品.clone();
        ItemMeta 元数据 = 高亮物品.getItemMeta();
        if (元数据 == null) {
            return;
        }
        元数据.setEnchantmentGlintOverride(Boolean.TRUE);
        高亮物品.setItemMeta(元数据);
        界面.setItem(槽位, 高亮物品);
        玩家.updateInventory();
        // 调度恢复任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    return;
                }
                Inventory 当前界面 = 玩家.getOpenInventory().getTopInventory();
                if (当前界面 == null || 当前界面 != 界面) {
                    return;
                }
                ItemStack 当前物品 = 界面.getItem(槽位);
                if (当前物品 == null) {
                    界面.setItem(槽位, 原始物品);
                    return;
                }
                // 仅当当前物品仍是高亮版本时恢复（避免覆盖玩家后续点击的新状态）
                ItemMeta 当前元数据 = 当前物品.getItemMeta();
                if (当前元数据 != null && Boolean.TRUE.equals(当前元数据.hasEnchantmentGlintOverride()
                        ? 当前元数据.getEnchantmentGlintOverride() : false)) {
                    界面.setItem(槽位, 原始物品);
                    玩家.updateInventory();
                }
            }
        }.runTaskLater(插件, Math.max(1, 持续tick));
    }

    private void 处理音域切换(Player 玩家, 乐器服务实现.演奏界面持有者 持有者,
                            boolean 增加, Inventory 界面, 乐器定义 乐器) {
        int 当前八度 = 持有者.获取当前八度();
        int 新八度 = 增加 ? 当前八度 + 1 : 当前八度 - 1;
        持有者.设置当前八度(新八度);
        if (乐器服务 instanceof 乐器服务实现 实现) {
            实现.重新填充界面(界面, 乐器, 持有者.获取当前八度());
        }
    }

    private void 处理录制按钮(Player 玩家, 乐器定义 乐器) {
        UUID 玩家标识 = 玩家.getUniqueId();
        if (乐器服务.是否录制中(玩家标识)) {
            乐器服务.停止录制(玩家标识);
        } else {
            乐器服务.开始录制(玩家标识, 乐器);
        }
    }

    private void 处理停止按钮(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        乐器服务.停止录制(玩家标识);
        乐器服务.停止播放(玩家标识);
    }

    private void 处理播放按钮(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        if (乐器服务.是否播放中(玩家标识)) {
            乐器服务.停止播放(玩家标识);
            return;
        }
        乐器服务.加载乐谱(玩家标识, 获取最近乐谱名(玩家标识))
                .ifPresent(乐谱 -> 乐器服务.播放乐谱(玩家, 乐谱));
    }

    private void 处理保存按钮(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        String 乐谱名 = 获取最近乐谱名(玩家标识);
        // GAP-01: 首次点击遇到重名时，服务端已发送"乐谱已存在"提示；
        // 玩家再次点击保存则视为确认覆盖。新名输入待 FP-16 命令扩展。
        String 待覆盖 = 待覆盖乐谱名.get(玩家标识);
        if (待覆盖 != null && 待覆盖.equals(乐谱名)) {
            待覆盖乐谱名.remove(玩家标识);
            乐器服务.保存乐谱并覆盖(玩家标识, 乐谱名);
            return;
        }
        乐谱保存结果 结果 = 乐器服务.保存乐谱带结果(玩家标识, 乐谱名);
        if (结果 == 乐谱保存结果.重名) {
            待覆盖乐谱名.put(玩家标识, 乐谱名);
        } else {
            待覆盖乐谱名.remove(玩家标识);
        }
    }

    private void 处理清除按钮(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        乐器服务.停止录制(玩家标识);
        乐器服务.停止播放(玩家标识);
    }

    private String 获取最近乐谱名(UUID 玩家标识) {
        java.util.List<String> 乐谱列表 = 乐器服务.获取乐谱列表(玩家标识);
        if (乐谱列表.isEmpty()) {
            return 乐器服务.获取默认乐谱名();
        }
        return 乐谱列表.get(乐谱列表.size() - 1);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void 界面关闭(InventoryCloseEvent 事件) {
        if (!(事件.getInventory().getHolder() instanceof 乐器服务实现.演奏界面持有者)) {
            return;
        }
        if (!(事件.getPlayer() instanceof Player 玩家)) {
            return;
        }
        // FP-04 按住模式：GUI 关闭时停止延音
        乐器服务.停止延音(玩家.getUniqueId());
        if (乐器服务 instanceof 乐器服务实现 实现) {
            实现.移除演奏界面(玩家.getUniqueId());
        }
        待覆盖乐谱名.remove(玩家.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void 玩家退出(PlayerQuitEvent 事件) {
        乐器服务.玩家退出清理(事件.getPlayer().getUniqueId());
        待覆盖乐谱名.remove(事件.getPlayer().getUniqueId());
    }

    private 乐器定义 解析乐器物品(ItemStack 物品) {
        ItemMeta 元数据 = 物品.getItemMeta();
        if (元数据 == null || !元数据.hasCustomModelDataComponent()) {
            return null;
        }
        CustomModelDataComponent 组件 = 元数据.getCustomModelDataComponent();
        List<Float> floats = 组件.getFloats();
        if (floats.isEmpty()) {
            return null;
        }
        int 模型数据 = floats.get(0).intValue();
        return 乐器注册表.按自定义模型数据查找(模型数据).orElse(null);
    }
}
