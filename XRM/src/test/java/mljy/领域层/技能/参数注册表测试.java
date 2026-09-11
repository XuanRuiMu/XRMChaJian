package mljy.领域层.技能;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("参数注册表")
class 参数注册表测试 {

    private 参数注册表.参数条目 条目(String 参数名, String 默认值, String 描述) {
        return new 参数注册表.参数条目(参数名, 默认值, 描述);
    }

    @Nested
    @DisplayName("注册参数条目")
    class 注册 {
        @Test
        @DisplayName("注册单个条目后应能查询到")
        void 注册单个_应能查询() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目("伤害系数", "1.5", "基础伤害倍率"));

            assertTrue(注册表.是否已注册("1_1"));
            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("1_1");
            assertEquals(1, 列表.size());
            assertEquals("伤害系数", 列表.get(0).参数名());
            assertEquals("1.5", 列表.get(0).默认值());
            assertEquals("基础伤害倍率", 列表.get(0).描述());
        }

        @Test
        @DisplayName("注册多个条目应保持注册顺序")
        void 注册多个_应保持顺序() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_2", 条目("伤害", "100", "伤害"));
            注册表.注册("1_2", 条目("范围", "5", "范围"));
            注册表.注册("1_2", 条目("持续时间", "3", "持续时间"));

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("1_2");
            assertEquals(3, 列表.size());
            assertEquals("伤害", 列表.get(0).参数名());
            assertEquals("范围", 列表.get(1).参数名());
            assertEquals("持续时间", 列表.get(2).参数名());
        }

        @Test
        @DisplayName("注册不同ID应独立存储")
        void 注册不同ID_应独立存储() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目("伤害", "100", ""));
            注册表.注册("1_2", 条目("伤害", "200", ""));

            assertEquals(1, 注册表.获取参数列表("1_1").size());
            assertEquals(1, 注册表.获取参数列表("1_2").size());
            assertEquals("100", 注册表.获取参数列表("1_1").get(0).默认值());
            assertEquals("200", 注册表.获取参数列表("1_2").get(0).默认值());
        }

        @Test
        @DisplayName("注册null ID应被忽略")
        void 注册nullID_应忽略() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册(null, 条目("伤害", "100", ""));

            assertFalse(注册表.是否已注册(null));
            assertTrue(注册表.获取参数列表(null).isEmpty());
        }

        @Test
        @DisplayName("注册null条目应被忽略")
        void 注册null条目_应忽略() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", null);

            assertFalse(注册表.是否已注册("1_1"));
        }

        @Test
        @DisplayName("注册null参数名应被忽略")
        void 注册null参数名_应忽略() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目(null, "100", ""));

            assertFalse(注册表.是否已注册("1_1"));
        }
    }

    @Nested
    @DisplayName("重复注册处理")
    class 重复注册 {
        @Test
        @DisplayName("同ID同参数名应被忽略(幂等)")
        void 同ID同参数名_应忽略() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目("伤害", "100", "原始"));
            注册表.注册("1_1", 条目("伤害", "200", "覆盖"));

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("1_1");
            assertEquals(1, 列表.size());
            assertEquals("100", 列表.get(0).默认值());
            assertEquals("原始", 列表.get(0).描述());
        }

        @Test
        @DisplayName("同ID不同参数名应都保留")
        void 同ID不同参数名_应都保留() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目("伤害", "100", ""));
            注册表.注册("1_1", 条目("范围", "5", ""));

            assertEquals(2, 注册表.获取参数列表("1_1").size());
        }

        @Test
        @DisplayName("重复注册不影响其他条目")
        void 重复注册_不影响其他条目() {
            参数注册表 注册表 = new 参数注册表();

            注册表.注册("1_1", 条目("伤害", "100", ""));
            注册表.注册("1_1", 条目("范围", "5", ""));
            注册表.注册("1_1", 条目("伤害", "999", ""));

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("1_1");
            assertEquals(2, 列表.size());
            assertEquals("100", 列表.get(0).默认值());
            assertEquals("5", 列表.get(1).默认值());
        }
    }

    @Nested
    @DisplayName("获取参数列表")
    class 获取参数列表 {
        @Test
        @DisplayName("未注册ID应返回空列表")
        void 未注册ID_应返回空列表() {
            参数注册表 注册表 = new 参数注册表();

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("不存在");

            assertNotNull(列表);
            assertTrue(列表.isEmpty());
        }

        @Test
        @DisplayName("null ID应返回空列表")
        void nullID_应返回空列表() {
            参数注册表 注册表 = new 参数注册表();

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表(null);

            assertNotNull(列表);
            assertTrue(列表.isEmpty());
        }

        @Test
        @DisplayName("返回列表应不可变")
        void 返回列表_应不可变() {
            参数注册表 注册表 = new 参数注册表();
            注册表.注册("1_1", 条目("伤害", "100", ""));

            List<参数注册表.参数条目> 列表 = 注册表.获取参数列表("1_1");

            assertThrows(UnsupportedOperationException.class, () -> 列表.add(条目("新", "1", "")));
        }
    }

    @Nested
    @DisplayName("是否已注册")
    class 是否已注册 {
        @Test
        @DisplayName("已注册ID应返回true")
        void 已注册_应返回true() {
            参数注册表 注册表 = new 参数注册表();
            注册表.注册("1_1", 条目("伤害", "100", ""));

            assertTrue(注册表.是否已注册("1_1"));
        }

        @Test
        @DisplayName("未注册ID应返回false")
        void 未注册_应返回false() {
            参数注册表 注册表 = new 参数注册表();

            assertFalse(注册表.是否已注册("1_1"));
        }

        @Test
        @DisplayName("null ID应返回false")
        void nullID_应返回false() {
            参数注册表 注册表 = new 参数注册表();

            assertFalse(注册表.是否已注册(null));
        }
    }

    @Nested
    @DisplayName("参数条目record")
    class 参数条目测试 {
        @Test
        @DisplayName("record应正确存储字段")
        void record应正确存储字段() {
            参数注册表.参数条目 条目 = new 参数注册表.参数条目("伤害", "100", "基础伤害");

            assertEquals("伤害", 条目.参数名());
            assertEquals("100", 条目.默认值());
            assertEquals("基础伤害", 条目.描述());
        }

        @Test
        @DisplayName("相同字段值应相等")
        void 相同字段值_应相等() {
            参数注册表.参数条目 条目1 = new 参数注册表.参数条目("伤害", "100", "描述");
            参数注册表.参数条目 条目2 = new 参数注册表.参数条目("伤害", "100", "描述");

            assertEquals(条目1, 条目2);
            assertEquals(条目1.hashCode(), 条目2.hashCode());
        }
    }
}
