package mljy.全流程文本输出测试;

import mljy.业务层.属性.修饰器管理器;
import mljy.业务层.属性计算服务;
import mljy.业务层.属性计算服务实现;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.玩家服务;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.基础设施层.内存属性服务;
import mljy.基础设施层.Yaml配置加载器;
import mljy.表现层.命令.属性指令.属性命令处理器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家会话;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FP-03 复现与回归测试：/sx 副属性显示为0 问题。
 *
 * 根因：源码 resources/专精属性.yml 中奥能法师的副属性（暴击几率/暴击伤害/全能/吸血/躲闪/生命恢复/移速）
 * 全部配置为0，导致 /sx 显示副属性全为0。需求文档"职业，专精与属性.md"规定这些副属性应为非0值。
 * 修复方向：修正源码配置与需求文档一致。
 *
 * 本测试通过真实内存属性服务（不mock）+ 模拟/sx命令，验证：
 * 1. 源码配置加载后奥能法师副属性非0（除精通/急速，需求文档规定为0）
 * 2. /sx 显示的副属性数值非0
 * 3. 显示保留1位小数
 * 4. 包含基础/装备/天赋/临时构成
 */
@DisplayName("FP-03 /sx副属性显示测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class sx副属性显示测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private Player 玩家;
    @Mock
    private Command 命令;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 内存属性服务 真实属性服务;
    private 修饰器管理器 共享修饰器管理器;
    private 属性命令处理器 处理器;

    private UUID 玩家标识;
    private 玩家会话 会话;

    @BeforeEach
    void setUp() throws Exception {
        // 加载源码 resources/专精属性.yml
        YamlConfiguration 专精配置 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(
                        Files.newInputStream(new File("src/main/resources/专精属性.yml").toPath()),
                        StandardCharsets.UTF_8));
        Yaml配置加载器 配置加载器 = mock(Yaml配置加载器.class);
        when(配置加载器.加载("专精属性.yml")).thenReturn(专精配置);

        // 构造真实的内存属性服务（不mock），加载源码配置
        共享修饰器管理器 = new 修饰器管理器();
        真实属性服务 = new 内存属性服务(共享修饰器管理器, 配置加载器);

        // 加载翻译文件
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/命令/属性指令/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        属性计算服务 属性计算服务 = new 属性计算服务实现(共享修饰器管理器);
        处理器 = new 属性命令处理器(翻译服务, 关键词解析器, 玩家服务, 真实属性服务, 属性计算服务, 共享修饰器管理器);

        玩家标识 = UUID.randomUUID();
        会话 = new 玩家会话(玩家标识, "玄锐暮");

        when(玩家.getUniqueId()).thenReturn(玩家标识);
        when(玩家.getName()).thenReturn("玄锐暮");
        when(玩家.getHealth()).thenReturn(20.0);
        when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
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

    private List<String> 捕获所有消息(CommandSender 发送者) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(发送者, atLeastOnce()).sendMessage(捕获器.capture());
        return 捕获器.getAllValues().stream()
                .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
    }

    private String 查找包含行(List<String> 消息列表, String 文本) {
        return 消息列表.stream()
                .filter(消息 -> 消息.contains(文本))
                .findFirst()
                .orElse("");
    }

    @Test
    @DisplayName("源码配置：奥能法师基础属性应与属性文档一致")
    void 源码配置_奥能法师基础副属性应非0() {
        属性快照 属性 = 真实属性服务.获取专精基础属性("奥能法师").orElseThrow();

        // 副属性（按属性文档"职业，专精与属性.md"奥能法师应为非0）
        assertEquals(10.0, 属性.法术暴击几率(), "法术暴击几率应为10.0");
        assertEquals(5.0, 属性.法术暴击伤害(), "法术暴击伤害应为5.0");
        assertEquals(5.0, 属性.全能(), "全能应为5.0");
        assertEquals(5.0, 属性.吸血(), "吸血应为5.0");
        assertEquals(5.0, 属性.躲闪(), "躲闪应为5.0");
        assertEquals(10.0, 属性.生命恢复(), "生命恢复应为10.0");
        assertEquals(0.1, 属性.移速(), "移速应为0.1");
        // 按属性文档"职业，专精与属性.md"第27/28/21行：精通=5，急速=5，公CD=1.2
        assertEquals(5.0, 属性.精通(), "精通应为5.0（按属性文档第27行）");
        assertEquals(5.0, 属性.急速(), "急速应为5.0（按属性文档第28行）");
        assertEquals(1.2, 属性.公共冷却时间(), "公共冷却时间应为1.2秒（按属性文档第21行）");
    }

    @Test
    @DisplayName("/sx显示：奥能法师副属性数值非0")
    void sx显示_奥能法师副属性应非0() {
        处理器.onCommand(玩家, 命令, "sx", new String[]{});

        List<String> 消息列表 = 捕获所有消息(玩家);
        String 全部消息 = String.join("\n", 消息列表);

        // 验证非0副属性显示（需求文档规定的非0值）
        assertTrue(全部消息.contains("5.0%"), "应显示5.0%（全能/吸血/躲闪/暴击伤害），实际: " + 全部消息);
        assertTrue(全部消息.contains("10.0%"), "应显示10.0%（暴击几率），实际: " + 全部消息);
        assertTrue(全部消息.contains("10.0") && 全部消息.contains("生命恢复"),
                "应显示生命恢复10.0，实际: " + 全部消息);
        assertTrue(全部消息.contains("0.1"), "应显示移速0.1，实际: " + 全部消息);

        // 验证不应所有副属性都是0
        assertFalse(全部消息.contains("全能: 0.0%"), "全能不能为0.0%，实际: " + 全部消息);
        assertFalse(全部消息.contains("吸血: 0.0%"), "吸血不能为0.0%，实际: " + 全部消息);
    }

    @Test
    @DisplayName("/sx显示：副属性数值与属性快照一致")
    void sx显示_副属性与属性快照一致() {
        处理器.onCommand(玩家, 命令, "sx", new String[]{});

        List<String> 消息列表 = 捕获所有消息(玩家);
        属性快照 属性 = 真实属性服务.计算属性(玩家标识);

        // 躲闪：显示应与属性快照一致
        String 躲闪行 = 查找包含行(消息列表, "躲闪");
        assertTrue(躲闪行.contains(String.format("%.1f", 属性.躲闪()) + "%"),
                "躲闪显示应与属性快照一致: " + 属性.躲闪() + "%，实际: " + 躲闪行);

        // 全能
        String 全能行 = 查找包含行(消息列表, "全能");
        assertTrue(全能行.contains(String.format("%.1f", 属性.全能()) + "%"),
                "全能显示应与属性快照一致: " + 属性.全能() + "%，实际: " + 全能行);

        // 吸血
        String 吸血行 = 查找包含行(消息列表, "吸血");
        assertTrue(吸血行.contains(String.format("%.1f", 属性.吸血()) + "%"),
                "吸血显示应与属性快照一致: " + 属性.吸血() + "%，实际: " + 吸血行);

        // 暴击几率
        String 暴击行 = 查找包含行(消息列表, "暴击");
        assertTrue(暴击行.contains(String.format("%.1f", 属性.法术暴击几率())),
                "法术暴击几率显示应与属性快照一致: " + 属性.法术暴击几率() + "，实际: " + 暴击行);
    }

    @Test
    @DisplayName("/sx显示：副属性保留1位小数")
    void sx显示_副属性保留1位小数() {
        处理器.onCommand(玩家, 命令, "sx", new String[]{});

        List<String> 消息列表 = 捕获所有消息(玩家);
        String 全部消息 = String.join("\n", 消息列表);

        // 5.0%、10.0%、0.1 都是1位小数格式
        assertTrue(全部消息.contains("5.0%"), "应包含5.0%（1位小数），实际: " + 全部消息);
        assertTrue(全部消息.contains("10.0%"), "应包含10.0%（1位小数），实际: " + 全部消息);
    }

    @Test
    @DisplayName("/sx显示：包含基础/装备/天赋/临时构成")
    void sx显示_包含构成() {
        处理器.onCommand(玩家, 命令, "sx", new String[]{});

        List<String> 消息列表 = 捕获所有消息(玩家);
        String 全部消息 = String.join("\n", 消息列表);

        assertTrue(全部消息.contains("基础"), "应包含'基础'构成，实际: " + 全部消息);
        assertTrue(全部消息.contains("装备"), "应包含'装备'构成，实际: " + 全部消息);
        assertTrue(全部消息.contains("天赋"), "应包含'天赋'构成，实际: " + 全部消息);
        assertTrue(全部消息.contains("临时"), "应包含'临时'构成，实际: " + 全部消息);
    }

    @Test
    @DisplayName("FP-03回归: 施加0.01移速临时修饰器后 /sx 应显示临时0.01与总移速0.11")
    void sx显示_临时移速0_01应可见() {
        修饰器 临时移速 = new 修饰器("移速", "移速_xrm_1", 修饰方式.相加, 0.01, 0);
        共享修饰器管理器.注册修饰器(玩家标识, 临时移速);

        处理器.onCommand(玩家, 命令, "sx", new String[]{});

        List<String> 消息列表 = 捕获所有消息(玩家);
        String 全部消息 = String.join("\n", 消息列表);

        // 总移速 = 基础0.1 + 临时0.01 = 0.11，必须可见（不能为0.1或0.0）
        assertTrue(全部消息.contains("0.11"), "总移速应显示0.11，实际: " + 全部消息);
        // 临时构成应显示0.01，不能为0.0
        assertTrue(全部消息.contains("临时：0.01"), "临时移速应显示0.01，实际: " + 全部消息);
        // 基础构成应为0.10（2位小数）
        assertTrue(全部消息.contains("基础：0.10"), "基础移速应显示0.10，实际: " + 全部消息);
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
