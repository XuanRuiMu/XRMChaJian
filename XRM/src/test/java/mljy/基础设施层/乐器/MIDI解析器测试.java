package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-17 MIDI 解析器/导出器往返一致性测试。
 * 验证 乐谱 → MIDI → 乐谱 的数据保真度。
 * <p>
 * 已知限制：MIDI 时值由 NOTE_ON/NOTE_OFF 事件对决定，
 * 解析时末音符的时值由其 NOTE_OFF 位置决定，可保留。
 * MIDI tick → MC tick 换算存在四舍五入，BPM=120/PPQ=480 时为整数。
 */
@DisplayName("FP-17: MIDI 解析器/导出器")
class MIDI解析器测试 {

    @TempDir
    private File 临时目录;

    private 乐谱 创建测试乐谱() {
        UUID 作者标识 = UUID.randomUUID();
        List<音符> 音符列表 = new ArrayList<>();
        // BPM=120 + PPQ=480 → 1 MC tick = 48 MIDI ticks，所有时值为整数
        音符列表.add(音符.of(0, 4, 1.0));
        音符列表.add(音符.of(7, 4, 0.8));
        音符列表.add(音符.of(12, 4, 0.5));
        音符列表.add(音符.of(24, 2, 1.0));
        return new 乐谱("测试MIDI", 作者标识, "测试者",
                System.currentTimeMillis(), 120, 音符列表);
    }

    @Nested
    @DisplayName("往返一致性")
    class 往返测试 {

        @Test
        @DisplayName("乐谱 → MIDI → 乐谱：音高/时值/力度/BPM 应保留")
        void 往返保留核心数据() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File midi文件 = new File(临时目录, "test.mid");

            MIDI导出器.导出(原始, midi文件);
            assertTrue(midi文件.exists());
            assertTrue(midi文件.length() > 0);

            乐谱 往返后 = MIDI解析器.解析(midi文件,
                    原始.获取作者标识(), 原始.获取作者名(), 原始.获取名称());

            assertEquals(原始.获取名称(), 往返后.获取名称());
            assertEquals(原始.获取速度BPM(), 往返后.获取速度BPM());
            assertEquals(原始.获取音符数量(), 往返后.获取音符数量());

            List<音符> 原始音符 = 原始.获取音符列表();
            List<音符> 往返音符 = 往返后.获取音符列表();
            for (int i = 0; i < 原始音符.size(); i++) {
                assertEquals(原始音符.get(i).获取音高(), 往返音符.get(i).获取音高(),
                        "音高 " + i + " 应保留");
                assertEquals(原始音符.get(i).获取力度(), 往返音符.get(i).获取力度(),
                        0.02, "力度 " + i + " 应保留（容差 0.02）");
                assertEquals(原始音符.get(i).获取时值Tick(), 往返音符.get(i).获取时值Tick(),
                        "时值 " + i + " 应保留");
            }
        }

        @Test
        @DisplayName("空乐谱导出应生成合法 MIDI 文件")
        void 空乐谱导出() throws IOException {
            UUID 作者标识 = UUID.randomUUID();
            乐谱 空乐谱 = new 乐谱("空", 作者标识, "测试者",
                    System.currentTimeMillis(), 120, new ArrayList<>());
            File midi文件 = new File(临时目录, "empty.mid");

            MIDI导出器.导出(空乐谱, midi文件);
            assertTrue(midi文件.exists());

            乐谱 往返后 = MIDI解析器.解析(midi文件, 作者标识, "测试者", "空");
            assertEquals(0, 往返后.获取音符数量());
            assertEquals(120, 往返后.获取速度BPM());
        }

        @Test
        @DisplayName("音高边界 0 和 24 应正确映射到 MIDI 54 和 78")
        void 音高边界映射() throws IOException {
            UUID 作者标识 = UUID.randomUUID();
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(0, 2, 1.0));   // F#3 = MIDI 54
            音符列表.add(音符.of(24, 1, 1.0));  // F#5 = MIDI 78
            乐谱 原始 = new 乐谱("边界", 作者标识, "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File midi文件 = new File(临时目录, "bounds.mid");

            MIDI导出器.导出(原始, midi文件);
            乐谱 往返后 = MIDI解析器.解析(midi文件, 作者标识, "测试者", "边界");

            assertEquals(2, 往返后.获取音符数量());
            assertEquals(0, 往返后.获取音符列表().get(0).获取音高());
            assertEquals(24, 往返后.获取音符列表().get(1).获取音高());
        }
    }

    @Nested
    @DisplayName("参数校验")
    class 参数校验测试 {

        @Test
        @DisplayName("解析 null 文件应抛 IOException")
        void 解析null文件抛异常() {
            assertThrows(IOException.class, () ->
                    MIDI解析器.解析((File) null, UUID.randomUUID(), "x", "y"));
        }

        @Test
        @DisplayName("导出 null 乐谱应抛 IOException")
        void 导出null乐谱抛异常() {
            File 文件 = new File(临时目录, "null.mid");
            assertThrows(IOException.class, () -> MIDI导出器.导出(null, 文件));
        }

        @Test
        @DisplayName("导出 null 目标文件应抛 IOException")
        void 导出null目标文件抛异常() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(IOException.class, () -> MIDI导出器.导出(乐谱, (File) null));
        }
    }
}
