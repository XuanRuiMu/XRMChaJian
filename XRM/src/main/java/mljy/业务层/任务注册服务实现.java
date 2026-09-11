package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.Yaml配置加载器;
import mljy.领域层.任务.任务分类;
import mljy.领域层.任务.任务定义;
import mljy.领域层.任务.任务目标;
import mljy.领域层.任务.任务目标类型;
import mljy.领域层.任务.任务奖励;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 任务注册服务实现（YAML配置驱动）。
 * 从"任务定义.yml"加载所有任务定义，按任务标识索引。
 * 配置结构：
 *   tasks:
 *     <任务标识>:
 *       分类: 主线|支线|日常
 *       名称翻译键: quest.<id>.name
 *       描述翻译键: quest.<id>.description
 *       所需等级: 0
 *       可重复: false
 *       前置任务: [任务ID1, 任务ID2]
 *       接取NPC: NPC名称
 *       交付NPC: NPC名称
 *       目标:
 *         类型: 击杀实体|收集物品|到达位置|对话NPC|使用技能|条件检查|护送
 *         目标ID: ZOMBIE
 *         数量: 5
 *       奖励:
 *         经验等级: 1
 *         金币: 100
 *         物品:
 *           IRON_INGOT: 10
 *       接取对话键: quest.<id>.dialog.accept
 *       进行中对话键: quest.<id>.dialog.progress
 *       可交付对话键: quest.<id>.dialog.deliver
 *       完成后对话键: quest.<id>.dialog.complete
 */
@Singleton
public class 任务注册服务实现 implements 任务注册服务 {
    private static final String 配置文件名 = "任务定义.yml";
    private static final String 配置根键 = "tasks";
    private static final String 键分类 = "分类";
    private static final String 键名称翻译键 = "名称翻译键";
    private static final String 键描述翻译键 = "描述翻译键";
    private static final String 键所需等级 = "所需等级";
    private static final String 键可重复 = "可重复";
    private static final String 键前置任务 = "前置任务";
    private static final String 键接取NPC = "接取NPC";
    private static final String 键交付NPC = "交付NPC";
    private static final String 键目标 = "目标";
    private static final String 键奖励 = "奖励";
    private static final String 键接取对话键 = "接取对话键";
    private static final String 键进行中对话键 = "进行中对话键";
    private static final String 键可交付对话键 = "可交付对话键";
    private static final String 键完成后对话键 = "完成后对话键";
    private static final String 键类型 = "类型";
    private static final String 键目标ID = "目标ID";
    private static final String 键数量 = "数量";
    private static final String 键经验等级 = "经验等级";
    private static final String 键金币 = "金币";
    private static final String 键物品 = "物品";
    private static final int 默认数量 = 1;
    private static final int 默认经验等级 = 0;
    private static final int 默认金币 = 0;
    private static final int 默认所需等级 = 0;
    private static final boolean 默认可重复 = false;

    private final Yaml配置加载器 配置加载器;
    private final Logger 日志器;
    private final Map<String, 任务定义> 任务定义表 = new ConcurrentHashMap<>();
    private final Map<String, List<任务定义>> NPC接取索引 = new ConcurrentHashMap<>();
    private final Map<String, List<任务定义>> NPC交付索引 = new ConcurrentHashMap<>();

    @Inject
    public 任务注册服务实现(Yaml配置加载器 配置加载器, JavaPlugin 插件) {
        this.配置加载器 = 配置加载器;
        this.日志器 = 插件.getLogger();
        加载任务定义();
    }

