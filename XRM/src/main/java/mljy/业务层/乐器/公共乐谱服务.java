package mljy.业务层.乐器;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.基础设施层.乐器.公共乐谱库仓储;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-18 公共乐谱服务。
 * 提供上传/下载/评分/审核/列出公共乐谱库的核心业务逻辑。
 * <p>
 * 设计：
 * - 乐谱ID：UUID 前 N 位十六进制（默认 8），碰撞时重新生成（默认最多 5 次）
 * - 评分防重复：内存中维护已评分玩家集合（乐谱ID -> 玩家UUID集合）
 *   注意：评分历史仅在会话内防重复，服务重启后丢失（按需求"最低满足"原则）
 * - 仅"已通过"审核的乐谱可被下载/评分/列出
 * - 评分上下限与 ID 长度可通过 {@code 乐器配置.yml} 的 {@code 公共乐谱库} 段配置
 */
@Singleton
public class 公共乐谱服务 {

    private static final String 公共乐谱库段键 = "公共乐谱库";
    private static final String 评分下限键 = "评分下限";
    private static final String 评分上限键 = "评分上限";
    private static final String 乐谱ID长度键 = "乐谱ID长度";
    private static final String ID生成最大尝试键 = "ID生成最大尝试";

    private static final int 默认ID长度 = 8;
    private static final int 默认ID生成最大尝试 = 5;
    private static final int 默认评分下限 = 1;
    private static final int 默认评分上限 = 5;
    private static final int ID长度下限 = 4;
    private static final int ID长度上限 = 32;
    private static final int 尝试次数下限 = 1;

    public enum 上传结果 { 成功, 重名, 参数无效 }
    public enum 下载结果 { 成功, 不存在, 未通过审核, 参数无效 }
    public enum 评分结果 { 成功, 不存在, 未通过审核, 已评分, 评分越界, 参数无效 }
    public enum 审核结果 { 成功, 不存在, 状态无效, 参数无效 }

    private final 公共乐谱库仓储 仓储;
    private final ConcurrentHashMap<String, Set<UUID>> 已评分玩家表 = new ConcurrentHashMap<>();

    private volatile int ID长度 = 默认ID长度;
    private volatile int ID生成最大尝试 = 默认ID生成最大尝试;
    private volatile int 评分下限 = 默认评分下限;
    private volatile int 评分上限 = 默认评分上限;

    @Inject
    public 公共乐谱服务(公共乐谱库仓储 仓储) {
        this.仓储 = 仓储;
    }

    /**
     * 从 {@code 乐器配置.yml} 的 {@code 公共乐谱库} 段加载参数。
     * 缺失或非法值回退到默认。
     *
     * @param 配置 已加载的乐器配置
     */
    public void 从配置加载(FileConfiguration 配置) {
        if (配置 == null) {
            return;
        }
        ConfigurationSection 段 = 配置.getConfigurationSection(公共乐谱库段键);
        if (段 == null) {
            return;
        }
        int 配置ID长度 = 段.getInt(乐谱ID长度键, 默认ID长度);
        if (配置ID长度 >= ID长度下限 && 配置ID长度 <= ID长度上限) {
            this.ID长度 = 配置ID长度;
        }
        int 配置尝试 = 段.getInt(ID生成最大尝试键, 默认ID生成最大尝试);
        if (配置尝试 >= 尝试次数下限) {
            this.ID生成最大尝试 = 配置尝试;
        }
        int 配置评分下限 = 段.getInt(评分下限键, 默认评分下限);
        int 配置评分上限 = 段.getInt(评分上限键, 默认评分上限);
        if (配置评分下限 <= 配置评分上限) {
            this.评分下限 = 配置评分下限;
            this.评分上限 = 配置评分上限;
        }
    }

    /**
     * 上传乐谱到公共库。生成唯一乐谱ID，存为"待审核"状态。
     *
     * @param 原乐谱     原乐谱
     * @param 上传者标识 上传者UUID
     * @param 上传者名   上传者名
     * @param 公开名     公开乐谱名（可为 null 则使用原乐谱名）
     * @return 上传结果，乐谱ID 通过 返回值对象 获取
     */
    public 上传结果 上传(乐谱 原乐谱, UUID 上传者标识, String 上传者名, String 公开名,
                          返回乐谱ID 回调) {
        if (原乐谱 == null || 上传者标识 == null) {
            return 上传结果.参数无效;
        }
        String 名称 = (公开名 == null || 公开名.isBlank()) ? 原乐谱.获取名称() : 公开名.trim();
        if (名称.isBlank()) {
            return 上传结果.参数无效;
        }
        // 生成唯一乐谱ID
        String 乐谱ID = 生成唯一ID();
        if (乐谱ID == null) {
            return 上传结果.重名;
        }
        // 使用公开名作为原乐谱的展示名（重新构造乐谱对象保留原音符列表与BPM）
        乐谱 展示乐谱 = new 乐谱(名称, 原乐谱.获取作者标识(), 原乐谱.获取作者名(),
                原乐谱.获取创建时间戳(), 原乐谱.获取速度BPM(), 原乐谱.获取音符列表());
        公共乐谱 公共 = 公共乐谱.创建(乐谱ID, 展示乐谱, 上传者标识, 上传者名);
        boolean 已保存 = 仓储.保存(公共);
        if (!已保存) {
            return 上传结果.参数无效;
        }
        if (回调 != null) {
            回调.接受(乐谱ID);
        }
        return 上传结果.成功;
    }

