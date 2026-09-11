package mljy.领域层.效果;

import mljy.领域层.技能.参数读取器;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("效果实例参数覆盖")
class 效果实例参数覆盖测试 {

    @Test
    @DisplayName("原有构造器应提供空参数覆盖读取器")
    void 原有构造器_参数覆盖为空() {
        效果实例 效果 = new 效果实例("1_3_1", "1_3", 2, 1000L);
        参数读取器 覆盖 = 效果.获取参数覆盖();
        assertNotNull(覆盖);
        assertFalse(覆盖.是否有覆盖("任意键"));
    }

    @Test
    @DisplayName("带参数覆盖构造器应保留覆盖表")
    void 带覆盖构造器_保留覆盖表() {
        参数读取器 覆盖 = new 参数读取器(Map.of("智力加成", "999"));
        效果实例 效果 = new 效果实例("1_7_2", "1_7", null, 4, 1000L, 覆盖);
        assertTrue(效果.获取参数覆盖().是否有覆盖("智力加成"));
        assertEquals(999, 效果.获取参数覆盖().获取整数("智力加成", 0));
    }

    @Test
    @DisplayName("传入null参数覆盖应回退空读取器")
    void null覆盖_回退空读取器() {
        效果实例 效果 = new 效果实例("1_3_1", "1_3", null, 2, 1000L, 100L, null);
        assertNotNull(效果.获取参数覆盖());
        assertFalse(效果.获取参数覆盖().是否有覆盖("护盾吸收量"));
    }

    @Test
    @DisplayName("带创建时间与参数覆盖的构造器应同时保留两者")
    void 创建时间与覆盖_同时保留() {
        参数读取器 覆盖 = new 参数读取器(Map.of("护盾吸收量", "500"));
        效果实例 效果 = new 效果实例("1_3_1", "1_3", null, 1, 2000L, 12345L, 覆盖);
        assertEquals(12345L, 效果.获取创建时间());
        assertEquals(500.0, 效果.获取参数覆盖().获取双精度("护盾吸收量", 0), 0.0001);
    }

    @Test
    @DisplayName("原有带施法者构造器也应提供空参数覆盖读取器")
    void 原有带施法者构造器_参数覆盖为空() {
        效果实例 效果 = new 效果实例("1_7_2", "1_7", null, 4, 1000L, 100L);
        assertNotNull(效果.获取参数覆盖());
        assertFalse(效果.获取参数覆盖().是否有覆盖("智力加成"));
    }
}
