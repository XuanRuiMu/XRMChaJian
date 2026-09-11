package 暮澜纪元.数据;

import 暮澜纪元.职业.职业;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

@DisplayName("玩家数据")
class 玩家数据测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("UUID和名字创建应默认未选择职业且等级为1")
        void UUID和名字创建_应默认未选择职业且等级为1() {
            UUID 测试ID = UUID.randomUUID();
            玩家数据 数据 = new 玩家数据(测试ID, "测试玩家");
            assertEquals(测试ID, 数据.获取UUID());
            assertEquals("测试玩家", 数据.获取名字());
            assertNull(数据.获取职业());
            assertFalse(数据.是否有职业());
            assertEquals(1, 数据.获取等级());
        }

        @Test
        @DisplayName("设置职业后应已选择职业")
        void 设置职业后_应已选择职业() {
            玩家数据 数据 = new 玩家数据(UUID.randomUUID(), "测试玩家");
            数据.设置职业(职业.奥能法师);
            assertEquals(职业.奥能法师, 数据.获取职业());
            assertTrue(数据.是否有职业());
        }

        @Test
        @DisplayName("设置等级应正确存储")
        void 设置等级_应正确存储() {
            玩家数据 数据 = new 玩家数据(UUID.randomUUID(), "测试玩家");
            数据.设置等级(60);
            assertEquals(60, 数据.获取等级());
        }
    }

    @Nested
    @DisplayName("设置职业为null")
    class 设置职业为null {

        @Test
        @DisplayName("设置职业为null应标记为未选择")
        void 设置职业为null_应标记为未选择() {
            玩家数据 数据 = new 玩家数据(UUID.randomUUID(), "测试玩家");
            数据.设置职业(职业.奥能法师);
            assertTrue(数据.是否有职业());
            数据.设置职业(null);
            assertFalse(数据.是否有职业());
            assertNull(数据.获取职业());
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("等级设置为零应正确存储")
        void 等级设置为零_应正确存储() {
            玩家数据 数据 = new 玩家数据(UUID.randomUUID(), "测试玩家");
            数据.设置等级(0);
            assertEquals(0, 数据.获取等级());
        }

        @Test
        @DisplayName("等级设置为负数应正确存储")
        void 等级设置为负数_应正确存储() {
            玩家数据 数据 = new 玩家数据(UUID.randomUUID(), "测试玩家");
            数据.设置等级(-1);
            assertEquals(-1, 数据.获取等级());
        }
    }
}
