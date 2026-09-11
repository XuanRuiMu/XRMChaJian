package mljy.业务层;

import com.google.inject.Inject;
import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.效果调度服务;
import mljy.资源服务;
import mljy.基础设施层.调试日志器;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;
import mljy.领域层.技能.参数注册表;
import mljy.领域层.效果.效果实例;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class 资源变更服务实现 implements 资源变更服务 {
    private static final String 修饰器前缀_资源 = "资源_";
    private static final String 修饰器后缀_上限 = "_上限";

    private static final String 秘能资源标识 = "秘能";
    private static final String 秘兆效果标识 = "1_9_1";
    private static final String 秘兆来源标识 = "1_9";
    private static final String 秘兆触发概率参数名 = "秘兆触发概率";
    private static final String 秘兆持续时间参数名 = "秘兆持续时间";
    private static final double 秘兆触发概率默认 = 0.5;
    private static final double 秘兆持续时间默认 = 5000.0;

    private final 资源服务 资源服务;
    private final 资源注册服务 资源注册服务;
    private final 修饰器管理器 修饰器管理器;
    private final 效果调度服务 效果调度服务;
    private final 参数注册表 参数注册表;
    private java.util.function.BiConsumer<UUID, String> 资源变更回调;

    @Inject
    public 资源变更服务实现(资源服务 资源服务, 资源注册服务 资源注册服务, 修饰器管理器 修饰器管理器,
                         效果调度服务 效果调度服务, 参数注册表 参数注册表) {
        this.资源服务 = 资源服务;
        this.资源注册服务 = 资源注册服务;
        this.修饰器管理器 = 修饰器管理器;
        this.效果调度服务 = 效果调度服务;
        this.参数注册表 = 参数注册表;
    }

    @Override
    public double 获取当前值(UUID 玩家标识, String 资源标识) {
        return 资源服务.获取资源(玩家标识, 资源标识).map(资源::获取当前值).orElse(0.0);
    }

    @Override
    public double 获取上限(UUID 玩家标识, String 资源标识) {
        return 资源服务.获取资源(玩家标识, 资源标识)
                .map(资源 -> 计算最终上限(玩家标识, 资源标识, 资源.获取上限()))
                .orElse(0.0);
    }

    @Override
    public double 获取每秒恢复(UUID 玩家标识, String 资源标识) {
        return 资源注册服务.获取资源定义(资源标识)
                .map(资源定义::获取每秒恢复)
                .orElse(0.0);
    }

    @Override
    public boolean 增加(UUID 玩家标识, String 资源标识, double 变化量) {
        调试日志器.调试("资源变更服务", "变更请求：玩家=%s 资源=%s 操作=增加 变化量=%.1f 来源=外部调用",
                玩家标识, 资源标识, 变化量);
        boolean[] 秘兆触发标志 = {false};
        资源服务.获取资源(玩家标识, 资源标识).ifPresent(资源 -> {
            double 最终上限 = 计算最终上限(玩家标识, 资源标识, 资源.获取上限());
            double 旧值 = 资源.获取当前值();
            调试日志器.调试("资源变更服务", "变更前：玩家=%s 资源=%s 当前量=%.1f 上限=%.1f",
                    玩家标识, 资源标识, 旧值, 最终上限);
            double 新值 = Math.max(0, Math.min(旧值 + 变化量, 最终上限));
            资源.设置当前值(新值);
            boolean 触顶 = 新值 >= 最终上限;
            double 实际增加 = 新值 - 旧值;
            调试日志器.调试("资源变更服务", "变更后：玩家=%s 资源=%s 操作=增加 变化量=%.1f 变化前=%.1f 变化后=%.1f 上限=%.1f 触顶=%s 实际增加=%.1f",
                    玩家标识, 资源标识, 变化量, 旧值, 新值, 最终上限, 触顶, 实际增加);
            try {
                if (秘能资源标识.equals(资源标识) && 实际增加 > 0) {
                    秘兆触发标志[0] = 执行秘兆判定(玩家标识);
                }
                触发资源变更回调(玩家标识, 资源标识);
            } catch (RuntimeException 异常) {
                资源.设置当前值(旧值);
                调试日志器.调试("资源变更服务", "资源变更回滚：玩家=%s 资源=%s 原值=%.1f 原因=%s 回滚量=%.1f",
                        玩家标识, 资源标识, 旧值, 异常.getClass().getSimpleName(), 新值 - 旧值);
                throw 异常;
            }
        });
        return 秘兆触发标志[0];
    }

    private boolean 执行秘兆判定(UUID 玩家标识) {
        double 概率 = 读取秘兆参数(秘兆触发概率参数名, 秘兆触发概率默认);
        if (!判定是否触发秘兆(概率)) {
            return false;
        }
        long 持续 = (long) 读取秘兆参数(秘兆持续时间参数名, 秘兆持续时间默认);
        long 到期时间 = System.currentTimeMillis() + 持续;
        效果实例 新效果 = new 效果实例(秘兆效果标识, 秘兆来源标识, 1, 到期时间);
        调试日志器.调试("资源变更服务", "秘兆添加请求：玩家=%s 效果=%s 层数=%d 到期=%d 概率=%.3f",
                玩家标识, 秘兆效果标识, 新效果.获取层数(), 到期时间, 概率);
        效果调度服务.添加(玩家标识, 新效果);
        return true;
    }

    protected boolean 判定是否触发秘兆(double 概率) {
        return ThreadLocalRandom.current().nextDouble() < 概率;
    }

    private double 读取秘兆参数(String 参数名, double 默认值) {
        List<参数注册表.参数条目> 列表 = 参数注册表.获取参数列表(秘兆效果标识);
        for (参数注册表.参数条目 条目 : 列表) {
            if (参数名.equals(条目.参数名())) {
                try {
                    return Double.parseDouble(条目.默认值());
                } catch (NumberFormatException 忽略) {
                    return 默认值;
                }
            }
        }
        return 默认值;
    }

    @Override
    public void 消耗(UUID 玩家标识, String 资源标识, double 变化量) {
        增加(玩家标识, 资源标识, -变化量);
    }

    @Override
    public void 设置上限(UUID 玩家标识, String 资源标识, double 上限) {
        调试日志器.调试("资源变更服务", "变更请求：玩家=%s 资源=%s 操作=设置上限 新上限=%.1f 来源=外部调用",
                玩家标识, 资源标识, 上限);
        double 旧上限 = 获取上限(玩家标识, 资源标识);
        调试日志器.调试("资源变更服务", "变更前：玩家=%s 资源=%s 当前上限=%.1f",
                玩家标识, 资源标识, 旧上限);
        资源服务.设置资源上限(玩家标识, 资源标识, 上限);
        调试日志器.调试("资源变更服务", "变更后：玩家=%s 资源=%s 操作=设置上限 旧上限=%.1f 新上限=%.1f",
                玩家标识, 资源标识, 旧上限, 上限);
    }

    @Override
    public void 清空(UUID 玩家标识, String 资源标识) {
        调试日志器.调试("资源变更服务", "变更请求：玩家=%s 资源=%s 操作=清空 来源=外部调用",
                玩家标识, 资源标识);
        Optional<mljy.领域层.资源.资源> 资源可选 = 资源服务.获取资源(玩家标识, 资源标识);
        if (资源可选.isEmpty()) {
            调试日志器.调试("资源变更服务", "玩家=%s 资源=%s 操作=清空 跳过：资源不存在",
                    玩家标识, 资源标识);
            return;
        }
        mljy.领域层.资源.资源 资源 = 资源可选.get();
        double 旧值 = 资源.获取当前值();
        double 上限 = 计算最终上限(玩家标识, 资源标识, 资源.获取上限());
        调试日志器.调试("资源变更服务", "变更前：玩家=%s 资源=%s 当前量=%.1f 上限=%.1f",
                玩家标识, 资源标识, 旧值, 上限);
        资源.设置当前值(0);
        调试日志器.调试("资源变更服务", "变更后：玩家=%s 资源=%s 操作=清空 清空前值=%.1f 变化后=0.0 触顶=false",
                玩家标识, 资源标识, 旧值);
        try {
            触发资源变更回调(玩家标识, 资源标识);
        } catch (RuntimeException 异常) {
            资源.设置当前值(旧值);
            调试日志器.调试("资源变更服务", "资源清空回滚：玩家=%s 资源=%s 原值=%.1f 原因=%s 回滚量=%.1f",
                    玩家标识, 资源标识, 旧值, 异常.getClass().getSimpleName(), 旧值);
            throw 异常;
        }
    }

    @Override
    public void 恢复(UUID 玩家标识, String 资源标识) {
        double 每秒恢复 = 获取每秒恢复(玩家标识, 资源标识);
        if (每秒恢复 <= 0) {
            return;
        }
        增加(玩家标识, 资源标识, 每秒恢复);
    }

    @Override
    public void 设置资源变更回调(java.util.function.BiConsumer<UUID, String> 回调) {
        this.资源变更回调 = 回调;
    }

    private void 触发资源变更回调(UUID 玩家标识, String 资源标识) {
        if (资源变更回调 != null) {
            资源变更回调.accept(玩家标识, 资源标识);
        }
    }

    private double 计算最终上限(UUID 玩家标识, String 资源标识, double 基础上限) {
        String 修饰器类别 = 修饰器前缀_资源 + 资源标识 + 修饰器后缀_上限;
        return 修饰器管理器.计算最终值(玩家标识, 修饰器类别, 基础上限);
    }
}
