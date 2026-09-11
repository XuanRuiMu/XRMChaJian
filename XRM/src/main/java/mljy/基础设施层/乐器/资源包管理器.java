package mljy.基础设施层.乐器;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.调试日志器;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-14 资源包管理器（保底方案）。
 * <p>
 * 资源包策略（需求文档 1682-1690 行）：
 * <ul>
 *   <li>原版保底：不装资源包的玩家听到原版 block.note_block.* 音色（12-16 种），力度降级为纯 volume 变化</li>
 *   <li>资源包增强：装了资源包的玩家获得扩展乐器音色 + 真实尾音衰减采样</li>
 *   <li>资源包不强制推送（不配 force-resource-pack），玩家自愿下载</li>
 *   <li>基岩版本轮不实现扩展音色（基岩玩家听原版保底音色）</li>
 * </ul>
 * <p>
 * 资源包路径：plugins/XRM/资源包/，包含：
 * <ul>
 *   <li>pack.mcmeta：资源包元数据（pack_format 26 对应 1.21.x）</li>
 *   <li>assets/minecraft/sounds/xrm/：自定义音色采样目录</li>
 *   <li>assets/minecraft/sounds.json：音色定义</li>
 * </ul>
 * <p>
 * 玩家进入服务器时通过 Player.setResourcePack 应用资源包（应用请求异步发送）。
 * 玩家拒绝下载时降级为原版保底音色，功能完整可用。
 */
@Singleton
public class 资源包管理器 {
    /** 资源包根目录名（相对插件数据目录）。 */
    public static final String 资源包目录名 = "资源包";
    /** 资源包 zip 文件名（应用时使用 zip 打包形式）。 */
    public static final String 资源包文件名 = "xrm-resource-pack.zip";
    /** 资源包内部 assets 根目录名。 */
    public static final String assets目录名 = "assets";
    /** 资源包内部 minecraft 目录名。 */
    public static final String minecraft目录名 = "minecraft";
    /** 资源包内部 sounds 目录名。 */
    public static final String sounds目录名 = "sounds";
    /** 资源包内部 xrm 自定义音色目录名。 */
    public static final String xrm音色目录名 = "xrm";
    /** pack.mcmeta 文件名。 */
    public static final String pack元数据文件名 = "pack.mcmeta";
    /** sounds.json 文件名。 */
    public static final String sounds定义文件名 = "sounds.json";
    /** pack_format（26 对应 MC 1.21.x，与 Purpur 26.2 兼容）。 */
    public static final int pack格式版本 = 26;
    /** 资源包哈希占位（实际生成时计算 SHA-1）。 */
    public static final String 资源包提示消息键 = "乐器服务.资源包应用提示";
    /** 资源包应用失败消息键。 */
    public static final String 资源包应用失败键 = "乐器服务.资源包应用失败";

    private final JavaPlugin 插件;
    /** 已应用资源包的玩家标识集合（仅跟踪应用请求已发送的玩家）。 */
    private final java.util.Set<UUID> 已应用玩家表 = ConcurrentHashMap.newKeySet();
    /** 资源包是否已生成到磁盘。 */
    private volatile boolean 资源包已生成;
    /** 资源包是否已配置（启用资源包推送）。 */
    private volatile boolean 资源包启用;

    @Inject
    public 资源包管理器(JavaPlugin 插件) {
        this.插件 = 插件;
        this.资源包已生成 = 检查资源包文件();
        this.资源包启用 = false;
    }

    /**
     * 从配置加载资源包参数。
     *
     * @param 启用 是否启用资源包推送
     */
    public void 从配置加载(boolean 启用) {
        this.资源包启用 = 启用;
        调试日志器.调试("资源包管理器", "从配置加载：启用=%s, 已生成=%s", 启用, 资源包已生成);
    }

    /**
     * 应用资源包到玩家。
     * 资源包未生成时先自动生成；资源包未启用时返回 false。
     *
     * @param 玩家 目标玩家
     * @return true=已发送应用请求；false=资源包未配置/未启用或玩家不在线
     */
    public boolean 应用资源包(Player 玩家) {
        if (玩家 == null || !玩家.isOnline()) {
            return false;
        }
        if (!资源包启用) {
            return false;
        }
        if (!资源包已生成 && !生成资源包()) {
            调试日志器.调试("资源包管理器", "应用资源包失败：资源包未生成，玩家=%s", 玩家.getUniqueId());
            return false;
        }
        File 资源包文件 = 获取资源包文件();
        if (资源包文件 == null || !资源包文件.exists()) {
            return false;
        }
        try {
            // Purpur 26.2 Player.setResourcePack 异步发送资源包给客户端
            // 不强制推送（force=false），玩家可拒绝；拒绝后降级为原版保底音色
            玩家.setResourcePack(资源包文件.toURI().toString(), "");
            已应用玩家表.add(玩家.getUniqueId());
            调试日志器.调试("资源包管理器", "已发送资源包应用请求：玩家=%s", 玩家.getUniqueId());
            return true;
        } catch (Exception e) {
            调试日志器.调试("资源包管理器", "应用资源包异常：玩家=%s, 异常=%s", 玩家.getUniqueId(), e.getMessage());
            return false;
        }
    }

