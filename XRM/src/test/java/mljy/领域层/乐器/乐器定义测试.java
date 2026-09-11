package mljy.领域层.乐器;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-13 乐器定义测试（配置驱动不可变值对象）。
 * 验证全部 getter、Optional 字段（资源包音色键/NoteBlockAPI乐器ID）、物理机制默认值、音高转Pitch 的 clamp 与公式。
 */
@DisplayName("FP-13: 乐器定义")
class 乐器定义测试 {

    private 乐器定义 创建典型乐器() {
        return new 乐器定义(
                "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, "xrm.instrument.piano",
                Material.STICK, 1001, 12, 0, 24, 0,
                true, "钢琴描述", 5, 乐器定义.物理机制_瞬时触发);
    }

    @Nested
    @DisplayName("正常流程：getter 保留构造参数")
    class 正常流程 {

        @Test
        @DisplayName("构造的乐器定义应保留全部参数")
        void 构造应保留全部参数() {
            乐器定义 乐器 = 创建典型乐器();
            assertEquals("钢琴", 乐器.获取标识());
            assertEquals("钢琴", 乐器.获取显示名称());
            assertEquals(Sound.BLOCK_NOTE_BLOCK_HARP, 乐器.获取原版音色());
            assertEquals(Material.STICK, 乐器.获取物品材质());
            assertEquals(1001, 乐器.获取自定义模型数据());
            assertEquals(12, 乐器.获取默认音域());
            assertEquals(0, 乐器.获取音域下限());
            assertEquals(24, 乐器.获取音域上限());
            assertEquals(0, 乐器.获取八度偏移());
            assertTrue(乐器.是否力度敏感());
            assertEquals("钢琴描述", 乐器.获取描述());
            assertEquals(乐器定义.物理机制_瞬时触发, 乐器.获取物理机制());
        }

        @Test
        @DisplayName("资源包音色键已配置时应返回非空 Optional")
        void 资源包音色键已配置() {
            乐器定义 乐器 = 创建典型乐器();
            Optional<String> 键 = 乐器.获取资源包音色键();
            assertTrue(键.isPresent());
            assertEquals("xrm.instrument.piano", 键.get());
        }

        @Test
        @DisplayName("NoteBlockAPI乐器ID 已配置时应返回非空 Optional")
        void noteBlockAPI乐器ID已配置() {
            乐器定义 乐器 = 创建典型乐器();
            Optional<Integer> id = 乐器.获取NoteBlockAPI乐器ID();
            assertTrue(id.isPresent());
            assertEquals(5, id.get());
        }

        @Test
        @DisplayName("力度不敏感乐器应返回 false")
        void 力度不敏感乐器() {
            乐器定义 乐器 = new 乐器定义(
                    "鼓", "鼓", Sound.BLOCK_NOTE_BLOCK_BASEDRUM, null,
                    Material.STICK, 1002, 12, 0, 24, 0,
                    false, "鼓描述", null, 乐器定义.物理机制_瞬时触发);
            assertFalse(乐器.是否力度敏感());
        }
    }

    @Nested
    @DisplayName("Optional 字段边界")
    class Optional字段边界 {

        @Test
        @DisplayName("资源包音色键为 null 时应返回空 Optional")
        void 资源包音色键为null() {
            乐器定义 乐器 = new 乐器定义(
                    "鼓", "鼓", Sound.BLOCK_NOTE_BLOCK_BASEDRUM, null,
                    Material.STICK, 1002, 12, 0, 24, 0,
                    false, "鼓描述", null, 乐器定义.物理机制_瞬时触发);
            assertTrue(乐器.获取资源包音色键().isEmpty());
        }

        @Test
        @DisplayName("NoteBlockAPI乐器ID 为 null 时应返回空 Optional")
        void noteBlockAPI乐器ID为null() {
            乐器定义 乐器 = new 乐器定义(
                    "鼓", "鼓", Sound.BLOCK_NOTE_BLOCK_BASEDRUM, null,
                    Material.STICK, 1002, 12, 0, 24, 0,
                    false, "鼓描述", null, 乐器定义.物理机制_瞬时触发);
            assertTrue(乐器.获取NoteBlockAPI乐器ID().isEmpty());
        }
    }

    @Nested
    @DisplayName("物理机制默认值")
    class 物理机制默认值 {

