package mljy.基础设施层;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录玩家可见日志的因果事件上下文。
 *
 * 事件标识决定日志能否合并；tick 只负责 Bukkit 的异步回调，不参与合并判定。
 * 根事件同步处理完成立即执行完成回调，不再等待异步子事件租约（该机制已废弃，保留空操作签名以兼容调用方）。
 * 未显式绑定的事件对象永远创建独立新根事件，ThreadLocal 当前事件不构成因果证明。
 */
public final class 事件日志上下文 {
    private static final ThreadLocal<UUID> 当前事件标识 = new ThreadLocal<>();
    private static final Map<UUID, 事件状态> 事件状态表 = new ConcurrentHashMap<>();
    private static final Map<UUID, 玩家目标因果凭证> 玩家目标凭证表 = new ConcurrentHashMap<>();
    private static final Map<Object, 事件对象绑定> 事件对象表 = new WeakHashMap<>();
    private static final Object 锁 = new Object();
    private static final long 玩家目标凭证泄漏清理毫秒 = 5000L;

    private 事件日志上下文() {
    }

    public static UUID 开始新事件() {
        UUID 事件标识 = UUID.randomUUID();
        事件状态表.put(事件标识, new 事件状态());
        当前事件标识.set(事件标识);
        调试日志器.调试("事件日志上下文", "开始新事件：标识=%s 来源=显式调用", 事件标识);
        return 事件标识;
    }

    public static UUID 获取当前事件标识() {
        UUID 事件标识 = 当前事件标识.get();
        return 事件标识 != null ? 事件标识 : 开始新事件();
    }

    public static UUID 获取当前事件标识或空() {
        return 当前事件标识.get();
    }

    public static UUID 获取当前事件标识或随机() {
        UUID 事件标识 = 当前事件标识.get();
        return 事件标识 != null ? 事件标识 : UUID.randomUUID();
    }

    /**
     * 为同一个 Bukkit 事件对象取得稳定的根事件标识。
     * 不同监听器收到同一事件对象时，必须共享此标识。
     * 显式传播凭据机制已废弃：未绑定的事件对象永远创建独立新根事件。
     */
    public static UUID 获取或创建事件标识(Object 事件对象) {
        if (事件对象 == null) {
            return 获取当前事件标识();
        }
        UUID 事件标识;
        synchronized (锁) {
            事件对象绑定 已有绑定 = 事件对象表.get(事件对象);
            if (已有绑定 != null) {
                事件标识 = 已有绑定.事件标识();
                调试日志器.调试("事件日志上下文", "复用事件对象事件：对象=%s 标识=%s 根对象=%s 来源=已绑定复用",
                        事件对象.getClass().getSimpleName(), 事件标识, 已有绑定.根事件对象());
            } else {
                事件标识 = UUID.randomUUID();
                事件对象表.put(事件对象, new 事件对象绑定(事件标识, true));
                事件状态表.put(事件标识, new 事件状态());
                调试日志器.调试("事件日志上下文",
                        "为未绑定事件对象创建独立根事件：对象=%s 标识=%s 当前线程事件=%s 来源=新根事件",
                        事件对象.getClass().getSimpleName(), 事件标识, 当前事件标识.get());
            }
        }
        当前事件标识.set(事件标识);
        return 事件标识;
    }

