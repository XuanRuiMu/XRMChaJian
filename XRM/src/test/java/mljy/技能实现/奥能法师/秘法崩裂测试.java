package mljy.技能实现.奥能法师;

import mljy.业务层.资源变更服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.战斗服务;
import mljy.翻译服务;
import mljy.领域层.实体;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.施法类型;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.效果.效果实例;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

/**
 * 秘法崩裂 FP-04 资源事务回归测试。
 */
@DisplayName("秘法崩裂 - FP-04 资源事务")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 秘法崩裂测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private 目标选择服务 目标选择服务;
    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private Player 玩家;
    @Mock
    private World 世界;
    @Mock
    private BukkitScheduler 调度器;
    @Mock
    private BukkitTask 调度任务;
    @Mock
    private 实体 目标;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 秘兆效果处理器 秘兆效果处理器;

    private static class 事务测试技能 extends 秘法崩裂 {
        private final 资源变更服务 资源服务;
        private final UUID 玩家标识;
        private boolean 已执行范围伤害;

        private 事务测试技能(资源变更服务 资源服务, UUID 玩家标识) {
            this.资源服务 = 资源服务;
            this.玩家标识 = 玩家标识;
        }

        @Override
        protected boolean 应用范围伤害并记录日志(玩家快照 施法者, 位置 中心, double 半径, double 基础伤害,
                                             int 衰减阈值, String 技能名翻译键,
                                             int 每目标秘能获取量, boolean 消耗秘兆) {
            verify(资源服务, org.mockito.Mockito.times(1)).清空(玩家标识, "秘能");
            已执行范围伤害 = true;
            return true;
        }
    }

    private static class 精通测试技能 extends 秘法崩裂 {
        private double 捕获的基础伤害 = Double.NaN;
        private int 调用次数 = 0;

        @Override
        protected 伤害结果 应用单体伤害(玩家快照 施法者, 实体 目标, double 基础伤害, boolean 是否暴击, boolean 消耗秘兆) {
            捕获的基础伤害 = 基础伤害;
            调用次数++;
            return new 伤害结果(基础伤害, false, false);
        }
    }

    private 秘法崩裂 技能;
    private UUID 施法者标识;
    private 玩家快照 施法者快照;
    private 技能上下文 上下文;

    @BeforeEach
    void setUp() throws Exception {
        施法者标识 = UUID.randomUUID();
        技能 = new 事务测试技能(资源变更服务, 施法者标识);
        注入父类字段(技能, "插件", 插件);
        注入父类字段(技能, "资源变更服务", 资源变更服务);
        注入父类字段(技能, "目标选择服务", 目标选择服务);
        注入父类字段(技能, "战斗服务", 战斗服务);
        注入父类字段(技能, "消息服务", 消息服务);
        注入父类字段(技能, "翻译服务", 翻译服务);
        注入父类字段(技能, "效果调度服务", 效果调度服务);
        注入父类字段(技能, "秘兆效果处理器", 秘兆效果处理器);

        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        位置 玩家位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        施法者快照 = new 玩家快照(
                施法者标识, "测试法师", 1, 玩家位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);

        技能定义 定义 = new 技能定义(
                "1_2", "skill.奥能法师.秘法崩裂.name", 施法类型.瞬发,
                0.0, 0.0, 4.0, 1.5, "", 0.0, 5.0, 0.0, 1, true, false, false);
        上下文 = new 技能上下文(施法者快照, "1_2", 定义, null, System.currentTimeMillis());
    }

    private void 注入父类字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    private void 离线执行技能() {
        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(null);
            技能.执行(上下文);
        }
    }

    @Test
    @DisplayName("施法者离线时释放失败路径不得清空秘能")
    void 施法者离线_不清空秘能() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(2.0);

        离线执行技能();

        verify(资源变更服务, never()).清空(施法者标识, "秘能");
    }

    @Test
    @DisplayName("秘能为零且施法者离线时也不得产生资源变更回调")
    void 秘能为零且施法者离线_不清空() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(0.0);

        assertDoesNotThrow(() -> 离线执行技能());

        verify(资源变更服务, never()).清空(施法者标识, "秘能");
    }

    private Runnable 执行延迟任务(List<实体> 目标列表) {
        return 执行延迟任务(目标列表, 2.0);
    }

    private Runnable 执行延迟任务(List<实体> 目标列表, double 初始秘能) {
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isDead()).thenReturn(false);
        when(玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));
        when(世界.getName()).thenReturn("world");
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(初始秘能, 0.0);
        when(目标选择服务.选择敌人(any(), any(), eq(5.0))).thenReturn(目标列表);
        when(目标.获取名称()).thenReturn("测试目标");
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(10.0, false, false));
        when(调度器.runTaskTimer(any(), any(Runnable.class), eq(0L), anyLong())).thenReturn(调度任务);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getWorld(anyString())).thenReturn(世界);
            技能.执行(上下文);

            org.mockito.ArgumentCaptor<Runnable> 任务捕获 = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(调度器).runTaskTimer(eq(插件), 任务捕获.capture(), eq(0L), eq(1L));
            Runnable 任务 = 任务捕获.getValue();
            for (int i = 0; i <= 8; i++) {
                任务.run();
            }
            return 任务;
        }
    }

    private void 执行精通测试延迟任务(精通测试技能 测试技能, List<实体> 目标列表, boolean 有秘兆) throws Exception {
        注入父类字段(测试技能, "插件", 插件);
        注入父类字段(测试技能, "资源变更服务", 资源变更服务);
        注入父类字段(测试技能, "目标选择服务", 目标选择服务);
        注入父类字段(测试技能, "战斗服务", 战斗服务);
        注入父类字段(测试技能, "消息服务", 消息服务);
        注入父类字段(测试技能, "翻译服务", 翻译服务);
        注入父类字段(测试技能, "效果调度服务", 效果调度服务);
        注入父类字段(测试技能, "秘兆效果处理器", 秘兆效果处理器);

        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 10000.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        位置 玩家位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        施法者快照 = new 玩家快照(
                施法者标识, "测试法师", 1, 玩家位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);
        技能定义 定义 = new 技能定义(
                "1_2", "skill.奥能法师.秘法崩裂.name", 施法类型.瞬发,
                0.0, 0.0, 4.0, 1.5, "", 0.0, 5.0, 0.0, 1, true, false, false);
        上下文 = new 技能上下文(施法者快照, "1_2", 定义, null, System.currentTimeMillis());

        技能 = 测试技能;

        if (有秘兆) {
            效果实例 秘兆效果 = new 效果实例("1_9_1", "1_9", 1, System.currentTimeMillis() + 5000);
            when(效果调度服务.获取列表(施法者标识)).thenReturn(List.of(秘兆效果));
        } else {
            when(效果调度服务.获取列表(施法者标识)).thenReturn(Collections.emptyList());
        }

        执行延迟任务(目标列表, 1.0);
    }

    @Test
    @DisplayName("有秘能时释放前原子消费一次且重复任务不重复消费")
    void 有效目标命中_释放前消费一次() {
        Runnable 任务 = 执行延迟任务(List.of(目标));

        verify(资源变更服务).清空(施法者标识, "秘能");
        任务.run();
        verify(资源变更服务, org.mockito.Mockito.times(1)).清空(施法者标识, "秘能");
    }

    @Test
    @DisplayName("有效位置且范围伤害成功后提交已消费资源")
    void 有效位置且执行成功_提交消费() {
        事务测试技能 测试技能 = (事务测试技能) 技能;
        执行延迟任务(List.of(目标));

        verify(资源变更服务).清空(施法者标识, "秘能");
        org.junit.jupiter.api.Assertions.assertTrue(测试技能.已执行范围伤害);
    }

    @Test
    @DisplayName("目标为空时回滚释放前的资源消费")
    void 目标为空_回滚消费() {
        执行延迟任务(Collections.emptyList());

        verify(资源变更服务, org.mockito.Mockito.times(1)).清空(施法者标识, "秘能");
        verify(资源变更服务).增加(施法者标识, "秘能", 2.0);
    }

    @Test
    @DisplayName("初始秘能为零时释放前失败且无任何副作用")
    void 初始秘能为零_释放前无副作用() {
        when(玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));
        when(世界.getName()).thenReturn("world");
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(0.0);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
            bukkitMock.when(Bukkit::getScheduler).thenReturn(调度器);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(世界);
            技能.执行(上下文);
        }

        verify(资源变更服务, never()).清空(施法者标识, "秘能");
        verify(资源变更服务, never()).增加(any(), anyString(), anyDouble());
        verify(调度器, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
        verify(世界, never()).spawnParticle(any(), any(), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
        verify(世界, never()).playSound(any(Location.class), any(org.bukkit.Sound.class), anyFloat(), anyFloat());
        verify(目标选择服务, never()).选择敌人(any(), any(), anyDouble());
    }

    @Test
    @DisplayName("执行异常时回滚释放前的资源消费")
    void 目标执行异常_回滚消费() {
        when(目标选择服务.选择敌人(any(), any(), eq(5.0)))
                .thenThrow(new IllegalStateException("目标选择失败"));

        执行延迟任务(Collections.emptyList());

        verify(资源变更服务, org.mockito.Mockito.times(1)).清空(施法者标识, "秘能");
        verify(资源变更服务).增加(施法者标识, "秘能", 2.0);
    }

    @Test
    @DisplayName("世界不存在时不创建任务且不扣除秘能")
    void 世界不存在_不扣除秘能() {
        when(资源变更服务.获取当前值(施法者标识, "秘能")).thenReturn(2.0);
        when(玩家.getLocation()).thenReturn(new Location(世界, 0, 64, 0));
        when(世界.getName()).thenReturn("world");

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(施法者标识)).thenReturn(玩家);
            bukkitMock.when(() -> Bukkit.getWorld("world")).thenReturn(null);
            技能.执行(上下文);
        }

        verify(资源变更服务, never()).清空(施法者标识, "秘能");
    }

    @Test
    @DisplayName("有秘兆且精通非零时范围伤害包含精通增伤")
    void 有秘兆且精通非零_伤害包含精通增伤() throws Exception {
        精通测试技能 测试技能 = new 精通测试技能();
        执行精通测试延迟任务(测试技能, List.of(目标), true);

        // 智力=30, 秘能=1
        // 基础伤害 = 30 * 0.15 = 4.5
        // 秘能伤害 = 1 * 0.37 * 30 = 11.1
        // 总伤害 = 15.6
        // 单目标基础伤害 = floor(15.6 / 1) = 15.0
        // 精通增伤 = 15.0 * (10000 * 0.3 / 100) = 15.0 * 30 = 450.0
        // 期望捕获的基础伤害 = 15.0 + 450.0 = 465.0
        assertEquals(1, 测试技能.调用次数, "应用单体伤害应被调用一次");
        assertEquals(465.0, 测试技能.捕获的基础伤害, 0.001,
                "有秘兆且精通=10000时，基础伤害应包含精通增伤 15 + 450 = 465");
    }

    @Test
    @DisplayName("无秘兆时范围伤害不包含精通增伤")
    void 无秘兆_伤害不包含精通增伤() throws Exception {
        精通测试技能 测试技能 = new 精通测试技能();
        执行精通测试延迟任务(测试技能, List.of(目标), false);

        // 无秘兆时，精通增伤=0
        // 期望捕获的基础伤害 = 15.0
        assertEquals(1, 测试技能.调用次数, "应用单体伤害应被调用一次");
        assertEquals(15.0, 测试技能.捕获的基础伤害, 0.001,
                "无秘兆时，基础伤害应等于单目标基础伤害 15，不包含精通增伤");
    }
}
