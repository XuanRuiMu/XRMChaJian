package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class 任务菜单 extends 菜单框架 {
    private static final String 菜单标识 = "任务菜单";
    private static final int 菜单大小 = 54;
    private static final int 关闭按钮槽位 = 49;
    private static final int 主线任务槽位 = 11;
    private static final int 支线任务槽位 = 13;
    private static final int 日常任务槽位 = 15;
    private static final String 标题键 = "任务菜单.标题";
    private static final String 主线任务键 = "任务菜单.分类.主线任务";
    private static final String 主线任务描述键 = "任务菜单.分类.主线任务描述";
    private static final String 支线任务键 = "任务菜单.分类.支线任务";
    private static final String 支线任务描述键 = "任务菜单.分类.支线任务描述";
    private static final String 日常任务键 = "任务菜单.分类.日常任务";
    private static final String 日常任务描述键 = "任务菜单.分类.日常任务描述";
    private static final String 未解锁状态键 = "任务菜单.状态.未解锁";
    private static final String 关闭文本键 = "菜单框架.通用.关闭";

    @Inject
    public 任务菜单(翻译服务 翻译服务,
                   关键词解析器 关键词解析器,
                   菜单占位符 占位符,
                   菜单条件 条件,
                   Provider<菜单服务> 菜单服务提供器) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
    }

    @Override
    public String 获取菜单标识() {
        return 菜单标识;
    }

    @Override
    public 菜单配置 构建配置(Player 玩家) {
        String 标题 = 翻译服务.获取(标题键);
        List<菜单按钮> 按钮 = new ArrayList<>();
        按钮.add(构建分类按钮(主线任务槽位, "BOOK", 主线任务键, 主线任务描述键));
        按钮.add(构建分类按钮(支线任务槽位, "WRITABLE_BOOK", 支线任务键, 支线任务描述键));
        按钮.add(构建分类按钮(日常任务槽位, "MAP", 日常任务键, 日常任务描述键));
        按钮.add(构建关闭按钮());
        return 菜单配置.of(标题, 菜单大小).with按钮(按钮);
    }

    private 菜单按钮 构建分类按钮(int 槽位, String 物品, String 名称键, String 描述键) {
        String 名称 = 翻译服务.获取(名称键);
        List<String> 描述 = List.of(翻译服务.获取(描述键), 翻译服务.获取(未解锁状态键));
        return 菜单按钮.of(槽位, 物品, 名称)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.发送消息("任务菜单.状态.未解锁")));
    }

    private 菜单按钮 构建关闭按钮() {
        return 菜单按钮.of(关闭按钮槽位, "BARRIER", 翻译服务.获取(关闭文本键))
                .with动作(Map.of("左键", 菜单动作.关闭菜单()));
    }
}
