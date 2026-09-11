package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.业务层.属性.修饰器管理器;
import mljy.属性服务;
import mljy.领域层.属性.属性快照;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 内存属性服务 implements 属性服务 {
    private static final String 模块名 = "内存属性服务";
    private static final String 配置文件名 = "专精属性.yml";
    private static final String 配置根键 = "专精";
    private static final String 基础属性键 = "基础属性";
    private static final String 默认专精 = "奥能法师";

    private static final String 键生命值上限 = "生命值上限";
    private static final String 键公共冷却时间 = "公共冷却时间";
    private static final String 键急速 = "急速";
    private static final String 键力量 = "力量";
    private static final String 键敏捷 = "敏捷";
    private static final String 键智力 = "智力";
    private static final String 键法术暴击几率 = "法术暴击几率";
    private static final String 键法术暴击伤害 = "法术暴击伤害";
    private static final String 键精通 = "精通";
    private static final String 键全能 = "全能";
    private static final String 键吸血 = "吸血";
    private static final String 键躲闪 = "躲闪";
    private static final String 键生命恢复 = "生命恢复";
    private static final String 键移速 = "移速";

    private static final double 默认生命值上限 = 100.0;
    private static final double 默认公共冷却时间 = 1.2;
    private static final double 默认智力 = 10.0;

    private final 修饰器管理器 修饰器管理器;
    private final Yaml配置加载器 配置加载器;
    private final Map<UUID, 属性快照> 最终属性缓存 = new ConcurrentHashMap<>();
    private final Map<UUID, 属性快照> 玩家基础属性表 = new ConcurrentHashMap<>();
    private final Map<String, 属性快照> 专精基础属性表 = new ConcurrentHashMap<>();

    @Inject
    public 内存属性服务(修饰器管理器 修饰器管理器, Yaml配置加载器 配置加载器) {
        this.修饰器管理器 = 修饰器管理器;
        this.配置加载器 = 配置加载器;
        加载专精基础属性();
    }

    private void 加载专精基础属性() {
        FileConfiguration 配置 = 配置加载器.加载(配置文件名);
        ConfigurationSection 根 = 配置.getConfigurationSection(配置根键);
        if (根 == null) {
            调试日志器.调试(模块名, "专精属性配置根键为空，专精基础属性表为空。配置文件: " + 配置文件名
                    + "，配置键数: " + 配置.getKeys(false).size());
            return;
        }
        for (String 专精名 : 根.getKeys(false)) {
            ConfigurationSection 基础属性节 = 根.getConfigurationSection(专精名 + "." + 基础属性键);
            if (基础属性节 == null) {
                调试日志器.调试(模块名, "专精缺少基础属性节，跳过: " + 专精名);
                continue;
            }
            double 力量 = 基础属性节.getDouble(键力量, 0);
            double 敏捷 = 基础属性节.getDouble(键敏捷, 0);
            double 智力 = 基础属性节.getDouble(键智力, 0);
            属性快照 快照 = 属性快照.创建(
                    基础属性节.getDouble(键生命值上限, 默认生命值上限),
                    基础属性节.getDouble(键公共冷却时间, 默认公共冷却时间),
                    基础属性节.getDouble(键急速, 0),
                    力量,
                    敏捷,
                    智力,
                    力量,
                    敏捷,
                    智力,
                    基础属性节.getDouble(键法术暴击几率, 0),
                    基础属性节.getDouble(键法术暴击伤害, 0),
                    基础属性节.getDouble(键精通, 0),
                    基础属性节.getDouble(键全能, 0),
                    基础属性节.getDouble(键吸血, 0),
                    基础属性节.getDouble(键躲闪, 0),
                    基础属性节.getDouble(键生命恢复, 0),
                    基础属性节.getDouble(键移速, 0)
            );
            专精基础属性表.put(专精名, 快照);
        }
        调试日志器.调试(模块名, "已加载专精基础属性: " + 专精基础属性表.keySet());
    }

    @Override
    public Optional<属性快照> 获取专精基础属性(String 专精) {
        return Optional.ofNullable(专精基础属性表.get(专精));
    }

    @Override
    public void 设置玩家基础属性(UUID 玩家标识, 属性快照 基础属性) {
        玩家基础属性表.put(玩家标识, 基础属性);
        最终属性缓存.remove(玩家标识);
        同步属性到玩家实体(玩家标识, 计算属性(玩家标识));
    }

    @Override
    public 属性快照 计算属性(UUID 玩家标识) {
        属性快照 缓存 = 最终属性缓存.get(玩家标识);
        if (缓存 != null) {
            return 缓存;
        }
        属性快照 基础 = 获取未修饰基础属性(玩家标识);
        属性快照 最终 = 计算修饰后属性(玩家标识, 基础);
        最终属性缓存.put(玩家标识, 最终);
        return 最终;
    }

    @Override
    public 属性快照 获取基础属性(UUID 玩家标识) {
        return 获取未修饰基础属性(玩家标识);
    }

    private 属性快照 获取未修饰基础属性(UUID 玩家标识) {
        属性快照 基础 = 玩家基础属性表.get(玩家标识);
        if (基础 == null) {
            基础 = 专精基础属性表.getOrDefault(默认专精, 构建默认属性());
        }
        return 基础;
    }

    @Override
    public void 刷新属性(UUID 玩家标识) {
        最终属性缓存.remove(玩家标识);
        属性快照 基础 = 玩家基础属性表.get(玩家标识);
        if (基础 != null) {
            属性快照 最终 = 计算修饰后属性(玩家标识, 基础);
            最终属性缓存.put(玩家标识, 最终);
            同步属性到玩家实体(玩家标识, 最终);
        }
    }

    @Override
    public void 重载() {
        专精基础属性表.clear();
        加载专精基础属性();
        最终属性缓存.clear();
        for (Player 玩家 : Bukkit.getOnlinePlayers()) {
            同步属性到玩家实体(玩家.getUniqueId(), 计算属性(玩家.getUniqueId()));
        }
        调试日志器.调试(模块名, "专精属性已热重载，在线玩家属性已同步");
    }

    private void 同步属性到玩家实体(UUID 玩家标识, 属性快照 属性) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        AttributeInstance 生命属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        if (生命属性 != null) {
            生命属性.setBaseValue(属性.生命值上限());
            if (玩家.getHealth() > 属性.生命值上限()) {
                玩家.setHealth(属性.生命值上限());
            }
        }
        AttributeInstance 移速属性 = 玩家.getAttribute(Attribute.MOVEMENT_SPEED);
        if (移速属性 != null) {
            移速属性.setBaseValue(属性.移速());
        }
    }

    private 属性快照 计算修饰后属性(UUID 玩家标识, 属性快照 基础) {
        属性快照 最终 = 属性快照.创建(
                修饰器管理器.计算最终值(玩家标识, 键生命值上限, 基础.生命值上限()),
                修饰器管理器.计算最终值(玩家标识, 键公共冷却时间, 基础.公共冷却时间()),
                修饰器管理器.计算最终值(玩家标识, 键急速, 基础.急速()),
                修饰器管理器.计算最终值(玩家标识, 键力量, 基础.力量()),
                修饰器管理器.计算最终值(玩家标识, 键敏捷, 基础.敏捷()),
                修饰器管理器.计算最终值(玩家标识, 键智力, 基础.智力()),
                基础.基础力量(),
                基础.基础敏捷(),
                基础.基础智力(),
                修饰器管理器.计算最终值(玩家标识, 键法术暴击几率, 基础.法术暴击几率()),
                修饰器管理器.计算最终值(玩家标识, 键法术暴击伤害, 基础.法术暴击伤害()),
                修饰器管理器.计算最终值(玩家标识, 键精通, 基础.精通()),
                修饰器管理器.计算最终值(玩家标识, 键全能, 基础.全能()),
                修饰器管理器.计算最终值(玩家标识, 键吸血, 基础.吸血()),
                修饰器管理器.计算最终值(玩家标识, 键躲闪, 基础.躲闪()),
                修饰器管理器.计算最终值(玩家标识, 键生命恢复, 基础.生命恢复()),
                修饰器管理器.计算最终值(玩家标识, 键移速, 基础.移速())
        );
        调试日志器.调试(模块名, "计算修饰后属性：玩家=%s 基础智力=%.1f 修饰后智力=%.1f 基础力量=%.1f 修饰后力量=%.1f",
                玩家标识, 基础.智力(), 最终.智力(), 基础.力量(), 最终.力量());
        return 最终;
    }

    private 属性快照 构建默认属性() {
        return 属性快照.创建(
                默认生命值上限,
                默认公共冷却时间,
                0,
                0,
                0,
                默认智力,
                0,
                0,
                默认智力,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }
}
