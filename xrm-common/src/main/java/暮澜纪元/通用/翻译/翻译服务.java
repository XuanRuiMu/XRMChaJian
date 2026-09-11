package 暮澜纪元.通用.翻译;

import java.util.Locale;

public interface 翻译服务 {

    String 获取(String 键, Locale 语言, Object... 参数);

    String 获取(String 键, Object... 参数);

    Locale 获取当前语言();
}
