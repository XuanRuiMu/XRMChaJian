package mljy.领域层.乐器;

import java.util.HashMap;
import java.util.Map;

/**
 * FP-22 表情控制状态（mod 独享，O10）。
 * 不可变值对象，承载 mod 玩家的 MIDI CC 控制器实时状态。
 * <p>
 * 支持的 CC 控制器（参考需求文档 FP-22.1）：
 * - CC1  Modulation（颤音，与 FP-21 调制轮等价）
 * - CC2  Breath（气声，影响管乐气声强度，需资源包支持，否则降级为 volume 微调）
 * - CC4  Foot（脚踏，通用脚踏控制）
 * - CC11 Expression（表情，整体音量表情变化，与 FP-05 力度的 volume 叠加，乘法关系）
 * <p>
 * 所有 CC 值范围 0-127，默认全部 0。
 * 服务端权威计算：CC11 影响最终 volume（乘法叠加）；CC1/CC2/CC4 影响音色变化（参数近似）。
 * 反作弊：服务端校验值范围 [0, 127]，频率上限每 tick 1 次。
 */
public final class 表情控制状态 {
    public static final int CC_MODULATION = 1;
    public static final int CC_BREATH = 2;
    public static final int CC_FOOT = 4;
    public static final int CC_EXPRESSION = 11;

    public static final int 值下限 = 0;
    public static final int 值上限 = 127;

    private final int cc1;
    private final int cc2;
    private final int cc4;
    private final int cc11;

    public 表情控制状态(int cc1, int cc2, int cc4, int cc11) {
        this.cc1 = clamp(cc1);
        this.cc2 = clamp(cc2);
        this.cc4 = clamp(cc4);
        this.cc11 = clamp(cc11);
    }

    public static 表情控制状态 默认状态() {
        return new 表情控制状态(值下限, 值下限, 值下限, 值下限);
    }

    public int 获取CC1() {
        return cc1;
    }

    public int 获取CC2() {
        return cc2;
    }

    public int 获取CC4() {
        return cc4;
    }

    public int 获取CC11() {
        return cc11;
    }

    /**
     * 计算 CC11 expression 对 volume 的乘数（0.0-1.0）。
     * CC11 = 0 → 0.0（静音）；CC11 = 127 → 1.0（原音量）；线性映射。
     * 与 FP-05 力度的 volume 叠加：最终音量 = 力度档.获取音量 × CC11 倍率。
     *
     * @return volume 乘数（0.0-1.0）
     */
    public float 计算表情Volume倍率() {
        return cc11 / (float) 值上限;
    }

    /**
     * 计算 CC2 breath 对 volume 的附加微调（管乐气声）。
     * 资源包未启用时降级为 volume 微调（最大 +20%）。
     *
     * @return volume 附加倍率（0.0-0.2）
     */
    public float 计算气声Volume附加() {
        return (cc2 / (float) 值上限) * 0.2f;
    }

    /**
     * 创建新状态：设置指定 CC 值。
     * 仅支持 CC1/CC2/CC4/CC11，其他 CC 编号忽略。
     *
     * @param cc编号 CC 编号
     * @param 值     CC 值（0-127，自动 clamp）
     * @return 新状态
     */
    public 表情控制状态 设置CC(int cc编号, int 值) {
        int 限制值 = clamp(值);
        return switch (cc编号) {
            case CC_MODULATION -> new 表情控制状态(限制值, cc2, cc4, cc11);
            case CC_BREATH -> new 表情控制状态(cc1, 限制值, cc4, cc11);
            case CC_FOOT -> new 表情控制状态(cc1, cc2, 限制值, cc11);
            case CC_EXPRESSION -> new 表情控制状态(cc1, cc2, cc4, 限制值);
            default -> this;
        };
    }

    /**
     * 转为 CC 编号 → 值的映射（仅含非零项）。
     */
    public Map<Integer, Integer> 转为映射() {
        Map<Integer, Integer> 映射 = new HashMap<>();
        if (cc1 != 值下限) 映射.put(CC_MODULATION, cc1);
        if (cc2 != 值下限) 映射.put(CC_BREATH, cc2);
        if (cc4 != 值下限) 映射.put(CC_FOOT, cc4);
        if (cc11 != 值下限) 映射.put(CC_EXPRESSION, cc11);
        return 映射;
    }

    private static int clamp(int 值) {
        return Math.max(值下限, Math.min(值上限, 值));
    }
}
