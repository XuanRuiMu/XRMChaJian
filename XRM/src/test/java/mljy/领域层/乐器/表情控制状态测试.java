package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-22 表情控制状态测试（mod 独享，O10）。
 * 验证 CC1/CC2/CC4/CC11 控制器的不可变值对象行为。
 */
@DisplayName("FP-22: 表情控制状态")
class 表情控制状态测试 {

    @Nested
    @DisplayName("默认状态")
    class 默认状态测试 {

        @Test
        @DisplayName("默认状态所有 CC 值应为 0")
        void 默认所有CC为0() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            assertEquals(0, 状态.获取CC1());
            assertEquals(0, 状态.获取CC2());
            assertEquals(0, 状态.获取CC4());
            assertEquals(0, 状态.获取CC11());
        }

        @Test
        @DisplayName("默认状态表情Volume倍率应为 0.0（静音）")
        void 默认表情倍率为0() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            assertEquals(0.0f, 状态.计算表情Volume倍率(), 0.0001f);
        }

        @Test
        @DisplayName("默认状态气声Volume附加应为 0.0")
        void 默认气声附加为0() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            assertEquals(0.0f, 状态.计算气声Volume附加(), 0.0001f);
        }

        @Test
        @DisplayName("默认状态转为映射应为空")
        void 默认转为映射为空() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            assertTrue(状态.转为映射().isEmpty());
        }
    }

    @Nested
    @DisplayName("CC11 表情Volume倍率")
    class 表情Volume测试 {

        @Test
        @DisplayName("CC11=127 应返回 1.0（原音量）")
        void cc11满值返回1() {
            表情控制状态 状态 = new 表情控制状态(0, 0, 0, 127);
            assertEquals(1.0f, 状态.计算表情Volume倍率(), 0.0001f);
        }

        @Test
        @DisplayName("CC11=64 应返回 64/127 ≈ 0.5039")
        void cc11中间值() {
            表情控制状态 状态 = new 表情控制状态(0, 0, 0, 64);
            float 期望 = 64.0f / 127.0f;
            assertEquals(期望, 状态.计算表情Volume倍率(), 0.0001f);
        }

        @Test
        @DisplayName("CC11=0 应返回 0.0（静音）")
        void cc11零值返回0() {
            表情控制状态 状态 = new 表情控制状态(0, 0, 0, 0);
            assertEquals(0.0f, 状态.计算表情Volume倍率(), 0.0001f);
        }

        @Test
        @DisplayName("表情Volume倍率应在 [0.0, 1.0] 范围内")
        void 倍率范围() {
            for (int cc11 = 0; cc11 <= 127; cc11 += 16) {
                表情控制状态 状态 = new 表情控制状态(0, 0, 0, cc11);
                float 倍率 = 状态.计算表情Volume倍率();
                assertTrue(倍率 >= 0.0f && 倍率 <= 1.0f,
                        "CC11=" + cc11 + " 倍率应在 [0,1] 范围内，实际=" + 倍率);
            }
        }
    }

    @Nested
    @DisplayName("CC2 气声Volume附加")
    class 气声Volume测试 {

        @Test
        @DisplayName("CC2=127 应返回 0.2（最大附加）")
        void cc2满值返回02() {
            表情控制状态 状态 = new 表情控制状态(0, 127, 0, 0);
            assertEquals(0.2f, 状态.计算气声Volume附加(), 0.0001f);
        }

        @Test
        @DisplayName("CC2=64 应返回 64/127*0.2 ≈ 0.1008")
        void cc2中间值() {
            表情控制状态 状态 = new 表情控制状态(0, 64, 0, 0);
            float 期望 = (64.0f / 127.0f) * 0.2f;
            assertEquals(期望, 状态.计算气声Volume附加(), 0.0001f);
        }

        @Test
        @DisplayName("CC2=0 应返回 0.0")
        void cc2零值返回0() {
            表情控制状态 状态 = new 表情控制状态(0, 0, 0, 0);
            assertEquals(0.0f, 状态.计算气声Volume附加(), 0.0001f);
        }

        @Test
        @DisplayName("气声附加应在 [0.0, 0.2] 范围内")
        void 附加范围() {
            for (int cc2 = 0; cc2 <= 127; cc2 += 16) {
                表情控制状态 状态 = new 表情控制状态(0, cc2, 0, 0);
                float 附加 = 状态.计算气声Volume附加();
                assertTrue(附加 >= 0.0f && 附加 <= 0.2f,
                        "CC2=" + cc2 + " 附加应在 [0,0.2] 范围内，实际=" + 附加);
            }
        }
    }

    @Nested
    @DisplayName("设置 CC 值")
    class 设置CC测试 {

        @Test
        @DisplayName("设置 CC1 应返回新实例且 CC1 改变")
        void 设置CC1() {
            表情控制状态 原状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 原状态.设置CC(表情控制状态.CC_MODULATION, 100);
            assertNotSame(原状态, 新状态);
            assertEquals(0, 原状态.获取CC1(), "原状态不应被修改");
            assertEquals(100, 新状态.获取CC1());
        }

        @Test
        @DisplayName("设置 CC2 应返回新实例且 CC2 改变")
        void 设置CC2() {
            表情控制状态 原状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 原状态.设置CC(表情控制状态.CC_BREATH, 80);
            assertNotSame(原状态, 新状态);
            assertEquals(0, 原状态.获取CC2(), "原状态不应被修改");
            assertEquals(80, 新状态.获取CC2());
        }

        @Test
        @DisplayName("设置 CC4 应返回新实例且 CC4 改变")
        void 设置CC4() {
            表情控制状态 原状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 原状态.设置CC(表情控制状态.CC_FOOT, 50);
            assertNotSame(原状态, 新状态);
            assertEquals(0, 原状态.获取CC4(), "原状态不应被修改");
            assertEquals(50, 新状态.获取CC4());
        }

        @Test
        @DisplayName("设置 CC11 应返回新实例且 CC11 改变")
        void 设置CC11() {
            表情控制状态 原状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 原状态.设置CC(表情控制状态.CC_EXPRESSION, 127);
            assertNotSame(原状态, 新状态);
            assertEquals(0, 原状态.获取CC11(), "原状态不应被修改");
            assertEquals(127, 新状态.获取CC11());
        }

        @Test
        @DisplayName("设置 CC1 不应改变其他 CC 值")
        void 设置CC1不影响其他() {
            表情控制状态 状态 = new 表情控制状态(50, 60, 70, 80);
            表情控制状态 新状态 = 状态.设置CC(表情控制状态.CC_MODULATION, 100);
            assertEquals(60, 新状态.获取CC2());
            assertEquals(70, 新状态.获取CC4());
            assertEquals(80, 新状态.获取CC11());
        }

        @Test
        @DisplayName("设置不支持的 CC 编号应返回同一实例")
        void 设置不支持的CC() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 状态.设置CC(3, 100); // CC3 不支持
            assertSame(状态, 新状态, "不支持的 CC 编号应返回同一实例");
        }

        @Test
        @DisplayName("设置 CC0 应返回同一实例")
        void 设置CC0() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 状态.设置CC(0, 100);
            assertSame(状态, 新状态);
        }

        @Test
        @DisplayName("设置 CC64（延音）应返回同一实例")
        void 设置CC64() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            表情控制状态 新状态 = 状态.设置CC(64, 100);
            assertSame(状态, 新状态);
        }

        @Test
        @DisplayName("连续设置多个 CC 应累积")
        void 连续设置多个CC() {
            表情控制状态 状态 = 表情控制状态.默认状态()
                    .设置CC(表情控制状态.CC_MODULATION, 30)
                    .设置CC(表情控制状态.CC_BREATH, 40)
                    .设置CC(表情控制状态.CC_FOOT, 50)
                    .设置CC(表情控制状态.CC_EXPRESSION, 60);
            assertEquals(30, 状态.获取CC1());
            assertEquals(40, 状态.获取CC2());
            assertEquals(50, 状态.获取CC4());
            assertEquals(60, 状态.获取CC11());
        }
    }

    @Nested
    @DisplayName("值范围 clamp")
    class Clamp测试 {

        @Test
        @DisplayName("构造器 CC 值负数应被 clamp 到 0")
        void 构造器负数clamp() {
            表情控制状态 状态 = new 表情控制状态(-10, -20, -30, -40);
            assertEquals(0, 状态.获取CC1());
            assertEquals(0, 状态.获取CC2());
            assertEquals(0, 状态.获取CC4());
            assertEquals(0, 状态.获取CC11());
        }

        @Test
        @DisplayName("构造器 CC 值超过 127 应被 clamp 到 127")
        void 构造器超上限clamp() {
            表情控制状态 状态 = new 表情控制状态(200, 300, 400, 500);
            assertEquals(127, 状态.获取CC1());
            assertEquals(127, 状态.获取CC2());
            assertEquals(127, 状态.获取CC4());
            assertEquals(127, 状态.获取CC11());
        }

        @Test
        @DisplayName("设置 CC 时也应 clamp")
        void 设置CC时clamp() {
            表情控制状态 状态 = 表情控制状态.默认状态()
                    .设置CC(表情控制状态.CC_MODULATION, -50);
            assertEquals(0, 状态.获取CC1());

            表情控制状态 状态2 = 表情控制状态.默认状态()
                    .设置CC(表情控制状态.CC_EXPRESSION, 999);
            assertEquals(127, 状态2.获取CC11());
        }

        @Test
        @DisplayName("边界值 0 和 127 应被保留")
        void 边界值保留() {
            表情控制状态 状态0 = new 表情控制状态(0, 0, 0, 0);
            assertEquals(0, 状态0.获取CC1());
            assertEquals(0, 状态0.获取CC2());
            assertEquals(0, 状态0.获取CC4());
            assertEquals(0, 状态0.获取CC11());

            表情控制状态 状态127 = new 表情控制状态(127, 127, 127, 127);
            assertEquals(127, 状态127.获取CC1());
            assertEquals(127, 状态127.获取CC2());
            assertEquals(127, 状态127.获取CC4());
            assertEquals(127, 状态127.获取CC11());
        }
    }

    @Nested
    @DisplayName("转为映射")
    class 转为映射测试 {

        @Test
        @DisplayName("全 0 状态应返回空映射")
        void 全0返回空映射() {
            表情控制状态 状态 = 表情控制状态.默认状态();
            assertTrue(状态.转为映射().isEmpty());
        }

        @Test
        @DisplayName("仅 CC1 非零应只含 CC1")
        void 仅CC1非零() {
            表情控制状态 状态 = new 表情控制状态(80, 0, 0, 0);
            Map<Integer, Integer> 映射 = 状态.转为映射();
            assertEquals(1, 映射.size());
            assertEquals(80, 映射.get(表情控制状态.CC_MODULATION));
        }

        @Test
        @DisplayName("所有 CC 非零应含 4 项")
        void 全部非零() {
            表情控制状态 状态 = new 表情控制状态(10, 20, 30, 40);
            Map<Integer, Integer> 映射 = 状态.转为映射();
            assertEquals(4, 映射.size());
            assertEquals(10, 映射.get(表情控制状态.CC_MODULATION));
            assertEquals(20, 映射.get(表情控制状态.CC_BREATH));
            assertEquals(30, 映射.get(表情控制状态.CC_FOOT));
            assertEquals(40, 映射.get(表情控制状态.CC_EXPRESSION));
        }

        @Test
        @DisplayName("部分 CC 非零应只含非零项")
        void 部分非零() {
            表情控制状态 状态 = new 表情控制状态(0, 50, 0, 70);
            Map<Integer, Integer> 映射 = 状态.转为映射();
            assertEquals(2, 映射.size());
            assertFalse(映射.containsKey(表情控制状态.CC_MODULATION));
            assertTrue(映射.containsKey(表情控制状态.CC_BREATH));
            assertFalse(映射.containsKey(表情控制状态.CC_FOOT));
            assertTrue(映射.containsKey(表情控制状态.CC_EXPRESSION));
        }
    }

    @Nested
    @DisplayName("CC 编号常量")
    class CC编号常量测试 {

        @Test
        @DisplayName("CC1 应为 Modulation（1）")
        void cc1是Modulation() {
            assertEquals(1, 表情控制状态.CC_MODULATION);
        }

        @Test
        @DisplayName("CC2 应为 Breath（2）")
        void cc2是Breath() {
            assertEquals(2, 表情控制状态.CC_BREATH);
        }

        @Test
        @DisplayName("CC4 应为 Foot（4）")
        void cc4是Foot() {
            assertEquals(4, 表情控制状态.CC_FOOT);
        }

        @Test
        @DisplayName("CC11 应为 Expression（11）")
        void cc11是Expression() {
            assertEquals(11, 表情控制状态.CC_EXPRESSION);
        }
    }
}
