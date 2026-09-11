package mljy.领域层.资源;

public class 资源 {
    private final String 资源标识;
    private double 当前值;
    private double 上限;

    public 资源(String 资源标识, double 当前值, double 上限) {
        this.资源标识 = 资源标识;
        this.当前值 = 当前值;
        this.上限 = 上限;
    }

    public String 获取资源标识() {
        return 资源标识;
    }

    public double 获取当前值() {
        return 当前值;
    }

    public void 设置当前值(double 当前值) {
        this.当前值 = 当前值;
    }

    public double 获取上限() {
        return 上限;
    }

    public void 设置上限(double 上限) {
        this.上限 = 上限;
    }
}
