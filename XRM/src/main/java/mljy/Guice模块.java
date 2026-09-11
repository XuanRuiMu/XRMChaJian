package mljy;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import mljy.业务层.*;
import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.业务层.消息.伤害日志格式化服务;
import mljy.业务层.消息.伤害日志格式化服务实现;
import mljy.业务层.额外功能.额外功能配置服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.基础设施层.数据库.数据库连接池;
import mljy.基础设施层.数据库.表初始化器;
import mljy.基础设施层.数据库.生命条缩放设置存储;
import mljy.表现层.菜单.*;
import 暮澜纪元.通用.文本.关键词解析器;
import 暮澜纪元.通用.文本.技能名解析器;
import mljy.表现层.计分板.计分板服务;
import mljy.表现层.计分板.计分板服务实现;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.技能.参数注册表;
import mljy.业务层.乐器.节拍器;
import mljy.业务层.乐器.公共乐谱服务;
import mljy.基础设施层.乐器.公共乐谱库仓储;
import mljy.基础设施层.乐器.资源包管理器;
import mljy.基础设施层.乐器.mod输入通道;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.驭空术.驭空术配置;
import mljy.基础设施层.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import mljy.业务层.乐器服务;
import mljy.表现层.监听器.技能移动打断监听器;

public class Guice模块 extends AbstractModule {
    private final JavaPlugin 插件;

    public Guice模块(JavaPlugin 插件) {
        this.插件 = 插件;
    }

    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(插件);
        bind(Plugin.class).toInstance(插件);
        bind(事件总线.class).to(内存事件总线.class).in(Singleton.class);
        bind(Yaml翻译加载器.class).in(Singleton.class);
        bind(修饰器管理器.class).in(Singleton.class);
        bind(翻译服务.class).to(内存翻译服务.class).in(Singleton.class);
        bind(数据服务.class).to(Yaml数据服务.class).in(Singleton.class);
        bind(玩家服务.class).to(内存玩家服务.class).in(Singleton.class);
        bind(资源注册服务.class).to(资源注册服务实现.class).in(Singleton.class);
        bind(资源服务.class).to(内存资源服务.class).in(Singleton.class);
        bind(效果注册服务.class).to(效果注册服务实现.class).in(Singleton.class);
        bind(效果服务.class).to(内存效果服务.class).in(Singleton.class);
        bind(天赋服务.class).to(内存天赋服务.class).in(Singleton.class);
        bind(属性服务.class).to(内存属性服务.class).in(Singleton.class);
        bind(战斗服务.class).to(内存战斗服务.class).in(Singleton.class);
        bind(技能服务.class).to(内存技能服务.class).in(Singleton.class);
        bind(内存技能服务.class).in(Singleton.class);
        bind(扫描技能注册表.class).in(Singleton.class);
        bind(属性注册表.class).in(Singleton.class);
        bind(参数注册表.class).in(Singleton.class);

        bind(消息服务.class).to(消息服务实现.class).in(Singleton.class);
        bind(伤害日志格式化服务.class).to(伤害日志格式化服务实现.class).in(Singleton.class);
        bind(目标选择服务.class).to(目标选择服务实现.class).in(Singleton.class);
        bind(伤害计算服务.class).to(伤害计算服务实现.class).in(Singleton.class);
        bind(属性计算服务.class).to(属性计算服务实现.class).in(Singleton.class);
        bind(资源变更服务.class).to(资源变更服务实现.class).in(Singleton.class);
        bind(效果调度服务.class).to(效果调度服务实现.class).in(Singleton.class);
        bind(mljy.技能实现.奥能法师.奥术护盾效果处理器.class).in(Singleton.class);
        bind(技能注册服务.class).to(技能注册服务实现.class).in(Singleton.class);
        bind(技能冷却服务.class).to(技能冷却服务实现.class).in(Singleton.class);
        bind(技能释放服务.class).to(技能释放服务实现.class).in(Singleton.class);
        bind(关键词解析器.class).in(Singleton.class);
        bind(技能名解析器.class).to(XRM技能名解析器.class).in(Singleton.class);
        bind(日志合并管理器.class).in(Singleton.class);
        bind(战斗日志上下文管理器.class).in(Singleton.class);
        bind(职业类型服务.class).to(职业类型服务实现.class).in(Singleton.class);
        bind(仇恨服务.class).to(仇恨服务实现.class).in(Singleton.class);
        bind(战斗状态服务.class).to(战斗状态服务实现.class).in(Singleton.class);
        bind(吸血处理服务.class).to(吸血处理服务实现.class).in(Singleton.class);
        bind(额外功能配置服务.class).in(Singleton.class);
        bind(菜单服务.class).to(菜单服务实现.class).in(Singleton.class);
        bind(菜单占位符.class).in(Singleton.class);
        bind(菜单条件.class).in(Singleton.class);
        bind(菜单配置加载器.class).in(Singleton.class);
        bind(技能菜单.class).in(Singleton.class);
        bind(天赋菜单.class).in(Singleton.class);
        bind(任务菜单.class).in(Singleton.class);
        bind(总菜单.class).in(Singleton.class);
        bind(静态菜单.class).in(Singleton.class);
        bind(菜单监听器.class).in(Singleton.class);
        bind(总菜单配置.class).in(Singleton.class);

        bind(计分板服务.class).to(计分板服务实现.class).in(Singleton.class);
        bind(天赋注册服务.class).to(天赋注册服务实现.class).in(Singleton.class);
        bind(天赋管理服务.class).to(天赋管理服务实现.class).in(Singleton.class);
        bind(任务注册服务.class).to(任务注册服务实现.class).in(Singleton.class);
        bind(任务管理服务.class).to(任务管理服务实现.class).in(Singleton.class);
        bind(组队服务.class).to(组队服务实现.class).in(Singleton.class);
        bind(驭空术配置.class).in(Singleton.class);
        bind(驭空术服务.class).to(驭空术服务实现.class).in(Singleton.class);
        bind(乐器服务.class).to(乐器服务实现.class).in(Singleton.class);
        bind(乐器注册表.class).in(Singleton.class);
        bind(节拍器.class).in(Singleton.class);
        bind(公共乐谱库仓储.class).in(Singleton.class);
        bind(公共乐谱服务.class).in(Singleton.class);
        bind(资源包管理器.class).in(Singleton.class);
        bind(mod输入通道.class).in(Singleton.class);
        bind(公共冷却显示服务.class).to(公共冷却显示服务实现.class).in(Singleton.class);
        bind(技能移动打断监听器.class).in(Singleton.class);
        bind(数据库连接池.class).in(Singleton.class);
        bind(表初始化器.class).in(Singleton.class);
        bind(生命条缩放设置存储.class).in(Singleton.class);
    }
}