    /**
     * 为明确的玩家输入保留根事件，等待该玩家的实体命中事件显式消费。
     * 输入事件本身没有目标实体，因此目标只允许在命中事件中绑定，绝不按时间推断命中关系。
     */
    public static boolean 创建玩家目标因果凭证(UUID 玩家标识, UUID 根事件标识, Object 输入事件) {
        if (玩家标识 == null || 根事件标识 == null || 输入事件 == null) {
            return false;
        }
        清理过期玩家目标因果凭证();
        玩家目标因果凭证 旧凭证;
        synchronized (锁) {
            事件状态 状态 = 事件状态表.get(根事件标识);
            if (状态 == null || 状态.已完成) {
                return false;
            }
            旧凭证 = 玩家目标凭证表.get(玩家标识);
            if (旧凭证 != null) {
                调试日志器.调试("事件日志上下文",
                        "拒绝重复玩家输入凭证：玩家=%s 新根事件=%s 旧根事件=%s 原因=避免合并另一输入",
                        玩家标识, 根事件标识, 旧凭证.根事件标识);
                玩家目标凭证表.remove(玩家标识, 旧凭证);
            } else {
                玩家目标凭证表.put(玩家标识,
                        new 玩家目标因果凭证(玩家标识, 根事件标识, 输入事件, System.currentTimeMillis()));
            }
        }
        if (旧凭证 != null) {
            结束事件(旧凭证.根事件标识);
            return false;
        }
        调试日志器.调试("事件日志上下文",
                "创建玩家目标因果凭证：玩家=%s 根事件=%s 输入对象=%s 来源=明确输入",
                玩家标识, 根事件标识, 输入事件.getClass().getSimpleName());
        return true;
    }

    /**
     * 将明确输入凭证一次性绑定到玩家和实际命中目标，并把根事件绑定到伤害事件对象。
     */
    public static UUID 消费玩家目标因果凭证(UUID 玩家标识, Object 目标对象, Object 伤害事件对象) {
        if (玩家标识 == null || 目标对象 == null || 伤害事件对象 == null) {
            return null;
        }
        清理过期玩家目标因果凭证();
        玩家目标因果凭证 凭证;
        synchronized (锁) {
            凭证 = 玩家目标凭证表.get(玩家标识);
            if (凭证 == null) {
                return null;
            }
            事件对象绑定 已有绑定 = 事件对象表.get(伤害事件对象);
            if (已有绑定 != null && !凭证.根事件标识.equals(已有绑定.事件标识())) {
                调试日志器.调试("事件日志上下文",
                        "拒绝消费玩家目标因果凭证：玩家=%s 目标=%s 伤害对象已有其他根事件=%s 目标根事件=%s",
                        玩家标识, 目标对象.getClass().getSimpleName(), 已有绑定.事件标识(), 凭证.根事件标识);
                return null;
            }
            玩家目标凭证表.remove(玩家标识, 凭证);
            事件对象表.put(伤害事件对象, new 事件对象绑定(凭证.根事件标识, true));
        }
        调试日志器.调试("事件日志上下文",
                "消费玩家目标因果凭证：玩家=%s 目标=%s 根事件=%s 伤害对象=%s 来源=玩家+目标一次性命中",
                玩家标识, 目标对象.getClass().getSimpleName(), 凭证.根事件标识,
                伤害事件对象.getClass().getSimpleName());
        return 凭证.根事件标识;
    }

    public static void 清理过期玩家目标因果凭证() {
        清理过期玩家目标因果凭证(System.currentTimeMillis());
    }

    static void 清理过期玩家目标因果凭证(long 当前时间毫秒) {
        List<UUID> 待结束根事件 = new ArrayList<>();
        synchronized (锁) {
            玩家目标凭证表.entrySet().removeIf(条目 -> {
                玩家目标因果凭证 凭证 = 条目.getValue();
                if (当前时间毫秒 - 凭证.创建时间毫秒 < 玩家目标凭证泄漏清理毫秒) {
                    return false;
                }
                待结束根事件.add(凭证.根事件标识);
                调试日志器.调试("事件日志上下文",
                        "清理过期玩家目标因果凭证：玩家=%s 根事件=%s 原因=仅清除泄漏",
                        凭证.玩家标识, 凭证.根事件标识);
                return true;
            });
        }
        待结束根事件.forEach(事件日志上下文::结束事件);
    }

    public static void 清理全部玩家目标因果凭证() {
        List<UUID> 待结束根事件;
        synchronized (锁) {
            待结束根事件 = 玩家目标凭证表.values().stream()
                    .map(凭证 -> 凭证.根事件标识)
                    .toList();
            玩家目标凭证表.clear();
        }
        待结束根事件.forEach(事件日志上下文::结束事件);
    }

