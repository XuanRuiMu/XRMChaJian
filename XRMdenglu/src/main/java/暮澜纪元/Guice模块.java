package 暮澜纪元;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import 暮澜纪元.通用.文本.关键词解析器;
import 暮澜纪元.通用.文本.技能名解析器;
import 暮澜纪元.通用.文本.文本格式化器;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.配置.配置管理器;

import java.util.Optional;

/**
 * XRMdenglu 的 Guice 注入模块。
 * 绑定公共模块接口（文本格式化器链）和 XRMdenglu 自己的服务（消息/配置管理器）。
 */
public class Guice模块 extends AbstractModule {

    private final 登录服插件 插件;

    public Guice模块(登录服插件 插件) {
        this.插件 = 插件;
    }

    @Override
    protected void configure() {
        bind(登录服插件.class).toInstance(插件);
        bind(JavaPlugin.class).toInstance(插件.获取JavaPlugin());

        bind(技能名解析器.class).toInstance((槽位中文数字, 玩家标识) -> Optional.empty());
        bind(关键词解析器.class).in(Singleton.class);
        bind(文本格式化器.class).in(Singleton.class);

        bind(配置管理器.class).in(Singleton.class);
        bind(消息管理器.class).in(Singleton.class);
    }
}
