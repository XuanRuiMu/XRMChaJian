package 暮澜纪元.通用.翻译;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class 语言代码别名 {

    public static final String 默认语言代码 = "zh";

    public static final Set<String> 支持的语言列表;

    private static final Map<String, String> 语言别名映射;

    static {
        Set<String> 语言集合 = new LinkedHashSet<>();
        语言集合.add("zh");
        语言集合.add("zh_tw");
        语言集合.add("en");
        语言集合.add("ja");
        语言集合.add("ko");
        语言集合.add("de");
        语言集合.add("es");
        语言集合.add("fr");
        语言集合.add("it");
        语言集合.add("pt");
        语言集合.add("ru");
        语言集合.add("th");
        语言集合.add("tr");
        语言集合.add("vi");
        语言集合.add("id");
        语言集合.add("ms");
        语言集合.add("nl");
        语言集合.add("pl");
        支持的语言列表 = Collections.unmodifiableSet(语言集合);

        Map<String, String> 映射 = new java.util.HashMap<>();

        映射.put("zh", "zh");
        映射.put("zh_cn", "zh");
        映射.put("lzh", "zh");

        映射.put("af_za", "zh");
        映射.put("ar_sa", "zh");
        映射.put("bg_bg", "zh");
        映射.put("br_fr", "zh");
        映射.put("brb", "zh");
        映射.put("ca_es", "zh");
        映射.put("cy_gb", "zh");
        映射.put("da_dk", "zh");
        映射.put("el_gr", "zh");
        映射.put("eo_uy", "zh");
        映射.put("et_ee", "zh");
        映射.put("eu_es", "zh");
        映射.put("fa_ir", "zh");
        映射.put("fi_fi", "zh");
        映射.put("fo_fo", "zh");
        映射.put("ga_ie", "zh");
        映射.put("gd_gb", "zh");
        映射.put("gl_es", "zh");
        映射.put("haw_us", "zh");
        映射.put("he_il", "zh");
        映射.put("hi_in", "zh");
        映射.put("hn_no", "zh");
        映射.put("hy_am", "zh");
        映射.put("ig_ng", "zh");
        映射.put("io_en", "zh");
        映射.put("is_is", "zh");
        映射.put("jbo_en", "zh");
        映射.put("ka_ge", "zh");
        映射.put("kn_in", "zh");
        映射.put("kw_gb", "zh");
        映射.put("la_la", "zh");
        映射.put("lb_lu", "zh");
        映射.put("lt_lt", "zh");
        映射.put("lv_lv", "zh");
        映射.put("mn_mn", "zh");
        映射.put("mt_mt", "zh");
        映射.put("nah", "zh");
        映射.put("nn_no", "zh");
        映射.put("no_no", "zh");
        映射.put("ovd", "zh");
        映射.put("pls", "zh");
        映射.put("ro_ro", "zh");
        映射.put("se_no", "zh");
        映射.put("so_so", "zh");
        映射.put("sq_al", "zh");
        映射.put("sv_se", "zh");
        映射.put("ta_in", "zh");
        映射.put("tlh_aa", "zh");
        映射.put("tok", "zh");
        映射.put("tzo_mx", "zh");
        映射.put("yi_de", "zh");
        映射.put("yo_ng", "zh");
        映射.put("hal_ua", "zh");
        映射.put("qya_aa", "zh");
        映射.put("vp_vl", "zh");
        映射.put("lol_us", "zh");

        映射.put("zh_tw", "zh_tw");
        映射.put("zh_hk", "zh_tw");
        映射.put("lzh", "zh_tw");

        映射.put("en", "en");
        映射.put("en_us", "en");
        映射.put("en_gb", "en");
        映射.put("en_au", "en");
        映射.put("en_ca", "en");
        映射.put("en_nz", "en");
        映射.put("en_pt", "en");
        映射.put("en_ud", "en");
        映射.put("enp", "en");
        映射.put("enws", "en");

        映射.put("ja", "ja");
        映射.put("ja_jp", "ja");

        映射.put("ko", "ko");
        映射.put("ko_kr", "ko");

        映射.put("de", "de");
        映射.put("de_de", "de");
        映射.put("de_at", "de");
        映射.put("de_ch", "de");
        映射.put("bar", "de");
        映射.put("nds_de", "de");
        映射.put("ksh", "de");
        映射.put("sxu", "de");
        映射.put("vmf_de", "de");

        映射.put("es", "es");
        映射.put("es_es", "es");
        映射.put("es_mx", "es");
        映射.put("es_ar", "es");
        映射.put("es_cl", "es");
        映射.put("es_ec", "es");
        映射.put("es_uy", "es");
        映射.put("es_ve", "es");
        映射.put("esan", "es");
        映射.put("ast_es", "es");
        映射.put("qcb_es", "es");

        映射.put("fr", "fr");
        映射.put("fr_fr", "fr");
        映射.put("fr_ca", "fr");
        映射.put("fur_it", "fr");
        映射.put("oc_fr", "fr");
        映射.put("vec_it", "fr");
        映射.put("val_es", "fr");

        映射.put("it", "it");
        映射.put("it_it", "it");
        映射.put("lmo_it", "it");

        映射.put("pt", "pt");
        映射.put("pt_br", "pt");
        映射.put("pt_pt", "pt");

        映射.put("ru", "ru");
        映射.put("ru_ru", "ru");
        映射.put("be_by", "ru");
        映射.put("be_latn", "ru");
        映射.put("ba_ru", "ru");
        映射.put("uk_ua", "ru");
        映射.put("ry_ua", "ru");
        映射.put("sah_sah", "ru");
        映射.put("tt_ru", "ru");
        映射.put("kk_kz", "ru");
        映射.put("ky_kg", "ru");
        映射.put("rpr", "ru");

        映射.put("th", "th");
        映射.put("th_th", "th");
        映射.put("lo_la", "th");

        映射.put("tr", "tr");
        映射.put("tr_tr", "tr");
        映射.put("az_az", "tr");

        映射.put("vi", "vi");
        映射.put("vi_vn", "vi");
        映射.put("fil_ph", "vi");
        映射.put("tl_ph", "vi");

        映射.put("id", "id");
        映射.put("id_id", "id");
        映射.put("qid", "id");

        映射.put("ms", "ms");
        映射.put("ms_my", "ms");
        映射.put("zlm_arab", "ms");

        映射.put("nl", "nl");
        映射.put("nl_nl", "nl");
        映射.put("nl_be", "nl");
        映射.put("li_li", "nl");
        映射.put("fy_nl", "nl");

        映射.put("pl", "pl");
        映射.put("pl_pl", "pl");
        映射.put("szl", "pl");
        映射.put("cs_cz", "pl");
        映射.put("sk_sk", "pl");
        映射.put("sl_si", "pl");
        映射.put("bs_ba", "pl");
        映射.put("hr_hr", "pl");
        映射.put("mk_mk", "pl");
        映射.put("sr_cs", "pl");
        映射.put("sr_sp", "pl");
        映射.put("isv", "pl");

        语言别名映射 = Collections.unmodifiableMap(映射);
    }

    private 语言代码别名() {
    }

    public static String 标准化(String 输入) {
        if (输入 == null || 输入.isEmpty()) {
            return 默认语言代码;
        }
        String 规范化 = 输入.toLowerCase().replace("-", "_");
        return 语言别名映射.getOrDefault(规范化, 默认语言代码);
    }

    public static String 标准化(Locale 语言) {
        if (语言 == null) {
            return 默认语言代码;
        }
        return 标准化(语言.toString());
    }
}
