package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.玩家服务;
import mljy.翻译服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.基础设施层.调试日志器;
import mljy.领域层.组队.队伍;
import mljy.领域层.组队.组队操作结果;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组队服务实现。
 * 管理队伍全生命周期：创建/邀请/接受/拒绝/离开/踢出/解散/转让队长/就位检查。
 * 队伍ID = 队长UUID（队长变更时队伍ID同步变更）。
 * 邀请有效期可配置（默认30秒），离线踢出时间可配置（默认300秒）。
 */
@Singleton
public class 组队服务实现 implements 组队服务 {
    private static final String 配置根键 = "组队";
    private static final String 键邀请过期秒数 = "邀请过期秒数";
    private static final String 键离线踢出秒数 = "离线踢出秒数";
    private static final String 键最大队伍人数 = "最大队伍人数";

    private static final int 默认邀请过期秒数 = 30;
    private static final int 默认离线踢出秒数 = 300;
    private static final int 默认最大队伍人数 = 5;
    private static final long 毫秒每秒 = 1000L;

    private static final String 翻译根键 = "队伍管理器.队伍";
    private static final String 未知专精键 = "通用.未知";
    private static final String 创建成功键 = 翻译根键 + ".创建成功";
    private static final String 已解散键 = 翻译根键 + ".已解散";
    private static final String 邀请已发送键 = 翻译根键 + ".邀请已发送";
    private static final String 收到邀请键 = 翻译根键 + ".收到邀请";
    private static final String 成员加入键 = 翻译根键 + ".成员加入";
    private static final String 队长变更键 = 翻译根键 + ".队长变更";
    private static final String 已离开键 = 翻译根键 + ".已离开";
    private static final String 被踢出键 = 翻译根键 + ".被踢出";
    private static final String 成员被踢出键 = 翻译根键 + ".成员被踢出";
    private static final String 已转让队长键 = 翻译根键 + ".已转让队长";
    private static final String 成为新队长键 = 翻译根键 + ".成为新队长";
    private static final String 就位检查发起键 = 翻译根键 + ".就位检查发起";
    private static final String 已就位键 = 翻译根键 + ".已就位";
    private static final String 全部就位键 = 翻译根键 + ".全部就位";
    private static final String 邀请已过期键 = 翻译根键 + ".邀请已过期";
    private static final String 成员离线超时键 = 翻译根键 + ".成员离线超时";
    private static final String 成员上线键 = 翻译根键 + ".成员上线";
    private static final String 成员下线键 = 翻译根键 + ".成员下线";
    private static final String 失败根键 = 翻译根键 + ".失败";
    private static final String 失败已在队伍中键 = 失败根键 + ".已在队伍中";
    private static final String 失败不在队伍中键 = 失败根键 + ".不在队伍中";
    private static final String 失败仅队长可解散键 = 失败根键 + ".仅队长可解散";
    private static final String 失败队伍已满键 = 失败根键 + ".队伍已满";
    private static final String 失败目标已在队伍键 = 失败根键 + ".目标已在队伍";
    private static final String 失败已邀请过键 = 失败根键 + ".已邀请过";
    private static final String 失败无待处理邀请键 = 失败根键 + ".无待处理邀请";
    private static final String 失败队伍已满简单键 = 失败根键 + ".队伍已满简单";
    private static final String 失败已在其他队伍键 = 失败根键 + ".已在其他队伍";
    private static final String 失败仅队长可踢人键 = 失败根键 + ".仅队长可踢人";
    private static final String 失败目标不在队伍键 = 失败根键 + ".目标不在队伍";
    private static final String 失败不能踢自己键 = 失败根键 + ".不能踢自己";
    private static final String 失败仅队长可转让键 = 失败根键 + ".仅队长可转让";
    private static final String 失败仅队长可就位检查键 = 失败根键 + ".仅队长可就位检查";
    private static final String 失败无就位检查键 = 失败根键 + ".无就位检查";
    private static final String 失败不能邀请自己键 = 失败根键 + ".不能邀请自己";
    private static final String 失败不能转让给自己键 = 失败根键 + ".不能转让给自己";
    private static final String 失败玩家不在线键 = 失败根键 + ".玩家不在线";

