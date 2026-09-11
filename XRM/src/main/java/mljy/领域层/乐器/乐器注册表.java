package mljy.领域层.乐器;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FP-13 乐器注册表（配置驱动）。
 * 从 乐器配置.yml 的 乐器 段加载所有 乐器定义 到内存。
 * 替代旧的硬编码 乐器类型 枚举，支持热重载与动态乐器。
 *
 * 同时承载 乐器配置.yml 的 全局 段（听觉范围/最大乐谱数等），
 * 通过 {@link #获取全局参数()} 暴露给业务层。
 *
 * 线程安全：加载时重建不可变 map，查询走快照引用。
 */
public class 乐器注册表 {

    private static final String 乐器段键 = "乐器";
    private static final String 全局段键 = "全局";
    private static final String 键NoteBlockAPI乐器ID = "NoteBlockAPI乐器ID";
    private static final String 键物理机制 = "物理机制";
    private static final int NoteBlockAPI乐器ID未配置 = -1;

    private volatile Map<String, 乐器定义> 按标识 = Collections.emptyMap();
    private volatile Map<Integer, 乐器定义> 按自定义模型数据 = Collections.emptyMap();
    private volatile Map<Integer, 乐器定义> 按NoteBlockAPI乐器ID = Collections.emptyMap();
    private volatile 全局参数 全局 = 全局参数.默认();

    /**
     * 从配置加载所有乐器定义与全局参数。支持热重载：每次调用重建内部表。
     *
     * @param 配置 已加载的 乐器配置.yml
     * @return 加载的乐器数量
     */
    public synchronized int 从配置加载(FileConfiguration 配置) {
        if (配置 == null) {
            return 0;
        }
        this.全局 = 解析全局参数(配置.getConfigurationSection(全局段键));
        ConfigurationSection 乐器段 = 配置.getConfigurationSection(乐器段键);
        if (乐器段 == null) {
            按标识 = Collections.emptyMap();
            按自定义模型数据 = Collections.emptyMap();
            按NoteBlockAPI乐器ID = Collections.emptyMap();
            return 0;
        }
        Map<String, 乐器定义> 新按标识 = new LinkedHashMap<>();
        Map<Integer, 乐器定义> 新按CMD = new LinkedHashMap<>();
        Map<Integer, 乐器定义> 新按NoteBlockAPI = new LinkedHashMap<>();
        for (String 标识 : 乐器段.getKeys(false)) {
            乐器定义 定义 = 解析乐器(标识, 乐器段.getConfigurationSection(标识));
            if (定义 != null) {
                新按标识.put(标识, 定义);
                新按CMD.put(定义.获取自定义模型数据(), 定义);
                定义.获取NoteBlockAPI乐器ID().ifPresent(id -> 新按NoteBlockAPI.put(id, 定义));
            }
        }
        this.按标识 = Collections.unmodifiableMap(新按标识);
        this.按自定义模型数据 = Collections.unmodifiableMap(新按CMD);
        this.按NoteBlockAPI乐器ID = Collections.unmodifiableMap(新按NoteBlockAPI);
        return 新按标识.size();
    }

    /**
     * 获取全局参数（听觉范围/最大乐谱数等）。
     *
     * @return 全局参数快照；未加载返回默认值
     */
    public 全局参数 获取全局参数() {
        return 全局;
    }

    private static 全局参数 解析全局参数(ConfigurationSection 段) {
        if (段 == null) {
            return 全局参数.默认();
        }
        return new 全局参数(
                段.getDouble("听觉范围", 48.0),
                段.getBoolean("音符粒子效果", true),
                段.getInt("最大乐谱存储数", 20),
                段.getInt("最大录制时长秒", 300),
                段.getInt("默认速度BPM", 120),
                段.getInt("默认时值Tick", 4),
                段.getDouble("默认力度", 1.0),
                段.getDouble("默认音量", 1.0),
                段.getInt("合奏最小人数", 2)
        );
    }

    /**
     * 按标识查找乐器定义。
     *
     * @param 标识 乐器标识（配置键名，如 钢琴）
     * @return 乐器定义；未找到返回 Optional.empty()
     */
    public Optional<乐器定义> 查找(String 标识) {
        if (标识 == null || 标识.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(按标识.get(标识));
    }

    /**
     * 按显示名称或标识查找（兼容旧 乐器类型.根据名称查找 两种匹配）。
     *
     * @param 名称 显示名称或标识
     * @return 乐器定义；未找到返回 Optional.empty()
     */
    public Optional<乐器定义> 按名称查找(String 名称) {
        if (名称 == null || 名称.isBlank()) {
            return Optional.empty();
        }
        乐器定义 直接 = 按标识.get(名称);
        if (直接 != null) {
            return Optional.of(直接);
        }
        for (乐器定义 定义 : 按标识.values()) {
            if (定义.获取显示名称().equals(名称)) {
                return Optional.of(定义);
            }
        }
        return Optional.empty();
    }

    /**
     * 按自定义模型数据查找（用于从乐器物品反查乐器定义）。
     *
     * @param 模型数据 CustomModelData 值
     * @return 乐器定义；未找到返回 Optional.empty()
     */
    public Optional<乐器定义> 按自定义模型数据查找(int 模型数据) {
        return Optional.ofNullable(按自定义模型数据.get(模型数据));
    }

    /**
     * 按 NoteBlockAPI 乐器 ID 查找（用于 NBS 播放引擎选乐器）。
     *
     * @param 乐器ID NoteBlockAPI 乐器 ID（0-17）
     * @return 乐器定义；未找到返回 Optional.empty()
     */
    public Optional<乐器定义> 按NoteBlockAPI乐器ID查找(int 乐器ID) {
        return Optional.ofNullable(按NoteBlockAPI乐器ID.get(乐器ID));
    }

    /**
     * 获取所有已加载乐器定义。
     *
     * @return 不可变集合
     */
    public Collection<乐器定义> 全部() {
        return 按标识.values();
    }

    /**
     * 获取所有乐器标识（用于 tab 补全）。
     *
     * @return 列表
     */
    public List<String> 全部标识() {
        return new ArrayList<>(按标识.keySet());
    }

    /**
     * 获取所有乐器显示名称（用于 tab 补全）。
     *
     * @return 列表
     */
    public List<String> 全部显示名称() {
        List<String> 列表 = new ArrayList<>(按标识.size());
        for (乐器定义 定义 : 按标识.values()) {
            列表.add(定义.获取显示名称());
        }
        return 列表;
    }

    /**
     * 获取默认乐器（第一个加载的，通常为钢琴）。
     *
     * @return 默认乐器定义；注册表为空返回 Optional.empty()
     */
    public Optional<乐器定义> 默认乐器() {
        return 按标识.values().stream().findFirst();
    }

    /**
     * 已加载乐器数量。
     *
     * @return 数量
     */
    public int 数量() {
        return 按标识.size();
    }

    private 乐器定义 解析乐器(String 标识, ConfigurationSection 段) {
        if (段 == null) {
            return null;
        }
        String 显示名称 = 段.getString("显示名称", 标识);
        Sound 音色 = 解析音色(段.getString("原版音色", 段.getString("音色", "BLOCK_NOTE_BLOCK_HARP")));
        String 资源包键 = 段.getString("资源包音色", null);
        Material 材质 = 解析材质(段.getString("物品材质", "STICK"));
        int 自定义模型数据 = 段.getInt("自定义模型数据", 0);
        int 默认音域 = 段.getInt("默认音域", 段.getInt("默认八度", 0));
        int 音域下限 = 段.getInt("音域下限", 0);
        int 音域上限 = 段.getInt("音域上限", 24);
        int 八度偏移 = 段.getInt("八度偏移", 0);
        boolean 力度敏感 = 段.getBoolean("力度敏感", true);
        String 描述 = 段.getString("描述", "");
        int nbid原始 = 段.getInt(键NoteBlockAPI乐器ID, NoteBlockAPI乐器ID未配置);
        Integer NoteBlockAPI乐器ID = nbid原始 == NoteBlockAPI乐器ID未配置 ? null : nbid原始;
        String 物理机制 = 段.getString(键物理机制, 乐器定义.物理机制_瞬时触发);
        return new 乐器定义(标识, 显示名称, 音色, 资源包键, 材质,
                自定义模型数据, 默认音域, 音域下限, 音域上限,
                八度偏移, 力度敏感, 描述, NoteBlockAPI乐器ID, 物理机制);
    }

    /**
     * 遍历比较解析音色，避开 Sound.valueOf 内部的 Bukkit.getUnsafe() 调用（测试环境无服务器）。
     */
    @SuppressWarnings({"deprecation", "removal"})
    static Sound 解析音色(String 名称) {
        if (名称 == null || 名称.isBlank()) {
            return Sound.BLOCK_NOTE_BLOCK_HARP;
        }
        for (Sound 候选 : Sound.values()) {
            if (候选.name().equals(名称)) {
                return 候选;
            }
        }
        return Sound.BLOCK_NOTE_BLOCK_HARP;
    }

    /**
     * 遍历比较解析材质，避开 Material.valueOf 在测试环境的潜在问题。
     */
    static Material 解析材质(String 名称) {
        if (名称 == null || 名称.isBlank()) {
            return Material.STICK;
        }
        for (Material 候选 : Material.values()) {
            if (候选.name().equals(名称)) {
                return 候选;
            }
        }
        return Material.STICK;
    }

    /**
     * 全局参数（不可变值对象）。
     * 承载 乐器配置.yml 的 全局 段，供业务层读取。
     */
    public static final class 全局参数 {
        private static final 全局参数 默认实例 = new 全局参数(
                48.0, true, 20, 300, 120, 4, 1.0, 1.0, 2
        );

        private final double 听觉范围;
        private final boolean 音符粒子效果;
        private final int 最大乐谱存储数;
        private final int 最大录制时长秒;
        private final int 默认速度BPM;
        private final int 默认时值Tick;
        private final double 默认力度;
        private final double 默认音量;
        private final int 合奏最小人数;

        private 全局参数(double 听觉范围, boolean 音符粒子效果, int 最大乐谱存储数,
                         int 最大录制时长秒, int 默认速度BPM, int 默认时值Tick,
                         double 默认力度, double 默认音量, int 合奏最小人数) {
            this.听觉范围 = 听觉范围;
            this.音符粒子效果 = 音符粒子效果;
            this.最大乐谱存储数 = 最大乐谱存储数;
            this.最大录制时长秒 = 最大录制时长秒;
            this.默认速度BPM = 默认速度BPM;
            this.默认时值Tick = 默认时值Tick;
            this.默认力度 = 默认力度;
            this.默认音量 = 默认音量;
            this.合奏最小人数 = 合奏最小人数;
        }

        public static 全局参数 默认() {
            return 默认实例;
        }

        public double 获取听觉范围() {
            return 听觉范围;
        }

        public boolean 是否音符粒子效果() {
            return 音符粒子效果;
        }

        public int 获取最大乐谱存储数() {
            return 最大乐谱存储数;
        }

        public int 获取最大录制时长秒() {
            return 最大录制时长秒;
        }

        public int 获取默认速度BPM() {
            return 默认速度BPM;
        }

        public int 获取默认时值Tick() {
            return 默认时值Tick;
        }

        public double 获取默认力度() {
            return 默认力度;
        }

        public double 获取默认音量() {
            return 默认音量;
        }

        public int 获取合奏最小人数() {
            return 合奏最小人数;
        }
    }
}