    private String 生成唯一ID() {
        for (int i = 0; i < ID生成最大尝试; i++) {
            String 候选 = UUID.randomUUID().toString().replace("-", "").substring(0, ID长度);
            if (仓储.加载(候选).isEmpty()) {
                return 候选;
            }
        }
        return null;
    }

    /**
     * 下载公共乐谱到玩家私有库。仅"已通过"审核的乐谱可下载。
     *
     * @param 乐谱ID     乐谱ID
     * @param 下载者标识 下载者UUID
     * @return 下载结果，乐谱对象 通过 返回值对象 获取
     */
    public 下载结果 下载(String 乐谱ID, UUID 下载者标识, 返回乐谱 回调) {
        if (乐谱ID == null || 乐谱ID.isBlank() || 下载者标识 == null) {
            return 下载结果.参数无效;
        }
        Optional<公共乐谱> 加载结果 = 仓储.加载(乐谱ID);
        if (加载结果.isEmpty()) {
            return 下载结果.不存在;
        }
        公共乐谱 公共 = 加载结果.get();
        if (公共.获取状态() != 审核状态.已通过) {
            return 下载结果.未通过审核;
        }
        公共.增加下载();
        仓储.保存(公共);
        if (回调 != null) {
            回调.接受(公共.获取原乐谱());
        }
        return 下载结果.成功;
    }

    /**
     * 评分公共乐谱。同一玩家对同一乐谱仅可评一次。
     *
     * @param 乐谱ID     乐谱ID
     * @param 评分者标识 评分者UUID
     * @param 评分       评分（1-5）
     * @return 评分结果
     */
    public 评分结果 评分(String 乐谱ID, UUID 评分者标识, int 评分) {
        if (乐谱ID == null || 乐谱ID.isBlank() || 评分者标识 == null) {
            return 评分结果.参数无效;
        }
        if (评分 < 评分下限 || 评分 > 评分上限) {
            return 评分结果.评分越界;
        }
        Optional<公共乐谱> 加载结果 = 仓储.加载(乐谱ID);
        if (加载结果.isEmpty()) {
            return 评分结果.不存在;
        }
        公共乐谱 公共 = 加载结果.get();
        if (公共.获取状态() != 审核状态.已通过) {
            return 评分结果.未通过审核;
        }
        Set<UUID> 已评分 = 已评分玩家表.computeIfAbsent(乐谱ID, k -> ConcurrentHashMap.newKeySet());
        if (!已评分.add(评分者标识)) {
            return 评分结果.已评分;
        }
        公共.增加评分(评分);
        仓储.保存(公共);
        return 评分结果.成功;
    }

    /**
     * 审核公共乐谱（仅允许 待审核 -> 已通过 / 已拒绝）。
     *
     * @param 乐谱ID  乐谱ID
     * @param 新状态 目标状态（已通过 / 已拒绝）
     * @return 审核结果
     */
    public 审核结果 审核(String 乐谱ID, 审核状态 新状态) {
        if (乐谱ID == null || 乐谱ID.isBlank() || 新状态 == null) {
            return 审核结果.参数无效;
        }
        if (新状态 != 审核状态.已通过 && 新状态 != 审核状态.已拒绝) {
            return 审核结果.状态无效;
        }
        Optional<公共乐谱> 加载结果 = 仓储.加载(乐谱ID);
        if (加载结果.isEmpty()) {
            return 审核结果.不存在;
        }
        公共乐谱 公共 = 加载结果.get();
        if (公共.获取状态() != 审核状态.待审核) {
            return 审核结果.状态无效;
        }
        公共.设置状态(新状态);
        仓储.保存(公共);
        return 审核结果.成功;
    }

    /**
     * 物理删除公共乐谱。
     *
     * @param 乐谱ID 乐谱ID
     * @return true=已删除；false=不存在
     */
    public boolean 删除(String 乐谱ID) {
        if (乐谱ID == null || 乐谱ID.isBlank()) {
            return false;
        }
        已评分玩家表.remove(乐谱ID);
        return 仓储.删除(乐谱ID);
    }

    /**
     * 列出所有已通过审核的公共乐谱。
     *
     * @return 公共乐谱列表
     */
    public List<公共乐谱> 列出已通过() {
        List<String> IDs = 仓储.列出按状态(审核状态.已通过);
        List<公共乐谱> 结果 = new ArrayList<>(IDs.size());
        for (String id : IDs) {
            Optional<公共乐谱> 加载 = 仓储.加载(id);
            加载.ifPresent(结果::add);
        }
        return Collections.unmodifiableList(结果);
    }

    /**
     * 列出所有待审核的公共乐谱。
     *
     * @return 公共乐谱列表
     */
    public List<公共乐谱> 列出待审核() {
        List<String> IDs = 仓储.列出按状态(审核状态.待审核);
        List<公共乐谱> 结果 = new ArrayList<>(IDs.size());
        for (String id : IDs) {
            Optional<公共乐谱> 加载 = 仓储.加载(id);
            加载.ifPresent(结果::add);
        }
        return Collections.unmodifiableList(结果);
    }

    /**
     * 加载单个公共乐谱（不改变下载计数）。
     *
     * @param 乐谱ID 乐谱ID
     * @return 公共乐谱；不存在返回 empty
     */
    public Optional<公共乐谱> 加载(String 乐谱ID) {
        return 仓储.加载(乐谱ID);
    }

    /**
     * 上传结果回调接口，用于返回生成的乐谱ID。
     */
    @FunctionalInterface
    public interface 返回乐谱ID {
        void 接受(String 乐谱ID);
    }

    /**
     * 下载结果回调接口，用于返回乐谱对象。
     */
    @FunctionalInterface
    public interface 返回乐谱 {
        void 接受(乐谱 乐谱);
    }
}
