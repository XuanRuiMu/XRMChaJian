package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-21 弯音轮 + 调制轮状态测试（mod 独享，O6）。
 * 验证不可变值对象的弯音/调制计算与 clamp 行为。
 */
@DisplayName("FP-21: 弯音轮 + 调制轮状态")
class 弯音调制状态测试 {

    @Nested
    @DisplayName("默认状态")
    class 默认状态测试 {

        @Test
        @DisplayName("默认状态弯音值应为 64（中心值）")
        void 默认弯音值为64() {
            弯音调制状态 状态 = 弯音调制状态.默认状态();
            assertEquals(弯音调制状态.弯音中心值, 状态.获取弯音值());
        }

        @Test
        @DisplayName("默认状态调制值应为 0（无颤音）")
        void 默认调制值为0() {
            弯音调制状态 状态 = 弯音调制状态.默认状态();
            assertEquals(0, 状态.获取调制值());
        }

        @Test
        @DisplayName("默认状态弯音Pitch倍率应为 1.0")
        void 默认弯音倍率为1() {
            弯音调制状态 状态 = 弯音调制状态.默认状态();
            assertEquals(1.0f, 状态.计算弯音Pitch倍率(弯音调制状态.默认弯音范围半音), 0.0001f);
        }

        @Test
        @DisplayName("默认状态调制Pitch倍率应为 1.0")
        void 默认调制倍率为1() {
            弯音调制状态 状态 = 弯音调制状态.默认状态();
            assertEquals(1.0f, 状态.计算调制Pitch倍率(System.currentTimeMillis(),
                    弯音调制状态.默认调制频率Hz), 0.0001f);
        }
    }

    @Nested
    @DisplayName("弯音Pitch倍率计算")
    class 弯音倍率测试 {

        @Test
        @DisplayName("弯音值 0（最大降音）范围 2 应返回约 0.8909")
        void 最大降音倍率() {
            弯音调制状态 状态 = new 弯音调制状态(0, 0);
            // 半音偏移 = (0-64)/64 * 2 = -2，倍率 = pow(2, -2/12) ≈ 0.8909
            float 期望 = (float) Math.pow(2.0, -2.0 / 12.0);
            assertEquals(期望, 状态.计算弯音Pitch倍率(2), 0.0001f);
            assertTrue(状态.计算弯音Pitch倍率(2) < 1.0f, "弯音值 0 应降音");
        }

        @Test
        @DisplayName("弯音值 127（最大升音）范围 2 应返回约 1.1213")
        void 最大升音倍率() {
            弯音调制状态 状态 = new 弯音调制状态(127, 0);
            // 半音偏移 = (127-64)/64 * 2 = 1.96875，倍率 = pow(2, 1.96875/12) ≈ 1.1213
            float 期望 = (float) Math.pow(2.0, 1.96875 / 12.0);
            assertEquals(期望, 状态.计算弯音Pitch倍率(2), 0.0001f);
            assertTrue(状态.计算弯音Pitch倍率(2) > 1.0f, "弯音值 127 应升音");
        }

        @Test
        @DisplayName("弯音值 32 范围 2 应返回降音倍率")
        void 中间降音倍率() {
            弯音调制状态 状态 = new 弯音调制状态(32, 0);
            // 半音偏移 = (32-64)/64 * 2 = -1，倍率 = pow(2, -1/12) ≈ 0.9439
            float 期望 = (float) Math.pow(2.0, -1.0 / 12.0);
            assertEquals(期望, 状态.计算弯音Pitch倍率(2), 0.0001f);
        }

        @Test
        @DisplayName("弯音值 96 范围 2 应返回升音倍率")
        void 中间升音倍率() {
            弯音调制状态 状态 = new 弯音调制状态(96, 0);
            // 半音偏移 = (96-64)/64 * 2 = 1，倍率 = pow(2, 1/12) ≈ 1.0595
            float 期望 = (float) Math.pow(2.0, 1.0 / 12.0);
            assertEquals(期望, 状态.计算弯音Pitch倍率(2), 0.0001f);
        }

        @Test
        @DisplayName("弯音范围 0 应被强制为 1 不抛异常")
        void 弯音范围0强制为1() {
            弯音调制状态 状态 = new 弯音调制状态(0, 0);
            // 范围 0 → 强制为 1，半音偏移 = (0-64)/64 * 1 = -1
            float 期望 = (float) Math.pow(2.0, -1.0 / 12.0);
            assertEquals(期望, 状态.计算弯音Pitch倍率(0), 0.0001f);
        }

