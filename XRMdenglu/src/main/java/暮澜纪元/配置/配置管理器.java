package 暮澜纪元.配置;

import com.google.inject.Inject;
import 暮澜纪元.登录服插件;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理器：统一管理 config.yml 的读取。
 * 与XRM主插件的配置管理器保持一致。
 */
public class 配置管理器 {

    private final 登录服插件 插件;
    private FileConfiguration 缓存配置;

    @Inject
    public 配置管理器(登录服插件 插件) {
        this.插件 = 插件;
        this.缓存配置 = 插件.getConfig();
    }

    /**
     * 获取原始配置对象（用于复杂访问）。
     */
    public FileConfiguration 获取配置() {
        return 插件.getConfig();
    }

    /**
     * 重新加载配置缓存。
     */
    public void 刷新缓存() {
        this.缓存配置 = 插件.getConfig();
    }

    // ===== 调试设置 =====

    public boolean 调试模式开启() {
        return 缓存配置.getBoolean("debug", false);
    }

    // ===== 服务器设置 =====

    public String 目标服务器() {
        return 缓存配置.getString("目标服务器", "mmorpg");
    }

    // ===== 数据库设置 =====

    public String 数据库主机() {
        return 缓存配置.getString("数据库.主机", "localhost");
    }

    public int 数据库端口() {
        return 缓存配置.getInt("数据库.端口", 3306);
    }

    public String 数据库名称() {
        return 缓存配置.getString("数据库.数据库名", "燃烧之陨");
    }

    public String 数据库用户名() {
        return 缓存配置.getString("数据库.用户名", "root");
    }

    public String 数据库密码() {
        return 缓存配置.getString("数据库.密码", "BXYXblupz542284");
    }

    public String 数据库连接字符串() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8" +
            "&connectionCollation=utf8mb4_unicode_ci&serverTimezone=UTC" +
            "&useSSL=false&allowPublicKeyRetrieval=true",
            数据库主机(),
            数据库端口(),
            数据库名称()
        );
    }

    /**
     * 保存配置到文件。
     */
    public void 保存配置() {
        插件.saveConfig();
    }
}
