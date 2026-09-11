package mljy.领域层.乐器;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * FP-06 逐音域完全独立键位绑定表。
 * 5 音域 × 12 键 = 60 项配置，每个音域拥有独立的完整键位映射表。
 * <p>
 * 默认填充规则（八度平移）：
 * <ul>
 *   <li>音域 k 的根音 = 基准根音（音域 0）+ k × 12 个半音</li>
 *   <li>音域 0 是基础，音域 1-4 自动平移 +12 +24 +36 +48</li>
 *   <li>音域 0 的键位表必须显式配置；音域 1-4 可省略，回退到八度平移</li>
 * </ul>
 * 逐音域覆盖（FP-06.3）：
 * <ul>
 *   <li>玩家可在任意音域 k 显式指定某键的音高，覆盖默认的八度平移结果</li>
 *   <li>未显式指定的键仍按八度平移</li>
 * </ul>
 * <p>
 * 默认键位流派：Apple Musical Typing（A S D F G H J 白键 + W E T Y U 黑键）。
 * 实际键位由 乐器配置.yml 的「键位绑定」段配置，本类仅承载加载结果与查询。
 * <p>
 * 不可变值对象，加载后线程安全。
 */
public final class 键位绑定表 {

    public static final int 音域数量 = 5;
    public static final int 每音域键数 = 12;

    private static final String 键位绑定段键 = "键位绑定";
    private static final String 音域前缀 = "音域";

    private final Map<Integer, Map<String, Integer>> 音域键位映射;
    private final int 实际音域数量;

    private 键位绑定表(Map<Integer, Map<String, Integer>> 音域键位映射, int 实际音域数量) {
        this.音域键位映射 = Collections.unmodifiableMap(音域键位映射);
        this.实际音域数量 = 实际音域数量;
    }

    /**
     * 从配置加载键位绑定表。
     * 配置结构：
     * <pre>
     * 键位绑定:
     *   音域0:
     *     a: 0
     *     w: 1
     *     ...
     *   音域1:
     *     a: 14  # 显式覆盖（默认应为 12）
     *   音域2: {}  # 空，全部回退到八度平移
     * </pre>
     * 音域 0 必须显式配置；音域 1-4 未配置或为空时按八度平移自动填充。
     *
     * @param 配置 已加载的乐器配置（乐器配置.yml）
     * @return 键位绑定表；配置无 键位绑定 段时返回 默认表()
     */
    public static 键位绑定表 从配置加载(FileConfiguration 配置) {
        if (配置 == null) {
            return 默认表();
        }
        ConfigurationSection 绑定段 = 配置.getConfigurationSection(键位绑定段键);
        if (绑定段 == null) {
            return 默认表();
        }
        Map<Integer, Map<String, Integer>> 结果 = new LinkedHashMap<>();
        Map<String, Integer> 音域0 = null;
        for (int i = 0; i < 音域数量; i++) {
            String 段名 = 音域前缀 + i;
            ConfigurationSection 音域段 = 绑定段.getConfigurationSection(段名);
            Map<String, Integer> 音域映射 = new LinkedHashMap<>();
            if (音域段 != null) {
                for (String 键 : 音域段.getKeys(false)) {
                    int 音高 = 音域段.getInt(键, -1);
                    if (音高 >= 0 && 音高 < 每音域键数 * 音域数量) {
                        音域映射.put(键.toLowerCase(), 音高);
                    }
                }
            }
            if (i == 0) {
                音域0 = 音域映射;
                if (音域0.isEmpty()) {
                    音域0 = 默认音域0();
                }
                结果.put(0, Collections.unmodifiableMap(音域0));
            } else {
                Map<String, Integer> 填充 = 八度平移填充(音域0, 音域映射, i);
                结果.put(i, Collections.unmodifiableMap(填充));
            }
        }
        return new 键位绑定表(结果, 音域数量);
    }