    /**
     * 将同一个因果根事件从一个 Bukkit 事件对象转移到另一个事件对象。
     *
     * 典型场景是玩家左键空气后实际命中实体：PlayerInteractEvent 是输入事件，
     * EntityDamageByEntityEvent 是该输入产生的伤害事件，二者必须继续使用同一个
     * 根事件标识，才能把战斗日志和技能日志归入同一根事件并分别输出。
     */
    public static boolean 转移事件对象根事件(Object 原事件对象, Object 新事件对象, UUID 事件标识) {
        if (原事件对象 == null || 新事件对象 == null || 事件标识 == null
                || 原事件对象 == 新事件对象) {
            return false;
        }
        synchronized (锁) {
            事件对象绑定 原绑定 = 事件对象表.get(原事件对象);
            UUID 原标识 = 原绑定 == null ? null : 原绑定.事件标识();
            if (!事件标识.equals(原标识) || !事件状态表.containsKey(事件标识)) {
                调试日志器.调试("事件日志上下文",
                        "转移事件根事件失败：原对象未绑定指定标识 原对象=%s 标识=%s",
                        原事件对象.getClass().getSimpleName(), 事件标识);
                return false;
            }
            事件对象绑定 已有新对象绑定 = 事件对象表.get(新事件对象);
            UUID 已有新对象标识 = 已有新对象绑定 == null ? null : 已有新对象绑定.事件标识();
            if (已有新对象标识 != null && !事件标识.equals(已有新对象标识)) {
                调试日志器.调试("事件日志上下文",
                        "转移事件根事件失败：新对象已有其他标识 新对象=%s 已有标识=%s 目标标识=%s",
                        新事件对象.getClass().getSimpleName(), 已有新对象标识, 事件标识);
                return false;
            }
            事件对象表.remove(原事件对象);
            事件对象表.put(新事件对象, new 事件对象绑定(事件标识, true));
            调试日志器.调试("事件日志上下文",
                    "转移事件根事件成功：原对象=%s 新对象=%s 标识=%s",
                    原事件对象.getClass().getSimpleName(),
                    新事件对象.getClass().getSimpleName(), 事件标识);
            return true;
        }
    }

    public static void 结束事件对象(Object 事件对象) {
        if (事件对象 == null) {
            return;
        }
        事件对象绑定 绑定;
        boolean 保留待命根事件 = false;
        synchronized (锁) {
            绑定 = 事件对象表.remove(事件对象);
            if (绑定 != null && 绑定.根事件对象()) {
                保留待命根事件 = 玩家目标凭证表.values().stream()
                        .anyMatch(凭证 -> 凭证.输入事件 == 事件对象
                                && 凭证.根事件标识.equals(绑定.事件标识()));
            }
        }
        if (绑定 != null && 绑定.根事件对象() && !保留待命根事件) {
            结束事件(绑定.事件标识());
        } else if (绑定 != null && 保留待命根事件) {
            调试日志器.调试("事件日志上下文", "保留待命根事件：对象=%s 标识=%s 原因=等待玩家目标命中",
                    事件对象.getClass().getSimpleName(), 绑定.事件标识());
        } else if (绑定 != null) {
            调试日志器.调试("事件日志上下文", "结束同步子事件对象：不结束根事件 对象=%s 标识=%s",
                    事件对象.getClass().getSimpleName(), 绑定.事件标识());
        }
    }

    /**
     * 标记根事件结束。根事件同步处理完成立即执行完成回调，不再等待异步子事件租约。
     */
    public static void 结束事件(UUID 事件标识) {
        if (事件标识 == null) {
            return;
        }
        List<Runnable> 待执行回调 = List.of();
        synchronized (锁) {
            事件状态 状态 = 事件状态表.get(事件标识);
            if (状态 == null) {
                调试日志器.调试("事件日志上下文", "结束事件跳过：状态不存在 标识=%s", 事件标识);
                return;
            }
            状态.根事件已结束 = true;
            调试日志器.调试("事件日志上下文", "根事件结束：标识=%s 原因=同步完成 回调数=%d",
                    事件标识, 状态.完成回调列表.size());
            待执行回调 = 完成并移除(事件标识, 状态);
        }
        执行完成回调(事件标识, 待执行回调);
    }

