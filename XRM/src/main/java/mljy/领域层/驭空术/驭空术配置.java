package mljy.领域层.驭空术;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 驭空术配置。
 * 从config.yml的"驭空术"区块加载所有可配置参数。
 */
public class 驭空术配置 {
    private static final String 根键 = "驭空术";
    private static final String 键总开关 = "总开关";
    private static final String 键战斗中禁用 = "战斗中禁用";

    private static final String 子键活力 = "活力";
    private static final String 键活力上限 = "上限";
    private static final String 键飞行每秒消耗 = "飞行每秒消耗";
    private static final String 键地面每秒恢复 = "地面每秒恢复";
    private static final String 键飞行每秒恢复 = "飞行每秒恢复";

    private static final String 子键起飞 = "起飞";
    private static final String 键起飞消耗 = "消耗";
    private static final String 键起飞升空高度 = "升空高度";
    private static final String 键起飞升空时长 = "升空时长";

    private static final String 子键冲刺 = "冲刺";
    private static final String 键冲刺消耗 = "消耗";
    private static final String 键冲刺冷却 = "冷却";
    private static final String 键冲刺推力 = "推力";
    private static final String 键冲刺持续Tick = "持续Tick";

    private static final String 子键爆发 = "爆发";
    private static final String 键爆发消耗 = "消耗";
    private static final String 键爆发冷却 = "冷却";
    private static final String 键爆发推力 = "推力";
    private static final String 键爆发持续Tick = "持续Tick";

    private static final String 子键俯冲 = "俯冲";
    private static final String 键俯冲消耗 = "消耗";
    private static final String 键俯冲冷却 = "冷却";
    private static final String 键俯冲推力 = "推力";
    private static final String 键俯冲持续Tick = "持续Tick";

    private static final String 子键物理 = "物理";
    private static final String 键重力加速度 = "重力加速度";
    private static final String 键基础速度 = "基础速度";
    private static final String 键最大速度 = "最大速度";
    private static final String 键升力系数 = "升力系数";
    private static final String 键空气阻力系数 = "空气阻力系数";
    private static final String 键俯冲加速倍数 = "俯冲加速倍数";
    private static final String 键抬头减速倍数 = "抬头减速倍数";

    private static final String 子键摔落保护 = "摔落保护";
    private static final String 键摔落保护秒数 = "秒数";

    private static final String 子键准驭空 = "准驭空";
    private static final String 键准驭空超时秒 = "超时秒";
    private static final String 键准驭空双击间隔毫秒 = "双击间隔毫秒";

    private static final String 子键BossBar = "BossBar";
    private static final String 键BossBar更新间隔Tick = "更新间隔Tick";

    private static final boolean 默认总开关 = true;
    private static final boolean 默认战斗中禁用 = true;
    private static final double 默认活力上限 = 100.0;
    private static final double 默认飞行每秒消耗 = 5.0;
    private static final double 默认地面每秒恢复 = 10.0;
    private static final double 默认飞行每秒恢复 = 0.0;
    private static final double 默认起飞消耗 = 20.0;
    private static final double 默认起飞升空高度 = 20.0;
    private static final int 默认起飞升空时长 = 40;
    private static final double 默认冲刺消耗 = 15.0;
    private static final int 默认冲刺冷却 = 60;
    private static final double 默认冲刺推力 = 1.1;
    private static final int 默认冲刺持续Tick = 30;
    private static final double 默认爆发消耗 = 25.0;
    private static final int 默认爆发冷却 = 100;
    private static final double 默认爆发推力 = 0.9;
    private static final int 默认爆发持续Tick = 30;
    private static final double 默认俯冲消耗 = 10.0;
    private static final int 默认俯冲冷却 = 40;
    private static final double 默认俯冲推力 = 1.5;
    private static final int 默认俯冲持续Tick = 30;
    private static final double 默认重力加速度 = -0.015;
    private static final double 默认基础速度 = 0.8;
    private static final double 默认最大速度 = 9.5;
    private static final double 默认升力系数 = 0.001;
    private static final double 默认空气阻力系数 = 0.001;
    private static final double 默认俯冲加速倍数 = 2.0;
    private static final double 默认抬头减速倍数 = 0.5;
    private static final int 默认摔落保护秒数 = 2;
    private static final int 默认准驭空超时秒 = 1;
    private static final int 默认准驭空双击间隔毫秒 = 500;
    private static final int 默认BossBar更新间隔Tick = 5;

