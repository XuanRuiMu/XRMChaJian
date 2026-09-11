package mljy.全流程文本输出测试;

import mljy.业务层.技能冷却服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.资源注册服务;
import mljy.业务层.资源变更服务;
import mljy.资源服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.表现层.计分板.计分板服务实现;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;
import mljy.领域层.技能.技能按键类型;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-01 计分板全流程文本输出测试。
 *
 * 验证计分板服务实现的资源行和技能行构建逻辑符合翻译文件 + §传统颜色代码规范。
 * 使用反射访问私有方法 构建资源行(UUID) 和 构建技能行(UUID, 技能按键类型)，
 * 捕获实际输出字符串，验证 §颜色代码、文本片段、数值格式。
 *
 * 计分板使用 §传统颜色代码（非 MiniMessage），由代码硬编码注入：
 * - 资源名颜色：§5§l
 * - 总值颜色（白色，用于数字和斜杠）：§f
 * - 技能名颜色：对应槽位色（第一技能§c，第二§6，...第九§f），仅就绪态用槽位色
 * - 冷却中颜色：§4（深红色，按需求.md第1696行"冷却中的技能均显示为深红色"）
 * - 数字颜色：就绪态用白色§f，冷却中用深红色§4
 * - 空槽位颜色：§8
 */
