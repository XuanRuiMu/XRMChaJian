package 暮澜纪元.测试工具;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings({"null", "unchecked"})
public class 翻译文件加载器 {

    private static final String 资源根路径 = "src/main/resources/文本消息/";

    public static YamlConfiguration 加载翻译文件(String 相对路径) throws Exception {
        File 文件 = new File(资源根路径 + 相对路径);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
    }

    public static Map<String, String> 加载翻译表(String 相对路径) throws Exception {
        YamlConfiguration 配置 = 加载翻译文件(相对路径);
        Map<String, String> 翻译表 = new LinkedHashMap<>();
        递归加载配置(配置, "", 翻译表);
        return 翻译表;
    }

    public static 消息管理器 创建带磁盘翻译的消息管理器(登录服插件 插件, String 类文件夹路径) throws Exception {
        return 创建带磁盘翻译的消息管理器(插件, List.of(类文件夹路径));
    }

    public static 消息管理器 创建带磁盘翻译的消息管理器(登录服插件 插件, List<String> 类文件夹路径列表) throws Exception {
        Constructor<消息管理器> constructor = 消息管理器.class.getDeclaredConstructor(登录服插件.class);
        constructor.setAccessible(true);
        消息管理器 实例 = constructor.newInstance(插件);

        Field 类语言表Field = 消息管理器.class.getDeclaredField("类语言表");
        类语言表Field.setAccessible(true);

        Map<String, Map<String, Map<String, String>>> 类表 =
            (Map<String, Map<String, Map<String, String>>>) 类语言表Field.get(实例);

        for (String 类文件夹路径 : 类文件夹路径列表) {
            Map<String, String> zhMap = 加载翻译表(类文件夹路径 + "/zh.yml");
            if (!zhMap.isEmpty()) {
                类表.computeIfAbsent(类文件夹路径, k -> new HashMap<>()).put("zh", zhMap);
            }
            try {
                Map<String, String> enMap = 加载翻译表(类文件夹路径 + "/en.yml");
                if (!enMap.isEmpty()) {
                    类表.computeIfAbsent(类文件夹路径, k -> new HashMap<>()).put("en", enMap);
                }
            } catch (Exception ignored) {
            }
        }

        return 实例;
    }

    private static void 递归加载配置(FileConfiguration 配置, String 前缀, Map<String, String> 翻译表) {
        for (String 键 : 配置.getKeys(true)) {
            if (配置.isString(键)) {
                String 完整键 = 前缀.isEmpty() ? 键 : 前缀 + "." + 键;
                翻译表.put(完整键, 配置.getString(键));
            }
        }
    }
}
