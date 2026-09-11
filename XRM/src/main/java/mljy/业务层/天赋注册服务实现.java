package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.Yaml配置加载器;
import mljy.领域层.天赋.天赋图;
import mljy.领域层.天赋.天赋节点;
import mljy.领域层.天赋.天赋类型;
import mljy.领域层.属性.修饰方式;
import mljy.领域层.属性.修饰器;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class 天赋注册服务实现 implements 天赋注册服务 {

    private static final String 配置文件名 = "天赋定义.yml";
    private static final String 配置根键 = "天赋树";
    private static final String 键最大点数 = "最大点数";
    private static final String 键节点 = "节点";
    private static final String 键类型 = "类型";
    private static final String 键坐标X = "坐标X";
    private static final String 键坐标Y = "坐标Y";
    private static final String 键前置 = "前置";
    private static final String 键选择组 = "选择组";
    private static final String 键所需总点数 = "所需总点数";
    private static final String 键影响技能 = "影响技能";
    private static final String 键名称翻译键 = "名称翻译键";
    private static final String 键描述翻译键 = "描述翻译键";
    private static final String 键修饰器 = "修饰器";
    private static final String 键修饰器类别 = "类别";
    private static final String 键修饰器方式 = "方式";
    private static final String 键修饰器数值 = "数值";
    private static final String 键修饰器优先级 = "优先级";
    private static final String 修饰器标签前缀 = "天赋_";
    private static final int 默认最大点数 = 10;
    private static final int 默认所需总点数 = 0;
    private static final int 默认修饰器优先级 = 0;
    private static final String 默认选择组 = "";

    private final Yaml配置加载器 配置加载器;
    private final Map<String, 天赋图> 天赋图表 = new ConcurrentHashMap<>();

    @Inject
    public 天赋注册服务实现(Yaml配置加载器 配置加载器) {
        this.配置加载器 = 配置加载器;
        加载天赋定义();
    }

    private void 加载天赋定义() {
        天赋图表.clear();
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            return;
        }
        for (String 专精 : 根.getKeys(false)) {
            ConfigurationSection 专精节 = 根.getConfigurationSection(专精);
            if (专精节 == null) {
                continue;
            }
            try {
                天赋图 图 = 解析天赋图(专精, 专精节);
                天赋图表.put(专精, 图);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("加载专精「" + 专精 + "」的天赋树失败: " + e.getMessage(), e);
            }
        }
    }

    private 天赋图 解析天赋图(String 专精, ConfigurationSection 专精节) {
        int 最大点数 = 专精节.getInt(键最大点数, 默认最大点数);
        ConfigurationSection 节点根 = 专精节.getConfigurationSection(键节点);
        if (节点根 == null) {
            return new 天赋图(专精, 最大点数, List.of());
        }
        List<天赋节点> 节点列表 = new ArrayList<>();
        for (String 天赋标识 : 节点根.getKeys(false)) {
            ConfigurationSection 节点节 = 节点根.getConfigurationSection(天赋标识);
            if (节点节 == null) {
                continue;
            }
            天赋节点 节点 = 解析天赋节点(专精, 天赋标识, 节点节);
            节点列表.add(节点);
        }
        return new 天赋图(专精, 最大点数, 节点列表);
    }

    private 天赋节点 解析天赋节点(String 专精, String 天赋标识, ConfigurationSection 节点节) {
        天赋类型 类型 = 解析天赋类型(节点节.getString(键类型, "普通"));
        int 坐标X = 节点节.getInt(键坐标X, 0);
        int 坐标Y = 节点节.getInt(键坐标Y, 0);
        List<String> 前置列表 = 节点节.getStringList(键前置);
        String 选择组 = 节点节.getString(键选择组, 默认选择组);
        int 所需总点数 = 节点节.getInt(键所需总点数, 默认所需总点数);
        List<String> 影响技能 = 节点节.getStringList(键影响技能);
        String 名称翻译键 = 节点节.getString(键名称翻译键, "talent." + 专精 + "." + 天赋标识 + ".name");
        String 描述翻译键 = 节点节.getString(键描述翻译键, "talent." + 专精 + "." + 天赋标识 + ".description");
        List<修饰器> 修饰器列表 = 解析修饰器列表(天赋标识, 节点节);

        return 天赋节点.of(
                天赋标识,
                专精,
                类型,
                坐标X,
                坐标Y,
                前置列表,
                选择组,
                所需总点数,
                影响技能,
                修饰器列表,
                名称翻译键,
                描述翻译键
        );
    }

    private 天赋类型 解析天赋类型(String 类型名) {
        return switch (类型名) {
            case "选择" -> 天赋类型.选择;
            case "关键" -> 天赋类型.关键;
            default -> 天赋类型.普通;
        };
    }

    private List<修饰器> 解析修饰器列表(String 天赋标识, ConfigurationSection 节点节) {
        List<修饰器> 列表 = new ArrayList<>();
        List<Map<?, ?>> 原始列表 = 节点节.getMapList(键修饰器);
        String 标签 = 修饰器标签前缀 + 天赋标识;
        for (Map<?, ?> 项 : 原始列表) {
            String 类别 = String.valueOf(项.get(键修饰器类别));
            修饰方式 方式 = 解析修饰方式(String.valueOf(项.get(键修饰器方式)));
            double 数值 = 项.get(键修饰器数值) instanceof Number 数 ? 数.doubleValue() : 0.0;
            int 优先级 = 项.get(键修饰器优先级) instanceof Number 数 ? 数.intValue() : 默认修饰器优先级;
            列表.add(new 修饰器(类别, 标签, 方式, 数值, 优先级));
        }
        return 列表;
    }

    private 修饰方式 解析修饰方式(String 方式名) {
        return switch (方式名) {
            case "相乘" -> 修饰方式.相乘;
            case "覆盖" -> 修饰方式.覆盖;
            default -> 修饰方式.相加;
        };
    }

    @Override
    public Optional<天赋图> 获取天赋图(String 专精) {
        return Optional.ofNullable(天赋图表.get(专精));
    }

    @Override
    public Optional<天赋节点> 获取天赋定义(String 专精, String 天赋标识) {
        天赋图 图 = 天赋图表.get(专精);
        if (图 == null) {
            return Optional.empty();
        }
        return 图.获取节点(天赋标识);
    }

    @Override
    public Collection<天赋节点> 获取所有天赋定义(String 专精) {
        天赋图 图 = 天赋图表.get(专精);
        if (图 == null) {
            return Collections.emptyList();
        }
        return 图.获取所有节点();
    }

    @Override
    public boolean 存在天赋树(String 专精) {
        return 天赋图表.containsKey(专精);
    }

    @Override
    public Collection<String> 获取所有专精() {
        return Collections.unmodifiableSet(天赋图表.keySet());
    }

    @Override
    public void 重载() {
        加载天赋定义();
    }
}