        @Test
        @DisplayName("弯音范围负数应被强制为 1")
        void 弯音范围负数强制为1() {
            弯音调制状态 状态 = new 弯音调制状态(0, 0);
            assertDoesNotThrow(() -> 状态.计算弯音Pitch倍率(-5));
        }

        @Test
        @DisplayName("不同弯音范围应产生不同倍率")
        void 不同范围不同倍率() {
            弯音调制状态 状态 = new 弯音调制状态(0, 0);
            float 范围2 = 状态.计算弯音Pitch倍率(2);
            float 范围12 = 状态.计算弯音Pitch倍率(12);
            assertNotEquals(范围2, 范围12, 0.0001f, "不同弯音范围应产生不同倍率");
            assertTrue(范围12 < 范围2, "范围 12 的降音幅度应大于范围 2");
        }
    }

    @Nested
    @DisplayName("调制Pitch倍率计算")
    class 调制倍率测试 {

        @Test
        @DisplayName("调制值 0 应始终返回 1.0（无颤音）")
        void 调制值0返回1() {
            弯音调制状态 状态 = new 弯音调制状态(64, 0);
            assertEquals(1.0f, 状态.计算调制Pitch倍率(0L, 6), 0.0001f);
            assertEquals(1.0f, 状态.计算调制Pitch倍率(1000L, 6), 0.0001f);
            assertEquals(1.0f, 状态.计算调制Pitch倍率(System.currentTimeMillis(), 10), 0.0001f);
        }

        @Test
        @DisplayName("调制值 127 时间戳 0 应返回 1.0（sin(0)=0）")
        void 调制值127时间戳0() {
            弯音调制状态 状态 = new 弯音调制状态(64, 127);
            // 相位 = 0，sin(0)=0，半音偏移=0，倍率=1.0
            assertEquals(1.0f, 状态.计算调制Pitch倍率(0L, 6), 0.0001f);
        }

        @Test
        @DisplayName("调制值 127 时间戳 1/4 周期应返回升音倍率")
        void 调制值127四分之一周期() {
            弯音调制状态 状态 = new 弯音调制状态(64, 127);
            // 频率 6Hz，1/4 周期 = 1000/24 ms ≈ 41.67ms
            // 相位 = PI/2，sin = 1，半音偏移 = 0.5，倍率 = pow(2, 0.5/12) ≈ 1.0293
            long 时间戳 = 1000L / 24;
            float 期望 = (float) Math.pow(2.0, 0.5 / 12.0);
            assertEquals(期望, 状态.计算调制Pitch倍率(时间戳, 6), 0.001f);
            assertTrue(状态.计算调制Pitch倍率(时间戳, 6) > 1.0f, "1/4 周期处应升音");
        }

        @Test
        @DisplayName("调制值 127 时间戳 3/4 周期应返回降音倍率")
        void 调制值127四分之三周期() {
            弯音调制状态 状态 = new 弯音调制状态(64, 127);
            // 频率 6Hz，3/4 周期 = 3000/24 ms = 125ms
            // 相位 = 3*PI/2，sin = -1，半音偏移 = -0.5，倍率 = pow(2, -0.5/12) ≈ 0.9715
            long 时间戳 = 3000L / 24;
            float 期望 = (float) Math.pow(2.0, -0.5 / 12.0);
            assertEquals(期望, 状态.计算调制Pitch倍率(时间戳, 6), 0.001f);
            assertTrue(状态.计算调制Pitch倍率(时间戳, 6) < 1.0f, "3/4 周期处应降音");
        }

        @Test
        @DisplayName("频率 0 应被强制为 1 不抛异常")
        void 频率0强制为1() {
            弯音调制状态 状态 = new 弯音调制状态(64, 127);
            assertDoesNotThrow(() -> 状态.计算调制Pitch倍率(1000L, 0));
        }

        @Test
        @DisplayName("频率超过 10 应被 clamp 到 10")
        void 频率超过10被clamp() {
            弯音调制状态 状态 = new 弯音调制状态(64, 127);
            assertDoesNotThrow(() -> 状态.计算调制Pitch倍率(1000L, 100));
        }

        @Test
        @DisplayName("调制值 64 应产生中间深度颤音")
        void 调制值64中间深度() {
            弯音调制状态 状态 = new 弯音调制状态(64, 64);
            // 深度 = 64/127 * 0.5 ≈ 0.2519 半音
            long 时间戳 = 1000L / 24; // 1/4 周期
            float 倍率 = 状态.计算调制Pitch倍率(时间戳, 6);
            // 应略大于 1.0 但小于调制值 127 的倍率
            assertTrue(倍率 > 1.0f, "调制值 64 1/4 周期应升音");
            弯音调制状态 满调制 = new 弯音调制状态(64, 127);
            float 满倍率 = 满调制.计算调制Pitch倍率(时间戳, 6);
            assertTrue(倍率 < 满倍率, "调制值 64 的颤音幅度应小于 127");
        }
    }