    /**
     * 异步子事件租约机制已废弃。保留方法签名以兼容调用方，内部为空操作，不再延迟根事件完成。
     */
    public static boolean 持有异步子事件(UUID 事件标识) {
        调试日志器.调试("事件日志上下文", "持有异步子事件（已废弃空操作）：标识=%s", 事件标识);
        return 事件标识 != null;
    }

    /**
     * 异步子事件租约机制已废弃。保留方法签名以兼容调用方，返回空操作租约，不再延迟根事件完成。
     */
    public static 异步事件租约 获取异步事件租约(UUID 事件标识) {
        调试日志器.调试("事件日志上下文", "获取异步事件租约（已废弃空操作）：标识=%s", 事件标识);
        return new 异步事件租约(事件标识);
    }

    /**
     * 异步子事件租约机制已废弃。保留方法签名以兼容调用方，内部为空操作，不再触发完成回调。
     */
    public static void 释放异步子事件(UUID 事件标识) {
        调试日志器.调试("事件日志上下文", "释放异步子事件（已废弃空操作）：标识=%s", 事件标识);
    }

    /**
     * 日志合并管理器使用此方法把“发送”绑定到事件完成，而不是绑定到某个 tick。
     */
    public static boolean 注册完成回调(UUID 事件标识, Runnable 回调) {
        if (事件标识 == null || 回调 == null) {
            return false;
        }
        boolean 立即执行 = false;
        synchronized (锁) {
            事件状态 状态 = 事件状态表.get(事件标识);
            if (状态 == null) {
                return false;
            }
            if (状态.已完成) {
                立即执行 = true;
            } else {
                状态.完成回调列表.add(回调);
                调试日志器.调试("事件日志上下文", "注册事件完成回调：标识=%s 回调数=%d 根事件已结束=%s",
                        事件标识, 状态.完成回调列表.size(), 状态.根事件已结束);
            }
        }
        if (立即执行) {
            执行完成回调(事件标识, List.of(回调));
        }
        return true;
    }

    public static boolean 是否已托管(UUID 事件标识) {
        return 事件标识 != null && 事件状态表.containsKey(事件标识);
    }

    public static void 在事件中执行(UUID 事件标识, Runnable 任务) {
        if (事件标识 == null || 任务 == null) {
            return;
        }
        UUID 原事件标识 = 当前事件标识.get();
        当前事件标识.set(事件标识);
        调试日志器.调试("事件日志上下文", "进入事件：标识=%s 原事件=%s", 事件标识, 原事件标识);
        try {
            任务.run();
        } catch (RuntimeException | Error 异常) {
            调试日志器.调试("事件日志上下文", "事件执行异常：标识=%s", 事件标识, 异常);
            throw 异常;
        } finally {
            if (原事件标识 == null) {
                当前事件标识.remove();
            } else {
                当前事件标识.set(原事件标识);
            }
            调试日志器.调试("事件日志上下文", "退出事件：标识=%s 恢复事件=%s", 事件标识, 原事件标识);
        }
    }

    public static void 清除() {
        当前事件标识.remove();
    }

    /**
     * 异步事件租约。已废弃，保留类签名以兼容 try-with-resources 调用方。
     * 释放() 和 close() 均为空操作，不再延迟根事件完成。
     */
    public static final class 异步事件租约 implements AutoCloseable {
        private final UUID 事件标识;
        private boolean 已释放;

        private 异步事件租约(UUID 事件标识) {
            this.事件标识 = 事件标识;
        }

        public void 释放() {
            if (已释放) {
                return;
            }
            已释放 = true;
            调试日志器.调试("事件日志上下文", "异步事件租约释放（已废弃空操作）：标识=%s", 事件标识);
        }

        @Override
        public void close() {
            释放();
        }
    }

    private static List<Runnable> 完成并移除(UUID 事件标识, 事件状态 状态) {
        状态.已完成 = true;
        事件状态表.remove(事件标识, 状态);
        return List.copyOf(状态.完成回调列表);
    }

