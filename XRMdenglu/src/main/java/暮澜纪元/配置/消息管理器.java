package 暮澜纪元.配置;

import com.google.inject.Inject;
import 暮澜纪元.登录服插件;
import 暮澜纪元.通用.文本.文本格式化器;
import 暮澜纪元.通用.翻译.语言代码别名;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * 消息管理器：统一管理多语言消息。
 * 支持从内置资源加载语言文件，支持玩家本地语言自动识别。
 *
 * 翻译文件采用三层镜像结构：
 * 实现代码 src/main/java/暮澜纪元/{包路径}/{类名}.java
 * 翻译文件 src/main/resources/文本消息/{包路径}/{类名}/zh.yml
 * 路径跳过暮澜纪元/层，从功能包开始完全镜像。
 */
public class 消息管理器 {

    private final 登录服插件 插件;
    private final 文本格式化器 格式化器;

    // ===== 新结构：三层镜像 =====
    private final Map<String, Map<String, Map<String, String>>> 类语言表 = new HashMap<>();

    private static final String 翻译根路径 = "文本消息/";

    // ===== 旧结构：功能分类（过渡期兼容） =====
    private final Map<String, Map<String, Map<String, String>>> 功能语言表 = new HashMap<>();

    // 功能分类文件夹列表
    private static final String[] 功能分类列表 = {
        "通用消息",
        "登录系统",
        "职业选择"
    };

    @Inject
    public 消息管理器(登录服插件 插件, 文本格式化器 格式化器) {
        this.插件 = 插件;
        this.格式化器 = 格式化器;
    }

    private 消息管理器(登录服插件 插件) {
        this(插件, 文本格式化器.创建纯文本());
    }

    public void 初始化语言系统() {
        加载所有语言();
        系统日志(获取类翻译(消息管理器.class, "消息管理器.初始化.完成", null, 类语言表.size(), 功能语言表.size(), String.join(", ", 语言代码别名.支持的语言列表)));
    }

    private void 加载所有语言() {
        类语言表.clear();
        功能语言表.clear();

        扫描并加载镜像翻译文件();

        for (String 功能分类 : 功能分类列表) {
            for (String 语言代码 : 语言代码别名.支持的语言列表) {
                加载功能语言文件(功能分类, 语言代码);
            }
        }
    }

