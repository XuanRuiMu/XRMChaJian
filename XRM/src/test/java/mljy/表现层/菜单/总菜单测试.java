package mljy.表现层.菜单;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.领域层.玩家.玩家会话;
import mljy.表现层.命令.玩家指令.技能日志命令处理器;
import mljy.表现层.命令.玩家指令.战斗日志命令处理器;
import com.google.inject.Provider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FP-09 总菜单 - bug1 修复验证（任务系统打开任务菜单）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 总菜单测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 技能日志命令处理器 技能日志处理器;
    @Mock
    private 战斗日志命令处理器 战斗日志处理器;
    @Mock
    private 菜单服务 菜单服务实例;
    @Mock
    private Player 玩家;
    @Mock
    private 菜单占位符 占位符;
    @Mock
    private 菜单条件 条件;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 总菜单配置 配置;
    private Provider<菜单服务> 菜单服务提供器;
    private 总菜单 菜单;
    private YamlConfiguration 翻译文件;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/菜单/总菜单/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        配置 = new 总菜单配置(new YamlConfiguration());
        菜单服务提供器 = () -> 菜单服务实例;
        菜单 = new 总菜单(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器,
                玩家服务, 技能日志处理器, 战斗日志处理器, 配置);

        玩家标识 = UUID.randomUUID();
        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("测试玩家");
    }

    private void 加载翻译文件(String 相对路径) throws Exception {
        File 文件 = new File(相对路径);
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(文件.toPath()), StandardCharsets.UTF_8));
        for (String 键 : 配置.getKeys(true)) {
            if (!配置.isConfigurationSection(键)) {
                翻译文件.set(键, 配置.get(键));
            }
        }
    }

    @Nested
    @DisplayName("按钮结构 - 4按钮槽位与物品验证")
    class 按钮结构 {

        @Test
        @DisplayName("构建配置 - 应返回4个按钮")
        void 构建配置_应返回4个按钮() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            assertEquals(4, 配置结果.按钮().size(), "应返回4个按钮");
        }

        @Test
        @DisplayName("任务系统按钮 - 槽位20 物品BOOK")
        void 任务系统按钮_槽位20_物品BOOK() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 任务系统按钮 = 查找按钮(配置结果, 20);
            assertNotNull(任务系统按钮, "任务系统按钮应存在于槽位20");
            assertEquals("BOOK", 任务系统按钮.物品标识(), "任务系统物品应为BOOK");
        }

        @Test
        @DisplayName("属性系统按钮 - 槽位28 物品DIAMOND_SWORD")
        void 属性系统按钮_槽位28_物品DIAMOND_SWORD() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 属性系统按钮 = 查找按钮(配置结果, 28);
            assertNotNull(属性系统按钮, "属性系统按钮应存在于槽位28");
            assertEquals("DIAMOND_SWORD", 属性系统按钮.物品标识(), "属性系统物品应为DIAMOND_SWORD");
        }

        @Test
        @DisplayName("技能日志按钮 - 槽位11 物品FEATHER")
        void 技能日志按钮_槽位11_物品FEATHER() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 技能日志按钮 = 查找按钮(配置结果, 11);
            assertNotNull(技能日志按钮, "技能日志按钮应存在于槽位11");
            assertEquals("FEATHER", 技能日志按钮.物品标识(), "技能日志物品应为FEATHER");
        }

        @Test
        @DisplayName("战斗日志按钮 - 槽位13 物品IRON_SWORD")
        void 战斗日志按钮_槽位13_物品IRON_SWORD() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 战斗日志按钮 = 查找按钮(配置结果, 13);
            assertNotNull(战斗日志按钮, "战斗日志按钮应存在于槽位13");
            assertEquals("IRON_SWORD", 战斗日志按钮.物品标识(), "战斗日志物品应为IRON_SWORD");
        }
    }

    @Nested
    @DisplayName("bug1修复验证 - 任务系统按钮动作")
    class Bug1修复验证 {

        @Test
        @DisplayName("任务系统按钮 - 左键动作应为打开菜单(任务菜单)")
        void 任务系统按钮_动作应为打开任务菜单() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 任务系统按钮 = 查找按钮(配置结果, 20);
            assertNotNull(任务系统按钮);
            菜单动作 动作 = 任务系统按钮.动作().get("左键");
            assertNotNull(动作, "左键动作不应为空");
            assertEquals(菜单动作类型.打开菜单, 动作.类型(), "动作类型应为打开菜单");
            assertEquals("任务菜单", 动作.参数(), "动作参数应为任务菜单");
        }
    }

    @Nested
    @DisplayName("附魔光效 - 状态决定光效")
    class 附魔光效 {

        @Test
        @DisplayName("技能日志开启 - 技能日志按钮应有附魔光效")
        void 技能日志开启_技能日志按钮应有附魔光效() {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            会话.设置技能日志开关(true);
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 技能日志按钮 = 查找按钮(配置结果, 11);
            assertTrue(技能日志按钮.附魔光效(), "技能日志开启时按钮应有附魔光效");
        }

        @Test
        @DisplayName("技能日志关闭 - 技能日志按钮不应有附魔光效")
        void 技能日志关闭_技能日志按钮不应有附魔光效() {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            会话.设置技能日志开关(false);
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 技能日志按钮 = 查找按钮(配置结果, 11);
            assertFalse(技能日志按钮.附魔光效(), "技能日志关闭时按钮不应有附魔光效");
        }

        @Test
        @DisplayName("战斗日志开启 - 战斗日志按钮应有附魔光效")
        void 战斗日志开启_战斗日志按钮应有附魔光效() {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            会话.设置战斗日志开关(true);
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 战斗日志按钮 = 查找按钮(配置结果, 13);
            assertTrue(战斗日志按钮.附魔光效(), "战斗日志开启时按钮应有附魔光效");
        }

        @Test
        @DisplayName("战斗日志关闭 - 战斗日志按钮不应有附魔光效")
        void 战斗日志关闭_战斗日志按钮不应有附魔光效() {
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            会话.设置战斗日志开关(false);
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 战斗日志按钮 = 查找按钮(配置结果, 13);
            assertFalse(战斗日志按钮.附魔光效(), "战斗日志关闭时按钮不应有附魔光效");
        }
    }

    @Nested
    @DisplayName("会话不存在 - 使用默认false状态")
    class 会话不存在 {

        @Test
        @DisplayName("会话不存在 - 技能日志和战斗日志按钮都不应有附魔光效")
        void 会话不存在_日志按钮不应有附魔光效() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            菜单配置 配置结果 = 菜单.构建配置(玩家);

            菜单按钮 技能日志按钮 = 查找按钮(配置结果, 11);
            assertFalse(技能日志按钮.附魔光效(), "会话不存在时技能日志按钮不应有附魔光效");
            菜单按钮 战斗日志按钮 = 查找按钮(配置结果, 13);
            assertFalse(战斗日志按钮.附魔光效(), "会话不存在时战斗日志按钮不应有附魔光效");
        }
    }

    private 菜单按钮 查找按钮(菜单配置 配置, int 槽位) {
        return 配置.按钮().stream()
                .filter(b -> b.槽位() == 槽位)
                .findFirst()
                .orElse(null);
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
            return 文本 != null ? 文本 : 键;
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
