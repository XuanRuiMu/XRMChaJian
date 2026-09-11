package mljy.技能实现.奥能法师;

import mljy.基础设施层.Yaml配置加载器;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FP-05 大招特效配置。
 * 配置只影响视觉表现与视觉任务周期，不参与伤害、资源和伤害调度计算。
 */
public final class 奥能法师大招特效配置 {
    private static final String 配置文件 = "奥能法师大招特效.yml";
    private static final int 服务端粒子硬上限 = 4096;

    private final double 强度;
    private final double 视觉半径;
    private final double 最大距离;
    private final int 单次粒子上限;
    private final float 音量倍率;
    private final int 冥想开始持续刻;
    private final int 冥想开始间隔刻;
    private final int 冥想完成持续刻;
    private final int 冥想完成间隔刻;
    private final int 冥想蓄能间隔刻;
    private final int 冥想持续间隔刻;
    private final int 湮灭充能持续刻;
    private final int 湮灭充能间隔刻;
    private final int 湮灭续燃持续刻;
    private final int 湮灭续燃间隔刻;

    private 奥能法师大招特效配置(
            double 强度,
            double 视觉半径,
            double 最大距离,
            int 单次粒子上限,
            float 音量倍率,
            int 冥想开始持续刻,
            int 冥想开始间隔刻,
            int 冥想完成持续刻,
            int 冥想完成间隔刻,
            int 冥想蓄能间隔刻,
            int 冥想持续间隔刻,
            int 湮灭充能持续刻,
            int 湮灭充能间隔刻,
            int 湮灭续燃持续刻,
            int 湮灭续燃间隔刻) {
        this.强度 = 限制(强度, 0.5, 3.0);
        this.视觉半径 = 限制(视觉半径, 1.0, 64.0);
        this.最大距离 = 限制(最大距离, 1.0, 128.0);
        this.单次粒子上限 = Math.max(1, Math.min(单次粒子上限, 服务端粒子硬上限));
        this.音量倍率 = (float) 限制(音量倍率, 0.1, 2.0);
        this.冥想开始持续刻 = Math.max(1, 冥想开始持续刻);
        this.冥想开始间隔刻 = Math.max(1, 冥想开始间隔刻);
        this.冥想完成持续刻 = Math.max(1, 冥想完成持续刻);
        this.冥想完成间隔刻 = Math.max(1, 冥想完成间隔刻);
        this.冥想蓄能间隔刻 = Math.max(1, 冥想蓄能间隔刻);
        this.冥想持续间隔刻 = Math.max(1, 冥想持续间隔刻);
        this.湮灭充能持续刻 = Math.max(1, 湮灭充能持续刻);
        this.湮灭充能间隔刻 = Math.max(1, 湮灭充能间隔刻);
        this.湮灭续燃持续刻 = Math.max(1, 湮灭续燃持续刻);
        this.湮灭续燃间隔刻 = Math.max(1, 湮灭续燃间隔刻);
    }

    public static 奥能法师大招特效配置 加载(JavaPlugin 插件) {
        奥能法师大招特效配置 默认配置 = 默认配置();
        if (插件 == null) {
            return 默认配置;
        }
        try {
            FileConfiguration 配置 = new Yaml配置加载器(插件).加载(配置文件);
            return new 奥能法师大招特效配置(
                    配置.getDouble("通用.强度", 默认配置.强度),
                    配置.getDouble("通用.视觉半径", 默认配置.视觉半径),
                    配置.getDouble("通用.最大距离", 默认配置.最大距离),
                    配置.getInt("通用.单次粒子上限", 默认配置.单次粒子上限),
                    (float) 配置.getDouble("通用.音量倍率", 默认配置.音量倍率),
                    配置.getInt("奥能冥想.开始动画.持续刻", 默认配置.冥想开始持续刻),
                    配置.getInt("奥能冥想.开始动画.间隔刻", 默认配置.冥想开始间隔刻),
                    配置.getInt("奥能冥想.完成动画.持续刻", 默认配置.冥想完成持续刻),
                    配置.getInt("奥能冥想.完成动画.间隔刻", 默认配置.冥想完成间隔刻),
                    配置.getInt("奥能冥想.蓄能动画.间隔刻", 默认配置.冥想蓄能间隔刻),
                    配置.getInt("奥能冥想.持续效果.间隔刻", 默认配置.冥想持续间隔刻),
                    配置.getInt("秘法湮灭.充能动画.持续刻", 默认配置.湮灭充能持续刻),
                    配置.getInt("秘法湮灭.充能动画.间隔刻", 默认配置.湮灭充能间隔刻),
                    配置.getInt("秘法湮灭.续燃.持续刻", 默认配置.湮灭续燃持续刻),
                    配置.getInt("秘法湮灭.续燃.间隔刻", 默认配置.湮灭续燃间隔刻));
        } catch (RuntimeException 异常) {
            mljy.基础设施层.调试日志器.调试("奥能法师大招特效配置",
                    "配置加载失败，使用默认值：文件=%s 异常=%s", 配置文件, 异常.getClass().getSimpleName());
            return 默认配置;
        }
    }

    public static 奥能法师大招特效配置 默认配置() {
        return new 奥能法师大招特效配置(
                1.35, 6.5, 48.0, 250, 1.25f,
                20, 2, 26, 1, 3, 4,
                60, 1, 6, 2);
    }

    public int 粒子数量(int 基础数量) {
        int 请求数量 = Math.max(1, (int) Math.round(基础数量 * 强度));
        return Math.max(1, Math.min(请求数量, 单次粒子上限));
    }

    public int 环点数量(int 基础数量) {
        return Math.max(1, (int) Math.round(基础数量 * 强度));
    }

    public float 粒子尺寸(float 基础尺寸) {
        return Math.max(0.01f, Math.min(4.0f, 基础尺寸 * (float) 强度));
    }

    public float 音量(float 基础音量) {
        return Math.max(0.0f, Math.min(4.0f, 基础音量 * 音量倍率));
    }

    public double 强度() {
        return 强度;
    }

    public double 视觉半径() {
        return 视觉半径;
    }

    public double 最大距离() {
        return 最大距离;
    }

    public int 冥想开始持续刻() {
        return 冥想开始持续刻;
    }

    public int 冥想开始间隔刻() {
        return 冥想开始间隔刻;
    }

    public int 冥想完成持续刻() {
        return 冥想完成持续刻;
    }

    public int 冥想完成间隔刻() {
        return 冥想完成间隔刻;
    }

    public int 冥想蓄能间隔刻() {
        return 冥想蓄能间隔刻;
    }

    public int 冥想持续间隔刻() {
        return 冥想持续间隔刻;
    }

    public int 湮灭充能持续刻() {
        return 湮灭充能持续刻;
    }

    public int 湮灭充能间隔刻() {
        return 湮灭充能间隔刻;
    }

    public int 湮灭续燃持续刻() {
        return 湮灭续燃持续刻;
    }

    public int 湮灭续燃间隔刻() {
        return 湮灭续燃间隔刻;
    }

    private static double 限制(double 值, double 最小值, double 最大值) {
        return Math.max(最小值, Math.min(最大值, 值));
    }
}
