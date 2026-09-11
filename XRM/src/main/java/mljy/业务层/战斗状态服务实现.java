package mljy.业务层;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗状态服务实现。
 * 进入战斗条件：造成伤害、受到伤害、治疗、释放技能。
 * 脱战判定：5秒无战斗行为自动脱战。
 * 脱战回血：基于生命恢复属性自动回复（生命恢复只有脱战情况下才会触发）。
 */
public class 战斗状态服务实现 implements 战斗状态服务 {
    private static final String 配置根键 = "战斗";
    private static final String 键战斗状态退出秒数 = "战斗状态退出秒数";
    private static final long 默认战斗状态退出秒数 = 5L;
    private static final long 毫秒每秒 = 1000L;

    private static final String 进入战斗日志键 = "战斗日志.进入战斗";
    private static final String 脱离战斗日志键 = "战斗日志.脱离战斗";
    private static final String 脱战回血日志键 = "战斗日志.脱战回血";
    private static final String 数值格式 = "%.1f";

    private static final String 秘能资源标识 = "秘能";
    private static final long 秘能衰减间隔毫秒 = 5000L;
    private static final double 秘能衰减量 = 1.0;

    private final 玩家服务 玩家服务;
    private final 属性计算服务 属性计算服务;
    private final 消息服务 消息服务;
    private final 资源变更服务 资源变更服务;

    private final Set<UUID> 战斗中玩家 = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> 最后战斗时间 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> 脱战时间表 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> 上次衰减时间表 = new ConcurrentHashMap<>();
    private volatile boolean 调试模式 = false;

    private final long 战斗状态退出毫秒;

    @Inject
    public 战斗状态服务实现(玩家服务 玩家服务, 属性计算服务 属性计算服务, 消息服务 消息服务, 资源变更服务 资源变更服务, JavaPlugin 插件) {
        this.玩家服务 = 玩家服务;
        this.属性计算服务 = 属性计算服务;
        this.消息服务 = 消息服务;
        this.资源变更服务 = 资源变更服务;
        FileConfiguration 配置 = 插件.getConfig();
        long 退出秒数 = 配置.getLong(配置根键 + "." + 键战斗状态退出秒数, 默认战斗状态退出秒数);
        this.战斗状态退出毫秒 = 退出秒数 * 毫秒每秒;
    }

    @Override
    public void 进入战斗(UUID 玩家标识) {
        进入战斗(玩家标识, null);
    }

    @Override
    public void 进入战斗(UUID 玩家标识, UUID 事件标识) {
        调试日志器.调试("战斗状态服务", "进入战斗：玩家=%s 事件=%s", 玩家标识, 事件标识);
        boolean 新进入 = !战斗中玩家.contains(玩家标识);
        战斗中玩家.add(玩家标识);
        最后战斗时间.put(玩家标识, System.currentTimeMillis());
        脱战时间表.remove(玩家标识);
        上次衰减时间表.remove(玩家标识);
        if (新进入) {
            发送战斗日志共享事件(玩家标识, 事件标识, 进入战斗日志键);
        }
    }

    @Override
    public 战斗状态 获取战斗状态(UUID 玩家标识) {
        return 战斗中玩家.contains(玩家标识) ? 战斗状态.战斗中 : 战斗状态.非战斗;
    }

    @Override
    public boolean 是否战斗中(UUID 玩家标识) {
        return 战斗中玩家.contains(玩家标识);
    }

    @Override
    public void 强制脱战(UUID 玩家标识) {
        调试日志器.调试("战斗状态服务", "强制脱战：玩家=%s", 玩家标识);
        战斗中玩家.remove(玩家标识);
        最后战斗时间.remove(玩家标识);
        发送战斗日志独立(玩家标识, 脱离战斗日志键);
    }

    @Override
    public void 刷新脱战() {
        long 当前时间 = System.currentTimeMillis();
        List<UUID> 待脱战 = new ArrayList<>();
        for (java.util.Map.Entry<UUID, Long> 条目 : 最后战斗时间.entrySet()) {
            UUID 玩家标识 = 条目.getKey();
            long 最后时间 = 条目.getValue();
            if (当前时间 - 最后时间 >= 战斗状态退出毫秒) {
                待脱战.add(玩家标识);
            }
        }
        for (UUID 玩家标识 : 待脱战) {
            战斗中玩家.remove(玩家标识);
            最后战斗时间.remove(玩家标识);
            long 脱战时间 = System.currentTimeMillis();
            脱战时间表.put(玩家标识, 脱战时间);
            上次衰减时间表.put(玩家标识, 脱战时间);
            发送战斗日志独立(玩家标识, 脱离战斗日志键);
            if (调试模式) {
                Player 玩家 = Bukkit.getPlayer(玩家标识);
                String 名字 = 玩家 != null ? 玩家.getName() : 玩家标识.toString();
                调试日志器.调试("战斗状态服务", "玩家 %s 已脱战", 名字);
            }
        }
    }

