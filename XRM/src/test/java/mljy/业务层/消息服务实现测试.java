package mljy.业务层;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.基础设施层.XRM技能名解析器;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import mljy.领域层.玩家.玩家快照;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("消息服务实现")
@ExtendWith(MockitoExtension.class)
class 消息服务实现测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 日志合并管理器 日志合并管理器;
    @Mock
    private 战斗日志上下文管理器 战斗日志上下文管理器;
    @Mock
    private Player 玩家;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 消息服务实现 消息服务;
    private YamlConfiguration 翻译文件;
    private UUID 玩家标识;
    private 玩家快照 快照;
    private 玩家会话 会话;

    @BeforeEach
    void setUp() throws Exception {
        翻译文件 = new YamlConfiguration();
        加载翻译文件("src/main/resources/文本消息/common/zh.yml");
        加载翻译文件("src/main/resources/文本消息/战斗日志/zh.yml");
        加载翻译文件("src/main/resources/文本消息/技能日志/zh.yml");
        加载翻译文件("src/main/resources/文本消息/技能/法术/奥能法师/技能槽位/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        消息服务 = new 消息服务实现(翻译服务, 关键词解析器, 玩家服务, 日志合并管理器, 战斗日志上下文管理器);

        玩家标识 = UUID.randomUUID();
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, null, Map.of(), null);
        会话 = new 玩家会话(玩家标识, "测试玩家");
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

    private String 捕获并序列化(Player mock玩家) {
        ArgumentCaptor<Component> 捕获器 = ArgumentCaptor.forClass(Component.class);
        verify(mock玩家).sendMessage(捕获器.capture());
        return PlainTextComponentSerializer.plainText().serialize(捕获器.getValue());
    }

    @Nested
    @DisplayName("正常流程 - 发送消息")
    class 发送消息 {

        @Test
        @DisplayName("发送消息 - 玩家应收到中文文本")
        void 发送消息_玩家应收到中文文本() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                消息服务.发送消息(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

                String 最终文本 = 捕获并序列化(玩家);
                assertTrue(最终文本.contains("你用"), "应包含'你用'，实际: " + 最终文本);
                assertTrue(最终文本.contains("铁剑"), "应包含'铁剑'，实际: " + 最终文本);
                assertTrue(最终文本.contains("僵尸"), "应包含'僵尸'，实际: " + 最终文本);
                assertTrue(最终文本.contains("100.0"), "应包含'100.0'，实际: " + 最终文本);
                assertTrue(最终文本.contains("伤害"), "应包含'伤害'，实际: " + 最终文本);
            }
        }

        @Test
        @DisplayName("发送错误 - 玩家应收到带错误前缀的中文文本")
        void 发送错误_玩家应收到带前缀的文本() {
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                消息服务.发送错误(快照, "通用.错误.参数不足");

                String 最终文本 = 捕获并序列化(玩家);
                assertTrue(最终文本.contains("[错误]"), "应包含'[错误]'前缀，实际: " + 最终文本);
                assertTrue(最终文本.contains("参数不足"), "应包含'参数不足'，实际: " + 最终文本);
            }
        }
    }

    @Nested
    @DisplayName("UUID 重载 - 发送消息")
    class UUID重载发送消息 {

        @Test
        @DisplayName("UUID 重载发送消息 - 玩家在线时应通过快照发送中文文本")
        void UUID重载发送消息_玩家在线时应发送消息() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.of(快照));
            when(玩家.isOnline()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                消息服务.发送消息(玩家标识, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

                String 最终文本 = 捕获并序列化(玩家);
                assertTrue(最终文本.contains("你用"), "应包含'你用'，实际: " + 最终文本);
                assertTrue(最终文本.contains("铁剑"), "应包含'铁剑'，实际: " + 最终文本);
                assertTrue(最终文本.contains("100.0"), "应包含'100.0'，实际: " + 最终文本);
                assertTrue(最终文本.contains("伤害"), "应包含'伤害'，实际: " + 最终文本);
            }
        }

        @Test
        @DisplayName("UUID 重载发送消息 - 玩家不在线时不应发送消息")
        void UUID重载发送消息_玩家不在线_不应发送消息() {
            when(玩家服务.获取快照(玩家标识)).thenReturn(Optional.empty());

            消息服务.发送消息(玩家标识, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(玩家, never()).sendMessage(any(Component.class));
        }
    }

    @Nested
    @DisplayName("正常流程 - 发送技能日志")
    class 发送技能日志 {

        @Test
        @DisplayName("发送技能日志 - 应添加到日志合并管理器")
        void 发送技能日志_应添加到合并管理器() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("释放"), "日志内容应包含'释放'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("发送技能日志 - 应解析[第一技能]为技能名")
        void 发送技能日志_应解析技能槽位() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("奥术冲击"), "日志内容应包含技能名'奥术冲击'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("发送技能日志带参数 - 应正确替换参数")
        void 发送技能日志带参数_应替换参数() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.蓄力释放", "1.5");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("1.5"), "日志内容应包含'1.5'，实际: " + 日志内容);
            assertTrue(日志内容.contains("蓄力"), "日志内容应包含'蓄力'，实际: " + 日志内容);
        }
    }

    @Nested
    @DisplayName("正常流程 - 发送战斗日志")
    class 发送战斗日志 {

        @Test
        @DisplayName("发送战斗日志 - 应添加到日志合并管理器")
        void 发送战斗日志_应添加到合并管理器() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("你用"), "日志内容应包含'你用'，实际: " + 日志内容);
            assertTrue(日志内容.contains("100.0"), "日志内容应包含'100.0'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("发送战斗日志带吸血 - 应以尾缀形式在合并句末尾附加吸血日志")
        void 发送战斗日志带吸血_应附加吸血日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(25.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("你用"), "日志内容应包含'你用'，实际: " + 日志内容);

            ArgumentCaptor<Double> 吸血捕获 = ArgumentCaptor.forClass(Double.class);
            verify(日志合并管理器).设置吸血尾缀(eq(玩家标识), any(), 吸血捕获.capture());
            assertEquals(25.0, 吸血捕获.getValue(), 0.0001, "吸血尾缀应登记吸血量数值 25.0，实际: " + 吸血捕获.getValue());
        }

        @Test
        @DisplayName("发送战斗日志无吸血 - 不应附加吸血日志")
        void 发送战斗日志无吸血_不应附加吸血日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertFalse(日志内容.contains("吸血"), "无吸血时不应包含'吸血'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("发送战斗日志受击伤害 - 应使用无裸括号格式（FP-A点9）")
        void 发送战斗日志受击伤害_应无裸括号() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.受到伤害", "僵尸", "5.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("你受到了来自"), "应包含'你受到了来自'前缀，实际: " + 日志内容);
            assertTrue(日志内容.contains("僵尸"), "应包含来源名'僵尸'，实际: " + 日志内容);
            assertTrue(日志内容.contains("<dark_red>5.0</dark_red>"),
                    "伤害数值应着色<dark_red>，实际: " + 日志内容);
            assertFalse(日志内容.contains("[伤害数值:"), "不应包含裸括号'[伤害数值:'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("发送战斗日志受击暴击 - 应保留暴击标记且无裸括号（FP-A点9）")
        void 发送战斗日志受击暴击_应保留暴击标记() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.受到伤害暴击", "僵尸", "5.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("暴击"), "暴击键应保留[暴击]标记，实际: " + 日志内容);
            assertTrue(日志内容.contains("5.0"), "应包含伤害数值'5.0'，实际: " + 日志内容);
            assertFalse(日志内容.contains("[伤害数值:"), "不应包含裸括号'[伤害数值:'，实际: " + 日志内容);
        }
    }

    @Nested
    @DisplayName("边界值 - 日志开关")
    class 日志开关 {

        @Test
        @DisplayName("技能日志开关关闭 - 不应发送日志")
        void 技能日志开关关闭_不应发送日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(false);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            verify(日志合并管理器, never()).添加技能日志(any(), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("战斗日志开关关闭 - 不应发送日志且清除上下文")
        void 战斗日志开关关闭_不应发送日志且清除上下文() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(false);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString());
            verify(战斗日志上下文管理器).清除上下文(any(), any());
        }

        @Test
        @DisplayName("会话不存在 - 使用默认开启的技能日志开关时应发送日志")
        void 会话不存在_默认技能日志开启时应发送日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认技能日志开关()).thenReturn(true);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("释放"), "默认开启时应输出技能日志，实际: " + 日志内容);
        }

        @Test
        @DisplayName("会话不存在 - 使用默认关闭的技能日志开关时不应发送日志")
        void 会话不存在_默认技能日志关闭时不应发送日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认技能日志开关()).thenReturn(false);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            verify(日志合并管理器, never()).添加技能日志(any(), any(UUID.class), anyString());
        }

        @Test
        @DisplayName("会话不存在 - 使用默认开启的战斗日志开关时应发送日志")
        void 会话不存在_默认战斗日志开启时应发送日志() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认战斗日志开关()).thenReturn(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("你用"), "默认开启时应输出战斗日志，实际: " + 日志内容);
        }

        @Test
        @DisplayName("会话不存在 - 使用默认关闭的战斗日志开关时不应发送日志且清除上下文")
        void 会话不存在_默认战斗日志关闭时不应发送日志且清除上下文() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());
            when(玩家服务.获取默认战斗日志开关()).thenReturn(false);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            verify(日志合并管理器, never()).添加战斗日志(any(), any(UUID.class), anyString());
            verify(战斗日志上下文管理器).清除上下文(any(), any());
        }
    }

    @Nested
    @DisplayName("异常输入 - 玩家不在线")
    class 玩家不在线 {

        @Test
        @DisplayName("发送消息时玩家不在线 - 不应发送消息")
        void 发送消息_玩家不在线_不应发送() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                消息服务.发送消息(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

                verify(玩家, never()).sendMessage(any(Component.class));
            }
        }

        @Test
        @DisplayName("发送消息时玩家离线 - 不应发送消息")
        void 发送消息_玩家离线_不应发送() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);
                when(玩家.isOnline()).thenReturn(false);

                消息服务.发送消息(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

                verify(玩家, never()).sendMessage(any(Component.class));
            }
        }

        @Test
        @DisplayName("发送错误时玩家不在线 - 不应发送消息")
        void 发送错误_玩家不在线_不应发送() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                消息服务.发送错误(快照, "通用.错误.参数不足");

                verify(玩家, never()).sendMessage(any(Component.class));
            }
        }
    }

    @Nested
    @DisplayName("玩家可见输出 - 关键词解析验证")
    class 关键词解析验证 {

        @Test
        @DisplayName("战斗日志 - 应正确解析[伤害数值]关键词")
        void 战斗日志_应解析伤害数值关键词() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(0.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(玩家标识), any(UUID.class), 内容捕获.capture(), anyInt());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("100.0"), "应包含伤害数值'100.0'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("技能日志 - 应正确解析[第一技能]为'奥术冲击'")
        void 技能日志_应解析第一技能为奥术冲击() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.瞬发释放", "一");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("奥术冲击"), "应解析[第一技能]为'奥术冲击'，实际: " + 日志内容);
            assertFalse(日志内容.contains("[第一技能]"), "不应包含未解析的'[第一技能]'，实际: " + 日志内容);
        }

        @Test
        @DisplayName("技能日志命中 - 应正确解析所有关键词")
        void 技能日志命中_应解析所有关键词() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置技能日志开关(true);

            消息服务.发送技能日志(快照, "技能日志.命中", "僵尸", "100.0", "奥术冲击", "2");

            ArgumentCaptor<String> 内容捕获 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加技能日志(eq(玩家标识), any(UUID.class), 内容捕获.capture());
            String 日志内容 = 内容捕获.getValue();
            assertTrue(日志内容.contains("奥术冲击"), "应包含技能名'奥术冲击'，实际: " + 日志内容);
            assertTrue(日志内容.contains("僵尸"), "应包含目标名'僵尸'，实际: " + 日志内容);
            assertTrue(日志内容.contains("100.0"), "应包含伤害'100.0'，实际: " + 日志内容);
            assertTrue(日志内容.contains("秘能"), "应包含'秘能'关键词，实际: " + 日志内容);
        }

        @Test
        @DisplayName("吸血日志 - 应正确解析[吸血][治疗数值][生命值]关键词")
        void 吸血日志_应解析吸血关键词() {
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            会话.设置战斗日志开关(true);
            when(战斗日志上下文管理器.获取并清除吸血量(any(), any())).thenReturn(30.0);

            消息服务.发送战斗日志(快照, "战斗日志.普通攻击", "铁剑", "僵尸", "100.0");

            ArgumentCaptor<Double> 吸血捕获 = ArgumentCaptor.forClass(Double.class);
            verify(日志合并管理器).设置吸血尾缀(eq(玩家标识), any(), 吸血捕获.capture());
            assertEquals(30.0, 吸血捕获.getValue(), 0.0001, "吸血尾缀应登记吸血量数值 30.0，实际: " + 吸血捕获.getValue());
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
            return 文本 != null ? 文本 : 键;
        }

        @Override
        public Locale 获取当前语言() {
            return Locale.SIMPLIFIED_CHINESE;
        }
    }
}
