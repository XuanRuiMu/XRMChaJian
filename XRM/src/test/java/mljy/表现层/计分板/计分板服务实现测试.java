package mljy.表现层.计分板;

import mljy.业务层.技能冷却服务;
import mljy.业务层.技能注册服务;
import mljy.业务层.资源注册服务;
import mljy.业务层.资源变更服务;
import mljy.资源服务;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.领域层.技能.技能按键类型;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("计分板服务实现")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 计分板服务实现测试 {
    @Mock private 玩家服务 玩家服务;
    @Mock private 技能冷却服务 技能冷却服务;
    @Mock private 技能注册服务 技能注册服务;
    @Mock private 资源注册服务 资源注册服务;
    @Mock private 资源服务 资源服务;
    @Mock private 资源变更服务 资源变更服务;
    @Mock private 翻译服务 翻译服务;
    @Mock private JavaPlugin 插件;
    @Mock private FileConfiguration 配置;
    @Mock private ConfigurationSection 专精编号区段;

    private 计分板服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        when(插件.getConfig()).thenReturn(配置);
        when(配置.getConfigurationSection("技能.专精编号映射")).thenReturn(专精编号区段);
        when(专精编号区段.getKeys(false)).thenReturn(Set.of("奥能法师"));
        when(专精编号区段.getInt("奥能法师", 0)).thenReturn(1);
        服务 = new 计分板服务实现(玩家服务, 技能冷却服务, 技能注册服务, 资源注册服务, 资源服务, 资源变更服务, 翻译服务, 插件);
        玩家标识 = UUID.randomUUID();
        when(翻译服务.获取(anyString(), any(Object[].class))).thenAnswer(调用 -> {
            String 键 = 调用.getArgument(0);
            Object[] 参数 = Arrays.copyOfRange(调用.getArguments(), 1, 调用.getArguments().length);
            String 文本;
            switch (键) {
                case "计分板.标题": return "暮澜纪元MMORPG";
                case "计分板.空槽位": return "空槽位";
                case "计分板.自定义技能": return "自定义技能";
                case "计分板.图标对齐填充字符": return " ";
                case "资源类型.秘能": return "秘能";
                case "计分板.资源行": 文本 = "{0}:{1}/{2}"; break;
                case "计分板.空槽位行": 文本 = "{0}"; break;
                case "计分板.技能就绪行": 文本 = "{0}{1}:{2}"; break;
                case "计分板.技能冷却中行": 文本 = "{0}{1}:{2}"; break;
                case "计分板.技能冷却格式": 文本 = "{0}/{1}秒"; break;
                default:
                    if (键.startsWith("skill.")) return "测试技能";
                    return 键;
            }
            if (参数 != null) {
                for (int i = 0; i < 参数.length; i++) {
                    文本 = 文本.replace("{" + i + "}", String.valueOf(参数[i]));
                }
            }
            return 文本;
        });
        玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
    }

    private String 调用构建资源行(UUID 标识) throws Exception {
        Method 方法 = 计分板服务实现.class.getDeclaredMethod("构建资源行", UUID.class);
        方法.setAccessible(true);
        return (String) 方法.invoke(服务, 标识);
    }

    private String 调用构建技能行(UUID 标识, 技能按键类型 按键类型) throws Exception {
        Method 方法 = 计分板服务实现.class.getDeclaredMethod("构建技能行", UUID.class, 技能按键类型.class);
        方法.setAccessible(true);
        return (String) 方法.invoke(服务, 标识, 按键类型);
    }

    private void 设置秘能资源(double 当前值, double 上限) {
        资源定义 定义 = new 资源定义("秘能", "秘能", 上限, 0, "秘能显示键");
        when(资源注册服务.获取所有资源定义()).thenReturn(List.of(定义));
        when(资源服务.获取资源(玩家标识, "秘能")).thenReturn(Optional.of(new 资源("秘能", 当前值, 上限)));
    }

    private 技能定义 创建技能定义(double 冷却时间) {
        return new 技能定义(
                "1_1", "skill.奥能法师.slot.一.name", 施法类型.瞬发,
                0, 0, 冷却时间, 0, null, 0, 10, 5, 1,
                true, false, false);
    }

    private void 设置技能定义(技能按键类型 按键类型, double 冷却时间) {
        String 技能标识 = "1_" + 按键类型.获取槽位编号();
        技能定义 定义 = new 技能定义(
                技能标识, "skill.奥能法师.slot.一.name", 施法类型.瞬发,
                0, 0, 冷却时间, 0, null, 0, 10, 5, 1,
                true, false, false);
        when(技能注册服务.获取定义(技能标识)).thenReturn(Optional.of(定义));
        when(技能冷却服务.获取剩余冷却(玩家标识, 技能标识)).thenReturn(0.0);
    }

    @Nested
    @DisplayName("秘能个位数着色")
    class 秘能个位数着色 {
        @Test
        @DisplayName("秘能 0/4 → §5§l秘能:§f0/§44")
        void 秘能零四() throws Exception {
            设置秘能资源(0, 4);
            assertEquals("§5§l秘能:§f0/§44", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 1/4 → §5§l秘能:§11/§44")
        void 秘能一四() throws Exception {
            设置秘能资源(1, 4);
            assertEquals("§5§l秘能:§11/§44", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 2/4 → §5§l秘能:§22/§44")
        void 秘能二四() throws Exception {
            设置秘能资源(2, 4);
            assertEquals("§5§l秘能:§22/§44", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 3/4 → §5§l秘能:§33/§44")
        void 秘能三四() throws Exception {
            设置秘能资源(3, 4);
            assertEquals("§5§l秘能:§33/§44", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 4/4 → §5§l秘能:§44/§44")
        void 秘能四四() throws Exception {
            设置秘能资源(4, 4);
            assertEquals("§5§l秘能:§44/§44", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 5/5 → §5§l秘能:§55/§55")
        void 秘能五五() throws Exception {
            设置秘能资源(5, 5);
            assertEquals("§5§l秘能:§55/§55", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("秘能 10/14 → §5§l秘能:§a10/§e14")
        void 秘能十十四() throws Exception {
            设置秘能资源(10, 14);
            assertEquals("§5§l秘能:§a10/§e14", 调用构建资源行(玩家标识));
        }
    }

    @Nested
    @DisplayName("技能行格式")
    class 技能行格式 {
        @Test
        @DisplayName("第一技能就绪态：§c⚔§c测试技能:§f0/§f5秒")
        void 第一技能就绪() throws Exception {
            设置技能定义(技能按键类型.第一技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);
            assertEquals("§c⚔§c测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第一技能冷却中：整行渲染为深红色")
        void 第一技能冷却中() throws Exception {
            String 技能标识 = "1_1";
            技能定义 定义 = 创建技能定义(5.0);
            when(技能注册服务.获取定义(技能标识)).thenReturn(Optional.of(定义));
            when(技能冷却服务.获取剩余冷却(玩家标识, 技能标识)).thenReturn(3.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);
            assertEquals("§4⚔§4测试技能:§43/§45秒", 行);
        }

        @Test
        @DisplayName("第二技能就绪态：§6🪓§6测试技能:§f0/§f5秒（🪓实际16px无需补空格）")
        void 第二技能就绪() throws Exception {
            设置技能定义(技能按键类型.第二技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第二技能);
            assertEquals("§6🪓§6测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第三技能就绪态：§e⛏§e测试技能:§f0/§f5秒")
        void 第三技能就绪() throws Exception {
            设置技能定义(技能按键类型.第三技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第三技能);
            assertEquals("§e⛏§e测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第四技能就绪态：§a🥄 §a测试技能:§f0/§f5秒（🥄实际12px需补1空格对齐到16px）")
        void 第四技能就绪() throws Exception {
            设置技能定义(技能按键类型.第四技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第四技能);
            assertEquals("§a🥄 §a测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第五技能就绪态：§b🌾§b测试技能:§f0/§f5秒")
        void 第五技能就绪() throws Exception {
            设置技能定义(技能按键类型.第五技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第五技能);
            assertEquals("§b🌾§b测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第六技能就绪态：§9🔱§9测试技能:§f0/§f5秒")
        void 第六技能就绪() throws Exception {
            设置技能定义(技能按键类型.第六技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第六技能);
            assertEquals("§9🔱§9测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第七技能就绪态：§5🔨§5测试技能:§f0/§f5秒")
        void 第七技能就绪() throws Exception {
            设置技能定义(技能按键类型.第七技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第七技能);
            assertEquals("§5🔨§5测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第八技能就绪态：§7📚§7测试技能:§f0/§f5秒")
        void 第八技能就绪() throws Exception {
            设置技能定义(技能按键类型.第八技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第八技能);
            assertEquals("§7📚§7测试技能:§f0/§f5秒", 行);
        }

        @Test
        @DisplayName("第九技能(自定义)就绪态：§f⭐自定义技能:§f0/§f0秒（需求.md第1635-1639行无空格）")
        void 第九技能自定义() throws Exception {
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第九技能);
            assertEquals("§f⭐自定义技能:§f0/§f0秒", 行);
        }

        @Test
        @DisplayName("自定义技能行无 {0}/{1} 占位符")
        void 自定义技能无占位符() throws Exception {
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第九技能);
            assertFalse(行.contains("{0}"), "自定义技能行不应包含 {0} 占位符: " + 行);
            assertFalse(行.contains("{1}"), "自定义技能行不应包含 {1} 占位符: " + 行);
        }
    }

    @Nested
    @DisplayName("emoji 替换")
    class Emoji替换 {
        @Test
        @DisplayName("技能行无中文字'剑/斧/镐/铲/锄/戟/锤/书'图标")
        void 无中文字图标() throws Exception {
            设置技能定义(技能按键类型.第一技能, 5.0);
            设置技能定义(技能按键类型.第二技能, 5.0);
            设置技能定义(技能按键类型.第三技能, 5.0);
            设置技能定义(技能按键类型.第四技能, 5.0);
            设置技能定义(技能按键类型.第五技能, 5.0);
            设置技能定义(技能按键类型.第六技能, 5.0);
            设置技能定义(技能按键类型.第七技能, 5.0);
            设置技能定义(技能按键类型.第八技能, 5.0);
            for (技能按键类型 按键类型 : 技能按键类型.values()) {
                String 行 = 调用构建技能行(玩家标识, 按键类型);
                assertFalse(行.contains("剑"), "技能行不应包含'剑'字: " + 行);
                assertFalse(行.contains("斧"), "技能行不应包含'斧'字: " + 行);
                assertFalse(行.contains("镐"), "技能行不应包含'镐'字: " + 行);
                assertFalse(行.contains("铲"), "技能行不应包含'铲'字: " + 行);
                assertFalse(行.contains("锄"), "技能行不应包含'锄'字: " + 行);
                assertFalse(行.contains("戟"), "技能行不应包含'戟'字: " + 行);
                assertFalse(行.contains("锤"), "技能行不应包含'锤'字: " + 行);
                assertFalse(行.contains("书"), "技能行不应包含'书'字: " + 行);
            }
        }

        @Test
        @DisplayName("第一技能行包含 ⚔ emoji")
        void 第一技能含剑emoji() throws Exception {
            设置技能定义(技能按键类型.第一技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);
            assertTrue(行.contains("⚔"), "第一技能行应包含 ⚔ emoji: " + 行);
        }

        @Test
        @DisplayName("第六技能行包含 🔱 emoji（矛）")
        void 第六技能含矛emoji() throws Exception {
            设置技能定义(技能按键类型.第六技能, 5.0);
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第六技能);
            assertTrue(行.contains("🔱"), "第六技能行应包含 🔱 emoji: " + 行);
        }

        @Test
        @DisplayName("第九技能行包含 ⭐ emoji")
        void 第九技能含星emoji() throws Exception {
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第九技能);
            assertTrue(行.contains("⭐"), "第九技能行应包含 ⭐ emoji: " + 行);
        }

        @Test
        @DisplayName("第九技能图标后无对齐填充空格（需求.md第1635-1639行：⭐自定义技能无空格）")
        void 第九技能无对齐填充() throws Exception {
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第九技能);
            assertFalse(行.startsWith("§f⭐ 自定义技能"), "第九技能行图标后不应有填充空格: " + 行);
            assertTrue(行.startsWith("§f⭐自定义技能"), "第九技能行应以⭐紧接自定义技能开头: " + 行);
        }
    }

    @Nested
    @DisplayName("边界与异常")
    class 边界与异常 {
        @Test
        @DisplayName("无资源定义时资源行返回空字符串")
        void 无资源定义返回空() throws Exception {
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of());
            assertEquals("", 调用构建资源行(玩家标识));
        }

        @Test
        @DisplayName("无玩家会话时技能行返回空槽位行")
        void 无玩家会话返回空槽位() throws Exception {
            UUID 未知玩家 = UUID.randomUUID();
            when(玩家服务.获取会话(未知玩家)).thenReturn(Optional.empty());
            String 行 = 调用构建技能行(未知玩家, 技能按键类型.第一技能);
            assertEquals("§8⚔空槽位:§f0/§f0秒", 行);
        }

        @Test
        @DisplayName("无技能定义时技能行返回空槽位行")
        void 无技能定义返回空槽位() throws Exception {
            String 技能标识 = "1_1";
            when(技能注册服务.获取定义(技能标识)).thenReturn(Optional.empty());
            String 行 = 调用构建技能行(玩家标识, 技能按键类型.第一技能);
            assertEquals("§8⚔空槽位:§f0/§f0秒", 行);
        }

        @Test
        @DisplayName("大范围资源(上限100)走比例着色，不走个位数着色")
        void 大范围资源比例着色() throws Exception {
            资源定义 定义 = new 资源定义("生命值", "生命值", 100, 0, "生命值显示键");
            when(资源注册服务.获取所有资源定义()).thenReturn(List.of(定义));
            when(资源服务.获取资源(玩家标识, "生命值")).thenReturn(Optional.of(new 资源("生命值", 50, 100)));
            String 行 = 调用构建资源行(玩家标识);
            assertTrue(行.startsWith("§5§l生命值:"), "资源行应以 §5§l生命值: 开头: " + 行);
            assertTrue(行.contains("/"), "资源行应包含斜杠: " + 行);
            assertFalse(行.contains("§1") && 行.contains("§2") && 行.contains("§3"),
                    "大范围资源不应走个位数着色: " + 行);
        }
    }
}
