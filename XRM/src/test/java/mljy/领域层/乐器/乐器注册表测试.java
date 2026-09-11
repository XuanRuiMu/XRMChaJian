package mljy.领域层.乐器;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FP-13 乐器注册表测试。
 * 验证从配置加载、按标识/CMD/名称/NoteBlockAPI ID 查找、热重载、音高转Pitch、全局参数。
 *
 * 测试策略：
 * - 单元测试：mock 单个乐器段，验证解析逻辑
 * - 集成测试：加载真实 乐器配置.yml 资源文件，验证 23 种乐器完整加载
 */
@DisplayName("FP-13: 乐器注册表（配置驱动）")
class 乐器注册表测试 {

    private FileConfiguration 配置;
    private ConfigurationSection 乐器段;
    private ConfigurationSection 钢琴段;
    private 乐器注册表 注册表;

    @BeforeEach
    void setUp() {
        配置 = mock(FileConfiguration.class);
        乐器段 = mock(ConfigurationSection.class);
        钢琴段 = mock(ConfigurationSection.class);
        when(配置.getConfigurationSection("乐器")).thenReturn(乐器段);
        when(乐器段.getKeys(false)).thenReturn(new java.util.LinkedHashSet<>(java.util.List.of("钢琴")));
        when(乐器段.getConfigurationSection("钢琴")).thenReturn(钢琴段);

        when(钢琴段.getString("显示名称", "钢琴")).thenReturn("钢琴");
        when(钢琴段.getString("原版音色", "BLOCK_NOTE_BLOCK_HARP")).thenReturn("BLOCK_NOTE_BLOCK_HARP");
        when(钢琴段.getString("音色", "BLOCK_NOTE_BLOCK_HARP")).thenReturn("BLOCK_NOTE_BLOCK_HARP");
        when(钢琴段.getString("资源包音色", null)).thenReturn("xrm.instrument.piano");
        when(钢琴段.getString("物品材质", "STICK")).thenReturn("STICK");
        when(钢琴段.getInt("自定义模型数据", 0)).thenReturn(9001);
        when(钢琴段.getInt(eq("默认音域"), anyInt())).thenReturn(2);
        when(钢琴段.getInt("默认八度", 0)).thenReturn(2);
        when(钢琴段.getInt("音域下限", 0)).thenReturn(0);
        when(钢琴段.getInt("音域上限", 24)).thenReturn(24);
        when(钢琴段.getInt("八度偏移", 0)).thenReturn(0);
        when(钢琴段.getBoolean("力度敏感", true)).thenReturn(true);
        when(钢琴段.getString("描述", "")).thenReturn("钢琴音色");
        when(钢琴段.getInt(eq("NoteBlockAPI乐器ID"), anyInt())).thenReturn(0);
        when(钢琴段.getString(eq("物理机制"), anyString())).thenReturn("瞬时触发");

        注册表 = new 乐器注册表();
    }

    @Nested
    @DisplayName("从配置加载")
    class 加载测试 {

        @Test
        @DisplayName("加载1个乐器返回数量1")
        void 加载返回数量() {
            int 数量 = 注册表.从配置加载(配置);
            assertEquals(1, 数量);
        }

        @Test
        @DisplayName("加载后数量方法返回1")
        void 加载后数量() {
            注册表.从配置加载(配置);
            assertEquals(1, 注册表.数量());
        }

