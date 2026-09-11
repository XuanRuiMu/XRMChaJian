package mljy.领域层.乐器;

/**
 * 音符领域对象。
 * 包含音高（0-24，对应2个八度）、时值（tick数）、力度（0.0-1.0）。
 * 音高值对应Bukkit的playNote方法中的note参数（0-24）。
 */
public class 音符 {
    private final int 音高;
    private final int 时值Tick;
    private final double 力度;

    public 音符(int 音高, int 时值Tick, double 力度) {
        if (音高 < 0 || 音高 > 24) {
            throw new IllegalArgumentException("音高必须在0-24范围内: " + 音高);
        }
        if (时值Tick < 0) {
            throw new IllegalArgumentException("时值不能为负: " + 时值Tick);
        }
        if (力度 < 0.0 || 力度 > 1.0) {
            throw new IllegalArgumentException("力度必须在0.0-1.0范围内: " + 力度);
        }
        this.音高 = 音高;
        this.时值Tick = 时值Tick;
        this.力度 = 力度;
    }

    public static 音符 of(int 音高, int 时值Tick, double 力度) {
        return new 音符(音高, 时值Tick, 力度);
    }

    public static 音符 简单(int 音高) {
        return new 音符(音高, 4, 1.0);
    }

    public int 获取音高() {
        return 音高;
    }

    public int 获取时值Tick() {
        return 时值Tick;
    }

    public double 获取力度() {
        return 力度;
    }

    public int 获取八度() {
        return 音高 / 12;
    }

    public int 获取音阶() {
        return 音高 % 12;
    }

    @Override
    public String toString() {
        return "音符{音高=" + 音高 + ", 时值=" + 时值Tick + "tick, 力度=" + 力度 + "}";
    }

    @Override
    public boolean equals(Object 对象) {
        if (this == 对象) {
            return true;
        }
        if (!(对象 instanceof 音符 其他)) {
            return false;
        }
        return 音高 == 其他.音高 && 时值Tick == 其他.时值Tick
                && Double.compare(力度, 其他.力度) == 0;
    }

    @Override
    public int hashCode() {
        int 结果 = 音高;
        结果 = 31 * 结果 + 时值Tick;
        结果 = 31 * 结果 + Double.hashCode(力度);
        return 结果;
    }
}
