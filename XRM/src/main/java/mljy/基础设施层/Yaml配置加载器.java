package mljy.基础设施层;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Yaml配置加载器 {
    private static final String 模块名 = "Yaml配置加载器";

    private final JavaPlugin 插件;

    @Inject
    public Yaml配置加载器(JavaPlugin 插件) {
        this.插件 = 插件;
    }

    public FileConfiguration 加载(String 文件名) {
        File 文件 = new File(插件.getDataFolder(), 文件名);
        if (!文件.exists()) {
            保存默认文件(文件名);
        }
        if (文件.exists()) {
            FileConfiguration 配置 = YamlConfiguration.loadConfiguration(文件);
            if (配置.getKeys(false).isEmpty()) {
                调试日志器.调试(模块名, "外部文件加载为空配置，从 jar 内资源回退加载: " + 文件名);
                return 加载资源(文件名);
            }
            return 配置;
        }
        // 外部文件不存在（saveResource 失败，可能 jar 内无该资源或路径不匹配），从 jar 内资源回退加载
        调试日志器.调试(模块名, "外部文件不存在，从 jar 内资源回退加载: " + 文件名);
        FileConfiguration 资源配置 = 加载资源(文件名);
        if (资源配置.getKeys(false).isEmpty()) {
            调试日志器.调试(模块名, "jar 内资源也为空: " + 文件名);
        }
        return 资源配置;
    }

    public FileConfiguration 加载资源(String 资源路径) {
        InputStream 流 = 插件.getResource(资源路径);
        if (流 == null) {
            return YamlConfiguration.loadConfiguration(new File(插件.getDataFolder(), 资源路径));
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(流, StandardCharsets.UTF_8));
    }

    public void 保存默认文件(String 文件名) {
        File 文件 = new File(插件.getDataFolder(), 文件名);
        if (!文件.getParentFile().exists()) {
            文件.getParentFile().mkdirs();
        }
        插件.saveResource(文件名, false);
    }

    public File 获取文件(String 文件名) {
        return new File(插件.getDataFolder(), 文件名);
    }
}
