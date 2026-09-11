package mljy.表现层.菜单;

import com.google.inject.Inject;
import mljy.基础设施层.Yaml配置加载器;
import mljy.基础设施层.调试日志器;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class 菜单配置加载器 {
    private static final String 标题键 = "标题";
    private static final String 大小键 = "大小";
    private static final String 权限键 = "权限";
    private static final String 条件键 = "条件";
    private static final String 按钮键 = "按钮";
    private static final String 槽位键 = "槽位";
    private static final String 物品键 = "物品";
    private static final String 显示名键 = "显示名";
    private static final String 描述键 = "描述";
    private static final String 按钮权限键 = "权限";
    private static final String 按钮条件键 = "条件";
    private static final String 动作键 = "动作";
    private static final String 类型键 = "类型";
    private static final String 参数键 = "参数";
    private static final String 附魔光效键 = "附魔光效";
    private static final String 菜单目录 = "菜单";

    private final Yaml配置加载器 yaml加载器;

    @Inject
    public 菜单配置加载器(Yaml配置加载器 yaml加载器) {
        this.yaml加载器 = yaml加载器;
    }

    public 菜单配置 加载(String 文件名) {
        FileConfiguration 配置 = yaml加载器.加载(菜单目录 + "/" + 文件名);
        return 解析配置(配置);
    }

    private 菜单配置 解析配置(FileConfiguration 配置) {
        String 标题 = 配置.getString(标题键, "");
        int 大小 = 配置.getInt(大小键, 54);
        String 权限 = 配置.getString(权限键, "");
        List<String> 条件 = 配置.getStringList(条件键);
        List<菜单按钮> 按钮 = 解析按钮(配置);
        return new 菜单配置(标题, 大小, 权限, 条件, 按钮);
    }

    private List<菜单按钮> 解析按钮(FileConfiguration 配置) {
        List<菜单按钮> 按钮列表 = new ArrayList<>();
        ConfigurationSection 按钮区 = 配置.getConfigurationSection(按钮键);
        if (按钮区 == null) {
            return 按钮列表;
        }
        for (String 键 : 按钮区.getKeys(false)) {
            ConfigurationSection 按钮配置 = 按钮区.getConfigurationSection(键);
            if (按钮配置 == null) {
                continue;
            }
            菜单按钮 按钮 = 解析单个按钮(按钮配置);
            按钮列表.add(按钮);
        }
        return 按钮列表;
    }

    private 菜单按钮 解析单个按钮(ConfigurationSection 配置) {
        int 槽位 = 配置.getInt(槽位键, 0);
        String 物品 = 配置.getString(物品键, "STONE");
        String 显示名 = 配置.getString(显示名键, "");
        List<String> 描述 = 配置.getStringList(描述键);
        String 权限 = 配置.getString(按钮权限键, "");
        List<String> 条件 = 配置.getStringList(按钮条件键);
        Map<String, 菜单动作> 动作表 = 解析动作(配置);
        boolean 附魔光效 = 配置.getBoolean(附魔光效键, false);
        return new 菜单按钮(槽位, 物品, 显示名, 描述, 权限, 条件, 动作表, 附魔光效);
    }

    private Map<String, 菜单动作> 解析动作(ConfigurationSection 配置) {
        Map<String, 菜单动作> 动作表 = new HashMap<>();
        ConfigurationSection 动作区 = 配置.getConfigurationSection(动作键);
        if (动作区 == null) {
            return 动作表;
        }
        for (String 点击类型 : 动作区.getKeys(false)) {
            ConfigurationSection 动作配置 = 动作区.getConfigurationSection(点击类型);
            if (动作配置 == null) {
                continue;
            }
            菜单动作 动作 = 解析单个动作(动作配置);
            if (动作 != null) {
                动作表.put(点击类型, 动作);
            }
        }
        return 动作表;
    }

    private 菜单动作 解析单个动作(ConfigurationSection 配置) {
        String 类型名 = 配置.getString(类型键, "");
        String 参数 = 配置.getString(参数键, "");
        try {
            菜单动作类型 类型 = 菜单动作类型.valueOf(类型名);
            return new 菜单动作(类型, 参数);
        } catch (IllegalArgumentException e) {
            调试日志器.调试("菜单配置加载器", "解析菜单动作类型失败: %s", e.toString());
            return null;
        }
    }
}