    private void 扫描并加载镜像翻译文件() {
        try {
            File jarFile = new File(插件.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarFile.isFile()) {
                系统警告(获取类翻译(消息管理器.class, "消息管理器.日志.文件创建失败", null, "JAR scan failed"));
                return;
            }

            Set<String> 已处理路径 = new HashSet<>();

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(翻译根路径) || !name.endsWith(".yml") || entry.isDirectory()) {
                        continue;
                    }

                    String 相对路径 = name.substring(翻译根路径.length());
                    int 最后斜杠 = 相对路径.lastIndexOf('/');
                    if (最后斜杠 < 0) {
                        continue;
                    }

                    String 类文件夹路径 = 相对路径.substring(0, 最后斜杠);
                    String 文件名 = 相对路径.substring(最后斜杠 + 1);
                    String 语言代码 = 文件名.replace(".yml", "");

                    if (已处理路径.contains(类文件夹路径 + "/" + 语言代码)) {
                        continue;
                    }
                    已处理路径.add(类文件夹路径 + "/" + 语言代码);

                    加载镜像翻译文件(类文件夹路径, 语言代码, name);
                }
            }
        } catch (Exception e) {
            系统警告(获取类翻译(消息管理器.class, "消息管理器.日志.写入失败", null, e.getMessage()));
        }
    }

    private void 加载镜像翻译文件(String 类文件夹路径, String 语言代码, String 资源路径) {
        try (InputStream 输入流 = getClass().getClassLoader().getResourceAsStream(资源路径)) {
            if (输入流 == null) {
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(输入流, StandardCharsets.UTF_8)) {
                FileConfiguration 配置 = YamlConfiguration.loadConfiguration(reader);

                Map<String, String> 翻译表 = new HashMap<>();
                递归加载配置(配置, "", 翻译表);

                if (!翻译表.isEmpty()) {
                    类语言表
                        .computeIfAbsent(类文件夹路径, k -> new HashMap<>())
                        .put(语言代码, 翻译表);
                }
            }
        } catch (IOException e) {
            插件.getLogger().log(Level.WARNING, 获取类翻译(消息管理器.class, "消息管理器.日志.找不到语言文件", null, 资源路径), e);
        }
    }

    private void 系统警告(String 消息) {
        插件.getLogger().warning("[系统] " + 消息);
    }

    /**
     * 加载单个功能分类的语言文件。
     * 从插件内部资源加载，路径格式: 文本消息/功能分类/语言.yml
     */
    private void 加载功能语言文件(String 功能分类名, String 语言代码) {
        String 文件名 = "文本消息/" + 功能分类名 + "/" + 语言代码 + ".yml";

        try (InputStream 输入流 = getClass().getClassLoader().getResourceAsStream(文件名)) {
            if (输入流 == null) {
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(输入流, StandardCharsets.UTF_8)) {
                FileConfiguration 配置 = YamlConfiguration.loadConfiguration(reader);

                Map<String, String> 翻译表 = new HashMap<>();
                递归加载配置(配置, "", 翻译表);

                功能语言表
                    .computeIfAbsent(功能分类名, k -> new HashMap<>())
                    .put(语言代码, 翻译表);
            }
        } catch (IOException e) {
            插件.getLogger().log(Level.WARNING, 获取类翻译(消息管理器.class, "消息管理器.日志.找不到语言文件", null, 文件名), e);
        }
    }

    /**
     * 递归加载配置节。
     */
    private void 递归加载配置(FileConfiguration 配置, String 前缀, Map<String, String> 翻译表) {
        for (String 键 : 配置.getKeys(true)) {
            if (配置.isString(键)) {
                String 完整键 = 前缀.isEmpty() ? 键 : 前缀 + "." + 键;
                翻译表.put(完整键, 配置.getString(键));
            }
        }
    }

    /**
     * 输出系统日志。
     */
    public void 系统日志(String 消息) {
        插件.getLogger().info("[系统] " + 消息);
    }

    /**
     * 获取玩家语言代码。
     */
    private String 获取玩家语言(Player 玩家) {
        try {
            return 语言代码别名.标准化(玩家.locale());
        } catch (Exception e) {
            return 语言代码别名.默认语言代码;
        }
    }

    /**
     * 从指定功能分类获取翻译文本。
     */
    public String 获取翻译(String 功能分类, String 键, CommandSender 发送者) {
        String 语言代码 = (发送者 instanceof Player) ? 获取玩家语言((Player) 发送者) : 语言代码别名.默认语言代码;

        Map<String, Map<String, String>> 功能表 = 功能语言表.get(功能分类);
        if (功能表 == null) {
            return 键;
        }

        Map<String, String> 翻译表 = 功能表.getOrDefault(语言代码, 功能表.get(语言代码别名.默认语言代码));
        if (翻译表 == null) {
            return 键;
        }

        return 翻译表.getOrDefault(键, 键);
    }

    /**
     * 从指定功能分类获取带参数替换的翻译。
     */
    public String 获取翻译(String 功能分类, String 键, CommandSender 发送者, Object... 参数) {
        String 消息 = 获取翻译(功能分类, 键, 发送者);
        for (int i = 0; i < 参数.length; i++) {
            消息 = 消息.replace("{" + i + "}", String.valueOf(参数[i]));
        }
        return 消息;
    }

    /**
     * 发送消息给玩家。
     */
    public void 发送(Player 玩家, String 功能分类, String 键, Object... 参数) {
        String 消息 = 获取翻译(功能分类, 键, 玩家, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        玩家.sendMessage(MiniMessage.miniMessage().deserialize(消息));
    }

    /**
     * 发送消息给命令发送者。
     */
    public void 发送(CommandSender 发送者, String 功能分类, String 键, Object... 参数) {
        String 消息 = 获取翻译(功能分类, 键, 发送者, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        if (发送者 instanceof Player 玩家) {
            玩家.sendMessage(MiniMessage.miniMessage().deserialize(消息));
        } else {
            发送者.sendMessage(消息);
        }
    }

    /**
     * 获取组件形式的消息（使用MiniMessage解析）。
     */
    public Component 获取组件(Player 玩家, String 功能分类, String 键, Object... 参数) {
        String 消息 = 获取翻译(功能分类, 键, 玩家, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        return MiniMessage.miniMessage().deserialize(消息);
    }

    // ===== 新API：三层镜像结构（Class<?> 参数） =====

    public String 获取类翻译(Class<?> 类, String 键, CommandSender 发送者) {
        String 类路径 = 推导类路径(类);
        String 语言代码 = (发送者 instanceof Player) ? 获取玩家语言((Player) 发送者) : 语言代码别名.默认语言代码;

        Map<String, Map<String, String>> 类表 = 类语言表.get(类路径);
        if (类表 == null) {
            return 获取翻译(类路径, 键, 发送者);
        }

        Map<String, String> 翻译表 = 类表.getOrDefault(语言代码, 类表.get(语言代码别名.默认语言代码));
        if (翻译表 == null) {
            return 获取翻译(类路径, 键, 发送者);
        }

        return 翻译表.getOrDefault(键, 键);
    }

    public String 获取类翻译(Class<?> 类, String 键, CommandSender 发送者, Object... 参数) {
        String 消息 = 获取类翻译(类, 键, 发送者);
        for (int i = 0; i < 参数.length; i++) {
            消息 = 消息.replace("{" + i + "}", String.valueOf(参数[i]));
        }
        return 消息;
    }

    public void 发送类翻译(Player 玩家, Class<?> 类, String 键, Object... 参数) {
        String 消息 = 获取类翻译(类, 键, 玩家, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        玩家.sendMessage(MiniMessage.miniMessage().deserialize(消息));
    }

    public void 发送类翻译(CommandSender 发送者, Class<?> 类, String 键, Object... 参数) {
        String 消息 = 获取类翻译(类, 键, 发送者, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        if (发送者 instanceof Player 玩家) {
            玩家.sendMessage(MiniMessage.miniMessage().deserialize(消息));
        } else {
            发送者.sendMessage(消息);
        }
    }

    public Component 获取类组件(Player 玩家, Class<?> 类, String 键, Object... 参数) {
        String 消息 = 获取类翻译(类, 键, 玩家, 参数);
        if (消息 == null) {
            消息 = 键 != null ? 键 : "";
        }
        return MiniMessage.miniMessage().deserialize(消息);
    }

    /**
     * 格式化消息：获取翻译文本后调用文本格式化器处理 [dt]/[plain] 标记。
     * 返回处理后的 Component，支持伤害类型着色和纯文本段。
     */
    public Component 格式化消息(Class<?> 类, String 键, Player 玩家, Object... 参数) {
        String 文本 = 获取类翻译(类, 键, 玩家, 参数);
        if (文本 == null) {
            文本 = 键 != null ? 键 : "";
        }
        return 格式化器.格式化(文本);
    }

    /**
     * 格式化消息（简化版）：默认用消息管理器类路径获取翻译。
     */
    public Component 格式化消息(String 键, Player 玩家, Object... 参数) {
        return 格式化消息(消息管理器.class, 键, 玩家, 参数);
    }

    /**
     * 格式化纯文本：直接调用文本格式化器处理 [dt]/[plain] 标记。
     * 用于已获取的翻译文本需要按段落分隔符分割后逐段格式化的场景。
     */
    public Component 格式化文本(String 文本) {
        if (文本 == null) {
            文本 = "";
        }
        return 格式化器.格式化(文本);
    }

    private String 推导类路径(Class<?> 类) {
        String 全名 = 类.getName();
        String 包路径 = 全名.substring("暮澜纪元.".length());
        return 包路径.replace('.', '/');
    }

    // ===== 快捷方法：通用消息 =====

    public String 获取通用(CommandSender 发送者, String 键) {
        return 获取翻译("通用消息", "通用消息." + 键, 发送者);
    }

    public String 获取通用(CommandSender 发送者, String 键, Object... 参数) {
        return 获取翻译("通用消息", "通用消息." + 键, 发送者, 参数);
    }

    // ===== 快捷方法：登录系统 =====

    public String 获取登录(CommandSender 发送者, String 键) {
        return 获取翻译("登录系统", "登录系统." + 键, 发送者);
    }

    public String 获取登录(CommandSender 发送者, String 键, Object... 参数) {
        return 获取翻译("登录系统", "登录系统." + 键, 发送者, 参数);
    }

    // ===== 快捷方法：职业选择 =====

    public String 获取职业选择(CommandSender 发送者, String 键) {
        return 获取翻译("职业选择", "职业选择." + 键, 发送者);
    }

    public String 获取职业选择(CommandSender 发送者, String 键, Object... 参数) {
        return 获取翻译("职业选择", "职业选择." + 键, 发送者, 参数);
    }

    // ===== 快捷方法：传送管理 =====

    public String 获取传送管理(CommandSender 发送者, String 键) {
        return 获取翻译("通用消息", "传送管理." + 键, 发送者);
    }

    public String 获取传送管理(CommandSender 发送者, String 键, Object... 参数) {
        return 获取翻译("通用消息", "传送管理." + 键, 发送者, 参数);
    }

    /**
     * 重新加载所有消息文件。
     */
    public void 重载() {
        类语言表.clear();
        功能语言表.clear();
        加载所有语言();
        系统日志(获取类翻译(消息管理器.class, "消息管理器.重载.完成", null, 类语言表.size(), 功能语言表.size()));
    }
}
