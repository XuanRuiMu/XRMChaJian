package mljy.全流程文本输出测试;

import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.业务层.消息服务实现;
import mljy.业务层.消息.日志合并管理器;
import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.业务层.消息.伤害日志格式化服务实现;
import mljy.翻译服务;
import mljy.基础设施层.XRM技能名解析器;
import mljy.表现层.监听器.躲闪监听器;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-06 躲闪提示全流程文本输出测试。
 *
 * 验证玩家被攻击触发躲闪成功时，从躲闪监听器到战斗日志的完整流程。
 * 使用真实消息服务实现（不mock），mock日志合并管理器捕获战斗日志内容，
 * 验证最终文本符合翻译文件 + 关键词解析器规范。
 *
 * 躲闪翻译键：战斗日志.躲闪 = "你[躲闪]了来自{0}的普通攻击。"
 * 其中 {0} 会被替换为攻击者名字。
 */
@DisplayName("FP-TEXT-06 躲闪提示全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 躲闪提示全流程测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private 日志合并管理器 日志合并管理器;
    @Mock
    private 战斗日志上下文管理器 战斗日志上下文管理器;
    @Mock
    private JavaPlugin 插件;
    @Mock
    private Player 受击者;
    @Mock
    private Player 攻击者;
    @Mock
    private EntityDamageByEntityEvent 事件;

    private 测试翻译服务 翻译服务;
    private 关键词解析器 关键词解析器;
    private 消息服务实现 消息服务;
    private 伤害日志格式化服务实现 伤害日志格式化服务;
    private 躲闪监听器 监听器;

    private UUID 受击者标识;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration 翻译文件 = new YamlConfiguration();
        加载翻译文件(翻译文件, "src/main/resources/文本消息/common/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/战斗/伤害服务/zh.yml");
        加载翻译文件(翻译文件, "src/main/resources/文本消息/战斗日志/zh.yml");

        翻译服务 = new 测试翻译服务(翻译文件);
        关键词解析器 = new 关键词解析器(new XRM技能名解析器(翻译服务, 玩家服务));
        消息服务 = new 消息服务实现(翻译服务, 关键词解析器, 玩家服务, 日志合并管理器, 战斗日志上下文管理器);
        伤害日志格式化服务 = new 伤害日志格式化服务实现(翻译服务);

        try (var _ = org.mockito.Mockito.mockConstruction(NamespacedKey.class)) {
            监听器 = new 躲闪监听器(玩家服务, 属性计算服务, 消息服务, 伤害日志格式化服务, 插件);
        }

        受击者标识 = UUID.randomUUID();
        when(受击者.getUniqueId()).thenReturn(受击者标识);
        when(受击者.getName()).thenReturn("玄锐暮");
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(false);
        when(受击者.getPersistentDataContainer()).thenReturn(pdc);
        when(攻击者.getName()).thenReturn("怪物");
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

    private 玩家快照 创建快照() {
        return new 玩家快照(受击者标识, "玄锐暮", 1, null, null, Collections.emptyMap(), 战斗状态.非战斗);
    }

    @Nested
    @DisplayName("躲闪成功全流程：受击 → 躲闪判定 → 战斗日志 → 日志合并")
    class 躲闪成功全流程 {

        @Test
        @DisplayName("100%躲闪：战斗日志应包含攻击者名字和'躲闪'关键词")
        void 百分之百躲闪_应输出完整中文躲闪提示() {
            玩家快照 快照 = 创建快照();
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(玩家服务.获取默认战斗日志开关()).thenReturn(true);
            属性快照 属性 = 属性快照.创建(100, 1.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0);
            when(属性计算服务.计算(快照)).thenReturn(属性);
            when(属性计算服务.计算派生值(eq("躲闪几率"), any())).thenReturn(1.0);

            监听器.实体被攻击(事件);

            verify(事件).setCancelled(true);
            ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(受击者标识), any(UUID.class), 捕获器.capture(), anyInt());
            String 纯文本 = PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(捕获器.getValue()));
            assertTrue(纯文本.contains("怪物"), "应包含攻击者名字'怪物'，实际：" + 纯文本);
            assertTrue(纯文本.contains("躲闪"), "应包含'躲闪'关键词，实际：" + 纯文本);
        }

        @Test
        @DisplayName("100%躲闪：战斗日志应为完整格式")
        void 百分之百躲闪_应输出完整句子() {
            玩家快照 快照 = 创建快照();
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(玩家服务.获取默认战斗日志开关()).thenReturn(true);
            属性快照 属性 = 属性快照.创建(100, 1.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0);
            when(属性计算服务.计算(快照)).thenReturn(属性);
            when(属性计算服务.计算派生值(eq("躲闪几率"), any())).thenReturn(1.0);

            监听器.实体被攻击(事件);

            ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
            verify(日志合并管理器).添加战斗日志(eq(受击者标识), any(UUID.class), 捕获器.capture(), anyInt());
            String 纯文本 = PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(捕获器.getValue()));
            assertEquals("你躲闪了来自怪物的普通攻击。", 纯文本,
                    "应输出完整句子'你躲闪了来自怪物的普通攻击。'，实际：" + 纯文本);
        }
    }

    @Nested
    @DisplayName("边界值与异常场景：0%躲闪、XRM技能伤害、非玩家受击者")
    class 边界值与异常 {

        @Test
        @DisplayName("0%躲闪：不应取消事件也不应发送消息")
        void 百分之零躲闪_不应取消事件也不应发送消息() {
            玩家快照 快照 = 创建快照();
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            属性快照 属性 = 属性快照.创建(100, 1.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            when(属性计算服务.计算(快照)).thenReturn(属性);
            when(属性计算服务.计算派生值(eq("躲闪几率"), any())).thenReturn(0.0);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(受击者, never()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("XRM技能伤害：PDC有标记时不应处理躲闪")
        void XRM技能伤害_不应处理躲闪() {
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.DOUBLE))).thenReturn(true);
            when(受击者.getPersistentDataContainer()).thenReturn(pdc);
            when(事件.getEntity()).thenReturn(受击者);

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(受击者, never()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("会话不存在：不应发送消息")
        void 会话不存在_不应发送消息() {
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.empty());

            监听器.实体被攻击(事件);

            verify(事件, never()).setCancelled(true);
            verify(受击者, never()).sendMessage(any(Component.class));
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
