package mljy.技能实现.奥能法师;

import mljy.业务层.效果注册服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.技能释放服务;
import mljy.业务层.资源变更服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.技能执行结果;
import mljy.领域层.玩家.玩家快照;
import 暮澜纪元.通用.文本.关键词解析器;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 奥能冥想测试。
 *
 * FP-04 资源事务：
 *   - 施法者离线/为null 时不得清空秘能
 *   - 取消任务时释放延迟事件租约（源码结构校验）
 *
 * FP-6 治疗与智力日志：
 *   - 秘能不足时不发送技能日志（由技能释放服务统一发送"技能日志.失败.资源不足"）
 *   - 释放成功时发送 奥能冥想.施放 日志（参数含消耗秘能与治疗量），同时抑制通用释放日志
 *   - 蓄能完成时发送 奥能冥想.效果结束 日志（参数含智力加成点数）
 *
 * 玩家可见输出测试规范：除验证翻译键与参数外，加载 zh.yml 翻译文件并验证最终文本包含期望关键词。
 */
@DisplayName("奥能冥想 - FP-04 资源事务 / FP-6 治疗与智力日志")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 奥能冥想测试 {

    @Mock private JavaPlugin 插件;
    @Mock private 资源变更服务 资源变更服务;
    @Mock private 消息服务 消息服务;
    @Mock private 效果调度服务 效果调度服务;
    @Mock private 效果注册服务 效果注册服务;
    @Mock private 技能释放服务 技能释放服务;
    @Mock private Player 在线玩家;
    @Mock private World 世界;
    @Mock private BukkitScheduler 调度器;
    @Mock private BukkitTask 蓄能任务句柄;
    @Mock private AttributeInstance 最大生命属性;

    private 奥能冥想 技能;
    private UUID 施法者标识;
    private 技能上下文 上下文;
    private 玩家快照 施法者快照;
    private YamlConfiguration 翻译文件;

    @BeforeEach
    void setUp() throws Exception {
        技能 = new 奥能冥想();
        施法者标识 = UUID.randomUUID();
        注入字段("插件", 插件);
        注入字段("资源变更服务", 资源变更服务);
        注入字段("消息服务", 消息服务);
        注入字段("效果调度服务", 效果调度服务);
        注入字段("效果注册服务", 效果注册服务);
        注入字段("技能释放服务实例", 技能释放服务);
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 0.0,
                0.0, 0.0, 100.0,
                0.0, 0.0, 100.0,
                0.0, 1.5, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0);
        施法者快照 = new 玩家快照(施法者标识, "测试法师", 1, null, 属性, Map.of(), null);
        技能定义 定义 = new 技能定义(
                "1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发,
                0.0, 0.0, 60.0, 1.5, "", 0.0, 0.0, 0.0, 1, true, false, false);
        上下文 = new 技能上下文(施法者快照, "1_7", 定义, null, System.currentTimeMillis());

        when(在线玩家.isOnline()).thenReturn(true);
        when(在线玩家.isDead()).thenReturn(false);
        when(在线玩家.getUniqueId()).thenReturn(施法者标识);
        when(在线玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(最大生命属性);
        when(最大生命属性.getValue()).thenReturn(100.0);
        when(世界.getName()).thenReturn("world");

        翻译文件 = YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(
                        Path.of("src/main/resources/文本消息/技能/法术/奥能法师/奥能冥想/zh.yml")),
                        StandardCharsets.UTF_8));
    }

    private void 注入字段(String 名称, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(名称);
        字段.setAccessible(true);
        字段.set(技能, 值);
    }

    // ==================== FP-04 资源事务 ====================

    @Test
    @DisplayName("施法者离线时不得清空秘能")
    void 施法者离线_不清空秘能() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(4.0);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(null);
            assertDoesNotThrow(() -> 技能.执行(上下文));
        }

        verify(资源变更服务, never()).清空(施法者标识, "秘能");
    }

    @Test
    @DisplayName("施法者快照为null时不得访问资源")
    void 施法者为null_不访问资源() {
        技能上下文 空上下文 = new 技能上下文(null, "1_7", 上下文.技能定义(), null, System.currentTimeMillis());

        assertDoesNotThrow(() -> 技能.执行(空上下文));

        verify(资源变更服务, never()).获取当前值(施法者标识, "秘能");
        verify(资源变更服务, never()).清空(施法者标识, "秘能");
    }

    @Test
    @DisplayName("取消任务时应同步释放延迟事件租约")
    void 生命周期取消_释放延迟事件租约() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/奥能冥想.java"), StandardCharsets.UTF_8);

        assertTrue(源码.contains("注册异步租约(玩家标识, 异步租约)"));
        assertTrue(源码.contains("租约集合.forEach(事件日志上下文.异步事件租约::释放)"));
        assertTrue(源码.contains("public void 取消全部任务(String 原因)"));
    }

    @Test
    @DisplayName("FP-02 释放/持续特效不得再生成超过玩家身高(2格)的天空光柱粒子")
    void FP02_无超高天空光柱循环() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/奥能冥想.java"), StandardCharsets.UTF_8);
        assertFalse(源码.contains("天空光柱高度; y += 1.2"),
                "奥能冥想释放/持续特效不得保留超过2格高的天空光柱粒子循环");
    }

    // ==================== FP-6 治疗与智力日志 ====================

    @Test
    @DisplayName("秘能不足时不发送技能日志，由技能释放服务统一发送失败日志")
    void 秘能不足_不发技能日志_由技能释放服务统一发失败日志() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(0.0);
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));

        技能执行结果 结果;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            结果 = assertDoesNotThrow(() -> 技能.执行(上下文));
        }

        assertEquals(技能执行结果.资源不足, 结果, "秘能不足时应返回资源不足结果");
        verify(消息服务, never()).发送技能日志(eq(施法者快照), any(UUID.class), any(String.class));
        verify(技能释放服务, never()).抑制通用释放日志(any(), any());
    }

    @Test
    @DisplayName("释放成功时发送 奥能冥想.施放 日志，参数含消耗秘能与治疗量，同时抑制通用释放日志")
    void 释放成功_发送施放日志且参数正确() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
        final double[] 当前生命值 = {20.0};
        when(在线玩家.getHealth()).thenAnswer(inv -> 当前生命值[0]);
        doAnswer(inv -> {
            当前生命值[0] = Math.min((Double) inv.getArgument(0), 100.0);
            return null;
        }).when(在线玩家).setHealth(anyDouble());
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskLater(eq(插件), any(Runnable.class), anyLong())).thenReturn(蓄能任务句柄);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(蓄能任务句柄);

            assertDoesNotThrow(() -> 技能.执行(上下文));
        }

        verify(技能释放服务).抑制通用释放日志(施法者标识, "1_7");
        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("奥能冥想.施放"),
                eq("3"),
                eq("20.0"));

        String 最终文本 = 翻译文件.getString("奥能冥想.施放");
        assertNotNull(最终文本, "翻译键 奥能冥想.施放 必须存在于 zh.yml");
        assertTrue(最终文本.contains("消耗"), "施放文本应包含'消耗'，实际：" + 最终文本);
        assertTrue(最终文本.contains("治疗了"), "施放文本应包含'治疗了'，实际：" + 最终文本);
        assertTrue(最终文本.contains("{0}"), "施放文本应含占位符{0}，实际：" + 最终文本);
        assertTrue(最终文本.contains("{1}"), "施放文本应含占位符{1}，实际：" + 最终文本);
    }

    @Test
    @DisplayName("蓄能完成时发送 奥能冥想.效果结束 日志，参数含智力加成点数")
    void 蓄能完成_发送效果结束日志() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
        when(在线玩家.getHealth()).thenReturn(20.0);
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));

        ArgumentCaptor<Runnable> 蓄能任务捕获 = ArgumentCaptor.forClass(Runnable.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskLater(eq(插件), 蓄能任务捕获.capture(), anyLong())).thenReturn(蓄能任务句柄);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(蓄能任务句柄);

            assertDoesNotThrow(() -> 技能.执行(上下文));
            蓄能任务捕获.getValue().run();
        }

        verify(消息服务).发送技能日志(
                eq(施法者快照),
                any(UUID.class),
                eq("奥能冥想.效果结束"),
                eq("3.0"));

        String 最终文本 = 翻译文件.getString("奥能冥想.效果结束");
        assertNotNull(最终文本, "翻译键 奥能冥想.效果结束 必须存在于 zh.yml");
        assertTrue(最终文本.contains("蓄能完成"), "效果结束文本应包含'蓄能完成'，实际：" + 最终文本);
        assertTrue(最终文本.contains("临时智力值"), "效果结束文本应包含'临时智力值'，实际：" + 最终文本);
        assertTrue(最终文本.contains("点"), "效果结束文本应包含'点'，实际：" + 最终文本);
        assertTrue(最终文本.contains("{0}"), "效果结束文本应含占位符{0}，实际：" + 最终文本);
    }

    @Test
    @DisplayName("FP-06 效果结束日志最终渲染包含'获得了1.0点临时智力值'")
    void 效果结束_最终渲染含临时智力值() {
        String 模板 = 翻译文件.getString("奥能冥想.效果结束");
        assertNotNull(模板, "翻译键 奥能冥想.效果结束 必须存在于 zh.yml");
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        String 渲染 = 解析器.解析(模板, "1.0");
        String 纯文本 = 渲染.replaceAll("<[^>]+>", "");
        assertTrue(纯文本.contains("获得了1.0点临时智力值"),
                "效果结束日志最终渲染应包含'获得了1.0点临时智力值'，实际：" + 纯文本);
    }

    @Test
    @DisplayName("蓄能完成时智力加成点数=等级×秘能（等级≠1时仍正确）")
    void 蓄能完成_智力加成点数为等级乘秘能_等级非1() {
        // setUp默认等级=1，1×秘能=秘能，无法暴露×等级因子缺失；本测试用等级=5验证
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 0.0,
                0.0, 0.0, 100.0,
                0.0, 0.0, 100.0,
                0.0, 1.5, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0);
        玩家快照 高等级施法者 = new 玩家快照(施法者标识, "测试法师", 5, null, 属性, Map.of(), null);
        技能定义 定义 = new 技能定义(
                "1_7", "skill.奥能法师.奥能冥想.name", 施法类型.瞬发,
                0.0, 0.0, 60.0, 1.5, "", 0.0, 0.0, 0.0, 1, true, false, false);
        技能上下文 高等级上下文 = new 技能上下文(高等级施法者, "1_7", 定义, null, System.currentTimeMillis());

        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
        when(在线玩家.getHealth()).thenReturn(20.0);
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));

        ArgumentCaptor<Runnable> 蓄能任务捕获 = ArgumentCaptor.forClass(Runnable.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
            when(调度器.runTaskLater(eq(插件), 蓄能任务捕获.capture(), anyLong())).thenReturn(蓄能任务句柄);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(蓄能任务句柄);

            assertDoesNotThrow(() -> 技能.执行(高等级上下文));
            蓄能任务捕获.getValue().run();
        }

        // 等级=5, 秘能=3, 智力加成=5×3=15.0
        verify(消息服务).发送技能日志(
                eq(高等级施法者),
                any(UUID.class),
                eq("奥能冥想.效果结束"),
                eq("15.0"));
    }

    @Test
    @DisplayName("FP-4 施放日志与效果结束日志使用相同事件标识，确保日志可合并")
    void 施放与效果结束_使用相同事件标识() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(3.0);
        final double[] 当前生命值 = {20.0};
        when(在线玩家.getHealth()).thenAnswer(inv -> 当前生命值[0]);
        doAnswer(inv -> {
            当前生命值[0] = Math.min((Double) inv.getArgument(0), 100.0);
            return null;
        }).when(在线玩家).setHealth(anyDouble());
        when(在线玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));

        ArgumentCaptor<UUID> 施放事件标识 = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> 结束事件标识 = ArgumentCaptor.forClass(UUID.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(在线玩家);
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            bukkit.when(Bukkit::getScheduler).thenReturn(调度器);
            ArgumentCaptor<Runnable> 蓄能任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            when(调度器.runTaskLater(eq(插件), 蓄能任务捕获.capture(), anyLong())).thenReturn(蓄能任务句柄);
            when(调度器.runTaskTimer(eq(插件), any(Runnable.class), anyLong(), anyLong())).thenReturn(蓄能任务句柄);

            assertDoesNotThrow(() -> 技能.执行(上下文));
            蓄能任务捕获.getValue().run();
        }

        verify(消息服务).发送技能日志(
                eq(施法者快照),
                施放事件标识.capture(),
                eq("奥能冥想.施放"),
                any(),
                any());
        verify(消息服务).发送技能日志(
                eq(施法者快照),
                结束事件标识.capture(),
                eq("奥能冥想.效果结束"),
                any());
        assertEquals(施放事件标识.getValue(), 结束事件标识.getValue(),
                "施放日志与效果结束日志必须使用同一事件标识以保证日志合并");
    }
}
