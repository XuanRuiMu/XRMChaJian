package 暮澜纪元.通用.翻译;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class 语言代码别名测试 {

    @Test
    void 标准语言代码应原样返回() {
        assertEquals("zh", 语言代码别名.标准化("zh"));
        assertEquals("en", 语言代码别名.标准化("en"));
        assertEquals("ja", 语言代码别名.标准化("ja"));
        assertEquals("ko", 语言代码别名.标准化("ko"));
        assertEquals("de", 语言代码别名.标准化("de"));
        assertEquals("fr", 语言代码别名.标准化("fr"));
        assertEquals("es", 语言代码别名.标准化("es"));
        assertEquals("it", 语言代码别名.标准化("it"));
        assertEquals("pt", 语言代码别名.标准化("pt"));
        assertEquals("ru", 语言代码别名.标准化("ru"));
        assertEquals("th", 语言代码别名.标准化("th"));
        assertEquals("tr", 语言代码别名.标准化("tr"));
        assertEquals("vi", 语言代码别名.标准化("vi"));
        assertEquals("id", 语言代码别名.标准化("id"));
        assertEquals("ms", 语言代码别名.标准化("ms"));
        assertEquals("nl", 语言代码别名.标准化("nl"));
        assertEquals("pl", 语言代码别名.标准化("pl"));
    }

    @Test
    void zh_tw应保持zh_tw不被合并到zh() {
        assertEquals("zh_tw", 语言代码别名.标准化("zh_tw"));
    }

    @Test
    void 别名应映射到标准代码() {
        assertEquals("zh", 语言代码别名.标准化("zh_cn"));
        assertEquals("en", 语言代码别名.标准化("en_us"));
        assertEquals("en", 语言代码别名.标准化("en_gb"));
        assertEquals("ja", 语言代码别名.标准化("ja_jp"));
        assertEquals("ko", 语言代码别名.标准化("ko_kr"));
        assertEquals("de", 语言代码别名.标准化("de_de"));
        assertEquals("de", 语言代码别名.标准化("de_at"));
        assertEquals("es", 语言代码别名.标准化("es_mx"));
        assertEquals("fr", 语言代码别名.标准化("fr_ca"));
        assertEquals("it", 语言代码别名.标准化("it_it"));
        assertEquals("pt", 语言代码别名.标准化("pt_br"));
        assertEquals("ru", 语言代码别名.标准化("ru_ru"));
        assertEquals("ru", 语言代码别名.标准化("uk_ua"));
        assertEquals("th", 语言代码别名.标准化("th_th"));
        assertEquals("tr", 语言代码别名.标准化("tr_tr"));
        assertEquals("vi", 语言代码别名.标准化("vi_vn"));
        assertEquals("id", 语言代码别名.标准化("id_id"));
        assertEquals("ms", 语言代码别名.标准化("ms_my"));
        assertEquals("nl", 语言代码别名.标准化("nl_nl"));
        assertEquals("pl", 语言代码别名.标准化("pl_pl"));
        assertEquals("pl", 语言代码别名.标准化("cs_cz"));
    }

    @Test
    void zh_tw的别名应映射到zh_tw() {
        assertEquals("zh_tw", 语言代码别名.标准化("zh_hk"));
        assertEquals("zh_tw", 语言代码别名.标准化("lzh"));
    }

    @Test
    void 未知语言应返回默认zh() {
        assertEquals("zh", 语言代码别名.标准化("xxx"));
        assertEquals("zh", 语言代码别名.标准化("unknown_lang"));
    }

    @Test
    void null输入应返回默认zh() {
        assertEquals("zh", 语言代码别名.标准化((String) null));
    }

    @Test
    void 空字符串应返回默认zh() {
        assertEquals("zh", 语言代码别名.标准化(""));
    }

    @Test
    void 大写输入应正常处理() {
        assertEquals("zh", 语言代码别名.标准化("ZH_CN"));
        assertEquals("en", 语言代码别名.标准化("EN_US"));
        assertEquals("ja", 语言代码别名.标准化("JA_JP"));
    }

    @Test
    void 连字符应转为下划线() {
        assertEquals("zh", 语言代码别名.标准化("zh-CN"));
        assertEquals("en", 语言代码别名.标准化("en-US"));
        assertEquals("ja", 语言代码别名.标准化("ja-JP"));
    }

    @Test
    void Locale参数版本应正确工作() {
        assertEquals("zh", 语言代码别名.标准化(Locale.SIMPLIFIED_CHINESE));
        assertEquals("en", 语言代码别名.标准化(Locale.US));
        assertEquals("ja", 语言代码别名.标准化(Locale.JAPAN));
        assertEquals("ko", 语言代码别名.标准化(Locale.KOREA));
        assertEquals("de", 语言代码别名.标准化(Locale.GERMANY));
        assertEquals("fr", 语言代码别名.标准化(Locale.FRANCE));
        assertEquals("it", 语言代码别名.标准化(Locale.ITALY));
        assertEquals("zh_tw", 语言代码别名.标准化(Locale.TRADITIONAL_CHINESE));
    }

    @Test
    void nullLocale应返回默认zh() {
        assertEquals("zh", 语言代码别名.标准化((Locale) null));
    }
}
