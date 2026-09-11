package mljy.表现层.计分板;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.mrmicky.fastboard.FastBoard;
import mljy.业务层.技能冷却服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.资源注册服务;
import mljy.业务层.资源变更服务;
import mljy.资源服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.技能.技能按键类型;
import mljy.领域层.技能.技能定义;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class 计分板服务实现 implements 计分板服务 {
    private static final String 专精编号映射配置键 = "技能.专精编号映射";
    private static final String 技能标识分隔符 = "_";
    private static final double 小范围资源上限阈值 = 14.0;
    private static final int 计分板行数 = 10;
    private static final long 每秒刻数 = 20L;

    private static final String 标题颜色 = "§6§l";
    private static final String 资源名颜色 = "§5§l";
    private static final String 总值颜色 = "§f";
    private static final String 空槽位颜色 = "§8";
    private static final String 冷却中颜色 = "§4";

    private static final String 标题翻译键 = "计分板.标题";
    private static final String 资源行翻译键 = "计分板.资源行";
    private static final String 空槽位翻译键 = "计分板.空槽位";
    private static final String 空槽位行翻译键 = "计分板.空槽位行";
    private static final String 自定义技能翻译键 = "计分板.自定义技能";
    private static final String 秒翻译键 = "计分板.秒";
    private static final String 技能就绪行翻译键 = "计分板.技能就绪行";
    private static final String 技能冷却中行翻译键 = "计分板.技能冷却中行";
    private static final String 技能冷却格式翻译键 = "计分板.技能冷却格式";
    private static final String 图标对齐填充字符翻译键 = "计分板.图标对齐填充字符";
    private static final String 资源类型翻译前缀 = "资源类型.";
    private static final String 技能名翻译前缀 = "skill.";
    private static final String 技能名翻译中缀 = ".slot.";
    private static final String 技能名翻译后缀 = ".name";

    private static final Map<Integer, String> 中文数字映射 = Map.of(
            1, "一", 2, "二", 3, "三", 4, "四", 5, "五",
            6, "六", 7, "七", 8, "八", 9, "九"
    );

    private static final Map<技能按键类型, String> 图标映射 = Map.of(
            技能按键类型.第一技能, "⚔",
            技能按键类型.第二技能, "🪓",
            技能按键类型.第三技能, "⛏",
            技能按键类型.第四技能, "🥄",
            技能按键类型.第五技能, "🌾",
            技能按键类型.第六技能, "🔱",
            技能按键类型.第七技能, "🔨",
            技能按键类型.第八技能, "📚",
            技能按键类型.第九技能, "⭐"
    );

    private static final Map<技能按键类型, String> 槽位颜色映射 = Map.of(
            技能按键类型.第一技能, "§c",
            技能按键类型.第二技能, "§6",
            技能按键类型.第三技能, "§e",
            技能按键类型.第四技能, "§a",
            技能按键类型.第五技能, "§b",
            技能按键类型.第六技能, "§9",
            技能按键类型.第七技能, "§5",
            技能按键类型.第八技能, "§7",
            技能按键类型.第九技能, "§f"
    );

    private static final Map<技能按键类型, Integer> 图标像素宽度映射 = Map.of(
            技能按键类型.第一技能, 16,
            技能按键类型.第二技能, 16,
            技能按键类型.第三技能, 16,
            技能按键类型.第四技能, 12,
            技能按键类型.第五技能, 16,
            技能按键类型.第六技能, 16,
            技能按键类型.第七技能, 16,
            技能按键类型.第八技能, 16,
            技能按键类型.第九技能, 14
    );
    private static final int 图标像素宽度上限 = 图标像素宽度映射.values().stream()
            .max(Integer::compareTo).orElse(16);
    private static final int 填充字符像素宽度 = 4;

    private static final 技能按键类型[] 槽位顺序 = {
            技能按键类型.第一技能, 技能按键类型.第二技能, 技能按键类型.第三技能,
            技能按键类型.第四技能, 技能按键类型.第五技能, 技能按键类型.第六技能,
            技能按键类型.第七技能, 技能按键类型.第八技能, 技能按键类型.第九技能
    };

    private final Map<UUID, FastBoard> 计分板表 = new ConcurrentHashMap<>();
    private final Map<String, Integer> 专精编号映射 = new HashMap<>();
    private final Map<UUID, Map<String, BukkitTask>> 技能刷新任务表 = new ConcurrentHashMap<>();

    private final 玩家服务 玩家服务;
    private final 技能冷却服务 技能冷却服务;
    private final 技能注册服务 技能注册服务;
    private final 资源注册服务 资源注册服务;
    private final 资源服务 资源服务;
    private final 资源变更服务 资源变更服务;
    private final 翻译服务 翻译服务;
    private final JavaPlugin 插件;

    @Inject
    public 计分板服务实现(玩家服务 玩家服务, 技能冷却服务 技能冷却服务,
                         技能注册服务 技能注册服务, 资源注册服务 资源注册服务,
                         资源服务 资源服务, 资源变更服务 资源变更服务,
                         翻译服务 翻译服务,
                         JavaPlugin 插件) {
        this.玩家服务 = 玩家服务;
        this.技能冷却服务 = 技能冷却服务;
        this.技能注册服务 = 技能注册服务;
        this.资源注册服务 = 资源注册服务;
        this.资源服务 = 资源服务;
        this.资源变更服务 = 资源变更服务;
        this.翻译服务 = 翻译服务;
        this.插件 = 插件;
        加载专精编号映射();
        绑定独立CD回调();
        绑定资源变更回调();
    }

    private void 加载专精编号映射() {
        FileConfiguration 配置 = 插件.getConfig();
        ConfigurationSection 区段 = 配置.getConfigurationSection(专精编号映射配置键);
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

    private void 绑定独立CD回调() {
        技能冷却服务.设置独立CD开始回调((玩家标识, 技能标识) -> {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                return;
            }
            调试日志器.调试("计分板服务", "独立CD开始回调：玩家=%s 技能=%s", 玩家标识, 技能标识);
            更新技能行(玩家, 技能标识);
            启动技能刷新定时器(玩家标识, 技能标识);
        });

        技能冷却服务.设置独立CD完成回调((玩家标识, 技能标识) -> {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                return;
            }
            调试日志器.调试("计分板服务", "独立CD完成回调：玩家=%s 技能=%s", 玩家标识, 技能标识);
            取消技能刷新定时器(玩家标识, 技能标识);
            更新技能行(玩家, 技能标识);
        });
    }

    private void 启动技能刷新定时器(UUID 玩家标识, String 技能标识) {
        Map<String, BukkitTask> 玩家任务表 = 技能刷新任务表.computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>());
        BukkitTask 旧任务 = 玩家任务表.get(技能标识);
        if (旧任务 != null && !旧任务.isCancelled()) {
            旧任务.cancel();
        }
        BukkitTask 新任务 = 插件.getServer().getScheduler().runTaskTimer(插件, () -> {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                取消技能刷新定时器(玩家标识, 技能标识);
                return;
            }
            更新技能行(玩家, 技能标识);
        }, 每秒刻数, 每秒刻数);
        玩家任务表.put(技能标识, 新任务);
    }

    private void 取消技能刷新定时器(UUID 玩家标识, String 技能标识) {
        Map<String, BukkitTask> 玩家任务表 = 技能刷新任务表.get(玩家标识);
        if (玩家任务表 == null) {
            return;
        }
        BukkitTask 任务 = 玩家任务表.remove(技能标识);
        if (任务 != null && !任务.isCancelled()) {
            任务.cancel();
        }
    }

    private void 绑定资源变更回调() {
        资源变更服务.设置资源变更回调((玩家标识, 资源标识) -> {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                return;
            }
            调试日志器.调试("计分板服务", "资源变更回调：玩家=%s 资源=%s", 玩家标识, 资源标识);
            更新资源行(玩家);
        });
    }

    @Override
    public void 创建计分板(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        if (计分板表.containsKey(玩家标识)) {
            return;
        }
        调试日志器.调试("计分板服务", "创建计分板：玩家=%s", 玩家.getName());
        FastBoard 计分板 = new FastBoard(玩家);
        计分板.updateTitle(标题颜色 + 翻译服务.获取(标题翻译键));
        计分板表.put(玩家标识, 计分板);
        更新计分板(玩家);
    }

    @Override
    public void 删除计分板(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        FastBoard 计分板 = 计分板表.remove(玩家标识);
        if (计分板 != null) {
            计分板.delete();
            调试日志器.调试("计分板服务", "删除计分板：玩家=%s", 玩家.getName());
        }
        Map<String, BukkitTask> 玩家任务表 = 技能刷新任务表.remove(玩家标识);
        if (玩家任务表 != null) {
            玩家任务表.values().forEach(任务 -> { if (!任务.isCancelled()) 任务.cancel(); });
        }
    }

    @Override
    public void 更新计分板(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        FastBoard 计分板 = 计分板表.get(玩家标识);
        if (计分板 == null) {
            return;
        }
        调试日志器.调试("计分板服务", "更新计分板：玩家=%s", 玩家.getName());
        计分板.updateLines(构建行内容(玩家标识));
    }

    @Override
    public void 更新所有计分板() {
        for (Player 玩家 : Bukkit.getOnlinePlayers()) {
            更新计分板(玩家);
        }
    }

    @Override
    public void 更新技能行(Player 玩家, String 技能标识) {
        UUID 玩家标识 = 玩家.getUniqueId();
        FastBoard 计分板 = 计分板表.get(玩家标识);
        if (计分板 == null) {
            return;
        }
        int 槽位编号 = 提取槽位编号(技能标识);
        if (槽位编号 <= 0 || 槽位编号 > 9) {
            return;
        }
        技能按键类型 按键类型 = 获取按键类型By槽位(槽位编号);
        if (按键类型 == null) {
            return;
        }
        String 行内容 = 构建技能行(玩家标识, 按键类型);
        计分板.updateLine(槽位编号, 行内容);
    }

    @Override
    public void 更新资源行(Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        FastBoard 计分板 = 计分板表.get(玩家标识);
        if (计分板 == null) {
            return;
        }
        String 行内容 = 构建资源行(玩家标识);
        计分板.updateLine(0, 行内容);
    }

    private String[] 构建行内容(UUID 玩家标识) {
        String[] 行 = new String[计分板行数];
        行[0] = 构建资源行(玩家标识);
        for (int i = 0; i < 槽位顺序.length; i++) {
            行[i + 1] = 构建技能行(玩家标识, 槽位顺序[i]);
        }
        return 行;
    }

    private String 构建资源行(UUID 玩家标识) {
        Collection<资源定义> 定义列表 = 资源注册服务.获取所有资源定义();
        if (定义列表.isEmpty()) {
            return "";
        }
        资源定义 定义 = 定义列表.iterator().next();
        Optional<资源> 资源可选 = 资源服务.获取资源(玩家标识, 定义.获取资源标识());
        if (资源可选.isEmpty()) {
            return "";
        }
        资源 资源 = 资源可选.get();
        double 当前值 = 资源.获取当前值();
        double 上限 = 资源.获取上限();
        String 资源名 = 获取资源名(定义);
        String 当前值文本;
        String 上限文本;
        if (是个位数上限资源(上限)) {
            当前值文本 = 构建个位数数值文本(当前值);
            上限文本 = 构建个位数数值文本(上限);
        } else {
            当前值文本 = 获取资源数值颜色(当前值, 上限) + 格式化数值(当前值);
            上限文本 = 总值颜色 + 格式化数值(上限);
        }
        return 翻译服务.获取(资源行翻译键,
                资源名颜色 + 资源名,
                当前值文本,
                上限文本);
    }

    private boolean 是个位数上限资源(double 上限) {
        return 上限 == Math.floor(上限) && 上限 >= 0 && 上限 <= 小范围资源上限阈值;
    }

    private String 构建个位数数值文本(double 值) {
        int 上限整数 = (int) 小范围资源上限阈值;
        int 整数值 = (int) Math.round(值);
        if (整数值 < 0) {
            整数值 = 0;
        } else if (整数值 > 上限整数) {
            整数值 = 上限整数;
        }
        return 获取个位数数字颜色(整数值) + 格式化数值(值);
    }

    private String 获取个位数数字颜色(int 数值) {
        switch (数值) {
            case 0: return "§f";
            case 1: return "§1";
            case 2: return "§2";
            case 3: return "§3";
            case 4: return "§4";
            case 5: return "§5";
            case 6: return "§6";
            case 7: return "§7";
            case 8: return "§8";
            case 9: return "§9";
            case 10: return "§a";
            case 11: return "§b";
            case 12: return "§c";
            case 13: return "§d";
            case 14: return "§e";
            default: return "§f";
        }
    }

    private String 获取资源名(资源定义 定义) {
        String 翻译键 = 资源类型翻译前缀 + 定义.获取资源标识();
        String 翻译值 = 翻译服务.获取(翻译键);
        if (翻译值.equals(翻译键)) {
            return 定义.获取名称();
        }
        return 翻译值;
    }

    private String 构建技能行(UUID 玩家标识, 技能按键类型 按键类型) {
        String 槽位颜色 = 槽位颜色映射.get(按键类型);
        String 对齐后图标 = 获取对齐后图标(按键类型);

        if (按键类型 == 技能按键类型.第九技能) {
            String 自定义技能名 = 翻译服务.获取(自定义技能翻译键);
            String 第九技能图标 = 图标映射.get(按键类型);
            String 前缀 = 槽位颜色 + 第九技能图标 + 自定义技能名;
            String 冷却值 = 翻译服务.获取(技能冷却格式翻译键,
                    总值颜色 + "0", 总值颜色 + "0");
            return 翻译服务.获取(空槽位行翻译键, 前缀 + ":" + 冷却值);
        }

        String 空槽位文本 = 翻译服务.获取(空槽位翻译键);
        String 空行前缀 = 空槽位颜色 + 对齐后图标 + 空槽位文本;
        String 空行冷却值 = 翻译服务.获取(技能冷却格式翻译键,
                总值颜色 + "0", 总值颜色 + "0");
        String 空行 = 翻译服务.获取(空槽位行翻译键, 空行前缀 + ":" + 空行冷却值);

        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        if (会话可选.isEmpty()) {
            return 空行;
        }
        String 专精 = 会话可选.get().获取专精();
        Integer 编号 = 专精编号映射.get(专精);
        if (编号 == null) {
            return 空行;
        }

        String 技能标识 = 编号 + 技能标识分隔符 + 按键类型.获取槽位编号();
        Optional<技能定义> 定义可选 = 技能注册服务.获取定义(技能标识);
        if (定义可选.isEmpty()) {
            return 空行;
        }
        技能定义 定义 = 定义可选.get();

        String 技能名 = 获取技能名(专精, 按键类型.获取槽位编号());
        double 剩余 = 技能冷却服务.获取剩余冷却(玩家标识, 技能标识);
        double 总冷却 = 定义.冷却时间();
        int 剩余秒 = (int) Math.round(剩余);
        int 总冷却秒 = (int) Math.round(总冷却);

        if (剩余 > 0) {
            String 颜色 = 冷却中颜色;
            String 图标参数 = 颜色 + 对齐后图标;
            String 技能名参数 = 颜色 + 技能名;
            String 冷却值 = 翻译服务.获取(技能冷却格式翻译键,
                    颜色 + 剩余秒, 颜色 + 总冷却秒);
            return 翻译服务.获取(技能冷却中行翻译键,
                    图标参数, 技能名参数, 冷却值);
        }

        String 图标参数 = 槽位颜色 + 对齐后图标;
        String 技能名参数 = 槽位颜色 + 技能名;
        String 冷却值 = 翻译服务.获取(技能冷却格式翻译键,
                总值颜色 + "0", 总值颜色 + 总冷却秒);
        return 翻译服务.获取(技能就绪行翻译键,
                图标参数, 技能名参数, 冷却值);
    }

    private String 获取对齐后图标(技能按键类型 按键类型) {
        String 图标 = 图标映射.get(按键类型);
        int 当前宽度 = 图标像素宽度映射.getOrDefault(按键类型, 图标像素宽度上限);
        int 需补齐像素 = Math.max(0, 图标像素宽度上限 - 当前宽度);
        if (需补齐像素 == 0) {
            return 图标;
        }
        String 填充字符 = 翻译服务.获取(图标对齐填充字符翻译键);
        if (填充字符 == null || 填充字符.isEmpty()) {
            return 图标;
        }
        int 填充次数 = (int) Math.round((double) 需补齐像素 / 填充字符像素宽度);
        StringBuilder 构建器 = new StringBuilder(图标.length() + 填充次数 * 填充字符.length());
        构建器.append(图标);
        for (int i = 0; i < 填充次数; i++) {
            构建器.append(填充字符);
        }
        return 构建器.toString();
    }

    private String 获取技能名(String 专精, int 槽位编号) {
        String 中文数字 = 中文数字映射.get(槽位编号);
        if (中文数字 == null) {
            return "";
        }
        String 翻译键 = 技能名翻译前缀 + 专精 + 技能名翻译中缀 + 中文数字 + 技能名翻译后缀;
        return 翻译服务.获取(翻译键);
    }

    private String 获取资源数值颜色(double 当前值, double 上限) {
        if (上限 <= 0) {
            return "§0";
        }
        double 比例 = Math.max(0.0, Math.min(1.0, 当前值 / 上限));
        int 索引 = Math.min(14, (int) Math.floor(比例 * 15));
        return "§" + "0123456789abcde".charAt(索引);
    }

    private String 格式化数值(double 值) {
        if (值 == (long) 值) {
            return String.valueOf((long) 值);
        }
        return String.valueOf(值);
    }

    private int 提取槽位编号(String 技能标识) {
        int 下划线索引 = 技能标识.indexOf(技能标识分隔符);
        if (下划线索引 < 0 || 下划线索引 + 1 >= 技能标识.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(技能标识.substring(下划线索引 + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private 技能按键类型 获取按键类型By槽位(int 槽位编号) {
        for (技能按键类型 类型 : 槽位顺序) {
            if (类型.获取槽位编号() == 槽位编号) {
                return 类型;
            }
        }
        return null;
    }
}