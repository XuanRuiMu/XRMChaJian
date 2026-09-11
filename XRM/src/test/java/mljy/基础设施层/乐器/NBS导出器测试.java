package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.音符;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-07 NBS 导出器独立测试。
 * 验证导出方法签名、文件格式正确性、边界条件。
 * <p>
 * 重点覆盖 FP-03 修复：NBS 导出器乐器ID硬编码问题。
 * 验证传入的乐器ID被正确写入 NBS 文件的每个音符 instrument 字段。
 * 往返一致性由 {@link NBS解析器测试} 覆盖。
 */
@DisplayName("FP-07: NBS 导出器")
class NBS导出器测试 {

    @TempDir
    private File 临时目录;

    private 乐谱 创建测试乐谱() {
        List<音符> 音符列表 = new ArrayList<>();
        音符列表.add(音符.of(0, 4, 1.0));
        音符列表.add(音符.of(7, 4, 0.8));
        音符列表.add(音符.of(12, 4, 0.5));
        音符列表.add(音符.of(24, 1, 1.0));
        return new 乐谱("测试", UUID.randomUUID(), "测试者",
                System.currentTimeMillis(), 120, 音符列表);
    }

    private int 读Short(DataInputStream 输入) throws IOException {
        return 输入.readByte() & 0xFF | (输入.readByte() & 0xFF) << 8;
    }

    private int 读Int(DataInputStream 输入) throws IOException {
        return (输入.readByte() & 0xFF)
                | (输入.readByte() & 0xFF) << 8
                | (输入.readByte() & 0xFF) << 16
                | (输入.readByte() & 0xFF) << 24;
    }

    private String 读字符串(DataInputStream 输入) throws IOException {
        int 长度 = 读Int(输入);
        if (长度 <= 0) {
            return "";
        }
        byte[] 字节 = new byte[长度];
        输入.readFully(字节);
        return new String(字节, StandardCharsets.UTF_8);
    }

    private DataInputStream 跳过Header(DataInputStream 输入) throws IOException {
        读Short(输入); // magic length=0
        输入.readUnsignedByte(); // version
        输入.readUnsignedByte(); // 原版乐器数
        读Short(输入); // 总tick
        读Short(输入); // 层数
        读字符串(输入); // name
        读字符串(输入); // author
        读字符串(输入); // original author
        读字符串(输入); // description
        读Short(输入); // tempo
        输入.readByte(); // auto-save off
        输入.readByte(); // auto-save duration
        输入.readByte(); // time signature
        读Int(输入); // minutes spent
        读Int(输入); // left clicks
        读Int(输入); // right clicks
        读Int(输入); // note blocks added
        读Int(输入); // note blocks removed
        读字符串(输入); // midi sponge file
        输入.readByte(); // loop on finish
        输入.readByte(); // max loop count
        读Short(输入); // loop start tick
        return 输入;
    }

    private List<Integer> 提取所有音符乐器ID(byte[] 字节) throws IOException {
        DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
        跳过Header(输入);
        List<Integer> 乐器ID列表 = new ArrayList<>();
        while (true) {
            int tick跳跃 = 读Short(输入);
            if (tick跳跃 == 0) {
                break;
            }
            while (true) {
                int 层跳跃 = 读Short(输入);
                if (层跳跃 == 0) {
                    break;
                }
                int 乐器ID = 输入.readUnsignedByte();
                输入.readUnsignedByte(); // key
                输入.readUnsignedByte(); // velocity
                输入.readUnsignedByte(); // panning
                读Short(输入); // pitch
                乐器ID列表.add(乐器ID);
            }
        }
        return 乐器ID列表;
    }

    @Nested
    @DisplayName("正常流程")
    class 正常流程测试 {

        @Test
        @DisplayName("导出到 File 应生成非空 NBS 文件")
        void 导出到文件生成非空文件() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "normal.nbs");

