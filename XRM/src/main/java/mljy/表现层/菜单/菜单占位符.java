package mljy.表现层.菜单;

import com.google.inject.Inject;
import mljy.业务层.技能注册服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 菜单占位符 {
    private static final Pattern 玩家名模式 = Pattern.compile("\\{玩家名\\}");
    private static final Pattern 等级模式 = Pattern.compile("\\{等级\\}");
    private static final Pattern 职业模式 = Pattern.compile("\\{职业\\}");
    private static final Pattern 专精模式 = Pattern.compile("\\{专精\\}");
    private static final Pattern 技能名模式 = Pattern.compile("\\{技能名:([^}]+)\\}");
    private static final Pattern 冷却模式 = Pattern.compile("\\{冷却:([^}]+)\\}");
    private static final Pattern 资源模式 = Pattern.compile("\\{资源:([^}]+)\\}");
    private static final String 技能名键前缀 = "skill.";
    private static final String 技能名键后缀 = ".name";
    private static final String 数值格式 = "%.1f";

    private final 翻译服务 翻译服务;
    private final 玩家服务 玩家服务;
    private final 技能注册服务 技能注册服务;

    @Inject
    public 菜单占位符(翻译服务 翻译服务, 玩家服务 玩家服务, 技能注册服务 技能注册服务) {
        this.翻译服务 = 翻译服务;
        this.玩家服务 = 玩家服务;
        this.技能注册服务 = 技能注册服务;
    }

    public String 替换(String 文本, Player 玩家) {
        if (文本 == null || 文本.isEmpty()) {
            return "";
        }
        String 结果 = 文本;
        结果 = 替换玩家名(结果, 玩家);
        结果 = 替换等级(结果, 玩家);
        结果 = 替换职业(结果, 玩家);
        结果 = 替换专精(结果, 玩家);
        结果 = 替换技能名(结果, 玩家);
        结果 = 替换冷却(结果, 玩家);
        结果 = 替换资源(结果, 玩家);
        return 结果;
    }

    private String 替换玩家名(String 文本, Player 玩家) {
        return 玩家名模式.matcher(文本).replaceAll(Matcher.quoteReplacement(玩家.getName()));
    }

    private String 替换等级(String 文本, Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        String 等级文本 = 会话.map(s -> String.valueOf(s.获取等级())).orElse("1");
        return 等级模式.matcher(文本).replaceAll(Matcher.quoteReplacement(等级文本));
    }

    private String 替换职业(String 文本, Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        String 职业文本 = 会话.map(玩家会话::获取职业).orElse("");
        return 职业模式.matcher(文本).replaceAll(Matcher.quoteReplacement(职业文本));
    }

    private String 替换专精(String 文本, Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        String 专精文本 = 会话.map(玩家会话::获取专精).orElse("");
        return 专精模式.matcher(文本).replaceAll(Matcher.quoteReplacement(专精文本));
    }

    private String 替换技能名(String 文本, Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        String 专精 = 会话.map(玩家会话::获取专精).orElse("");
        Matcher 匹配器 = 技能名模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            String 技能标识 = 匹配器.group(1);
            String 技能名 = 查找技能名(技能标识, 专精);
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(技能名));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 查找技能名(String 技能标识, String 专精) {
        Optional<mljy.领域层.技能.技能定义> 定义 = 技能注册服务.获取定义(技能标识);
        if (定义.isPresent()) {
            String 翻译键 = 定义.get().名称翻译键();
            String 翻译值 = 翻译服务.获取(翻译键);
            if (!翻译值.equals(翻译键)) {
                return 翻译值;
            }
        }
        String 推测键 = 技能名键前缀 + 专精 + "." + 技能标识 + 技能名键后缀;
        return 翻译服务.获取(推测键);
    }

    private String 替换冷却(String 文本, Player 玩家) {
        Matcher 匹配器 = 冷却模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            String 技能标识 = 匹配器.group(1);
            String 冷却文本 = 查找冷却(技能标识);
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(冷却文本));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 查找冷却(String 技能标识) {
        Optional<mljy.领域层.技能.技能定义> 定义 = 技能注册服务.获取定义(技能标识);
        if (定义.isPresent()) {
            return String.format(数值格式, 定义.get().冷却时间());
        }
        return "0.0";
    }

    private String 替换资源(String 文本, Player 玩家) {
        Matcher 匹配器 = 资源模式.matcher(文本);
        if (!匹配器.find()) {
            return 文本;
        }
        return 文本;
    }
}
