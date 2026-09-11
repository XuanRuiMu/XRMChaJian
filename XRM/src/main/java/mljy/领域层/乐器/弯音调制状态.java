package mljy.领域层.乐器;

/**
 * FP-21 弯音轮 + 调制轮状态（mod 独享，O6）。
 * 不可变值对象，承载 mod 玩家的弯音轮与调制轮实时状态。
 * <p>
 * 弯音轮：0-127，64 = 无弯音，0 = -2 半音，127 = +2 半音（FP-21 默认 ±2 半音）。
 * 调制轮：0-127，0 = 无颤音，127 = 最大颤音深度。
 * <p>
 * 服务端权威计算：弯音改变 playSound 的 pitch 参数；调制轮通过周期性微调 pitch 模拟颤音。
 * 反作弊：服务端校验值范围 [0, 127]，频率上限每 tick 1 次。
 */
public final class 弯音调制状态 {
    public static final int 值下限 = 0;
    public static final int 值上限 = 127;
    public static final int 弯音中心值 = 64;
    public static final int 默认弯音范围半音 = 2;
    public static final int 默认调制频率Hz = 6;

    private final int 弯音值;
    private final int 调制值;

    public 弯音调制状态(int 弯音值, int 调制值) {
        this.弯音值 = clamp(弯音值);
        this.调制值 = clamp(调制值);
    }

    public static 弯音调制状态 默认状态() {
        return new 弯音调制状态(弯音中心值, 值下限);
    }

    public int 获取弯音值() {
        return 弯音值;
    }

    public int 获取调制值() {
        return 调制值;
    }

    /**
     * 计算弯音对 pitch 的偏移量（半音 → float 倍率因子）。
     * 弯音值 64 → 0 半音；弯音值 0 → -2 半音；弯音值 127 → +2 半音。
     * 半音转 pitch 倍率：pow(2, 半音 / 12.0)。
     *
     * @param 弯音范围半音 弯音范围（默认 ±2 半音）
     * @return pitch 倍率因子（1.0 = 无偏移；< 1.0 = 降音；> 1.0 = 升音）
     */
    public float 计算弯音Pitch倍率(int 弯音范围半音) {
        int 范围 = Math.max(1, 弯音范围半音);
        double 半音偏移 = ((double) (弯音值 - 弯音中心值) / (弯音中心值)) * 范围;
        return (float) Math.pow(2.0, 半音偏移 / 12.0);
    }

    /**
     * 计算调制轮对 pitch 的颤音偏移量（基于时间相位）。
     * 调制值 0 → 无颤音；调制值 127 → ±0.5 半音颤音。
     * 颤音频率默认 6 Hz（每秒 6 次周期性变化）。
     *
     * @param 时间戳毫秒 当前时间戳（毫秒）
     * @param 频率Hz      颤音频率（默认 6 Hz）
     * @return pitch 倍率因子（1.0 = 无偏移）
     */
    public float 计算调制Pitch倍率(long 时间戳毫秒, int 频率Hz) {
        if (调制值 == 值下限) {
            return 1.0f;
        }
        int 频率 = Math.max(1, Math.min(10, 频率Hz));
        double 深度半音 = (调制值 / (double) 值上限) * 0.5;
        double 相位 = (时间戳毫秒 / 1000.0) * 频率 * 2 * Math.PI;
        double 半音偏移 = Math.sin(相位) * 深度半音;
        return (float) Math.pow(2.0, 半音偏移 / 12.0);
    }

    /**
     * 创建新状态：设置弯音值。
     */
    public 弯音调制状态 设置弯音值(int 新弯音值) {
        return new 弯音调制状态(新弯音值, 调制值);
    }

    /**
     * 创建新状态：设置调制值。
     */
    public 弯音调制状态 设置调制值(int 新调制值) {
        return new 弯音调制状态(弯音值, 新调制值);
    }

    private static int clamp(int 值) {
        return Math.max(值下限, Math.min(值上限, 值));
    }
}
