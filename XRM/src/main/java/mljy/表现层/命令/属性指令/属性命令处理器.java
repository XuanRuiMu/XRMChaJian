package mljy.表现层.命令.属性指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.属性服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.属性计算服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家会话;
import mljy.表现层.命令.抽象命令处理器;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class 属性命令处理器 extends 抽象命令处理器 {
    private static final String 分隔线键 = "属性指令.界面.分隔线";
    private static final String 标题行键 = "属性指令.界面.标题行";
    private static final String 主属性标题模板键 = "属性指令.界面.主属性标题";
    private static final String 生存属性标题模板键 = "属性指令.界面.生存属性标题";
    private static final String 战斗属性标题模板键 = "属性指令.界面.战斗属性标题";
    private static final String 主属性标题键 = "属性指令.属性界面.主属性标题";
    private static final String 生存属性标题键 = "属性指令.属性界面.生存属性标题";
    private static final String 战斗属性标题键 = "属性指令.属性界面.战斗属性标题";
    private static final String 等级后缀键 = "属性指令.属性界面.等级后缀";
    private static final String 的属性键 = "属性指令.属性界面.的属性";

    private static final String 力量名键 = "属性指令.属性名.力量";
    private static final String 敏捷名键 = "属性指令.属性名.敏捷";
    private static final String 智力名键 = "属性指令.属性名.智力";
    private static final String 生命值名键 = "属性指令.属性名.生命值";
    private static final String 生命恢复名键 = "属性指令.属性名.生命恢复";
    private static final String 躲闪几率名键 = "属性指令.属性名.躲闪几率";
    private static final String 公共冷却时间名键 = "属性指令.属性名.公共冷却时间";
    private static final String 急速名键 = "属性指令.属性名.急速";
    private static final String 法术暴击几率名键 = "属性指令.属性名.法术暴击几率";
    private static final String 法术暴击伤害名键 = "属性指令.属性名.法术暴击伤害";
    private static final String 精通名键 = "属性指令.属性名.精通";
    private static final String 全能名键 = "属性指令.属性名.全能";
    private static final String 吸血名键 = "属性指令.属性名.吸血";
    private static final String 移速名键 = "属性指令.属性名.移速";

    private static final String 秒单位键 = "属性指令.单位.秒";
    private static final String 每秒单位键 = "属性指令.单位.每秒";

    private static final String 力量类别 = "力量";
    private static final String 敏捷类别 = "敏捷";
    private static final String 智力类别 = "智力";
    private static final String 生命值上限类别 = "生命值上限";
    private static final String 生命恢复类别 = "生命恢复";
    private static final String 躲闪类别 = "躲闪";
    private static final String 公共冷却时间类别 = "公共冷却时间";
    private static final String 急速类别 = "急速";
    private static final String 法术暴击几率类别 = "法术暴击几率";
    private static final String 法术暴击伤害类别 = "法术暴击伤害";
    private static final String 精通类别 = "精通";
    private static final String 全能类别 = "全能";
    private static final String 吸血类别 = "吸血";
    private static final String 移速类别 = "移速";

    private static final String 基础键 = "属性指令.构成.基础";
    private static final String 装备键 = "属性指令.构成.装备";
    private static final String 天赋键 = "属性指令.构成.天赋";
    private static final String 临时键 = "属性指令.构成.临时";
    private static final String 构成分隔符键 = "属性指令.构成.分隔符";
    private static final String 构成左括号键 = "属性指令.构成.左括号";
    private static final String 构成右括号键 = "属性指令.构成.右括号";
    private static final String 构成数值分隔符键 = "属性指令.构成.数值分隔符";

    private static final String 普通行模板键 = "属性指令.属性行.普通";
    private static final String 带单位行模板键 = "属性指令.属性行.带单位";
    private static final String 百分比行模板键 = "属性指令.属性行.百分比";
    private static final String 生命值行模板键 = "属性指令.属性行.生命值";
    private static final String 躲闪行模板键 = "属性指令.属性行.躲闪";
    private static final String 公共冷却行模板键 = "属性指令.属性行.公共冷却";
    private static final String 公共冷却构成急速键 = "属性指令.公共冷却构成.急速";
    private static final String 公共冷却构成其他键 = "属性指令.公共冷却构成.其他";

    private static final String 数值格式 = "%.1f";
    private static final double 零值 = 0.0;

    private final 玩家服务 玩家服务;
    private final 属性服务 属性服务;
    private final 属性计算服务 属性计算服务;
    private final 修饰器管理器 修饰器管理器;

    @Inject
    public 属性命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器,
                         玩家服务 玩家服务, 属性服务 属性服务, 属性计算服务 属性计算服务,
                         修饰器管理器 修饰器管理器) {
        super(翻译服务, 关键词解析器);
        this.玩家服务 = 玩家服务;
        this.属性服务 = 属性服务;
        this.属性计算服务 = 属性计算服务;
        this.修饰器管理器 = 修饰器管理器;
    }

    @Override
    public String 获取命令名() {
        return "属性";
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        调试日志器.调试("属性命令处理器", "onCommand入口：发送者=%s 标签=%s 参数长度=%d", 发送者.getName(), 标签, 参数.length);
        if (!检查玩家(发送者)) {
            return true;
        }

        Player 目标玩家;
        if (参数.length >= 1) {
            目标玩家 = Bukkit.getPlayer(参数[0]);
            if (目标玩家 == null) {
                调试日志器.调试("属性命令处理器", "目标玩家不在线：参数=%s", 参数[0]);
                发送错误(发送者, 玩家不在线键, 参数[0]);
                return true;
            }
        } else {
            目标玩家 = (Player) 发送者;
        }

        UUID 玩家标识 = 目标玩家.getUniqueId();
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        if (会话可选.isEmpty()) {
            调试日志器.调试("属性命令处理器", "会话不存在：玩家=%s", 目标玩家.getName());
            发送错误(发送者, 会话不存在键);
            return true;
        }
        玩家会话 会话 = 会话可选.get();
        属性快照 属性 = 属性服务.计算属性(玩家标识);
        属性快照 基础属性 = 属性服务.获取基础属性(玩家标识);
        调试日志器.调试("属性命令处理器", "属性计算完成：玩家=%s 力量=%.1f 智力=%.1f 生命值上限=%.1f", 目标玩家.getName(), 属性.力量(), 属性.智力(), 属性.生命值上限());

        发送属性界面(发送者, 会话, 属性, 基础属性, 目标玩家);
        return true;
    }

    private void 发送属性界面(CommandSender 发送者, 玩家会话 会话, 属性快照 属性, 属性快照 基础属性, Player 玩家) {
        UUID 玩家标识 = 玩家.getUniqueId();
        发送消息(发送者, 分隔线键);

        String 等级文本 = 会话.获取等级() + 翻译服务.获取(等级后缀键);
        String 专精文本 = 会话.获取专精();
        String 名称文本 = 会话.获取名称();
        String 的属性文本 = 翻译服务.获取(的属性键);
        发送消息(发送者, 标题行键, 等级文本, 专精文本, 名称文本, 的属性文本);

        发送分类标题(发送者, 主属性标题模板键, 主属性标题键);
        发送普通属性行(发送者, 玩家标识, 力量名键, 力量类别, 属性.力量(), 属性.基础力量());
        发送普通属性行(发送者, 玩家标识, 敏捷名键, 敏捷类别, 属性.敏捷(), 属性.基础敏捷());
        发送普通属性行(发送者, 玩家标识, 智力名键, 智力类别, 属性.智力(), 属性.基础智力());

        发送分类标题(发送者, 生存属性标题模板键, 生存属性标题键);
        发送生命值属性行(发送者, 玩家标识, 生命值名键, 生命值上限类别, 玩家.getHealth(), 属性.生命值上限(), 基础属性.生命值上限());
        发送带单位属性行(发送者, 玩家标识, 带单位行模板键, 生命恢复名键, 生命恢复类别, 属性.生命恢复(), 基础属性.生命恢复(), 每秒单位键);
        发送躲闪属性行(发送者, 玩家标识, 躲闪几率名键, 躲闪类别, 属性.躲闪(), 基础属性.躲闪());

        发送分类标题(发送者, 战斗属性标题模板键, 战斗属性标题键);
        double 实际公共冷却 = 属性计算服务.计算实际公共冷却(属性.公共冷却时间(), 属性.急速());
        发送公共冷却行(发送者, 玩家标识, 公共冷却时间名键, 公共冷却行模板键, 实际公共冷却, 基础属性.公共冷却时间(), 属性.公共冷却时间(), 属性.急速());
        发送普通属性行(发送者, 玩家标识, 急速名键, 急速类别, 属性.急速(), 基础属性.急速());
        发送百分比属性行(发送者, 玩家标识, 法术暴击几率名键, 法术暴击几率类别, 属性.法术暴击几率(), 基础属性.法术暴击几率());
        发送百分比属性行(发送者, 玩家标识, 法术暴击伤害名键, 法术暴击伤害类别, 属性.法术暴击伤害(), 基础属性.法术暴击伤害());
        发送百分比属性行(发送者, 玩家标识, 精通名键, 精通类别, 属性.精通(), 基础属性.精通());
        发送百分比属性行(发送者, 玩家标识, 全能名键, 全能类别, 属性.全能(), 基础属性.全能());
        发送百分比属性行(发送者, 玩家标识, 吸血名键, 吸血类别, 属性.吸血(), 基础属性.吸血());
        发送普通属性行(发送者, 玩家标识, 移速名键, 移速类别, 属性.移速(), 基础属性.移速());
    }

    private void 发送分类标题(CommandSender 发送者, String 模板键, String 文本键) {
        String 标题文本 = 翻译服务.获取(文本键);
        发送消息(发送者, 模板键, 标题文本);
    }

    private void 发送普通属性行(CommandSender 发送者, UUID 玩家标识, String 属性名键, String 属性类别, double 数值, double 基础值) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 数值文本 = 格式化属性值(属性类别, 数值);
        String 构成文本 = 构建构成文本(玩家标识, 属性类别, 基础值);
        发送消息(发送者, 普通行模板键, 属性名, 数值文本, 构成文本);
    }

    private void 发送带单位属性行(CommandSender 发送者, UUID 玩家标识, String 模板键, String 属性名键, String 属性类别, double 数值, double 基础值, String 单位键) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 数值文本 = String.format(数值格式, 数值);
        String 单位 = 翻译服务.获取(单位键);
        String 构成文本 = 构建构成文本(玩家标识, 属性类别, 基础值);
        发送消息(发送者, 模板键, 属性名, 数值文本, 单位, 构成文本);
    }

    private void 发送百分比属性行(CommandSender 发送者, UUID 玩家标识, String 属性名键, String 属性类别, double 数值, double 基础值) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 数值文本 = String.format(数值格式, 数值);
        String 构成文本 = 构建构成文本(玩家标识, 属性类别, 基础值);
        发送消息(发送者, 百分比行模板键, 属性名, 数值文本, 构成文本);
    }

    private void 发送生命值属性行(CommandSender 发送者, UUID 玩家标识, String 属性名键, String 属性类别, double 当前值, double 最大值, double 基础值) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 当前文本 = String.format(数值格式, 当前值);
        String 最大文本 = String.format(数值格式, 最大值);
        String 构成文本 = 构建构成文本(玩家标识, 属性类别, 基础值);
        发送消息(发送者, 生命值行模板键, 属性名, 当前文本, 最大文本, 构成文本);
    }

    private void 发送躲闪属性行(CommandSender 发送者, UUID 玩家标识, String 属性名键, String 属性类别, double 数值, double 基础值) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 数值文本 = String.format(数值格式, 数值);
        String 构成文本 = 构建构成文本(玩家标识, 属性类别, 基础值);
        发送消息(发送者, 躲闪行模板键, 属性名, 数值文本, 构成文本);
    }

    private void 发送公共冷却行(CommandSender 发送者, UUID 玩家标识, String 属性名键, String 模板键, double 实际公共冷却, double 未修饰基础公共冷却, double 修饰后基础公共冷却, double 急速) {
        String 属性名 = 翻译服务.获取(属性名键);
        String 实际文本 = String.format(数值格式, 实际公共冷却);
        String 单位 = 翻译服务.获取(秒单位键);
        double 急速减少秒数 = 修饰后基础公共冷却 - 实际公共冷却;
        String 基础文本 = String.format(数值格式, 未修饰基础公共冷却);
        String 急速减少文本 = 格式化修正值(-急速减少秒数);
        double 临时加成 = 计算临时加成(玩家标识, 公共冷却时间类别);
        String 其他修正文本 = 格式化修正值(临时加成);
        String 基础标签 = 翻译服务.获取(基础键);
        String 急速标签 = 翻译服务.获取(公共冷却构成急速键);
        String 其他标签 = 翻译服务.获取(公共冷却构成其他键);
        String 左括号 = 翻译服务.获取(构成左括号键);
        String 右括号 = 翻译服务.获取(构成右括号键);
        String 分隔符 = 翻译服务.获取(构成分隔符键);
        String 数值分隔符 = 翻译服务.获取(构成数值分隔符键);
        String 构成文本 = 左括号 + 基础标签 + 数值分隔符 + 基础文本 + 单位 +
                分隔符 + 急速标签 + 数值分隔符 + 急速减少文本 + 单位 +
                分隔符 + 其他标签 + 数值分隔符 + 其他修正文本 + 单位 + 右括号;
        发送消息(发送者, 模板键, 属性名, 实际文本, 单位, 构成文本);
    }

    private String 格式化属性值(String 属性类别, double 数值) {
        if (移速类别.equals(属性类别)) {
            return String.format("%.2f", 数值);
        }
        return String.format(数值格式, 数值);
    }

    private String 格式化修正值(double 值) {
        if (Math.abs(值) < 0.05) {
            return String.format("%.1f", 0.0);
        }
        return String.format("%+.1f", 值);
    }

    private String 构建构成文本(UUID 玩家标识, String 属性类别, double 基础值) {
        String 基础 = 翻译服务.获取(基础键);
        String 装备 = 翻译服务.获取(装备键);
        String 天赋 = 翻译服务.获取(天赋键);
        String 临时 = 翻译服务.获取(临时键);
        String 分隔符 = 翻译服务.获取(构成分隔符键);
        String 左括号 = 翻译服务.获取(构成左括号键);
        String 右括号 = 翻译服务.获取(构成右括号键);
        String 数值分隔符 = 翻译服务.获取(构成数值分隔符键);
        double 临时加成 = 计算临时加成(玩家标识, 属性类别);
        调试日志器.调试("属性命令处理器", "构建构成文本：玩家=%s 类别=%s 基础值=%.1f 临时加成=%.1f",
                玩家标识, 属性类别, 基础值, 临时加成);
        return 左括号 + 基础 + 数值分隔符 + 格式化属性值(属性类别, 基础值) +
                分隔符 + 装备 + 数值分隔符 + 格式化属性值(属性类别, 零值) +
                分隔符 + 天赋 + 数值分隔符 + 格式化属性值(属性类别, 零值) +
                分隔符 + 临时 + 数值分隔符 + 格式化属性值(属性类别, 临时加成) + 右括号;
    }

    private double 计算临时加成(UUID 玩家标识, String 属性类别) {
        List<修饰器> 修饰器列表 = 修饰器管理器.查询修饰器(玩家标识, 属性类别);
        double 加成总和 = 0.0;
        for (修饰器 项 : 修饰器列表) {
            加成总和 += 项.数值();
        }
        return 加成总和;
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        return Collections.emptyList();
    }
}