    private void 加载任务定义() {
        任务定义表.clear();
        NPC接取索引.clear();
        NPC交付索引.clear();
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            日志器.warning("任务定义配置为空或缺失: " + 配置文件名);
            return;
        }
        for (String 任务标识 : 根.getKeys(false)) {
            ConfigurationSection 节 = 根.getConfigurationSection(任务标识);
            if (节 == null) {
                continue;
            }
            try {
                任务定义 定义 = 解析任务定义(任务标识, 节);
                任务定义表.put(任务标识, 定义);
                索引NPC(定义);
            } catch (Exception 异常) {
                日志器.log(Level.WARNING, "加载任务定义失败: " + 任务标识, 异常);
            }
        }
        日志器.info("已加载任务定义数量: " + 任务定义表.size());
    }

    private 任务定义 解析任务定义(String 任务标识, ConfigurationSection 节) {
        任务分类 分类 = 解析分类(节.getString(键分类, "主线"), 任务标识);
        String 名称翻译键 = 节.getString(键名称翻译键, "quest." + 任务标识 + ".name");
        String 描述翻译键 = 节.getString(键描述翻译键, "quest." + 任务标识 + ".description");
        int 所需等级 = 节.getInt(键所需等级, 默认所需等级);
        boolean 可重复 = 节.getBoolean(键可重复, 默认可重复);
        List<String> 前置任务 = 节.getStringList(键前置任务);
        String 接取NPC = 节.getString(键接取NPC, "");
        String 交付NPC = 节.getString(键交付NPC, "");
        任务目标 目标 = 解析任务目标(节.getConfigurationSection(键目标), 任务标识);
        任务奖励 奖励 = 解析任务奖励(节.getConfigurationSection(键奖励), 任务标识);
        String 接取对话键 = 节.getString(键接取对话键, "");
        String 进行中对话键 = 节.getString(键进行中对话键, "");
        String 可交付对话键 = 节.getString(键可交付对话键, "");
        String 完成后对话键 = 节.getString(键完成后对话键, "");

        return new 任务定义(
                任务标识, 分类, 名称翻译键, 描述翻译键,
                目标, 奖励,
                Collections.unmodifiableList(new ArrayList<>(前置任务)),
                接取NPC, 交付NPC,
                所需等级, 可重复,
                接取对话键, 进行中对话键, 可交付对话键, 完成后对话键
        );
    }

    private 任务分类 解析分类(String 分类文本, String 任务标识) {
        return switch (分类文本) {
            case "主线" -> 任务分类.主线;
            case "支线" -> 任务分类.支线;
            case "日常" -> 任务分类.日常;
            default -> {
                日志器.warning("任务 " + 任务标识 + " 分类未知: " + 分类文本 + "，默认为主线");
                yield 任务分类.主线;
            }
        };
    }

    private 任务目标 解析任务目标(ConfigurationSection 节, String 任务标识) {
        if (节 == null) {
            日志器.warning("任务 " + 任务标识 + " 缺少目标配置，使用默认对话NPC目标");
            return 任务目标.单次(任务目标类型.对话NPC, "");
        }
        String 类型文本 = 节.getString(键类型, "对话NPC");
        任务目标类型 类型 = 解析目标类型(类型文本, 任务标识);
        String 目标ID = 节.getString(键目标ID, "");
        int 数量 = 节.getInt(键数量, 默认数量);
        return new 任务目标(类型, 目标ID, 数量);
    }

    private 任务目标类型 解析目标类型(String 类型文本, String 任务标识) {
        return switch (类型文本) {
            case "击杀实体" -> 任务目标类型.击杀实体;
            case "收集物品" -> 任务目标类型.收集物品;
            case "到达位置" -> 任务目标类型.到达位置;
            case "对话NPC" -> 任务目标类型.对话NPC;
            case "使用技能" -> 任务目标类型.使用技能;
            case "条件检查" -> 任务目标类型.条件检查;
            case "护送" -> 任务目标类型.护送;
            default -> {
                日志器.warning("任务 " + 任务标识 + " 目标类型未知: " + 类型文本 + "，默认为对话NPC");
                yield 任务目标类型.对话NPC;
            }
        };
    }

    private 任务奖励 解析任务奖励(ConfigurationSection 节, String 任务标识) {
        if (节 == null) {
            return 任务奖励.空;
        }
        int 经验等级 = 节.getInt(键经验等级, 默认经验等级);
        int 金币 = 节.getInt(键金币, 默认金币);
        Map<String, Integer> 物品表 = new LinkedHashMap<>();
        ConfigurationSection 物品节 = 节.getConfigurationSection(键物品);
        if (物品节 != null) {
            for (String 物品标识 : 物品节.getKeys(false)) {
                int 数量 = 物品节.getInt(物品标识, 1);
                物品表.put(物品标识, 数量);
            }
        }
        return 任务奖励.完整(经验等级, 金币, 物品表);
    }

    private void 索引NPC(任务定义 定义) {
        if (定义.接取NPC() != null && !定义.接取NPC().isBlank()) {
            NPC接取索引.computeIfAbsent(定义.接取NPC(), k -> new CopyOnWriteArrayList<>()).add(定义);
        }
        if (定义.交付NPC() != null && !定义.交付NPC().isBlank()) {
            NPC交付索引.computeIfAbsent(定义.交付NPC(), k -> new CopyOnWriteArrayList<>()).add(定义);
        }
    }

    @Override
    public void 注册(任务定义 定义) {
        任务定义表.put(定义.任务标识(), 定义);
        索引NPC(定义);
    }

    @Override
    public Optional<任务定义> 获取定义(String 任务标识) {
        return Optional.ofNullable(任务定义表.get(任务标识));
    }

    @Override
    public Collection<任务定义> 获取所有定义() {
        return Collections.unmodifiableCollection(任务定义表.values());
    }

    @Override
    public List<任务定义> 获取分类任务(任务分类 分类) {
        List<任务定义> 结果 = new ArrayList<>();
        for (任务定义 定义 : 任务定义表.values()) {
            if (定义.分类() == 分类) {
                结果.add(定义);
            }
        }
        return Collections.unmodifiableList(结果);
    }

    @Override
    public boolean 存在(String 任务标识) {
        return 任务定义表.containsKey(任务标识);
    }

    @Override
    public List<任务定义> 获取NPC接取任务(String NPC名称) {
        if (NPC名称 == null || NPC名称.isBlank()) {
            return List.of();
        }
        return Collections.unmodifiableList(NPC接取索引.getOrDefault(NPC名称, List.of()));
    }

    @Override
    public List<任务定义> 获取NPC交付任务(String NPC名称) {
        if (NPC名称 == null || NPC名称.isBlank()) {
            return List.of();
        }
        return Collections.unmodifiableList(NPC交付索引.getOrDefault(NPC名称, List.of()));
    }

    @Override
    public void 重载() {
        加载任务定义();
    }
}
