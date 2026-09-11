package 暮澜纪元.菜单;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("菜单持有者")
class 菜单持有者测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("无参创建应为世界选择类型")
        void 无参创建_应为世界选择类型() {
            菜单持有者 持有者 = new 菜单持有者();
            assertEquals(菜单持有者.菜单类型.世界选择, 持有者.获取类型());
            assertEquals(-1, 持有者.获取世界索引());
        }

        @Test
        @DisplayName("带索引创建应为专精选择类型")
        void 带索引创建_应为专精选择类型() {
            菜单持有者 持有者 = new 菜单持有者(2);
            assertEquals(菜单持有者.菜单类型.专精选择, 持有者.获取类型());
            assertEquals(2, 持有者.获取世界索引());
        }

        @Test
        @DisplayName("getInventory应返回null")
        void getInventory_应返回null() {
            菜单持有者 持有者 = new 菜单持有者();
            assertNull(持有者.getInventory());
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("世界索引为零应正确存储")
        void 世界索引为零_应正确存储() {
            菜单持有者 持有者 = new 菜单持有者(0);
            assertEquals(0, 持有者.获取世界索引());
        }

        @Test
        @DisplayName("世界索引为负数应正确存储")
        void 世界索引为负数_应正确存储() {
            菜单持有者 持有者 = new 菜单持有者(-5);
            assertEquals(-5, 持有者.获取世界索引());
        }
    }
}
