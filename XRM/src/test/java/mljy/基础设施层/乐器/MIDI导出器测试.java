package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-17 MIDI 导出器独立测试。
 * 验证导出方法签名、文件格式正确性、边界条件与 BPM/tempo meta 写入。
 * 往返一致性由 {@link MIDI解析器测试} 覆盖，本类聚焦导出器自身行为。
 */
@DisplayName("FP-17: MIDI 导出器")
class MIDI导出器测试 {

    @TempDir
    private File 临时目录;

    private 乐谱 创建测试乐谱(int 速度BPM) {
        List<音符> 音符列表 = new ArrayList<>();
        音符列表.add(音符.of(0, 4, 1.0));
        音符列表.add(音符.of(7, 4, 0.8));
        音符列表.add(音符.of(12, 4, 0.5));
        音符列表.add(音符.of(24, 2, 1.0));
        return new 乐谱("测试", UUID.randomUUID(), "测试者",
                System.currentTimeMillis(), 速度BPM, 音符列表);
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程测试 {

        @Test
        @DisplayName("导出到 File 应生成非空 MIDI 文件")
        void 导出到文件生成非空文件() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "normal.mid");

            MIDI导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
        }

        @Test
        @DisplayName("导出到 OutputStream 应写入非空字节")
        void 导出到流写入非空字节() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            ByteArrayOutputStream 输出 = new ByteArrayOutputStream();

            MIDI导出器.导出(乐谱, (OutputStream) 输出);