        @Test
        @DisplayName("加载后字段正确填充（含 NoteBlockAPI ID 和物理机制）")
        void 加载字段正确() {
            注册表.从配置加载(配置);
            乐器定义 钢琴 = 注册表.查找("钢琴").orElseThrow();
            assertEquals("钢琴", 钢琴.获取标识());
            assertEquals("钢琴", 钢琴.获取显示名称());
            assertEquals(Sound.BLOCK_NOTE_BLOCK_HARP, 钢琴.获取原版音色());
            assertEquals(java.util.Optional.of("xrm.instrument.piano"), 钢琴.获取资源包音色键());
            assertEquals(Material.STICK, 钢琴.获取物品材质());
            assertEquals(9001, 钢琴.获取自定义模型数据());
            assertEquals(2, 钢琴.获取默认音域());
            assertEquals(0, 钢琴.获取音域下限());
            assertEquals(24, 钢琴.获取音域上限());
            assertEquals(0, 钢琴.获取八度偏移());
            assertTrue(钢琴.是否力度敏感());
            assertEquals("钢琴音色", 钢琴.获取描述());
            assertEquals(java.util.Optional.of(0), 钢琴.获取NoteBlockAPI乐器ID());
            assertEquals(乐器定义.物理机制_瞬时触发, 钢琴.获取物理机制());
        }

        @Test
        @DisplayName("null配置返回0不抛异常")
        void null配置返回零() {
            assertEquals(0, 注册表.从配置加载(null));
        }

        @Test
        @DisplayName("配置无乐器段返回0")
        void 无乐器段返回零() {
            when(配置.getConfigurationSection("乐器")).thenReturn(null);
            assertEquals(0, 注册表.从配置加载(配置));
        }
    }

    @Nested
    @DisplayName("查找")
    class 查找测试 {

        @BeforeEach
        void 加载() {
            注册表.从配置加载(配置);
        }

        @Test
        @DisplayName("按标识查找钢琴成功")
        void 按标识查找成功() {
            assertTrue(注册表.查找("钢琴").isPresent());
        }

        @Test
        @DisplayName("按标识查找不存在返回empty")
        void 按标识查找不存在() {
            assertTrue(注册表.查找("不存在").isEmpty());
        }

        @Test
        @DisplayName("按名称查找(显示名称)成功")
        void 按名称查找显示名称() {
            assertTrue(注册表.按名称查找("钢琴").isPresent());
        }

        @Test
        @DisplayName("按自定义模型数据查找9001成功")
        void 按CMD查找成功() {
            assertTrue(注册表.按自定义模型数据查找(9001).isPresent());
        }

        @Test
        @DisplayName("按自定义模型数据查找不存在返回empty")
        void 按CMD查找不存在() {
            assertTrue(注册表.按自定义模型数据查找(9999).isEmpty());
        }

        @Test
        @DisplayName("按 NoteBlockAPI 乐器 ID 查找 0 成功")
        void 按NoteBlockAPI查找成功() {
            assertTrue(注册表.按NoteBlockAPI乐器ID查找(0).isPresent());
        }

        @Test
        @DisplayName("按 NoteBlockAPI 乐器 ID 查找不存在返回empty")
        void 按NoteBlockAPI查找不存在() {
            assertTrue(注册表.按NoteBlockAPI乐器ID查找(99).isEmpty());
        }

        @Test
        @DisplayName("null标识查找返回empty")
        void null查找返回empty() {
            assertTrue(注册表.查找(null).isEmpty());
        }

        @Test
        @DisplayName("空字符串标识查找返回empty")
        void 空字符串查找返回empty() {
            assertTrue(注册表.查找("").isEmpty());
        }
    }

    @Nested
    @DisplayName("默认乐器")
    class 默认乐器测试 {

        @Test
        @DisplayName("加载后默认乐器为钢琴")
        void 默认乐器为钢琴() {
            注册表.从配置加载(配置);
            乐器定义 默认 = 注册表.默认乐器().orElseThrow();
            assertEquals("钢琴", 默认.获取标识());
        }

        @Test
        @DisplayName("未加载时默认乐器为empty")
        void 未加载默认为empty() {
            assertTrue(注册表.默认乐器().isEmpty());
        }
    }

    @Nested
    @DisplayName("热重载")
    class 热重载测试 {

