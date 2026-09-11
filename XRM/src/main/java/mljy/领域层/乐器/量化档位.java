package mljy.领域层.乐器;

/**
 * FP-09 录音量化档位枚举。
 * 包含 1/8、1/16、1/32 三种基础档位 + 三连音变体（1/8T、1/16T、1/32T）。
 * <p>
 * 网格 tick = 每拍 tick / 每拍分割数。每拍 tick = 1200 / BPM（1秒=20tick，每拍秒数=60/BPM）。
 * <ul>
 *   <li>1/8：每半拍一网格（每拍分割数=2）</li>
 *   <li>1/16：每 1/4 拍一网格（每拍分割数=4，默认）</li>
 *   <li>1/32：每 1/8 拍一网格（每拍分割数=8）</li>
 *   <li>1/8T：三连音八分（每拍分割数=3，3 个音符均分一拍，时值 = 1/8 × 2/3）</li>
 *   <li>1/16T：三连音十六分（每拍分割数=6）</li>
 *   <li>1/32T：三连音三十二分（每拍分割数=12）</li>
 * </ul>
 * 量化算法：将每个录音音符的累计起始 tick 吸附到最近的网格点（round(tick / 网格tick) × 网格tick）。
 * 非破坏性：保留原始力度与音高，仅修正时间。
 */
public enum 量化档位 {
    一分之八("1/8", 2, false),
    一分之十六("1/16", 4, false),
    一分之三十二("1/32", 8, false),
    一分之八T("1/8T", 3, true),
    一分之十六T("1/16T", 6, true),
    一分之三十二T("1/32T", 12, true);

    private static final 量化档位 默认档位 = 一分之十六;

    private final String 标识;
    private final int 每拍分割数;
    private final boolean 三连音;

    量化档位(String 标识, int 每拍分割数, boolean 三连音) {
        this.标识 = 标识;
        this.每拍分割数 = 每拍分割数;
        this.三连音 = 三连音;
    }

    public String 获取标识() {
        return 标识;
    }

    public int 获取每拍分割数() {
        return 每拍分割数;
    }

    public boolean 是否三连音() {
        return 三连音;
    }

    /**
     * 计算当前档位的网格 tick 间隔。
     * 网格 tick = 每拍 tick / 每拍分割数 = (1200 / BPM) / 每拍分割数。
     * 最小为 1 tick。
     *
     * @param 速度BPM 乐谱 BPM
     * @return 网格 tick 间隔
     */
    public int 计算网格Tick(int 速度BPM) {
        if (速度BPM <= 0) {
            return 1;
        }
        long 每拍tick = Math.max(1L, Math.round(1200.0 / 速度BPM));
        return Math.max(1, (int) (每拍tick / 每拍分割数));
    }

    /**
     * 将给定 tick 值吸附到最近的网格点。
     * 算法：quantized = round(tick / 网格tick) × 网格tick。
     *
     * @param tick 原始 tick 值
     * @param 速度BPM 乐谱 BPM
     * @return 量化后的 tick 值
     */
    public int 量化Tick(int tick, int 速度BPM) {
        if (tick <= 0) {
            return 0;
        }
        int 网格tick = 计算网格Tick(速度BPM);
        return Math.round((float) tick / 网格tick) * 网格tick;
    }

    /**
     * 默认量化档位（1/16）。
     */
    public static 量化档位 默认档位() {
        return 默认档位;
    }

    /**
     * 按标识查找量化档位（如 "1/8" "1/16T"）。未匹配返回默认档位（1/16）。
     *
     * @param 标识 档位标识字符串
     * @return 量化档位；未匹配返回默认档位
     */
    public static 量化档位 按标识查找(String 标识) {
        if (标识 == null || 标识.isBlank()) {
            return 默认档位;
        }
        String 修剪 = 标识.trim();
        for (量化档位 档 : values()) {
            if (档.标识.equals(修剪)) {
                return 档;
            }
        }
        return 默认档位;
    }
}
