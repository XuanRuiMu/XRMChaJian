package mljy.领域层.技能;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("参数读取器")
class 参数读取器测试 {

    @Nested
    @DisplayName("无覆盖表时返回默认值")
    class 无覆盖表 {
        @Test
        @DisplayName("空构造器应返回所有类型的默认值")
        void 空构造器_应返回默认值() {
            参数读取器 读取器 = new 参数读取器();

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
            assertEquals(10, 读取器.获取整数("层数", 10));
            assertEquals("默认", 读取器.获取字符串("名字", "默认"));
            assertTrue(读取器.获取布尔值("启用", true));
            assertFalse(读取器.获取布尔值("禁用", false));
            assertEquals(1000L, 读取器.获取长整数("时间", 1000L));
        }

        @Test
        @DisplayName("是否有覆盖应返回false")
        void 是否有覆盖_应返回false() {
            参数读取器 读取器 = new 参数读取器();

            assertFalse(读取器.是否有覆盖("任意键"));
        }

        @Test
        @DisplayName("获取覆盖表应返回空表")
        void 获取覆盖表_应返回空表() {
            参数读取器 读取器 = new 参数读取器();

            assertTrue(读取器.获取覆盖表().isEmpty());
        }
    }

    @Nested
    @DisplayName("有覆盖值时返回覆盖值")
    class 有覆盖值 {
        @Test
        @DisplayName("Map构造器应使用覆盖值")
        void map构造器_应使用覆盖值() {
            Map<String, String> 覆盖 = new HashMap<>();
            覆盖.put("伤害", "999.5");
            覆盖.put("层数", "42");
            覆盖.put("名字", "覆盖名称");
            覆盖.put("启用", "false");
            覆盖.put("时间", "9999999999");
            参数读取器 读取器 = new 参数读取器(覆盖);

            assertEquals(999.5, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertEquals(42, 读取器.获取整数("层数", 0));
            assertEquals("覆盖名称", 读取器.获取字符串("名字", "默认"));
            assertFalse(读取器.获取布尔值("启用", true));
            assertEquals(9999999999L, 读取器.获取长整数("时间", 0L));
        }

        @Test
        @DisplayName("添加覆盖应追加到覆盖表")
        void 添加覆盖_应追加() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("新键", "新值");

            assertTrue(读取器.是否有覆盖("新键"));
            assertEquals("新值", 读取器.获取字符串("新键", "默认"));
        }

        @Test
        @DisplayName("添加null键应被忽略")
        void 添加覆盖_null键_应忽略() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖(null, "值");

            assertTrue(读取器.获取覆盖表().isEmpty());
        }

        @Test
        @DisplayName("是否有覆盖应返回true")
        void 是否有覆盖_应返回true() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("存在键", "1");

