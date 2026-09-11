package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import mljy.表现层.命令.玩家指令.技能日志命令处理器;
import mljy.表现层.命令.玩家指令.战斗日志命令处理器;
import mljy.基础设施层.调试日志器;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class 总菜单 extends 菜单框架 {
    private static final String 菜单标识 = "总菜单";
    private static final String 任务菜单标识 = "任务菜单";
    private static final String 标题键 = "总菜单.标题";
    private static final String 任务系统名称键 = "总菜单.任务系统.名称";
    private static final String 任务系统描述键 = "总菜单.任务系统.描述";
    private static final String 任务系统描述2键 = "总菜单.任务系统.描述2";
    private static final String 属性系统名称键 = "总菜单.属性系统.名称";
    private static final String 属性系统描述键 = "总菜单.属性系统.描述";
    private static final String 技能日志名称键 = "总菜单.技能日志设置.名称";
    private static final String 技能日志描述键 = "总菜单.技能日志设置.描述";
    private static final String 战斗日志名称键 = "总菜单.战斗日志设置.名称";
    private static final String 战斗日志描述键 = "总菜单.战斗日志设置.描述";
    private static final String 状态行键 = "总菜单.状态行";
    private static final String 开启键 = "总菜单.状态.开启";
    private static final String 关闭键 = "总菜单.状态.关闭";

    private final 玩家服务 玩家服务;
    private final 技能日志命令处理器 技能日志处理器;
    private final 战斗日志命令处理器 战斗日志处理器;
    private final 总菜单配置 配置;

    @Inject
    public 总菜单(翻译服务 翻译服务,
                  关键词解析器 关键词解析器,
                  菜单占位符 占位符,
                  菜单条件 条件,
                  Provider<菜单服务> 菜单服务提供器,
                  玩家服务 玩家服务,
                  技能日志命令处理器 技能日志处理器,
                  战斗日志命令处理器 战斗日志处理器,
                  总菜单配置 配置) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
        this.玩家服务 = 玩家服务;
        this.技能日志处理器 = 技能日志处理器;
        this.战斗日志处理器 = 战斗日志处理器;
        this.配置 = 配置;
    }

    @Override
    public String 获取菜单标识() {
        return 菜单标识;
    }

    @Override
    public 菜单配置 构建配置(Player 玩家) {
        String 标题 = 翻译服务.获取(标题键);
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家.getUniqueId());
        boolean 技能日志开启 = 会话可选.map(玩家会话::获取技能日志开关).orElse(false);
        boolean 战斗日志开启 = 会话可选.map(玩家会话::获取战斗日志开关).orElse(false);
        调试日志器.调试("总菜单", "构建配置 玩家=%s 技能日志=%b 战斗日志=%b",
                玩家.getName(), 技能日志开启, 战斗日志开启);
        List<菜单按钮> 按钮 = new ArrayList<>();
        按钮.add(构建任务系统按钮());
        按钮.add(构建属性系统按钮());
        按钮.add(构建技能日志按钮(技能日志开启));
        按钮.add(构建战斗日志按钮(战斗日志开启));
        return 菜单配置.of(标题, 配置.菜单大小()).with按钮(按钮);
    }

    @Override
    public boolean 处理点击(Player 玩家, int 槽位, String 点击类型) {
        boolean 结果 = super.处理点击(玩家, 槽位, 点击类型);
        if (!结果) {
            return false;
        }
        if (槽位 == 配置.技能日志槽位()) {
            切换技能日志(玩家);
        } else if (槽位 == 配置.战斗日志槽位()) {
            切换战斗日志(玩家);
        } else if (槽位 == 配置.属性系统槽位()) {
            玩家.performCommand(配置.属性命令());
        }
        return true;
    }

    private 菜单按钮 构建任务系统按钮() {
        String 名称 = 翻译服务.获取(任务系统名称键);
        List<String> 描述 = List.of(
                翻译服务.获取(任务系统描述键),
                翻译服务.获取(任务系统描述2键));
        return 菜单按钮.of(配置.任务系统槽位(), 配置.任务系统物品(), 名称)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.打开菜单(任务菜单标识)));
    }

    private 菜单按钮 构建属性系统按钮() {
        String 名称 = 翻译服务.获取(属性系统名称键);
        List<String> 描述 = List.of(翻译服务.获取(属性系统描述键));
        return 菜单按钮.of(配置.属性系统槽位(), 配置.属性系统物品(), 名称)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.关闭菜单()));
    }

    private 菜单按钮 构建技能日志按钮(boolean 开启) {
        String 名称 = 翻译服务.获取(技能日志名称键);
        String 状态文本 = 翻译服务.获取(开启 ? 开启键 : 关闭键);
        String 状态行 = 翻译服务.获取(状态行键, 状态文本);
        List<String> 描述 = List.of(翻译服务.获取(技能日志描述键), 状态行);
        return 菜单按钮.of(配置.技能日志槽位(), 配置.技能日志物品(), 名称)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.关闭菜单()))
                .with附魔光效(开启);
    }

    private 菜单按钮 构建战斗日志按钮(boolean 开启) {
        String 名称 = 翻译服务.获取(战斗日志名称键);
        String 状态文本 = 翻译服务.获取(开启 ? 开启键 : 关闭键);
        String 状态行 = 翻译服务.获取(状态行键, 状态文本);
        List<String> 描述 = List.of(翻译服务.获取(战斗日志描述键), 状态行);
        return 菜单按钮.of(配置.战斗日志槽位(), 配置.战斗日志物品(), 名称)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.关闭菜单()))
                .with附魔光效(开启);
    }

    private void 切换技能日志(Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        if (会话.isEmpty()) {
            调试日志器.调试("总菜单", "切换技能日志失败：会话不存在 玩家=%s", 玩家.getName());
            return;
        }
        boolean 新状态 = !会话.get().获取技能日志开关();
        调试日志器.调试("总菜单", "切换技能日志 玩家=%s 新状态=%b", 玩家.getName(), 新状态);
        技能日志处理器.切换开关(玩家, 新状态);
    }

    private void 切换战斗日志(Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        if (会话.isEmpty()) {
            调试日志器.调试("总菜单", "切换战斗日志失败：会话不存在 玩家=%s", 玩家.getName());
            return;
        }
        boolean 新状态 = !会话.get().获取战斗日志开关();
        调试日志器.调试("总菜单", "切换战斗日志 玩家=%s 新状态=%b", 玩家.getName(), 新状态);
        战斗日志处理器.切换开关(玩家, 新状态);
    }
}