        @Test
        @DisplayName("重新加载不同配置后乐器更新")
        void 热重载更新() {
            注册表.从配置加载(配置);
            assertEquals(1, 注册表.数量());

            FileConfiguration 新配置 = mock(FileConfiguration.class);
            when(新配置.getConfigurationSection("乐器")).thenReturn(null);
            注册表.从配置加载(新配置);
            assertEquals(0, 注册表.数量());
        }

        @Test
        @DisplayName("热重载后旧引用查询返回新结果")
        void 热重载后查询返回新结果() {
            注册表.从配置加载(配置);
            assertTrue(注册表.查找("钢琴").isPresent());

            FileConfiguration 新配置 = mock(FileConfiguration.class);
            when(新配置.getConfigurationSection("乐器")).thenReturn(null);
            注册表.从配置加载(新配置);
            assertTrue(注册表.查找("钢琴").isEmpty());
        }
    }

    @Nested
    @DisplayName("音高转Pitch")
    class 音高转Pitch测试 {

        @Test
        @DisplayName("音高12 → pitch 1.0")
        void 音高12为中音() {
            注册表.从配置加载(配置);
            乐器定义 钢琴 = 注册表.查找("钢琴").orElseThrow();
            assertEquals(1.0f, 钢琴.音高转Pitch(12), 0.001f);
        }

        @Test
        @DisplayName("音高0 → pitch 0.5（最低音）")
        void 音高0为最低音() {
            注册表.从配置加载(配置);
            乐器定义 钢琴 = 注册表.查找("钢琴").orElseThrow();
            assertEquals(0.5f, 钢琴.音高转Pitch(0), 0.001f);
        }

        @Test
        @DisplayName("音高24 → pitch 2.0（最高音）")
        void 音高24为最高音() {
            注册表.从配置加载(配置);
            乐器定义 钢琴 = 注册表.查找("钢琴").orElseThrow();
            assertEquals(2.0f, 钢琴.音高转Pitch(24), 0.001f);
        }
    }

    @Nested
    @DisplayName("全部标识与全部")
    class 集合查询测试 {

        @Test
        @DisplayName("全部标识包含钢琴")
        void 全部标识() {
            注册表.从配置加载(配置);
            assertTrue(注册表.全部标识().contains("钢琴"));
        }

        @Test
        @DisplayName("全部集合大小为1")
        void 全部集合() {
            注册表.从配置加载(配置);
            assertEquals(1, 注册表.全部().size());
        }

        @Test
        @DisplayName("未加载时全部标识为空")
        void 未加载全部标识为空() {
            assertTrue(注册表.全部标识().isEmpty());
        }

        @Test
        @DisplayName("全部显示名称包含钢琴")
        void 全部显示名称包含钢琴() {
            注册表.从配置加载(配置);
            assertTrue(注册表.全部显示名称().contains("钢琴"));
        }
    }

    @Nested
    @DisplayName("全局参数")
    class 全局参数测试 {

        @Test
        @DisplayName("未加载时全局参数为默认值")
        void 未加载全局参数默认() {
            乐器注册表.全局参数 全局 = 注册表.获取全局参数();
            assertEquals(48.0, 全局.获取听觉范围());
            assertTrue(全局.是否音符粒子效果());
            assertEquals(20, 全局.获取最大乐谱存储数());
            assertEquals(300, 全局.获取最大录制时长秒());
            assertEquals(120, 全局.获取默认速度BPM());
            assertEquals(4, 全局.获取默认时值Tick());
            assertEquals(1.0, 全局.获取默认力度());
            assertEquals(1.0, 全局.获取默认音量());
            assertEquals(2, 全局.获取合奏最小人数());
        }

        @Test
        @DisplayName("加载真实资源配置后全局参数正确")
        void 加载真实配置全局参数() {
            FileConfiguration 真实配置 = 加载资源配置("乐器配置.yml");
            乐器注册表 真实注册表 = new 乐器注册表();
            真实注册表.从配置加载(真实配置);
            乐器注册表.全局参数 全局 = 真实注册表.获取全局参数();
            assertEquals(48.0, 全局.获取听觉范围());
            assertTrue(全局.是否音符粒子效果());
            assertEquals(20, 全局.获取最大乐谱存储数());
            assertEquals(300, 全局.获取最大录制时长秒());
            assertEquals(120, 全局.获取默认速度BPM());
            assertEquals(2, 全局.获取合奏最小人数());
        }
    }