    private final boolean 总开关;
    private final boolean 战斗中禁用;
    private final double 活力上限;
    private final double 飞行每秒消耗;
    private final double 地面每秒恢复;
    private final double 飞行每秒恢复;
    private final double 起飞消耗;
    private final double 起飞升空高度;
    private final int 起飞升空时长;
    private final double 冲刺消耗;
    private final int 冲刺冷却;
    private final double 冲刺推力;
    private final int 冲刺持续Tick;
    private final double 爆发消耗;
    private final int 爆发冷却;
    private final double 爆发推力;
    private final int 爆发持续Tick;
    private final double 俯冲消耗;
    private final int 俯冲冷却;
    private final double 俯冲推力;
    private final int 俯冲持续Tick;
    private final double 重力加速度;
    private final double 基础速度;
    private final double 最大速度;
    private final double 升力系数;
    private final double 空气阻力系数;
    private final double 俯冲加速倍数;
    private final double 抬头减速倍数;
    private final int 摔落保护秒数;
    private final int 准驭空超时秒;
    private final int 准驭空双击间隔毫秒;
    private final int BossBar更新间隔Tick;

    @Inject
    public 驭空术配置(JavaPlugin 插件) {
        FileConfiguration 配置 = 插件.getConfig();
        this.总开关 = 配置.getBoolean(根键 + "." + 键总开关, 默认总开关);
        this.战斗中禁用 = 配置.getBoolean(根键 + "." + 键战斗中禁用, 默认战斗中禁用);

        String 活力前缀 = 根键 + "." + 子键活力 + ".";
        this.活力上限 = 配置.getDouble(活力前缀 + 键活力上限, 默认活力上限);
        this.飞行每秒消耗 = 配置.getDouble(活力前缀 + 键飞行每秒消耗, 默认飞行每秒消耗);
        this.地面每秒恢复 = 配置.getDouble(活力前缀 + 键地面每秒恢复, 默认地面每秒恢复);
        this.飞行每秒恢复 = 配置.getDouble(活力前缀 + 键飞行每秒恢复, 默认飞行每秒恢复);

        String 起飞前缀 = 根键 + "." + 子键起飞 + ".";
        this.起飞消耗 = 配置.getDouble(起飞前缀 + 键起飞消耗, 默认起飞消耗);
        this.起飞升空高度 = 配置.getDouble(起飞前缀 + 键起飞升空高度, 默认起飞升空高度);
        this.起飞升空时长 = 配置.getInt(起飞前缀 + 键起飞升空时长, 默认起飞升空时长);

        String 冲刺前缀 = 根键 + "." + 子键冲刺 + ".";
        this.冲刺消耗 = 配置.getDouble(冲刺前缀 + 键冲刺消耗, 默认冲刺消耗);
        this.冲刺冷却 = 配置.getInt(冲刺前缀 + 键冲刺冷却, 默认冲刺冷却);
        this.冲刺推力 = 配置.getDouble(冲刺前缀 + 键冲刺推力, 默认冲刺推力);
        this.冲刺持续Tick = 配置.getInt(冲刺前缀 + 键冲刺持续Tick, 默认冲刺持续Tick);

        String 爆发前缀 = 根键 + "." + 子键爆发 + ".";
        this.爆发消耗 = 配置.getDouble(爆发前缀 + 键爆发消耗, 默认爆发消耗);
        this.爆发冷却 = 配置.getInt(爆发前缀 + 键爆发冷却, 默认爆发冷却);
        this.爆发推力 = 配置.getDouble(爆发前缀 + 键爆发推力, 默认爆发推力);
        this.爆发持续Tick = 配置.getInt(爆发前缀 + 键爆发持续Tick, 默认爆发持续Tick);

        String 俯冲前缀 = 根键 + "." + 子键俯冲 + ".";
        this.俯冲消耗 = 配置.getDouble(俯冲前缀 + 键俯冲消耗, 默认俯冲消耗);
        this.俯冲冷却 = 配置.getInt(俯冲前缀 + 键俯冲冷却, 默认俯冲冷却);
        this.俯冲推力 = 配置.getDouble(俯冲前缀 + 键俯冲推力, 默认俯冲推力);
        this.俯冲持续Tick = 配置.getInt(俯冲前缀 + 键俯冲持续Tick, 默认俯冲持续Tick);

        String 物理前缀 = 根键 + "." + 子键物理 + ".";
        this.重力加速度 = 配置.getDouble(物理前缀 + 键重力加速度, 默认重力加速度);
        this.基础速度 = 配置.getDouble(物理前缀 + 键基础速度, 默认基础速度);
        this.最大速度 = 配置.getDouble(物理前缀 + 键最大速度, 默认最大速度);
        this.升力系数 = 配置.getDouble(物理前缀 + 键升力系数, 默认升力系数);
        this.空气阻力系数 = 配置.getDouble(物理前缀 + 键空气阻力系数, 默认空气阻力系数);
        this.俯冲加速倍数 = 配置.getDouble(物理前缀 + 键俯冲加速倍数, 默认俯冲加速倍数);
        this.抬头减速倍数 = 配置.getDouble(物理前缀 + 键抬头减速倍数, 默认抬头减速倍数);

        String 摔落保护前缀 = 根键 + "." + 子键摔落保护 + ".";
        this.摔落保护秒数 = 配置.getInt(摔落保护前缀 + 键摔落保护秒数, 默认摔落保护秒数);

        String 准驭空前缀 = 根键 + "." + 子键准驭空 + ".";
        this.准驭空超时秒 = 配置.getInt(准驭空前缀 + 键准驭空超时秒, 默认准驭空超时秒);
        this.准驭空双击间隔毫秒 = 配置.getInt(准驭空前缀 + 键准驭空双击间隔毫秒, 默认准驭空双击间隔毫秒);

        String BossBar前缀 = 根键 + "." + 子键BossBar + ".";
        this.BossBar更新间隔Tick = 配置.getInt(BossBar前缀 + 键BossBar更新间隔Tick, 默认BossBar更新间隔Tick);
    }

