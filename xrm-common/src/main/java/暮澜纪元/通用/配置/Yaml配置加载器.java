package 暮澜纪元.通用.配置;

import com.google.inject.Inject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Yaml配置加载器 {

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
        return YamlConfiguration.loadConfiguration(文件);
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