            NBS导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
        }

        @Test
        @DisplayName("导出到 OutputStream 应写入非空字节")
        void 导出到流写入非空字节() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            ByteArrayOutputStream 输出 = new ByteArrayOutputStream();

            NBS导出器.导出(乐谱, (OutputStream) 输出);

            assertTrue(输出.size() > 0);
        }

        @Test
        @DisplayName("单音符乐谱应生成有效 NBS 文件")
        void 单音符乐谱导出() throws IOException {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 4, 1.0));
            乐谱 乐谱 = new 乐谱("单音", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "single.nbs");

            NBS导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
        }

        @Test
        @DisplayName("导出到 OutputStream 与 File 应生成相同字节")
        void 流与文件导出一致() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "consistency.nbs");
            ByteArrayOutputStream 流 = new ByteArrayOutputStream();

            NBS导出器.导出(乐谱, 文件);
            NBS导出器.导出(乐谱, (OutputStream) 流);

            byte[] 文件字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            byte[] 流字节 = 流.toByteArray();
            assertArrayEquals(文件字节, 流字节, "File 与 OutputStream 重载应生成相同字节");
        }
    }

    @Nested
    @DisplayName("边界值")
    class 边界值测试 {

        @Test
        @DisplayName("空乐谱导出应生成有效 NBS 文件")
        void 空乐谱导出有效文件() throws IOException {
            乐谱 空乐谱 = new 乐谱("空", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, new ArrayList<>());
            File 文件 = new File(临时目录, "empty.nbs");

            NBS导出器.导出(空乐谱, 文件);

            assertTrue(文件.exists());
            assertTrue(文件.length() > 0);
            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            assertTrue(乐器ID列表.isEmpty(), "空乐谱不应包含音符");
        }

        @Test
        @DisplayName("音高边界 0 和 24 应正确导出（NBS key 33 和 57）")
        void 音高边界导出() throws IOException {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(0, 2, 1.0));
            音符列表.add(音符.of(24, 2, 1.0));
            乐谱 乐谱 = new 乐谱("边界", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "bounds.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            assertEquals(2, 乐器ID列表.size(), "两个音符应生成两条乐器记录");
        }

        @Test
        @DisplayName("力度 0.0 应映射为 NBS velocity 0")
        void 力度零映射0() throws IOException {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 2, 0.0));
            乐谱 乐谱 = new 乐谱("零力度", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "zero_vel.nbs");

            NBS导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(
                    java.nio.file.Files.readAllBytes(文件.toPath()));
            assertEquals(1, 乐器ID列表.size(), "力度 0.0 的音符应被导出");
        }

        @Test
        @DisplayName("力度 1.0 应映射为 NBS velocity 100")
        void 力度满值映射100() throws IOException {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 2, 1.0));
            乐谱 乐谱 = new 乐谱("满力度", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "full_vel.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            跳过Header(输入);
            读Short(输入); // tick跳跃
            读Short(输入); // 层跳跃
            输入.readUnsignedByte(); // 乐器ID
            输入.readUnsignedByte(); // key
            int velocity = 输入.readUnsignedByte();
            assertEquals(100, velocity, "力度 1.0 应映射为 velocity=100");
        }

        @Test
        @DisplayName("时值 1 的音符应正确导出（最小正时值）")
        void 时值1导出() throws IOException {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 1, 1.0));
            乐谱 乐谱 = new 乐谱("时值1", UUID.randomUUID(), "测试者",
                    System.currentTimeMillis(), 120, 音符列表);
            File 文件 = new File(临时目录, "dur1.nbs");

            NBS导出器.导出(乐谱, 文件);

            assertTrue(文件.exists());
            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            assertEquals(1, 乐器ID列表.size(), "时值 1 的音符应被导出");
        }
    }

    @Nested
    @DisplayName("异常输入")
    class 异常输入测试 {

        @Test
        @DisplayName("导出 null 乐谱到 File 应抛 IOException（File 重载做 null 检查）")
        void null乐谱到File抛异常() {
            File 文件 = new File(临时目录, "null_score.nbs");
            assertThrows(IOException.class, () -> NBS导出器.导出(null, 文件));
        }

        @Test
        @DisplayName("导出 null 乐谱到 OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null乐谱到流抛NPE() {
            OutputStream 输出 = new ByteArrayOutputStream();
            assertThrows(NullPointerException.class, () -> NBS导出器.导出(null, 输出));
        }

        @Test
        @DisplayName("导出到 null File 应抛 IOException（File 重载做 null 检查）")
        void null文件抛异常() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(IOException.class, () -> NBS导出器.导出(乐谱, (File) null));
        }

        @Test
        @DisplayName("导出到 null OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null流抛NPE() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(NullPointerException.class, () -> NBS导出器.导出(乐谱, (OutputStream) null));
        }

        @Test
        @DisplayName("带乐器ID导出 null 乐谱到 File 应抛 IOException（File 重载做 null 检查）")
        void null乐谱带乐器ID抛异常() {
            File 文件 = new File(临时目录, "null_id.nbs");
            assertThrows(IOException.class, () -> NBS导出器.导出(null, 文件, 5));
        }

        @Test
        @DisplayName("带乐器ID导出到 null File 应抛 IOException（File 重载做 null 检查）")
        void null文件带乐器ID抛异常() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(IOException.class, () -> NBS导出器.导出(乐谱, (File) null, 5));
        }

        @Test
        @DisplayName("带乐器ID导出 null 乐谱到 OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null乐谱带乐器ID到流抛NPE() {
            OutputStream 输出 = new ByteArrayOutputStream();
            assertThrows(NullPointerException.class, () -> NBS导出器.导出(null, 输出, 5));
        }

        @Test
        @DisplayName("带乐器ID导出到 null OutputStream 应抛 NullPointerException（OutputStream 重载未做 null 检查）")
        void null流带乐器ID抛NPE() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(NullPointerException.class, () -> NBS导出器.导出(乐谱, (OutputStream) null, 5));
        }
    }

    @Nested
    @DisplayName("格式正确性")
    class 格式正确性测试 {

        @Test
        @DisplayName("导出文件首两字节应为 0（NBS 新格式 magic）")
        void 首字节为新格式() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "magic.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            assertEquals(0, 字节[0], "首字节应为 0（新格式 magic length 低字节）");
            assertEquals(0, 字节[1], "第二字节应为 0（新格式 magic length 高字节）");
        }

        @Test
        @DisplayName("导出文件版本号应为 4（OpenNBS v4）")
        void 版本号为4() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "version.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            读Short(输入); // magic
            int 版本 = 输入.readUnsignedByte();
            assertEquals(4, 版本, "NBS 版本应为 4");
        }

        @Test
        @DisplayName("导出文件原版乐器数应为 18")
        void 原版乐器数为18() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "instruments.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            读Short(输入); // magic
            输入.readUnsignedByte(); // version
            int 乐器数 = 输入.readUnsignedByte();
            assertEquals(18, 乐器数, "原版乐器数应为 18");
        }

        @Test
        @DisplayName("导出文件层信息应包含单层层名 Layer 0")
        void 层名为Layer0() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "layer.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            跳过Header(输入);
            // 跳过音符块
            while (true) {
                int tick跳跃 = 读Short(输入);
                if (tick跳跃 == 0) {
                    break;
                }
                while (true) {
                    int 层跳跃 = 读Short(输入);
                    if (层跳跃 == 0) {
                        break;
                    }
                    输入.readUnsignedByte(); // 乐器
                    输入.readUnsignedByte(); // key
                    输入.readUnsignedByte(); // velocity
                    输入.readUnsignedByte(); // panning
                    读Short(输入); // pitch
                }
            }
            String 层名 = 读字符串(输入);
            assertEquals("Layer 0", 层名, "单层导出层名应为 'Layer 0'");
        }

        @Test
        @DisplayName("导出文件末尾自定义乐器数应为 0")
        void 自定义乐器数为0() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "custom.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            跳过Header(输入);
            // 跳过音符块
            while (true) {
                int tick跳跃 = 读Short(输入);
                if (tick跳跃 == 0) {
                    break;
                }
                while (true) {
                    int 层跳跃 = 读Short(输入);
                    if (层跳跃 == 0) {
                        break;
                    }
                    输入.readUnsignedByte(); // 乐器
                    输入.readUnsignedByte(); // key
                    输入.readUnsignedByte(); // velocity
                    输入.readUnsignedByte(); // panning
                    读Short(输入); // pitch
                }
            }
            读字符串(输入); // layer name
            输入.readByte(); // layer velocity
            输入.readByte(); // layer panning
            int 自定义乐器数 = 输入.readUnsignedByte();
            assertEquals(0, 自定义乐器数, "自定义乐器数应为 0");
        }

        @Test
        @DisplayName("BPM=120 时 NBS tempo 应为 800（120×100/15）")
        void bpm120Tempo编码() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "tempo.nbs");

            NBS导出器.导出(乐谱, 文件);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(字节));
            读Short(输入); // magic
            输入.readUnsignedByte(); // version
            输入.readUnsignedByte(); // 原版乐器数
            读Short(输入); // 总tick
            读Short(输入); // 层数
            读字符串(输入); // name
            读字符串(输入); // author
            读字符串(输入); // original author
            读字符串(输入); // description
            int tempo = 读Short(输入);
            assertEquals(800, tempo, "BPM=120 时 NBS tempo 应为 800（120×100/15）");
        }
    }

    @Nested
    @DisplayName("FP-03 乐器ID传入场景")
    class FP03乐器ID测试 {

        @Test
        @DisplayName("乐器ID=5 导出后所有音符 instrument 字段应为 5")
        void 乐器ID5写入所有音符() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst5.nbs");

            NBS导出器.导出(乐谱, 文件, 5);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            assertEquals(4, 乐器ID列表.size(), "4 个音符应有 4 条乐器记录");
            for (int id : 乐器ID列表) {
                assertEquals(5, id, "所有音符的乐器ID应为 5");
            }
        }

        @Test
        @DisplayName("乐器ID=17（最大原版乐器）应正确写入")
        void 乐器ID17写入() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst17.nbs");

            NBS导出器.导出(乐谱, 文件, 17);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            for (int id : 乐器ID列表) {
                assertEquals(17, id, "所有音符的乐器ID应为 17");
            }
        }

        @Test
        @DisplayName("乐器ID=0（钢琴）应正确写入")
        void 乐器ID0写入() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst0.nbs");

            NBS导出器.导出(乐谱, 文件, 0);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            for (int id : 乐器ID列表) {
                assertEquals(0, id, "所有音符的乐器ID应为 0");
            }
        }

        @Test
        @DisplayName("乐器ID=-1 应被 clamp 到 0（钢琴）")
        void 乐器ID负1clamp到0() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst_neg.nbs");

            NBS导出器.导出(乐谱, 文件, -1);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            for (int id : 乐器ID列表) {
                assertEquals(0, id, "乐器ID=-1 应被 clamp 到 0");
            }
        }

        @Test
        @DisplayName("乐器ID=100 应被 clamp 到 17（最大原版乐器）")
        void 乐器ID100clamp到17() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst_over.nbs");

            NBS导出器.导出(乐谱, 文件, 100);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            for (int id : 乐器ID列表) {
                assertEquals(17, id, "乐器ID=100 应被 clamp 到 17");
            }
        }

        @Test
        @DisplayName("乐器ID=18 应被 clamp 到 17（边界 +1）")
        void 乐器ID18clamp到17() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件 = new File(临时目录, "inst_18.nbs");

            NBS导出器.导出(乐谱, 文件, 18);

            byte[] 字节 = java.nio.file.Files.readAllBytes(文件.toPath());
            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(字节);
            for (int id : 乐器ID列表) {
                assertEquals(17, id, "乐器ID=18 应被 clamp 到 17");
            }
        }

        @Test
        @DisplayName("旧版两参数导出 与 显式乐器ID=0 应生成完全相同字节")
        void 旧版与显式0一致() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件旧 = new File(临时目录, "legacy.nbs");
            File 文件0 = new File(临时目录, "explicit0.nbs");

            NBS导出器.导出(乐谱, 文件旧);
            NBS导出器.导出(乐谱, 文件0, 0);

            byte[] 字节旧 = java.nio.file.Files.readAllBytes(文件旧.toPath());
            byte[] 字节0 = java.nio.file.Files.readAllBytes(文件0.toPath());
            assertArrayEquals(字节0, 字节旧, "旧版两参数导出应与显式乐器ID=0 字节完全一致");
        }

        @Test
        @DisplayName("不同乐器ID导出 应生成不同字节")
        void 不同乐器ID生成不同字节() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            File 文件0 = new File(临时目录, "diff0.nbs");
            File 文件5 = new File(临时目录, "diff5.nbs");
            File 文件17 = new File(临时目录, "diff17.nbs");

            NBS导出器.导出(乐谱, 文件0, 0);
            NBS导出器.导出(乐谱, 文件5, 5);
            NBS导出器.导出(乐谱, 文件17, 17);

            byte[] 字节0 = java.nio.file.Files.readAllBytes(文件0.toPath());
            byte[] 字节5 = java.nio.file.Files.readAllBytes(文件5.toPath());
            byte[] 字节17 = java.nio.file.Files.readAllBytes(文件17.toPath());

            assertFalse(java.util.Arrays.equals(字节0, 字节5), "乐器ID 0 和 5 字节应不同");
            assertFalse(java.util.Arrays.equals(字节0, 字节17), "乐器ID 0 和 17 字节应不同");
            assertFalse(java.util.Arrays.equals(字节5, 字节17), "乐器ID 5 和 17 字节应不同");
        }

        @Test
        @DisplayName("带乐器ID导出到 OutputStream 应正确写入乐器ID")
        void 带乐器ID导出到流() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            ByteArrayOutputStream 输出 = new ByteArrayOutputStream();

            NBS导出器.导出(乐谱, (OutputStream) 输出, 7);

            List<Integer> 乐器ID列表 = 提取所有音符乐器ID(输出.toByteArray());
            for (int id : 乐器ID列表) {
                assertEquals(7, id, "OutputStream 重载也应正确写入乐器ID=7");
            }
        }

        @Test
        @DisplayName("旧版 OutputStream 两参数导出 与 显式乐器ID=0 应生成相同字节")
        void 旧版流与显式0一致() throws IOException {
            乐谱 乐谱 = 创建测试乐谱();
            ByteArrayOutputStream 流旧 = new ByteArrayOutputStream();
            ByteArrayOutputStream 流0 = new ByteArrayOutputStream();

            NBS导出器.导出(乐谱, (OutputStream) 流旧);
            NBS导出器.导出(乐谱, (OutputStream) 流0, 0);

            assertArrayEquals(流0.toByteArray(), 流旧.toByteArray(),
                    "旧版 OutputStream 两参数应与显式乐器ID=0 一致");
        }
    }
}
