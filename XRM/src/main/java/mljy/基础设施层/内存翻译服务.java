package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.翻译服务;

import java.util.Locale;

public class 内存翻译服务 implements 翻译服务 {
    private static final Locale 默认语言 = Locale.SIMPLIFIED_CHINESE;

    private final Yaml翻译加载器 加载器;

    @Inject
    public 内存翻译服务(Yaml翻译加载器 加载器) {
        this.加载器 = 加载器;
    }

    @Override
    public String 获取(String 键, Locale 语言, Object... 参数) {
        Locale 目标语言 = 语言 != null ? 语言 : 默认语言;
        String 文本 = 加载器.获取(键, 目标语言);
        if (参数 != null && 参数.length > 0) {
            if (包含大括号参数(文本)) {
                return 替换大括号参数(文本, 参数);
            }
            return String.format(文本, 参数);
        }
        return 文本;
    }

    private boolean 包含大括号参数(String 文本) {
        return 文本 != null && 文本.contains("{0}");
    }

    private String 替换大括号参数(String 文本, Object... 参数) {
        String 结果 = 文本;
        for (int i = 0; i < 参数.length; i++) {
            结果 = 结果.replace("{" + i + "}", String.valueOf(参数[i]));
        }
        return 结果;
    }

    @Override
    public String 获取(String 键, Object... 参数) {
        return 获取(键, 默认语言, 参数);
    }

    @Override
    public Locale 获取当前语言() {
        return 默认语言;
    }
}
