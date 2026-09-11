package 暮澜纪元;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import 暮澜纪元.菜单.职业选择GUI;
import 暮澜纪元.菜单.职业选择GUI事件处理器;
import 暮澜纪元.命令.传送管理命令;
import 暮澜纪元.数据.数据库连接管理器;
import 暮澜纪元.监听器.传送门监听器;
import 暮澜纪元.监听器.冒险模式强制监听器;
import 暮澜纪元.监听器.重生点监听器;
import 暮澜纪元.监听器.聊天阻止监听器;
import 暮澜纪元.配置.配置管理器;
import 暮澜纪元.配置.登录服配置;
import 暮澜纪元.配置.消息管理器;

/**
 * XRMdenglu 登录服插件主类。
 * 处理玩家登录、职业选择和服务器传送。
 * 采用与XRM主插件一致的结构设计。
 */
public class 登录服插件 {

    private static 登录服插件 实例;
    private static JavaPlugin 外部插件实例;

    // 核心子系统
    private 配置管理器 配置;
    private 消息管理器 消息;
    private 登录服配置 登录配置;
    private 数据库连接管理器 数据库;
    private 职业选择GUI 职业选择GUI实例;

    public static void 设置外部实例(JavaPlugin 插件) {
        外部插件实例 = 插件;
    }

    public JavaPlugin 获取JavaPlugin() {
        return 外部插件实例;
    }

    public static 登录服插件 获取实例() {
        return 实例;
    }

    /**
     * 启动插件。
     * 采用阶段式初始化，与XRM主插件保持一致。
     */
    public void 启动(JavaPlugin 插件) {
        实例 = this;
        外部插件实例 = 插件;

        // 强制保存所有配置文件
        保存所有默认资源(插件);

        同步数据库配置到运行时config();

        Injector 注入器 = Guice.createInjector(new Guice模块(this));

        // 阶段1：配置系统
        this.配置 = 注入器.getInstance(配置管理器.class);
        this.消息 = 注入器.getInstance(消息管理器.class);
        this.消息.初始化语言系统();
        消息.系统日志("配置系统初始化完成");

        // 阶段2：登录配置系统
        this.登录配置 = new 登录服配置(this);
        this.登录配置.加载配置();
        消息.系统日志("登录配置系统初始化完成");

        // 阶段3：数据库系统
        this.数据库 = new 数据库连接管理器(this);
        if (!this.数据库.初始化()) {
            插件.getServer().getPluginManager().disablePlugin(插件);
            return;
        }
        消息.系统日志("数据库系统初始化完成");

        // 阶段4：GUI系统
        this.职业选择GUI实例 = new 职业选择GUI(this, this.数据库);
        消息.系统日志("GUI系统初始化完成");

        // 注册BungeeCord通道
        插件.getServer().getMessenger().registerOutgoingPluginChannel(插件, "BungeeCord");

        // 阶段5：注册监听器和命令
        注册监听器(插件);
        注册命令(插件);

        消息.系统日志("登录服插件已启动");
    }

