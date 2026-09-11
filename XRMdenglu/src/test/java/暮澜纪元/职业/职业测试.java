package 暮澜纪元.职业;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("职业")
class 职业测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("每个职业应有角色类型")
        void 每个职业应有角色类型() {
            for (职业 当前职业 : 职业.values()) {
                assertNotNull(当前职业.获取角色类型(), "职业'" + 当前职业.name() + "'应有角色类型");
            }
        }

        @Test
        @DisplayName("所有枚举值应可通过valueOf获取")
        void valueOf测试() {
            for (职业 当前职业 : 职业.values()) {
                assertEquals(当前职业, 职业.valueOf(当前职业.name()));
            }
        }
    }

    @Nested
    @DisplayName("角色类型分布")
    class 角色类型分布 {

        @Test
        @DisplayName("应至少有一个输出职业")
        void 应至少有一个输出职业() {
            boolean 有输出 = false;
            for (职业 当前职业 : 职业.values()) {
                if (当前职业.获取角色类型() == 角色类型.输出) {
                    有输出 = true;
                    break;
                }
            }
            assertTrue(有输出, "应至少有一个输出职业");
        }

        @Test
        @DisplayName("应至少有一个治疗职业")
        void 应至少有一个治疗职业() {
            boolean 有治疗 = false;
            for (职业 当前职业 : 职业.values()) {
                if (当前职业.获取角色类型() == 角色类型.治疗) {
                    有治疗 = true;
                    break;
                }
            }
            assertTrue(有治疗, "应至少有一个治疗职业");
        }

        @Test
        @DisplayName("应至少有一个坦克职业")
        void 应至少有一个坦克职业() {
            boolean 有坦克 = false;
            for (职业 当前职业 : 职业.values()) {
                if (当前职业.获取角色类型() == 角色类型.坦克) {
                    有坦克 = true;
                    break;
                }
            }
            assertTrue(有坦克, "应至少有一个坦克职业");
        }
    }
}
