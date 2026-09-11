package mljy.业务层.额外功能;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class 额外功能配置服务 {
    private static final String 根键 = "额外功能";
    private static final String 键总开关 = "总开关";
    private static final String 键调试输出 = "调试输出";

    private static final String 子键伤害免疫 = "伤害免疫";
    private static final String 键取消玩家无敌帧 = "取消玩家无敌帧";
    private static final String 键取消生物无敌帧 = "取消生物无敌帧";
    private static final String 键监控持续秒 = "监控持续秒";
    private static final String 键监控间隔Tick = "监控间隔Tick";

    private static final String 子键岩浆块免疫 = "岩浆块免疫";
    private static final String 键玩家免疫 = "玩家免疫";
    private static final String 键狗免疫 = "狗免疫";

    private static final String 子键猪灵 = "猪灵";
    private static final String 键取消以物易物 = "取消以物易物";
    private static final String 键永久敌对 = "永久敌对";
    private static final String 键索敌范围 = "索敌范围";
    private static final String 键阻止僵尸化 = "阻止僵尸化";

    private static final String 子键蜘蛛 = "蜘蛛";
    private static final String 键检查间隔Tick = "检查间隔Tick";

    private static final String 子键末影人 = "末影人";
    private static final String 键免疫水雨伤害 = "免疫水雨伤害";
    private static final String 键禁止传送 = "禁止传送";

    private static final String 子键击退抗性 = "击退抗性";
    private static final String 键开启 = "开启";
    private static final String 键值 = "值";

    private static final boolean 默认总开关 = true;
    private static final boolean 默认调试输出 = false;
    private static final boolean 默认取消玩家无敌帧 = true;
    private static final boolean 默认取消生物无敌帧 = true;
    private static final int 默认监控持续秒 = 5;
    private static final int 默认监控间隔Tick = 1;
    private static final boolean 默认玩家岩浆块免疫 = true;
    private static final boolean 默认狗岩浆块免疫 = true;
    private static final boolean 默认猪灵取消以物易物 = true;
    private static final boolean 默认猪灵永久敌对 = true;
    private static final double 默认猪灵索敌范围 = 20.0;
    private static final boolean 默认猪灵阻止僵尸化 = true;
    private static final int 默认猪灵检查间隔Tick = 20;
    private static final boolean 默认蜘蛛永久敌对 = true;
    private static final double 默认蜘蛛索敌范围 = 20.0;
    private static final int 默认蜘蛛检查间隔Tick = 20;
    private static final boolean 默认末影人免疫水雨伤害 = true;
    private static final boolean 默认末影人禁止传送 = true;
    private static final boolean 默认击退抗性开启 = true;
    private static final double 默认击退抗性值 = 1.0;

    private final boolean 总开关;
    private final boolean 调试输出;
    private final boolean 取消玩家无敌帧;
    private final boolean 取消生物无敌帧;
    private final int 监控持续秒;
    private final int 监控间隔Tick;
    private final boolean 玩家岩浆块免疫;
    private final boolean 狗岩浆块免疫;
    private final boolean 猪灵取消以物易物;
    private final boolean 猪灵永久敌对;
    private final double 猪灵索敌范围;
    private final boolean 猪灵阻止僵尸化;
    private final int 猪灵检查间隔Tick;
    private final boolean 蜘蛛永久敌对;
    private final double 蜘蛛索敌范围;
    private final int 蜘蛛检查间隔Tick;
    private final boolean 末影人免疫水雨伤害;
    private final boolean 末影人禁止传送;
    private final boolean 击退抗性开启;
    private final double 击退抗性值;

    @Inject
    public 额外功能配置服务(JavaPlugin 插件) {
        FileConfiguration 配置 = 插件.getConfig();
        this.总开关 = 配置.getBoolean(根键 + "." + 键总开关, 默认总开关);
        this.调试输出 = 配置.getBoolean(根键 + "." + 键调试输出, 默认调试输出);

        String 伤害免疫前缀 = 根键 + "." + 子键伤害免疫 + ".";
        this.取消玩家无敌帧 = 配置.getBoolean(伤害免疫前缀 + 键取消玩家无敌帧, 默认取消玩家无敌帧);
        this.取消生物无敌帧 = 配置.getBoolean(伤害免疫前缀 + 键取消生物无敌帧, 默认取消生物无敌帧);
        this.监控持续秒 = 配置.getInt(伤害免疫前缀 + 键监控持续秒, 默认监控持续秒);
        this.监控间隔Tick = 配置.getInt(伤害免疫前缀 + 键监控间隔Tick, 默认监控间隔Tick);

        String 岩浆块前缀 = 根键 + "." + 子键岩浆块免疫 + ".";
        this.玩家岩浆块免疫 = 配置.getBoolean(岩浆块前缀 + 键玩家免疫, 默认玩家岩浆块免疫);
        this.狗岩浆块免疫 = 配置.getBoolean(岩浆块前缀 + 键狗免疫, 默认狗岩浆块免疫);

        String 猪灵前缀 = 根键 + "." + 子键猪灵 + ".";
        this.猪灵取消以物易物 = 配置.getBoolean(猪灵前缀 + 键取消以物易物, 默认猪灵取消以物易物);
        this.猪灵永久敌对 = 配置.getBoolean(猪灵前缀 + 键永久敌对, 默认猪灵永久敌对);
        this.猪灵索敌范围 = 配置.getDouble(猪灵前缀 + 键索敌范围, 默认猪灵索敌范围);
        this.猪灵阻止僵尸化 = 配置.getBoolean(猪灵前缀 + 键阻止僵尸化, 默认猪灵阻止僵尸化);
        this.猪灵检查间隔Tick = 配置.getInt(猪灵前缀 + 键检查间隔Tick, 默认猪灵检查间隔Tick);

        String 蜘蛛前缀 = 根键 + "." + 子键蜘蛛 + ".";
        this.蜘蛛永久敌对 = 配置.getBoolean(蜘蛛前缀 + 键永久敌对, 默认蜘蛛永久敌对);
        this.蜘蛛索敌范围 = 配置.getDouble(蜘蛛前缀 + 键索敌范围, 默认蜘蛛索敌范围);
        this.蜘蛛检查间隔Tick = 配置.getInt(蜘蛛前缀 + 键检查间隔Tick, 默认蜘蛛检查间隔Tick);

        String 末影人前缀 = 根键 + "." + 子键末影人 + ".";
        this.末影人免疫水雨伤害 = 配置.getBoolean(末影人前缀 + 键免疫水雨伤害, 默认末影人免疫水雨伤害);
        this.末影人禁止传送 = 配置.getBoolean(末影人前缀 + 键禁止传送, 默认末影人禁止传送);

        String 击退抗性前缀 = 根键 + "." + 子键击退抗性 + ".";
        this.击退抗性开启 = 配置.getBoolean(击退抗性前缀 + 键开启, 默认击退抗性开启);
        this.击退抗性值 = 配置.getDouble(击退抗性前缀 + 键值, 默认击退抗性值);
    }

    public boolean 总开关() {
        return 总开关;
    }

    public boolean 调试输出() {
        return 调试输出;
    }

    public boolean 取消玩家无敌帧() {
        return 取消玩家无敌帧;
    }

    public boolean 取消生物无敌帧() {
        return 取消生物无敌帧;
    }

    public int 监控持续秒() {
        return 监控持续秒;
    }

    public int 监控间隔Tick() {
        return 监控间隔Tick;
    }

    public boolean 玩家岩浆块免疫() {
        return 玩家岩浆块免疫;
    }

    public boolean 狗岩浆块免疫() {
        return 狗岩浆块免疫;
    }

    public boolean 猪灵取消以物易物() {
        return 猪灵取消以物易物;
    }

    public boolean 猪灵永久敌对() {
        return 猪灵永久敌对;
    }

    public double 猪灵索敌范围() {
        return 猪灵索敌范围;
    }

    public boolean 猪灵阻止僵尸化() {
        return 猪灵阻止僵尸化;
    }

    public int 猪灵检查间隔Tick() {
        return 猪灵检查间隔Tick;
    }

    public boolean 蜘蛛永久敌对() {
        return 蜘蛛永久敌对;
    }

    public double 蜘蛛索敌范围() {
        return 蜘蛛索敌范围;
    }

    public int 蜘蛛检查间隔Tick() {
        return 蜘蛛检查间隔Tick;
    }

    public boolean 末影人免疫水雨伤害() {
        return 末影人免疫水雨伤害;
    }

    public boolean 末影人禁止传送() {
        return 末影人禁止传送;
    }

    public boolean 击退抗性开启() {
        return 击退抗性开启;
    }

    public double 击退抗性值() {
        return 击退抗性值;
    }
}
