package mljy.业务层;

import mljy.领域层.任务.任务分类;
import mljy.领域层.任务.任务定义;
import mljy.领域层.任务.任务进度;
import mljy.领域层.任务.任务状态;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 任务管理服务接口。
 * 负责任务的接取、完成、进度查询、进度更新等运行时操作。
 * 对应需求文档《任务.md》第二章核心流程。
 */
public interface 任务管理服务 {
    /**
     * 接取任务。
     * 状态从"可接取"变为"进行中"。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @return 接取成功返回true，任务不存在/状态不允许/前置未完成返回false
     */
    boolean 接取任务(UUID 玩家标识, String 任务标识);

    /**
     * 交付任务。
     * 状态从"可交付"变为"已完成"，并发放奖励。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @return 交付成功返回true，任务不存在/状态不允许返回false
     */
    boolean 交付任务(UUID 玩家标识, String 任务标识);

    /**
     * 管理员强制完成任务（跳过状态检查，直接发放奖励）。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @return 完成成功返回true
     */
    boolean 强制完成任务(UUID 玩家标识, String 任务标识);

    /**
     * 重置玩家任务进度。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @return 重置成功返回true
     */
    boolean 重置任务(UUID 玩家标识, String 任务标识);

    /**
     * 增加任务进度（用于击杀/收集等计数型目标）。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @param 增量 进度增量
     */
    void 增加进度(UUID 玩家标识, String 任务标识, int 增量);

    /**
     * 设置任务进度（用于条件检查型目标）。
     *
     * @param 玩家标识 玩家UUID
     * @param 任务标识 任务ID
     * @param 新进度 新进度值
     */
    void 设置进度(UUID 玩家标识, String 任务标识, int 新进度);

    /**
     * 获取玩家任务进度。
     */
    Optional<任务进度> 获取进度(UUID 玩家标识, String 任务标识);

    /**
     * 获取玩家所有任务进度。
     */
    List<任务进度> 获取所有进度(UUID 玩家标识);

    /**
     * 获取玩家指定分类的任务进度。
     */
    List<任务进度> 获取分类进度(UUID 玩家标识, 任务分类 分类);

    /**
     * 获取任务当前状态（综合任务定义和玩家进度计算）。
     */
    任务状态 获取任务状态(UUID 玩家标识, String 任务标识);

    /**
     * 保存玩家任务进度到持久化存储。
     */
    void 保存进度(UUID 玩家标识);

    /**
     * 加载玩家任务进度从持久化存储。
     */
    void 加载进度(UUID 玩家标识);

    /**
     * 玩家登录时初始化任务进度（解锁可接取的任务）。
     */
    void 玩家登录(UUID 玩家标识);

    /**
     * 玩家退出时保存任务进度。
     */
    void 玩家退出(UUID 玩家标识);

    /**
     * 检查并解锁可接取的任务（前置任务已完成时触发）。
     */
    void 检查解锁(UUID 玩家标识);

    /**
     * 获取任务定义（委托给任务注册服务）。
     */
    Optional<任务定义> 获取任务定义(String 任务标识);
}