    private static void 执行完成回调(UUID 事件标识, List<Runnable> 回调列表) {
        int 回调数 = 回调列表.size();
        if (回调数 == 0) {
            return;
        }
        int 成功数 = 0;
        int 失败数 = 0;
        for (Runnable 回调 : 回调列表) {
            try {
                回调.run();
                成功数++;
            } catch (RuntimeException | Error 异常) {
                失败数++;
                调试日志器.调试("事件日志上下文", "事件完成回调异常：标识=%s", 事件标识, 异常);
            }
        }
        调试日志器.调试("事件日志上下文", "执行完成回调：标识=%s 回调数=%d 成功=%d 失败=%d",
                事件标识, 回调数, 成功数, 失败数);
    }

    private static final class 事件状态 {
        private boolean 根事件已结束;
        private boolean 已完成;
        private final List<Runnable> 完成回调列表 = new ArrayList<>();
    }

    private static final class 玩家目标因果凭证 {
        private final UUID 玩家标识;
        private final UUID 根事件标识;
        private final Object 输入事件;
        private final long 创建时间毫秒;

        private 玩家目标因果凭证(UUID 玩家标识, UUID 根事件标识,
                                  Object 输入事件, long 创建时间毫秒) {
            this.玩家标识 = 玩家标识;
            this.根事件标识 = 根事件标识;
            this.输入事件 = 输入事件;
            this.创建时间毫秒 = 创建时间毫秒;
        }
    }

    /**
     * 在显式子事件中执行任务。已废弃显式传播凭据机制：直接在根事件上下文执行任务，
     * 任务内调用 获取或创建事件标识(子事件对象) 会创建独立新根事件，不再回绑到原根事件。
     */
    public static void 在显式子事件中执行(UUID 根事件标识, UUID 施法标识,
                                      UUID 弹道标识, Runnable 任务) {
        if (根事件标识 == null || 任务 == null) {
            return;
        }
        调试日志器.调试("事件日志上下文",
                "进入显式子事件执行（独立根事件）：根事件=%s 施法=%s 弹道=%s", 根事件标识, 施法标识, 弹道标识);
        try {
            在事件中执行(根事件标识, 任务);
        } finally {
            调试日志器.调试("事件日志上下文",
                    "退出显式子事件执行：根事件=%s 施法=%s 弹道=%s", 根事件标识, 施法标识, 弹道标识);
        }
    }

    /**
     * 绑定子事件对象到根事件。仍允许显式绑定子事件对象到指定根事件标识，
     * 但根事件结束条件不再依赖异步子事件租约计数。
     */
    public static boolean 绑定子事件对象(Object 子事件对象, UUID 根事件标识,
                                     UUID 施法标识, UUID 弹道标识) {
        if (子事件对象 == null || 根事件标识 == null) {
            return false;
        }
        synchronized (锁) {
            事件状态 状态 = 事件状态表.get(根事件标识);
            if (状态 == null || 状态.已完成) {
                调试日志器.调试("事件日志上下文",
                        "显式绑定子事件失败：对象=%s 根事件=%s 施法=%s 弹道=%s 原因=根事件不存在或已完成",
                        子事件对象.getClass().getSimpleName(), 根事件标识, 施法标识, 弹道标识);
                return false;
            }
            事件对象绑定 已有绑定 = 事件对象表.get(子事件对象);
            if (已有绑定 != null && !根事件标识.equals(已有绑定.事件标识())) {
                调试日志器.调试("事件日志上下文",
                        "显式绑定子事件失败：对象=%s 根事件=%s 已有根事件=%s 施法=%s 弹道=%s",
                        子事件对象.getClass().getSimpleName(), 根事件标识, 已有绑定.事件标识(),
                        施法标识, 弹道标识);
                return false;
            }
            事件对象表.put(子事件对象, new 事件对象绑定(根事件标识, false));
            调试日志器.调试("事件日志上下文",
                    "显式绑定子事件成功：对象=%s 根事件=%s 施法=%s 弹道=%s 来源=显式标识",
                    子事件对象.getClass().getSimpleName(), 根事件标识, 施法标识, 弹道标识);
            return true;
        }
    }

    private record 事件对象绑定(UUID 事件标识, boolean 根事件对象) {
    }
}
