package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import org.bukkit.entity.Player;

public class 静态菜单 extends 菜单框架 {
    private final 菜单配置加载器 配置加载器;
    private final String 菜单标识;
    private final String 配置文件名;

    @Inject
    public 静态菜单(翻译服务 翻译服务,
                  关键词解析器 关键词解析器,
                  菜单占位符 占位符,
                  菜单条件 条件,
                  Provider<菜单服务> 菜单服务提供器,
                  菜单配置加载器 配置加载器) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
        this.配置加载器 = 配置加载器;
        this.菜单标识 = "主菜单";
        this.配置文件名 = "主菜单.yml";
    }

    public 静态菜单(翻译服务 翻译服务,
                  关键词解析器 关键词解析器,
                  菜单占位符 占位符,
                  菜单条件 条件,
                  Provider<菜单服务> 菜单服务提供器,
                  菜单配置加载器 配置加载器,
                  String 菜单标识,
                  String 配置文件名) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
        this.配置加载器 = 配置加载器;
        this.菜单标识 = 菜单标识;
        this.配置文件名 = 配置文件名;
    }

    @Override
    public String 获取菜单标识() {
        return 菜单标识;
    }

    @Override
    public 菜单配置 构建配置(Player 玩家) {
        return 配置加载器.加载(配置文件名);
    }
}
