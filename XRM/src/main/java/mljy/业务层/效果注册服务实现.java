package mljy.业务层;

import com.google.inject.Inject;
import mljy.基础设施层.Yaml配置加载器;
import mljy.领域层.效果.效果定义;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class 效果注册服务实现 implements 效果注册服务 {
    private static final String 配置文件名 = "效果定义.yml";
    private static final String 配置根键 = "效果";
    private static final String 键名称 = "名称";
    private static final String 键名称翻译键 = "名称翻译键";
    private static final String 键描述翻译键 = "描述翻译键";
    private static final String 键处理器 = "处理器";
    private static final String 键持续时间 = "持续时间";
    private static final String 键最大层数 = "最大层数";
    private static final String 键可驱散 = "可驱散";
    private static final String 键可叠加 = "可叠加";

    private static final long 默认持续时间 = 效果定义.永久持续;
    private static final int 默认最大层数 = 1;
    private static final boolean 默认可驱散 = false;
    private static final boolean 默认可叠加 = false;

    private final Yaml配置加载器 配置加载器;
    private final Map<String, 效果定义> 效果定义表 = new ConcurrentHashMap<>();

    @Inject
    public 效果注册服务实现(Yaml配置加载器 配置加载器) {
        this.配置加载器 = 配置加载器;
        加载效果定义();
    }

    private void 加载效果定义() {
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            return;
        }
        for (String 效果标识 : 根.getKeys(false)) {
            ConfigurationSection 节 = 根.getConfigurationSection(效果标识);
            if (节 == null) {
                continue;
            }
            效果定义 定义 = 解析效果定义(效果标识, 节);
            效果定义表.put(效果标识, 定义);
        }
    }

    private 效果定义 解析效果定义(String 效果标识, ConfigurationSection 节) {
        return new 效果定义(
                效果标识,
                节.getString(键名称, 效果标识),
                节.getString(键名称翻译键, ""),
                节.getString(键描述翻译键, ""),
                节.getString(键处理器, ""),
                节.getLong(键持续时间, 默认持续时间),
                节.getInt(键最大层数, 默认最大层数),
                节.getBoolean(键可驱散, 默认可驱散),
                节.getBoolean(键可叠加, 默认可叠加)
        );
    }

    @Override
    public void 注册(效果定义 定义) {
        效果定义表.put(定义.效果标识(), 定义);
    }

    @Override
    public Optional<效果定义> 获取定义(String 效果标识) {
        return Optional.ofNullable(效果定义表.get(效果标识));
    }

    @Override
    public Collection<效果定义> 获取所有定义() {
        return Collections.unmodifiableCollection(效果定义表.values());
    }

    @Override
    public boolean 存在(String 效果标识) {
        return 效果定义表.containsKey(效果标识);
    }
}