    private void 保存所有默认资源(JavaPlugin 插件) {
        String 当前插件版本 = 插件.getPluginMeta().getVersion();

        java.io.File 配置文件 = new java.io.File(插件.getDataFolder(), "config.yml");
        boolean 需要更新 = true;

        if (配置文件.exists()) {
            try {
                java.util.List<String> 行 = java.nio.file.Files.readAllLines(
                    配置文件.toPath(), java.nio.charset.StandardCharsets.UTF_8);

                for (String 内容 : 行) {
                    if (内容.contains("版本:")) {
                        String 文件版本 = 内容.substring(内容.indexOf("版本:") + 3).trim();
                        if (文件版本.equals(当前插件版本)) {
                            需要更新 = false;
                        }
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (需要更新) {
            插件.saveDefaultConfig();
        }
    }

    private void 同步数据库配置到运行时config() {
        String 源码数据库名 = "燃烧之陨";
        String 源码数据库密码 = "BXYXblupz542284";
        boolean 需要重载 = false;
        String 运行时数据库名 = getConfig().getString("数据库.数据库名", "");
        String 运行时数据库密码 = getConfig().getString("数据库.密码", "");
        if (!源码数据库名.equals(运行时数据库名)) {
            getLogger().info("[XRMdenglu-Config同步] 数据库.数据库名 不一致：运行时=" + 运行时数据库名 + "，源码默认=" + 源码数据库名 + "，将覆盖为源码默认值");
            getConfig().set("数据库.数据库名", 源码数据库名);
            需要重载 = true;
        }
        if (!源码数据库密码.equals(运行时数据库密码)) {
            getLogger().info("[XRMdenglu-Config同步] 数据库.密码 不一致：运行时=***，源码默认=***，将覆盖为源码默认值");
            getConfig().set("数据库.密码", 源码数据库密码);
            需要重载 = true;
        }
        if (需要重载) {
            saveConfig();
            reloadConfig();
            getLogger().info("[XRMdenglu-Config同步] 运行时 config.yml 已同步源码默认数据库配置并 reload");
        } else {
            getLogger().info("[XRMdenglu-Config同步] 运行时 config.yml 数据库配置与源码默认值一致，无需同步");
        }
    }

    /**
     * 关闭插件。
     */
    public void 关闭() {
        if (外部插件实例 != null) {
            外部插件实例.getServer().getMessenger()
                .unregisterOutgoingPluginChannel(外部插件实例);
        }

        if (数据库 != null) {
            数据库.关闭();
        }

        消息.系统日志("登录服插件已关闭");
        实例 = null;
    }

    private void 注册监听器(JavaPlugin 插件) {
        插件.getServer().getPluginManager().registerEvents(
            new 传送门监听器(this, 职业选择GUI实例), 插件);
        插件.getServer().getPluginManager().registerEvents(
            new 职业选择GUI事件处理器(职业选择GUI实例), 插件);
        插件.getServer().getPluginManager().registerEvents(
            new 重生点监听器(this), 插件);
        插件.getServer().getPluginManager().registerEvents(
            new 冒险模式强制监听器(this), 插件);
        插件.getServer().getPluginManager().registerEvents(
            new 聊天阻止监听器(this), 插件);
    }

    private void 注册命令(JavaPlugin 插件) {
        PluginCommand 传送管理命令实例 = 插件.getCommand("传送管理");
        if (传送管理命令实例 != null) {
            传送管理命令 执行器 = new 传送管理命令(this);
            传送管理命令实例.setExecutor(执行器);
            传送管理命令实例.setTabCompleter(执行器);
        }
    }

    // ===== 获取器方法 =====

    public 配置管理器 获取配置管理器() {
        return this.配置;
    }

    public 消息管理器 获取消息管理器() {
        return this.消息;
    }

    public 登录服配置 获取登录配置() {
        return this.登录配置;
    }

    public 数据库连接管理器 获取数据库() {
        return this.数据库;
    }

    public 职业选择GUI 获取职业选择GUI() {
        return this.职业选择GUI实例;
    }

    /**
     * 获取原始配置对象（用于复杂访问）。
     */
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return 外部插件实例.getConfig();
    }

    public void saveConfig() {
        外部插件实例.saveConfig();
    }

    public void reloadConfig() {
        外部插件实例.reloadConfig();
    }

    /**
     * 获取日志记录器。
     */
    public java.util.logging.Logger getLogger() {
        return 外部插件实例.getLogger();
    }

    /**
     * 通过BungeeCord传送玩家到指定服务器。
     *
     * @param 玩家 目标玩家
     * @param 服务器名 目标服务器名称
     */
    public void 传送玩家到服务器(Player 玩家, String 服务器名) {
        if (服务器名 == null || 服务器名.isEmpty()) {
            return;
        }
        ByteArrayDataOutput 输出流 = ByteStreams.newDataOutput();
        输出流.writeUTF("Connect");
        输出流.writeUTF(服务器名);
        玩家.sendPluginMessage(获取JavaPlugin(), "BungeeCord", 输出流.toByteArray());
    }
}
