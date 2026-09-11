package mljy.领域层.乐器;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-06 逐音域完全独立键位绑定表测试。
 * 验证默认表（Apple Musical Typing）、八度平移、逐音域覆盖、查询、音域有效/clamp/切换、从配置加载。
 */
@DisplayName("FP-06: 键位绑定表")
class 键位绑定表测试 {

    @Nested
    @DisplayName("默认表")
    class 默认表测试 {

        @Test
        @DisplayName("默认表音域0应为 Apple Musical Typing 布局")
        void 默认表音域0布局() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
            assertEquals(1, 表.查询音高(0, "s").getAsInt());
            assertEquals(2, 表.查询音高(0, "d").getAsInt());
            assertEquals(3, 表.查询音高(0, "f").getAsInt());
            assertEquals(4, 表.查询音高(0, "g").getAsInt());
            assertEquals(5, 表.查询音高(0, "h").getAsInt());
            assertEquals(6, 表.查询音高(0, "j").getAsInt());
            assertEquals(7, 表.查询音高(0, "w").getAsInt());
            assertEquals(8, 表.查询音高(0, "e").getAsInt());
            assertEquals(9, 表.查询音高(0, "t").getAsInt());
            assertEquals(10, 表.查询音高(0, "y").getAsInt());
            assertEquals(11, 表.查询音高(0, "u").getAsInt());
        }

