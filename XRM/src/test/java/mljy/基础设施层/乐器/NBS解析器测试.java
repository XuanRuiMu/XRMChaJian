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
 * FP-07 NBS 解析器/导出器往返一致性测试。
 * 验证 乐谱 → NBS → 乐谱 的数据保真度。
 * <p>
 * 已知限制：NBS 格式只存储 tick 位置不存储时值，最后音符时值会变为 1。
 * 测试用例使用末尾时值=1 的乐谱以避免该限制影响。
 */
@DisplayName("FP-07: NBS 解析器/导出器")
class NBS解析器测试 {

    @TempDir
    private File 临时目录;

    private 乐谱 创建测试乐谱() {
        UUID 作者标识 = UUID.randomUUID();
        List<音符> 音符列表 = new ArrayList<>();
        // 4 个音符，末尾时值 = 1 以保证往返一致性
        音符列表.add(音符.of(0, 4, 1.0));
        音符列表.add(音符.of(7, 4, 0.8));
        音符列表.add(音符.of(12, 4, 0.5));
        音符列表.add(音符.of(24, 1, 1.0));
        return new 乐谱("测试NBS", 作者标识, "测试者",
                System.currentTimeMillis(), 120, 音符列表);
    }

    @Nested
    @DisplayName("往返一致性")
    class 往返测试 {

        @Test
        @DisplayName("乐谱 → NBS → 乐谱：音高/力度/BPM/名称应完全保留")
        void 往返保留核心数据() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File nbs文件 = new File(临时目录, "test.nbs");

            NBS导出器.导出(原始, nbs文件);
            assertTrue(nbs文件.exists());
            assertTrue(nbs文件.length() > 0);

            乐谱 往返后 = NBS解析器.解析(nbs文件,
                    原始.获取作者标识(), 原始.获取作者名(), 原始.获取名称());

            assertEquals(原始.获取名称(), 往返后.获取名称());
            assertEquals(原始.获取作者名(), 往返后.获取作者名());
            assertEquals(原始.获取速度BPM(), 往返后.获取速度BPM());
            assertEquals(原始.获取音符数量(), 往返后.获取音符数量());

            List<音符> 原始音符 = 原始.获取音符列表();
            List<音符> 往返音符 = 往返后.获取音符列表();
            for (int i = 0; i < 原始音符.size(); i++) {
                assertEquals(原始音符.get(i).获取音高(), 往返音符.get(i).获取音高(),
                        "音高 " + i + " 应保留");
                assertEquals(原始音符.get(i).获取力度(), 往返音符.get(i).获取力度(),
                        0.01, "力度 " + i + " 应保留");
                assertEquals(原始音符.get(i).获取时值Tick(), 往返音符.get(i).获取时值Tick(),
                        "时值 " + i + " 应保留");
            }
        }

        @Test
        @DisplayName("空乐谱导出应生成合法 NBS 文件")
        void 空乐谱导出() throws IOException {
            UUID 作者标识 = UUID.randomUUID();
            乐谱 空乐谱 = new 乐谱("空", 作者标识, "测试者",
                    System.currentTimeMillis(), 120, new ArrayList<>());
            File nbs文件 = new File(临时目录, "empty.nbs");

            NBS导出器.导出(空乐谱, nbs文件);
            assertTrue(nbs文件.exists());

            乐谱 往返后 = NBS解析器.解析(nbs文件, 作者标识, "测试者", "空");
            assertEquals(0, 往返后.获取音符数量());
            assertEquals(120, 往返后.获取速度BPM());
        }

