package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.玩家服务;
import mljy.领域层.任务.任务分类;
import mljy.领域层.任务.任务定义;
import mljy.领域层.任务.任务进度;
import mljy.领域层.任务.任务状态;
import mljy.领域层.任务.任务奖励;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 任务管理服务实现。
 * 管理玩家任务进度的内存缓存和YAML持久化。
 * 数据文件位置：plugins/XRM/任务数据/{UUID}.yml
 */
@Singleton
public class 任务管理服务实现 implements 任务管理服务 {
    private static final String 数据目录名 = "任务数据";
    private static final String 文件后缀 = ".yml";
    private static final String 配置根键 = "tasks";
    private static final String 键状态 = "状态";
    private static final String 键当前进度 = "当前进度";
    private static final String 键目标进度 = "目标进度";
    private static final String 键接取时间 = "接取时间";
    private static final String 键完成时间 = "完成时间";
    private static final String 键上次重置时间 = "上次重置时间";
    private static final String 默认状态 = "未解锁";

    private final 任务注册服务 任务注册服务;
    private final 玩家服务 玩家服务;
    private final Logger 日志器;
    private final File 数据目录;
    private final Map<UUID, Map<String, 任务进度>> 进度缓存 = new ConcurrentHashMap<>();

    @Inject
    public 任务管理服务实现(任务注册服务 任务注册服务,
                          玩家服务 玩家服务,
                          JavaPlugin 插件) {
        this.任务注册服务 = 任务注册服务;
        this.玩家服务 = 玩家服务;
        this.日志器 = 插件.getLogger();
        this.数据目录 = new File(插件.getDataFolder(), 数据目录名);
        if (!数据目录.exists()) {
            数据目录.mkdirs();
        }
    }

    @Override
    public boolean 接取任务(UUID 玩家标识, String 任务标识) {
        Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
        if (定义可选.isEmpty()) {
            return false;
        }
        任务定义 定义 = 定义可选.get();
        任务进度 进度 = 获取或创建进度(玩家标识, 定义);
        if (进度.获取状态() != 任务状态.可接取 && 进度.获取状态() != 任务状态.未解锁) {
            return false;
        }
        if (!检查前置任务(玩家标识, 定义)) {
            return false;
        }
        if (!检查所需等级(玩家标识, 定义)) {
            return false;
        }
        long 当前时间 = System.currentTimeMillis();
        进度.接取(当前时间);
        保存进度(玩家标识);
        return true;
    }

