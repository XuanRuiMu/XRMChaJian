package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.属性服务;
import mljy.数据服务;
import mljy.基础设施层.Bukkit适配.位置适配器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 内存玩家服务 implements 玩家服务 {
    private static final String 模块名 = "内存玩家服务";
    private static final String 配置根键 = "玩家默认设置";
    private static final String 键默认职业 = "默认职业";
    private static final String 键默认专精 = "默认专精";
    private static final String 键默认技能日志开启 = "默认技能日志开启";
    private static final String 键默认战斗日志开启 = "默认战斗日志开启";
    private static final String 默认职业值 = "魔法世界";
    private static final String 默认专精值 = "奥能法师";
    private static final boolean 默认技能日志开启值 = true;
    private static final boolean 默认战斗日志开启值 = true;

    private final 数据服务 数据服务;
    private final 属性服务 属性服务;
    private final Map<UUID, 玩家快照> 快照缓存 = new ConcurrentHashMap<>();
    private final String 默认职业;
    private final String 默认专精;
    private final boolean 默认技能日志开启;
    private final boolean 默认战斗日志开启;

    @Inject
    public 内存玩家服务(数据服务 数据服务, 属性服务 属性服务, JavaPlugin 插件) {
        this.数据服务 = 数据服务;
        this.属性服务 = 属性服务;
        FileConfiguration 配置 = 插件.getConfig();
        this.默认职业 = 配置.getString(配置根键 + "." + 键默认职业, 默认职业值);
        this.默认专精 = 配置.getString(配置根键 + "." + 键默认专精, 默认专精值);
        this.默认技能日志开启 = 配置.getBoolean(配置根键 + "." + 键默认技能日志开启, 默认技能日志开启值);
        this.默认战斗日志开启 = 配置.getBoolean(配置根键 + "." + 键默认战斗日志开启, 默认战斗日志开启值);
    }

    @Override
    public Optional<玩家快照> 获取快照(UUID 玩家标识) {
        玩家快照 缓存 = 快照缓存.get(玩家标识);
        if (缓存 == null) {
            return Optional.empty();
        }
        属性快照 实时属性 = 属性服务.计算属性(玩家标识);
        return Optional.of(new 玩家快照(
                缓存.唯一标识(), 缓存.名称(), 缓存.等级(), 缓存.位置(),
                实时属性, 缓存.资源表(), 缓存.战斗状态()
        ));
    }

    @Override
    public Optional<玩家会话> 获取会话(UUID 玩家标识) {
        return 数据服务.加载(玩家标识);
    }

    @Override
    public boolean 获取默认技能日志开关() {
        return 默认技能日志开启;
    }

    @Override
    public boolean 获取默认战斗日志开关() {
        return 默认战斗日志开启;
    }

    @Override
    public void 玩家登录(UUID 玩家标识, String 名称) {
        玩家会话 会话 = 加载或创建会话(玩家标识, 名称);
        会话.设置登录状态(true);
        数据服务.保存(会话);
        玩家快照 快照 = 构建快照(会话);
        快照缓存.put(玩家标识, 快照);
    }

    @Override
    public void 玩家退出(UUID 玩家标识) {
        数据服务.加载(玩家标识).ifPresent(会话 -> {
            会话.设置登录状态(false);
            数据服务.保存(会话);
        });
        快照缓存.remove(玩家标识);
    }

    private 玩家会话 加载或创建会话(UUID 玩家标识, String 名称) {
        Optional<玩家会话> 已存在会话 = 数据服务.加载(玩家标识);
        if (已存在会话.isPresent()) {
            return 已存在会话.get();
        }
        玩家会话 新会话 = new 玩家会话(玩家标识, 名称);
        新会话.设置职业(默认职业);
        新会话.设置专精(默认专精);
        新会话.设置技能日志开关(默认技能日志开启);
        新会话.设置战斗日志开关(默认战斗日志开启);
        return 新会话;
    }

    private 玩家快照 构建快照(玩家会话 会话) {
        UUID 玩家标识 = 会话.获取玩家标识();
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        位置 位置 = 玩家 != null ? 位置适配器.转换(玩家.getLocation()) : new 位置("world", 0, 0, 0, 0, 0);
        属性快照 属性 = 构建基础属性(会话.获取专精(), 会话.获取等级());
        属性服务.设置玩家基础属性(玩家标识, 属性);
        return new 玩家快照(玩家标识, 会话.获取名称(), 会话.获取等级(), 位置, 属性, Collections.emptyMap(), mljy.领域层.战斗.战斗状态.非战斗);
    }

    private 属性快照 构建基础属性(String 专精, int 等级) {
        int 有效等级 = Math.max(等级, 1);
        Optional<属性快照> 专精属性 = 属性服务.获取专精基础属性(专精);
        if (专精属性.isPresent()) {
            return 应用等级缩放(专精属性.get(), 有效等级);
        }
        调试日志器.调试(模块名, "专精基础属性缺失，回退到默认专精: 专精=" + 专精);
        Optional<属性快照> 默认属性 = 属性服务.获取专精基础属性(默认专精);
        if (默认属性.isPresent()) {
            return 应用等级缩放(默认属性.get(), 有效等级);
        }
        调试日志器.调试(模块名, "默认专精基础属性也缺失，回退到兜底属性（副属性全0）: 默认专精=" + 默认专精);
        return 应用等级缩放(构建兜底属性(), 有效等级);
    }

    private 属性快照 应用等级缩放(属性快照 基础, int 等级) {
        if (等级 <= 1) {
            return 基础;
        }
        return 属性快照.创建(
                基础.生命值上限() * 等级,
                基础.公共冷却时间(),
                基础.急速(),
                基础.力量() * 等级,
                基础.敏捷() * 等级,
                基础.智力() * 等级,
                基础.基础力量() * 等级,
                基础.基础敏捷() * 等级,
                基础.基础智力() * 等级,
                基础.法术暴击几率(),
                基础.法术暴击伤害(),
                基础.精通(),
                基础.全能(),
                基础.吸血(),
                基础.躲闪(),
                基础.生命恢复() * 等级,
                基础.移速()
        );
    }

    private 属性快照 构建兜底属性() {
        return 属性快照.创建(
                100.0,
                1.0,
                0.0,
                0.0,
                0.0,
                10.0,
                0.0,
                0.0,
                10.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }
}