    @Nested
    @DisplayName("不可变性与状态变更")
    class 不可变性测试 {

        @Test
        @DisplayName("设置弯音值应返回新实例")
        void 设置弯音值返回新实例() {
            弯音调制状态 原状态 = 弯音调制状态.默认状态();
            弯音调制状态 新状态 = 原状态.设置弯音值(0);
            assertNotSame(原状态, 新状态);
            assertEquals(64, 原状态.获取弯音值(), "原状态不应被修改");
            assertEquals(0, 新状态.获取弯音值());
        }

        @Test
        @DisplayName("设置调制值应返回新实例")
        void 设置调制值返回新实例() {
            弯音调制状态 原状态 = 弯音调制状态.默认状态();
            弯音调制状态 新状态 = 原状态.设置调制值(127);
            assertNotSame(原状态, 新状态);
            assertEquals(0, 原状态.获取调制值(), "原状态不应被修改");
            assertEquals(127, 新状态.获取调制值());
        }

        @Test
        @DisplayName("连续设置应每次返回新实例")
        void 连续设置返回新实例() {
            弯音调制状态 状态 = 弯音调制状态.默认状态();
            弯音调制状态 s1 = 状态.设置弯音值(32);
            弯音调制状态 s2 = s1.设置调制值(64);
            assertNotSame(状态, s1);
            assertNotSame(s1, s2);
            assertEquals(32, s2.获取弯音值());
            assertEquals(64, s2.获取调制值());
        }

        @Test
        @DisplayName("设置弯音值不应改变调制值")
        void 设置弯音值不改变调制值() {
            弯音调制状态 状态 = new 弯音调制状态(64, 100);
            弯音调制状态 新状态 = 状态.设置弯音值(0);
            assertEquals(100, 新状态.获取调制值(), "设置弯音值不应改变调制值");
        }

        @Test
        @DisplayName("设置调制值不应改变弯音值")
        void 设置调制值不改变弯音值() {
            弯音调制状态 状态 = new 弯音调制状态(100, 0);
            弯音调制状态 新状态 = 状态.设置调制值(127);
            assertEquals(100, 新状态.获取弯音值(), "设置调制值不应改变弯音值");
        }
    }

    @Nested
    @DisplayName("值范围 clamp")
    class Clamp测试 {

        @Test
        @DisplayName("弯音值负数应被 clamp 到 0")
        void 弯音值负数clamp() {
            弯音调制状态 状态 = new 弯音调制状态(-50, 0);
            assertEquals(0, 状态.获取弯音值());
        }

        @Test
        @DisplayName("弯音值超过 127 应被 clamp 到 127")
        void 弯音值超上限clamp() {
            弯音调制状态 状态 = new 弯音调制状态(200, 0);
            assertEquals(127, 状态.获取弯音值());
        }

        @Test
        @DisplayName("调制值负数应被 clamp 到 0")
        void 调制值负数clamp() {
            弯音调制状态 状态 = new 弯音调制状态(64, -10);
            assertEquals(0, 状态.获取调制值());
        }

        @Test
        @DisplayName("调制值超过 127 应被 clamp 到 127")
        void 调制值超上限clamp() {
            弯音调制状态 状态 = new 弯音调制状态(64, 300);
            assertEquals(127, 状态.获取调制值());
        }

        @Test
        @DisplayName("设置弯音值时也应 clamp")
        void 设置弯音值时clamp() {
            弯音调制状态 状态 = 弯音调制状态.默认状态().设置弯音值(-100);
            assertEquals(0, 状态.获取弯音值());
        }

        @Test
        @DisplayName("设置调制值时也应 clamp")
        void 设置调制值时clamp() {
            弯音调制状态 状态 = 弯音调制状态.默认状态().设置调制值(500);
            assertEquals(127, 状态.获取调制值());
        }

        @Test
        @DisplayName("边界值 0 和 127 应被保留")
        void 边界值保留() {
            弯音调制状态 状态0 = new 弯音调制状态(0, 0);
            assertEquals(0, 状态0.获取弯音值());
            assertEquals(0, 状态0.获取调制值());

            弯音调制状态 状态127 = new 弯音调制状态(127, 127);
            assertEquals(127, 状态127.获取弯音值());
            assertEquals(127, 状态127.获取调制值());
        }
    }
}
