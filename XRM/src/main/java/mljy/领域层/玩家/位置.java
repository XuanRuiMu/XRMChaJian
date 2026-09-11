package mljy.领域层.玩家;

public record 位置(
        String 世界,
        double x,
        double y,
        double z,
        float 偏航角,
        float 俯仰角
) {
    public double 距离平方(位置 目标) {
        double dx = x - 目标.x();
        double dy = y - 目标.y();
        double dz = z - 目标.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public double 距离(位置 目标) {
        return Math.sqrt(距离平方(目标));
    }
}
