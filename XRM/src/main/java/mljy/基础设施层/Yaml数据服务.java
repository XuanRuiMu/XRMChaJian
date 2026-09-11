package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.数据服务;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Yaml数据服务 implements 数据服务 {
    private static final String 数据目录名 = "玩家数据";
    private static final String 文件后缀 = ".yml";

    private static final String 键玩家标识 = "玩家标识";
    private static final String 键名称 = "名称";
    private static final String 键职业 = "职业";
    private static final String 键专精 = "专精";
    private static final String 键等级 = "等级";
    private static final String 键技能日志开关 = "技能日志开关";
    private static final String 键战斗日志开关 = "战斗日志开关";
    private static final String 键已选天赋 = "已选天赋";

    private final Logger 日志器;
    private final Map<UUID, 玩家会话> 会话缓存 = new ConcurrentHashMap<>();
    private final File 数据目录;

    @Inject
    public Yaml数据服务(JavaPlugin 插件) {
        this.日志器 = 插件.getLogger();
        this.数据目录 = new File(插件.getDataFolder(), 数据目录名);
        if (!数据目录.exists()) {
            数据目录.mkdirs();
        }
    }

    @Override
    public void 保存(玩家会话 会话) {
        会话缓存.put(会话.获取玩家标识(), 会话);
        保存到文件(会话);
    }

    @Override
    public Optional<玩家会话> 加载(UUID 玩家标识) {
        玩家会话 缓存会话 = 会话缓存.get(玩家标识);
        if (缓存会话 != null) {
            return Optional.of(缓存会话);
        }
        return 从文件加载(玩家标识);
    }

    @Override
    public boolean 存在(UUID 玩家标识) {
        if (会话缓存.containsKey(玩家标识)) {
            return true;
        }
        return 获取数据文件(玩家标识).exists();
    }

    private File 获取数据文件(UUID 玩家标识) {
        return new File(数据目录, 玩家标识.toString() + 文件后缀);
    }

    private Optional<玩家会话> 从文件加载(UUID 玩家标识) {
        File 文件 = 获取数据文件(玩家标识);
        if (!文件.exists()) {
            return Optional.empty();
        }
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(文件);
        String 名称 = 配置.getString(键名称, "");
        玩家会话 会话 = new 玩家会话(玩家标识, 名称);
        会话.设置职业(配置.getString(键职业, 会话.获取职业()));
        会话.设置专精(配置.getString(键专精, 会话.获取专精()));
        会话.设置等级(配置.getInt(键等级, 会话.获取等级()));
        会话.设置技能日志开关(配置.getBoolean(键技能日志开关, 会话.获取技能日志开关()));
        会话.设置战斗日志开关(配置.getBoolean(键战斗日志开关, 会话.获取战斗日志开关()));
        List<String> 天赋列表 = 配置.getStringList(键已选天赋);
        for (String 天赋 : 天赋列表) {
            会话.添加天赋(天赋);
        }
        会话.设置登录状态(false);
        会话缓存.put(玩家标识, 会话);
        return Optional.of(会话);
    }

    private void 保存到文件(玩家会话 会话) {
        YamlConfiguration 配置 = new YamlConfiguration();
        配置.set(键玩家标识, 会话.获取玩家标识().toString());
        配置.set(键名称, 会话.获取名称());
        配置.set(键职业, 会话.获取职业());
        配置.set(键专精, 会话.获取专精());
        配置.set(键等级, 会话.获取等级());
        配置.set(键技能日志开关, 会话.获取技能日志开关());
        配置.set(键战斗日志开关, 会话.获取战斗日志开关());
        配置.set(键已选天赋, new ArrayList<>(会话.获取已选天赋()));

        File 文件 = 获取数据文件(会话.获取玩家标识());
        try {
            配置.save(文件);
        } catch (IOException 异常) {
            日志器.log(Level.SEVERE, "保存玩家数据失败: " + 会话.获取玩家标识(), 异常);
        }
    }
}
