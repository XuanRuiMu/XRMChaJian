package 暮澜纪元.通用.翻译;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.google.inject.Inject;

public class Yaml翻译加载器 {
    private static final String 翻译根目录 = "文本消息";
    private static final String 默认语言码 = "zh";
    private static final String YML扩展名 = ".yml";
    private static final String 路径分隔符 = "/";
    private static final int 语言码预估数量 = 32;

    private final JavaPlugin 插件;
    private final Map<String, Map<String, String>> 翻译缓存 = new ConcurrentHashMap<>(语言码预估数量);
    private final List<String> 已注册路径 = new CopyOnWriteArrayList<>();
    private volatile boolean 已全量扫描 = false;

    @Inject
    public Yaml翻译加载器(JavaPlugin 插件) {
        this.插件 = 插件;
    }

    public Yaml翻译加载器() {
        this.插件 = null;
    }

    public String 获取(String 键, Locale 语言) {
        String 语言码 = 语言 != null ? 语言.getLanguage() : 默认语言码;
        确保加载(语言码);
        String 值 = 查找翻译(语言码, 键);
        if (值 != null) {
            return 值;
        }
        if (!语言码.equals(默认语言码)) {
            确保加载(默认语言码);
            值 = 查找翻译(默认语言码, 键);
            if (值 != null) {
                return 值;
            }
        }
        return 键;
    }

    private String 查找翻译(String 语言码, String 键) {
        Map<String, String> 语言表 = 翻译缓存.get(语言码);
        return 语言表 != null ? 语言表.get(键) : null;
    }

    private void 确保加载(String 语言码) {
        if (翻译缓存.containsKey(语言码)) {
            return;
        }
        if (!已全量扫描 && 插件 != null) {
            全量扫描();
        }
    }

    public synchronized void 全量扫描() {
        if (已全量扫描) {
            return;
        }
        已全量扫描 = true;
        if (插件 == null) {
            调试输出("全量扫描跳过：插件为 null");
            return;
        }
        Optional<File> jar文件 = 获取插件Jar文件();
        if (jar文件.isEmpty()) {
            调试输出("全量扫描跳过：未找到插件 jar 文件");
            return;
        }
        String 前缀 = 翻译根目录 + 路径分隔符;
        int 加载计数 = 0;
        try (JarFile jar = new JarFile(jar文件.get())) {
            Enumeration<JarEntry> 条目枚举 = jar.entries();
            while (条目枚举.hasMoreElements()) {
                JarEntry 条目 = 条目枚举.nextElement();
                String 名字 = 条目.getName();
                if (名字.startsWith(前缀) && 名字.endsWith(YML扩展名) && !条目.isDirectory()) {
                    String 语言码 = 提取语言码(名字);
                    try (InputStream 流 = jar.getInputStream(条目)) {
                        加载翻译流(流, 语言码);
                        加载计数++;
                    }
                }
            }
            调试输出("全量扫描完成：加载 " + 加载计数 + " 个翻译文件");
            Map<String, Map<String, String>> 缓存快照 = 翻译缓存;
            for (Map.Entry<String, Map<String, String>> 条目 : 缓存快照.entrySet()) {
                调试输出("语言码=" + 条目.getKey() + " 翻译条目数=" + 条目.getValue().size());
            }
        } catch (Exception e) {
            Logger 日志 = 插件.getLogger();
            if (日志 != null) {
                日志.warning("翻译文件全量扫描失败: " + e.getMessage());
            }
            调试输出("全量扫描异常: " + e.getMessage());
        }
    }

    private void 调试输出(String 消息) {
        if (插件 != null) {
            Logger 日志 = 插件.getLogger();
            if (日志 != null) {
                日志.info("[XRM-DEBUG][翻译加载] " + 消息);
            }
        }
    }

    private String 提取语言码(String 资源路径) {
        int 斜杠位置 = 资源路径.lastIndexOf(路径分隔符);
        String 文件名 = 斜杠位置 >= 0 ? 资源路径.substring(斜杠位置 + 1) : 资源路径;
        return 文件名.substring(0, 文件名.length() - YML扩展名.length());
    }

    public void 注册(String 路径, Locale 语言) {
        if (插件 == null) {
            return;
        }
        String 语言码 = 语言.getLanguage();
        String 完整路径 = 翻译根目录 + 路径分隔符 + 路径 + 路径分隔符 + 语言码 + YML扩展名;
        if (已注册路径.contains(完整路径)) {
            return;
        }
        已注册路径.add(完整路径);
        InputStream 流 = 插件.getResource(完整路径);
        if (流 == null) {
            return;
        }
        try (流) {
            加载翻译流(流, 语言码);
        } catch (Exception e) {
            Logger 日志 = 插件.getLogger();
            if (日志 != null) {
                日志.warning("翻译文件注册加载失败: " + 完整路径 + " - " + e.getMessage());
            }
        }
    }

    public void 加载翻译流(InputStream 流, String 语言码) {
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(new InputStreamReader(流, StandardCharsets.UTF_8));
        Map<String, String> 语言表 = 翻译缓存.computeIfAbsent(语言码, 键 -> new ConcurrentHashMap<>());
        扁平化(配置, 语言表);
    }

    private void 扁平化(FileConfiguration 配置, Map<String, String> 结果) {
        for (String 键 : 配置.getKeys(true)) {
            if (配置.isConfigurationSection(键)) {
                continue;
            }
            结果.put(键, 配置.getString(键, 键));
        }
    }

    private Optional<File> 获取插件Jar文件() {
        if (插件 == null) {
            return Optional.empty();
        }
        try {
            Method 获取文件方法 = JavaPlugin.class.getDeclaredMethod("getFile");
            获取文件方法.setAccessible(true);
            return Optional.ofNullable((File) 获取文件方法.invoke(插件));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
