package 暮澜纪元.职业;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("角色类型")
class 角色类型测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("枚举值数量应正确")
        void 枚举值数量() {
            assertEquals(3, 角色类型.values().length, "角色类型应有3个枚举值");
        }

        @Test
        @DisplayName("所有枚举值应可通过valueOf获取")
        void valueOf测试() {
            assertNotNull(角色类型.valueOf("输出"));
            assertNotNull(角色类型.valueOf("治疗"));
            assertNotNull(角色类型.valueOf("坦克"));
        }

        @Test
        @DisplayName("枚举值不应为null")
        void 枚举值非空() {
            for (角色类型 值 : 角色类型.values()) {
                assertNotNull(值, "枚举值不应为null");
            }
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入 {

        @Test
        @DisplayName("valueOf无效名称应抛出异常")
        void valueOf_无效名称应抛出异常() {
            assertThrows(IllegalArgumentException.class, () -> 角色类型.valueOf("不存在的类型"));
        }
    }
}
