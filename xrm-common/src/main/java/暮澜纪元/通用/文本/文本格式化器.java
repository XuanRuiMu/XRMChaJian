package 暮澜纪元.通用.文本;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 文本格式化器 {
    private static final Pattern 标记模式 = Pattern.compile("\\[(dt|plain)](.*?)\\[/\\1]", Pattern.DOTALL);
    private static final Pattern 标签模式 = Pattern.compile("<[^>]+>");
    private static final String 默认颜色开 = "<gray>";
    private static final String 默认颜色关 = "</gray>";
    private static final String 伤害描述颜色开 = "<red>";
    private static final String 伤害描述颜色关 = "</red>";

    private static final List<关键词颜色> 伤害类型关键词 = List.of(
            关键词颜色.真实,
            关键词颜色.多彩,
            关键词颜色.星界,
            关键词颜色.混乱,
            关键词颜色.生命,
            关键词颜色.死亡,
            关键词颜色.神圣,
            关键词颜色.暗影,
            关键词颜色.奥术,
            关键词颜色.自然,
            关键词颜色.冰霜,
            关键词颜色.火焰,
            关键词颜色.物理,
            关键词颜色.魔法
    );

    private final 关键词解析器 解析器;

    @Inject
    public 文本格式化器(关键词解析器 解析器) {
        this.解析器 = 解析器;
    }

    public static 文本格式化器 创建纯文本() {
        return new 文本格式化器(关键词解析器.创建纯文本());
    }

    public Component 格式化(String 文本) {
        String 处理后 = 处理标记(文本);
        return MiniMessage.miniMessage().deserialize(处理后);
    }

    public String 处理标记(String 文本) {
        if (文本 == null || 文本.isEmpty()) {
            return "";
        }
        StringBuilder 结果 = new StringBuilder();
        Matcher 匹配器 = 标记模式.matcher(文本);
        int 当前位置 = 0;
        while (匹配器.find()) {
            if (匹配器.start() > 当前位置) {
                String 普通段 = 文本.substring(当前位置, 匹配器.start());
                结果.append(默认颜色开).append(解析器.解析(普通段)).append(默认颜色关);
            }
            String 标记类型 = 匹配器.group(1);
            String 内容 = 匹配器.group(2);
            if ("dt".equals(标记类型)) {
                结果.append(伤害描述颜色开).append(着色伤害类型关键词(内容)).append(伤害描述颜色关);
            } else {
                结果.append(默认颜色开).append(内容).append(默认颜色关);
            }
            当前位置 = 匹配器.end();
        }
        if (当前位置 < 文本.length()) {
            String 剩余段 = 文本.substring(当前位置);
            结果.append(默认颜色开).append(解析器.解析(剩余段)).append(默认颜色关);
        }
        return 结果.toString();
    }

    private String 着色伤害类型关键词(String 内容) {
        StringBuilder 结果 = new StringBuilder();
        Matcher 标签匹配器 = 标签模式.matcher(内容);
        int 当前位置 = 0;
        while (标签匹配器.find()) {
            if (标签匹配器.start() > 当前位置) {
                String 普通段 = 内容.substring(当前位置, 标签匹配器.start());
                结果.append(替换伤害类型关键词(普通段));
            }
            结果.append(标签匹配器.group());
            当前位置 = 标签匹配器.end();
        }
        if (当前位置 < 内容.length()) {
            String 剩余段 = 内容.substring(当前位置);
            结果.append(替换伤害类型关键词(剩余段));
        }
        return 结果.toString();
    }

    private String 替换伤害类型关键词(String 文本) {
        String 结果 = 文本;
        for (关键词颜色 颜色 : 伤害类型关键词) {
            String 关键词 = 颜色.获取显示文本();
            String 着色后 = 颜色.格式化MiniMessage(关键词);
            结果 = 结果.replace(关键词, 着色后);
        }
        return 结果;
    }
}
