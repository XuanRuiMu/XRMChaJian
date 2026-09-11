package 暮澜纪元.配置;

import 暮澜纪元.传送门区域;
import 暮澜纪元.登录服插件;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录服配置管理器。
 * 管理传送门和重生点配置。
 */
public class 登录服配置 {

    private final 登录服插件 插件;
    private final Map<String, 传送门区域> 传送门列表 = new HashMap<>();
    private 重生点配置 重生点;
    private boolean 调试模式;
    private String 目标服务器 = "mmorpg";

    public 登录服配置(登录服插件 插件) {
        this.插件 = 插件;
    }

    /**
     * 从配置文件加载所有配置。
     */
    public void 加载配置() {
        this.调试模式 = 插件.getConfig().getBoolean("调试模式", false);
        this.传送门列表.clear();

        // 加载传送门配置
        if (插件.getConfig().contains("传送门")) {
            for (String 传送门ID : 插件.getConfig().getConfigurationSection("传送门").getKeys(false)) {
                String 路径 = "传送门." + 传送门ID;
                String 世界名 = 插件.getConfig().getString(路径 + ".世界");
                int 最小X = 插件.getConfig().getInt(路径 + ".最小X");
                int 最小Y = 插件.getConfig().getInt(路径 + ".最小Y");
                int 最小Z = 插件.getConfig().getInt(路径 + ".最小Z");
                int 最大X = 插件.getConfig().getInt(路径 + ".最大X");
                int 最大Y = 插件.getConfig().getInt(路径 + ".最大Y");
                int 最大Z = 插件.getConfig().getInt(路径 + ".最大Z");
                String 目标服务器 = 插件.getConfig().getString(路径 + ".目标服务器");
                String 提示消息 = 插件.getConfig().getString(路径 + ".提示消息", "&a正在传送...");

                传送门区域 传送门 = new 传送门区域(
                    传送门ID, 世界名, 最小X, 最小Y, 最小Z,
                    最大X, 最大Y, 最大Z, 目标服务器, 提示消息
                );
                this.传送门列表.put(传送门ID, 传送门);
                插件.获取消息管理器().系统日志("已加载传送门: " + 传送门ID + " -> " + 目标服务器);
            }
        }

        // 加载重生点配置
        if (插件.getConfig().contains("重生点")) {
            String 世界名 = 插件.getConfig().getString("重生点.世界", "login_world");
            double 坐标X = 插件.getConfig().getDouble("重生点.X", 0.0);
            double 坐标Y = 插件.getConfig().getDouble("重生点.Y", 60.0);
            double 坐标Z = 插件.getConfig().getDouble("重生点.Z", 0.0);
            float 偏航角 = (float) 插件.getConfig().getDouble("重生点.偏航", 0.0);
            float 俯仰角 = (float) 插件.getConfig().getDouble("重生点.俯仰", 0.0);

            this.重生点 = new 重生点配置(世界名, 坐标X, 坐标Y, 坐标Z, 偏航角, 俯仰角);
            插件.获取消息管理器().系统日志("已加载重生点: " + 世界名 + " (" + 坐标X + ", " + 坐标Y + ", " + 坐标Z + ")");
        }

        // 加载目标服务器配置
        this.目标服务器 = 插件.getConfig().getString("目标服务器", "mmorpg");
    }

    /**
     * 获取目标服务器名称。
     */
    public String 获取目标服务器() {
        return this.目标服务器;
    }

    /**
     * 获取所有传送门列表。
     */
    public Map<String, 传送门区域> 获取传送门列表() {
        return this.传送门列表;
    }

    /**
     * 获取重生点配置。
     */
    public 重生点配置 获取重生点() {
        return this.重生点;
    }

    /**
     * 是否开启调试模式。
     */
    public boolean 是否调试模式() {
        return this.调试模式;
    }

    /**
     * 获取指定传送门区域。
     *
     * @param 传送门ID 传送门标识
     * @return 传送门区域，如果不存在返回null
     */
    public 传送门区域 获取传送门区域(String 传送门ID) {
        return this.传送门列表.get(传送门ID);
    }

    /**
     * 设置传送门区域。
     *
     * @param 传送门ID 传送门标识
     * @param 区域 传送门区域
     */
    public void 设置传送门区域(String 传送门ID, 传送门区域 区域) {
        this.传送门列表.put(传送门ID, 区域);

        // 同步到Bukkit配置
        String 路径 = "传送门." + 传送门ID;
        插件.getConfig().set(路径 + ".世界", 区域.获取世界名());
        插件.getConfig().set(路径 + ".最小X", 区域.获取最小X());
        插件.getConfig().set(路径 + ".最小Y", 区域.获取最小Y());
        插件.getConfig().set(路径 + ".最小Z", 区域.获取最小Z());
        插件.getConfig().set(路径 + ".最大X", 区域.获取最大X());
        插件.getConfig().set(路径 + ".最大Y", 区域.获取最大Y());
        插件.getConfig().set(路径 + ".最大Z", 区域.获取最大Z());
        插件.getConfig().set(路径 + ".目标服务器", 区域.获取目标服务器());
    }

    /**
     * 移除传送门区域。
     *
     * @param 传送门ID 传送门标识
     */
    public void 移除传送门区域(String 传送门ID) {
        this.传送门列表.remove(传送门ID);
        插件.getConfig().set("传送门." + 传送门ID, null);
    }

    /**
     * 保存配置到文件。
     */
    public void 保存配置() {
        插件.saveConfig();
    }

    /**
     * 设置重生点配置。
     *
     * @param 世界名 世界名称
     * @param 坐标X X坐标
     * @param 坐标Y Y坐标
     * @param 坐标Z Z坐标
     * @param 偏航角 偏航角度
     * @param 俯仰角 俯仰角度
     */
    public void 设置重生点(String 世界名, double 坐标X, double 坐标Y, double 坐标Z, float 偏航角, float 俯仰角) {
        this.重生点 = new 重生点配置(世界名, 坐标X, 坐标Y, 坐标Z, 偏航角, 俯仰角);

        // 同步到Bukkit配置
        插件.getConfig().set("重生点.世界", 世界名);
        插件.getConfig().set("重生点.X", 坐标X);
        插件.getConfig().set("重生点.Y", 坐标Y);
        插件.getConfig().set("重生点.Z", 坐标Z);
        插件.getConfig().set("重生点.偏航", 偏航角);
        插件.getConfig().set("重生点.俯仰", 俯仰角);
    }

    /**
     * 重载所有配置。
     */
    public void 重载() {
        插件.获取JavaPlugin().reloadConfig();
        加载配置();
        插件.获取消息管理器().系统日志("配置已重载");
    }
}