    @Nested
    @DisplayName("23 种乐器完整加载（资源文件）")
    class 二十三种乐器加载测试 {

        private 乐器注册表 真实注册表;

        @BeforeEach
        void 加载真实配置() {
            FileConfiguration 真实配置 = 加载资源配置("乐器配置.yml");
            真实注册表 = new 乐器注册表();
            真实注册表.从配置加载(真实配置);
        }

        @Test
        @DisplayName("加载 23 种乐器")
        void 加载23种() {
            assertEquals(23, 真实注册表.数量());
        }

        @Test
        @DisplayName("包含全部 16 种原版基础乐器")
        void 包含原版基础16种() {
            String[] 原版基础 = {"钢琴", "贝斯", "底鼓", "军鼓", "镲", "吉他", "长笛", "铃",
                    "棒状钟", "木琴", "铁木琴", "牛铃", "迪吉里杜管", "方波", "班卓琴", "合成音"};
            for (String 标识 : 原版基础) {
                assertTrue(真实注册表.查找(标识).isPresent(), "缺少原版基础乐器: " + 标识);
            }
        }

        @Test
        @DisplayName("包含全部 6 种头颅乐器")
        void 包含头颅6种() {
            String[] 头颅 = {"凋灵骷髅头", "骷髅头", "僵尸头", "苦力怕头", "末影龙头", "猪灵头"};
            for (String 标识 : 头颅) {
                assertTrue(真实注册表.查找(标识).isPresent(), "缺少头颅乐器: " + 标识);
            }
        }

        @Test
        @DisplayName("包含小号（Trumpet）")
        void 包含小号() {
            assertTrue(真实注册表.查找("小号").isPresent());
        }

        @Test
        @DisplayName("钢琴字段完整正确")
        void 钢琴字段完整() {
            乐器定义 钢琴 = 真实注册表.查找("钢琴").orElseThrow();
            assertEquals("钢琴", 钢琴.获取显示名称());
            assertEquals(Sound.BLOCK_NOTE_BLOCK_HARP, 钢琴.获取原版音色());
            assertEquals(9001, 钢琴.获取自定义模型数据());
            assertEquals(0, 钢琴.获取NoteBlockAPI乐器ID().orElseThrow());
            assertEquals(乐器定义.物理机制_瞬时触发, 钢琴.获取物理机制());
            assertEquals(0, 钢琴.获取八度偏移());
        }

        @Test
        @DisplayName("贝斯字段完整正确（慢衰减、八度偏移-2）")
        void 贝斯字段完整() {
            乐器定义 贝斯 = 真实注册表.查找("贝斯").orElseThrow();
            assertEquals(Sound.BLOCK_NOTE_BLOCK_BASS, 贝斯.获取原版音色());
            assertEquals(9002, 贝斯.获取自定义模型数据());
            assertEquals(1, 贝斯.获取NoteBlockAPI乐器ID().orElseThrow());
            assertEquals(乐器定义.物理机制_慢衰减, 贝斯.获取物理机制());
            assertEquals(-2, 贝斯.获取八度偏移());
        }

        @Test
        @DisplayName("长笛字段完整正确（持续激励、八度偏移+1）")
        void 长笛字段完整() {
            乐器定义 长笛 = 真实注册表.查找("长笛").orElseThrow();
            assertEquals(Sound.BLOCK_NOTE_BLOCK_FLUTE, 长笛.获取原版音色());
            assertEquals(6, 长笛.获取NoteBlockAPI乐器ID().orElseThrow());
            assertEquals(乐器定义.物理机制_持续激励, 长笛.获取物理机制());
            assertEquals(1, 长笛.获取八度偏移());
        }

