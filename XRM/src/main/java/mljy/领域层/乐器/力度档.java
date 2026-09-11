package mljy.领域层.乐器;

/**
 * FP-05 力度档枚举（5 档，全员可用）。
 * 对应需求文档「功能点 5」表 5.1，参数近似方案。
 * <p>
 * 档位 / 名称 / velocity 范围 / volume 映射 / pitch 微调：
 * <ul>
 *   <li>PP（pianissimo，极弱）：0-25，volume 0.30，pitch -0.005</li>
 *   <li>P（piano，弱）：26-50，volume 0.50，pitch -0.003</li>
 *   <li>MF（mezzo-forte，中强，默认）：51-90，volume 0.75，pitch 0</li>
 *   <li>F（forte，强）：91-110，volume 0.90，pitch +0.002</li>
 *   <li>FF（fortissimo，极强）：111-127，volume 1.00，pitch +0.004</li>
 * </ul>
 * pitch 微调幅度极小（±0.005），仅为增加触感的「轻重差异」，不影响音准。
 * <p>
 * 力度档转 NBS velocity（0-127）取档位中点：PP=13, P=38, MF=70, F=100, FF=119。
 * mod 玩家启用 velocity-by-duration 时使用连续值（FP-05.2）。
 */
public enum 力度档 {
    PP("pp", "pianissimo", 13, 0.30f, -0.005f),
    P("p", "piano", 38, 0.50f, -0.003f),
    MF("mf", "mezzo-forte", 70, 0.75f, 0.0f),
    F("f", "forte", 100, 0.90f, 0.002f),
    FF("ff", "fortissimo", 119, 1.00f, 0.004f);

    private static final 力度档[] 顺序 = {PP, P, MF, F, FF};
    private static final 力度档 默认档 = MF;

    private final String 标识;
    private final String 全名;
    private final int velocity中点;
    private final float 音量;
    private final float pitch微调;

    力度档(String 标识, String 全名, int velocity中点, float 音量, float pitch微调) {
        this.标识 = 标识;
        this.全名 = 全名;
        this.velocity中点 = velocity中点;
        this.音量 = 音量;
        this.pitch微调 = pitch微调;
    }

    public String 获取标识() {
        return 标识;
    }

    public String 获取全名() {
        return 全名;
    }

    /**
     * 力度档对应的 NBS per-note velocity 中点值（0-127）。
     * 录音写入 NBS 时使用此值（FP-05.1）。
     */
    public int 获取Velocity中点() {
        return velocity中点;
    }

    /**
     * 力度档对应的 playSound 音量倍数（0.0-1.0）。
     * 与乐器默认音量相乘得到最终音量。
     */
    public float 获取音量() {
        return 音量;
    }

    /**
     * 力度档对应的 pitch 微调（极小幅度，±0.005）。
     * 叠加到 乐器定义.音高转Pitch 计算结果上。
     */
    public float 获取pitch微调() {
        return pitch微调;
    }

    /**
     * 默认力度档（MF，中强）。
     */
    public static 力度档 默认档() {
        return 默认档;
    }

    /**
     * 切换到上一档（更弱）。已在最弱档时保持不变。
     */
    public 力度档 上一档() {
        int 索引 = ordinal();
        return 索引 == 0 ? this : 顺序[索引 - 1];
    }

    /**
     * 切换到下一档（更强）。已在最强档时保持不变。
     */
    public 力度档 下一档() {
        int 索引 = ordinal();
        return 索引 == 顺序.length - 1 ? this : 顺序[索引 + 1];
    }

    /**
     * 按标识查找力度档（如 "pp" "mf"）。未匹配返回 默认档（MF）。
     */
    public static 力度档 按标识(String 标识) {
        if (标识 == null || 标识.isBlank()) {
            return 默认档;
        }
        String 小写 = 标识.trim().toLowerCase();
        for (力度档 档 : 顺序) {
            if (档.标识.equals(小写)) {
                return 档;
            }
        }
        return 默认档;
    }
}
