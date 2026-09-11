package 暮澜纪元.通用.文本;

import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 关键词解析器 {
    private static final Pattern 参数模式 = Pattern.compile("\\{(\\d+)\\}");
    private static final Pattern 带值关键词模式 = Pattern.compile("\\[([^\\[\\]:]+):([^\\[\\]]*)\\]");
    private static final Pattern 静态关键词模式 = Pattern.compile("\\[([^\\[\\]:]+)\\]");
    private static final Pattern 技能槽位模式 = Pattern.compile("\\[第([一二三四五六七八九])技能\\]");
    private static final String 技能槽位前缀 = "第";
    private static final String 技能槽位后缀 = "技能";

    private final 技能名解析器 技能名解析器实例;

    @Inject
    public 关键词解析器(技能名解析器 技能名解析器实例) {
        this.技能名解析器实例 = 技能名解析器实例;
    }

    public static 关键词解析器 创建纯文本() {
        return new 关键词解析器((槽位中文数字, 玩家标识) -> Optional.empty());
    }

    public String 解析(String 文本, Object... 参数) {
        return 解析(文本, null, 参数);
    }

    public String 解析(String 文本, UUID 玩家标识, Object... 参数) {
        if (文本 == null || 文本.isEmpty()) {
            return "";
        }
        String 结果 = 替换参数(文本, 参数);
        结果 = 替换带值关键词(结果);
        结果 = 替换静态关键词(结果);
        结果 = 替换技能槽位(结果, 玩家标识);
        return 结果;
    }

    private String 替换参数(String 文本, Object... 参数) {
        if (参数 == null || 参数.length == 0) {
            return 文本;
        }
        Matcher 匹配器 = 参数模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            int 索引 = Integer.parseInt(匹配器.group(1));
            String 替换值 = 索引 < 参数.length ? String.valueOf(参数[索引]) : 匹配器.group();
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(替换值));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 替换带值关键词(String 文本) {
        Matcher 匹配器 = 带值关键词模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            String 关键词名 = 匹配器.group(1);
            String 值 = 匹配器.group(2);
            String 替换值 = 解析带值关键词(关键词名, 值);
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(替换值));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 解析带值关键词(String 关键词名, String 值) {
        try {
            关键词颜色 颜色 = 关键词颜色.valueOf(关键词名);
            return 颜色.格式化MiniMessage(值);
        } catch (IllegalArgumentException e) {
            return "[" + 关键词名 + ":" + 值 + "]";
        }
    }

    private String 替换静态关键词(String 文本) {
        Matcher 匹配器 = 静态关键词模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            String 关键词名 = 匹配器.group(1);
            String 替换值 = 解析静态关键词(关键词名);
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(替换值));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 解析静态关键词(String 关键词名) {
        if (是技能槽位标记(关键词名)) {
            return "[" + 关键词名 + "]";
        }
        try {
            关键词颜色 颜色 = 关键词颜色.valueOf(关键词名);
            return 颜色.格式化MiniMessage(颜色.获取显示文本());
        } catch (IllegalArgumentException e) {
            return "[" + 关键词名 + "]";
        }
    }

    private boolean 是技能槽位标记(String 关键词名) {
        return 关键词名.startsWith(技能槽位前缀)
                && 关键词名.endsWith(技能槽位后缀)
                && 关键词名.length() > 技能槽位前缀.length() + 技能槽位后缀.length();
    }

    private String 替换技能槽位(String 文本, UUID 玩家标识) {
        Matcher 匹配器 = 技能槽位模式.matcher(文本);
        StringBuilder 构建器 = new StringBuilder();
        while (匹配器.find()) {
            String 槽位中文数字 = 匹配器.group(1);
            String 替换值 = 解析技能槽位(槽位中文数字, 玩家标识);
            匹配器.appendReplacement(构建器, Matcher.quoteReplacement(替换值));
        }
        匹配器.appendTail(构建器);
        return 构建器.toString();
    }

    private String 解析技能槽位(String 槽位中文数字, UUID 玩家标识) {
        关键词颜色 槽位颜色 = 获取槽位颜色(槽位中文数字);
        String 默认文本 = 技能槽位前缀 + 槽位中文数字 + 技能槽位后缀;
        Optional<String> 技能名可选 = 技能名解析器实例.解析技能名(槽位中文数字, 玩家标识);
        if (技能名可选.isEmpty()) {
            return 槽位颜色.格式化MiniMessage(默认文本);
        }
        return 槽位颜色.格式化MiniMessage(技能名可选.get());
    }

    private 关键词颜色 获取槽位颜色(String 槽位中文数字) {
        return switch (槽位中文数字) {
            case "一" -> 关键词颜色.第一技能;
            case "二" -> 关键词颜色.第二技能;
            case "三" -> 关键词颜色.第三技能;
            case "四" -> 关键词颜色.第四技能;
            case "五" -> 关键词颜色.第五技能;
            case "六" -> 关键词颜色.第六技能;
            case "七" -> 关键词颜色.第七技能;
            case "八" -> 关键词颜色.第八技能;
            case "九" -> 关键词颜色.第九技能;
            default -> 关键词颜色.点数;
        };
    }
}
