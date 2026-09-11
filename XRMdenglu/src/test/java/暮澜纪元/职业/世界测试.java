package 暮澜纪元.职业;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("世界")
class 世界测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("每个世界应包含至少一个职业")
        void 每个世界应包含至少一个职业() {
            for (世界 当前世界 : 世界.values()) {
                assertFalse(当前世界.获取包含职业().isEmpty(),
                    "世界'" + 当前世界.name() + "'应包含至少一个职业");
            }
        }

        @Test
        @DisplayName("所有枚举值应可通过valueOf获取")
        void valueOf测试() {
            for (世界 当前世界 : 世界.values()) {
                assertEquals(当前世界, 世界.valueOf(当前世界.name()));
            }
        }
    }

    @Nested
    @DisplayName("世界职业分布")
    class 世界职业分布 {

        @Test
        @DisplayName("所有世界的职业总数应与职业枚举总数一致")
        void 所有世界职业总数应一致() {
            int 总数 = 0;
            for (世界 当前世界 : 世界.values()) {
                总数 += 当前世界.获取包含职业().size();
            }
            assertEquals(职业.values().length, 总数, "所有世界的职业总数应与职业枚举总数一致");
        }

        @Test
        @DisplayName("不同世界的职业不应重复")
        void 不同世界的职业不应重复() {
            java.util.Set<职业> 已出现 = new java.util.HashSet<>();
            for (世界 当前世界 : 世界.values()) {
                for (职业 当前职业 : 当前世界.获取包含职业()) {
                    assertTrue(已出现.add(当前职业),
                        "职业'" + 当前职业.name() + "'不应在多个世界中重复出现");
                }
            }
        }
    }
}