            assertTrue(读取器.是否有覆盖("存在键"));
        }

        @Test
        @DisplayName("Map构造器应对null容错")
        void map构造器_null_应返回空表() {
            参数读取器 读取器 = new 参数读取器(null);

            assertTrue(读取器.获取覆盖表().isEmpty());
        }

        @Test
        @DisplayName("Map构造器应防御性拷贝")
        void map构造器_应防御性拷贝() {
            Map<String, String> 原表 = new HashMap<>();
            原表.put("键", "原值");
            参数读取器 读取器 = new 参数读取器(原表);

            原表.put("键", "改后值");

            assertEquals("原值", 读取器.获取字符串("键", "默认"));
        }

        @Test
        @DisplayName("获取覆盖表应返回不可变视图")
        void 获取覆盖表_应不可变() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("键", "值");

            Map<String, String> 视图 = 读取器.获取覆盖表();
            assertThrows(UnsupportedOperationException.class, () -> 视图.put("新键", "新值"));
        }

        @Test
        @DisplayName("布尔值应支持大小写不敏感")
        void 布尔值_应大小写不敏感() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("大写", "TRUE");
            读取器.添加覆盖("大写假", "FALSE");
            读取器.添加覆盖("混合", "True");
            读取器.添加覆盖("混合假", "False");

            assertTrue(读取器.获取布尔值("大写", false));
            assertFalse(读取器.获取布尔值("大写假", true));
            assertTrue(读取器.获取布尔值("混合", false));
            assertFalse(读取器.获取布尔值("混合假", true));
        }
    }

    @Nested
    @DisplayName("覆盖值解析失败时回退默认值")
    class 解析失败 {
        @Test
        @DisplayName("双精度解析失败应回退默认值")
        void 双精度解析失败_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "不是数字");

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
        }

        @Test
        @DisplayName("整数解析失败应回退默认值")
        void 整数解析失败_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("层数", "abc");

            assertEquals(10, 读取器.获取整数("层数", 10));
        }

        @Test
        @DisplayName("长整数解析失败应回退默认值")
        void 长整数解析失败_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("时间", "非数字");

            assertEquals(1000L, 读取器.获取长整数("时间", 1000L));
        }

        @Test
        @DisplayName("布尔值解析失败应回退默认值")
        void 布尔值解析失败_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("启用", "yes");

            assertTrue(读取器.获取布尔值("启用", true));
        }

        @Test
        @DisplayName("整数溢出应回退默认值")
        void 整数溢出_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("大数", "99999999999");

            assertEquals(5, 读取器.获取整数("大数", 5));
        }

        @Test
        @DisplayName("null值覆盖应回退默认值")
        void null值覆盖_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("键", null);

            assertEquals(3.14, 读取器.获取双精度("键", 3.14), 0.0001);
            assertEquals(10, 读取器.获取整数("键", 10));
            assertEquals("默认", 读取器.获取字符串("键", "默认"));
            assertTrue(读取器.获取布尔值("键", true));
            assertEquals(1000L, 读取器.获取长整数("键", 1000L));
        }
    }

    @Nested
    @DisplayName("从参数数组解析")
    class 从参数数组解析 {
        @Test
        @DisplayName("应解析--key=value格式")
        void 应解析键值对() {
            String[] 参数 = {"技能ID", "玩家ID", "--伤害=500.5", "--层数=3", "--启用=true"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 2);

            assertEquals(500.5, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertEquals(3, 读取器.获取整数("层数", 0));
            assertTrue(读取器.获取布尔值("启用", false));
        }

        @Test
        @DisplayName("起始位置前参数应被忽略")
        void 起始位置前参数_应忽略() {
            String[] 参数 = {"--伤害=100", "--层数=1", "--伤害=200"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 2);

            assertEquals(200.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertFalse(读取器.是否有覆盖("层数"));
        }

        @Test
        @DisplayName("非--前缀参数应被忽略")
        void 非前缀参数_应忽略() {
            String[] 参数 = {"普通参数", "--伤害=100", "另一个普通", "--层数=2"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertEquals(100.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertEquals(2, 读取器.获取整数("层数", 0));
            assertEquals(2, 读取器.获取覆盖表().size());
        }

        @Test
        @DisplayName("无等号参数应被忽略")
        void 无等号参数_应忽略() {
            String[] 参数 = {"--无等号", "--伤害=100"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertFalse(读取器.是否有覆盖("无等号"));
            assertEquals(100.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
        }

        @Test
        @DisplayName("空键应被忽略")
        void 空键_应忽略() {
            String[] 参数 = {"--=值", "--伤害=100"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertFalse(读取器.是否有覆盖(""));
            assertEquals(100.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
        }

        @Test
        @DisplayName("空值应被允许")
        void 空值_应允许() {
            String[] 参数 = {"--名字=", "--伤害=100"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertTrue(读取器.是否有覆盖("名字"));
            assertEquals("", 读取器.获取字符串("名字", "默认"));
        }

        @Test
        @DisplayName("null数组应返回空读取器")
        void null数组_应返回空读取器() {
            参数读取器 读取器 = 参数读取器.从参数数组解析(null, 0);

            assertTrue(读取器.获取覆盖表().isEmpty());
        }

        @Test
        @DisplayName("null元素应被跳过")
        void null元素_应跳过() {
            String[] 参数 = {null, "--伤害=100", null, "--层数=2"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertEquals(100.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertEquals(2, 读取器.获取整数("层数", 0));
        }

        @Test
        @DisplayName("负起始位置应从0开始")
        void 负起始位置_应从0开始() {
            String[] 参数 = {"--伤害=100"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, -5);

            assertEquals(100.0, 读取器.获取双精度("伤害", 0.0), 0.0001);
        }

        @Test
        @DisplayName("起始位置超出长度应返回空读取器")
        void 起始位置超出_应返回空读取器() {
            String[] 参数 = {"--伤害=100"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 10);

            assertTrue(读取器.获取覆盖表().isEmpty());
        }

        @Test
        @DisplayName("值中包含等号应正确解析")
        void 值含等号_应正确解析() {
            String[] 参数 = {"--公式=a=b+c"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 0);

            assertEquals("a=b+c", 读取器.获取字符串("公式", "默认"));
        }
    }

    @Nested
    @DisplayName("成对尖括号包裹的值")
    class 成对尖括号 {
        @Test
        @DisplayName("弹道总长度=<100.0>应等效于100.0")
        void 弹道总长度_尖括号_应等效() {
            String[] 参数 = {"技能ID", "玩家ID", "--弹道总长度=<100.0>"};

            参数读取器 读取器 = 参数读取器.从参数数组解析(参数, 2);

            assertEquals(100.0, 读取器.获取双精度("弹道总长度", 0.0), 0.0001);
        }

        @Test
        @DisplayName("数值类型尖括号包裹应被正确剥离")
        void 数值_尖括号_应剥离() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "<500.5>");
            读取器.添加覆盖("层数", "<7>");
            读取器.添加覆盖("时间", "<9999999999>");

            assertEquals(500.5, 读取器.获取双精度("伤害", 0.0), 0.0001);
            assertEquals(7, 读取器.获取整数("层数", 0));
            assertEquals(9999999999L, 读取器.获取长整数("时间", 0L));
        }

        @Test
        @DisplayName("字符串类型尖括号包裹应被正确剥离")
        void 字符串_尖括号_应剥离() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("名字", "<覆盖名称>");

            assertEquals("覆盖名称", 读取器.获取字符串("名字", "默认"));
        }

        @Test
        @DisplayName("布尔值尖括号包裹应被正确剥离")
        void 布尔值_尖括号_应剥离() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("启用", "<true>");

            assertTrue(读取器.获取布尔值("启用", false));
        }

        @Test
        @DisplayName("缺少闭合尖括号<100.0应被拒绝并回退默认值")
        void 缺少闭合_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "<100.0");

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
        }

        @Test
        @DisplayName("缺少起始尖括号100.0>应被拒绝并回退默认值")
        void 缺少起始_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "100.0>");

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
        }

        @Test
        @DisplayName("空尖括号<>应被拒绝并回退默认值")
        void 空尖括号_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "<>");

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
        }

        @Test
        @DisplayName("非数值尖括号<abc>应被拒绝并回退默认值")
        void 非数值尖括号_应回退默认值() {
            参数读取器 读取器 = new 参数读取器();
            读取器.添加覆盖("伤害", "<abc>");

            assertEquals(3.14, 读取器.获取双精度("伤害", 3.14), 0.0001);
        }
    }
}