        @Test
        @DisplayName("不同 BPM 值往返应保留")
        void 不同BPM往返() throws IOException {
            int[] bpm值表 = {60, 90, 120, 180, 240};
            for (int bpm : bpm值表) {
                UUID 作者标识 = UUID.randomUUID();
                List<音符> 音符列表 = new ArrayList<>();
                音符列表.add(音符.of(0, 2, 1.0));
                音符列表.add(音符.of(12, 1, 1.0));
                乐谱 原始 = new 乐谱("bpm" + bpm, 作者标识, "测试者",
                        System.currentTimeMillis(), bpm, 音符列表);
                File nbs文件 = new File(临时目录, "bpm" + bpm + ".nbs");

                NBS导出器.导出(原始, nbs文件);
                乐谱 往返后 = NBS解析器.解析(nbs文件, 作者标识, "测试者", "bpm" + bpm);

                assertEquals(bpm, 往返后.获取速度BPM(),
                        "BPM=" + bpm + " 往返应保留");
            }
        }
    }

    @Nested
    @DisplayName("参数校验")
    class 参数校验测试 {

        @Test
        @DisplayName("解析 null 文件应抛 IOException")
        void 解析null文件抛异常() {
            assertThrows(IOException.class, () ->
                    NBS解析器.解析((File) null, UUID.randomUUID(), "x", "y"));
        }

        @Test
        @DisplayName("导出 null 乐谱应抛 IOException")
        void 导出null乐谱抛异常() {
            File 文件 = new File(临时目录, "null.nbs");
            assertThrows(IOException.class, () -> NBS导出器.导出(null, 文件));
        }

        @Test
        @DisplayName("导出 null 目标文件应抛 IOException")
        void 导出null目标文件抛异常() {
            乐谱 乐谱 = 创建测试乐谱();
            assertThrows(IOException.class, () -> NBS导出器.导出(乐谱, (File) null));
        }
    }

    @Nested
    @DisplayName("FP-03 NBS导出器乐器ID测试")
    class FP03导出器乐器ID测试 {

        @Test
        @DisplayName("带乐器ID导出 应生成有效NBS文件且可被解析")
        void 带乐器ID导出有效文件() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File nbs文件 = new File(临时目录, "instrument5.nbs");

            NBS导出器.导出(原始, nbs文件, 5);
            assertTrue(nbs文件.exists());
            assertTrue(nbs文件.length() > 0);

            // 导出的文件应能被正确解析（音高/力度/BPM保留）
            乐谱 往返后 = NBS解析器.解析(nbs文件,
                    原始.获取作者标识(), 原始.获取作者名(), 原始.获取名称());
            assertEquals(原始.获取名称(), 往返后.获取名称());
            assertEquals(原始.获取速度BPM(), 往返后.获取速度BPM());
            assertEquals(原始.获取音符数量(), 往返后.获取音符数量());
        }

        @Test
        @DisplayName("不同乐器ID导出 应生成不同的NBS文件")
        void 不同乐器ID生成不同文件() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File 文件0 = new File(临时目录, "inst0.nbs");
            File 文件5 = new File(临时目录, "inst5.nbs");
            File 文件17 = new File(临时目录, "inst17.nbs");

            NBS导出器.导出(原始, 文件0, 0);
            NBS导出器.导出(原始, 文件5, 5);
            NBS导出器.导出(原始, 文件17, 17);

            byte[] 字节0 = java.nio.file.Files.readAllBytes(文件0.toPath());
            byte[] 字节5 = java.nio.file.Files.readAllBytes(文件5.toPath());
            byte[] 字节17 = java.nio.file.Files.readAllBytes(文件17.toPath());

            // 不同乐器ID应生成不同文件
            assertFalse(java.util.Arrays.equals(字节0, 字节5), "乐器ID 0 和 5 的文件应不同");
            assertFalse(java.util.Arrays.equals(字节0, 字节17), "乐器ID 0 和 17 的文件应不同");
            assertFalse(java.util.Arrays.equals(字节5, 字节17), "乐器ID 5 和 17 的文件应不同");
        }

        @Test
        @DisplayName("乐器ID越界应被 clamp 到 0-17 不抛异常")
        void 乐器ID越界被clamp() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File 文件负 = new File(临时目录, "inst_neg.nbs");
            File 文件超 = new File(临时目录, "inst_over.nbs");

            assertDoesNotThrow(() -> NBS导出器.导出(原始, 文件负, -1));
            assertDoesNotThrow(() -> NBS导出器.导出(原始, 文件超, 100));

            // clamp 后的文件应有效
            乐谱 解析负 = NBS解析器.解析(文件负, 原始.获取作者标识(), "x", "y");
            乐谱 解析超 = NBS解析器.解析(文件超, 原始.获取作者标识(), "x", "y");
            assertEquals(原始.获取音符数量(), 解析负.获取音符数量());
            assertEquals(原始.获取音符数量(), 解析超.获取音符数量());
        }

        @Test
        @DisplayName("旧版两参数导出 应委托新版方法（向后兼容，默认钢琴ID=0）")
        void 旧版两参数导出委托新版() throws IOException {
            乐谱 原始 = 创建测试乐谱();
            File 文件旧 = new File(临时目录, "legacy.nbs");
            File 文件0 = new File(临时目录, "explicit0.nbs");

            NBS导出器.导出(原始, 文件旧);
            NBS导出器.导出(原始, 文件0, 0);

            byte[] 字节旧 = java.nio.file.Files.readAllBytes(文件旧.toPath());
            byte[] 字节0 = java.nio.file.Files.readAllBytes(文件0.toPath());

            // 旧版（默认钢琴）应与显式传入 ID=0 的文件完全一致
            assertArrayEquals(字节0, 字节旧, "旧版两参数导出应与显式ID=0一致");
        }
    }
}
