package 暮澜纪元;

import org.bukkit.Location;

/**
 * 传送门区域类。
 * 定义一个矩形传送门区域及其目标服务器。
 */
public class 传送门区域 {

    private final String 传送门ID;
    private final String 世界名;
    private final int 最小X;
    private final int 最小Y;
    private final int 最小Z;
    private final int 最大X;
    private final int 最大Y;
    private final int 最大Z;
    private final String 目标服务器;

    public 传送门区域(String 传送门ID, String 世界名, int 最小X, int 最小Y, int 最小Z, int 最大X, int 最大Y, int 最大Z, String 目标服务器, String 提示消息) {
        this.传送门ID = 传送门ID;
        this.世界名 = 世界名;
        this.最小X = Math.min(最小X, 最大X);
        this.最小Y = Math.min(最小Y, 最大Y);
        this.最小Z = Math.min(最小Z, 最大Z);
        this.最大X = Math.max(最小X, 最大X);
        this.最大Y = Math.max(最小Y, 最大Y);
        this.最大Z = Math.max(最小Z, 最大Z);
        this.目标服务器 = 目标服务器;
    }

    /**
     * 通过两个Location创建传送门区域。
     *
     * @param 传送门ID 传送门标识
     * @param 点一 第一个位置
     * @param 点二 第二个位置
     */
    public 传送门区域(String 传送门ID, Location 点一, Location 点二) {
        this.传送门ID = 传送门ID;
        this.世界名 = 点一.getWorld().getName();
        this.最小X = Math.min(点一.getBlockX(), 点二.getBlockX());
        this.最小Y = Math.min(点一.getBlockY(), 点二.getBlockY());
        this.最小Z = Math.min(点一.getBlockZ(), 点二.getBlockZ());
        this.最大X = Math.max(点一.getBlockX(), 点二.getBlockX());
        this.最大Y = Math.max(点一.getBlockY(), 点二.getBlockY());
        this.最大Z = Math.max(点一.getBlockZ(), 点二.getBlockZ());
        this.目标服务器 = 传送门ID;
    }

    public boolean 是否在区域内(String 世界2, int 坐标X, int 坐标Y, int 坐标Z) {
        return this.世界名.equals(世界2)
            && 坐标X >= this.最小X && 坐标X <= this.最大X
            && 坐标Y >= this.最小Y && 坐标Y <= this.最大Y
            && 坐标Z >= this.最小Z && 坐标Z <= this.最大Z;
    }

    public String 获取传送门ID() {
        return this.传送门ID;
    }

    public String 获取世界名() {
        return this.世界名;
    }

    public String 获取目标服务器() {
        return this.目标服务器;
    }

    public int 获取最小X() {
        return this.最小X;
    }

    public int 获取最小Y() {
        return this.最小Y;
    }

    public int 获取最小Z() {
        return this.最小Z;
    }

    public int 获取最大X() {
        return this.最大X;
    }

    public int 获取最大Y() {
        return this.最大Y;
    }

    public int 获取最大Z() {
        return this.最大Z;
    }
}
