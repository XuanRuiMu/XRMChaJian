package mljy.基础设施层;

public final class 数值验证器 {
    private 数值验证器() {
    }

    public static boolean 是有限值(double 数值) {
        return !Double.isNaN(数值) && !Double.isInfinite(数值);
    }

    public static double 清理为零(double 数值) {
        return 是有限值(数值) ? 数值 : 0;
    }

    public static double 清理为默认(double 数值, double 默认值) {
        return 是有限值(数值) ? 数值 : 默认值;
    }

    public static double 限制下限(double 数值, double 下限) {
        if (!是有限值(数值)) {
            return 下限;
        }
        return Math.max(数值, 下限);
    }

    public static double 限制范围(double 数值, double 下限, double 上限) {
        if (!是有限值(数值)) {
            return 下限;
        }
        return Math.max(下限, Math.min(数值, 上限));
    }
}
