package mljy.基础设施层.乐器;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.音符;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-18 公共乐谱库仓储。
 * YAML 文件存储，路径：plugins/XRM/公共乐谱/{乐谱ID}.yml。
 * 每个文件包含原乐谱字段 + 上传者 + 审核状态 + 评分聚合 + 下载次数。
 * <p>
 * 设计：
 * - 文件名为乐谱ID + ".yml"
 * - 内存中无缓存，每次操作直接读写文件（公共乐谱库为低频操作，无需缓存）
 * - 线程安全：依赖 YamlConfiguration 的线程安全保证 + 单文件原子写
 */
@Singleton
public class 公共乐谱库仓储 {

    private static final String 公共乐谱目录名 = "公共乐谱库";
    private static final String 乐谱ID键 = "乐谱ID";
    private static final String 名称键 = "名称";
    private static final String 作者键 = "作者";
    private static final String 作者名键 = "作者名";
    private static final String 创建时间键 = "创建时间";
    private static final String 速度键 = "速度BPM";
    private static final String 音符列表键 = "音符列表";
    private static final String 音高键 = "音高";
    private static final String 时值键 = "时值Tick";
    private static final String 力度键 = "力度";
    private static final String 上传者键 = "上传者";
    private static final String 上传者名键 = "上传者名";
    private static final String 上传时间键 = "上传时间";
    private static final String 状态键 = "审核状态";
    private static final String 评分总和键 = "评分总和";
    private static final String 评分人数键 = "评分人数";
    private static final String 下载次数键 = "下载次数";

    private final JavaPlugin 插件;

    @Inject
    public 公共乐谱库仓储(JavaPlugin 插件) {
        this.插件 = 插件;
    }

    private File 获取目录() {
        return new File(插件.getDataFolder(), 公共乐谱目录名);
    }

    private File 获取文件(String 乐谱ID) {
        return new File(获取目录(), 乐谱ID + ".yml");
    }

    private void 创建父目录(File 文件) {
        File 父目录 = 文件.getParentFile();
        if (父目录 != null && !父目录.exists()) {
            父目录.mkdirs();
        }
    }