    @Override
    public boolean 交付任务(UUID 玩家标识, String 任务标识) {
        Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
        if (定义可选.isEmpty()) {
            return false;
        }
        任务定义 定义 = 定义可选.get();
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return false;
        }
        任务进度 进度 = 玩家进度表.get(任务标识);
        if (进度 == null || 进度.获取状态() != 任务状态.可交付) {
            return false;
        }
        long 当前时间 = System.currentTimeMillis();
        进度.完成(当前时间);
        发放奖励(玩家标识, 定义.奖励());
        保存进度(玩家标识);
        检查解锁(玩家标识);
        return true;
    }

    @Override
    public boolean 强制完成任务(UUID 玩家标识, String 任务标识) {
        Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
        if (定义可选.isEmpty()) {
            return false;
        }
        任务定义 定义 = 定义可选.get();
        任务进度 进度 = 获取或创建进度(玩家标识, 定义);
        long 当前时间 = System.currentTimeMillis();
        if (进度.获取状态() != 任务状态.已完成) {
            进度.设置状态(任务状态.可交付);
            进度.完成(当前时间);
            发放奖励(玩家标识, 定义.奖励());
            保存进度(玩家标识);
            检查解锁(玩家标识);
        }
        return true;
    }

    @Override
    public boolean 重置任务(UUID 玩家标识, String 任务标识) {
        Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
        if (定义可选.isEmpty()) {
            return false;
        }
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return true;
        }
        任务进度 进度 = 玩家进度表.get(任务标识);
        if (进度 != null) {
            进度.强制重置();
            保存进度(玩家标识);
        }
        检查解锁(玩家标识);
        return true;
    }

    @Override
    public void 增加进度(UUID 玩家标识, String 任务标识, int 增量) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return;
        }
        任务进度 进度 = 玩家进度表.get(任务标识);
        if (进度 == null || !进度.是否进行中()) {
            return;
        }
        进度.增加进度(增量);
        保存进度(玩家标识);
    }

    @Override
    public void 设置进度(UUID 玩家标识, String 任务标识, int 新进度) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return;
        }
        任务进度 进度 = 玩家进度表.get(任务标识);
        if (进度 == null || !进度.是否进行中()) {
            return;
        }
        进度.更新进度(新进度);
        保存进度(玩家标识);
    }

    @Override
    public Optional<任务进度> 获取进度(UUID 玩家标识, String 任务标识) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(玩家进度表.get(任务标识));
    }

    @Override
    public List<任务进度> 获取所有进度(UUID 玩家标识) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return List.of();
        }
        return List.copyOf(玩家进度表.values());
    }

    @Override
    public List<任务进度> 获取分类进度(UUID 玩家标识, 任务分类 分类) {
        List<任务进度> 结果 = new ArrayList<>();
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return 结果;
        }
        for (任务进度 进度 : 玩家进度表.values()) {
            Optional<任务定义> 定义可选 = 任务注册服务.获取定义(进度.获取任务标识());
            if (定义可选.isPresent() && 定义可选.get().分类() == 分类) {
                结果.add(进度);
            }
        }
        return 结果;
    }

    @Override
    public 任务状态 获取任务状态(UUID 玩家标识, String 任务标识) {
        Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
        if (定义可选.isEmpty()) {
            return 任务状态.未解锁;
        }
        任务定义 定义 = 定义可选.get();
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return 计算初始状态(玩家标识, 定义);
        }
        任务进度 进度 = 玩家进度表.get(任务标识);
        if (进度 == null) {
            return 计算初始状态(玩家标识, 定义);
        }
        return 进度.获取状态();
    }

    @Override
    public void 保存进度(UUID 玩家标识) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return;
        }
        YamlConfiguration 配置 = new YamlConfiguration();
        for (任务进度 进度 : 玩家进度表.values()) {
            String 路径 = 配置根键 + "." + 进度.获取任务标识() + ".";
            配置.set(路径 + 键状态, 进度.获取状态().name());
            配置.set(路径 + 键当前进度, 进度.获取当前进度());
            配置.set(路径 + 键目标进度, 进度.获取目标进度());
            配置.set(路径 + 键接取时间, 进度.获取接取时间());
            配置.set(路径 + 键完成时间, 进度.获取完成时间());
            配置.set(路径 + 键上次重置时间, 进度.获取上次重置时间());
        }
        File 文件 = 获取数据文件(玩家标识);
        try {
            配置.save(文件);
        } catch (IOException 异常) {
            日志器.log(Level.SEVERE, "保存任务数据失败: " + 玩家标识, 异常);
        }
    }

    @Override
    public void 加载进度(UUID 玩家标识) {
        File 文件 = 获取数据文件(玩家标识);
        if (!文件.exists()) {
            进度缓存.put(玩家标识, new ConcurrentHashMap<>());
            return;
        }
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(文件);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        Map<String, 任务进度> 玩家进度表 = new ConcurrentHashMap<>();
        if (根 != null) {
            for (String 任务标识 : 根.getKeys(false)) {
                ConfigurationSection 节 = 根.getConfigurationSection(任务标识);
                if (节 == null) {
                    continue;
                }
                Optional<任务定义> 定义可选 = 任务注册服务.获取定义(任务标识);
                if (定义可选.isEmpty()) {
                    continue;
                }
                任务进度 进度 = 解析进度(玩家标识, 任务标识, 节, 定义可选.get());
                玩家进度表.put(任务标识, 进度);
            }
        }
        进度缓存.put(玩家标识, 玩家进度表);
    }

    @Override
    public void 玩家登录(UUID 玩家标识) {
        加载进度(玩家标识);
        检查解锁(玩家标识);
    }

    @Override
    public void 玩家退出(UUID 玩家标识) {
        保存进度(玩家标识);
        进度缓存.remove(玩家标识);
    }

    @Override
    public void 检查解锁(UUID 玩家标识) {
        Collection<任务定义> 所有定义 = 任务注册服务.获取所有定义();
        Map<String, 任务进度> 玩家进度表 = 进度缓存.computeIfAbsent(玩家标识, k -> new ConcurrentHashMap<>());
        boolean 有变更 = false;
        for (任务定义 定义 : 所有定义) {
            任务进度 进度 = 玩家进度表.get(定义.任务标识());
            if (进度 == null) {
                任务状态 初始状态 = 计算初始状态(玩家标识, 定义);
                if (初始状态 == 任务状态.可接取) {
                    任务进度 新进度 = new 任务进度(玩家标识, 定义.任务标识(), 定义.目标().数量());
                    新进度.设置状态(任务状态.可接取);
                    玩家进度表.put(定义.任务标识(), 新进度);
                    有变更 = true;
                }
            } else if (进度.是否未解锁()) {
                if (检查前置任务(玩家标识, 定义) && 检查所需等级(玩家标识, 定义)) {
                    进度.设置状态(任务状态.可接取);
                    有变更 = true;
                }
            } else if (进度.是否已完成() && 定义.可重复()) {
                // 日常任务每日重置逻辑预留（由定时任务调用重置方法）
            }
        }
        if (有变更) {
            保存进度(玩家标识);
        }
    }

    @Override
    public Optional<任务定义> 获取任务定义(String 任务标识) {
        return 任务注册服务.获取定义(任务标识);
    }

    /**
     * 获取或创建任务进度。如果玩家进度表中不存在，则创建初始进度。
     */
    private 任务进度 获取或创建进度(UUID 玩家标识, 任务定义 定义) {
        Map<String, 任务进度> 玩家进度表 = 进度缓存.computeIfAbsent(玩家标识, k -> new ConcurrentHashMap<>());
        return 玩家进度表.computeIfAbsent(定义.任务标识(), id -> {
            任务进度 新进度 = new 任务进度(玩家标识, id, 定义.目标().数量());
            任务状态 初始状态 = 计算初始状态(玩家标识, 定义);
            新进度.设置状态(初始状态);
            return 新进度;
        });
    }

    /**
     * 计算任务的初始状态（玩家未接取时）。
     * 如果前置任务全部完成 → 可接取
     * 否则 → 未解锁
     */
    private 任务状态 计算初始状态(UUID 玩家标识, 任务定义 定义) {
        if (!检查前置任务(玩家标识, 定义)) {
            return 任务状态.未解锁;
        }
        if (!检查所需等级(玩家标识, 定义)) {
            return 任务状态.未解锁;
        }
        return 任务状态.可接取;
    }

    /**
     * 检查前置任务是否全部完成。
     */
    private boolean 检查前置任务(UUID 玩家标识, 任务定义 定义) {
        if (!定义.是否有前置()) {
            return true;
        }
        Map<String, 任务进度> 玩家进度表 = 进度缓存.get(玩家标识);
        if (玩家进度表 == null) {
            return false;
        }
        for (String 前置ID : 定义.前置任务()) {
            任务进度 前置进度 = 玩家进度表.get(前置ID);
            if (前置进度 == null || !前置进度.是否已完成()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查玩家等级是否达到任务所需等级。
     */
    private boolean 检查所需等级(UUID 玩家标识, 任务定义 定义) {
        if (定义.所需等级() <= 0) {
            return true;
        }
        Optional<mljy.领域层.玩家.玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        return 快照可选.map(快照 -> 快照.等级() >= 定义.所需等级()).orElse(false);
    }

    /**
     * 发放任务奖励。
     * 经验等级通过Player.giveExpLevels发放。
     * 金币和物品奖励预留扩展点（当前仅经验等级）。
     */
    private void 发放奖励(UUID 玩家标识, 任务奖励 奖励) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        if (奖励.经验等级() > 0) {
            玩家.giveExpLevels(奖励.经验等级());
        }
        if (奖励.金币() > 0) {
            // 金币奖励预留：当前版本不直接发放金币（无经济系统依赖）
            // 未来集成经济系统时在此处实现
            日志器.fine("任务金币奖励预留: " + 玩家标识 + " 金币=" + 奖励.金币());
        }
        if (奖励.物品列表() != null && !奖励.物品列表().isEmpty()) {
            PlayerInventory 背包 = 玩家.getInventory();
            for (Map.Entry<String, Integer> 条目 : 奖励.物品列表().entrySet()) {
                Material 材质 = 解析材质(条目.getKey());
                if (材质 != null && 材质 != Material.AIR) {
                    背包.addItem(new ItemStack(材质, 条目.getValue()));
                }
            }
        }
    }

    /**
     * 解析物品材质。
     */
    private Material 解析材质(String 物品标识) {
        if (物品标识 == null || 物品标识.isBlank()) {
            return null;
        }
        try {
            return Material.valueOf(物品标识.toUpperCase());
        } catch (IllegalArgumentException e) {
            日志器.warning("任务奖励物品材质未知: " + 物品标识);
            return null;
        }
    }

    /**
     * 解析任务进度从YAML配置节。
     */
    private 任务进度 解析进度(UUID 玩家标识, String 任务标识, ConfigurationSection 节, 任务定义 定义) {
        任务状态 状态 = 解析状态(节.getString(键状态, 默认状态));
        int 当前进度 = 节.getInt(键当前进度, 0);
        int 目标进度 = 节.getInt(键目标进度, 定义.目标().数量());
        long 接取时间 = 节.getLong(键接取时间, 0L);
        long 完成时间 = 节.getLong(键完成时间, 0L);
        long 上次重置时间 = 节.getLong(键上次重置时间, 0L);
        return new 任务进度(玩家标识, 任务标识, 状态, 当前进度, 目标进度, 接取时间, 完成时间, 上次重置时间);
    }

    /**
     * 解析任务状态从字符串。
     */
    private 任务状态 解析状态(String 状态文本) {
        return switch (状态文本) {
            case "未解锁" -> 任务状态.未解锁;
            case "可接取" -> 任务状态.可接取;
            case "进行中" -> 任务状态.进行中;
            case "可交付" -> 任务状态.可交付;
            case "已完成" -> 任务状态.已完成;
            default -> 任务状态.未解锁;
        };
    }

    /**
     * 获取玩家任务数据文件。
     */
    private File 获取数据文件(UUID 玩家标识) {
        return new File(数据目录, 玩家标识.toString() + 文件后缀);
    }
}
