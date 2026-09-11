package mljy.基础设施层;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 调试日志器 —— FP-07 重写。
 *
 * 使用 java.util.logging.FileHandler 将调试日志写入 plugins/XRM/logs/xrm-debug-YYYY-MM-DD-HHmmss.log。
 * 每次服务器启动创建新文件。日志格式：[HH:mm:ss.SSS] [模块] 消息。
 */
public final class 调试日志器 {
    private static final String 日志目录名 = "logs";
    private static final String 日志文件名前缀 = "xrm-debug";

    private static boolean 已启用 = false;
    private static FileHandler 文件处理器;
    private static Logger 文件日志器;
    private static Set<String> 已禁用模块 = Collections.emptySet();

    private 调试日志器() {
    }

    public static void 初始化(Logger 日志器, boolean 启用) {
        初始化(日志器, 启用, Collections.emptySet());
    }

    public static void 初始化(Logger 日志器, boolean 启用标志, Set<String> 已禁用模块集) {
        已启用 = 启用标志;
        已禁用模块 = 已禁用模块集 == null ? Collections.emptySet() : 已禁用模块集;
        if (日志器 != null) {
            日志器.info("[XRM-DEBUG] 调试日志器：文件输出尚未配置，请使用 初始化(JavaPlugin) 方法来启用文件日志。"
                    + "当前仅设置内存标志，调试消息不会写入文件。");
        }
    }

    /**
     * 使用 JavaPlugin 实例初始化调试日志器，配置文件输出。
     * 应在插件 onEnable 中调用。
     */
    public static void 初始化(JavaPlugin 插件) {
        try {
            File 日志目录 = new File(插件.getDataFolder(), 日志目录名);
            if (!日志目录.exists()) {
                日志目录.mkdirs();
            }

            String 时间戳 = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
            String 日志文件名 = 日志文件名前缀 + "-" + 时间戳 + ".log";
            File 日志文件 = new File(日志目录, 日志文件名);

            文件日志器 = Logger.getLogger("XRM-Debug");
            文件日志器.setUseParentHandlers(false);

            文件处理器 = new FileHandler(日志文件.getAbsolutePath(), true);
            文件处理器.setFormatter(new 调试格式化器());
            文件日志器.addHandler(文件处理器);

            已启用 = true;
            插件.getLogger().info("[XRM-DEBUG] 调试日志已启用，写入文件: " + 日志文件.getAbsolutePath());
            文件日志器.info("[系统] 调试日志器初始化完成，写入文件: " + 日志文件.getAbsolutePath());
        } catch (IOException e) {
            插件.getLogger().warning("[XRM-DEBUG] 无法创建调试日志文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void 关闭() {
        if (文件日志器 != null && 文件处理器 != null) {
            文件处理器.flush();
            文件处理器.close();
            文件日志器.removeHandler(文件处理器);
        }
        已启用 = false;
    }

    public static boolean 启用() {
        return 已启用;
    }

    public static void 调试(String 模块, String 消息) {
        if (!已启用) return;
        if (已禁用模块.contains(模块)) return;
        写入日志(模块, 消息);
    }

    public static void 调试(String 模块, String 消息, Object... 参数) {
        if (!已启用) return;
        if (已禁用模块.contains(模块)) return;
        String 格式化消息;
        try {
            格式化消息 = String.format(消息, 参数);
        } catch (Exception e) {
            格式化消息 = 消息 + " [格式化失败]";
        }
        写入日志(模块, 格式化消息);
    }

    public static void 调试(String 模块, String 消息, Throwable 异常) {
        if (!已启用) return;
        if (已禁用模块.contains(模块)) return;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        异常.printStackTrace(pw);
        String 堆栈信息 = sw.toString();
        写入日志(模块, 消息 + " 异常=" + 异常.getClass().getSimpleName()
                + " 消息=" + 异常.getMessage() + "\n" + 堆栈信息);
    }

    private static void 写入日志(String 模块, String 消息) {
        if (文件日志器 == null) return;
        文件日志器.info("[" + 模块 + "] " + 消息);
    }

    /**
     * 自定义格式化器。
     * 格式：[HH:mm:ss.SSS] [模块] 消息
     * 注意：模块名已包含在消息中（由 写入日志 方法添加），此处仅添加时间戳前缀。
     */
    private static class 调试格式化器 extends Formatter {
        private final ThreadLocal<SimpleDateFormat> 日期格式化器 =
                ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss.SSS"));

        @Override
        public String format(LogRecord record) {
            String 时间戳 = 日期格式化器.get().format(new Date(record.getMillis()));
            return "[" + 时间戳 + "] " + record.getMessage() + "\n";
        }
    }
}
