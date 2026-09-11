package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 乐谱领域对象测试。
 * 验证构造/工厂、防御性拷贝、不可修改视图、总时长与播放间隔计算、
 * FP-09 非破坏性量化状态机（应用量化/撤销量化/是否已量化/获取原始音符列表）。
 */
@DisplayName("乐谱")
class 乐谱测试 {

    private List<音符> 创建测试音符列表() {
        return new ArrayList<>(List.of(
                音符.of(0, 4, 1.0),
                音符.of(6, 8, 0.5),
                音符.of(12, 2, 0.75)
        ));
    }

    @Nested
    @DisplayName("创建与构造")
    class 创建与构造测试 {

        @Test
        @DisplayName("构造器应保留全部字段")
        void 构造器应保留字段() {
            UUID 作者 = UUID.randomUUID();
            List<音符> 音符列表 = 创建测试音符列表();
            乐谱 谱 = new 乐谱("测试乐谱", 作者, "作者名", 1000L, 120, 音符列表);
            assertEquals("测试乐谱", 谱.获取名称());
            assertEquals(作者, 谱.获取作者标识());
            assertEquals("作者名", 谱.获取作者名());
            assertEquals(1000L, 谱.获取创建时间戳());
            assertEquals(120, 谱.获取速度BPM());
            assertEquals(3, 谱.获取音符数量());
        }

        @Test
        @DisplayName("构造器应对音符列表做防御性拷贝")
        void 构造器应防御性拷贝() {
            List<音符> 原列表 = 创建测试音符列表();
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 原列表);
            原列表.add(音符.简单(24));
            assertEquals(3, 谱.获取音符数量(), "修改原列表不应影响乐谱内部副本");
        }

