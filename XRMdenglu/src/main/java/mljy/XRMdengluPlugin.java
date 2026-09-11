package mljy;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 英文包入口类：满足 plugin.yml 的命名规范。
 * 委托给中文主类处理所有逻辑。
 */
public final class XRMdengluPlugin extends JavaPlugin {

    private 暮澜纪元.登录服插件 中文插件实例;

    @Override
    public void onLoad() {
        暮澜纪元.登录服插件.设置外部实例(this);
    }

    @Override
    public void onEnable() {
        中文插件实例 = new 暮澜纪元.登录服插件();
        中文插件实例.启动(this);
    }

    @Override
    public void onDisable() {
        if (中文插件实例 != null) {
            中文插件实例.关闭();
        }
    }
}