    public boolean 总开关() {
        return 总开关;
    }

    public boolean 战斗中禁用() {
        return 战斗中禁用;
    }

    public double 活力上限() {
        return 活力上限;
    }

    public double 飞行每秒消耗() {
        return 飞行每秒消耗;
    }

    public double 地面每秒恢复() {
        return 地面每秒恢复;
    }

    public double 飞行每秒恢复() {
        return 飞行每秒恢复;
    }

    public double 起飞消耗() {
        return 起飞消耗;
    }

    public double 起飞升空高度() {
        return 起飞升空高度;
    }

    public int 起飞升空时长() {
        return 起飞升空时长;
    }

    public double 冲刺消耗() {
        return 冲刺消耗;
    }

    public int 冲刺冷却() {
        return 冲刺冷却;
    }

    public double 冲刺推力() {
        return 冲刺推力;
    }

    public int 冲刺持续Tick() {
        return 冲刺持续Tick;
    }

    public double 爆发消耗() {
        return 爆发消耗;
    }

    public int 爆发冷却() {
        return 爆发冷却;
    }

    public double 爆发推力() {
        return 爆发推力;
    }

    public int 爆发持续Tick() {
        return 爆发持续Tick;
    }

    public double 俯冲消耗() {
        return 俯冲消耗;
    }

    public int 俯冲冷却() {
        return 俯冲冷却;
    }

    public double 俯冲推力() {
        return 俯冲推力;
    }

    public int 俯冲持续Tick() {
        return 俯冲持续Tick;
    }

    public double 重力加速度() {
        return 重力加速度;
    }

    public double 基础速度() {
        return 基础速度;
    }

    public double 最大速度() {
        return 最大速度;
    }

    public double 升力系数() {
        return 升力系数;
    }

    public double 空气阻力系数() {
        return 空气阻力系数;
    }

    public double 俯冲加速倍数() {
        return 俯冲加速倍数;
    }

    public double 抬头减速倍数() {
        return 抬头减速倍数;
    }

    public int 摔落保护秒数() {
        return 摔落保护秒数;
    }

    public int 准驭空超时秒() {
        return 准驭空超时秒;
    }

    public int 准驭空双击间隔毫秒() {
        return 准驭空双击间隔毫秒;
    }

    public int BossBar更新间隔Tick() {
        return BossBar更新间隔Tick;
    }
}
