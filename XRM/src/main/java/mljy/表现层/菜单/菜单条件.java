package mljy.表现层.菜单;

import com.google.inject.Inject;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 菜单条件 {
    private static final Pattern 比较条件模式 = Pattern.compile("^(\\S+?)(>=|<=|==|!=|>|<)(.+)$");
    private static final Pattern 权限条件模式 = Pattern.compile("^权限:(.+)$");
    private static final String 等级字段 = "等级";
    private static final String 职业字段 = "职业";
    private static final String 专精字段 = "专精";

    private final 玩家服务 玩家服务;

    @Inject
    public 菜单条件(玩家服务 玩家服务) {
        this.玩家服务 = 玩家服务;
    }

    public boolean 检查全部(Player 玩家, List<String> 条件列表) {
        if (条件列表 == null || 条件列表.isEmpty()) {
            return true;
        }
        for (String 条件 : 条件列表) {
            if (!检查(玩家, 条件)) {
                return false;
            }
        }
        return true;
    }

    public boolean 检查(Player 玩家, String 条件) {
        if (条件 == null || 条件.isBlank()) {
            return true;
        }
        String 去空格 = 条件.trim();
        Matcher 权限匹配 = 权限条件模式.matcher(去空格);
        if (权限匹配.matches()) {
            return 玩家.hasPermission(权限匹配.group(1));
        }
        Matcher 比较匹配 = 比较条件模式.matcher(去空格);
        if (比较匹配.matches()) {
            return 检查比较条件(玩家, 比较匹配.group(1), 比较匹配.group(2), 比较匹配.group(3));
        }
        return true;
    }

    private boolean 检查比较条件(Player 玩家, String 字段, String 运算符, String 期望值) {
        String 实际值 = 获取字段值(玩家, 字段);
        if (实际值 == null) {
            return false;
        }
        return switch (运算符) {
            case "==" -> 实际值.equals(期望值);
            case "!=" -> !实际值.equals(期望值);
            case ">=", "<=", ">", "<" -> 检查数值比较(实际值, 运算符, 期望值);
            default -> false;
        };
    }

    private boolean 检查数值比较(String 实际值, String 运算符, String 期望值) {
        try {
            double 实际 = Double.parseDouble(实际值);
            double 期望 = Double.parseDouble(期望值);
            return switch (运算符) {
                case ">=" -> 实际 >= 期望;
                case "<=" -> 实际 <= 期望;
                case ">" -> 实际 > 期望;
                case "<" -> 实际 < 期望;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String 获取字段值(Player 玩家, String 字段) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        if (会话.isEmpty()) {
            return null;
        }
        玩家会话 实例 = 会话.get();
        return switch (字段) {
            case 等级字段 -> String.valueOf(实例.获取等级());
            case 职业字段 -> 实例.获取职业();
            case 专精字段 -> 实例.获取专精();
            default -> null;
        };
    }
}