    @Override
    public void 执行脱战回血() {
        for (Player 玩家 : Bukkit.getOnlinePlayers()) {
            UUID 玩家标识 = 玩家.getUniqueId();
            if (战斗中玩家.contains(玩家标识)) {
                continue;
            }
            为脱战玩家回血(玩家标识);
        }
    }

    @Override
    public void 执行秘能衰减() {
        long 当前时间 = System.currentTimeMillis();
        List<UUID> 待清理 = new ArrayList<>();
        for (Player 玩家 : Bukkit.getOnlinePlayers()) {
            UUID 玩家标识 = 玩家.getUniqueId();
            if (战斗中玩家.contains(玩家标识)) {
                continue;
            }
            double 当前秘能 = 资源变更服务.获取当前值(玩家标识, 秘能资源标识);
            if (当前秘能 <= 0) {
                待清理.add(玩家标识);
                continue;
            }
            Long 上次时间 = 上次衰减时间表.get(玩家标识);
            if (上次时间 == null) {
                上次衰减时间表.put(玩家标识, 当前时间);
                continue;
            }
            if (当前时间 - 上次时间 < 秘能衰减间隔毫秒) {
                continue;
            }
            资源变更服务.消耗(玩家标识, 秘能资源标识, 秘能衰减量);
            上次衰减时间表.put(玩家标识, 当前时间);
            调试日志器.调试("战斗状态服务", "秘能衰减：玩家=%s 衰减前=%.1f", 玩家.getName(), 当前秘能);
            if (资源变更服务.获取当前值(玩家标识, 秘能资源标识) <= 0) {
                待清理.add(玩家标识);
            }
        }
        for (UUID 玩家标识 : 待清理) {
            脱战时间表.remove(玩家标识);
            上次衰减时间表.remove(玩家标识);
        }
    }

    private void 为脱战玩家回血(UUID 玩家标识) {
        if (战斗中玩家.contains(玩家标识)) {
            return;
        }
        Long 脱战时间 = 脱战时间表.get(玩家标识);
        if (脱战时间 != null && System.currentTimeMillis() - 脱战时间 < 1000L) {
            return;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline() || 玩家.isDead()) {
            return;
        }
        double 恢复量 = 获取生命恢复(玩家标识);
        if (恢复量 <= 0) {
            return;
        }
        double 当前生命值 = 玩家.getHealth();
        double 最大生命值 = 获取最大生命值(玩家);
        if (当前生命值 >= 最大生命值) {
            return;
        }
        double 新生命值 = Math.min(当前生命值 + 恢复量, 最大生命值);
        调试日志器.调试("战斗状态服务", "脱战回血：玩家=%s 当前=%.1f 恢复=%.1f 新值=%.1f", 玩家.getName(), 当前生命值, 恢复量, 新生命值);
        玩家.setHealth(新生命值);
        double 实际恢复量 = 新生命值 - 当前生命值;
        发送战斗日志独立(玩家标识, 脱战回血日志键, String.format(数值格式, 实际恢复量));
    }

    private double 获取生命恢复(UUID 玩家标识) {
        return 玩家服务.获取快照(玩家标识)
                .map(属性计算服务::计算)
                .map(属性快照::生命恢复)
                .orElse(0.0);
    }

    private double 获取最大生命值(Player 玩家) {
        AttributeInstance 属性 = 玩家.getAttribute(Attribute.MAX_HEALTH);
        return 属性 != null ? 属性.getValue() : 20.0;
    }

    @Override
    public void 设置调试模式(boolean 开启) {
        this.调试模式 = 开启;
    }

    @Override
    public boolean 是否调试模式() {
        return 调试模式;
    }

    @Override
    public List<UUID> 获取战斗中玩家列表() {
        return Collections.unmodifiableList(new ArrayList<>(战斗中玩家));
    }

    private void 发送战斗日志共享事件(UUID 玩家标识, UUID 事件标识, String 键) {
        UUID 实际标识 = 事件标识 != null ? 事件标识 : UUID.randomUUID();
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        快照可选.ifPresent(快照 -> 消息服务.发送战斗日志(快照, 实际标识, 0, 键));
    }

    private void 发送战斗日志独立(UUID 玩家标识, String 键) {
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        快照可选.ifPresent(快照 -> 消息服务.发送战斗日志(快照, UUID.randomUUID(), 0, 键));
    }

    private void 发送战斗日志独立(UUID 玩家标识, String 键, Object... 参数) {
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        快照可选.ifPresent(快照 -> 消息服务.发送战斗日志(快照, UUID.randomUUID(), 0, 键, 参数));
    }
}
