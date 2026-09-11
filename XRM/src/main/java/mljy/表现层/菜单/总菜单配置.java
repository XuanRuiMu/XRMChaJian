package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class 总菜单配置 {
    private static final String 根键 = "总菜单";
    private static final String 键菜单大小 = "菜单大小";

    private static final String 子键任务系统 = "任务系统";
    private static final String 子键属性系统 = "属性系统";
    private static final String 子键技能日志设置 = "技能日志设置";
    private static final String 子键战斗日志设置 = "战斗日志设置";

    private static final String 键槽位 = "槽位";
    private static final String 键物品材质 = "物品材质";
    private static final String 键命令 = "命令";

    private static final int 默认菜单大小 = 54;
    private static final int 默认任务系统槽位 = 20;
    private static final int 默认属性系统槽位 = 28;
    private static final int 默认技能日志槽位 = 11;
    private static final int 默认战斗日志槽位 = 13;
    private static final String 默认任务系统物品 = "BOOK";
    private static final String 默认属性系统物品 = "DIAMOND_SWORD";
    private static final String 默认技能日志物品 = "FEATHER";
    private static final String 默认战斗日志物品 = "IRON_SWORD";
    private static final String 默认属性命令 = "sx";

    private final int 菜单大小;
    private final int 任务系统槽位;
    private final int 属性系统槽位;
    private final int 技能日志槽位;
    private final int 战斗日志槽位;
    private final String 任务系统物品;
    private final String 属性系统物品;
    private final String 技能日志物品;
    private final String 战斗日志物品;
    private final String 属性命令;

    @Inject
    public 总菜单配置(JavaPlugin 插件) {
        this(插件.getConfig());
    }

    public 总菜单配置(FileConfiguration 配置) {
        this.菜单大小 = 配置.getInt(根键 + "." + 键菜单大小, 默认菜单大小);

        String 任务系统前缀 = 根键 + "." + 子键任务系统 + ".";
        this.任务系统槽位 = 配置.getInt(任务系统前缀 + 键槽位, 默认任务系统槽位);
        this.任务系统物品 = 配置.getString(任务系统前缀 + 键物品材质, 默认任务系统物品);

        String 属性系统前缀 = 根键 + "." + 子键属性系统 + ".";
        this.属性系统槽位 = 配置.getInt(属性系统前缀 + 键槽位, 默认属性系统槽位);
        this.属性系统物品 = 配置.getString(属性系统前缀 + 键物品材质, 默认属性系统物品);
        this.属性命令 = 配置.getString(属性系统前缀 + 键命令, 默认属性命令);

        String 技能日志前缀 = 根键 + "." + 子键技能日志设置 + ".";
        this.技能日志槽位 = 配置.getInt(技能日志前缀 + 键槽位, 默认技能日志槽位);
        this.技能日志物品 = 配置.getString(技能日志前缀 + 键物品材质, 默认技能日志物品);

        String 战斗日志前缀 = 根键 + "." + 子键战斗日志设置 + ".";
        this.战斗日志槽位 = 配置.getInt(战斗日志前缀 + 键槽位, 默认战斗日志槽位);
        this.战斗日志物品 = 配置.getString(战斗日志前缀 + 键物品材质, 默认战斗日志物品);
    }

    public int 菜单大小() {
        return 菜单大小;
    }

    public int 任务系统槽位() {
        return 任务系统槽位;
    }

    public int 属性系统槽位() {
        return 属性系统槽位;
    }

    public int 技能日志槽位() {
        return 技能日志槽位;
    }

    public int 战斗日志槽位() {
        return 战斗日志槽位;
    }

    public String 任务系统物品() {
        return 任务系统物品;
    }

    public String 属性系统物品() {
        return 属性系统物品;
    }

    public String 技能日志物品() {
        return 技能日志物品;
    }

    public String 战斗日志物品() {
        return 战斗日志物品;
    }

    public String 属性命令() {
        return 属性命令;
    }
}
