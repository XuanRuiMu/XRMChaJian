package 暮澜纪元;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("传送门区域")
class 传送门区域测试 {

    @Nested
    @DisplayName("正常流程")
    class 正常流程 {

        @Test
        @DisplayName("创建传送门区域应正确存储所有参数")
        void 创建_应正确存储所有参数() {
            传送门区域 区域 = new 传送门区域("test_portal", "world", 10, 20, 30, 50, 60, 70, "MMORPG", "传送中");
            assertEquals("test_portal", 区域.获取传送门ID());
            assertEquals("world", 区域.获取世界名());
            assertEquals(10, 区域.获取最小X());
            assertEquals(20, 区域.获取最小Y());
            assertEquals(30, 区域.获取最小Z());
            assertEquals(50, 区域.获取最大X());
            assertEquals(60, 区域.获取最大Y());
            assertEquals(70, 区域.获取最大Z());
            assertEquals("MMORPG", 区域.获取目标服务器());
        }

        @Test
        @DisplayName("坐标在区域内应返回true")
        void 是否在区域内_坐标在区域内应返回true() {
            传送门区域 区域 = new 传送门区域("p1", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            assertTrue(区域.是否在区域内("world", 5, 5, 5));
        }

        @Test
        @DisplayName("坐标在边界上应返回true")
        void 是否在区域内_坐标在边界上应返回true() {
            传送门区域 区域 = new 传送门区域("p1", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            assertTrue(区域.是否在区域内("world", 0, 0, 0));
            assertTrue(区域.是否在区域内("world", 10, 10, 10));
        }

        @Test
        @DisplayName("坐标在区域外应返回false")
        void 是否在区域内_坐标在区域外应返回false() {
            传送门区域 区域 = new 传送门区域("p1", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            assertFalse(区域.是否在区域内("world", 11, 5, 5));
            assertFalse(区域.是否在区域内("world", 5, 11, 5));
            assertFalse(区域.是否在区域内("world", 5, 5, 11));
        }

        @Test
        @DisplayName("不同世界名应返回false")
        void 是否在区域内_不同世界名应返回false() {
            传送门区域 区域 = new 传送门区域("p1", "world", 0, 0, 0, 10, 10, 10, "srv", "");
            assertFalse(区域.是否在区域内("other_world", 5, 5, 5));
        }
    }

    @Nested
    @DisplayName("坐标自动排序")
    class 坐标自动排序 {

        @Test
        @DisplayName("最小最大坐标反转时应自动排序")
        void 最小最大坐标反转_应自动排序() {
            传送门区域 区域 = new 传送门区域("p1", "world", 50, 60, 70, 10, 20, 30, "srv", "");
            assertEquals(10, 区域.获取最小X());
            assertEquals(20, 区域.获取最小Y());
            assertEquals(30, 区域.获取最小Z());
            assertEquals(50, 区域.获取最大X());
            assertEquals(60, 区域.获取最大Y());
            assertEquals(70, 区域.获取最大Z());
        }

        @Test
        @DisplayName("坐标排序后区域内判断应正确")
        void 坐标排序后_区域内判断应正确() {
            传送门区域 区域 = new 传送门区域("p1", "world", 50, 60, 70, 10, 20, 30, "srv", "");
            assertTrue(区域.是否在区域内("world", 30, 40, 50));
            assertFalse(区域.是否在区域内("world", 5, 5, 5));
        }
    }
}
