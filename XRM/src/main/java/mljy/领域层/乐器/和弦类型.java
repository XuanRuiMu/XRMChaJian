package mljy.领域层.乐器;

import java.util.Collections;
import java.util.List;

/**
 * FP-11 和弦类型枚举（12 种业界标准预设）。
 * 每种类型定义为相对根音的音程集合（半音偏移）。
 * <ul>
 *   <li>maj = [0, 4, 7]（大三和弦）</li>
 *   <li>min = [0, 3, 7]（小三和弦）</li>
 *   <li>7 = [0, 4, 7, 10]（属七和弦）</li>
 *   <li>maj7 = [0, 4, 7, 11]（大七和弦）</li>
 *   <li>m7 = [0, 3, 7, 10]（小七和弦）</li>
 *   <li>dim = [0, 3, 6]（减三和弦）</li>
 *   <li>aug = [0, 4, 8]（增三和弦）</li>
 *   <li>sus2 = [0, 2, 7]（挂二和弦）</li>
 *   <li>sus4 = [0, 5, 7]（挂四和弦）</li>
 *   <li>add9 = [0, 4, 7, 14]（加九和弦）</li>
 *   <li>6 = [0, 4, 7, 9]（大六和弦）</li>
 *   <li>9 = [0, 4, 7, 10, 14]（属九和弦）</li>
 * </ul>
 * 音程集合不可变；调用 获取音高(根音) 时将每个音程偏移叠加到根音得到实际音高。
 */
public enum 和弦类型 {
    大三("maj", List.of(0, 4, 7)),
    小三("min", List.of(0, 3, 7)),
    属七("7", List.of(0, 4, 7, 10)),
    大七("maj7", List.of(0, 4, 7, 11)),
    小七("m7", List.of(0, 3, 7, 10)),
    减三("dim", List.of(0, 3, 6)),
    增三("aug", List.of(0, 4, 8)),
    挂二("sus2", List.of(0, 2, 7)),
    挂四("sus4", List.of(0, 5, 7)),
    加九("add9", List.of(0, 4, 7, 14)),
    大六("6", List.of(0, 4, 7, 9)),
    属九("9", List.of(0, 4, 7, 10, 14));

    private final String 标识;
    private final List<Integer> 音程集合;

    和弦类型(String 标识, List<Integer> 音程集合) {
        this.标识 = 标识;
        this.音程集合 = Collections.unmodifiableList(音程集合);
    }

    public String 获取标识() {
        return 标识;
    }

    /**
     * 获取和弦类型的音程集合（相对根音的半音偏移）。
     *
     * @return 不可修改的音程列表
     */
    public List<Integer> 获取音程() {
        return 音程集合;
    }

    /**
     * 根据根音生成实际音高列表（根音 + 各音程偏移）。
     * 不对音高做范围限制，调用方负责 clamp 到乐器音域。
     *
     * @param 根音 根音（0-59）
     * @return 实际音高列表
     */
    public List<Integer> 获取音高(int 根音) {
        return 音程集合.stream().map(音程 -> 根音 + 音程).toList();
    }

    /**
     * 按标识查找和弦类型（如 "maj" "m7"）。
     *
     * @param 标识 和弦标识字符串
     * @return 和弦类型；未匹配返回 null
     */
    public static 和弦类型 按标识查找(String 标识) {
        if (标识 == null || 标识.isBlank()) {
            return null;
        }
        String 修剪 = 标识.trim();
        for (和弦类型 类型 : values()) {
            if (类型.标识.equals(修剪)) {
                return 类型;
            }
        }
        return null;
    }
}
