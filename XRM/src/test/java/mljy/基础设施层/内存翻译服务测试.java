package mljy.基础设施层;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ERR-10: 内存翻译服务参数替换")
@ExtendWith(MockitoExtension.class)
class 内存翻译服务测试 {

    @Mock
    private Yaml翻译加载器 加载器;

    private 内存翻译服务 翻译服务;

    @BeforeEach
    void setUp() {
        翻译服务 = new 内存翻译服务(加载器);
    }

    @Test
    @DisplayName("ERR-10复现：翻译值用{0}格式时，参数应被替换")
    void 大括号格式参数应被替换() {
        when(加载器.获取("测试键", Locale.SIMPLIFIED_CHINESE))
                .thenReturn("冷却时间：{0}秒");

        String 结果 = 翻译服务.获取("测试键", "1.0");

        assertEquals("冷却时间：1.0秒", 结果,
                "ERR-10: 翻译值用{0}格式时，参数应被替换");
        assertFalse(结果.contains("{0}"),
                "ERR-10: 结果不应包含{0}字面值");
    }

    @Test
    @DisplayName("多参数：翻译值用{0}{1}格式时，参数应被替换")
    void 多个大括号参数应被替换() {
        when(加载器.获取("测试键", Locale.SIMPLIFIED_CHINESE))
                .thenReturn("<gold>{0} - {1}</gold>");

        String 结果 = 翻译服务.获取("测试键", "奥能法师", "测试玩家");

        assertEquals("<gold>奥能法师 - 测试玩家</gold>", 结果,
                "多参数：翻译值用{0}{1}格式时，参数应被替换");
    }

    @Test
    @DisplayName("兼容性：%.1f格式翻译值应继续使用String.format")
    void 百分号格式应使用StringFormat() {
        when(加载器.获取("测试键", Locale.SIMPLIFIED_CHINESE))
                .thenReturn("活力：%.1f/%.1f");

        String 结果 = 翻译服务.获取("测试键", 50.0, 100.0);

        assertEquals("活力：50.0/100.0", 结果,
                "兼容性：%.1f格式翻译值应继续使用String.format");
    }

    @Test
    @DisplayName("兼容性：%s格式翻译值应继续使用String.format")
    void 百分号s格式应使用StringFormat() {
        when(加载器.获取("测试键", Locale.SIMPLIFIED_CHINESE))
                .thenReturn("冲刺冷却中，剩余 %s 秒");

        String 结果 = 翻译服务.获取("测试键", 3);

        assertEquals("冲刺冷却中，剩余 3 秒", 结果,
                "兼容性：%s格式翻译值应继续使用String.format");
    }

    @Test
    @DisplayName("无参数：翻译值应原样返回")
    void 无参数应原样返回() {
        when(加载器.获取("测试键", Locale.SIMPLIFIED_CHINESE))
                .thenReturn("纯文本消息");

        String 结果 = 翻译服务.获取("测试键");

        assertEquals("纯文本消息", 结果,
                "无参数：翻译值应原样返回");
    }
}
