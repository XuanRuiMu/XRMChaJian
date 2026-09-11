package 暮澜纪元.配置;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("重生点配置")
class 重生点配置测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("创建重生点配置应正确存储所有参数")
        void 创建_应正确存储所有参数() {
            重生点配置 配置 = new 重生点配置("login_world", 100.5, 64.0, -200.3, 90.0f, 45.0f);
            assertEquals("login_world", 配置.获取世界名());
            assertEquals(100.5, 配置.获取坐标X(), 0.001);
            assertEquals(64.0, 配置.获取坐标Y(), 0.001);
            assertEquals(-200.3, 配置.获取坐标Z(), 0.001);
            assertEquals(90.0f, 配置.获取偏航角(), 0.001);
            assertEquals(45.0f, 配置.获取俯仰角(), 0.001);
        }

        @Test
        @DisplayName("零坐标重生点应正确存储")
        void 创建_零坐标应正确存储() {
            重生点配置 配置 = new 重生点配置("world", 0.0, 0.0, 0.0, 0.0f, 0.0f);
            assertEquals(0.0, 配置.获取坐标X(), 0.001);
            assertEquals(0.0, 配置.获取坐标Y(), 0.001);
            assertEquals(0.0, 配置.获取坐标Z(), 0.001);
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值 {

        @Test
        @DisplayName("负坐标应正确存储")
        void 创建_负坐标应正确存储() {
            重生点配置 配置 = new 重生点配置("world", -999.99, -64.0, -999.99, -180.0f, -90.0f);
            assertEquals(-999.99, 配置.获取坐标X(), 0.001);
            assertEquals(-64.0, 配置.获取坐标Y(), 0.001);
            assertEquals(-999.99, 配置.获取坐标Z(), 0.001);
            assertEquals(-180.0f, 配置.获取偏航角(), 0.001);
            assertEquals(-90.0f, 配置.获取俯仰角(), 0.001);
        }

        @Test
        @DisplayName("极大坐标应正确存储")
        void 创建_极大坐标应正确存储() {
            重生点配置 配置 = new 重生点配置("world", 30000000.0, 320.0, 30000000.0, 180.0f, 90.0f);
            assertEquals(30000000.0, 配置.获取坐标X(), 0.001);
            assertEquals(320.0, 配置.获取坐标Y(), 0.001);
            assertEquals(30000000.0, 配置.获取坐标Z(), 0.001);
        }

        @Test
        @DisplayName("空世界名应正确存储")
        void 创建_空世界名应正确存储() {
            重生点配置 配置 = new 重生点配置("", 0.0, 0.0, 0.0, 0.0f, 0.0f);
            assertEquals("", 配置.获取世界名());
        }
    }
}