    /**
     * 保存公共乐谱到文件。
     *
     * @param 乐谱 公共乐谱
     * @return true=成功；false=失败
     */
    public boolean 保存(公共乐谱 乐谱) {
        if (乐谱 == null || 乐谱.获取乐谱ID() == null || 乐谱.获取乐谱ID().isBlank()) {
            return false;
        }
        File 文件 = 获取文件(乐谱.获取乐谱ID());
        创建父目录(文件);
        YamlConfiguration 配置 = new YamlConfiguration();
        配置.set(乐谱ID键, 乐谱.获取乐谱ID());
        乐谱 原乐谱 = 乐谱.获取原乐谱();
        配置.set(名称键, 原乐谱.获取名称());
        配置.set(作者键, 原乐谱.获取作者标识().toString());
        配置.set(作者名键, 原乐谱.获取作者名());
        配置.set(创建时间键, 原乐谱.获取创建时间戳());
        配置.set(速度键, 原乐谱.获取速度BPM());
        List<Map<String, Object>> 音符数据列表 = new ArrayList<>();
        for (音符 音符 : 原乐谱.获取音符列表()) {
            Map<String, Object> 音符数据 = new HashMap<>();
            音符数据.put(音高键, 音符.获取音高());
            音符数据.put(时值键, 音符.获取时值Tick());
            音符数据.put(力度键, 音符.获取力度());
            音符数据列表.add(音符数据);
        }
        配置.set(音符列表键, 音符数据列表);
        配置.set(上传者键, 乐谱.获取上传者标识().toString());
        配置.set(上传者名键, 乐谱.获取上传者名());
        配置.set(上传时间键, 乐谱.获取上传时间戳());
        配置.set(状态键, 乐谱.获取状态().name());
        配置.set(评分总和键, 乐谱.获取评分总和());
        配置.set(评分人数键, 乐谱.获取评分人数());
        配置.set(下载次数键, 乐谱.获取下载次数());
        try {
            配置.save(文件);
            return true;
        } catch (IOException e) {
            插件.getLogger().warning("保存公共乐谱失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 按乐谱ID加载公共乐谱。
     *
     * @param 乐谱ID 乐谱ID
     * @return 公共乐谱；不存在或损坏返回 empty
     */
    public Optional<公共乐谱> 加载(String 乐谱ID) {
        if (乐谱ID == null || 乐谱ID.isBlank()) {
            return Optional.empty();
        }
        File 文件 = 获取文件(乐谱ID);
        if (!文件.exists()) {
            return Optional.empty();
        }
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(文件);
        String 名称 = 配置.getString(名称键, 乐谱ID);
        UUID 作者标识;
        try {
            作者标识 = UUID.fromString(配置.getString(作者键, new UUID(0L, 0L).toString()));
        } catch (IllegalArgumentException e) {
            作者标识 = new UUID(0L, 0L);
        }
        String 作者名 = 配置.getString(作者名键, "");
        long 创建时间 = 配置.getLong(创建时间键, System.currentTimeMillis());
        int 速度 = 配置.getInt(速度键, 120);
        List<音符> 音符列表 = 解析音符列表(配置);
        乐谱 原乐谱 = new 乐谱(名称, 作者标识, 作者名, 创建时间, 速度, 音符列表);
        UUID 上传者标识;
        try {
            上传者标识 = UUID.fromString(配置.getString(上传者键, new UUID(0L, 0L).toString()));
        } catch (IllegalArgumentException e) {
            上传者标识 = new UUID(0L, 0L);
        }
        String 上传者名 = 配置.getString(上传者名键, "");
        long 上传时间 = 配置.getLong(上传时间键, System.currentTimeMillis());
        审核状态 状态 = 解析状态(配置.getString(状态键, 审核状态.待审核.name()));
        long 评分总和 = 配置.getLong(评分总和键, 0L);
        long 评分人数 = 配置.getLong(评分人数键, 0L);
        long 下载次数 = 配置.getLong(下载次数键, 0L);
        return Optional.of(new 公共乐谱(乐谱ID, 原乐谱, 上传者标识, 上传者名,
                上传时间, 状态, 评分总和, 评分人数, 下载次数));
    }

    private List<音符> 解析音符列表(YamlConfiguration 配置) {
        List<Map<?, ?>> 音符数据列表 = 配置.getMapList(音符列表键);
        List<音符> 音符列表 = new ArrayList<>();
        for (Map<?, ?> 音符数据 : 音符数据列表) {
            Object 音高值 = 音符数据.get(音高键);
            Object 时值值 = 音符数据.get(时值键);
            Object 力度值 = 音符数据.get(力度键);
            if (音高值 == null || 时值值 == null || 力度值 == null) {
                continue;
            }
            try {
                int 音高 = ((Number) 音高值).intValue();
                int 时值 = ((Number) 时值值).intValue();
                double 力度 = ((Number) 力度值).doubleValue();
                音符列表.add(音符.of(音高, 时值, 力度));
            } catch (ClassCastException ignored) {
                // 跳过损坏条目
            }
        }
        return 音符列表;
    }

    private 审核状态 解析状态(String 状态名) {
        if (状态名 == null) {
            return 审核状态.待审核;
        }
        try {
            return 审核状态.valueOf(状态名);
        } catch (IllegalArgumentException e) {
            return 审核状态.待审核;
        }
    }

    /**
     * 删除公共乐谱。
     *
     * @param 乐谱ID 乐谱ID
     * @return true=已删除；false=不存在或删除失败
     */
    public boolean 删除(String 乐谱ID) {
        if (乐谱ID == null || 乐谱ID.isBlank()) {
            return false;
        }
        File 文件 = 获取文件(乐谱ID);
        return 文件.delete();
    }

    /**
     * 列出所有公共乐谱ID（已通过审核 + 待审核 + 已拒绝）。
     *
     * @return 乐谱ID列表（按字典序）
     */
    public List<String> 列出所有() {
        File 目录 = 获取目录();
        if (!目录.exists()) {
            return Collections.emptyList();
        }
        File[] 文件列表 = 目录.listFiles((dir, name) -> name.endsWith(".yml"));
        if (文件列表 == null || 文件列表.length == 0) {
            return Collections.emptyList();
        }
        List<String> 结果 = new ArrayList<>();
        for (File 文件 : 文件列表) {
            String 文件名 = 文件.getName();
            结果.add(文件名.substring(0, 文件名.length() - 4));
        }
        Collections.sort(结果);
        return 结果;
    }

    /**
     * 列出指定状态的公共乐谱ID。
     *
     * @param 状态 审核状态
     * @return 乐谱ID列表
     */
    public List<String> 列出按状态(审核状态 状态) {
        if (状态 == null) {
            return Collections.emptyList();
        }
        List<String> 全部 = 列出所有();
        List<String> 结果 = new ArrayList<>();
        for (String 乐谱ID : 全部) {
            Optional<公共乐谱> 乐谱 = 加载(乐谱ID);
            if (乐谱.isPresent() && 乐谱.get().获取状态() == 状态) {
                结果.add(乐谱ID);
            }
        }
        return 结果;
    }
}
