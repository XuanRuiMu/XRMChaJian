package mljy.表现层.适配器;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("玄锐暮插件")
class 玄锐暮插件测试 {
    private static final Path 源文件 = Path.of(
            "src/main/java/mljy/表现层/适配器/玄锐暮插件.java");

    @Test
    @DisplayName("每秒生产周期应刷新全部在线玩家效果")
    void 每秒生产周期_刷新在线玩家效果() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("效果调度.刷新(玩家.getUniqueId(), 当前时间)"));
    }

    @Test
    @DisplayName("玩家退出应清空该玩家全部效果")
    void 玩家退出_清空玩家效果() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("效果调度.清空(事件.getPlayer().getUniqueId(), \"玩家退出\")"));
    }

    @Test
    @DisplayName("玩家加入应在属性同步后应用默认生命条缩放")
    void 玩家加入_属性同步后应用默认生命条缩放() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        int 属性同步位置 = 源码.indexOf("服务.玩家登录(事件.getPlayer().getUniqueId(), 事件.getPlayer().getName())");
        int 延迟调度位置 = 源码.indexOf("Bukkit.getScheduler().runTask(this");
        int 默认缩放位置 = 源码.indexOf("生命条缩放处理器.应用默认缩放(玩家)", 延迟调度位置);
        assertTrue(属性同步位置 >= 0);
        assertTrue(延迟调度位置 > 属性同步位置);
        assertTrue(默认缩放位置 > 延迟调度位置);
        assertTrue(源码.contains("Bukkit.getPlayer(玩家标识)"));
        assertTrue(源码.contains("玩家 == null || !玩家.isOnline()"));
    }

    @Test
    @DisplayName("插件重载应为全部在线玩家重新应用默认生命条缩放")
    void 插件重载_应用在线玩家默认生命条缩放() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("应用在线玩家默认生命条缩放();"));
        assertTrue(源码.contains("for (Player 玩家 : Bukkit.getOnlinePlayers())"));
        assertTrue(源码.contains("生命条缩放处理器.应用默认缩放(玩家)"));
    }

    @Test
    @DisplayName("玩家退出应清理生命条缩放内存设置")
    void 玩家退出_清理生命条缩放内存设置() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("生命条缩放处理器.清理玩家设置(事件.getPlayer().getUniqueId())"));
    }

    @Test
    @DisplayName("插件禁用应清空效果仓库中的全部玩家")
    void 插件禁用_清空效果仓库全部玩家() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("效果调度.清空全部(\"插件禁用\")"));
        assertTrue(源码.contains("生命条缩放处理器.清理全部设置()"));
        assertTrue(源码.contains("效果生命周期监听器.class"));
    }

    @Test
    @DisplayName("玩家退出死亡换世界及插件禁用应立即清理大招任务")
    void 大招任务_覆盖全部生命周期出口() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("清理奥能法师大招任务(事件.getPlayer().getUniqueId(), \"玩家退出\")"));
        assertTrue(源码.contains("清理奥能法师大招任务(事件.getEntity().getUniqueId(), \"玩家死亡\")"));
        assertTrue(源码.contains("清理奥能法师大招任务(事件.getPlayer().getUniqueId(), \"玩家切换世界\")"));
        assertTrue(源码.contains("清理全部奥能法师大招任务(\"插件禁用\")"));
        assertTrue(源码.contains("执行器.取消全部任务(原因)"));
    }

    @Test
    @DisplayName("FP-9 插件启用应初始化数据库连接池与生命条缩放设置表")
    void 插件启用_初始化数据库与生命条缩放表() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("import mljy.基础设施层.数据库.数据库连接池;"));
        assertTrue(源码.contains("import mljy.基础设施层.数据库.表初始化器;"));
        assertTrue(源码.contains("数据库连接池 数据库连接 = 注入器.getInstance(数据库连接池.class);"));
        assertTrue(源码.contains("if (数据库连接.已启用())"));
        assertTrue(源码.contains("表初始化器 表初始化 = 注入器.getInstance(表初始化器.class);"));
        assertTrue(源码.contains("表初始化.初始化生命条缩放设置表();"));
        assertTrue(源码.contains("数据库未启用，生命条缩放持久化降级为内存模式"));
    }

    @Test
    @DisplayName("FP-9 插件禁用应关闭数据库连接池")
    void 插件禁用_关闭数据库连接池() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("数据库连接池 数据库连接 = 注入器.getInstance(数据库连接池.class);"));
        assertTrue(源码.contains("数据库连接.关闭();"));
    }

    @Test
    @DisplayName("FP-9 玩家加入应优先应用持久化设置无记录才回退默认缩放")
    void 玩家加入_优先应用持久化设置() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        int 持久化位置 = 源码.indexOf("生命条缩放处理器.应用持久化设置(玩家)");
        int 默认缩放位置 = 源码.indexOf("生命条缩放处理器.应用默认缩放(玩家)", 持久化位置);
        assertTrue(持久化位置 >= 0);
        assertTrue(默认缩放位置 > 持久化位置);
        assertTrue(源码.contains("boolean 持久化结果 = 生命条缩放处理器.应用持久化设置(玩家);"));
        assertTrue(源码.contains("if (!持久化结果)"));
    }

    @Test
    @DisplayName("FP-9 应用在线玩家默认生命条缩放应优先尝试持久化加载")
    void 应用在线玩家_优先持久化加载() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(源码.contains("if (生命条缩放处理器.应用持久化设置(玩家)) {"));
        assertTrue(源码.contains("持久化命中数量++"));
        assertTrue(源码.contains("} else if (生命条缩放处理器.应用默认缩放(玩家)) {"));
        assertTrue(源码.contains("默认应用数量++"));
    }

    @Test
    @DisplayName("FP-7 调试日志器初始化应在注入器创建与命令处理器实例化之前")
    void 调试日志器初始化_应在注入器创建之前() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        int 调试日志器初始化位置 = 源码.indexOf("调试日志器.初始化(this);");
        int 注入器创建位置 = 源码.indexOf("注入器 = Guice.createInjector(new Guice模块(this));");
        int 处理器实例化位置 = 源码.indexOf("生命条缩放处理器 = 注入器.getInstance(生命条缩放命令处理器.class);");

        assertTrue(调试日志器初始化位置 >= 0, "应包含 调试日志器.初始化(this) 调用");
        assertTrue(注入器创建位置 >= 0, "应包含 Guice.createInjector 调用");
        assertTrue(处理器实例化位置 >= 0, "应包含 生命条缩放命令处理器 实例化");

        assertTrue(调试日志器初始化位置 < 注入器创建位置,
                "调试日志器初始化必须在注入器创建之前，否则数据库连接池构造函数中的日志会被丢弃");
        assertTrue(注入器创建位置 < 处理器实例化位置,
                "注入器创建必须在命令处理器实例化之前");
    }

    @Test
    @DisplayName("FP-7 config.yml 数据库密码应已更新为正确值")
    void configYml_数据库密码应已更新() throws Exception {
        File 配置文件 = new File("src/main/resources/config.yml");
        assertTrue(配置文件.exists(), "config.yml 应存在");

        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(配置文件.toPath()), StandardCharsets.UTF_8));

        assertEquals("BXYXblupz542284", 配置.getString("数据库.密码"),
                "数据库.密码 应为 BXYXblupz542284，实际：" + 配置.getString("数据库.密码"));
    }

    @Test
    @DisplayName("FP-7 config.yml 数据库名应已更新为 燃烧之陨")
    void configYml_数据库名应已更新() throws Exception {
        File 配置文件 = new File("src/main/resources/config.yml");
        assertTrue(配置文件.exists(), "config.yml 应存在");

        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(配置文件.toPath()), StandardCharsets.UTF_8));

        assertEquals("燃烧之陨", 配置.getString("数据库.数据库名"),
                "数据库.数据库名 应为 燃烧之陨，实际：" + 配置.getString("数据库.数据库名"));
    }

    @Test
    @DisplayName("FP-04 onEnable 应在 调试日志器初始化 后、注入器创建 前 同步数据库配置")
    void onEnable_同步数据库配置时机正确() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);
        int 调试日志器初始化位置 = 源码.indexOf("调试日志器.初始化(this);");
        int 同步方法调用位置 = 源码.indexOf("同步数据库配置到运行时config();");
        int 注入器创建位置 = 源码.indexOf("注入器 = Guice.createInjector(new Guice模块(this));");
        assertTrue(调试日志器初始化位置 >= 0, "应包含 调试日志器.初始化(this) 调用");
        assertTrue(同步方法调用位置 >= 0, "应包含 同步数据库配置到运行时config() 调用");
        assertTrue(注入器创建位置 >= 0, "应包含 Guice.createInjector 调用");
        assertTrue(调试日志器初始化位置 < 同步方法调用位置, "调试日志器初始化必须在同步方法调用之前");
        assertTrue(同步方法调用位置 < 注入器创建位置, "同步方法调用必须在注入器创建之前（数据库连接池在注入器创建时初始化）");
    }

    @Test
    @DisplayName("FP-04 同步方法应包含源码默认值与运行时值比较逻辑")
    void 同步方法_应包含配置比较逻辑() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);
        assertTrue(源码.contains("private void 同步数据库配置到运行时config()"), "应定义同步方法");
        assertTrue(源码.contains("String 源码数据库名 = \"燃烧之陨\""), "应包含源码数据库名默认值");
        assertTrue(源码.contains("String 源码数据库密码 = \"BXYXblupz542284\""), "应包含源码数据库密码默认值");
        assertTrue(源码.contains("getConfig().getString(\"数据库.数据库名\""), "应读取运行时数据库名");
        assertTrue(源码.contains("getConfig().getString(\"数据库.密码\""), "应读取运行时数据库密码");
        assertTrue(源码.contains("saveConfig()"), "应保存配置");
        assertTrue(源码.contains("reloadConfig()"), "应重新加载配置");
    }
}