    /**
     * 默认键位绑定表（Apple Musical Typing 流派）。
     * 音域 0：A=0, S=1, D=2, F=3, G=4, H=5, J=6, W=7, E=8, T=9, Y=10, U=11
     * 音域 1-4：按八度平移自动填充
     */
    public static 键位绑定表 默认表() {
        Map<String, Integer> 音域0 = 默认音域0();
        Map<Integer, Map<String, Integer>> 结果 = new LinkedHashMap<>();
        结果.put(0, Collections.unmodifiableMap(音域0));
        for (int i = 1; i < 音域数量; i++) {
            结果.put(i, Collections.unmodifiableMap(八度平移填充(音域0, Map.of(), i)));
        }
        return new 键位绑定表(结果, 音域数量);
    }

    private static Map<String, Integer> 默认音域0() {
        Map<String, Integer> 表 = new LinkedHashMap<>();
        表.put("a", 0);
        表.put("s", 1);
        表.put("d", 2);
        表.put("f", 3);
        表.put("g", 4);
        表.put("h", 5);
        表.put("j", 6);
        表.put("w", 7);
        表.put("e", 8);
        表.put("t", 9);
        表.put("y", 10);
        表.put("u", 11);
        return 表;
    }

    private static Map<String, Integer> 八度平移填充(Map<String, Integer> 基准表,
                                                    Map<String, Integer> 显式覆盖,
                                                    int 音域编号) {
        Map<String, Integer> 结果 = new LinkedHashMap<>();
        int 偏移 = 音域编号 * 每音域键数;
        for (Map.Entry<String, Integer> 条目 : 基准表.entrySet()) {
            String 键 = 条目.getKey();
            int 平移音高 = 条目.getValue() + 偏移;
            int 最终音高 = 显式覆盖.getOrDefault(键, 平移音高);
            结果.put(键, 最终音高);
        }
        return 结果;
    }

    /**
     * 查询指定音域中某键对应的音高。
     *
     * @param 音域 音域编号 0-{实际音域数量-1}
     * @param 键   键名（如 "a" "s" "w"），大小写不敏感
     * @return 音高 Optional；音域或键不存在返回 Optional.empty()
     */
    public OptionalInt 查询音高(int 音域, String 键) {
        if (键 == null || 键.isBlank()) {
            return OptionalInt.empty();
        }
        Map<String, Integer> 映射 = 音域键位映射.get(音域);
        if (映射 == null) {
            return OptionalInt.empty();
        }
        Integer 音高 = 映射.get(键.toLowerCase());
        return 音高 == null ? OptionalInt.empty() : OptionalInt.of(音高);
    }

    /**
     * 获取指定音域的完整键位映射（不可变快照）。
     *
     * @param 音域 音域编号
     * @return 不可变 Map；音域不存在返回空 Map
     */
    public Map<String, Integer> 获取音域映射(int 音域) {
        return 音域键位映射.getOrDefault(音域, Collections.emptyMap());
    }

    /**
     * 实际配置的音域数量（通常为 5）。
     */
    public int 获取音域数量() {
        return 实际音域数量;
    }

    /**
     * 判断指定音域是否有效（0 ≤ 音域 < 实际音域数量）。
     */
    public boolean 音域有效(int 音域) {
        return 音域 >= 0 && 音域 < 实际音域数量;
    }

    /**
     * 将音域 clamp 到有效范围 [0, 实际音域数量-1]。
     */
    public int 规范化音域(int 音域) {
        return Math.max(0, Math.min(实际音域数量 - 1, 音域));
    }

    /**
     * 切换音域：增加=true 升一个八度，增加=false 降一个八度。
     * 越界时 clamp 到有效范围。
     */
    public int 切换音域(int 当前音域, boolean 增加) {
        int 新音域 = 增加 ? 当前音域 + 1 : 当前音域 - 1;
        return 规范化音域(新音域);
    }
}