    private final 玩家服务 玩家服务;
    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final MiniMessage 迷你消息;

    private final Map<UUID, 队伍> 队伍表 = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> 玩家队伍映射 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> 离线时间表 = new ConcurrentHashMap<>();

    private final int 邀请过期秒数;
    private final int 离线踢出秒数;
    private final int 最大队伍人数;

    @Inject
    public 组队服务实现(玩家服务 玩家服务, 翻译服务 翻译服务, 关键词解析器 关键词解析器, JavaPlugin 插件) {
        this.玩家服务 = 玩家服务;
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.迷你消息 = MiniMessage.miniMessage();
        FileConfiguration 配置 = 插件.getConfig();
        this.邀请过期秒数 = 配置.getInt(配置根键 + "." + 键邀请过期秒数, 默认邀请过期秒数);
        this.离线踢出秒数 = 配置.getInt(配置根键 + "." + 键离线踢出秒数, 默认离线踢出秒数);
        this.最大队伍人数 = 配置.getInt(配置根键 + "." + 键最大队伍人数, 默认最大队伍人数);
    }

    @Override
    public 组队操作结果 创建队伍(UUID 队长标识) {
        if (玩家队伍映射.containsKey(队长标识)) {
            return 组队操作结果.失败(失败已在队伍中键);
        }
        队伍 新队伍 = new 队伍(队长标识);
        队伍表.put(队长标识, 新队伍);
        玩家队伍映射.put(队长标识, 队长标识);
        发送消息(队长标识, 创建成功键);
        调试日志器.调试("组队", "创建队伍：队长=%s 队伍ID=%s", 队长标识, 队长标识);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 邀请玩家(UUID 队长标识, UUID 被邀请者标识) {
        if (队长标识.equals(被邀请者标识)) {
            return 组队操作结果.失败(失败不能邀请自己键);
        }
        UUID 队伍标识 = 玩家队伍映射.get(队长标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.获取队长标识().equals(队长标识)) {
            return 组队操作结果.失败(失败仅队长可踢人键);
        }
        if (玩家队伍映射.containsKey(被邀请者标识)) {
            return 组队操作结果.失败(失败目标已在队伍键);
        }
        if (队伍.包含邀请(被邀请者标识)) {
            return 组队操作结果.失败(失败已邀请过键);
        }
        if (队伍.获取成员数量() >= 最大队伍人数) {
            return 组队操作结果.失败(失败队伍已满键, 最大队伍人数);
        }
        if (!是玩家在线(被邀请者标识)) {
            return 组队操作结果.失败(失败玩家不在线键);
        }
        long 过期时间戳 = System.currentTimeMillis() + 邀请过期秒数 * 毫秒每秒;
        队伍.添加邀请(被邀请者标识, 过期时间戳);
        String 队长名 = 获取玩家名(队长标识);
        发送消息(队长标识, 邀请已发送键, 队长名);
        发送消息(被邀请者标识, 收到邀请键, 队长名);
        调试日志器.调试("组队", "发送邀请：队长=%s 被邀请者=%s", 队长标识, 被邀请者标识);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 接受邀请(UUID 玩家标识) {
        队伍 目标队伍 = 查找待处理邀请队伍(玩家标识);
        if (目标队伍 == null) {
            return 组队操作结果.失败(失败无待处理邀请键);
        }
        if (玩家队伍映射.containsKey(玩家标识)) {
            目标队伍.移除邀请(玩家标识);
            return 组队操作结果.失败(失败已在其他队伍键);
        }
        if (目标队伍.获取成员数量() >= 最大队伍人数) {
            目标队伍.移除邀请(玩家标识);
            return 组队操作结果.失败(失败队伍已满简单键);
        }
        目标队伍.移除邀请(玩家标识);
        目标队伍.添加成员(玩家标识);
        玩家队伍映射.put(玩家标识, 目标队伍.获取队伍标识());
        String 玩家名 = 获取玩家名(玩家标识);
        String 专精 = 获取玩家专精(玩家标识);
        通知队伍成员(目标队伍, 成员加入键, 玩家名, 专精);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 拒绝邀请(UUID 玩家标识) {
        队伍 目标队伍 = 查找待处理邀请队伍(玩家标识);
        if (目标队伍 == null) {
            return 组队操作结果.失败(失败无待处理邀请键);
        }
        目标队伍.移除邀请(玩家标识);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 离开队伍(UUID 玩家标识) {
        UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            玩家队伍映射.remove(玩家标识);
            return 组队操作结果.失败(失败不在队伍中键);
        }
        boolean 是队长 = 队伍.获取队长标识().equals(玩家标识);
        if (是队长) {
            if (队伍.获取成员数量() <= 1) {
                return 解散队伍内部(队伍, 玩家标识);
            }
            UUID 新队长 = 选择新队长(队伍, 玩家标识);
            队伍.移除成员(玩家标识);
            玩家队伍映射.remove(玩家标识);
            队伍.设置队长标识(新队长);
            队伍表.remove(队伍标识);
            队伍表.put(新队长, 队伍);
            for (UUID 成员标识 : 队伍.获取成员标识集合()) {
                玩家队伍映射.put(成员标识, 新队长);
            }
            String 新队长名 = 获取玩家名(新队长);
            通知队伍成员(队伍, 队长变更键, 新队长名);
            发送消息(新队长, 成为新队长键);
            发送消息(玩家标识, 已离开键);
            return 组队操作结果.成功();
        }
        队伍.移除成员(玩家标识);
        玩家队伍映射.remove(玩家标识);
        发送消息(玩家标识, 已离开键);
        调试日志器.调试("组队", "成员离开：玩家=%s 队伍ID=%s", 玩家标识, 队伍标识);
        String 玩家名 = 获取玩家名(玩家标识);
        通知队伍成员(队伍, 成员被踢出键, 玩家名);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 踢出成员(UUID 队长标识, UUID 成员标识) {
        if (队长标识.equals(成员标识)) {
            return 组队操作结果.失败(失败不能踢自己键);
        }
        UUID 队伍标识 = 玩家队伍映射.get(队长标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.获取队长标识().equals(队长标识)) {
            return 组队操作结果.失败(失败仅队长可踢人键);
        }
        if (!队伍.包含成员(成员标识)) {
            return 组队操作结果.失败(失败目标不在队伍键);
        }
        队伍.移除成员(成员标识);
        玩家队伍映射.remove(成员标识);
        发送消息(成员标识, 被踢出键);
        调试日志器.调试("组队", "踢出成员：队长=%s 被踢出成员=%s", 队长标识, 成员标识);
        String 成员名 = 获取玩家名(成员标识);
        通知队伍成员(队伍, 成员被踢出键, 成员名);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 解散队伍(UUID 队长标识) {
        UUID 队伍标识 = 玩家队伍映射.get(队长标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            玩家队伍映射.remove(队长标识);
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.获取队长标识().equals(队长标识)) {
            return 组队操作结果.失败(失败仅队长可解散键);
        }
        调试日志器.调试("组队", "解散队伍：队长=%s 队伍ID=%s", 队长标识, 队伍标识);
        return 解散队伍内部(队伍, 队长标识);
    }

    @Override
    public 组队操作结果 转让队长(UUID 队长标识, UUID 新队长标识) {
        if (队长标识.equals(新队长标识)) {
            return 组队操作结果.失败(失败不能转让给自己键);
        }
        UUID 队伍标识 = 玩家队伍映射.get(队长标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.获取队长标识().equals(队长标识)) {
            return 组队操作结果.失败(失败仅队长可转让键);
        }
        if (!队伍.包含成员(新队长标识)) {
            return 组队操作结果.失败(失败目标不在队伍键);
        }
        队伍.设置队长标识(新队长标识);
        队伍表.remove(队伍标识);
        队伍表.put(新队长标识, 队伍);
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            玩家队伍映射.put(成员标识, 新队长标识);
        }
        String 新队长名 = 获取玩家名(新队长标识);
        发送消息(队长标识, 已转让队长键, 新队长名);
        调试日志器.调试("组队", "转让队长：旧队长=%s 新队长=%s 队伍ID变更=%s->%s", 队长标识, 新队长标识, 队伍标识, 新队长标识);
        通知队伍成员(队伍, 队长变更键, 新队长名);
        发送消息(新队长标识, 成为新队长键);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 发起就位检查(UUID 队长标识) {
        UUID 队伍标识 = 玩家队伍映射.get(队长标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.获取队长标识().equals(队长标识)) {
            return 组队操作结果.失败(失败仅队长可就位检查键);
        }
        队伍.设置就位检查进行中(true);
        String 队长名 = 获取玩家名(队长标识);
        通知队伍成员(队伍, 就位检查发起键, 队长名);
        return 组队操作结果.成功();
    }

    @Override
    public 组队操作结果 确认就位(UUID 玩家标识) {
        UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
        if (队伍标识 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return 组队操作结果.失败(失败不在队伍中键);
        }
        if (!队伍.是否就位检查进行中()) {
            return 组队操作结果.失败(失败无就位检查键);
        }
        队伍.设置准备状态(玩家标识, true);
        String 玩家名 = 获取玩家名(玩家标识);
        通知队伍成员(队伍, 已就位键, 玩家名);
        if (队伍.是否全员已准备()) {
            队伍.清除准备状态();
            通知队伍成员(队伍, 全部就位键);
        }
        return 组队操作结果.成功();
    }

    @Override
    public Optional<队伍> 获取玩家队伍(UUID 玩家标识) {
        UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
        if (队伍标识 == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(队伍表.get(队伍标识));
    }

    @Override
    public Optional<队伍> 获取队伍(UUID 队伍标识) {
        return Optional.ofNullable(队伍表.get(队伍标识));
    }

    @Override
    public void 玩家上线(UUID 玩家标识) {
        离线时间表.remove(玩家标识);
        UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
        if (队伍标识 == null) {
            return;
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return;
        }
        String 玩家名 = 获取玩家名(玩家标识);
        通知队伍成员(队伍, 成员上线键, 玩家名);
    }

    @Override
    public void 玩家下线(UUID 玩家标识) {
        离线时间表.put(玩家标识, System.currentTimeMillis());
        UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
        if (队伍标识 == null) {
            return;
        }
        队伍 队伍 = 队伍表.get(队伍标识);
        if (队伍 == null) {
            return;
        }
        String 玩家名 = 获取玩家名(玩家标识);
        通知队伍成员(队伍, 成员下线键, 玩家名);
    }

    @Override
    public void 清理过期邀请() {
        long 当前时间 = System.currentTimeMillis();
        for (队伍 队伍 : 队伍表.values()) {
            List<UUID> 过期邀请 = new ArrayList<>();
            for (Map.Entry<UUID, Long> 邀请条目 : 队伍.获取邀请过期时间表().entrySet()) {
                if (邀请条目.getValue() <= 当前时间) {
                    过期邀请.add(邀请条目.getKey());
                }
            }
            for (UUID 被邀请者 : 过期邀请) {
                队伍.移除邀请(被邀请者);
                发送消息(被邀请者, 邀请已过期键);
            }
        }
    }

    @Override
    public void 清理离线超时成员() {
        long 当前时间 = System.currentTimeMillis();
        long 超时毫秒 = 离线踢出秒数 * 毫秒每秒;
        List<UUID> 待清理玩家 = new ArrayList<>();
        for (Map.Entry<UUID, Long> 条目 : 离线时间表.entrySet()) {
            if (当前时间 - 条目.getValue() >= 超时毫秒) {
                待清理玩家.add(条目.getKey());
            }
        }
        for (UUID 玩家标识 : 待清理玩家) {
            离线时间表.remove(玩家标识);
            UUID 队伍标识 = 玩家队伍映射.get(玩家标识);
            if (队伍标识 == null) {
                continue;
            }
            队伍 队伍 = 队伍表.get(队伍标识);
            if (队伍 == null) {
                玩家队伍映射.remove(玩家标识);
                continue;
            }
            if (队伍.获取队长标识().equals(玩家标识)) {
                if (队伍.获取成员数量() <= 1) {
                    解散队伍内部(队伍, 玩家标识);
                } else {
                    UUID 新队长 = 选择新队长(队伍, 玩家标识);
                    队伍.移除成员(玩家标识);
                    玩家队伍映射.remove(玩家标识);
                    队伍.设置队长标识(新队长);
                    队伍表.remove(队伍标识);
                    队伍表.put(新队长, 队伍);
                    for (UUID 成员标识 : 队伍.获取成员标识集合()) {
                        玩家队伍映射.put(成员标识, 新队长);
                    }
                    String 新队长名 = 获取玩家名(新队长);
                    通知队伍成员(队伍, 队长变更键, 新队长名);
                    发送消息(新队长, 成为新队长键);
                    通知队伍成员(队伍, 成员离线超时键);
                }
            } else {
                队伍.移除成员(玩家标识);
                玩家队伍映射.remove(玩家标识);
                通知队伍成员(队伍, 成员离线超时键);
            }
        }
    }

    @Override
    public int 获取最大队伍人数() {
        return 最大队伍人数;
    }

    private 组队操作结果 解散队伍内部(队伍 队伍, UUID 队长标识) {
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            玩家队伍映射.remove(成员标识);
            if (!成员标识.equals(队长标识)) {
                发送消息(成员标识, 已解散键);
            }
        }
        队伍表.remove(队伍.获取队伍标识());
        发送消息(队长标识, 已解散键);
        return 组队操作结果.成功();
    }

    private 队伍 查找待处理邀请队伍(UUID 玩家标识) {
        long 当前时间 = System.currentTimeMillis();
        for (队伍 队伍 : 队伍表.values()) {
            if (!队伍.包含邀请(玩家标识)) {
                continue;
            }
            Long 过期时间 = 队伍.获取邀请过期时间表().get(玩家标识);
            if (过期时间 == null || 过期时间 <= 当前时间) {
                队伍.移除邀请(玩家标识);
                发送消息(玩家标识, 邀请已过期键);
                continue;
            }
            return 队伍;
        }
        return null;
    }

    private UUID 选择新队长(队伍 队伍, UUID 离开队长) {
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            if (!成员标识.equals(离开队长)) {
                return 成员标识;
            }
        }
        return 离开队长;
    }

    private void 通知队伍成员(队伍 队伍, String 消息键, Object... 参数) {
        for (UUID 成员标识 : 队伍.获取成员标识集合()) {
            发送消息(成员标识, 消息键, 参数);
        }
    }

    private void 发送消息(UUID 玩家标识, String 消息键, Object... 参数) {
        if (玩家标识 == null) {
            return;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        String 文本 = 翻译服务.获取(消息键);
        String 解析后 = 关键词解析器.解析(文本, 玩家标识, 参数);
        玩家.sendMessage(迷你消息.deserialize(解析后));
    }

    private boolean 是玩家在线(UUID 玩家标识) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        return 玩家 != null && 玩家.isOnline();
    }

    private String 获取玩家名(UUID 玩家标识) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            return 玩家.getName();
        }
        Optional<mljy.领域层.玩家.玩家会话> 会话 = 玩家服务.获取会话(玩家标识);
        return 会话.map(mljy.领域层.玩家.玩家会话::获取名称).orElse(玩家标识.toString());
    }

    private String 获取玩家专精(UUID 玩家标识) {
        Optional<mljy.领域层.玩家.玩家会话> 会话 = 玩家服务.获取会话(玩家标识);
        return 会话.map(mljy.领域层.玩家.玩家会话::获取专精).orElse(翻译服务.获取(未知专精键));
    }
}
