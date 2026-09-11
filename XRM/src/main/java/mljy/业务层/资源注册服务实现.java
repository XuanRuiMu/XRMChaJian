package mljy.业务层;

import com.google.inject.Inject;
import mljy.领域层.资源.资源定义;
import mljy.基础设施层.Yaml配置加载器;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class 资源注册服务实现 implements 资源注册服务 {
    private static final String 配置文件名 = "资源定义.yml";
    private static final String 配置根键 = "resources";
    private static final String 键名称 = "名称";
    private static final String 键默认上限 = "默认上限";
    private static final String 键每秒恢复 = "每秒恢复";
    private static final String 键显示键 = "显示键";
    private static final double 默认上限兜底 = 0.0;
    private static final double 默认每秒恢复兜底 = 0.0;

    private final Map<String, 资源定义> 资源定义表 = new ConcurrentHashMap<>();

    @Inject
    public 资源注册服务实现(Yaml配置加载器 配置加载器) {
        加载资源定义(配置加载器);
    }

    private void 加载资源定义(Yaml配置加载器 配置加载器) {
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            return;
        }
        for (String 资源标识 : 根.getKeys(false)) {
            ConfigurationSection 资源节 = 根.getConfigurationSection(资源标识);
            if (资源节 == null) {
                continue;
            }
            String 名称 = 资源节.getString(键名称, 资源标识);
            double 默认上限 = 资源节.getDouble(键默认上限, 默认上限兜底);
            double 每秒恢复 = 资源节.getDouble(键每秒恢复, 默认每秒恢复兜底);
            String 显示键 = 资源节.getString(键显示键, "resource." + 资源标识);
            资源定义 定义 = new 资源定义(资源标识, 名称, 默认上限, 每秒恢复, 显示键);
            资源定义表.put(资源标识, 定义);
        }
    }

    @Override
    public Optional<资源定义> 获取资源定义(String 资源标识) {
        return Optional.ofNullable(资源定义表.get(资源标识));
    }

    @Override
    public Collection<资源定义> 获取所有资源定义() {
        return Collections.unmodifiableCollection(资源定义表.values());
    }

    @Override
    public boolean 资源是否存在(String 资源标识) {
        return 资源定义表.containsKey(资源标识);
    }
}