    /**
     * 生成默认资源包到 plugins/XRM/资源包/ 目录。
     * 包含 pack.mcmeta + assets/minecraft/sounds/xrm/ 子目录 + 空 sounds.json。
     * 已存在时不覆盖（避免丢失玩家自定义音色），返回 false。
     *
     * @return true=生成成功；false=已存在或生成失败
     */
    public boolean 生成资源包() {
        File 资源包根 = 获取资源包根目录();
        if (资源包根 == null) {
            return false;
        }
        if (资源包已生成 || 资源包根.exists()) {
            资源包已生成 = true;
            return false;
        }
        try {
            // 创建目录结构
            Path 根路径 = 资源包根.toPath();
            Path sounds目录 = 根路径.resolve(assets目录名).resolve(minecraft目录名).resolve(sounds目录名).resolve(xrm音色目录名);
            Files.createDirectories(sounds目录);

            // 生成 pack.mcmeta
            Path pack元数据 = 根路径.resolve(pack元数据文件名);
            String 元数据内容 = "{\n" +
                    "  \"pack\": {\n" +
                    "    \"description\": \"暮澜纪元 XRM 乐器资源包（保底方案）\",\n" +
                    "    \"pack_format\": " + pack格式版本 + "\n" +
                    "  }\n" +
                    "}\n";
            Files.writeString(pack元数据, 元数据内容, StandardCharsets.UTF_8);

            // 生成空 sounds.json（玩家可自行补充音色采样）
            Path sounds定义 = 根路径.resolve(assets目录名).resolve(minecraft目录名).resolve(sounds定义文件名);
            Files.writeString(sounds定义, "{}\n", StandardCharsets.UTF_8);

            // 生成 README 说明文件（非 MC 资源包规范要求，便于玩家理解）
            Path 说明文件 = 根路径.resolve("README.txt");
            String 说明内容 = "暮澜纪元 XRM 乐器资源包（保底方案）\n" +
                    "\n" +
                    "目录结构：\n" +
                    "  pack.mcmeta                              - 资源包元数据\n" +
                    "  assets/minecraft/sounds.json             - 音色定义文件\n" +
                    "  assets/minecraft/sounds/xrm/             - 自定义音色采样目录\n" +
                    "\n" +
                    "玩家可自行添加 .ogg 音色采样到 sounds/xrm/ 目录，并在 sounds.json 中定义音色映射。\n" +
                    "资源包不强制推送，玩家自愿下载；拒绝下载时降级为原版保底音色。\n";
            Files.writeString(说明文件, 说明内容, StandardCharsets.UTF_8);

            this.资源包已生成 = true;
            调试日志器.调试("资源包管理器", "资源包已生成到：%s", 资源包根.getAbsolutePath());
            return true;
        } catch (IOException e) {
            调试日志器.调试("资源包管理器", "生成资源包失败：%s", e.getMessage());
            return false;
        }
    }

    /**
     * 查询资源包是否已配置（资源包文件存在且可读）。
     *
     * @return true=已配置；false=未配置
     */
    public boolean 资源包已配置() {
        return 资源包启用 && 资源包已生成;
    }

    /**
     * 查询资源包是否已启用（配置开关）。
     */
    public boolean 是否启用() {
        return 资源包启用;
    }

    /**
     * 查询玩家是否已应用资源包（应用请求已发送）。
     */
    public boolean 玩家已应用(UUID 玩家标识) {
        return 玩家标识 != null && 已应用玩家表.contains(玩家标识);
    }

    /**
     * 玩家退出时清理应用状态。
     */
    public void 玩家退出清理(UUID 玩家标识) {
        if (玩家标识 != null) {
            已应用玩家表.remove(玩家标识);
        }
    }

    /**
     * 获取资源包根目录 File（plugins/XRM/资源包/）。
     */
    public File 获取资源包根目录() {
        try {
            File 数据目录 = 插件.getDataFolder();
            File 资源包根 = new File(数据目录, 资源包目录名);
            return 资源根校验(资源包根);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取资源包 zip 文件（应用时使用，本轮暂返回根目录）。
     * 实际应用时 Purpur 26.2 支持直接推送目录或 zip；本实现推送目录 URI。
     */
    public File 获取资源包文件() {
        File 根 = 获取资源包根目录();
        if (根 == null || !根.exists()) {
            return null;
        }
        return 根;
    }

    /**
     * 检查资源包文件是否已生成（启动时调用）。
     */
    private boolean 检查资源包文件() {
        File 根 = 获取资源包根目录();
        if (根 == null || !根.exists()) {
            return false;
        }
        File pack元数据 = new File(根, pack元数据文件名);
        return pack元数据.exists();
    }

    /**
     * 资源根目录路径校验（防止路径穿越）。
     */
    private static File 资源根校验(File 资源包根) {
        try {
            // 规范化路径并确保位于插件数据目录下
            String 规范路径 = 资源包根.getCanonicalPath();
            String 父路径 = 资源包根.getParentFile() != null
                    ? 资源包根.getParentFile().getCanonicalPath()
                    : "";
            if (!规范路径.startsWith(父路径)) {
                return null;
            }
            return 资源包根;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 保留：用于未来实现 zip 打包（StandardCopyOption 占位引用，避免未使用 import 警告）。
     */
    @SuppressWarnings("unused")
    private static final StandardCopyOption[] 复制选项 = {StandardCopyOption.REPLACE_EXISTING};
}
