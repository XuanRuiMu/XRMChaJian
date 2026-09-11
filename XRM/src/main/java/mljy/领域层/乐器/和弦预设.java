package mljy.领域层.乐器;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-11 和弦预设服务。
 * 按根音 + 和弦类型生成实际音高列表，并 clamp 到乐器音域。
 * <p>
 * 设计：
 * - 领域层纯逻辑，不依赖 Bukkit API
 * - clamp 算法：超出上限的音高按八度下移（音高 - 12），直到落入音域；多次下移仍越界则舍弃
 * - 调用方（乐器服务实现）负责实际演奏与和弦记忆任务调度
 */
public final class 和弦预设 {

    /**
     * 根据根音 + 和弦类型生成实际音高列表，并 clamp 到乐器音域 [下限, 上限]。
     * 超出上限的音通过八度下移（-12）尝试落入音域；下限以下或多次下移仍超上限的音舍弃。
     *
     * @param 根音   根音
     * @param 类型   和弦类型
     * @param 下限   乐器音域下限
     * @param 上限   乐器音域上限
     * @return clamp 后的音高列表（可能为空）
     */
    public static List<Integer> 生成和弦(int 根音, 和弦类型 类型, int 下限, int 上限) {
        if (类型 == null || 下限 > 上限) {
            return List.of();
        }
        List<Integer> 原始 = 类型.获取音高(根音);
        List<Integer> 结果 = new ArrayList<>(原始.size());
        for (int 音高 : 原始) {
            int 调整 = 音高;
            while (调整 > 上限) {
                调整 -= 12;
            }
            if (调整 < 下限) {
                continue;
            }
            结果.add(调整);
        }
        return 结果;
    }

    private 和弦预设() {
    }
}
