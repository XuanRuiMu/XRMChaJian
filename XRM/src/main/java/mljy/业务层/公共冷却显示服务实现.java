package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.玩家服务;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class 公共冷却显示服务实现 implements 公共冷却显示服务 {
    private static final long 更新间隔tick = 1L;
    private static final double 三分之二 = 2.0 / 3.0;
    private static final double 三分之一 = 1.0 / 3.0;
    private static final double 显示数值倍数 = 10.0;
    private static final double 最小有效剩余 = 0.0;
    private static final double 进度更新阈值 = 0.01;
    private static final String 红色阶段键 = "公CD显示管理器.标题.红色阶段";
    private static final String 黄色阶段键 = "公CD显示管理器.标题.黄色阶段";
    private static final String 绿色阶段键 = "公CD显示管理器.标题.绿色阶段";
    private static final String 空闲阶段键 = "公CD显示管理器.标题.空闲阶段";

    private final JavaPlugin 插件;
    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final 玩家服务 玩家服务;
    private final 技能冷却服务 技能冷却服务;
    private final 属性计算服务 属性计算服务;
    private final Map<UUID, 显示状态> 状态表 = new ConcurrentHashMap<>();

    @Inject
    public 公共冷却显示服务实现(JavaPlugin 插件, 翻译服务 翻译服务, 关键词解析器 关键词解析器,
                                玩家服务 玩家服务, 技能冷却服务 技能冷却服务, 属性计算服务 属性计算服务) {
        this.插件 = 插件;
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.玩家服务 = 玩家服务;
        this.技能冷却服务 = 技能冷却服务;
        this.属性计算服务 = 属性计算服务;
        绑定公CD回调();
    }

    private void 绑定公CD回调() {
        技能冷却服务.设置公CD开始回调(玩家标识 -> {
            double 实际GCD = 读取实际公共冷却时间(玩家标识);
            调试日志器.调试("公CD显示服务", "公CD开始回调：玩家=%s 实际GCD=%.3f", 玩家标识, 实际GCD);
            显示(玩家标识, 实际GCD);
        });

        技能冷却服务.设置公CD完成回调(玩家标识 -> {
            调试日志器.调试("公CD显示服务", "公CD完成回调：玩家=%s", 玩家标识);
        });
    }

    @Override
    public void 启动恒定显示(UUID 玩家标识) {
        if (玩家标识 == null) {
            return;
        }
        if (状态表.containsKey(玩家标识)) {
            return;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        double 总秒数 = 读取实际公共冷却时间(玩家标识);
        if (总秒数 <= 0) {
            调试日志器.调试("公CD显示服务", "启动恒定显示跳过：玩家=%s 总秒数=%.3f 非正", 玩家标识, 总秒数);
            return;
        }
        调试日志器.调试("公CD显示服务", "启动恒定显示：玩家=%s 总秒数=%.3f", 玩家标识, 总秒数);
        BossBar bossBar = Bukkit.createBossBar(构建标题(玩家标识, 最小有效剩余, 总秒数, 阶段.空闲),
                BarColor.WHITE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(玩家);
        BukkitTask 任务 = Bukkit.getScheduler().runTaskTimer(插件, () -> 更新(玩家标识), 0L, 更新间隔tick);
        状态表.put(玩家标识, new 显示状态(bossBar, 任务, 0.0));
    }

    @Override
    public void 显示(UUID 玩家标识, double 公共冷却秒数) {
        if (玩家标识 == null || 公共冷却秒数 <= 0) {
            return;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        调试日志器.调试("公CD显示服务", "触发倒计时：玩家=%s 公CD秒数=%.3f", 玩家标识, 公共冷却秒数);
        if (!状态表.containsKey(玩家标识)) {
            启动恒定显示(玩家标识);
        }
    }

    @Override
    public void 清理(UUID 玩家标识) {
        显示状态 状态 = 状态表.remove(玩家标识);
        if (状态 == null) {
            return;
        }
        if (状态.任务 != null) {
            状态.任务.cancel();
        }
        状态.bossBar.removeAll();
        调试日志器.调试("公CD显示服务", "清理BossBar：玩家=%s", 玩家标识);
    }

    private void 更新(UUID 玩家标识) {
        显示状态 状态 = 状态表.get(玩家标识);
        if (状态 == null) {
            return;
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || !玩家.isOnline()) {
            清理(玩家标识);
            return;
        }
        double 总秒数 = 读取实际公共冷却时间(玩家标识);
        if (总秒数 <= 0) {
            清理(玩家标识);
            return;
        }
        double 剩余秒数 = 技能冷却服务.获取公共冷却剩余(玩家标识);
        if (剩余秒数 <= 0) {
            状态.bossBar.setColor(BarColor.WHITE);
            更新进度(状态, 0.0);
            状态.bossBar.setTitle(构建标题(玩家标识, 最小有效剩余, 总秒数, 阶段.空闲));
            return;
        }
        double 进度 = 剩余秒数 / 总秒数;
        阶段 当前阶段 = 计算阶段(进度);
        double 显示剩余 = 格式化显示秒数(剩余秒数);
        double 显示总秒数 = 格式化显示秒数(总秒数);
        状态.bossBar.setColor(BarColor.valueOf(当前阶段.bossBar颜色名));
        更新进度(状态, 进度);
        状态.bossBar.setTitle(构建标题(玩家标识, 显示剩余, 显示总秒数, 当前阶段));
    }

    private void 更新进度(显示状态 状态, double 进度) {
        double 限制进度 = Math.max(0.0, Math.min(1.0, 进度));
        if (Math.abs(限制进度 - 状态.最后进度) >= 进度更新阈值) {
            状态.bossBar.setProgress(限制进度);
            状态.最后进度 = 限制进度;
        }
    }

    private double 读取实际公共冷却时间(UUID 玩家标识) {
        Optional<玩家快照> 快照可选 = 玩家服务.获取快照(玩家标识);
        if (快照可选.isEmpty()) {
            return 0.0;
        }
        属性快照 属性 = 快照可选.get().属性();
        double 基础公共冷却 = 属性.公共冷却时间();
        double 急速 = 属性.急速();
        return 属性计算服务.计算实际公共冷却(基础公共冷却, 急速);
    }

    private 阶段 计算阶段(double 进度) {
        if (进度 > 三分之二) {
            return 阶段.红色;
        }
        if (进度 > 三分之一) {
            return 阶段.黄色;
        }
        return 阶段.绿色;
    }

    private double 格式化显示秒数(double 实际秒数) {
        if (实际秒数 <= 0) {
            return 0.0;
        }
        return Math.ceil(实际秒数 * 显示数值倍数) / 显示数值倍数;
    }

    private String 构建标题(UUID 玩家标识, double 显示剩余, double 显示总秒数, 阶段 当前阶段) {
        String 键 = switch (当前阶段) {
            case 红色 -> 红色阶段键;
            case 黄色 -> 黄色阶段键;
            case 绿色 -> 绿色阶段键;
            case 空闲 -> 空闲阶段键;
        };
        String 模板 = 翻译服务.获取(键);
        String 文本 = 安全格式化(模板, 显示剩余, 显示总秒数);
        return 转传统颜色(关键词解析器.解析(文本, 玩家标识));
    }

    private String 安全格式化(String 模板, double 显示剩余, double 显示总秒数) {
        try {
            return String.format(模板, 显示剩余, 显示总秒数);
        } catch (Exception e) {
            调试日志器.调试("公CD显示服务", "翻译格式化失败，回退为模板原文：键模板=%s 异常=%s", 模板, e.getMessage());
            return 模板;
        }
    }

    private String 转传统颜色(String 文本) {
        return 文本
                .replace("<black>", "§0").replace("</black>", "")
                .replace("<dark_blue>", "§1").replace("</dark_blue>", "")
                .replace("<dark_green>", "§2").replace("</dark_green>", "")
                .replace("<dark_aqua>", "§3").replace("</dark_aqua>", "")
                .replace("<dark_red>", "§4").replace("</dark_red>", "")
                .replace("<dark_purple>", "§5").replace("</dark_purple>", "")
                .replace("<gold>", "§6").replace("</gold>", "")
                .replace("<gray>", "§7").replace("</gray>", "")
                .replace("<dark_gray>", "§8").replace("</dark_gray>", "")
                .replace("<blue>", "§9").replace("</blue>", "")
                .replace("<green>", "§a").replace("</green>", "")
                .replace("<aqua>", "§b").replace("</aqua>", "")
                .replace("<red>", "§c").replace("</red>", "")
                .replace("<light_purple>", "§d").replace("</light_purple>", "")
                .replace("<yellow>", "§e").replace("</yellow>", "")
                .replace("<white>", "§f").replace("</white>", "")
                .replace("<bold>", "§l").replace("</bold>", "");
    }

    private enum 阶段 {
        红色("RED"),
        黄色("YELLOW"),
        绿色("GREEN"),
        空闲("WHITE");

        private final String bossBar颜色名;

        阶段(String bossBar颜色名) {
            this.bossBar颜色名 = bossBar颜色名;
        }
    }

    private static class 显示状态 {
        private final BossBar bossBar;
        private final BukkitTask 任务;
        private double 最后进度;

        显示状态(BossBar bossBar, BukkitTask 任务, double 初始进度) {
            this.bossBar = bossBar;
            this.任务 = 任务;
            this.最后进度 = 初始进度;
        }
    }
}