        @Test
        @DisplayName("默认表音域1应按八度平移 +12")
        void 默认表音域1八度平移() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(12, 表.查询音高(1, "a").getAsInt());
            assertEquals(13, 表.查询音高(1, "s").getAsInt());
            assertEquals(17, 表.查询音高(1, "h").getAsInt());
            assertEquals(18, 表.查询音高(1, "j").getAsInt());
            assertEquals(23, 表.查询音高(1, "u").getAsInt());
        }

        @Test
        @DisplayName("默认表音域4应按八度平移 +48")
        void 默认表音域4八度平移() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(48, 表.查询音高(4, "a").getAsInt());
            assertEquals(59, 表.查询音高(4, "u").getAsInt());
        }

        @Test
        @DisplayName("默认表音域数量应为 5")
        void 默认表音域数量为5() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(5, 表.获取音域数量());
        }

        @Test
        @DisplayName("每音域键数常量应为 12")
        void 每音域键数为12() {
            assertEquals(12, 键位绑定表.每音域键数);
        }

        @Test
        @DisplayName("音域数量常量应为 5")
        void 音域数量常量为5() {
            assertEquals(5, 键位绑定表.音域数量);
        }
    }

    @Nested
    @DisplayName("查询音高")
    class 查询音高测试 {

        @Test
        @DisplayName("有效音域有效键应返回对应音高")
        void 有效音域有效键() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(0, "a").isPresent());
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
        }

        @Test
        @DisplayName("大写键名应大小写不敏感匹配")
        void 大写键名应不敏感() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.查询音高(0, "A").getAsInt());
            assertEquals(7, 表.查询音高(0, "W").getAsInt());
            assertEquals(11, 表.查询音高(0, "U").getAsInt());
        }

        @Test
        @DisplayName("null 键应返回空 OptionalInt")
        void null键应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(0, null).isEmpty());
        }

        @Test
        @DisplayName("空字符串键应返回空 OptionalInt")
        void 空字符串键应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(0, "").isEmpty());
        }

        @Test
        @DisplayName("纯空白键应返回空 OptionalInt")
        void 纯空白键应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(0, "   ").isEmpty());
        }

        @Test
        @DisplayName("不存在的键应返回空 OptionalInt")
        void 不存在键应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(0, "z").isEmpty());
            assertTrue(表.查询音高(0, "x").isEmpty());
        }

        @Test
        @DisplayName("无效音域（负数）应返回空 OptionalInt")
        void 无效音域负数应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(-1, "a").isEmpty());
        }

        @Test
        @DisplayName("无效音域（超出数量）应返回空 OptionalInt")
        void 无效音域超出应返回空() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.查询音高(5, "a").isEmpty());
            assertTrue(表.查询音高(100, "a").isEmpty());
        }
    }

    @Nested
    @DisplayName("获取音域映射")
    class 获取音域映射测试 {

        @Test
        @DisplayName("音域0应返回12个键的映射")
        void 音域0应返回12键() {
            键位绑定表 表 = 键位绑定表.默认表();
            Map<String, Integer> 映射 = 表.获取音域映射(0);
            assertEquals(12, 映射.size());
        }

        @Test
        @DisplayName("音域4应返回12个键的映射")
        void 音域4应返回12键() {
            键位绑定表 表 = 键位绑定表.默认表();
            Map<String, Integer> 映射 = 表.获取音域映射(4);
            assertEquals(12, 映射.size());
        }

        @Test
        @DisplayName("无效音域应返回空映射")
        void 无效音域应返回空映射() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.获取音域映射(-1).isEmpty());
            assertTrue(表.获取音域映射(5).isEmpty());
        }

        @Test
        @DisplayName("获取的音域映射应不可修改")
        void 音域映射应不可修改() {
            键位绑定表 表 = 键位绑定表.默认表();
            Map<String, Integer> 映射 = 表.获取音域映射(0);
            assertThrows(UnsupportedOperationException.class, () -> 映射.put("z", 99));
        }
    }

    @Nested
    @DisplayName("音域有效性")
    class 音域有效性测试 {

        @Test
        @DisplayName("音域 0-4 应有效")
        void 音域0到4应有效() {
            键位绑定表 表 = 键位绑定表.默认表();
            for (int i = 0; i < 5; i++) {
                assertTrue(表.音域有效(i), "音域 " + i + " 应有效");
            }
        }

        @Test
        @DisplayName("音域 -1 应无效")
        void 音域负一应无效() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertFalse(表.音域有效(-1));
        }

        @Test
        @DisplayName("音域 5 应无效")
        void 音域5应无效() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertFalse(表.音域有效(5));
        }
    }

    @Nested
    @DisplayName("规范化音域")
    class 规范化音域测试 {

        @Test
        @DisplayName("负数音域应 clamp 到 0")
        void 负数应clamp到0() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.规范化音域(-1));
            assertEquals(0, 表.规范化音域(-100));
        }

        @Test
        @DisplayName("音域 0 应保持 0")
        void 音域0应保持0() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.规范化音域(0));
        }

        @Test
        @DisplayName("音域 4 应保持 4")
        void 音域4应保持4() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(4, 表.规范化音域(4));
        }

        @Test
        @DisplayName("超出上限音域应 clamp 到 4")
        void 超出上限应clamp到4() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(4, 表.规范化音域(5));
            assertEquals(4, 表.规范化音域(100));
        }
    }

    @Nested
    @DisplayName("切换音域")
    class 切换音域测试 {

        @Test
        @DisplayName("音域0 增加 应切换到音域1")
        void 音域0增加应到1() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(1, 表.切换音域(0, true));
        }

        @Test
        @DisplayName("音域2 减少 应切换到音域1")
        void 音域2减少应到1() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(1, 表.切换音域(2, false));
        }

        @Test
        @DisplayName("音域4 增加应 clamp 到 4")
        void 音域4增加应clamp到4() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(4, 表.切换音域(4, true));
        }

        @Test
        @DisplayName("音域0 减少应 clamp 到 0")
        void 音域0减少应clamp到0() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.切换音域(0, false));
        }

        @Test
        @DisplayName("音域3 增加应到 4")
        void 音域3增加到4() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(4, 表.切换音域(3, true));
        }

        @Test
        @DisplayName("越界当前音域增加应 clamp 到 4")
        void 越界当前音域增加应clamp() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(4, 表.切换音域(10, true));
        }

        @Test
        @DisplayName("越界当前音域减少应 clamp 到 0")
        void 越界当前音域减少应clamp() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.切换音域(-5, false));
        }
    }

    @Nested
    @DisplayName("从配置加载")
    class 从配置加载测试 {

        @Test
        @DisplayName("null 配置应返回默认表")
        void null配置应返回默认表() {
            键位绑定表 表 = 键位绑定表.从配置加载(null);
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
            assertEquals(12, 表.查询音高(1, "a").getAsInt());
            assertEquals(5, 表.获取音域数量());
        }

        @Test
        @DisplayName("无 键位绑定 段应返回默认表")
        void 无键位绑定段应返回默认表() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("其他配置.键", 1);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
            assertEquals(12, 表.查询音高(1, "a").getAsInt());
        }

        @Test
        @DisplayName("显式配置音域0应加载自定义键位")
        void 显式配置音域0() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.a", 0);
            配置.set("键位绑定.音域0.s", 1);
            配置.set("键位绑定.音域0.d", 2);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
            assertEquals(1, 表.查询音高(0, "s").getAsInt());
            assertEquals(2, 表.查询音高(0, "d").getAsInt());
        }

        @Test
        @DisplayName("音域1显式覆盖某键应保留覆盖值")
        void 音域1显式覆盖() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.a", 0);
            配置.set("键位绑定.音域0.s", 1);
            配置.set("键位绑定.音域1.a", 14);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(14, 表.查询音高(1, "a").getAsInt(), "音域1 a键应为显式覆盖值14");
            assertEquals(13, 表.查询音高(1, "s").getAsInt(), "音域1 s键应为八度平移 1+12=13");
        }

        @Test
        @DisplayName("音域1为空应全部回退到八度平移")
        void 音域1为空应八度平移() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.a", 0);
            配置.set("键位绑定.音域0.s", 1);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(12, 表.查询音高(1, "a").getAsInt());
            assertEquals(13, 表.查询音高(1, "s").getAsInt());
        }

        @Test
        @DisplayName("音域0为空应回退到默认音域0布局")
        void 音域0为空应回退默认() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.createSection("键位绑定.音域0");
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(0, 表.查询音高(0, "a").getAsInt(), "音域0为空应回退到默认 a=0");
            assertEquals(7, 表.查询音高(0, "w").getAsInt(), "音域0为空应回退到默认 w=7");
            assertEquals(12, 表.查询音高(1, "a").getAsInt());
        }

        @Test
        @DisplayName("无效音高（负数）应被过滤")
        void 无效音高负数应过滤() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.a", 0);
            配置.set("键位绑定.音域0.s", 1);
            配置.set("键位绑定.音域0.z", -1);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertTrue(表.查询音高(0, "z").isEmpty(), "音高-1应被过滤，z键不应存在");
        }

        @Test
        @DisplayName("无效音高（超出60）应被过滤")
        void 无效音高超出应过滤() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.a", 0);
            配置.set("键位绑定.音域0.s", 1);
            配置.set("键位绑定.音域0.z", 60);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertTrue(表.查询音高(0, "z").isEmpty(), "音高60应被过滤，z键不应存在");
        }

        @Test
        @DisplayName("配置键名大小写应统一转为小写存储")
        void 配置键名应转小写() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("键位绑定.音域0.A", 0);
            配置.set("键位绑定.音域0.S", 1);
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(0, 表.查询音高(0, "a").getAsInt(), "大写键名A应转为小写a");
            assertEquals(0, 表.查询音高(0, "A").getAsInt(), "查询时大小写均应能匹配");
        }
    }
}
