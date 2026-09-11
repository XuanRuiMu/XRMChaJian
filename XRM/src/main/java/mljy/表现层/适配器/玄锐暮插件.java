package mljy.表现层.适配器;

import com.google.inject.Guice;
import com.google.inject.Injector;
import mljy.Guice模块;
import mljy.玩家服务;
import mljy.技能服务;
import mljy.基础设施层.扫描技能注册表;
import mljy.基础设施层.调试日志器;
import mljy.技能实现.奥能法师.奥术冲击;
import mljy.技能实现.奥能法师.奥术护盾;
import mljy.技能实现.奥能法师.奥能冥想;
import mljy.技能实现.奥能法师.秘法崩裂;
import mljy.技能实现.奥能法师.秘法回流;
import mljy.技能实现.奥能法师.秘法湮灭;
import mljy.技能实现.奥能法师.秘法陨落;
import mljy.技能实现.奥能法师.混沌束缚;
import mljy.技能实现.奥能法师.精通魔法预兆;
import mljy.资源服务;
import mljy.业务层.乐器服务;
import mljy.业务层.公共冷却显示服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.技能冷却服务;
import mljy.业务层.战斗状态服务;
import mljy.业务层.仇恨服务;
import mljy.业务层.组队服务;
import mljy.业务层.驭空术服务;
import mljy.基础设施层.数据库.数据库连接池;
import mljy.基础设施层.数据库.表初始化器;
import mljy.表现层.命令.生命条缩放指令.生命条缩放命令处理器;
import mljy.表现层.监听器.乐器监听器;
import mljy.表现层.监听器.效果生命周期监听器;
import mljy.表现层.监听器.额外功能监听器管理器;
import mljy.表现层.监听器.技能按键绑定监听器;
import mljy.表现层.监听器.技能移动打断监听器;
import mljy.表现层.监听器.战斗监听器;
import mljy.表现层.监听器.反伤监听器;
import mljy.表现层.监听器.伤害探针监听器;
import mljy.表现层.监听器.EliteMobs伤害修复监听器;
import mljy.表现层.监听器.躲闪监听器;
import mljy.表现层.监听器.装备切换监听器;
import mljy.表现层.监听器.驭空术监听器;
import mljy.表现层.菜单.菜单监听器;
import mljy.表现层.菜单.菜单服务;
import mljy.表现层.计分板.计分板监听器;
import mljy.表现层.计分板.计分板服务;
import mljy.业务层.乐器.节拍器;
import mljy.领域层.乐器.乐器注册表;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class 玄锐暮插件 extends JavaPlugin implements Listener {
    private Injector 注入器;
    private 生命条缩放命令处理器 生命条缩放处理器;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean 调试开关 = getConfig().getBoolean("调试.全局开关", true);
        Set<String> 已禁用模块 = new HashSet<>(getConfig().getStringList("调试.已禁用模块"));
        调试日志器.初始化(getLogger(), 调试开关, 已禁用模块);
        调试日志器.初始化(this);
        同步数据库配置到运行时config();
        注入器 = Guice.createInjector(new Guice模块(this));
        加载乐器配置();
        // FP-12 注册 mod 输入通道（PluginMessage 通道 "xrm:music_input"）
        乐器服务 乐器服务实例 = 注入器.getInstance(乐器服务.class);
        乐器服务实例.注册mod输入通道();
        生命条缩放处理器 = 注入器.getInstance(生命条缩放命令处理器.class);
        getServer().getPluginManager().registerEvents(this, this);

        扫描技能注册表 注册表 = 注入器.getInstance(扫描技能注册表.class);
        注册表.扫描并注册(Set.of(
                奥术冲击.class,
                秘法崩裂.class,
                奥术护盾.class,
                秘法陨落.class,
                混沌束缚.class,
                秘法回流.class,
                奥能冥想.class,
                秘法湮灭.class,
                精通魔法预兆.class
        ));

        命令注册器 命令注册 = 注入器.getInstance(命令注册器.class);
        命令注册.注册命令();
        数据库连接池 数据库连接 = 注入器.getInstance(数据库连接池.class);
        调试日志器.调试("数据库连接池", "插件启用：数据库连接池状态=[%s]", 数据库连接.获取配置摘要());
        if (数据库连接.已启用()) {
            表初始化器 表初始化 = 注入器.getInstance(表初始化器.class);
            boolean 表初始化结果 = 表初始化.初始化生命条缩放设置表();
            调试日志器.调试("数据库连接池", "插件启用：生命条缩放设置表初始化结果=%s", 表初始化结果);
        } else {
            调试日志器.调试("数据库连接池", "插件启用：数据库未启用，生命条缩放持久化降级为内存模式");
        }
        应用在线玩家默认生命条缩放();

        额外功能监听器管理器 额外功能管理器 = 注入器.getInstance(额外功能监听器管理器.class);
        额外功能管理器.注册所有监听器();

        getServer().getPluginManager().registerEvents(注入器.getInstance(技能按键绑定监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(战斗监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(躲闪监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(反伤监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(装备切换监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(菜单监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(计分板监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(驭空术监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(乐器监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(技能移动打断监听器.class), this);
        getServer().getPluginManager().registerEvents(注入器.getInstance(效果生命周期监听器.class), this);

        boolean 伤害探针启用 = getConfig().getBoolean("伤害探针.启用", false);
        if (伤害探针启用) {
            getServer().getPluginManager().registerEvents(注入器.getInstance(伤害探针监听器.class), this);
            getLogger().info("[XRM-FP04] 伤害探针监听器已启用，将输出6个优先级的XRM技能伤害日志");
        }

        boolean EliteMobs伤害修复启用 = getConfig().getBoolean("EliteMobs伤害修复.启用", true);
        if (EliteMobs伤害修复启用) {
            getServer().getPluginManager().registerEvents(注入器.getInstance(EliteMobs伤害修复监听器.class), this);
            getLogger().info("[XRM-FP03] EliteMobs伤害修复监听器已启用，将在HIGHEST优先级恢复XRM技能伤害值");
        }

        菜单服务 菜单 = 注入器.getInstance(菜单服务.class);
        菜单.注册菜单();

        战斗状态服务 战斗状态 = 注入器.getInstance(战斗状态服务.class);
        仇恨服务 仇恨 = 注入器.getInstance(仇恨服务.class);
        组队服务 组队 = 注入器.getInstance(组队服务.class);
        计分板服务 计分板 = 注入器.getInstance(计分板服务.class);
        驭空术服务 驭空术 = 注入器.getInstance(驭空术服务.class);
        效果调度服务 效果调度 = 注入器.getInstance(效果调度服务.class);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long 当前时间 = System.currentTimeMillis();
            for (Player 玩家 : Bukkit.getOnlinePlayers()) {
                效果调度.刷新(玩家.getUniqueId(), 当前时间);
            }
            战斗状态.刷新脱战();
            战斗状态.执行脱战回血();
            战斗状态.执行秘能衰减();
            仇恨.清除无效仇恨();
            组队.清理过期邀请();
            组队.清理离线超时成员();
            驭空术.每秒更新();
        }, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            驭空术.每Tick更新();
        }, 1L, 1L);

        getLogger().info("暮澜纪元 XRM 插件已启用");
    }

    @Override
    public void onDisable() {
        if (注入器 != null) {
            清理全部奥能法师大招任务("插件禁用");
            if (生命条缩放处理器 != null) {
                生命条缩放处理器.清理全部设置();
            }
            效果调度服务 效果调度 = 注入器.getInstance(效果调度服务.class);
            调试日志器.调试("效果生命周期", "插件禁用清理全部效果：原因=插件禁用");
            效果调度.清空全部("插件禁用");
            数据库连接池 数据库连接 = 注入器.getInstance(数据库连接池.class);
            数据库连接.关闭();
        }
        调试日志器.关闭();
        getLogger().info("暮澜纪元 XRM 插件已禁用");
    }

    public Injector 获取注入器() {
        return 注入器;
    }

    private void 应用在线玩家默认生命条缩放() {
        if (生命条缩放处理器 == null) {
            调试日志器.调试("生命条缩放命令处理器", "插件启用/重载：处理器为空，跳过在线玩家默认缩放");
            return;
        }
        int 在线玩家数量 = 0;
        int 持久化命中数量 = 0;
        int 默认应用数量 = 0;
        for (Player 玩家 : Bukkit.getOnlinePlayers()) {
            在线玩家数量++;
            if (生命条缩放处理器.应用持久化设置(玩家)) {
                持久化命中数量++;
            } else if (生命条缩放处理器.应用默认缩放(玩家)) {
                默认应用数量++;
            }
        }
        调试日志器.调试("生命条缩放命令处理器",
                "插件启用/重载：在线玩家缩放应用完成，总数=%d，持久化命中=%d，默认应用=%d",
                在线玩家数量, 持久化命中数量, 默认应用数量);
    }

    private void 同步数据库配置到运行时config() {
        String 源码数据库名 = "燃烧之陨";
        String 源码数据库密码 = "BXYXblupz542284";
        boolean 需要重载 = false;
        String 运行时数据库名 = getConfig().getString("数据库.数据库名", "");
        String 运行时数据库密码 = getConfig().getString("数据库.密码", "");
        if (!源码数据库名.equals(运行时数据库名)) {
            getLogger().info("[XRM-Config同步] 数据库.数据库名 不一致：运行时=" + 运行时数据库名 + "，源码默认=" + 源码数据库名 + "，将覆盖为源码默认值");
            getConfig().set("数据库.数据库名", 源码数据库名);
            需要重载 = true;
        }
        if (!源码数据库密码.equals(运行时数据库密码)) {
            getLogger().info("[XRM-Config同步] 数据库.密码 不一致：运行时=***，源码默认=***，将覆盖为源码默认值");
            getConfig().set("数据库.密码", 源码数据库密码);
            需要重载 = true;
        }
        if (需要重载) {
            saveConfig();
            reloadConfig();
            getLogger().info("[XRM-Config同步] 运行时 config.yml 已同步源码默认数据库配置并 reload");
        } else {
            getLogger().info("[XRM-Config同步] 运行时 config.yml 数据库配置与源码默认值一致，无需同步");
        }
    }

    /**
     * FP-13 配置驱动：加载 乐器配置.yml 到 乐器注册表。
     * 必须在 乐器服务实现 构造前完成，否则 乐器服务实现 会拿到默认全局参数。
     */
    private void 加载乐器配置() {
        File 配置文件 = new File(getDataFolder(), "乐器配置.yml");
        if (!配置文件.exists()) {
            saveResource("乐器配置.yml", false);
        }
        if (!配置文件.exists()) {
            getLogger().warning("[XRM-乐器配置] 乐器配置.yml 不存在，使用默认配置");
            return;
        }
        FileConfiguration 乐器配置 = YamlConfiguration.loadConfiguration(配置文件);
        乐器注册表 注册表 = 注入器.getInstance(乐器注册表.class);
        int 数量 = 注册表.从配置加载(乐器配置);
        getLogger().info("[XRM-乐器配置] 加载了 " + 数量 + " 种乐器");
        // FP-08: 加载节拍器配置（BPM/拍号/count-in 等）
        节拍器 节拍器实例 = 注入器.getInstance(节拍器.class);
        节拍器实例.从配置加载(乐器配置);
    }

    @EventHandler
    public void 玩家加入(PlayerJoinEvent 事件) {
        玩家服务 服务 = 注入器.getInstance(玩家服务.class);
        服务.玩家登录(事件.getPlayer().getUniqueId(), 事件.getPlayer().getName());
        UUID 玩家标识 = 事件.getPlayer().getUniqueId();
        调试日志器.调试("生命条缩放命令处理器", "玩家加入事件触发：玩家=%s，姓名=%s，准备延迟应用生命条缩放",
                玩家标识, 事件.getPlayer().getName());
        Bukkit.getScheduler().runTask(this, () -> {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 == null || !玩家.isOnline()) {
                调试日志器.调试("生命条缩放命令处理器", "玩家加入延迟应用跳过：玩家已离线，玩家=%s", 玩家标识);
                return;
            }
            调试日志器.调试("生命条缩放命令处理器", "玩家加入延迟任务开始执行：玩家=%s，尝试从 MySQL 加载持久化设置", 玩家标识);
            boolean 持久化结果 = 生命条缩放处理器.应用持久化设置(玩家);
            if (!持久化结果) {
                调试日志器.调试("生命条缩放命令处理器", "玩家加入持久化未命中或应用失败，回退到默认缩放：玩家=%s", 玩家标识);
                boolean 默认结果 = 生命条缩放处理器.应用默认缩放(玩家);
                调试日志器.调试("生命条缩放命令处理器", "玩家加入默认缩放应用结果=%s，玩家=%s", 默认结果, 玩家标识);
            } else {
                调试日志器.调试("生命条缩放命令处理器", "玩家加入持久化应用成功，无需默认缩放：玩家=%s", 玩家标识);
            }
        });
        资源服务 资源服务 = 注入器.getInstance(资源服务.class);
        资源服务.初始化玩家资源(事件.getPlayer().getUniqueId());
        组队服务 组队 = 注入器.getInstance(组队服务.class);
        组队.玩家上线(事件.getPlayer().getUniqueId());
        公共冷却显示服务 公共冷却显示 = 注入器.getInstance(公共冷却显示服务.class);
        公共冷却显示.启动恒定显示(事件.getPlayer().getUniqueId());
        // FP-14 玩家加入时应用资源包（如果已启用且已配置）
        乐器服务 乐器 = 注入器.getInstance(乐器服务.class);
        if (乐器.资源包已配置()) {
            乐器.应用资源包(事件.getPlayer());
        }
    }

    @EventHandler
    public void 玩家退出(PlayerQuitEvent 事件) {
        清理奥能法师大招任务(事件.getPlayer().getUniqueId(), "玩家退出");
        效果调度服务 效果调度 = 注入器.getInstance(效果调度服务.class);
        调试日志器.调试("效果生命周期", "玩家退出清理：玩家=%s 原因=玩家退出", 事件.getPlayer().getUniqueId());
        效果调度.清空(事件.getPlayer().getUniqueId(), "玩家退出");
        生命条缩放处理器.清理玩家设置(事件.getPlayer().getUniqueId());
        玩家服务 服务 = 注入器.getInstance(玩家服务.class);
        服务.玩家退出(事件.getPlayer().getUniqueId());
        组队服务 组队 = 注入器.getInstance(组队服务.class);
        组队.玩家下线(事件.getPlayer().getUniqueId());
        驭空术服务 驭空术 = 注入器.getInstance(驭空术服务.class);
        驭空术.玩家退出清理(事件.getPlayer().getUniqueId());
        乐器服务 乐器 = 注入器.getInstance(乐器服务.class);
        乐器.玩家退出清理(事件.getPlayer().getUniqueId());
        公共冷却显示服务 公共冷却显示 = 注入器.getInstance(公共冷却显示服务.class);
        公共冷却显示.清理(事件.getPlayer().getUniqueId());
        技能冷却服务 冷却服务 = 注入器.getInstance(技能冷却服务.class);
        冷却服务.清理玩家(事件.getPlayer().getUniqueId());
    }

    @EventHandler
    public void 玩家死亡(PlayerDeathEvent 事件) {
        清理奥能法师大招任务(事件.getEntity().getUniqueId(), "玩家死亡");
    }

    @EventHandler
    public void 玩家切换世界(PlayerChangedWorldEvent 事件) {
        清理奥能法师大招任务(事件.getPlayer().getUniqueId(), "玩家切换世界");
    }

    private void 清理奥能法师大招任务(UUID 玩家标识, String 原因) {
        if (注入器 == null || 玩家标识 == null) {
            return;
        }
        技能服务 技能 = 注入器.getInstance(技能服务.class);
        技能.获取执行器("1_7")
                .filter(奥能冥想.class::isInstance)
                .map(奥能冥想.class::cast)
                .ifPresent(执行器 -> 执行器.取消玩家任务(玩家标识, 原因));
        技能.获取执行器("1_8")
                .filter(秘法湮灭.class::isInstance)
                .map(秘法湮灭.class::cast)
                .ifPresent(执行器 -> 执行器.取消玩家任务(玩家标识, 原因));
    }

    private void 清理全部奥能法师大招任务(String 原因) {
        if (注入器 == null) {
            return;
        }
        技能服务 技能 = 注入器.getInstance(技能服务.class);
        技能.获取执行器("1_7")
                .filter(奥能冥想.class::isInstance)
                .map(奥能冥想.class::cast)
                .ifPresent(执行器 -> 执行器.取消全部任务(原因));
        技能.获取执行器("1_8")
                .filter(秘法湮灭.class::isInstance)
                .map(秘法湮灭.class::cast)
                .ifPresent(执行器 -> 执行器.取消全部任务(原因));
    }
}