@DisplayName("FP-TEXT-01 计分板全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 计分板全流程测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 技能冷却服务 技能冷却服务;
    @Mock
    private 技能注册服务 技能注册服务;
    @Mock
    private 资源注册服务 资源注册服务;
    @Mock
    private 资源服务 资源服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private JavaPlugin 插件;

    private 测试翻译服务 翻译服务;
    private 计分板服务实现 计分板服务;

    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/计分板/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/计分板/技能冷却计分板管理器/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);

        FileConfiguration 配置 = new YamlConfiguration();
        配置.set("技能.专精编号映射.奥能法师", 1);
        when(插件.getConfig()).thenReturn(配置);

        计分板服务 = new 计分板服务实现(
                玩家服务, 技能冷却服务, 技能注册服务, 资源注册服务,
                资源服务, 资源变更服务, 翻译服务, 插件);

        玩家标识 = UUID.randomUUID();
    }

    private void 加载翻译文件(YamlConfiguration 合并配置, String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                合并配置.set(键, 配置.get(键));
            }
        }
    }

    private String 调用构建资源行(UUID 玩家标识) throws Exception {
        Method 方法 = 计分板服务实现.class.getDeclaredMethod("构建资源行", UUID.class);
        方法.setAccessible(true);
        return (String) 方法.invoke(计分板服务, 玩家标识);
    }

    private String 调用构建技能行(UUID 玩家标识, 技能按键类型 按键类型) throws Exception {
        Method 方法 = 计分板服务实现.class.getDeclaredMethod("构建技能行", UUID.class, 技能按键类型.class);
        方法.setAccessible(true);
        return (String) 方法.invoke(计分板服务, 玩家标识, 按键类型);
    }

    @Nested
    @DisplayName("资源行全流程：资源定义 → 资源服务查询 → §颜色代码构建")
    class 资源行全流程 {

        @Test
        @DisplayName("资源行：捕获实际输出应包含资源名颜色(§5§l)和总值颜色(§f)")
        void 资源行_应包含资源名和总值颜色代码() throws Exception {
            资源定义 定义 = new 资源定义("怒气", "怒气", 100.0, 1.0, "资源类型.怒气");
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of(定义));
            资源 资源 = new 资源("怒气", 50.0, 100.0);
            when(资源服务.获取资源(玩家标识, "怒气")).thenReturn(Optional.of(资源));

            String 行 = 调用构建资源行(玩家标识);

            assertTrue(行.contains("§5§l"), "资源名应使用§5§l颜色，实际：" + 行);
            assertTrue(行.contains("§f"), "应包含§f总值颜色（大范围资源上限用§f），实际：" + 行);
            assertTrue(行.contains("怒气"), "应包含资源名'怒气'，实际：" + 行);
        }

        @Test
        @DisplayName("资源行：捕获实际输出应包含当前值/上限格式")
        void 资源行_应包含当前值和上限格式() throws Exception {
            资源定义 定义 = new 资源定义("秘能", "秘能", 4.0, 1.0, "资源类型.秘能");
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of(定义));
            资源 资源 = new 资源("秘能", 2.0, 4.0);
            when(资源服务.获取资源(玩家标识, "秘能")).thenReturn(Optional.of(资源));

            String 行 = 调用构建资源行(玩家标识);

            assertTrue(行.contains("2"), "应包含当前值'2'，实际：" + 行);
            assertTrue(行.contains("4"), "应包含上限'4'，实际：" + 行);
            assertTrue(行.contains("/"), "应包含斜杠分隔符，实际：" + 行);
        }

        @Test
        @DisplayName("资源行：无资源定义时应返回空字符串")
        void 资源行_无资源定义时应返回空字符串() throws Exception {
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of());

            String 行 = 调用构建资源行(玩家标识);

            assertTrue(行.isEmpty(), "无资源定义时应返回空字符串，实际：" + 行);
        }

        @Test
        @DisplayName("资源行：大范围上限资源已满时应使用§e颜色")
        void 资源行_大范围已满时应使用黄色颜色() throws Exception {
            资源定义 定义 = new 资源定义("怒气", "怒气", 100.0, 1.0, "资源类型.怒气");
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of(定义));
            资源 资源 = new 资源("怒气", 100.0, 100.0);
            when(资源服务.获取资源(玩家标识, "怒气")).thenReturn(Optional.of(资源));

            String 行 = 调用构建资源行(玩家标识);

            assertTrue(行.contains("§e"), "大范围已满(100%)应使用§e颜色（§0-§e方案最高档），实际：" + 行);
        }
    }

    @Nested
    @DisplayName("技能行全流程：冷却中/就绪/空槽位")
    class 技能行全流程 {

        @Test
        @DisplayName("冷却中技能行：技能名和数字均用深红色(§4)（按需求.md第1696行）")
        void 冷却中技能行_应包含槽位色和白色数字() throws Exception {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            技能定义 定义 = new 技能定义(
                    "1_1", "skill.奥能法师.slot.一.name", 施法类型.蓄力,
                    0.5, 0.0, 3.0, 1.5, "", 0.0, 10.0, 1.0, 1, true, false, false);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能冷却服务.获取剩余冷却(玩家标识, "1_1")).thenReturn(2.0);

            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);

            // 按需求.md第1696行：冷却中的技能均显示为深红色（§4）
            assertTrue(行.contains("§4"), "冷却中技能应使用§4深红色（按需求.md第1696行），实际：" + 行);
            assertFalse(行.contains("§c"), "冷却中技能不应使用槽位色§c（按需求.md第1696行冷却中显示深红色），实际：" + 行);
            assertTrue(行.contains("秒"), "应包含'秒'单位，实际：" + 行);
        }

        @Test
        @DisplayName("就绪技能行：技能名用槽位色(§c)，数字用白色(§f)")
        void 就绪技能行_应包含槽位色和白色数字() throws Exception {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            技能定义 定义 = new 技能定义(
                    "1_1", "skill.奥能法师.slot.一.name", 施法类型.蓄力,
                    0.5, 0.0, 3.0, 1.5, "", 0.0, 10.0, 1.0, 1, true, false, false);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能冷却服务.获取剩余冷却(玩家标识, "1_1")).thenReturn(0.0);

            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);

            assertTrue(行.contains("§c"), "第一技能槽位应使用§c颜色，实际：" + 行);
            assertTrue(行.contains("§f"), "就绪数字应使用白色§f，实际：" + 行);
            assertFalse(行.contains("§a"), "就绪数字不应再使用§a绿色，实际：" + 行);
            assertTrue(行.contains("0"), "就绪时应显示0秒剩余，实际：" + 行);
        }

        @Test
        @DisplayName("空槽位技能行：捕获实际输出应包含空槽位颜色(§8)和空槽位文本")
        void 空槽位技能行_应包含空槽位颜色() throws Exception {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);

            assertTrue(行.contains("§8"), "空槽位应使用§8颜色，实际：" + 行);
            assertTrue(行.contains("空槽位"), "应包含'空槽位'文本，实际：" + 行);
        }

        @Test
        @DisplayName("第九技能自定义行：捕获实际输出应包含自定义技能颜色(§f)")
        void 第九技能自定义行_应包含自定义技能颜色() throws Exception {
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第九技能);

            assertTrue(行.contains("§f"), "自定义技能应使用§f颜色，实际：" + 行);
            assertTrue(行.contains("自定义技能"), "应包含'自定义技能'文本，实际：" + 行);
        }
    }

    private static class 测试翻译服务 implements 翻译服务 {
        private final YamlConfiguration 配置;

        测试翻译服务(YamlConfiguration 配置) {
            this.配置 = 配置;
        }

        @Override
        public String 获取(String 键, Locale 语言, Object... 参数) {
            return 获取(键, 参数);
        }

        @Override
        public String 获取(String 键, Object... 参数) {
            String 文本 = 配置.getString(键);
            if (文本 == null) {
                return 键;
            }
            if (参数 == null || 参数.length == 0) {
                return 文本;
            }
            if (文本.contains("{0}")) {
                String 结果 = 文本;
                for (int i = 0; i < 参数.length; i++) {
                    结果 = 结果.replace("{" + i + "}", String.valueOf(参数[i]));
                }
                return 结果;
            }
            return 文本;
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
