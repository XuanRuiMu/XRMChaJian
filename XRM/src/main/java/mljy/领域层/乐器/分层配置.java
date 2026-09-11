package mljy.领域层.乐器;

import java.util.Optional;

/**
 * FP-19 分层键盘 split 配置（O7）。
 * 不可变值对象，承载玩家的分层模式状态：
 * - 是否启用分层模式
 * - 分界点音高（左区 < 分界点，右区 >= 分界点）
 * - 左区乐器（音高 < 分界点时使用）
 * - 右区乐器（音高 >= 分界点时使用）
 * <p>
 * GUI 玩家与 mod 玩家共用此配置；mod 玩家在 /乐器 偏好 中指定乐器 1 / 乐器 2。
 * 默认配置：未启用、分界点 12（C4）、左右乐器为 null（回退到玩家当前乐器）。
 * <p>
 * 分层模式下音域切换对两层同时生效（同步上下平移分界点）。
 */
public final class 分层配置 {
    public static final int 默认分界点 = 12;
    public static final int 分界点下限 = 0;
    public static final int 分界点上限 = 24;

    private final boolean 启用分层;
    private final int 分界点;
    private final 乐器定义 左乐器;
    private final 乐器定义 右乐器;

    public 分层配置(boolean 启用分层, int 分界点, 乐器定义 左乐器, 乐器定义 右乐器) {
        this.启用分层 = 启用分层;
        this.分界点 = Math.max(分界点下限, Math.min(分界点上限, 分界点));
        this.左乐器 = 左乐器;
        this.右乐器 = 右乐器;
    }

    public static 分层配置 默认配置() {
        return new 分层配置(false, 默认分界点, null, null);
    }

    public boolean 是否启用分层() {
        return 启用分层;
    }

    public int 获取分界点() {
        return 分界点;
    }

    public Optional<乐器定义> 获取左乐器() {
        return Optional.ofNullable(左乐器);
    }

    public Optional<乐器定义> 获取右乐器() {
        return Optional.ofNullable(右乐器);
    }

    /**
     * 根据音高选择对应乐器。
     * - 未启用分层：返回 玩家当前乐器参数（由调用方提供）
     * - 启用分层：音高 < 分界点 返回左乐器（缺失时回退到默认乐器），否则返回右乐器（缺失时回退到默认乐器）
     *
     * @param 音高       音高
     * @param 默认乐器   默认乐器（未配置左右乐器时回退）
     * @return 实际使用的乐器
     */
    public 乐器定义 选择乐器(int 音高, 乐器定义 默认乐器) {
        if (!启用分层) {
            return 默认乐器;
        }
        乐器定义 选择 = 音高 < 分界点 ? 左乐器 : 右乐器;
        return 选择 != null ? 选择 : 默认乐器;
    }

    /**
     * 创建新配置：切换分层模式开关。
     */
    public 分层配置 切换分层() {
        return new 分层配置(!启用分层, 分界点, 左乐器, 右乐器);
    }

    /**
     * 创建新配置：设置分界点。
     */
    public 分层配置 设置分界点(int 新分界点) {
        return new 分层配置(启用分层, 新分界点, 左乐器, 右乐器);
    }

    /**
     * 创建新配置：设置左乐器。
     */
    public 分层配置 设置左乐器(乐器定义 新左乐器) {
        return new 分层配置(启用分层, 分界点, 新左乐器, 右乐器);
    }

    /**
     * 创建新配置：设置右乐器。
     */
    public 分层配置 设置右乐器(乐器定义 新右乐器) {
        return new 分层配置(启用分层, 分界点, 左乐器, 新右乐器);
    }
}