            assertTrue(输出.size() > 0);
        }

        @Test
        @DisplayName("单音符乐谱应生成有效 MIDI 文件")
        void 单音符乐谱导出() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 4, 1.0));
            乐谱 乐谱 = new 乐谱("单音", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "single.mid");

            MIDI导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
        }

        @Test
        @DisplayName("导出到 OutputStream 与 File 应生成相同字节")
        void 流与文件导出一致() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "consistency.mid");
            ByteArrayOutputStream 流 = new ByteArrayOutputStream();

            MIDI导出器.导出(乐谱, 文件);
            MIDI导出器.导出(乐谱, (OutputStream) 流);

            byte[] 文件字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            byte[] 流字节 = 流.toByteArray();
            assertArrayEquals(文件字节, 流字节, "File 与 OutputStream 重载应生成相同字节");
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值测试 {

        @Test
        @DisplayName("空乐谱导出应生成有效 MIDI 文件（仅 tempo meta + EndOfTrack）")
        void 空乐谱导出有效文件() throws Exception {
            乐谱 空乐谱 = new 乐谱("空", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, new ArrayList<>());
            File 文件 = new File(临时目录, "empty.mid");

            MIDI导出器.导出(空乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
            Sequence 序列 = MidiSystem.getSequence(文件);
            assertNotNull(序列);
        }

        @Test
        @DisplayName("音高边界 0 和 24 应正确导出")
        void 音高边界导出() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(0, 2, 1.0));
            音符列表.add(音符.of(24, 2, 1.0));
            乐谱 乐谱 = new 乐谱("边界", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "bounds.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            int noteOnCount = 0;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof ShortMessage 短消息
                            && 短消息.getCommand() == ShortMessage.NOTE_ON) {
                        noteOnCount++;
                    }
                }
            }
            assertEquals(2, noteOnCount, "两个音符应生成两个 NOTE_ON 事件");
        }

        @Test
        @DisplayName("力度 0.0 应被强制为 MIDI velocity 1（避免被视为 NOTE_OFF）")
        void 力度零强制为一() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 2, 0.0));
            乐谱 乐谱 = new 乐谱("零力度", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "zero_velocity.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            boolean 存在力度1 = false;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof ShortMessage 短消息
                            && 短消息.getCommand() == ShortMessage.NOTE_ON
                            && 短消息.getData2() == 1) {
                        存在力度1 = true;
                        break;
                    }
                }
            }
            assertTrue(存在力度1, "力度 0.0 应被强制为 velocity=1");
        }

        @Test
        @DisplayName("力度 1.0 应映射为 MIDI velocity 127")
        void 力度满值映射127() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 2, 1.0));
            乐谱 乐谱 = new 乐谱("满力度", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "full_velocity.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            boolean 存在力度127 = false;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof ShortMessage 短消息
                            && 短消息.getCommand() == ShortMessage.NOTE_ON
                            && 短消息.getData2() == 127) {
                        存在力度127 = true;
                        break;
                    }
                }
            }
            assertTrue(存在力度127, "力度 1.0 应映射为 velocity=127");
        }

        @Test
        @DisplayName("时值 1 的音符应正确导出（最小正时值）")
        void 时值1导出() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 1, 1.0));
            乐谱 乐谱 = new 乐谱("时值1", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "dur1.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            int noteOnCount = 0;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof ShortMessage 短消息
                            && 短消息.getCommand() == ShortMessage.NOTE_ON) {
                        noteOnCount++;
                    }
                }
            }
            assertEquals(1, noteOnCount, "时值 1 的音符应正确导出");
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入测试 {

        @Test
        @DisplayName("导出 null 乐谱到 File 应抛 IOException（File 重载做 null 检查）")
        void null乐谱到File抛异常() {
            File 文件 = new File(临时目录, "null_score.mid");
            assertThrows(IOException.class, () -> MIDI导出器.导出(null, 文件));
        }

        @Test
        @DisplayName("导出到 null File 应抛 IOException（File 重载做 null 检查）")
        void null文件抛异常() {
            乐谱 乐谱 = 创建测试乐谱(120);
            assertThrows(IOException.class, () -> MIDI导出器.导出(乐谱, (File) null));
        }

        @Test
        @DisplayName("导出 null 乐谱到 OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null乐谱到流抛NPE() {
            OutputStream 输出 = new ByteArrayOutputStream();
            assertThrows(NullPointerException.class, () -> MIDI导出器.导出(null, 输出));
        }

        @Test
        @DisplayName("导出到 null OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null流抛NPE() {
            乐谱 乐谱 = 创建测试乐谱(120);
            assertThrows(NullPointerException.class, () -> MIDI导出器.导出(乐谱, (OutputStream) null));
        }
    }

    @Nested
    @DisplayName("格式正确性")
    class 格式正确性测试 {

        @Test
        @DisplayName("导出文件应可被 MidiSystem 读取为合法 Sequence")
        void 导出文件可被读取() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "valid.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            assertNotNull(序列);
            assertTrue(序列.getResolution() > 0, "PPQ 应为正数");
        }

        @Test
        @DisplayName("导出文件应包含 tempo meta 事件（0x51）")
        void 导出含TempoMeta() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "tempo.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            boolean 含tempo = false;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    MidiEvent 事件 = 轨道.get(i);
                    if (事件.getMessage() instanceof MetaMessage meta
                            && meta.getType() == 0x51) {
                        含tempo = true;
                        break;
                    }
                }
            }
            assertTrue(含tempo, "应包含 tempo meta 事件");
        }

        @Test
        @DisplayName("四音符乐谱应生成 4 个 NOTE_ON + 4 个 NOTE_OFF 事件")
        void 音符事件数量正确() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "events.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            int noteOn = 0;
            int noteOff = 0;
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof ShortMessage 短消息) {
                        if (短消息.getCommand() == ShortMessage.NOTE_ON
                                && 短消息.getData2() > 0) {
                            noteOn++;
                        } else if (短消息.getCommand() == ShortMessage.NOTE_OFF
                                || (短消息.getCommand() == ShortMessage.NOTE_ON
                                        && 短消息.getData2() == 0)) {
                            noteOff++;
                        }
                    }
                }
            }
            assertEquals(4, noteOn, "4 个音符应生成 4 个 NOTE_ON");
            assertEquals(4, noteOff, "4 个音符应生成 4 个 NOTE_OFF");
        }

        @Test
        @DisplayName("BPM=120 时 tempo meta 应编码为 500000 微秒每拍")
        void bpm120Tempo编码() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(120);
            File 文件 = new File(临时目录, "bpm120.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    MidiEvent 事件 = 轨道.get(i);
                    if (事件.getMessage() instanceof MetaMessage meta
                            && meta.getType() == 0x51) {
                        byte[] 数据 = meta.getData();
                        int tempo = ((数据[0] & 0xFF) << 16)
                                | ((数据[1] & 0xFF) << 8)
                                | (数据[2] & 0xFF);
                        assertEquals(500000, tempo, "BPM=120 时 tempo 应为 500000 μs/拍");
                        return;
                    }
                }
            }
            fail("未找到 tempo meta 事件");
        }
    }

    @Nested
    @DisplayName("BPM 特殊处理")
    class BPM特殊处理测试 {

        @Test
        @DisplayName("BPM=0 应被替换为 120（写TempoMeta 默认逻辑）")
        void bpm零替换为120() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(0);
            File 文件 = new File(临时目录, "bpm0.mid");

            MIDI导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            Sequence 序列 = MidiSystem.getSequence(文件);
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof MetaMessage meta
                            && meta.getType() == 0x51) {
                        byte[] 数据 = meta.getData();
                        int tempo = ((数据[0] & 0xFF) << 16)
                                | ((数据[1] & 0xFF) << 8)
                                | (数据[2] & 0xFF);
                        assertEquals(500000, tempo, "BPM=0 应被替换为 120（tempo=500000）");
                        return;
                    }
                }
            }
            fail("未找到 tempo meta 事件");
        }

        @Test
        @DisplayName("负 BPM 应被替换为 120")
        void 负bpm替换为120() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(-100);
            File 文件 = new File(临时目录, "neg_bpm.mid");

            MIDI导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            Sequence 序列 = MidiSystem.getSequence(文件);
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof MetaMessage meta
                            && meta.getType() == 0x51) {
                        byte[] 数据 = meta.getData();
                        int tempo = ((数据[0] & 0xFF) << 16)
                                | ((数据[1] & 0xFF) << 8)
                                | (数据[2] & 0xFF);
                        assertEquals(500000, tempo, "负 BPM 应被替换为 120");
                        return;
                    }
                }
            }
            fail("未找到 tempo meta 事件");
        }

        @Test
        @DisplayName("BPM=60 时 tempo meta 应编码为 1000000 微秒每拍")
        void bpm60Tempo编码() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(60);
            File 文件 = new File(临时目录, "bpm60.mid");

            MIDI导出器.导出(乐谱, 文件);

            Sequence 序列 = MidiSystem.getSequence(文件);
            for (Track 轨道 : 序列.getTracks()) {
                for (int i = 0; i < 轨道.size(); i++) {
                    if (轨道.get(i).getMessage() instanceof MetaMessage meta
                            && meta.getType() == 0x51) {
                        byte[] 数据 = meta.getData();
                        int tempo = ((数据[0] & 0xFF) << 16)
                                | ((数据[1] & 0xFF) << 8)
                                | (数据[2] & 0xFF);
                        assertEquals(1000000, tempo, "BPM=60 时 tempo 应为 1000000 μs/拍");
                        return;
                    }
                }
            }
            fail("未找到 tempo meta 事件");
        }
    }
}
