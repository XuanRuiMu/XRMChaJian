package mljy.业务层;

import com.google.inject.Inject;
import mljy.基础设施层.Yaml配置加载器;
import mljy.基础设施层.调试日志器;
import mljy.领域层.战斗.角色类型;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 职业类型服务实现。
 * 从 职业类型.yml 配置文件加载专精→角色类型映射。
 * 配置文件定义35个职业的角色类型（坦克/治疗/输出）。
 */
public class 职业类型服务实现 implements 职业类型服务 {
    private static final String 配置文件名 = "职业类型.yml";
    private static final String 配置根键 = "职业类型";

    private final Yaml配置加载器 配置加载器;
    private final Map<String, 角色类型> 职业类型表 = new ConcurrentHashMap<>();

    @Inject
    public 职业类型服务实现(Yaml配置加载器 配置加载器) {
        this.配置加载器 = 配置加载器;
        加载职业类型();
    }

    private void 加载职业类型() {
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            return;
        }
        for (String 专精名 : 根.getKeys(false)) {
            String 类型名 = 根.getString(专精名);
            if (类型名 == null) {
                continue;
            }
            try {
                角色类型 类型 = 角色类型.valueOf(类型名);
                职业类型表.put(专精名, 类型);
            } catch (IllegalArgumentException 忽略) {
                调试日志器.调试("职业类型服务", "职业类型配置错误 专精=%s 类型=%s", 专精名, 类型名);
            }
        }
    }

    @Override
    public Optional<角色类型> 获取角色类型(String 专精名) {
        if (专精名 == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(职业类型表.get(专精名));
    }

    @Override
    public 角色类型 获取角色类型或默认(String 专精名, 角色类型 默认类型) {
        return 获取角色类型(专精名).orElse(默认类型);
    }
}
