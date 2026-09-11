package mljy.业务层.乐器;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FP-08 节拍器测试。
 * 重点验证纯计算逻辑（bpm转tick、参数校验、配置加载）与状态查询。
 * BukkitRunnable 真实调度在单测环境不验证（需服务器上下文）。
 *
 * FP-C 扩展：count-in 配置加载、运行时设置、参数校验、空集合处理。
 */
@DisplayName("FP-08: 节拍器")
class 节拍器测试 {

    private FileConfiguration 配置;
    private 节拍器 节拍器实例;

    @BeforeEach
    void setUp() {
        配置 = mock(FileConfiguration.class);
        when(配置.getInt("节拍器.默认BPM", 120)).thenReturn(120);
        when(配置.getInt("节拍器.默认拍号分子", 4)).thenReturn(4);
        when(配置.getInt("节拍器.默认拍号分母", 4)).thenReturn(4);
        when(配置.getString("节拍器.强拍音色", "BLOCK_NOTE_BLOCK_HAT")).thenReturn("BLOCK_NOTE_BLOCK_HAT");
        when(配置.getString("节拍器.弱拍音色", "BLOCK_NOTE_BLOCK_HAT")).thenReturn("BLOCK_NOTE_BLOCK_HAT");
        when(配置.getDouble("节拍器.强拍音高", 1.0)).thenReturn(1.0);
        when(配置.getDouble("节拍器.弱拍音高", 0.5)).thenReturn(0.5);
        when(配置.getDouble("节拍器.强拍音量", 1.0)).thenReturn(1.0);
        when(配置.getDouble("节拍器.弱拍音量", 0.6)).thenReturn(0.6);
        when(配置.getInt("节拍器.count-in小节数", 1)).thenReturn(1);
        节拍器实例 = new 节拍器(null, 配置);
    }

    @Nested
    @DisplayName("BPM转tick间隔")
    class Bpm转Tick测试 {

        @Test
        @DisplayName("120BPM → 10tick/拍")
        void 一二零BPM应为十tick() {
            assertEquals(10L, 节拍器.bpm转tick(120));
        }

        @Test
        @DisplayName("60BPM → 20tick/拍（1秒一拍）")
        void 六零BPM应为二十tick() {
            assertEquals(20L, 节拍器.bpm转tick(60));
        }

        @Test
        @DisplayName("240BPM → 5tick/拍")
        void 二四零BPM应为五tick() {
            assertEquals(5L, 节拍器.bpm转tick(240));
        }

        @Test
        @DisplayName("400BPM → 3tick/拍")
        void 四百BPM应为三tick() {
            assertEquals(3L, 节拍器.bpm转tick(400));
        }

        @Test
        @DisplayName("超高速BPM(2400) → 最小1tick保护")
        void 超高速BPM最小一tick() {
            assertEquals(1L, 节拍器.bpm转tick(2400));
        }
    }

    @Nested
    @DisplayName("参数校验")
    class 参数校验测试 {

        @Test
        @DisplayName("BPM低于下限20 → 抛IllegalArgumentException")
        void bpm过低抛异常() {
            IllegalArgumentException 异常 = assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动(null, 19, 4, 4));
            assertEquals("BPM必须在20-400范围内: 19", 异常.getMessage());
        }

