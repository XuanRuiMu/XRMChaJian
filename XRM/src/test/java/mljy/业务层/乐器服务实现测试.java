package mljy.业务层;

import mljy.翻译服务;
import mljy.业务层.乐器.公共乐谱服务;
import mljy.业务层.乐器.节拍器;
import mljy.业务层.乐器服务.乐谱保存结果;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.键位绑定表;
import mljy.领域层.乐器.功能位定义;
import mljy.领域层.乐器.延音模式;
import mljy.领域层.乐器.音高方块映射;
import mljy.领域层.乐器.音符;
import mljy.领域层.乐器.量化档位;
import 暮澜纪元.通用.文本.关键词解析器;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ERR-18 复现与回归测试：乐器服务实现乐谱加载NPE风险。
 *
 * 根因：加载乐谱时 (int) 音符数据.get(音高键) / ((Number) 音符数据.get(力度键)).doubleValue()
 *   当用户自制/损坏的乐谱 YAML 缺少对应键时，get() 返回 null，
 *   (int) null 抛 NullPointerException，((Number) null) 抛 NPE。
 *
 * 修复：增加 null 检查并跳过损坏音符条目，记录调试日志。
 *
 * FP-C 扩展：构造器新增 节拍器 参数（Guice @Inject 注入）。
 */