        @Test
        @DisplayName("凋灵骷髅头字段完整正确（IMITATE 音色、NoteBlockAPI ID 16）")
        void 凋灵骷髅头字段完整() {
            乐器定义 凋灵骷髅头 = 真实注册表.查找("凋灵骷髅头").orElseThrow();
            assertEquals(Sound.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON, 凋灵骷髅头.获取原版音色());
            assertEquals(16, 凋灵骷髅头.获取NoteBlockAPI乐器ID().orElseThrow());
            assertEquals(乐器定义.物理机制_慢衰减, 凋灵骷髅头.获取物理机制());
            assertEquals(Material.WITHER_SKELETON_SKULL, 凋灵骷髅头.获取物品材质());
        }

        @Test
        @DisplayName("骷髅头 NoteBlockAPI ID 未配置（-1）")
        void 骷髅头NoteBlockAPI未配置() {
            乐器定义 骷髅头 = 真实注册表.查找("骷髅头").orElseThrow();
            assertTrue(骷髅头.获取NoteBlockAPI乐器ID().isEmpty());
        }

        @Test
        @DisplayName("小号 NoteBlockAPI ID 为 17")
        void 小号NoteBlockAPI为17() {
            乐器定义 小号 = 真实注册表.查找("小号").orElseThrow();
            assertEquals(17, 小号.获取NoteBlockAPI乐器ID().orElseThrow());
        }

        @Test
        @DisplayName("按 NoteBlockAPI 乐器 ID 查找 0-17 全部成功")
        void 按NoteBlockAPI查找全部() {
            for (int id = 0; id <= 15; id++) {
                assertTrue(真实注册表.按NoteBlockAPI乐器ID查找(id).isPresent(),
                        "NoteBlockAPI ID " + id + " 应能找到对应乐器");
            }
            assertTrue(真实注册表.按NoteBlockAPI乐器ID查找(16).isPresent(), "NoteBlockAPI ID 16 应找到凋灵骷髅头");
            assertTrue(真实注册表.按NoteBlockAPI乐器ID查找(17).isPresent(), "NoteBlockAPI ID 17 应找到小号");
        }

        @Test
        @DisplayName("按自定义模型数据查找全部 23 个 CMD 成功")
        void 按CMD查找全部() {
            int[] cmds = {9001, 9002, 9003, 9004, 9013, 9007, 9006, 9012,
                    9005, 9008, 9009, 9010, 9011, 9014, 9015, 9016,
                    9017, 9018, 9019, 9020, 9021, 9022, 9023};
            for (int cmd : cmds) {
                assertTrue(真实注册表.按自定义模型数据查找(cmd).isPresent(),
                        "CustomModelData " + cmd + " 应能找到对应乐器");
            }
        }

        @Test
        @DisplayName("默认乐器为钢琴")
        void 默认乐器为钢琴() {
            乐器定义 默认 = 真实注册表.默认乐器().orElseThrow();
            assertEquals("钢琴", 默认.获取标识());
        }

        @Test
        @DisplayName("全部显示名称数量为 23")
        void 全部显示名称数量() {
            assertEquals(23, 真实注册表.全部显示名称().size());
        }

        @Test
        @DisplayName("全部标识数量为 23")
        void 全部标识数量() {
            assertEquals(23, 真实注册表.全部标识().size());
        }
    }

    /**
     * 从测试 classpath 加载资源配置文件。
     * src/main/resources/乐器配置.yml 会被 Gradle 加入测试 classpath。
     */
    private static FileConfiguration 加载资源配置(String 资源名) {
        InputStream 输入流 = 乐器注册表测试.class.getClassLoader().getResourceAsStream(资源名);
        Objects.requireNonNull(输入流, "资源文件未找到: " + 资源名);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(输入流, StandardCharsets.UTF_8));
    }
}