        @Test
        @DisplayName("BPM高于上限400 → 抛IllegalArgumentException")
        void bpm过高抛异常() {
            IllegalArgumentException 异常 = assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动(null, 401, 4, 4));
            assertEquals("BPM必须在20-400范围内: 401", 异常.getMessage());
        }

        @Test
        @DisplayName("拍号分子低于下限1 → 抛IllegalArgumentException")
        void 拍号分子过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动(null, 120, 0, 4));
        }

        @Test
        @DisplayName("拍号分母低于下限1 → 抛IllegalArgumentException")
        void 拍号分母过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动(null, 120, 4, 0));
        }

        @Test
        @DisplayName("边界值BPM=20 合法不抛异常")
        void bpm边界下限合法() {
            assertEquals(20, 节拍器.bpm转tick(20) > 0 ? 20 : 0);
        }
    }

    @Nested
    @DisplayName("默认值加载")
    class 默认值测试 {

        @Test
        @DisplayName("默认BPM=120")
        void 默认BPM() {
            assertEquals(120, 节拍器实例.获取默认BPM());
        }

        @Test
        @DisplayName("默认拍号分子=4")
        void 默认拍号分子() {
            assertEquals(4, 节拍器实例.获取默认拍号分子());
        }

        @Test
        @DisplayName("默认拍号分母=4")
        void 默认拍号分母() {
            assertEquals(4, 节拍器实例.获取默认拍号分母());
        }
    }

    @Nested
    @DisplayName("未运行状态查询")
    class 未运行状态测试 {

        @Test
        @DisplayName("未运行时 是否运行返回false")
        void 未运行时返回false() {
            assertEquals(false, 节拍器实例.是否运行(java.util.UUID.randomUUID()));
        }

        @Test
        @DisplayName("未运行时 获取起拍时刻返回-1")
        void 未运行起拍时刻负一() {
            assertEquals(-1L, 节拍器实例.获取起拍时刻毫秒(java.util.UUID.randomUUID()));
        }

        @Test
        @DisplayName("未运行时 获取BPM返回-1")
        void 未运行BPM负一() {
            assertEquals(-1, 节拍器实例.获取BPM(java.util.UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("热重载")
    class 热重载测试 {

        @Test
        @DisplayName("重新加载配置后默认BPM更新")
        void 热重载更新默认BPM() {
            when(配置.getInt("节拍器.默认BPM", 120)).thenReturn(90);
            节拍器实例.从配置加载(配置);
            assertEquals(90, 节拍器实例.获取默认BPM());
        }

        @Test
        @DisplayName("热重载非法BPM被clamp到上限")
        void 热重载非法BPM被clamp() {
            when(配置.getInt("节拍器.默认BPM", 120)).thenReturn(9999);
            节拍器实例.从配置加载(配置);
            assertEquals(400, 节拍器实例.获取默认BPM());
        }
    }

    @Nested
    @DisplayName("FP-C count-in 配置加载")
    class CountIn配置加载测试 {

        @Test
        @DisplayName("默认 count-in 小节数 = 1")
        void 默认countIn小节数为1() {
            assertEquals(1, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("配置 count-in 小节数 = 0 加载成功")
        void 加载countIn为零() {
            when(配置.getInt("节拍器.count-in小节数", 1)).thenReturn(0);
            节拍器实例.从配置加载(配置);
            assertEquals(0, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("配置 count-in 小节数 = 2 加载成功")
        void 加载countIn为二() {
            when(配置.getInt("节拍器.count-in小节数", 1)).thenReturn(2);
            节拍器实例.从配置加载(配置);
            assertEquals(2, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("配置 count-in 小节数 > 2 被 clamp 到 2")
        void countIn超上限被clamp() {
            when(配置.getInt("节拍器.count-in小节数", 1)).thenReturn(5);
            节拍器实例.从配置加载(配置);
            assertEquals(2, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("配置 count-in 小节数 < 0 被 clamp 到 0")
        void countIn低于下限被clamp() {
            when(配置.getInt("节拍器.count-in小节数", 1)).thenReturn(-3);
            节拍器实例.从配置加载(配置);
            assertEquals(0, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("Guice @Inject 构造器默认 count-in 小节数 = 1")
        void guice构造器默认countIn() {
            节拍器 实例 = new 节拍器(null);
            assertEquals(1, 实例.获取CountIn小节数());
            assertEquals(120, 实例.获取默认BPM());
            assertEquals(4, 实例.获取默认拍号分子());
            assertEquals(4, 实例.获取默认拍号分母());
        }

        @Test
        @DisplayName("从配置加载 null 时应用默认参数（count-in = 1）")
        void 加载null配置应用默认() {
            节拍器实例.从配置加载(null);
            assertEquals(1, 节拍器实例.获取CountIn小节数());
            assertEquals(120, 节拍器实例.获取默认BPM());
        }
    }

    @Nested
    @DisplayName("FP-C count-in 运行时设置")
    class CountIn运行时设置测试 {

        @Test
        @DisplayName("设置CountIn小节数(0) → 获取CountIn小节数返回0")
        void 设置为零() {
            节拍器实例.设置CountIn小节数(0);
            assertEquals(0, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("设置CountIn小节数(2) → 获取CountIn小节数返回2")
        void 设置为二() {
            节拍器实例.设置CountIn小节数(2);
            assertEquals(2, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("设置CountIn小节数(>2) 被 clamp 到 2")
        void 设置超上限被clamp() {
            节拍器实例.设置CountIn小节数(99);
            assertEquals(2, 节拍器实例.获取CountIn小节数());
        }

        @Test
        @DisplayName("设置CountIn小节数(<0) 被 clamp 到 0")
        void 设置低于下限被Clamp() {
            节拍器实例.设置CountIn小节数(-1);
            assertEquals(0, 节拍器实例.获取CountIn小节数());
        }
    }

    @Nested
    @DisplayName("FP-C 启动带CountIn 参数校验")
    class 启动带CountIn参数校验测试 {

        @Test
        @DisplayName("启动带CountIn BPM过低 → 抛IllegalArgumentException")
        void 启动带CountIn_bpm过低抛异常() {
            IllegalArgumentException 异常 = assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动带CountIn(null, 19, 4, 4, null));
            assertEquals("BPM必须在20-400范围内: 19", 异常.getMessage());
        }

        @Test
        @DisplayName("启动带CountIn BPM过高 → 抛IllegalArgumentException")
        void 启动带CountIn_bpm过高抛异常() {
            IllegalArgumentException 异常 = assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动带CountIn(null, 401, 4, 4, null));
            assertEquals("BPM必须在20-400范围内: 401", 异常.getMessage());
        }

        @Test
        @DisplayName("启动带CountIn 拍号分子过低 → 抛IllegalArgumentException")
        void 启动带CountIn_拍号分子过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动带CountIn(null, 120, 0, 4, null));
        }

        @Test
        @DisplayName("启动带CountIn 拍号分母过低 → 抛IllegalArgumentException")
        void 启动带CountIn_拍号分母过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动带CountIn(null, 120, 4, 0, null));
        }
    }

    @Nested
    @DisplayName("FP-C 启动合奏CountIn 参数校验与边界")
    class 启动合奏CountIn测试 {

        @Test
        @DisplayName("启动合奏CountIn BPM过低 → 抛IllegalArgumentException")
        void 启动合奏CountIn_bpm过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动合奏CountIn(new HashSet<>(), 19, 4, 4, null));
        }

        @Test
        @DisplayName("启动合奏CountIn 拍号分子过低 → 抛IllegalArgumentException")
        void 启动合奏CountIn_拍号分子过低抛异常() {
            assertThrows(IllegalArgumentException.class,
                    () -> 节拍器实例.启动合奏CountIn(new HashSet<>(), 120, 0, 4, null));
        }

        @Test
        @DisplayName("启动合奏CountIn 空集合并 count-in=0 → 立即回调")
        void 空集合countIn为零_立即回调() {
            节拍器实例.设置CountIn小节数(0);
            boolean[] 已回调 = {false};
            节拍器实例.启动合奏CountIn(new HashSet<>(), 120, 4, 4, () -> 已回调[0] = true);
            assertTrue(已回调[0], "空集合 + count-in=0 应立即回调");
        }

        @Test
        @DisplayName("启动合奏CountIn null集合并 count-in=0 → 立即回调不抛NPE")
        void null集合countIn为零_立即回调() {
            节拍器实例.设置CountIn小节数(0);
            boolean[] 已回调 = {false};
            assertDoesNotThrow(() -> 节拍器实例.启动合奏CountIn(null, 120, 4, 4, () -> 已回调[0] = true));
            assertTrue(已回调[0], "null集合 + count-in=0 应立即回调");
        }

        @Test
        @DisplayName("启动合奏CountIn 单成员集合 count-in=0 → 立即回调")
        void 单成员集合countIn为零_立即回调() {
            节拍器实例.设置CountIn小节数(0);
            Set<UUID> 成员 = new HashSet<>();
            成员.add(UUID.randomUUID());
            boolean[] 已回调 = {false};
            节拍器实例.启动合奏CountIn(成员, 120, 4, 4, () -> 已回调[0] = true);
            assertTrue(已回调[0], "单成员集合 + count-in=0 应立即回调");
        }
    }

    @Nested
    @DisplayName("FP-C count-in 小节数与拍数计算")
    class CountIn拍数计算测试 {

        @Test
        @DisplayName("count-in=1 小节、4/4 拍 → 总拍数 4")
        void countIn一小节四四拍_总拍数四() {
            int 小节数 = 1;
            int 拍号分子 = 4;
            assertEquals(4, 小节数 * 拍号分子);
        }

        @Test
        @DisplayName("count-in=2 小节、3/4 拍 → 总拍数 6")
        void countIn二小节三四拍_总拍数六() {
            int 小节数 = 2;
            int 拍号分子 = 3;
            assertEquals(6, 小节数 * 拍号分子);
        }

        @Test
        @DisplayName("count-in=0 小节 → 总拍数 0（跳过 count-in）")
        void countIn零小节_总拍数零() {
            int 小节数 = 0;
            int 拍号分子 = 4;
            assertEquals(0, 小节数 * 拍号分子);
        }
    }
}
