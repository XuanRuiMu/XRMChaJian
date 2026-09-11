package mljy.领域层.资源;

public class 资源定义 {
    private final String 资源标识;
    private final String 名称;
    private final double 默认上限;
    private final double 每秒恢复;
    private final String 显示键;

    public 资源定义(String 资源标识, String 名称, double 默认上限, double 每秒恢复, String 显示键) {
        this.资源标识 = 资源标识;
        this.名称 = 名称;
        this.默认上限 = 默认上限;
        this.每秒恢复 = 每秒恢复;
        this.显示键 = 显示键;
    }

    public String 获取资源标识() {
        return 资源标识;
    }

    public String 获取名称() {
        return 名称;
    }

    public double 获取默认上限() {
        return 默认上限;
    }

    public double 获取每秒恢复() {
        return 每秒恢复;
    }

    public String 获取显示键() {
        return 显示键;
    }
}