        @Test
        @DisplayName("获取音符列表 应返回不可修改视图")
        void 获取音符列表应不可修改() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            List<音符> 视图 = 谱.获取音符列表();
            assertThrows(UnsupportedOperationException.class, () -> 视图.add(音符.简单(0)),
                    "获取音符列表 返回的视图应不可修改");
            assertThrows(UnsupportedOperationException.class, () -> 视图.remove(0));
        }

        @Test
        @DisplayName("创建 工厂方法应生成接近当前时间的创建时间戳")
        void 创建工厂方法应生成时间戳() {
            long 调用前 = System.currentTimeMillis();
            乐谱 谱 = 乐谱.创建("谱", UUID.randomUUID(), "作者", 120, 创建测试音符列表());
            long 调用后 = System.currentTimeMillis();
            assertTrue(谱.获取创建时间戳() >= 调用前 && 谱.获取创建时间戳() <= 调用后,
                    "创建时间戳应接近当前时间");
        }

        @Test
        @DisplayName("创建 工厂方法应保留其他字段")
        void 创建工厂方法应保留字段() {
            UUID 作者 = UUID.randomUUID();
            乐谱 谱 = 乐谱.创建("我的乐谱", 作者, "作者名", 90, 创建测试音符列表());
            assertEquals("我的乐谱", 谱.获取名称());
            assertEquals(作者, 谱.获取作者标识());
            assertEquals("作者名", 谱.获取作者名());
            assertEquals(90, 谱.获取速度BPM());
        }
    }

    @Nested
    @DisplayName("获取音符数量")
    class 音符数量测试 {

        @Test
        @DisplayName("空音符列表数量应为 0")
        void 空列表数量为0() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, new ArrayList<>());
            assertEquals(0, 谱.获取音符数量());
        }

        @Test
        @DisplayName("多个音符应正确计数")
        void 多个音符应正确计数() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertEquals(3, 谱.获取音符数量());
        }
    }

    @Nested
    @DisplayName("获取总时长")
    class 总时长测试 {

        @Test
        @DisplayName("空列表总时长Tick应为 0")
        void 空列表总时长为0() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, new ArrayList<>());
            assertEquals(0L, 谱.获取总时长Tick());
        }

        @Test
        @DisplayName("总时长Tick 应为所有音符时值之和")
        void 总时长Tick应求和() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertEquals(4 + 8 + 2, 谱.获取总时长Tick());
        }

        @Test
        @DisplayName("总时长秒 应为 总时长Tick / 20")
        void 总时长秒应换算() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertEquals((4 + 8 + 2) / 20.0, 谱.获取总时长秒(), 0.0001);
        }

        @Test
        @DisplayName("时值为0的音符不增加总时长")
        void 时值0不增加总时长() {
            List<音符> 列表 = new ArrayList<>(List.of(
                    音符.of(0, 0, 1.0),
                    音符.of(12, 0, 1.0)
            ));
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 列表);
            assertEquals(0L, 谱.获取总时长Tick());
        }
    }

    @Nested
    @DisplayName("获取播放间隔Tick")
    class 播放间隔Tick测试 {

        @Test
        @DisplayName("BPM=120 应返回 max(1, round(1200/120))=10")
        void bpm120应返回10() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, new ArrayList<>());
            assertEquals(10L, 谱.获取播放间隔Tick());
        }

        @Test
        @DisplayName("BPM=0 应返回默认值 4")
        void bpm0应返回4() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 0, new ArrayList<>());
            assertEquals(4L, 谱.获取播放间隔Tick());
        }

        @Test
        @DisplayName("BPM 为负数应返回默认值 4")
        void bpm负数应返回4() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, -60, new ArrayList<>());
            assertEquals(4L, 谱.获取播放间隔Tick());
        }

        @Test
        @DisplayName("BPM=1 应返回 max(1, round(1200))=1200")
        void bpm1应返回1200() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 1, new ArrayList<>());
            assertEquals(1200L, 谱.获取播放间隔Tick());
        }

        @Test
        @DisplayName("BPM 极大值应 clamp 到 1")
        void bpm极大值应clamp到1() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 2000, new ArrayList<>());
            assertEquals(1L, 谱.获取播放间隔Tick());
        }
    }

    @Nested
    @DisplayName("FP-09 非破坏性量化状态机")
    class 量化状态机测试 {

        @Test
        @DisplayName("新建乐谱初始状态应为未量化")
        void 初始状态应为未量化() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertFalse(谱.是否已量化());
        }

        @Test
        @DisplayName("未量化时获取原始音符列表应返回空列表")
        void 未量化时原始列表为空() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertTrue(谱.获取原始音符列表().isEmpty());
        }

        @Test
        @DisplayName("应用量化null 应无操作")
        void 应用量化null应无操作() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            List<音符> 原视图 = 谱.获取音符列表();
            谱.应用量化(null);
            assertFalse(谱.是否已量化());
            assertEquals(3, 谱.获取音符数量(), "应用 null 后音符数量不应改变");
        }

        @Test
        @DisplayName("应用量化后应已量化且音符列表被替换")
        void 应用量化后应已量化() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            List<音符> 量化列表 = new ArrayList<>(List.of(音符.简单(0), 音符.简单(6)));
            谱.应用量化(量化列表);
            assertTrue(谱.是否已量化());
            assertEquals(2, 谱.获取音符数量(), "应用量化后音符列表应被替换");
            assertEquals(0, 谱.获取音符列表().get(0).获取音高());
            assertEquals(6, 谱.获取音符列表().get(1).获取音高());
        }

        @Test
        @DisplayName("应用量化后原始音符列表应保存量化前的音符")
        void 应用量化后原始列表应保存() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            List<音符> 原视图 = 谱.获取音符列表();
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            List<音符> 原始 = 谱.获取原始音符列表();
            assertEquals(3, 原始.size(), "原始音符列表应保存量化前的3个音符");
            assertEquals(0, 原始.get(0).获取音高());
            assertEquals(6, 原始.get(1).获取音高());
            assertEquals(12, 原始.get(2).获取音高());
        }

        @Test
        @DisplayName("撤销量化应恢复原始音符列表")
        void 撤销量化应恢复原始() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            assertTrue(谱.撤销量化());
            assertFalse(谱.是否已量化());
            assertEquals(3, 谱.获取音符数量());
            assertEquals(0, 谱.获取音符列表().get(0).获取音高());
            assertEquals(6, 谱.获取音符列表().get(1).获取音高());
            assertEquals(12, 谱.获取音符列表().get(2).获取音高());
        }

        @Test
        @DisplayName("未量化时撤销量化应返回 false 且无操作")
        void 未量化撤销应返回false() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            assertFalse(谱.撤销量化());
            assertFalse(谱.是否已量化());
            assertEquals(3, 谱.获取音符数量());
        }

        @Test
        @DisplayName("撤销量化后原始音符列表应重置为空")
        void 撤销后原始列表应为空() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            谱.撤销量化();
            assertTrue(谱.获取原始音符列表().isEmpty());
        }

        @Test
        @DisplayName("多次应用量化应保留最原始状态不被覆盖")
        void 多次应用量化不覆盖原始() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            谱.应用量化(new ArrayList<>(List.of(音符.简单(12), 音符.简单(24))));
            List<音符> 原始 = 谱.获取原始音符列表();
            assertEquals(3, 原始.size(), "多次应用量化原始应保留最初始的3个音符");
            assertEquals(2, 谱.获取音符数量(), "当前音符列表应为最后一次应用的2个");
            assertEquals(12, 谱.获取音符列表().get(0).获取音高());
        }

        @Test
        @DisplayName("多次应用量化后撤销应恢复到最原始状态")
        void 多次应用后撤销恢复原始() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            谱.应用量化(new ArrayList<>(List.of(音符.简单(12), 音符.简单(24))));
            assertTrue(谱.撤销量化());
            assertEquals(3, 谱.获取音符数量());
            assertEquals(0, 谱.获取音符列表().get(0).获取音高());
            assertEquals(6, 谱.获取音符列表().get(1).获取音高());
            assertEquals(12, 谱.获取音符列表().get(2).获取音高());
        }

        @Test
        @DisplayName("撤销后可重新应用量化")
        void 撤销后可重新应用() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            谱.撤销量化();
            assertFalse(谱.是否已量化());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(24))));
            assertTrue(谱.是否已量化());
            assertEquals(1, 谱.获取音符数量());
            assertEquals(24, 谱.获取音符列表().get(0).获取音高());
            assertEquals(3, 谱.获取原始音符列表().size());
        }

        @Test
        @DisplayName("应用量化后修改传入列表不应影响乐谱")
        void 应用量化后修改传入列表不影响() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            List<音符> 量化列表 = new ArrayList<>(List.of(音符.简单(0)));
            谱.应用量化(量化列表);
            量化列表.add(音符.简单(24));
            assertEquals(1, 谱.获取音符数量(), "修改传入列表不应影响乐谱内部状态");
        }

        @Test
        @DisplayName("获取原始音符列表应返回不可修改视图")
        void 获取原始音符列表应不可修改() {
            乐谱 谱 = new 乐谱("谱", UUID.randomUUID(), "作者", 0L, 120, 创建测试音符列表());
            谱.应用量化(new ArrayList<>(List.of(音符.简单(0))));
            List<音符> 原始 = 谱.获取原始音符列表();
            assertThrows(UnsupportedOperationException.class, () -> 原始.add(音符.简单(0)));
        }
    }
}
