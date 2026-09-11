package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.技能注册服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.技能.技能定义;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class 技能菜单 extends 菜单框架 {
    private static final String 菜单标识 = "技能菜单";
    private static final int 菜单大小 = 54;
    private static final int 关闭按钮槽位 = 49;
    private static final String 标题键 = "技能指令.菜单标题";
    private static final String 关闭文本键 = "技能指令.通用.关闭";
    private static final String 按键标签键 = "技能指令.按键标签";
    private static final String 冷却时间键 = "技能指令.冷却时间";
    private static final String 蓄力时间键 = "技能指令.蓄力时间";
    private static final String 引导时间键 = "技能指令.引导时间";
    private static final String 数值格式 = "%.1f";
    private static final String 默认专精 = "奥能法师";

    private final 技能注册服务 技能注册服务;
    private final 玩家服务 玩家服务;

    @Inject
    public 技能菜单(翻译服务 翻译服务,
                   关键词解析器 关键词解析器,
                   菜单占位符 占位符,
                   菜单条件 条件,
                   Provider<菜单服务> 菜单服务提供器,
                   技能注册服务 技能注册服务,
                   玩家服务 玩家服务) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
        this.技能注册服务 = 技能注册服务;
        this.玩家服务 = 玩家服务;
    }

    @Override
    public String 获取菜单标识() {
        return 菜单标识;
    }

    @Override
    public 菜单配置 构建配置(Player 玩家) {
        String 专精 = 获取玩家专精(玩家);
        String 标题 = 翻译服务.获取(标题键, 专精, 玩家.getName());
        List<菜单按钮> 按钮 = new ArrayList<>();
        按钮.addAll(构建技能按钮(专精));
        按钮.add(构建关闭按钮());
        return 菜单配置.of(标题, 菜单大小).with按钮(按钮);
    }

    private List<菜单按钮> 构建技能按钮(String 专精) {
        List<菜单按钮> 按钮 = new ArrayList<>();
        List<String> 技能标识列表 = 获取技能标识列表(专精);
        for (int i = 0; i < 技能标识列表.size() && i < 菜单大小 - 1; i++) {
            String 技能标识 = 技能标识列表.get(i);
            Optional<技能定义> 定义 = 技能注册服务.获取定义(技能标识);
            if (定义.isEmpty()) {
                continue;
            }
            按钮.add(构建单个技能按钮(i, 定义.get()));
        }
        return 按钮;
    }

    private 菜单按钮 构建单个技能按钮(int 槽位, 技能定义 定义) {
        String 显示名 = "{技能名:" + 定义.技能标识() + "}";
        List<String> 描述 = new ArrayList<>();
        描述.add(翻译服务.获取(按键标签键));
        if (定义.冷却时间() > 0) {
            描述.add(翻译服务.获取(冷却时间键, String.format(数值格式, 定义.冷却时间())));
        }
        if (定义.蓄力时间() > 0) {
            描述.add(翻译服务.获取(蓄力时间键, String.format(数值格式, 定义.蓄力时间())));
        }
        if (定义.引导时间() > 0) {
            描述.add(翻译服务.获取(引导时间键, String.format(数值格式, 定义.引导时间())));
        }
        return 菜单按钮.of(槽位, "BLAZE_ROD", 显示名)
                .with描述(描述)
                .with动作(Map.of("左键", 菜单动作.关闭菜单()));
    }

    private 菜单按钮 构建关闭按钮() {
        return 菜单按钮.of(关闭按钮槽位, "BARRIER", "[错误]" + 翻译服务.获取(关闭文本键))
                .with动作(Map.of("左键", 菜单动作.关闭菜单()));
    }

    private List<String> 获取技能标识列表(String 专精) {
        List<String> 标识列表 = new ArrayList<>();
        if (默认专精.equals(专精)) {
            标识列表.addAll(List.of("1_1", "1_2", "1_3", "1_4", "1_5", "1_6", "1_7", "1_8", "1_9"));
        }
        return 标识列表;
    }

    private String 获取玩家专精(Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        return 会话.map(玩家会话::获取专精).orElse(默认专精);
    }
}