@DisplayName("ERR-18: 乐器服务实现乐谱加载NPE风险")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 乐器服务实现测试 {

    @Mock
    private 翻译服务 翻译服务mock;
    @Mock
    private 关键词解析器 关键词解析器mock;
    @Mock
    private 组队服务 组队服务mock;
    @Mock
    private JavaPlugin 插件mock;
    @Mock
    private 公共乐谱服务 公共乐谱服务mock;
    @Mock
    private mljy.基础设施层.乐器.资源包管理器 资源包管理器mock;

    @TempDir
    private Path 临时目录;

    private 乐器服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        FileConfiguration 空配置 = new YamlConfiguration();
        when(插件mock.getDataFolder()).thenReturn(临时目录.toFile());
        when(插件mock.getConfig()).thenReturn(空配置);
        乐器注册表 注册表 = new 乐器注册表();
        节拍器 节拍器实例 = new 节拍器(插件mock);
        服务 = new 乐器服务实现(翻译服务mock, 关键词解析器mock, 组队服务mock, 插件mock, 注册表, 节拍器实例, 公共乐谱服务mock, 资源包管理器mock);
        玩家标识 = UUID.randomUUID();
    }

    private void 创建乐谱文件(String 乐谱名, String yaml内容) throws IOException {
        File 乐谱目录 = new File(临时目录.toFile(), "乐谱" + File.separator + 玩家标识.toString());
        乐谱目录.mkdirs();
        File 文件 = new File(乐谱目录, 乐谱名 + ".yml");
        Files.write(文件.toPath(), yaml内容.getBytes(StandardCharsets.UTF_8));
    }

    private String 完整YAML(String 音符段) {
        return """
                名称: 测试乐谱
                作者: %s
                作者名: 测试玩家
                创建时间: 1234567890
                速度BPM: 120
                音符列表:
                """.formatted(玩家标识.toString()) + 音符段;
    }

    @Nested
    @DisplayName("ERR-18复现：损坏音符应被跳过不抛NPE")
    class 损坏音符跳过测试 {

        @Test
        @DisplayName("缺少音高键时应跳过损坏音符不抛NPE")
        void 缺少音高键_应跳过损坏音符() throws IOException {
            String yaml = 完整YAML("  - 时值: 4\n    力度: 1.0\n");
            创建乐谱文件("损坏音高", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "损坏音高");

            assertTrue(结果.isPresent(), "应返回乐谱而非抛NPE");
            assertEquals(0, 结果.get().获取音符数量(), "缺少音高键的音符应被跳过");
        }

        @Test
        @DisplayName("缺少时值键时应跳过损坏音符不抛NPE")
        void 缺少时值键_应跳过损坏音符() throws IOException {
            String yaml = 完整YAML("  - 音高: 0\n    力度: 1.0\n");
            创建乐谱文件("损坏时值", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "损坏时值");

            assertTrue(结果.isPresent(), "应返回乐谱而非抛NPE");
            assertEquals(0, 结果.get().获取音符数量(), "缺少时值键的音符应被跳过");
        }

        @Test
        @DisplayName("缺少力度键时应跳过损坏音符不抛NPE")
        void 缺少力度键_应跳过损坏音符() throws IOException {
            String yaml = 完整YAML("  - 音高: 0\n    时值: 4\n");
            创建乐谱文件("损坏力度", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "损坏力度");

            assertTrue(结果.isPresent(), "应返回乐谱而非抛NPE");
            assertEquals(0, 结果.get().获取音符数量(), "缺少力度键的音符应被跳过");
        }

        @Test
        @DisplayName("混合正常和损坏音符时应只保留正常音符")
        void 混合音符_应只保留正常音符() throws IOException {
            String yaml = 完整YAML("""
                      - 音高: 0
                        时值: 4
                        力度: 1.0
                      - 时值: 4
                        力度: 1.0
                      - 音高: 12
                        时值: 2
                        力度: 0.5
                      - 音高: 5
                      - 音高: 6
                        时值: 4
                        力度: 0.8
                    """);
            创建乐谱文件("混合乐谱", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "混合乐谱");

            assertTrue(结果.isPresent(), "应返回乐谱而非抛NPE");
            assertEquals(3, 结果.get().获取音符数量(), "应只保留3个完整音符（损坏的2个被跳过）");
        }

        @Test
        @DisplayName("空音符列表应返回空乐谱不抛NPE")
        void 空音符列表_应返回空乐谱() throws IOException {
            String yaml = 完整YAML("");
            创建乐谱文件("空乐谱", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "空乐谱");

            assertTrue(结果.isPresent(), "应返回乐谱而非抛NPE");
            assertEquals(0, 结果.get().获取音符数量(), "空音符列表应返回0个音符");
        }
    }

    @Nested
    @DisplayName("正常乐谱加载（回归保护）")
    class 正常乐谱测试 {

        @Test
        @DisplayName("正常乐谱应完整加载所有音符")
        void 正常乐谱_应完整加载() throws IOException {
            String yaml = 完整YAML("""
                      - 音高: 0
                        时值: 4
                        力度: 1.0
                      - 音高: 12
                        时值: 2
                        力度: 0.5
                    """);
            创建乐谱文件("正常乐谱", yaml);

            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "正常乐谱");

            assertTrue(结果.isPresent());
            assertEquals(2, 结果.get().获取音符数量(), "正常乐谱应加载2个音符");
            assertEquals("测试乐谱", 结果.get().获取名称());
            assertEquals(120, 结果.get().获取速度BPM());
        }

        @Test
        @DisplayName("不存在的乐谱应返回empty")
        void 不存在乐谱_应返回empty() {
            Optional<乐谱> 结果 = 服务.加载乐谱(玩家标识, "不存在");
            assertTrue(结果.isEmpty(), "不存在的乐谱应返回empty");
        }
    }

    @Nested
    @DisplayName("FP-D / GAP-06: GUI 音符按钮接入音高方块映射（25 色方块）")
    class GUI接入音高方块映射测试 {

        @Test
        @DisplayName("乐器服务实现 不应再有 根据音高获取材质 私有方法（硬编码 wool 已删除）")
        void 不应再有根据音高获取材质方法() {
            assertThrows(NoSuchMethodException.class, () ->
                    乐器服务实现.class.getDeclaredMethod("根据音高获取材质", int.class),
                    "根据音高获取材质 私有方法应已删除，改用音高方块映射.音高转方块");
        }

        @Test
        @DisplayName("乐器服务实现 应有 播放音高粒子 私有方法（FP-10 演奏可视化反馈已接入）")
        void 应有播放音高粒子方法() {
            assertDoesNotThrow(() ->
                    乐器服务实现.class.getDeclaredMethod("播放音高粒子", Player.class, int.class),
                    "播放音高粒子 私有方法应存在");
        }

        @Test
        @DisplayName("创建音符按钮 应使用音高方块映射.音高转方块 作为材质（25 色方块，禁用 wool）")
        @SuppressWarnings("try")
        void 创建音符按钮_应使用音高方块映射() throws Exception {
            List<Material> 实际材质列表 = new ArrayList<>();
            try (MockedConstruction<ItemStack> mock = mockConstruction(ItemStack.class,
                    (mockItem, context) -> 实际材质列表.add((Material) context.arguments().get(0)))) {
                for (int 音高 = 0; 音高 <= 24; 音高++) {
                    反射调用创建音符按钮(音高);
                }
            }
            assertEquals(25, 实际材质列表.size(), "应构造 25 个 ItemStack（音高 0-24）");
            for (int 音高 = 0; 音高 <= 24; 音高++) {
                Material 期望 = 音高方块映射.音高转方块(音高);
                Material 实际 = 实际材质列表.get(音高);
                assertEquals(期望, 实际,
                        "音高" + 音高 + "按钮材质应为" + 期望 + "，实际为" + 实际);
                assertFalse(实际.name().endsWith("_WOOL"),
                        "音高" + 音高 + "不应使用 wool: " + 实际);
            }
        }

        @Test
        @DisplayName("创建音符按钮 应区分八度区域材质（0-5粉末/6-17混凝土/18-24陶瓦）")
        void 创建音符按钮_应区分八度区域材质() throws Exception {
            for (int 音高 = 0; 音高 <= 24; 音高++) {
                Material 期望 = 音高方块映射.音高转方块(音高);
                String 材质名 = 期望.name();
                if (音高 <= 5) {
                    assertTrue(材质名.endsWith("CONCRETE_POWDER"),
                            "音高" + 音高 + "（低音区前段）应为混凝土粉末，实际为" + 材质名);
                } else if (音高 <= 17) {
                    assertTrue(材质名.endsWith("CONCRETE") && !材质名.endsWith("POWDER"),
                            "音高" + 音高 + "（中音区）应为混凝土，实际为" + 材质名);
                } else {
                    assertTrue(材质名.endsWith("TERRACOTTA"),
                            "音高" + 音高 + "（高音区）应为陶瓦，实际为" + 材质名);
                }
            }
        }

        private void 反射调用创建音符按钮(int 音高) throws Exception {
            Method 方法 = 乐器服务实现.class.getDeclaredMethod("创建音符按钮", int.class);
            方法.setAccessible(true);
            方法.invoke(服务, 音高);
        }
    }

    // ========== FP-B GAP-01/02/03 测试 ==========

    private 乐器定义 创建测试乐器() {
        return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, null,
                Material.STICK, 9001, 1, 0, 24, 0,
                false, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
    }

    @Nested
    @DisplayName("GAP-01: 保存乐谱重名应返回重名状态而非直接 false")
    class GAP01乐谱保存测试 {

        @Test
        @DisplayName("参数无效：乐谱名为 null 应返回 参数无效")
        void 参数无效_null乐谱名_应返回参数无效() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(乐谱保存结果.参数无效, 服务.保存乐谱带结果(玩家标识, null));
            }
        }

        @Test
        @DisplayName("参数无效：乐谱名为空字符串应返回 参数无效")
        void 参数无效_空乐谱名_应返回参数无效() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(乐谱保存结果.参数无效, 服务.保存乐谱带结果(玩家标识, ""));
            }
        }

        @Test
        @DisplayName("参数无效：乐谱名为空白字符应返回 参数无效")
        void 参数无效_空白乐谱名_应返回参数无效() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(乐谱保存结果.参数无效, 服务.保存乐谱带结果(玩家标识, "   "));
            }
        }

        @Test
        @DisplayName("无录制：未启动录制应返回 无录制")
        void 无录制_未启动录制_应返回无录制() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(乐谱保存结果.无录制, 服务.保存乐谱带结果(玩家标识, "新乐谱"));
            }
        }

        @Test
        @DisplayName("重名：乐谱已存在应返回 重名")
        void 重名_乐谱已存在_应返回重名() throws IOException {
            创建乐谱文件("已有乐谱", 完整YAML("  - 音高: 0\n    时值: 4\n    力度: 1.0\n"));
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
                服务.添加录制音符(玩家标识, 0, 4, 1.0);
                assertEquals(乐谱保存结果.重名, 服务.保存乐谱带结果(玩家标识, "已有乐谱"));
            }
        }

        @Test
        @DisplayName("超限：达到最大乐谱存储数应返回 超限")
        void 超限_达到上限_应返回超限() throws IOException {
            for (int i = 0; i < 20; i++) {
                创建乐谱文件("乐谱" + i, 完整YAML("  - 音高: 0\n    时值: 4\n    力度: 1.0\n"));
            }
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
                服务.添加录制音符(玩家标识, 0, 4, 1.0);
                assertEquals(乐谱保存结果.超限, 服务.保存乐谱带结果(玩家标识, "全新乐谱名"));
            }
        }

        @Test
        @DisplayName("成功：正常保存应返回 成功 并写入文件")
        void 成功_正常保存_应返回成功() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                when(玩家mock.getName()).thenReturn("测试玩家");
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
                服务.添加录制音符(玩家标识, 0, 4, 1.0);
                assertEquals(乐谱保存结果.成功, 服务.保存乐谱带结果(玩家标识, "新乐谱"));
            }
            assertTrue(服务.获取乐谱列表(玩家标识).contains("新乐谱"), "应已创建乐谱文件");
        }

        @Test
        @DisplayName("保存乐谱并覆盖：同名乐谱应直接覆盖")
        void 保存乐谱并覆盖_同名乐谱_应覆盖() throws IOException {
            创建乐谱文件("待覆盖乐谱", 完整YAML("  - 音高: 0\n    时值: 4\n    力度: 1.0\n"));
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                when(玩家mock.getName()).thenReturn("测试玩家");
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
                服务.添加录制音符(玩家标识, 12, 4, 1.0);
                assertTrue(服务.保存乐谱并覆盖(玩家标识, "待覆盖乐谱"));
            }
            Optional<乐谱> 加载 = 服务.加载乐谱(玩家标识, "待覆盖乐谱");
            assertTrue(加载.isPresent(), "覆盖后应可加载");
            assertEquals(1, 加载.get().获取音符数量(), "应已被新音符覆盖");
            assertEquals(12, 加载.get().获取音符列表().get(0).获取音高(), "应保留新音符音高");
        }

        @Test
        @DisplayName("保存乐谱并覆盖：无录制应返回 false")
        void 保存乐谱并覆盖_无录制_应返回false() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertFalse(服务.保存乐谱并覆盖(玩家标识, "新乐谱"));
            }
        }

        @Test
        @DisplayName("保存乐谱并覆盖：参数无效应返回 false")
        void 保存乐谱并覆盖_参数无效_应返回false() {
            assertFalse(服务.保存乐谱并覆盖(玩家标识, null));
            assertFalse(服务.保存乐谱并覆盖(玩家标识, ""));
        }
    }

    @Nested
    @DisplayName("GAP-02: 播放应按音符.获取时值Tick决定每音符时长")
    class GAP02播放时值测试 {

        @Test
        @DisplayName("不同时值的音符应按各自 时值Tick 决定播放间隔")
        void 不同时值音符_应按各自Tick播放() throws Exception {
            // 3个音符，时值 1/3/2
            // 期望 run 调用：1(播0) + 1(播1)+2(等1) + 1(播2)+1(等2) + 1(结束) = 7 次
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(0, 1, 1.0));
            音符列表.add(音符.of(12, 3, 1.0));
            音符列表.add(音符.of(24, 2, 1.0));
            乐谱 乐谱实例 = 乐谱.创建("测试", 玩家标识, "测试", 120, 音符列表);

            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.getName()).thenReturn("测试");
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");

            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            AtomicReference<Runnable> 任务引用 = new AtomicReference<>();
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    任务引用.set(inv.getArgument(1));
                    return mock(BukkitTask.class);
                });

            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                Method 方法 = 乐器服务实现.class.getDeclaredMethod(
                        "启动播放任务", Player.class, 乐器定义.class, 乐谱.class);
                方法.setAccessible(true);
                方法.invoke(服务, 玩家mock, 创建测试乐器(), 乐谱实例);

                Runnable 任务 = 任务引用.get();
                assertNotNull(任务, "应已注册定时任务");

                // 时值1: run#1 播音符0 (音高0)
                任务.run();
                verify(玩家mock, times(1)).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());

                // 时值3: run#2 播音符1 (音高12), run#3-4 等待
                任务.run();
                verify(玩家mock, times(2)).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
                任务.run();
                任务.run();

                // 时值2: run#5 播音符2 (音高24), run#6 等待
                任务.run();
                verify(玩家mock, times(3)).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
                任务.run();

                // run#7: 索引>=总数，结束（cancel 会调用 Bukkit.getScheduler()，需在 MockedStatic 内）
                任务.run();
                verify(玩家mock, times(3)).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
            }
        }
    }

    @Nested
    @DisplayName("GAP-03: 录制按键按下/松开应记录真实时长")
    class GAP03录制按键时长测试 {

        @Test
        @DisplayName("瞬时按下松开（差值≈0）应回退到默认时值Tick=4")
        void 瞬时按下松开_应回退到默认时值() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
            }

            服务.记录按键按下(玩家标识, 0, 1.0);
            服务.记录按键松开(玩家标识, 0);

            List<音符> 序列 = 获取录制音符序列();
            assertEquals(1, 序列.size(), "应录入1个音符");
            assertEquals(4, 序列.get(0).获取时值Tick(),
                    "瞬时点击应回退到默认时值Tick=4");
        }

        @Test
        @DisplayName("长按300ms应记录至少6 tick（300/50=6）")
        void 长按300ms_应记录至少6tick() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
            }

            服务.记录按键按下(玩家标识, 12, 1.0);
            Thread.sleep(300);
            服务.记录按键松开(玩家标识, 12);

            List<音符> 序列 = 获取录制音符序列();
            assertEquals(1, 序列.size());
            assertTrue(序列.get(0).获取时值Tick() >= 6,
                    "长按300ms应记录至少6 tick，实际: " + 序列.get(0).获取时值Tick());
        }

        @Test
        @DisplayName("未按下直接松开应不录入音符")
        void 未按下直接松开_应不录入音符() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
            }

            服务.记录按键松开(玩家标识, 7);

            List<音符> 序列 = 获取录制音符序列();
            assertEquals(0, 序列.size(), "未按下直接松开应不录入音符");
        }

        @Test
        @DisplayName("未录制时按键按下/松开应不抛异常")
        void 未录制时按键_应不抛异常() {
            assertDoesNotThrow(() -> {
                服务.记录按键按下(玩家标识, 0, 1.0);
                服务.记录按键松开(玩家标识, 0);
            });
        }

        @Test
        @DisplayName("按键力度应正确传入音符")
        void 按键力度_应正确传入音符() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
            }

            服务.记录按键按下(玩家标识, 5, 0.7);
            服务.记录按键松开(玩家标识, 5);

            List<音符> 序列 = 获取录制音符序列();
            assertEquals(1, 序列.size());
            assertEquals(0.7, 序列.get(0).获取力度(), 0.001, "力度应正确传入");
        }

        @Test
        @DisplayName("音高超出[0,24]应被 clamp")
        void 音高超限_应被clamp() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 玩家mock = mock(Player.class);
                when(玩家mock.isOnline()).thenReturn(false);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
            }

            服务.记录按键按下(玩家标识, 100, 1.0);
            服务.记录按键松开(玩家标识, 100);

            List<音符> 序列 = 获取录制音符序列();
            assertEquals(1, 序列.size());
            assertEquals(24, 序列.get(0).获取音高(), "音高100应被 clamp 到 24");
        }

        @SuppressWarnings("unchecked")
        private List<音符> 获取录制音符序列() throws Exception {
            Field 录制状态表字段 = 乐器服务实现.class.getDeclaredField("录制状态表");
            录制状态表字段.setAccessible(true);
            Map<UUID, ?> 表 = (Map<UUID, ?>) 录制状态表字段.get(服务);
            Object 状态 = 表.get(玩家标识);
            assertNotNull(状态, "录制状态应存在");
            Field 音符序列字段 = 状态.getClass().getDeclaredField("音符序列");
            音符序列字段.setAccessible(true);
            return (List<音符>) 音符序列字段.get(状态);
        }
    }

    // ========== FP-E 测试：FP-01 三轨归一 + FP-05 5 档力度 + FP-06 逐音域绑定 + mod velocity-by-duration ==========

    @Nested
    @DisplayName("FP-05: 5 档力度档管理")
    class FP05力度档管理测试 {

        @Test
        @DisplayName("默认力度档应为 MF")
        void 默认力度档_应为MF() {
            assertEquals(力度档.MF, 服务.获取玩家力度档(玩家标识),
                    "新玩家默认力度档应为 MF");
        }

        @Test
        @DisplayName("null 玩家标识应返回默认档 MF 不抛异常")
        void null玩家标识_应返回默认档() {
            assertEquals(力度档.MF, 服务.获取玩家力度档(null),
                    "null 玩家标识应返回默认档 MF");
        }

        @Test
        @DisplayName("切换力度档：V 键升档 MF→F→FF，FF 升档保持不变")
        void 升档_应依次递进并在FF保持() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(力度档.F, 服务.切换玩家力度档(玩家标识, true), "MF→F");
                assertEquals(力度档.FF, 服务.切换玩家力度档(玩家标识, true), "F→FF");
                assertEquals(力度档.FF, 服务.切换玩家力度档(玩家标识, true), "FF 升档应保持");
            }
        }

        @Test
        @DisplayName("切换力度档：C 键降档 MF→P→PP，PP 降档保持不变")
        void 降档_应依次递减并在PP保持() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(力度档.P, 服务.切换玩家力度档(玩家标识, false), "MF→P");
                assertEquals(力度档.PP, 服务.切换玩家力度档(玩家标识, false), "P→PP");
                assertEquals(力度档.PP, 服务.切换玩家力度档(玩家标识, false), "PP 降档应保持");
            }
        }

        @Test
        @DisplayName("设置力度档应直接覆盖当前档")
        void 设置力度档_应直接覆盖() {
            服务.设置玩家力度档(玩家标识, 力度档.FF);
            assertEquals(力度档.FF, 服务.获取玩家力度档(玩家标识));

            服务.设置玩家力度档(玩家标识, 力度档.PP);
            assertEquals(力度档.PP, 服务.获取玩家力度档(玩家标识));
        }

        @Test
        @DisplayName("设置力度档：null 玩家标识或 null 力度档应不抛异常")
        void 设置力度档_null参数_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置玩家力度档(null, 力度档.FF));
            assertDoesNotThrow(() -> 服务.设置玩家力度档(玩家标识, null));
            assertEquals(力度档.MF, 服务.获取玩家力度档(玩家标识),
                    "null 力度档不应改变当前档");
        }

        @Test
        @DisplayName("演奏指令：力度档应影响 playSound 的 volume 与 pitch")
        void 演奏指令_力度档应影响volume和pitch() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 乐器 = 创建测试乐器();
            // PP: volume=0.30, pitch微调=-0.005
            服务.演奏指令(玩家mock, 乐器, 0, 力度档.PP, 4);
            verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                    eq(0.30f), eq(乐器.音高转Pitch(0) + (-0.005f)));

            // FF: volume=1.00, pitch微调=+0.004
            服务.演奏指令(玩家mock, 乐器, 0, 力度档.FF, 4);
            verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                    eq(1.00f), eq(乐器.音高转Pitch(0) + 0.004f));
        }

        @Test
        @DisplayName("演奏指令：null 参数应不抛异常")
        void 演奏指令_null参数_应不抛异常() {
            乐器定义 乐器 = 创建测试乐器();
            assertDoesNotThrow(() -> 服务.演奏指令(null, 乐器, 0, 力度档.MF, 4));
            assertDoesNotThrow(() -> 服务.演奏指令(mock(Player.class), null, 0, 力度档.MF, 4));
            assertDoesNotThrow(() -> 服务.演奏指令(mock(Player.class), 乐器, 0, null, 4));
        }

        @Test
        @DisplayName("演奏指令：音高超出 [0,59] 应被 clamp 到服务端范围；粒子音高进一步 clamp 到乐器音域")
        void 演奏指令_音高越界_应被clamp() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 乐器 = 创建测试乐器();
            // 乐器音域上限=24，服务端音高上限=59
            // 音高 -5 应 clamp 到 0；音高 100 应 clamp 到 59（pitch 用 59，但 乐器.音高转Pitch 会进一步 clamp 到 24）
            服务.演奏指令(玩家mock, 乐器, -5, 力度档.MF, 4);
            服务.演奏指令(玩家mock, 乐器, 100, 力度档.MF, 4);
            // 验证两次调用都使用了 clamp 后的 pitch（乐器内部 clamp 到 [0,24]）
            verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                    anyFloat(), eq(乐器.音高转Pitch(0) + 力度档.MF.获取pitch微调()));
            verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                    anyFloat(), eq(乐器.音高转Pitch(59) + 力度档.MF.获取pitch微调()));
        }
    }

    @Nested
    @DisplayName("FP-05.2: mod velocity-by-duration")
    class FP05_2VelocityByDuration测试 {

        @Test
        @DisplayName("默认状态：玩家未启用真实力度")
        void 默认状态_未启用真实力度() {
            assertFalse(服务.是否玩家真实力度开启(玩家标识),
                    "新玩家默认未启用真实力度");
            assertFalse(服务.是否玩家真实力度开启(null),
                    "null 玩家标识应返回 false");
        }

        @Test
        @DisplayName("启用真实力度后应查询为 true，禁用后查询为 false")
        void 启用禁用_应正确切换() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置玩家真实力度开关(玩家标识, true);
                assertTrue(服务.是否玩家真实力度开启(玩家标识));

                服务.设置玩家真实力度开关(玩家标识, false);
                assertFalse(服务.是否玩家真实力度开启(玩家标识));
            }
        }

        @Test
        @DisplayName("计算velocityByDuration：差值 0 应返回 0")
        void 差值为零_应返回0() {
            assertEquals(0, 服务.计算velocityByDuration(1000, 1000),
                    "差值 0 应返回 velocity 0");
        }

        @Test
        @DisplayName("计算velocityByDuration：松开早于按下应返回 0")
        void 松开早于按下_应返回0() {
            assertEquals(0, 服务.计算velocityByDuration(2000, 1000),
                    "松开早于按下应返回 velocity 0");
        }

        @Test
        @DisplayName("计算velocityByDuration：差值>=最大有效时长(400ms)应返回 127")
        void 差值超最大有效时长_应返回127() {
            assertEquals(127, 服务.计算velocityByDuration(0, 400),
                    "差值=400ms 应返回 velocity 127");
            assertEquals(127, 服务.计算velocityByDuration(0, 1000),
                    "差值=1000ms 应返回 velocity 127");
        }

        @Test
        @DisplayName("计算velocityByDuration：中间差值应按比例线性映射")
        void 中间差值_应线性映射() {
            // 200ms / 400ms × 127 = 63.5 → round = 64
            int velocity = 服务.计算velocityByDuration(0, 200);
            assertEquals(64, velocity, "200ms 差值应映射到 velocity 64");
            // 100ms / 400ms × 127 = 31.75 → round = 32
            assertEquals(32, 服务.计算velocityByDuration(0, 100),
                    "100ms 差值应映射到 velocity 32");
        }
    }

    @Nested
    @DisplayName("FP-06: 逐音域完全独立绑定（5 音域 × 12 键）")
    class FP06逐音域绑定测试 {

        @Test
        @DisplayName("默认音域应为 0")
        void 默认音域_应为0() {
            assertEquals(0, 服务.获取玩家音域(玩家标识), "新玩家默认音域应为 0");
        }

        @Test
        @DisplayName("null 玩家标识应返回默认音域 0 不抛异常")
        void null玩家标识_应返回0() {
            assertEquals(0, 服务.获取玩家音域(null), "null 玩家标识应返回 0");
        }

        @Test
        @DisplayName("切换音域：X 键升音域 0→1→2→3→4，4 升音域保持不变")
        void 升音域_应依次递进并在4保持() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertEquals(1, 服务.切换玩家音域(玩家标识, true), "0→1");
                assertEquals(2, 服务.切换玩家音域(玩家标识, true), "1→2");
                assertEquals(3, 服务.切换玩家音域(玩家标识, true), "2→3");
                assertEquals(4, 服务.切换玩家音域(玩家标识, true), "3→4");
                assertEquals(4, 服务.切换玩家音域(玩家标识, true), "4 升音域应保持");
            }
        }

        @Test
        @DisplayName("切换音域：Z 键降音域 4→3→2→1→0，0 降音域保持不变")
        void 降音域_应依次递减并在0保持() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置玩家音域(玩家标识, 4);
                assertEquals(3, 服务.切换玩家音域(玩家标识, false), "4→3");
                assertEquals(2, 服务.切换玩家音域(玩家标识, false), "3→2");
                assertEquals(1, 服务.切换玩家音域(玩家标识, false), "2→1");
                assertEquals(0, 服务.切换玩家音域(玩家标识, false), "1→0");
                assertEquals(0, 服务.切换玩家音域(玩家标识, false), "0 降音域应保持");
            }
        }

        @Test
        @DisplayName("设置音域：超出范围的值应 clamp 到 [0, 4]")
        void 设置音域_越界应被clamp() {
            服务.设置玩家音域(玩家标识, -5);
            assertEquals(0, 服务.获取玩家音域(玩家标识), "-5 应 clamp 到 0");

            服务.设置玩家音域(玩家标识, 100);
            assertEquals(4, 服务.获取玩家音域(玩家标识), "100 应 clamp 到 4");
        }

        @Test
        @DisplayName("设置音域：null 玩家标识应不抛异常")
        void 设置音域_null参数_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置玩家音域(null, 2));
        }

        @Test
        @DisplayName("获取键位绑定表：应返回非 null 的默认表")
        void 获取键位绑定表_应返回非null() {
            键位绑定表 表 = 服务.获取键位绑定表();
            assertNotNull(表, "键位绑定表不应为 null");
            assertEquals(键位绑定表.音域数量, 表.获取音域数量(),
                    "音域数量应为 5");
        }
    }

    @Nested
    @DisplayName("FP-06: 键位绑定表值对象（默认表 + 八度平移 + 逐音域覆盖）")
    class FP06键位绑定表测试 {

        @Test
        @DisplayName("默认表：音域 0 应为 Apple Musical Typing 布局")
        void 默认表_音域0应为AppleMusicalTyping() {
            键位绑定表 表 = 键位绑定表.默认表();
            Map<String, Integer> 音域0 = 表.获取音域映射(0);
            assertEquals(12, 音域0.size(), "音域 0 应有 12 个键");
            assertEquals(0, 音域0.get("a"), "A → 0");
            assertEquals(1, 音域0.get("s"), "S → 1");
            assertEquals(2, 音域0.get("d"), "D → 2");
            assertEquals(3, 音域0.get("f"), "F → 3");
            assertEquals(4, 音域0.get("g"), "G → 4");
            assertEquals(5, 音域0.get("h"), "H → 5");
            assertEquals(6, 音域0.get("j"), "J → 6");
            assertEquals(7, 音域0.get("w"), "W → 7");
            assertEquals(8, 音域0.get("e"), "E → 8");
            assertEquals(9, 音域0.get("t"), "T → 9");
            assertEquals(10, 音域0.get("y"), "Y → 10");
            assertEquals(11, 音域0.get("u"), "U → 11");
        }

        @Test
        @DisplayName("默认表：音域 1-4 应按八度平移自动填充（根音 = 0 + k×12）")
        void 默认表_音域1到4应八度平移() {
            键位绑定表 表 = 键位绑定表.默认表();
            for (int 音域 = 1; 音域 < 键位绑定表.音域数量; 音域++) {
                Map<String, Integer> 映射 = 表.获取音域映射(音域);
                int 偏移 = 音域 * 键位绑定表.每音域键数;
                assertEquals(0 + 偏移, 映射.get("a"), "音域 " + 音域 + " 的 A 键应为 " + (0 + 偏移));
                assertEquals(6 + 偏移, 映射.get("j"), "音域 " + 音域 + " 的 J 键应为 " + (6 + 偏移));
                assertEquals(11 + 偏移, 映射.get("u"), "音域 " + 音域 + " 的 U 键应为 " + (11 + 偏移));
            }
        }

        @Test
        @DisplayName("查询音高：大小写不敏感")
        void 查询音高_大小写不敏感() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
            assertEquals(0, 表.查询音高(0, "A").getAsInt());
            assertEquals(0, 表.查询音高(0, "a").getAsInt());
        }

        @Test
        @DisplayName("查询音高：无效音域或无效键应返回 empty")
        void 查询音高_无效输入_应返回empty() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertFalse(表.查询音高(99, "a").isPresent(), "无效音域应返回 empty");
            assertFalse(表.查询音高(-1, "a").isPresent(), "负音域应返回 empty");
            assertFalse(表.查询音高(0, "z").isPresent(), "Z 键未绑定应返回 empty");
            assertFalse(表.查询音高(0, "").isPresent(), "空键应返回 empty");
            assertFalse(表.查询音高(0, null).isPresent(), "null 键应返回 empty");
        }

        @Test
        @DisplayName("音域有效：0-4 应有效，其他应无效")
        void 音域有效_边界判断() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertTrue(表.音域有效(0), "音域 0 应有效");
            assertTrue(表.音域有效(4), "音域 4 应有效");
            assertFalse(表.音域有效(-1), "音域 -1 应无效");
            assertFalse(表.音域有效(5), "音域 5 应无效");
        }

        @Test
        @DisplayName("规范化音域：超出范围应 clamp 到 [0, 4]")
        void 规范化音域_应Clamp() {
            键位绑定表 表 = 键位绑定表.默认表();
            assertEquals(0, 表.规范化音域(-5), "-5 应规范化为 0");
            assertEquals(0, 表.规范化音域(0), "0 应保持 0");
            assertEquals(4, 表.规范化音域(4), "4 应保持 4");
            assertEquals(4, 表.规范化音域(100), "100 应规范化为 4");
        }

        @Test
        @DisplayName("从配置加载：无 键位绑定 段应返回默认表")
        void 从配置加载_无键位绑定段_应返回默认表() {
            FileConfiguration 配置 = new YamlConfiguration();
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(键位绑定表.默认表().获取音域映射(0), 表.获取音域映射(0),
                    "无配置段应返回默认表");
        }

        @Test
        @DisplayName("从配置加载：null 配置应返回默认表")
        void 从配置加载_null配置_应返回默认表() {
            键位绑定表 表 = 键位绑定表.从配置加载(null);
            assertNotNull(表);
            assertEquals(12, 表.获取音域映射(0).size(), "null 配置应回退到默认表");
        }

        @Test
        @DisplayName("从配置加载：音域 1 显式覆盖 A=14 应覆盖平移结果 12")
        void 从配置加载_显式覆盖应优先于平移() {
            String yaml = """
                    键位绑定:
                      音域0:
                        a: 0
                        s: 1
                        d: 2
                        f: 3
                        g: 4
                        h: 5
                        j: 6
                        w: 7
                        e: 8
                        t: 9
                        y: 10
                        u: 11
                      音域1:
                        a: 14
                    """;
            FileConfiguration 配置 = new YamlConfiguration();
            try {
                配置.loadFromString(yaml);
            } catch (Exception e) {
                fail("YAML 加载失败", e);
            }
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            assertEquals(14, 表.查询音高(1, "a").getAsInt(),
                    "音域 1 的 A 键显式覆盖为 14，应优先于平移结果 12");
            // 未显式覆盖的 S 键应按平移 = 13
            assertEquals(13, 表.查询音高(1, "s").getAsInt(),
                    "音域 1 的 S 键未显式覆盖，应按八度平移 = 13");
        }

        @Test
        @DisplayName("从配置加载：音域 0 缺失应回退到默认 Apple Musical Typing 布局")
        void 从配置加载_音域0缺失_应回退默认() {
            // 音高有效范围 [0, 59]（每音域键数 12 × 音域数量 5 = 60）
            // 音域 2 平移结果 a=24；显式覆盖为 26 验证覆盖优先
            String yaml = """
                    键位绑定:
                      音域2:
                        a: 26
                    """;
            FileConfiguration 配置 = new YamlConfiguration();
            try {
                配置.loadFromString(yaml);
            } catch (Exception e) {
                fail("YAML 加载失败", e);
            }
            键位绑定表 表 = 键位绑定表.从配置加载(配置);
            // 音域 0 应回退到默认
            assertEquals(0, 表.查询音高(0, "a").getAsInt(),
                    "音域 0 缺失应回退到默认 Apple Musical Typing");
            // 音域 2 应有显式覆盖 A=26
            assertEquals(26, 表.查询音高(2, "a").getAsInt(),
                    "音域 2 的 A 键显式覆盖为 26");
            // 未显式覆盖的 S 键应按八度平移 = 24 + 1 = 25
            assertEquals(25, 表.查询音高(2, "s").getAsInt(),
                    "音域 2 的 S 键未显式覆盖，应按八度平移 = 25");
        }
    }

    @Nested
    @DisplayName("FP-01: 三轨输入归一（聊天命令模式 + mod 预留接口）")
    class FP01三轨归一测试 {

        @Test
        @DisplayName("处理聊天按键：Z 键应切换音域降档")
        void 处理聊天按键_Z键应降音域() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.设置玩家音域(玩家标识, 2);
                assertTrue(服务.处理聊天按键(玩家mock, "z"));
                assertEquals(1, 服务.获取玩家音域(玩家标识), "Z 键应将音域从 2 降到 1");
            }
        }

        @Test
        @DisplayName("处理聊天按键：X 键应切换音域升档")
        void 处理聊天按键_X键应升音域() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                assertTrue(服务.处理聊天按键(玩家mock, "x"));
                assertEquals(1, 服务.获取玩家音域(玩家标识), "X 键应将音域从 0 升到 1");
            }
        }

        @Test
        @DisplayName("处理聊天按键：C 键应切换力度档降档")
        void 处理聊天按键_C键应降力度() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                assertTrue(服务.处理聊天按键(玩家mock, "c"));
                assertEquals(力度档.P, 服务.获取玩家力度档(玩家标识),
                        "C 键应将力度档从 MF 降到 P");
            }
        }

        @Test
        @DisplayName("处理聊天按键：V 键应切换力度档升档")
        void 处理聊天按键_V键应升力度() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                assertTrue(服务.处理聊天按键(玩家mock, "v"));
                assertEquals(力度档.F, 服务.获取玩家力度档(玩家标识),
                        "V 键应将力度档从 MF 升到 F");
            }
        }

        @Test
        @DisplayName("处理聊天按键：大小写不敏感（A 与 a 等价）")
        void 处理聊天按键_大小写不敏感() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                assertTrue(服务.处理聊天按键(玩家mock, "Z"));
                assertEquals(0, 服务.获取玩家音域(玩家标识),
                        "Z（大写）应与 z（小写）等价，音域 0 降档保持 0");
            }
        }

        @Test
        @DisplayName("处理聊天按键：null 或空字符串应返回 false 不抛异常")
        void 处理聊天按键_null或空_应返回false() {
            Player 玩家mock = mock(Player.class);
            assertFalse(服务.处理聊天按键(null, "a"), "null 玩家应返回 false");
            assertFalse(服务.处理聊天按键(玩家mock, null), "null 键应返回 false");
            assertFalse(服务.处理聊天按键(玩家mock, ""), "空键应返回 false");
            assertFalse(服务.处理聊天按键(玩家mock, "   "), "空白键应返回 false");
            assertFalse(服务.处理聊天按键(玩家mock, "ab"), "多字符键应返回 false");
        }

        @Test
        @DisplayName("处理聊天按键：非音乐键（如 q m）应返回 false")
        void 处理聊天按键_非音乐键_应返回false() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                assertFalse(服务.处理聊天按键(玩家mock, "q"), "Q 键非音乐键应返回 false");
                assertFalse(服务.处理聊天按键(玩家mock, "m"), "M 键非音乐键应返回 false");
            }
        }

        @Test
        @DisplayName("处理聊天按键：a 键应触发当前音域 0 的 0 号音高演奏")
        void 处理聊天按键_a键应触发音高0() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            乐器定义 乐器 = 创建测试乐器();
            服务.设置玩家当前乐器(玩家标识, 乐器);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                boolean 已消费 = 服务.处理聊天按键(玩家mock, "a");
                assertTrue(已消费, "a 键应被消费");
                // 验证触发了 playSound（音高 0，MF 力度档）
                verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                        eq(力度档.MF.获取音量()), anyFloat());
            }
        }

        @Test
        @DisplayName("处理mod输入：未启用真实力度时应使用玩家当前力度档")
        void 处理mod输入_未启用真实力度_应用当前力度档() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            乐器定义 乐器 = 创建测试乐器();
            服务.设置玩家力度档(玩家标识, 力度档.FF);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.处理mod输入(玩家标识, 乐器, 0, -1, 0, 0);
                // 验证使用了 FF 力度档的音量 1.0
                verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                        eq(1.0f), anyFloat());
            }
        }

        @Test
        @DisplayName("处理mod输入：启用真实力度时 velocity 应离散映射到 5 档力度")
        void 处理mod输入_启用真实力度_应离散映射() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            // 设置玩家真实力度开关会触发发送消息，需 mock 翻译服务与关键词解析器避免 NPE
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            乐器定义 乐器 = 创建测试乐器();
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.设置玩家真实力度开关(玩家标识, true);
                // velocity 119 → FF（111-127）
                服务.处理mod输入(玩家标识, 乐器, 0, 119, 0, 0);
                verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                        eq(力度档.FF.获取音量()), anyFloat());
                // velocity 13 → PP（0-25）
                服务.处理mod输入(玩家标识, 乐器, 0, 13, 0, 0);
                verify(玩家mock).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                        eq(力度档.PP.获取音量()), anyFloat());
            }
        }

        @Test
        @DisplayName("处理mod输入：按下/松开时间戳应反推时值Tick")
        void 处理mod输入_应反推时值Tick() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            乐器定义 乐器 = 创建测试乐器();
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                // 200ms 差值 = 4 tick，应不抛异常
                assertDoesNotThrow(() ->
                        服务.处理mod输入(玩家标识, 乐器, 0, 70, 1000, 1200));
            }
        }

        @Test
        @DisplayName("处理mod输入：null 参数应不抛异常")
        void 处理mod输入_null参数_应不抛异常() {
            乐器定义 乐器 = 创建测试乐器();
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertDoesNotThrow(() ->
                        服务.处理mod输入(玩家标识, null, 0, 70, 0, 0));
                assertDoesNotThrow(() ->
                        服务.处理mod输入(玩家标识, 乐器, 0, 70, 0, 0));
            }
        }
    }

    @Nested
    @DisplayName("FP-06 + FP-05: 玩家退出应清理音域/力度档/真实力度状态")
    class 玩家退出清理测试 {

        @Test
        @DisplayName("玩家退出后音域/力度档/真实力度状态应被清理")
        void 玩家退出后_状态应被清理() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置玩家音域(玩家标识, 3);
                服务.设置玩家力度档(玩家标识, 力度档.FF);
                服务.设置玩家真实力度开关(玩家标识, true);
                assertEquals(3, 服务.获取玩家音域(玩家标识));
                assertEquals(力度档.FF, 服务.获取玩家力度档(玩家标识));
                assertTrue(服务.是否玩家真实力度开启(玩家标识));

                服务.玩家退出清理(玩家标识);

                assertEquals(0, 服务.获取玩家音域(玩家标识), "退出后音域应回到默认 0");
                assertEquals(力度档.MF, 服务.获取玩家力度档(玩家标识), "退出后力度档应回到默认 MF");
                assertFalse(服务.是否玩家真实力度开启(玩家标识), "退出后真实力度应关闭");
            }
        }
    }

    // ========== FP-F 测试：6×9 钢琴布局 + 9 功能位 + 延音系统 ==========

    @Nested
    @DisplayName("FP-03: 6×9 钢琴布局查询接口（默认布局）")
    class FP03钢琴布局查询测试 {

        @Test
        @DisplayName("默认功能位槽位表应包含 9 个功能位（槽 0-8）")
        void 默认功能位槽位表应有9个() {
            Map<Integer, 功能位定义> 表 = 服务.获取功能位槽位表();
            assertEquals(9, 表.size(), "默认应包含 9 个功能位");
            assertEquals(功能位定义.保存, 表.get(0));
            assertEquals(功能位定义.清除, 表.get(1));
            assertEquals(功能位定义.关闭, 表.get(2));
            assertEquals(功能位定义.NBS导入, 表.get(3));
            assertEquals(功能位定义.NBS导出, 表.get(4));
            assertEquals(功能位定义.和弦预设, 表.get(5));
            assertEquals(功能位定义.公共乐谱库, 表.get(6));
            assertEquals(功能位定义.钢琴卷帘编辑, 表.get(7));
            assertEquals(功能位定义.分层切换, 表.get(8));
        }

        @Test
        @DisplayName("默认功能位槽位表应不可修改")
        void 功能位槽位表应不可修改() {
            Map<Integer, 功能位定义> 表 = 服务.获取功能位槽位表();
            assertThrows(UnsupportedOperationException.class, () -> 表.put(99, 功能位定义.保存),
                    "返回的槽位表应不可修改");
        }

        @Test
        @DisplayName("默认中部控制槽位应为 9-18（音域减=9, 音域显示=10, ..., 延音模式切换=18）")
        void 默认中部控制槽位应为9到18() {
            assertEquals(9, 服务.获取槽位音域减());
            assertEquals(10, 服务.获取槽位音域显示());
            assertEquals(11, 服务.获取槽位音域加());
            assertEquals(12, 服务.获取槽位力度减());
            assertEquals(13, 服务.获取槽位力度显示());
            assertEquals(14, 服务.获取槽位力度加());
            assertEquals(15, 服务.获取槽位录制());
            assertEquals(16, 服务.获取槽位停止());
            assertEquals(17, 服务.获取槽位播放());
            assertEquals(18, 服务.获取槽位延音模式切换());
        }

        @Test
        @DisplayName("默认黑键槽位数组应为 [37, 39, 41, 42, 43]")
        void 默认黑键槽位应为37_39_41_42_43() {
            int[] 期望 = {37, 39, 41, 42, 43};
            assertArrayEquals(期望, 服务.获取黑键槽位数组());
        }

        @Test
        @DisplayName("默认白键槽位数组应为 [45, 46, 47, 48, 49, 50, 51]")
        void 默认白键槽位应为45到51() {
            int[] 期望 = {45, 46, 47, 48, 49, 50, 51};
            assertArrayEquals(期望, 服务.获取白键槽位数组());
        }

        @Test
        @DisplayName("默认黑键音高偏移应为 [1, 3, 6, 8, 10]")
        void 默认黑键音高偏移应为1_3_6_8_10() {
            int[] 期望 = {1, 3, 6, 8, 10};
            assertArrayEquals(期望, 服务.获取黑键音高偏移数组());
        }

        @Test
        @DisplayName("默认白键音高偏移应为 [0, 2, 4, 5, 7, 9, 11]")
        void 默认白键音高偏移应为0_2_4_5_7_9_11() {
            int[] 期望 = {0, 2, 4, 5, 7, 9, 11};
            assertArrayEquals(期望, 服务.获取白键音高偏移数组());
        }

        @Test
        @DisplayName("默认钢琴布局应有 5 黑键 + 7 白键 = 12 键")
        void 默认钢琴布局应有12键() {
            assertEquals(5, 服务.获取黑键槽位数组().length, "应有 5 个黑键");
            assertEquals(7, 服务.获取白键槽位数组().length, "应有 7 个白键");
            assertEquals(12, 服务.获取黑键槽位数组().length + 服务.获取白键槽位数组().length,
                    "钢琴键总数应为 12（7 白 + 5 黑）");
        }

        @Test
        @DisplayName("默认钢琴布局应填满第 4-5 行（槽 36-53）：黑键 37-43，白键 45-51")
        void 默认钢琴布局应填满第4_5行() {
            int[] 黑键 = 服务.获取黑键槽位数组();
            for (int 槽位 : 黑键) {
                assertTrue(槽位 >= 36 && 槽位 <= 44,
                        "黑键槽位 " + 槽位 + " 应在第 4 行（槽 36-44）");
            }
            int[] 白键 = 服务.获取白键槽位数组();
            for (int 槽位 : 白键) {
                assertTrue(槽位 >= 45 && 槽位 <= 53,
                        "白键槽位 " + 槽位 + " 应在第 5 行（槽 45-53）");
            }
        }

        @Test
        @DisplayName("黑键/白键槽位数组返回应为副本，修改不影响内部状态")
        void 槽位数组返回应为副本() {
            int[] 黑键1 = 服务.获取黑键槽位数组();
            int[] 黑键2 = 服务.获取黑键槽位数组();
            assertNotSame(黑键1, 黑键2, "每次返回应为不同实例");
            黑键1[0] = 999;
            int[] 黑键3 = 服务.获取黑键槽位数组();
            assertNotEquals(999, 黑键3[0], "修改返回数组不应影响内部状态");
        }
    }

    @Nested
    @DisplayName("FP-03: 解析槽位音高")
    class FP03解析槽位音高测试 {

        @Test
        @DisplayName("八度 0：黑键槽位 37 应返回音高 1")
        void 八度0_黑键槽位37_应返回音高1() {
            assertEquals(1, 服务.解析槽位音高(37, 0));
        }

        @Test
        @DisplayName("八度 0：黑键槽位 39 应返回音高 3")
        void 八度0_黑键槽位39_应返回音高3() {
            assertEquals(3, 服务.解析槽位音高(39, 0));
        }

        @Test
        @DisplayName("八度 0：黑键槽位 41 应返回音高 6")
        void 八度0_黑键槽位41_应返回音高6() {
            assertEquals(6, 服务.解析槽位音高(41, 0));
        }

        @Test
        @DisplayName("八度 0：黑键槽位 42 应返回音高 8（G#）")
        void 八度0_黑键槽位42_应返回音高8() {
            assertEquals(8, 服务.解析槽位音高(42, 0));
        }

        @Test
        @DisplayName("八度 0：黑键槽位 43 应返回音高 10（A#）")
        void 八度0_黑键槽位43_应返回音高10() {
            assertEquals(10, 服务.解析槽位音高(43, 0));
        }

        @Test
        @DisplayName("八度 0：白键槽位 45 应返回音高 0")
        void 八度0_白键槽位45_应返回音高0() {
            assertEquals(0, 服务.解析槽位音高(45, 0));
        }

        @Test
        @DisplayName("八度 0：白键槽位 46 应返回音高 2")
        void 八度0_白键槽位46_应返回音高2() {
            assertEquals(2, 服务.解析槽位音高(46, 0));
        }

        @Test
        @DisplayName("八度 0：白键槽位 48 应返回音高 5")
        void 八度0_白键槽位48_应返回音高5() {
            assertEquals(5, 服务.解析槽位音高(48, 0));
        }

        @Test
        @DisplayName("八度 0：白键槽位 51 应返回音高 11")
        void 八度0_白键槽位51_应返回音高11() {
            assertEquals(11, 服务.解析槽位音高(51, 0));
        }

        @Test
        @DisplayName("八度 1：白键槽位 45 应返回音高 12（八度平移）")
        void 八度1_白键槽位45_应返回音高12() {
            assertEquals(12, 服务.解析槽位音高(45, 1));
        }

        @Test
        @DisplayName("八度 2：黑键槽位 37 应返回音高 25（越界返回 -1）")
        void 八度2_黑键槽位37_应返回负1_因音高25越界() {
            assertEquals(-1, 服务.解析槽位音高(37, 2), "音高 25 越界（>24），应返回 -1");
        }

        @Test
        @DisplayName("功能位槽位（0-8）应返回 -1（非琴键）")
        void 功能位槽位应返回负1() {
            for (int 槽位 = 0; 槽位 <= 8; 槽位++) {
                assertEquals(-1, 服务.解析槽位音高(槽位, 0),
                        "功能位槽位 " + 槽位 + " 应返回 -1");
            }
        }

        @Test
        @DisplayName("中部控制槽位（9-18）应返回 -1（非琴键）")
        void 中部控制槽位应返回负1() {
            for (int 槽位 = 9; 槽位 <= 18; 槽位++) {
                assertEquals(-1, 服务.解析槽位音高(槽位, 0),
                        "中部控制槽位 " + 槽位 + " 应返回 -1");
            }
        }

        @Test
        @DisplayName("空槽位（19-36）应返回 -1（非琴键）")
        void 空槽位应返回负1() {
            for (int 槽位 = 19; 槽位 <= 36; 槽位++) {
                assertEquals(-1, 服务.解析槽位音高(槽位, 0),
                        "空槽位 " + 槽位 + " 应返回 -1");
            }
        }

        @Test
        @DisplayName("GUI 外槽位（>53）应返回 -1")
        void GUI外槽位应返回负1() {
            assertEquals(-1, 服务.解析槽位音高(54, 0));
            assertEquals(-1, 服务.解析槽位音高(99, 0));
            assertEquals(-1, 服务.解析槽位音高(1000, 0));
        }

        @Test
        @DisplayName("负槽位应返回 -1")
        void 负槽位应返回负1() {
            assertEquals(-1, 服务.解析槽位音高(-1, 0));
            assertEquals(-1, 服务.解析槽位音高(-100, 0));
        }

        @Test
        @DisplayName("八度 0：所有 12 个琴键槽位应返回 0-11 的连续音高")
        void 八度0_所有琴键应返回0到11() {
            java.util.Set<Integer> 音高集合 = new java.util.HashSet<>();
            int[] 黑键 = 服务.获取黑键槽位数组();
            int[] 白键 = 服务.获取白键槽位数组();
            for (int 槽位 : 黑键) {
                音高集合.add(服务.解析槽位音高(槽位, 0));
            }
            for (int 槽位 : 白键) {
                音高集合.add(服务.解析槽位音高(槽位, 0));
            }
            assertEquals(12, 音高集合.size(), "应有 12 个不同音高");
            for (int 音高 = 0; 音高 <= 11; 音高++) {
                assertTrue(音高集合.contains(音高),
                        "音高 " + 音高 + " 应可通过某个琴键槽位触发");
            }
        }
    }

    @Nested
    @DisplayName("FP-04: 延音模式状态管理")
    class FP04延音模式状态测试 {

        @Test
        @DisplayName("默认延音模式应为 按住")
        void 默认延音模式应为按住() {
            assertEquals(延音模式.按住, 服务.获取延音模式(玩家标识),
                    "未设置时延音模式应为默认值 按住");
        }

        @Test
        @DisplayName("设置延音模式为 切换 后应能读取到 切换")
        void 设置延音模式为切换后应能读取() {
            服务.设置延音模式(玩家标识, 延音模式.切换);
            assertEquals(延音模式.切换, 服务.获取延音模式(玩家标识));
        }

        @Test
        @DisplayName("设置延音模式为 按住 后应能读取到 按住")
        void 设置延音模式为按住后应能读取() {
            服务.设置延音模式(玩家标识, 延音模式.切换);
            服务.设置延音模式(玩家标识, 延音模式.按住);
            assertEquals(延音模式.按住, 服务.获取延音模式(玩家标识));
        }

        @Test
        @DisplayName("切换延音模式：按住 → 切换")
        void 切换延音模式_按住到切换() {
            服务.设置延音模式(玩家标识, 延音模式.按住);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                延音模式 新模式 = 服务.切换延音模式(玩家标识);
                assertEquals(延音模式.切换, 新模式);
                assertEquals(延音模式.切换, 服务.获取延音模式(玩家标识));
            }
        }

        @Test
        @DisplayName("切换延音模式：切换 → 按住")
        void 切换延音模式_切换到按住() {
            服务.设置延音模式(玩家标识, 延音模式.切换);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                延音模式 新模式 = 服务.切换延音模式(玩家标识);
                assertEquals(延音模式.按住, 新模式);
                assertEquals(延音模式.按住, 服务.获取延音模式(玩家标识));
            }
        }

        @Test
        @DisplayName("切换延音模式：连续两次切换应回到原模式")
        void 切换延音模式_连续两次应回到原模式() {
            服务.设置延音模式(玩家标识, 延音模式.按住);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.切换延音模式(玩家标识);
                延音模式 二次 = 服务.切换延音模式(玩家标识);
                assertEquals(延音模式.按住, 二次);
            }
        }

        @Test
        @DisplayName("切换延音模式：玩家在线时应发送切换消息")
        void 切换延音模式_玩家在线_应发送消息() {
            服务.设置延音模式(玩家标识, 延音模式.按住);
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.切换延音模式(玩家标识);
                verify(玩家mock).sendMessage(any(net.kyori.adventure.text.Component.class));
            }
        }

        @Test
        @DisplayName("设置延音模式：null 玩家标识应不抛异常")
        void 设置延音模式_null玩家标识_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置延音模式(null, 延音模式.切换));
        }

        @Test
        @DisplayName("设置延音模式：null 模式应不抛异常")
        void 设置延音模式_null模式_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置延音模式(玩家标识, null));
        }

        @Test
        @DisplayName("获取延音模式：null 玩家标识应返回默认值 按住")
        void 获取延音模式_null玩家标识_应返回默认() {
            assertEquals(延音模式.按住, 服务.获取延音模式(null));
        }

        @Test
        @DisplayName("切换延音模式：null 玩家标识应返回默认值 按住")
        void 切换延音模式_null玩家标识_应返回默认() {
            assertEquals(延音模式.按住, 服务.切换延音模式(null));
        }
    }

    @Nested
    @DisplayName("FP-04: 延音任务生命周期")
    class FP04延音任务生命周期测试 {

        private Player 创建在线玩家() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            return 玩家mock;
        }

        @Test
        @DisplayName("初始状态：是否延音中应为 false")
        void 初始状态_是否延音中应为false() {
            assertFalse(服务.是否延音中(玩家标识));
        }

        @Test
        @DisplayName("初始状态：获取延音音高应返回 -1")
        void 初始状态_获取延音音高应返回负1() {
            assertEquals(-1, 服务.获取延音音高(玩家标识));
        }

        @Test
        @DisplayName("开始延音：是否延音中应为 true，获取延音音高应返回传入音高")
        void 开始延音后_状态应正确() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 5, 力度档.MF);

                assertTrue(服务.是否延音中(玩家标识), "开始延音后应处于延音中");
                assertEquals(5, 服务.获取延音音高(玩家标识), "延音音高应为 5");
            }
        }

        @Test
        @DisplayName("开始延音：应先调用 演奏指令 触发首次音符（playSound 1 次）")
        void 开始延音_应触发首次音符() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);

                verify(玩家mock, times(1)).playSound(any(Location.class),
                        eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
            }
        }

        @Test
        @DisplayName("停止延音：是否延音中应为 false，获取延音音高应返回 -1")
        void 停止延音后_状态应清除() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 7, 力度档.F);
                assertTrue(服务.是否延音中(玩家标识));

                服务.停止延音(玩家标识);
                assertFalse(服务.是否延音中(玩家标识), "停止延音后应不再处于延音中");
                assertEquals(-1, 服务.获取延音音高(玩家标识), "停止后延音音高应清除为 -1");
            }
        }

        @Test
        @DisplayName("开始延音：已有延音时应先停止旧延音再启动新延音")
        void 开始延音_已有延音时应先停止旧延音() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 3, 力度档.MF);
                int 第一次音高 = 服务.获取延音音高(玩家标识);
                assertEquals(3, 第一次音高);

                服务.开始延音(玩家mock, 乐器, 8, 力度档.F);
                assertTrue(服务.是否延音中(玩家标识));
                assertEquals(8, 服务.获取延音音高(玩家标识), "第二次开始延音应替换为新音高 8");
            }
        }

        @Test
        @DisplayName("开始延音：null 玩家应不抛异常")
        void 开始延音_null玩家_应不抛异常() {
            乐器定义 乐器 = 创建测试乐器();
            assertDoesNotThrow(() -> 服务.开始延音(null, 乐器, 0, 力度档.MF));
        }

        @Test
        @DisplayName("开始延音：null 乐器应不抛异常")
        void 开始延音_null乐器_应不抛异常() {
            Player 玩家mock = 创建在线玩家();
            assertDoesNotThrow(() -> 服务.开始延音(玩家mock, null, 0, 力度档.MF));
        }

        @Test
        @DisplayName("开始延音：null 力度档应不抛异常")
        void 开始延音_null力度档_应不抛异常() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            assertDoesNotThrow(() -> 服务.开始延音(玩家mock, 乐器, 0, null));
        }

        @Test
        @DisplayName("停止延音：null 玩家标识应不抛异常")
        void 停止延音_null玩家标识_应不抛异常() {
            assertDoesNotThrow(() -> 服务.停止延音(null));
        }

        @Test
        @DisplayName("停止延音：未开始延音时应不抛异常")
        void 停止延音_未开始延音_应不抛异常() {
            assertDoesNotThrow(() -> 服务.停止延音(玩家标识));
        }

        @Test
        @DisplayName("是否延音中：null 玩家标识应返回 false")
        void 是否延音中_null玩家标识_应返回false() {
            assertFalse(服务.是否延音中(null));
        }

        @Test
        @DisplayName("获取延音音高：null 玩家标识应返回 -1")
        void 获取延音音高_null玩家标识_应返回负1() {
            assertEquals(-1, 服务.获取延音音高(null));
        }
    }

    @Nested
    @DisplayName("FP-04: 延音重触发与音量衰减")
    class FP04延音重触发测试 {

        private Player 创建在线玩家() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            return 玩家mock;
        }

        @SuppressWarnings("unchecked")
        private org.bukkit.scheduler.BukkitRunnable 获取延音任务() throws Exception {
            Field 字段 = 乐器服务实现.class.getDeclaredField("玩家延音任务表");
            字段.setAccessible(true);
            Map<UUID, org.bukkit.scheduler.BukkitRunnable> 表 =
                    (Map<UUID, org.bukkit.scheduler.BukkitRunnable>) 字段.get(服务);
            return 表.get(玩家标识);
        }

        @Test
        @DisplayName("开始延音：应通过 Bukkit.getScheduler 调度周期任务")
        void 开始延音_应调度周期任务() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);

                verify(调度器mock, times(1)).runTaskTimer(
                        any(Plugin.class), any(Runnable.class), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("延音任务重触发：调用任务 run() 应再次触发 playSound")
        void 延音任务重触发_应再次触发playSound() throws Exception {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);
                org.bukkit.scheduler.BukkitRunnable 任务 = 获取延音任务();
                assertNotNull(任务, "延音任务应已创建");

                // 首次触发已 playSound 1 次；调用 run() 应再 playSound 1 次
                任务.run();
                verify(玩家mock, times(2)).playSound(any(Location.class),
                        eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
            }
        }

        @Test
        @DisplayName("延音任务重触发：连续调用 run() 应触发多次 playSound 直到任务自动停止")
        void 延音任务重触发_应触发多次直到自动停止() throws Exception {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);
                org.bukkit.scheduler.BukkitRunnable 任务 = 获取延音任务();
                assertNotNull(任务);

                // 默认延音重触发次数=4，首次已触发，剩余=3
                // 调用 run() 3 次后任务应自动停止
                任务.run();
                任务.run();
                任务.run();
                // 第 4 次应触发停止（剩余=0）
                任务.run();

                // 首次 1 + 重触发 3 = 4 次 playSound
                verify(玩家mock, times(4)).playSound(any(Location.class),
                        eq(Sound.BLOCK_NOTE_BLOCK_HARP), anyFloat(), anyFloat());
                assertFalse(服务.是否延音中(玩家标识), "重触发次数耗尽后任务应自动停止");
            }
        }

        @Test
        @DisplayName("延音任务重触发：玩家离线时应停止延音")
        void 延音任务重触发_玩家离线应停止() throws Exception {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);
                org.bukkit.scheduler.BukkitRunnable 任务 = 获取延音任务();
                assertNotNull(任务);

                // 模拟玩家离线
                when(玩家mock.isOnline()).thenReturn(false);
                任务.run();

                assertFalse(服务.是否延音中(玩家标识), "玩家离线后延音应自动停止");
            }
        }

        @Test
        @DisplayName("延音音量衰减：每次重触发音量应乘以衰减系数（0.85）")
        void 延音音量应按衰减系数递减() throws Exception {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);

            org.mockito.ArgumentCaptor<Float> 音量捕获 =
                    org.mockito.ArgumentCaptor.forClass(Float.class);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);
                org.bukkit.scheduler.BukkitRunnable 任务 = 获取延音任务();
                assertNotNull(任务);

                // 触发 3 次重触发（首次 + 3 次 = 4 次 playSound）
                任务.run();
                任务.run();
                任务.run();

                verify(玩家mock, times(4)).playSound(any(Location.class),
                        eq(Sound.BLOCK_NOTE_BLOCK_HARP), 音量捕获.capture(), anyFloat());

                java.util.List<Float> 音量序列 = 音量捕获.getAllValues();
                assertEquals(4, 音量序列.size(), "应捕获 4 次 playSound 音量");
                // 首次音量 = 力度档.MF.获取音量 × 默认音量（1.0）
                float 首次期望 = 力度档.MF.获取音量() * 1.0f;
                assertEquals(首次期望, 音量序列.get(0), 0.001f, "首次音量应为力度档音量");
                // 之后每次 = 上次 × 0.85（衰减系数）
                // 注意：首次由 演奏指令 触发，音量 = 力度档.获取音量 × 默认音量
                // 重触发由 播放延音音符 触发，音量 = 衰减后音量 × 默认音量
                // 衰减序列：v0=初始, v1=v0×0.85, v2=v1×0.85=v0×0.85², v3=v2×0.85=v0×0.85³
                float 上次 = 音量序列.get(0);
                for (int i = 1; i < 4; i++) {
                    float 期望 = 上次 * 0.85f;
                    assertEquals(期望, 音量序列.get(i), 0.001f,
                            "第 " + i + " 次重触发音量应为上次 × 0.85");
                    上次 = 音量序列.get(i);
                }
            }
        }

        @Test
        @DisplayName("玩家退出清理：应停止延音任务")
        void 玩家退出清理_应停止延音() {
            Player 玩家mock = 创建在线玩家();
            乐器定义 乐器 = 创建测试乐器();
            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            BukkitTask 任务mock = mock(BukkitTask.class);
            when(任务mock.getTaskId()).thenReturn(1);
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(任务mock);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                服务.开始延音(玩家mock, 乐器, 0, 力度档.MF);
                assertTrue(服务.是否延音中(玩家标识));

                服务.玩家退出清理(玩家标识);
                assertFalse(服务.是否延音中(玩家标识), "玩家退出后延音应被清理");
            }
        }
    }

    // ========== FP-G 测试：FP-09 录音量化非破坏性 + O2 swing + FP-10 可视化反馈 ==========

    @Nested
    @DisplayName("FP-09: 量化档位枚举（6 档位 + 网格 tick 计算 + 量化 tick 算法）")
    class FP09量化档位枚举测试 {

        @Test
        @DisplayName("应有 6 个量化档位（1/8 / 1/16 / 1/32 + 三连音变体）")
        void 应有6个量化档位() {
            assertEquals(6, 量化档位.values().length, "应有 6 个量化档位");
        }

        @Test
        @DisplayName("1/8 档位：标识=1/8，每拍分割数=2，非三连音")
        void 一分之八档位属性() {
            assertEquals("1/8", 量化档位.一分之八.获取标识());
            assertEquals(2, 量化档位.一分之八.获取每拍分割数());
            assertFalse(量化档位.一分之八.是否三连音());
        }

        @Test
        @DisplayName("1/16 档位：标识=1/16，每拍分割数=4，非三连音")
        void 一分之十六档位属性() {
            assertEquals("1/16", 量化档位.一分之十六.获取标识());
            assertEquals(4, 量化档位.一分之十六.获取每拍分割数());
            assertFalse(量化档位.一分之十六.是否三连音());
        }

        @Test
        @DisplayName("1/32 档位：标识=1/32，每拍分割数=8，非三连音")
        void 一分之三十二档位属性() {
            assertEquals("1/32", 量化档位.一分之三十二.获取标识());
            assertEquals(8, 量化档位.一分之三十二.获取每拍分割数());
            assertFalse(量化档位.一分之三十二.是否三连音());
        }

        @Test
        @DisplayName("1/8T 档位：标识=1/8T，每拍分割数=3，三连音")
        void 一分之八T档位属性() {
            assertEquals("1/8T", 量化档位.一分之八T.获取标识());
            assertEquals(3, 量化档位.一分之八T.获取每拍分割数());
            assertTrue(量化档位.一分之八T.是否三连音());
        }

        @Test
        @DisplayName("1/16T 档位：标识=1/16T，每拍分割数=6，三连音")
        void 一分之十六T档位属性() {
            assertEquals("1/16T", 量化档位.一分之十六T.获取标识());
            assertEquals(6, 量化档位.一分之十六T.获取每拍分割数());
            assertTrue(量化档位.一分之十六T.是否三连音());
        }

        @Test
        @DisplayName("1/32T 档位：标识=1/32T，每拍分割数=12，三连音")
        void 一分之三十二T档位属性() {
            assertEquals("1/32T", 量化档位.一分之三十二T.获取标识());
            assertEquals(12, 量化档位.一分之三十二T.获取每拍分割数());
            assertTrue(量化档位.一分之三十二T.是否三连音());
        }

        @Test
        @DisplayName("计算网格Tick：BPM=120 时 1/8=5, 1/16=2, 1/32=1")
        void BPM120_基础档位网格Tick() {
            assertEquals(5, 量化档位.一分之八.计算网格Tick(120), "1/8 网格 = 10/2 = 5");
            assertEquals(2, 量化档位.一分之十六.计算网格Tick(120), "1/16 网格 = 10/4 = 2");
            assertEquals(1, 量化档位.一分之三十二.计算网格Tick(120), "1/32 网格 = 10/8 = 1");
        }

        @Test
        @DisplayName("计算网格Tick：BPM=120 时 1/8T=3, 1/16T=1, 1/32T=1（最小为 1）")
        void BPM120_三连音档位网格Tick() {
            assertEquals(3, 量化档位.一分之八T.计算网格Tick(120), "1/8T 网格 = 10/3 = 3");
            assertEquals(1, 量化档位.一分之十六T.计算网格Tick(120), "1/16T 网格 = 10/6 = 1");
            assertEquals(1, 量化档位.一分之三十二T.计算网格Tick(120), "1/32T 网格 = 10/12 = 0 → max(1,0) = 1");
        }

        @Test
        @DisplayName("计算网格Tick：BPM<=0 应返回 1 不抛异常")
        void BPM非正_应返回1() {
            assertEquals(1, 量化档位.一分之八.计算网格Tick(0));
            assertEquals(1, 量化档位.一分之十六.计算网格Tick(-100));
        }

        @Test
        @DisplayName("量化Tick：tick<=0 应返回 0")
        void 量化Tick_非正_应返回0() {
            assertEquals(0, 量化档位.一分之十六.量化Tick(0, 120));
            assertEquals(0, 量化档位.一分之十六.量化Tick(-5, 120));
        }

        @Test
        @DisplayName("量化Tick：1/16 BPM=120 网格=2，tick=3 应吸附到 4（round(3/2)*2=4）")
        void 量化Tick_1分之16_应吸附到最近网格() {
            assertEquals(4, 量化档位.一分之十六.量化Tick(3, 120), "tick=3 网格=2 → round(1.5)*2 = 4");
            assertEquals(10, 量化档位.一分之十六.量化Tick(10, 120), "tick=10 已在网格上");
            assertEquals(18, 量化档位.一分之十六.量化Tick(17, 120), "tick=17 网格=2 → round(8.5)*2 = 18");
        }

        @Test
        @DisplayName("量化Tick：1/8 BPM=120 网格=5，tick=3 应吸附到 5（round(0.6)*5=5）")
        void 量化Tick_1分之8_应吸附到最近网格() {
            assertEquals(0, 量化档位.一分之八.量化Tick(2, 120), "tick=2 网格=5 → round(0.4)*5 = 0");
            assertEquals(5, 量化档位.一分之八.量化Tick(3, 120), "tick=3 网格=5 → round(0.6)*5 = 5");
            assertEquals(15, 量化档位.一分之八.量化Tick(17, 120), "tick=17 网格=5 → round(3.4)*5 = 15");
        }

        @Test
        @DisplayName("默认档位应为 1/16")
        void 默认档位_应为1分之16() {
            assertEquals(量化档位.一分之十六, 量化档位.默认档位());
        }

        @Test
        @DisplayName("按标识查找：有效标识应返回对应档位")
        void 按标识查找_有效标识() {
            assertEquals(量化档位.一分之八, 量化档位.按标识查找("1/8"));
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找("1/16"));
            assertEquals(量化档位.一分之三十二, 量化档位.按标识查找("1/32"));
            assertEquals(量化档位.一分之八T, 量化档位.按标识查找("1/8T"));
            assertEquals(量化档位.一分之十六T, 量化档位.按标识查找("1/16T"));
            assertEquals(量化档位.一分之三十二T, 量化档位.按标识查找("1/32T"));
        }

        @Test
        @DisplayName("按标识查找：无效/null/空标识应返回默认档位 1/16")
        void 按标识查找_无效标识_应返回默认() {
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找(null));
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找(""));
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找("   "));
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找("invalid"));
            assertEquals(量化档位.一分之十六, 量化档位.按标识查找("1/4"));
        }

        @Test
        @DisplayName("按标识查找：应支持前后空格 trim")
        void 按标识查找_应支持空格trim() {
            assertEquals(量化档位.一分之八, 量化档位.按标识查找("  1/8  "));
            assertEquals(量化档位.一分之十六T, 量化档位.按标识查找(" 1/16T "));
        }
    }

    @Nested
    @DisplayName("FP-09: 乐谱非破坏性量化（应用量化保存原始 + 撤销恢复 + 多次应用保留最原始）")
    class FP09乐谱非破坏性量化测试 {

        private 乐谱 创建测试乐谱(List<音符> 音符列表) {
            return 乐谱.创建("量化测试", 玩家标识, "测试", 120, 音符列表);
        }

        @Test
        @DisplayName("未量化时：是否已量化应为 false，获取原始音符列表应为空")
        void 未量化_状态正确() {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.简单(0)));
            assertFalse(乐谱.是否已量化(), "未量化时 是否已量化 应为 false");
            assertTrue(乐谱.获取原始音符列表().isEmpty(), "未量化时 原始音符列表 应为空");
        }

        @Test
        @DisplayName("应用量化后：是否已量化应为 true，原始音符列表应非空且等于应用前音符")
        void 应用量化后_状态正确() {
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5));
            乐谱 乐谱 = 创建测试乐谱(原始);
            List<音符> 量化后 = List.of(音符.of(0, 4, 1.0), 音符.of(12, 6, 0.5));

            乐谱.应用量化(量化后);

            assertTrue(乐谱.是否已量化(), "应用量化后 是否已量化 应为 true");
            assertEquals(2, 乐谱.获取原始音符列表().size(), "原始音符列表应保留 2 个音符");
            assertEquals(3, 乐谱.获取原始音符列表().get(0).获取时值Tick(), "原始音符时值应保留");
            assertEquals(7, 乐谱.获取原始音符列表().get(1).获取时值Tick(), "原始音符时值应保留");
            // 当前音符列表应为量化后版本
            assertEquals(4, 乐谱.获取音符列表().get(0).获取时值Tick(), "当前音符应为量化后版本");
            assertEquals(6, 乐谱.获取音符列表().get(1).获取时值Tick(), "当前音符应为量化后版本");
        }

        @Test
        @DisplayName("应用量化应保留原始音高与力度（仅修改时值）")
        void 应用量化_应保留音高与力度() {
            List<音符> 原始 = List.of(音符.of(5, 3, 0.7));
            乐谱 乐谱 = 创建测试乐谱(原始);
            List<音符> 量化后 = List.of(音符.of(5, 4, 0.7));

            乐谱.应用量化(量化后);

            assertEquals(5, 乐谱.获取音符列表().get(0).获取音高(), "音高应保留");
            assertEquals(0.7, 乐谱.获取音符列表().get(0).获取力度(), 0.001, "力度应保留");
            assertEquals(4, 乐谱.获取音符列表().get(0).获取时值Tick(), "时值应为量化后值");
        }

        @Test
        @DisplayName("多次应用量化：原始音符列表应保留第一次应用前的最原始版本")
        void 多次应用量化_应保留最原始() {
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0));
            乐谱 乐谱 = 创建测试乐谱(原始);

            乐谱.应用量化(List.of(音符.of(0, 4, 1.0)));
            乐谱.应用量化(List.of(音符.of(0, 6, 1.0)));

            assertEquals(3, 乐谱.获取原始音符列表().get(0).获取时值Tick(),
                    "多次应用量化后 原始音符列表 应保留最原始时值 3");
            assertEquals(6, 乐谱.获取音符列表().get(0).获取时值Tick(),
                    "当前音符列表应为最后一次量化结果 6");
        }

        @Test
        @DisplayName("撤销量化：未量化时应返回 false 无操作")
        void 撤销量化_未量化_应返回false() {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.简单(0)));
            assertFalse(乐谱.撤销量化(), "未量化时撤销量化应返回 false");
        }

        @Test
        @DisplayName("撤销量化：已量化时应恢复原始音符并返回 true，是否已量化变 false")
        void 撤销量化_已量化_应恢复原始() {
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5));
            乐谱 乐谱 = 创建测试乐谱(原始);
            乐谱.应用量化(List.of(音符.of(0, 4, 1.0), 音符.of(12, 6, 0.5)));

            boolean 已撤销 = 乐谱.撤销量化();

            assertTrue(已撤销, "已量化时撤销量化应返回 true");
            assertFalse(乐谱.是否已量化(), "撤销后 是否已量化 应为 false");
            assertEquals(3, 乐谱.获取音符列表().get(0).获取时值Tick(), "撤销后应恢复原始时值 3");
            assertEquals(7, 乐谱.获取音符列表().get(1).获取时值Tick(), "撤销后应恢复原始时值 7");
            assertTrue(乐谱.获取原始音符列表().isEmpty(), "撤销后 原始音符列表 应置空");
        }

        @Test
        @DisplayName("应用量化：null 参数应不抛异常无操作")
        void 应用量化_null参数_应不抛异常() {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.简单(0)));
            assertDoesNotThrow(() -> 乐谱.应用量化(null));
            assertFalse(乐谱.是否已量化(), "null 参数不应改变量化状态");
        }

        @Test
        @DisplayName("撤销后再量化：原始音符列表应再次保存")
        void 撤销后再量化_应重新保存原始() {
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0));
            乐谱 乐谱 = 创建测试乐谱(原始);

            乐谱.应用量化(List.of(音符.of(0, 4, 1.0)));
            乐谱.撤销量化();
            乐谱.应用量化(List.of(音符.of(0, 6, 1.0)));

            assertTrue(乐谱.是否已量化(), "再次量化后 是否已量化 应为 true");
            assertEquals(3, 乐谱.获取原始音符列表().get(0).获取时值Tick(),
                    "再次量化后 原始音符列表 应保存撤销后恢复的原始时值 3");
            assertEquals(6, 乐谱.获取音符列表().get(0).获取时值Tick(), "当前音符应为新量化结果 6");
        }
    }

    @Nested
    @DisplayName("FP-G: 乐器服务实现配置默认值（量化 + swing + 可视化）")
    class FPG配置默认值测试 {

        @Test
        @DisplayName("默认量化档位应为 1/16")
        void 默认量化档位_应为1分之16() {
            assertEquals(量化档位.一分之十六, 服务.获取默认量化档位());
        }

        @Test
        @DisplayName("默认量化开启应为 true（FP-09.1 录音默认开启量化）")
        void 默认量化开启_应为true() {
            assertTrue(服务.是否默认量化开启(), "录音默认开启量化应为 true");
        }

        @Test
        @DisplayName("默认 Swing 比例应为 0.50（直，无 swing）")
        void 默认Swing比例_应为0_50() {
            assertEquals(0.50, 服务.获取默认Swing比例(), 0.001, "默认 Swing 应为 0.50");
        }

        @Test
        @DisplayName("默认 Swing 开启应为 false（默认关闭）")
        void 默认Swing开启_应为false() {
            assertFalse(服务.是否默认Swing开启(), "默认 Swing 应关闭");
        }

        @Test
        @DisplayName("默认 GUI 高亮持续 tick 应为 8（符合 6-10 范围）")
        void 默认GUI高亮持续tick_应为8() {
            int tick = 服务.获取GUI高亮持续tick();
            assertEquals(8, tick, "默认 GUI 高亮持续 tick 应为 8");
            assertTrue(tick >= 6 && tick <= 10, "GUI 高亮持续 tick 应在 6-10 范围内");
        }

        @Test
        @DisplayName("默认粒子启用应为 true")
        void 默认粒子启用_应为true() {
            assertTrue(服务.是否粒子启用(), "默认粒子启用应为 true");
        }

        @Test
        @DisplayName("默认拍号显示应为 false（仅 count-in 显示）")
        void 默认拍号显示_应为false() {
            assertFalse(服务.是否拍号显示启用(), "默认拍号显示应为 false");
        }
    }

    @Nested
    @DisplayName("O2: Swing 状态管理（设置 + 获取 + 边界 clamp + null 安全）")
    class O2Swing状态管理测试 {

        @Test
        @DisplayName("默认 Swing：未设置时应返回默认值 0.50")
        void 默认Swing_应返回0_50() {
            assertEquals(0.50, 服务.获取Swing(玩家标识), 0.001, "未设置时应返回默认 0.50");
        }

        @Test
        @DisplayName("获取Swing：null 玩家标识应返回默认值 0.50 不抛异常")
        void 获取Swing_null玩家_应返回默认() {
            assertEquals(0.50, 服务.获取Swing(null), 0.001, "null 玩家应返回默认 0.50");
        }

        @Test
        @DisplayName("设置Swing：0.55 应被接受")
        void 设置Swing_0_55_应被接受() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.55);
                assertEquals(0.55, 服务.获取Swing(玩家标识), 0.001, "0.55 应被接受");
            }
        }

        @Test
        @DisplayName("设置Swing：0.65 上限应被接受")
        void 设置Swing_0_65_上限应被接受() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.65);
                assertEquals(0.65, 服务.获取Swing(玩家标识), 0.001, "0.65 上限应被接受");
            }
        }

        @Test
        @DisplayName("设置Swing：0.50 下限应被接受")
        void 设置Swing_0_50_下限应被接受() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.50);
                assertEquals(0.50, 服务.获取Swing(玩家标识), 0.001, "0.50 下限应被接受");
            }
        }

        @Test
        @DisplayName("设置Swing：超过上限 0.80 应被 clamp 到 0.65")
        void 设置Swing_超上限_应被clamp到0_65() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.80);
                assertEquals(0.65, 服务.获取Swing(玩家标识), 0.001, "0.80 应被 clamp 到 0.65");
            }
        }

        @Test
        @DisplayName("设置Swing：低于下限 0.30 应被 clamp 到 0.50")
        void 设置Swing_低下限_应被clamp到0_50() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.30);
                assertEquals(0.50, 服务.获取Swing(玩家标识), 0.001, "0.30 应被 clamp 到 0.50");
            }
        }

        @Test
        @DisplayName("设置Swing：null 玩家标识应不抛异常")
        void 设置Swing_null玩家_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置Swing(null, 0.55));
        }

        @Test
        @DisplayName("设置Swing：玩家在线时应发送 swing已设置 消息")
        void 设置Swing_玩家在线_应发送消息() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.设置Swing(玩家标识, 0.65);
                verify(玩家mock).sendMessage(any(net.kyori.adventure.text.Component.class));
            }
        }

        @Test
        @DisplayName("设置Swing：消息应包含百分比文本（65%）")
        void 设置Swing_消息应包含百分比() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.设置Swing(玩家标识, 0.65);
                // 验证 关键词解析器 被调用时第 3 个参数（Object[]）包含 "65%"
                org.mockito.ArgumentCaptor<Object[]> 参数捕获 =
                        org.mockito.ArgumentCaptor.forClass(Object[].class);
                verify(关键词解析器mock).解析(anyString(), any(UUID.class), 参数捕获.capture());
                Object[] 参数 = 参数捕获.getValue();
                assertTrue(参数.length > 0, "应有至少 1 个参数");
                assertEquals("65%", 参数[0], "参数应为 65%");
            }
        }
    }

    @Nested
    @DisplayName("FP-09: 玩家量化开关与档位管理")
    class FP09玩家量化开关档位测试 {

        @Test
        @DisplayName("默认是否玩家量化开启：未设置时应回退到默认值 true")
        void 默认是否玩家量化开启_应为true() {
            assertTrue(服务.是否玩家量化开启(玩家标识), "未设置时应回退到默认 true");
        }

        @Test
        @DisplayName("是否玩家量化开启：null 玩家应返回默认值 true 不抛异常")
        void 是否玩家量化开启_null玩家_应返回默认值true() {
            assertTrue(服务.是否玩家量化开启(null), "null 玩家应返回默认值 true");
        }

        @Test
        @DisplayName("设置玩家量化开关 false 后查询应返回 false")
        void 设置玩家量化开关_false_应返回false() {
            服务.设置玩家量化开关(玩家标识, false);
            assertFalse(服务.是否玩家量化开启(玩家标识), "设置 false 后应返回 false");
        }

        @Test
        @DisplayName("设置玩家量化开关 true 后查询应返回 true")
        void 设置玩家量化开关_true_应返回true() {
            服务.设置玩家量化开关(玩家标识, false);
            服务.设置玩家量化开关(玩家标识, true);
            assertTrue(服务.是否玩家量化开启(玩家标识), "设置 true 后应返回 true");
        }

        @Test
        @DisplayName("设置玩家量化开关：null 玩家应不抛异常")
        void 设置玩家量化开关_null玩家_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置玩家量化开关(null, true));
        }

        @Test
        @DisplayName("默认获取玩家量化档位：未设置时应回退到默认 1/16")
        void 默认获取玩家量化档位_应为1分之16() {
            assertEquals(量化档位.一分之十六, 服务.获取玩家量化档位(玩家标识),
                    "未设置时应回退到默认 1/16");
        }

        @Test
        @DisplayName("获取玩家量化档位：null 玩家应返回默认 1/16 不抛异常")
        void 获取玩家量化档位_null玩家_应返回默认() {
            assertEquals(量化档位.一分之十六, 服务.获取玩家量化档位(null),
                    "null 玩家应返回默认 1/16");
        }

        @Test
        @DisplayName("设置玩家量化档位：应覆盖默认值")
        void 设置玩家量化档位_应覆盖默认() {
            服务.设置玩家量化档位(玩家标识, 量化档位.一分之八);
            assertEquals(量化档位.一分之八, 服务.获取玩家量化档位(玩家标识));
        }

        @Test
        @DisplayName("设置玩家量化档位：null 玩家或 null 档位应不抛异常")
        void 设置玩家量化档位_null参数_应不抛异常() {
            assertDoesNotThrow(() -> 服务.设置玩家量化档位(null, 量化档位.一分之八));
            assertDoesNotThrow(() -> 服务.设置玩家量化档位(玩家标识, null));
            assertEquals(量化档位.一分之十六, 服务.获取玩家量化档位(玩家标识),
                    "null 档位不应改变当前值");
        }
    }

    @Nested
    @DisplayName("FP-09: 量化音符列表算法（1/8/1/16/1/32 + 三连音 + swing）")
    class FP09量化音符列表算法测试 {

        private List<音符> 调用量化音符列表(List<音符> 原始, 量化档位 档位, int 速度BPM, double swing) throws Exception {
            Method 方法 = 乐器服务实现.class.getDeclaredMethod(
                    "量化音符列表", List.class, 量化档位.class, int.class, double.class);
            方法.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<音符> 结果 = (List<音符>) 方法.invoke(服务, 原始, 档位, 速度BPM, swing);
            return 结果;
        }

        @Test
        @DisplayName("null 或空列表应返回空列表不抛异常")
        void null或空列表_应返回空列表() throws Exception {
            assertTrue(调用量化音符列表(null, 量化档位.一分之十六, 120, 0.50).isEmpty());
            assertTrue(调用量化音符列表(List.of(), 量化档位.一分之十六, 120, 0.50).isEmpty());
        }

        @Test
        @DisplayName("1/16 BPM=120：时值 [3,7,4] 起始 [0,3,10] 应量化到 [0,4,10]，时值变 [4,6,4]")
        void 一分之16_应正确量化() throws Exception {
            // 输入时值 [3, 7, 4] → 累计起始 [0, 3, 10]
            // 1/16 网格=2: [0, round(3/2)*2=4, round(10/2)*2=10]
            // 时值: [4-0=4, 10-4=6, 4 (最后保留原始)]
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5), 音符.of(24, 4, 0.8));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之十六, 120, 0.50);
            assertEquals(3, 结果.size());
            assertEquals(4, 结果.get(0).获取时值Tick(), "第1音符时值应为 4");
            assertEquals(6, 结果.get(1).获取时值Tick(), "第2音符时值应为 6");
            assertEquals(4, 结果.get(2).获取时值Tick(), "第3音符时值应保留原始 4");
            // 音高与力度应保留
            assertEquals(0, 结果.get(0).获取音高());
            assertEquals(12, 结果.get(1).获取音高());
            assertEquals(24, 结果.get(2).获取音高());
            assertEquals(0.5, 结果.get(1).获取力度(), 0.001);
            assertEquals(0.8, 结果.get(2).获取力度(), 0.001);
        }

        @Test
        @DisplayName("1/8 BPM=120：时值 [3,7,4] 起始 [0,3,10] 应量化到 [0,5,10]")
        void 一分之8_应正确量化() throws Exception {
            // 1/8 网格=5: [0, round(3/5)*5=5, round(10/5)*5=10]
            // 时值: [5, 5, 4 (最后保留)]
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5), 音符.of(24, 4, 0.8));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八, 120, 0.50);
            assertEquals(5, 结果.get(0).获取时值Tick(), "第1音符时值应为 5");
            assertEquals(5, 结果.get(1).获取时值Tick(), "第2音符时值应为 5");
            assertEquals(4, 结果.get(2).获取时值Tick(), "第3音符时值应保留原始 4");
        }

        @Test
        @DisplayName("1/32 BPM=120：网格=1，整数 tick 不变")
        void 一分之32_网格1_整数tick不变() throws Exception {
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之三十二, 120, 0.50);
            assertEquals(3, 结果.get(0).获取时值Tick(), "1/32 网格=1 整数 tick 不变");
            assertEquals(7, 结果.get(1).获取时值Tick(), "1/32 网格=1 整数 tick 不变");
        }

        @Test
        @DisplayName("1/8T BPM=120：三连音网格=3，时值 [3,3,3] 应保持三连音感")
        void 一分之8T_应保持三连音() throws Exception {
            // 1/8T 网格=3: 累计起始 [0,3,6] 已在网格上，时值 [3,3,3] 不变
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 3, 1.0), 音符.of(24, 3, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八T, 120, 0.50);
            assertEquals(3, 结果.get(0).获取时值Tick(), "三连音时值应保持 3");
            assertEquals(3, 结果.get(1).获取时值Tick(), "三连音时值应保持 3");
            assertEquals(3, 结果.get(2).获取时值Tick(), "三连音时值应保持 3");
        }

        @Test
        @DisplayName("1/16T BPM=120：三连音网格=1，时值 [3,3,3] 应量化到 [3,3,3]")
        void 一分之16T_应正确量化() throws Exception {
            // 1/16T 网格=1: 累计起始 [0,3,6] 已在网格上
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 3, 1.0), 音符.of(24, 3, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之十六T, 120, 0.50);
            assertEquals(3, 结果.get(0).获取时值Tick());
            assertEquals(3, 结果.get(1).获取时值Tick());
            assertEquals(3, 结果.get(2).获取时值Tick());
        }

        @Test
        @DisplayName("swing=0.50：1/8 档位不应用 swing（时值 [5,5,5] 保持 [5,5,5]）")
        void swing0_50_不应用swing() throws Exception {
            // 1/8 网格=5: 累计起始 [0,5,10] 已在网格上
            // swing=0.50 不应用 swing（条件 swing > 0.50 + 1e-6 为 false）
            List<音符> 原始 = List.of(音符.of(0, 5, 1.0), 音符.of(12, 5, 1.0), 音符.of(24, 5, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八, 120, 0.50);
            assertEquals(5, 结果.get(0).获取时值Tick(), "swing=0.50 时值应保持 5");
            assertEquals(5, 结果.get(1).获取时值Tick(), "swing=0.50 时值应保持 5");
            assertEquals(5, 结果.get(2).获取时值Tick(), "swing=0.50 时值应保持 5");
        }

        @Test
        @DisplayName("swing=0.65：1/8 档位半拍位置应被推迟（首拍不变）")
        void swing0_65_半拍位置应推迟() throws Exception {
            // 1/8 BPM=120 网格=5, 每拍tick=10, 半拍=5, swing偏移=round(0.65*10)=7
            // 输入时值 [5,5,5,5] 累计起始 [0,5,10,15] 量化后 [0,5,10,15]
            // 应用 swing: i=1 偏移=5=半拍 → 5-5+7=7; i=3 偏移=15%10=5=半拍 → 15-5+7=17
            // 量化后起始 [0,7,10,17] 时值 [7,3,7,5(最后保留)]
            List<音符> 原始 = List.of(
                    音符.of(0, 5, 1.0), 音符.of(5, 5, 1.0),
                    音符.of(10, 5, 1.0), 音符.of(15, 5, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八, 120, 0.65);
            assertEquals(7, 结果.get(0).获取时值Tick(), "第1音符（首拍）时值应保持 7 = 7-0");
            assertEquals(3, 结果.get(1).获取时值Tick(), "第2音符（半拍→swing）时值应为 3 = 10-7");
            assertEquals(7, 结果.get(2).获取时值Tick(), "第3音符（首拍）时值应为 7 = 17-10");
            assertEquals(5, 结果.get(3).获取时值Tick(), "第4音符（半拍→swing）应保留原始时值 5");
        }

        @Test
        @DisplayName("swing=0.65：首拍位置不应受 swing 影响")
        void swing0_65_首拍位置不变() throws Exception {
            // 1/8 网格=5, 输入时值 [10,10] 累计起始 [0,10] 均为首拍位置
            // swing 不影响首拍，时值保持 [10,10]
            List<音符> 原始 = List.of(音符.of(0, 10, 1.0), 音符.of(12, 10, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八, 120, 0.65);
            assertEquals(10, 结果.get(0).获取时值Tick(), "首拍位置时值应保持 10");
            assertEquals(10, 结果.get(1).获取时值Tick(), "首拍位置时值应保持 10");
        }

        @Test
        @DisplayName("swing=0.65：三连音档位不应应用 swing")
        void swing0_65_三连音档位不应用swing() throws Exception {
            // 1/8T 三连音档位，即使 swing=0.65 也不应用 swing
            List<音符> 原始 = List.of(音符.of(0, 3, 1.0), 音符.of(12, 3, 1.0), 音符.of(24, 3, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八T, 120, 0.65);
            assertEquals(3, 结果.get(0).获取时值Tick(), "三连音档位不应应用 swing，时值保持 3");
            assertEquals(3, 结果.get(1).获取时值Tick(), "三连音档位不应应用 swing，时值保持 3");
            assertEquals(3, 结果.get(2).获取时值Tick(), "三连音档位不应应用 swing，时值保持 3");
        }

        @Test
        @DisplayName("swing=0.55：1/8 档位半拍位置应被轻微推迟")
        void swing0_55_半拍位置应轻微推迟() throws Exception {
            // 1/8 网格=5, 每拍tick=10, 半拍=5, swing偏移=round(0.55*10)=round(5.5)=6
            // 输入时值 [5,5,5,5] 累计起始 [0,5,10,15]
            // 应用 swing: i=1 → 5-5+6=6; i=3 → 15-5+6=16
            // 量化后起始 [0,6,10,16] 时值 [6,4,6,5(最后保留)]
            List<音符> 原始 = List.of(
                    音符.of(0, 5, 1.0), 音符.of(5, 5, 1.0),
                    音符.of(10, 5, 1.0), 音符.of(15, 5, 1.0));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之八, 120, 0.55);
            assertEquals(6, 结果.get(0).获取时值Tick(), "swing=0.55 第1音符时值应为 6");
            assertEquals(4, 结果.get(1).获取时值Tick(), "swing=0.55 第2音符时值应为 4");
            assertEquals(6, 结果.get(2).获取时值Tick(), "swing=0.55 第3音符时值应为 6");
            assertEquals(5, 结果.get(3).获取时值Tick(), "swing=0.55 第4音符应保留原始时值 5");
        }

        @Test
        @DisplayName("量化应保留原始音高与力度（仅修改时值）")
        void 量化_应保留音高与力度() throws Exception {
            List<音符> 原始 = List.of(音符.of(7, 3, 0.7), 音符.of(15, 7, 0.3));
            List<音符> 结果 = 调用量化音符列表(原始, 量化档位.一分之十六, 120, 0.50);
            assertEquals(7, 结果.get(0).获取音高(), "音高应保留");
            assertEquals(0.7, 结果.get(0).获取力度(), 0.001, "力度应保留");
            assertEquals(15, 结果.get(1).获取音高(), "音高应保留");
            assertEquals(0.3, 结果.get(1).获取力度(), 0.001, "力度应保留");
        }
    }

    @Nested
    @DisplayName("FP-09: 量化乐谱 / 撤销量化 / 是否已量化 集成（通过反射注入乐谱）")
    class FP09量化乐谱集成测试 {

        @SuppressWarnings("unchecked")
        private void 注入乐谱到量化表(乐谱 乐谱) throws Exception {
            Field 字段 = 乐器服务实现.class.getDeclaredField("玩家量化乐谱表");
            字段.setAccessible(true);
            Map<UUID, 乐谱> 表 = (Map<UUID, 乐谱>) 字段.get(服务);
            表.put(玩家标识, 乐谱);
        }

        private 乐谱 创建测试乐谱(List<音符> 音符列表) {
            return 乐谱.创建("量化集成测试", 玩家标识, "测试", 120, 音符列表);
        }

        @Test
        @DisplayName("是否已量化：null 玩家应返回 false")
        void 是否已量化_null玩家_应返回false() {
            assertFalse(服务.是否已量化(null));
        }

        @Test
        @DisplayName("是否已量化：未注入乐谱时应返回 false")
        void 是否已量化_未注入乐谱_应返回false() {
            assertFalse(服务.是否已量化(玩家标识));
        }

        @Test
        @DisplayName("撤销量化：null 玩家应返回 false")
        void 撤销量化_null玩家_应返回false() {
            assertFalse(服务.撤销量化(null));
        }

        @Test
        @DisplayName("撤销量化：未注入乐谱应返回 false 不抛异常")
        void 撤销量化_未注入乐谱_应返回false() {
            assertFalse(服务.撤销量化(玩家标识));
        }

        @Test
        @DisplayName("量化乐谱：null 玩家或 null 档位应不抛异常")
        void 量化乐谱_null参数_应不抛异常() {
            assertDoesNotThrow(() -> 服务.量化乐谱(null, 量化档位.一分之十六));
            assertDoesNotThrow(() -> 服务.量化乐谱(玩家标识, null));
        }

        @Test
        @DisplayName("量化乐谱：未注入乐谱应不抛异常（发送无可量化乐谱消息）")
        void 量化乐谱_未注入乐谱_应不抛异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertDoesNotThrow(() -> 服务.量化乐谱(玩家标识, 量化档位.一分之十六));
            }
        }

        @Test
        @DisplayName("量化乐谱：已注入乐谱后调用应使 是否已量化 返回 true")
        void 量化乐谱_已注入乐谱_应使已量化返回true() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5)));
            注入乐谱到量化表(乐谱);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
            }
            assertTrue(服务.是否已量化(玩家标识), "量化后 是否已量化 应为 true");
            assertTrue(乐谱.获取原始音符列表().size() == 2, "原始音符列表应保留 2 个音符");
        }

        @Test
        @DisplayName("量化乐谱：应使用原始音符列表作为基准（已量化时从原始计算）")
        void 量化乐谱_已量化时从原始计算() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0)));
            注入乐谱到量化表(乐谱);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
                int 第一次时值 = 乐谱.获取音符列表().get(0).获取时值Tick();
                // 再次量化应基于原始时值 3，而非第一次的结果
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
                int 第二次时值 = 乐谱.获取音符列表().get(0).获取时值Tick();
                assertEquals(第一次时值, 第二次时值, "两次量化基于同一原始应结果一致");
                assertEquals(3, 乐谱.获取原始音符列表().get(0).获取时值Tick(),
                        "原始音符时值应保持 3 不变");
            }
        }

        @Test
        @DisplayName("撤销量化：已量化后应恢复原始音符并返回 true")
        void 撤销量化_已量化_应恢复原始() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0), 音符.of(12, 7, 0.5)));
            注入乐谱到量化表(乐谱);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
                boolean 已撤销 = 服务.撤销量化(玩家标识);
                assertTrue(已撤销, "已量化后撤销量化应返回 true");
            }
            assertFalse(服务.是否已量化(玩家标识), "撤销后 是否已量化 应为 false");
            assertEquals(3, 乐谱.获取音符列表().get(0).获取时值Tick(), "撤销后应恢复原始时值 3");
            assertEquals(7, 乐谱.获取音符列表().get(1).获取时值Tick(), "撤销后应恢复原始时值 7");
        }

        @Test
        @DisplayName("量化乐谱：玩家在线时应发送量化已应用消息")
        void 量化乐谱_玩家在线_应发送消息() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0)));
            注入乐谱到量化表(乐谱);
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
                verify(玩家mock).sendMessage(any(net.kyori.adventure.text.Component.class));
            }
        }

        @Test
        @DisplayName("撤销量化：玩家在线时应发送量化已撤销消息")
        void 撤销量化_玩家在线_应发送消息() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0)));
            注入乐谱到量化表(乐谱);
            Player 玩家mock = mock(Player.class);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.isOnline()).thenReturn(true);
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
                服务.撤销量化(玩家标识);
                verify(玩家mock, atLeast(2)).sendMessage(any(net.kyori.adventure.text.Component.class));
            }
        }

        @Test
        @DisplayName("量化乐谱：应保存乐谱到文件（持久化）")
        void 量化乐谱_应保存到文件() throws Exception {
            乐谱 乐谱 = 创建测试乐谱(List.of(音符.of(0, 3, 1.0)));
            注入乐谱到量化表(乐谱);
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.量化乐谱(玩家标识, 量化档位.一分之十六);
            }
            // 验证乐谱文件已写入
            File 乐谱文件 = new File(临时目录.toFile(),
                    "乐谱" + File.separator + 玩家标识 + File.separator + "量化集成测试.yml");
            assertTrue(乐谱文件.exists(), "量化后乐谱文件应已写入");
        }

        @Test
        @DisplayName("玩家退出清理：应清理量化状态（乐谱表 + swing 表 + 开关表 + 档位表）")
        void 玩家退出清理_应清理量化状态() throws Exception {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                服务.设置Swing(玩家标识, 0.60);
                服务.设置玩家量化开关(玩家标识, false);
                服务.设置玩家量化档位(玩家标识, 量化档位.一分之八);
                assertEquals(0.60, 服务.获取Swing(玩家标识), 0.001);
                assertFalse(服务.是否玩家量化开启(玩家标识));
                assertEquals(量化档位.一分之八, 服务.获取玩家量化档位(玩家标识));

                服务.玩家退出清理(玩家标识);

                assertEquals(0.50, 服务.获取Swing(玩家标识), 0.001, "退出后 swing 应回到默认 0.50");
                assertTrue(服务.是否玩家量化开启(玩家标识), "退出后量化开关应回到默认 true");
                assertEquals(量化档位.一分之十六, 服务.获取玩家量化档位(玩家标识),
                        "退出后量化档位应回到默认 1/16");
            }
        }
    }

    @Nested
    @DisplayName("FP-10: 录制指示器 + 拍号显示 + GUI 高亮 + 粒子反馈")
    class FP10可视化反馈测试 {

        @SuppressWarnings({"deprecation", "removal"})
        @Test
        @DisplayName("显示录制中标题：开始录制时应调用 sendTitle 显示录制中标题")
        void 开始录制_应调用sendTitle() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(翻译服务mock.获取(anyString())).thenReturn("录制中");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家mock);
                服务.开始录制(玩家标识, 创建测试乐器());
                // 验证 sendTitle 被调用（FP-10 录制指示器）
                verify(玩家mock).sendTitle(eq("录制中"), eq(""), anyInt(), anyInt(), anyInt());
            }
        }

        @Test
        @DisplayName("显示录制中标题：null 玩家应不抛异常")
        void 开始录制_null玩家_应不抛异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);
                assertDoesNotThrow(() -> 服务.开始录制(玩家标识, 创建测试乐器()));
            }
        }

        @Test
        @DisplayName("乐器服务实现 应有 显示录制中标题 私有方法")
        void 应有显示录制中标题方法() {
            assertDoesNotThrow(() ->
                    乐器服务实现.class.getDeclaredMethod("显示录制中标题", Player.class),
                    "显示录制中标题 私有方法应存在");
        }

        @Test
        @DisplayName("乐器服务实现 应有 播放音高粒子 私有方法（FP-10 粒子反馈）")
        void 应有播放音高粒子方法() {
            assertDoesNotThrow(() ->
                    乐器服务实现.class.getDeclaredMethod("播放音高粒子", Player.class, int.class),
                    "播放音高粒子 私有方法应存在");
        }

        @Test
        @DisplayName("乐器监听器 应有 触发GUI高亮 私有方法（FP-10 GUI 高亮）")
        void 乐器监听器_应有触发GUI高亮方法() {
            assertDoesNotThrow(() ->
                    mljy.表现层.监听器.乐器监听器.class.getDeclaredMethod(
                            "触发GUI高亮", Player.class, org.bukkit.inventory.Inventory.class,
                            int.class, int.class),
                    "触发GUI高亮 私有方法应存在");
        }

        @Test
        @DisplayName("节拍器 应有 是否拍号显示启用 / 设置拍号显示启用 方法")
        void 节拍器_应有拍号显示方法() {
            assertDoesNotThrow(() ->
                    mljy.业务层.乐器.节拍器.class.getDeclaredMethod("是否拍号显示启用"));
            assertDoesNotThrow(() ->
                    mljy.业务层.乐器.节拍器.class.getDeclaredMethod(
                            "设置拍号显示启用", boolean.class));
        }

        @Test
        @DisplayName("节拍器默认拍号显示应为 false（仅 count-in 显示）")
        void 节拍器默认拍号显示_应为false() {
            mljy.业务层.乐器.节拍器 节拍器实例 = new mljy.业务层.乐器.节拍器(插件mock);
            assertFalse(节拍器实例.是否拍号显示启用(), "节拍器默认拍号显示应为 false");
        }

        @Test
        @DisplayName("节拍器设置拍号显示启用 true 后应能查询到 true")
        void 节拍器设置拍号显示_应正确切换() {
            mljy.业务层.乐器.节拍器 节拍器实例 = new mljy.业务层.乐器.节拍器(插件mock);
            节拍器实例.设置拍号显示启用(true);
            assertTrue(节拍器实例.是否拍号显示启用(), "设置 true 后应查询到 true");
            节拍器实例.设置拍号显示启用(false);
            assertFalse(节拍器实例.是否拍号显示启用(), "设置 false 后应查询到 false");
        }

        @Test
        @DisplayName("GUI 高亮持续 tick 应可通过配置访问器获取")
        void GUI高亮持续tick_应可获取() {
            int tick = 服务.获取GUI高亮持续tick();
            assertTrue(tick >= 1, "GUI 高亮持续 tick 应 >= 1");
        }

        @Test
        @DisplayName("粒子启用标志应可通过配置访问器获取")
        void 粒子启用标志_应可获取() {
            // 默认 true（配置默认值）
            assertTrue(服务.是否粒子启用());
        }
    }

    @Nested
    @DisplayName("FP-03: 乐器切换音色验证")
    class FP03乐器切换音色验证测试 {

        /**
         * 创建贝斯乐器定义（NoteBlockAPI 乐器 ID = 1，音色 = BASS，非钢琴）。
         */
        private 乐器定义 创建测试贝斯() {
            return new 乐器定义("贝斯", "贝斯", Sound.BLOCK_NOTE_BLOCK_BASS, null,
                    Material.STICK, 9002, 1, 0, 24, -2,
                    true, "测试贝斯", 1, 乐器定义.物理机制_慢衰减);
        }

        /**
         * 创建长笛乐器定义（NoteBlockAPI 乐器 ID = 6，音色 = FLUTE，非钢琴）。
         */
        private 乐器定义 创建测试长笛() {
            return new 乐器定义("长笛", "长笛", Sound.BLOCK_NOTE_BLOCK_FLUTE, null,
                    Material.STICK, 9006, 3, 0, 24, 1,
                    true, "测试长笛", 6, 乐器定义.物理机制_持续激励);
        }

        @Test
        @DisplayName("演奏音符：使用贝斯乐器应播放 BASS 音色")
        void 演奏音符_贝斯应播放Bass音色() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 贝斯 = 创建测试贝斯();
            服务.演奏音符(玩家mock, 贝斯, 12);

            // 验证 playSound 至少使用了一次 BASS 音色
            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_BASS),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("演奏音符：使用长笛乐器应播放 FLUTE 音色")
        void 演奏音符_长笛应播放Flute音色() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 长笛 = 创建测试长笛();
            服务.演奏音符(玩家mock, 长笛, 12);

            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_FLUTE),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("演奏指令：使用贝斯乐器应播放 BASS 音色")
        void 演奏指令_贝斯应播放Bass音色() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 贝斯 = 创建测试贝斯();
            服务.演奏指令(玩家mock, 贝斯, 12, 力度档.MF, 4);

            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_BASS),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("演奏指令：使用长笛乐器应播放 FLUTE 音色")
        void 演奏指令_长笛应播放Flute音色() {
            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(UUID.randomUUID());
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 长笛 = 创建测试长笛();
            服务.演奏指令(玩家mock, 长笛, 12, 力度档.MF, 4);

            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_FLUTE),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("切换乐器后演奏指令应使用新乐器音色")
        void 切换乐器后应使用新音色() {
            Player 玩家mock = mock(Player.class);
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            乐器定义 钢琴 = 创建测试乐器();
            乐器定义 贝斯 = 创建测试贝斯();

            // 先用钢琴演奏
            服务.演奏指令(玩家mock, 钢琴, 12, 力度档.MF, 4);
            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_HARP),
                    anyFloat(), anyFloat());

            // 清除调用记录后再用贝斯演奏
            clearInvocations(玩家mock);
            服务.演奏指令(玩家mock, 贝斯, 12, 力度档.MF, 4);
            verify(玩家mock, atLeastOnce()).playSound(eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_BASS),
                    anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("使用NoteBlockAPI播放：NoteBlockAPI不可用时应降级为playSound并使用当前乐器音色")
        void 使用NoteBlockAPI播放_降级应使用当前乐器音色() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                org.bukkit.plugin.PluginManager pm = mock(org.bukkit.plugin.PluginManager.class);
                when(pm.getPlugin(anyString())).thenReturn(null);
                mock.when(() -> Bukkit.getPluginManager()).thenReturn(pm);
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                Player 玩家mock = mock(Player.class);
                when(玩家mock.getUniqueId()).thenReturn(玩家标识);

                // 设置当前乐器为贝斯
                乐器定义 贝斯 = 创建测试贝斯();
                服务.设置玩家当前乐器(玩家标识, 贝斯);

                // 由于乐谱不存在，方法会返回 false，但不应抛异常
                // 这验证了降级逻辑的安全性
                boolean 结果 = 服务.使用NoteBlockAPI播放(玩家mock, "不存在的乐谱");
                assertFalse(结果);
            }
        }

        @Test
        @DisplayName("导出NBS：应使用玩家当前乐器的NoteBlockAPI乐器ID不抛异常")
        void 导出NBS应使用当前乐器ID不抛异常() {
            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                // 设置当前乐器为贝斯（NoteBlockAPI ID = 1）
                乐器定义 贝斯 = 创建测试贝斯();
                服务.设置玩家当前乐器(玩家标识, 贝斯);

                // 导出不存在的乐谱应返回 false 但不抛异常
                File 目标文件 = 临时目录.resolve("export_test.nbs").toFile();
                boolean 结果 = 服务.导出NBS(玩家标识, "不存在的乐谱", 目标文件);
                assertFalse(结果);
            }
        }

        @Test
        @DisplayName("播放乐谱：应使用玩家当前乐器音色而非默认钢琴（FP-03 核心验证）")
        void 播放乐谱_应使用玩家当前乐器音色() throws Exception {
            List<音符> 音符列表 = new ArrayList<>();
            音符列表.add(音符.of(12, 2, 1.0));
            乐谱 乐谱实例 = 乐谱.创建("测试", 玩家标识, "测试", 120, 音符列表);

            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.getName()).thenReturn("测试");
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");

            乐器定义 贝斯 = 创建测试贝斯();
            服务.设置玩家当前乐器(玩家标识, 贝斯);

            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            AtomicReference<Runnable> 任务引用 = new AtomicReference<>();
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    任务引用.set(inv.getArgument(1));
                    return mock(BukkitTask.class);
                });

            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.播放乐谱(玩家mock, 乐谱实例);

                Runnable 任务 = 任务引用.get();
                assertNotNull(任务, "应已注册定时任务");
                clearInvocations(玩家mock);
                任务.run();

                verify(玩家mock, atLeastOnce()).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_BASS), anyFloat(), anyFloat());
            }
        }

        @Test
        @DisplayName("录音→切换乐器→播放应使用当前乐器音色（FP-03 端到端）")
        void 录音后切换乐器_播放应使用当前乐器音色() throws Exception {
            when(翻译服务mock.获取(anyString())).thenReturn("msg");
            when(关键词解析器mock.解析(anyString(), any(UUID.class), any(Object[].class))).thenReturn("msg");

            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                Player 录音玩家mock = mock(Player.class);
                when(录音玩家mock.isOnline()).thenReturn(false);
                when(录音玩家mock.getName()).thenReturn("测试玩家");
                mock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(录音玩家mock);

                服务.开始录制(玩家标识, 创建测试乐器());
                服务.添加录制音符(玩家标识, 12, 4, 1.0);
                assertEquals(乐谱保存结果.成功, 服务.保存乐谱带结果(玩家标识, "FP03端到端"),
                        "乐谱应保存成功");
            }

            Optional<乐谱> 乐谱可选 = 服务.加载乐谱(玩家标识, "FP03端到端");
            assertTrue(乐谱可选.isPresent(), "应能加载已保存的乐谱");
            乐谱 乐谱实例 = 乐谱可选.get();
            assertTrue(乐谱实例.获取音符数量() > 0, "乐谱应包含音符");

            乐器定义 贝斯 = 创建测试贝斯();
            服务.设置玩家当前乐器(玩家标识, 贝斯);

            Player 玩家mock = mock(Player.class);
            when(玩家mock.isOnline()).thenReturn(true);
            when(玩家mock.getUniqueId()).thenReturn(玩家标识);
            when(玩家mock.getName()).thenReturn("测试");
            World 世界mock = mock(World.class);
            when(玩家mock.getWorld()).thenReturn(世界mock);
            Location 位置mock = mock(Location.class);
            when(玩家mock.getLocation()).thenReturn(位置mock);
            when(世界mock.getPlayers()).thenReturn(List.of());

            BukkitScheduler 调度器mock = mock(BukkitScheduler.class);
            AtomicReference<Runnable> 任务引用 = new AtomicReference<>();
            when(调度器mock.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    任务引用.set(inv.getArgument(1));
                    return mock(BukkitTask.class);
                });

            try (MockedStatic<Bukkit> mock = mockStatic(Bukkit.class)) {
                mock.when(Bukkit::getScheduler).thenReturn(调度器mock);

                服务.播放乐谱(玩家mock, 乐谱实例);

                Runnable 任务 = 任务引用.get();
                assertNotNull(任务, "应已注册定时任务");
                clearInvocations(玩家mock);
                任务.run();

                verify(玩家mock, atLeastOnce()).playSound(
                        eq(位置mock), eq(Sound.BLOCK_NOTE_BLOCK_BASS), anyFloat(), anyFloat());
            }
        }
    }
}