        @Test
        @DisplayName("物理机制为 null 时应默认为 瞬时触发")
        void 物理机制null应默认瞬时触发() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, null);
            assertEquals(乐器定义.物理机制_瞬时触发, 乐器.获取物理机制());
        }

        @Test
        @DisplayName("物理机制 持续激励 应原样保留")
        void 物理机制持续激励() {
            乐器定义 乐器 = new 乐器定义(
                    "小提琴", "小提琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1003, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_持续激励);
            assertEquals(乐器定义.物理机制_持续激励, 乐器.获取物理机制());
        }

        @Test
        @DisplayName("物理机制 慢衰减 应原样保留")
        void 物理机制慢衰减() {
            乐器定义 乐器 = new 乐器定义(
                    "吉他", "吉他", Sound.BLOCK_NOTE_BLOCK_GUITAR, null,
                    Material.STICK, 1004, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_慢衰减);
            assertEquals(乐器定义.物理机制_慢衰减, 乐器.获取物理机制());
        }

        @Test
        @DisplayName("物理机制三个常量应为正确字符串")
        void 物理机制常量值() {
            assertEquals("瞬时触发", 乐器定义.物理机制_瞬时触发);
            assertEquals("持续激励", 乐器定义.物理机制_持续激励);
            assertEquals("慢衰减", 乐器定义.物理机制_慢衰减);
        }
    }

    @Nested
    @DisplayName("音高转Pitch")
    class 音高转Pitch测试 {

        @Test
        @DisplayName("音高=12（中音区）应返回 pitch 1.0")
        void 音高12应为1() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(1.0f, 乐器.音高转Pitch(12), 0.0001f);
        }

        @Test
        @DisplayName("音高=0（下限）应返回 pitch 0.5")
        void 音高0应为05() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(0.5f, 乐器.音高转Pitch(0), 0.0001f);
        }

        @Test
        @DisplayName("音高=24（上限）应返回 pitch 2.0")
        void 音高24应为2() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(2.0f, 乐器.音高转Pitch(24), 0.0001f);
        }

        @Test
        @DisplayName("音高越界上限应 clamp 到音域上限")
        void 音高越界上限应clamp() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(2.0f, 乐器.音高转Pitch(100), 0.0001f,
                    "音高100应 clamp 到上限24，pitch=2.0");
        }

        @Test
        @DisplayName("音高越界下限应 clamp 到音域下限")
        void 音高越界下限应clamp() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(0.5f, 乐器.音高转Pitch(-50), 0.0001f,
                    "音高-50应 clamp 到下限0，pitch=0.5");
        }

        @Test
        @DisplayName("音高=6 应返回 pow(2,-0.5)≈0.7071")
        void 音高6应为071() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            float 期望 = (float) Math.pow(2.0, (6 - 12) / 12.0);
            assertEquals(期望, 乐器.音高转Pitch(6), 0.0001f);
        }

        @Test
        @DisplayName("窄音域上下限相同时应始终返回同一 pitch")
        void 窄音域相同上下限() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 6, 6, 0,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            float 期望 = (float) Math.pow(2.0, (6 - 12) / 12.0);
            assertEquals(期望, 乐器.音高转Pitch(0), 0.0001f);
            assertEquals(期望, 乐器.音高转Pitch(6), 0.0001f);
            assertEquals(期望, 乐器.音高转Pitch(24), 0.0001f);
            assertEquals(期望, 乐器.音高转Pitch(100), 0.0001f);
        }

        @Test
        @DisplayName("八度偏移不影响音高转Pitch（仅 clamp 到音域上下限）")
        void 八度偏移不影响pitch() {
            乐器定义 乐器 = new 乐器定义(
                    "钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                    Material.STICK, 1001, 12, 0, 24, 12,
                    true, "描述", null, 乐器定义.物理机制_瞬时触发);
            assertEquals(1.0f, 乐器.音高转Pitch(12), 0.0001f,
                    "八度偏移不参与 pitch 计算");
        }
    }

    @Nested
    @DisplayName("不可变性")
    class 不可变性测试 {

        @Test
        @DisplayName("多次调用同一 getter 应返回相同结果")
        void 多次调用getter应一致() {
            乐器定义 乐器 = 创建典型乐器();
            assertEquals(乐器.获取标识(), 乐器.获取标识());
            assertEquals(乐器.获取原版音色(), 乐器.获取原版音色());
            assertEquals(乐器.获取物理机制(), 乐器.获取物理机制());
        }

        @Test
        @DisplayName("获取资源包音色键 每次返回独立 Optional 但值一致")
        void 资源包音色键一致() {
            乐器定义 乐器 = 创建典型乐器();
            Optional<String> a = 乐器.获取资源包音色键();
            Optional<String> b = 乐器.获取资源包音色键();
            assertEquals(a, b);
        }
    }
}
