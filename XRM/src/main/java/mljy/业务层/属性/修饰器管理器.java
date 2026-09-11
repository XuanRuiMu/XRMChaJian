package mljy.业务层.属性;

import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import mljy.基础设施层.调试日志器;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class 修饰器管理器 {
    private static final String 模块名 = "修饰器管理器";
    private final Map<UUID, Map<String, List<修饰器>>> 修饰器表 = new ConcurrentHashMap<>();

    public void 注册修饰器(UUID 玩家标识, 修饰器 修饰器) {
        修饰器表
                .computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>())
                .computeIfAbsent(修饰器.类别(), 键 -> new CopyOnWriteArrayList<>())
                .add(修饰器);
        调试日志器.调试(模块名, "注册修饰器：玩家=%s 类别=%s 标签=%s 方式=%s 数值=%.1f 优先级=%d",
                玩家标识, 修饰器.类别(), 修饰器.标签(), 修饰器.方式(), 修饰器.数值(), 修饰器.优先级());
    }

    public void 注销修饰器(UUID 玩家标识, String 类别, String 标签) {
        Map<String, List<修饰器>> 玩家修饰器 = 修饰器表.get(玩家标识);
        if (玩家修饰器 == null) {
            return;
        }
        List<修饰器> 列表 = 玩家修饰器.get(类别);
        if (列表 == null) {
            return;
        }
        列表.removeIf(项 -> 项.标签().equals(标签));
    }

    public void 注销来源修饰器(UUID 玩家标识, String 标签) {
        Map<String, List<修饰器>> 玩家修饰器 = 修饰器表.get(玩家标识);
        if (玩家修饰器 == null) {
            return;
        }
        for (List<修饰器> 列表 : 玩家修饰器.values()) {
            列表.removeIf(项 -> 项.标签().equals(标签));
        }
    }

    public List<修饰器> 查询修饰器(UUID 玩家标识, String 类别) {
        Map<String, List<修饰器>> 玩家修饰器 = 修饰器表.get(玩家标识);
        if (玩家修饰器 == null) {
            return List.of();
        }
        return List.copyOf(玩家修饰器.getOrDefault(类别, List.of()));
    }

    public List<修饰器> 按来源查询修饰器(UUID 玩家标识, String 标签) {
        Map<String, List<修饰器>> 玩家修饰器 = 修饰器表.get(玩家标识);
        if (玩家修饰器 == null) {
            return List.of();
        }
        List<修饰器> 结果 = new ArrayList<>();
        for (List<修饰器> 列表 : 玩家修饰器.values()) {
            for (修饰器 项 : 列表) {
                if (项.标签().equals(标签)) {
                    结果.add(项);
                }
            }
        }
        return 结果;
    }

    public double 计算最终值(UUID 玩家标识, String 类别, double 基础值) {
        List<修饰器> 列表 = 查询修饰器(玩家标识, 类别);
        if (列表.isEmpty()) {
            return 基础值;
        }

        List<修饰器> 排序后 = new ArrayList<>(列表);
        排序后.sort(Comparator.comparingInt(修饰器::优先级));

        double 覆盖值 = 基础值;
        boolean 有覆盖 = false;
        double 加法总和 = 0;
        double 乘法倍率 = 1.0;

        for (修饰器 项 : 排序后) {
            if (项.方式() == 修饰方式.覆盖) {
                if (!有覆盖) {
                    覆盖值 = 项.数值();
                    有覆盖 = true;
                }
            } else if (项.方式() == 修饰方式.相加) {
                加法总和 += 项.数值();
            } else if (项.方式() == 修饰方式.相乘) {
                乘法倍率 *= 项.数值();
            }
        }

        double 结果 = 有覆盖 ? 覆盖值 : 基础值 + 加法总和;
        double 最终结果 = 结果 * 乘法倍率;
        调试日志器.调试(模块名, "计算最终值：玩家=%s 类别=%s 基础值=%.1f 修饰器数量=%d 加法总和=%.1f 乘法倍率=%.2f 最终值=%.1f",
                玩家标识, 类别, 基础值, 列表.size(), 加法总和, 乘法倍率, 最终结果);
        return 最终结果;
    }

    public void 清空玩家修饰器(UUID 玩家标识) {
        修饰器表.remove(玩家标识);
    }
}
