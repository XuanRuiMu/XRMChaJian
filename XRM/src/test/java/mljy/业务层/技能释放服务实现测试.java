package mljy.业务层;

import com.google.inject.Provider;
import mljy.玩家服务;
import mljy.技能执行器;
import mljy.翻译服务;
import mljy.表现层.监听器.技能移动打断监听器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.技能.技能执行结果;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-05 修复验证：技能释放服务公CD数据源一致性。
 *
 * 根因：原实现第91行用 定义.公共冷却()（技能定义注解硬编码1.5）作为基础公CD，
 * 而 /sx 用专精属性公共冷却时间（1.2），bossbar 用快照基础属性，
 * 三处数据源脱节导致"显示1.0实际1.5"顽固BUG。
 *
 * 修复：技能释放改用 上下文.施法者().属性().公共冷却时间()（玩家最终属性）作为基础公CD，
 * 符合需求.md 第338行"基础 GCD = 专精基础公共冷却时间"。
 * 定义.公共冷却() 仅作为是否触发公CD的开关（>0 触发）。
 */
@DisplayName("FP-05 技能释放服务公CD数据源")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能释放服务实现测试 {

    @Mock
    private 技能注册服务 技能注册服务;
    @Mock
    private 技能冷却服务 技能冷却服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private JavaPlugin 插件;
    @Mock
    private 公共冷却显示服务 公共冷却显示服务;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 技能执行器 执行器;
    @Mock
    private Provider<技能移动打断监听器> 移动打断监听器提供者;
    @Mock
    private 技能移动打断监听器 移动打断监听器;
    @Mock
    private Server 资源测试服务器;
    @Mock
    private BukkitScheduler 资源测试调度器;
    @Mock
    private BukkitTask 资源测试任务;

    private 技能释放服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        服务 = new 技能释放服务实现(技能注册服务, 技能冷却服务, 资源变更服务,
                属性计算服务, 消息服务, 翻译服务, 公共冷却显示服务, 玩家服务, 插件);
        when(移动打断监听器提供者.get()).thenReturn(移动打断监听器);
        服务.设置移动打断监听器提供者(移动打断监听器提供者);
        when(插件.getServer()).thenReturn(资源测试服务器);
        when(资源测试服务器.getScheduler()).thenReturn(资源测试调度器);
        when(资源测试调度器.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(资源测试任务);
        玩家标识 = UUID.randomUUID();
    }

    private 属性快照 创建属性快照(double 公共冷却时间, double 急速) {
        return 属性快照.创建(
                100.0, 公共冷却时间, 急速,
                0.0, 0.0, 10.0,
                0.0, 0.0, 10.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private 技能上下文 构造上下文(属性快照 属性, 技能定义 定义) {
        玩家快照 施法者 = new 玩家快照(玩家标识, "测试玩家", 1, null, 属性, Map.of(), null);
        return new 技能上下文(施法者, "1_1", 定义, null, System.currentTimeMillis());
    }

    private 技能定义 构造技能定义(double 公共冷却, 施法类型 类型) {
        return new 技能定义(
                "1_1", "skill.test", 类型,
                0.0, 0.0, 0.0, 公共冷却,
                "", 0.0, 10.0, 1.0, 1, true, false, false);
    }

    private 技能定义 构造秘能消耗技能定义(double 消耗) {
        return new 技能定义(
                "1_1", "skill.test", 施法类型.瞬发,
                0.0, 0.0, 0.0, 1.5,
                "秘能", 消耗, 10.0, 1.0, 1, true, false, false);
    }

    private void 准备释放环境(技能定义 定义) {
        when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
        when(技能注册服务.获取执行器("1_1")).thenReturn(Optional.of(执行器));
        when(技能冷却服务.是否冷却中(玩家标识, "1_1")).thenReturn(false);
        when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
        when(执行器.执行(any())).thenReturn(技能执行结果.成功);
    }

    @Nested
    @DisplayName("公CD数据源一致性（FP-05核心修复）")
    class 公CD数据源一致性 {

        @Test
        @DisplayName("主动技能公CD应使用施法者最终属性的公共冷却时间而非技能定义硬编码值")
        void 主动技能公CD应用施法者属性() {
            属性快照 属性 = 创建属性快照(1.2, 5.0);
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            准备释放环境(定义);
            when(属性计算服务.计算实际公共冷却(eq(1.2), eq(5.0))).thenReturn(1.142857);

            技能上下文 上下文 = 构造上下文(属性, 定义);
            技能执行结果 结果 = 服务.释放(上下文);

            assertEquals(技能执行结果.成功, 结果);
            verify(属性计算服务).计算实际公共冷却(eq(1.2), eq(5.0));
            verify(属性计算服务, org.mockito.Mockito.never())
                    .计算实际公共冷却(eq(1.5), anyDouble());
            ArgumentCaptor<Double> 秒数捕获 = ArgumentCaptor.forClass(Double.class);
            verify(技能冷却服务).开始公共冷却(eq(玩家标识), 秒数捕获.capture());
            assertEquals(1.142857, 秒数捕获.getValue(), 0.0001,
                    "开始公共冷却秒数应为计算实际公共冷却(1.2, 5.0)的返回值");
        }

        @Test
        @DisplayName("被动技能（定义公共冷却=0）不应触发公CD")
        void 被动技能不应触发公CD() {
            属性快照 属性 = 创建属性快照(1.2, 5.0);
            技能定义 定义 = 构造技能定义(0.0, 施法类型.瞬发);
            准备释放环境(定义);
            when(属性计算服务.计算实际公共冷却(eq(0.0), eq(5.0))).thenReturn(0.0);

            技能上下文 上下文 = 构造上下文(属性, 定义);
            技能执行结果 结果 = 服务.释放(上下文);

            assertEquals(技能执行结果.成功, 结果);
            verify(属性计算服务).计算实际公共冷却(eq(0.0), eq(5.0));
            verify(技能冷却服务).开始公共冷却(eq(玩家标识), eq(0.0));
        }

        @Test
        @DisplayName("急速属性变化时公CD应随之变化（实时计算）")
        void 急速变化公CD应变化() {
            属性快照 低急速属性 = 创建属性快照(1.2, 5.0);
            属性快照 高急速属性 = 创建属性快照(1.2, 25.0);
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            准备释放环境(定义);
            when(属性计算服务.计算实际公共冷却(eq(1.2), eq(5.0))).thenReturn(1.142857);
            when(属性计算服务.计算实际公共冷却(eq(1.2), eq(25.0))).thenReturn(0.7619);

            技能上下文 低急速上下文 = 构造上下文(低急速属性, 定义);
            服务.释放(低急速上下文);
            ArgumentCaptor<Double> 低急速秒数 = ArgumentCaptor.forClass(Double.class);
            verify(技能冷却服务).开始公共冷却(eq(玩家标识), 低急速秒数.capture());
            assertEquals(1.142857, 低急速秒数.getValue(), 0.0001);

            技能上下文 高急速上下文 = 构造上下文(高急速属性, 定义);
            service_释放_重置冷却(高急速上下文);
            ArgumentCaptor<Double> 高急速秒数 = ArgumentCaptor.forClass(Double.class);
            verify(技能冷却服务, org.mockito.Mockito.times(2))
                    .开始公共冷却(eq(玩家标识), 高急速秒数.capture());
            assertEquals(0.7619, 高急速秒数.getValue(), 0.0001, "高急速应产生更短公CD");
        }

        private void service_释放_重置冷却(技能上下文 上下文) {
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
            服务.释放(上下文);
        }
    }

    @Nested
    @DisplayName("FP-A点10 冷却提示路由（走技能日志合并，带[技能日志]前缀）")
    class 冷却提示路由 {

        @Test
        @DisplayName("公共冷却中时应通过发送技能日志发送提示而非发送消息")
        void 公共冷却中应走发送技能日志() {
            属性快照 属性 = 创建属性快照(1.2, 5.0);
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(true);
            when(技能冷却服务.获取公共冷却剩余(玩家标识)).thenReturn(0.5);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.冷却中, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.公共冷却中"), eq("0.5"));
            verify(消息服务, never()).发送消息(any(玩家快照.class), eq("技能日志.公共冷却中"), any());
        }

        @Test
        @DisplayName("独立冷却中时应通过发送技能日志发送提示而非发送消息")
        void 独立冷却中应走发送技能日志() {
            属性快照 属性 = 创建属性快照(1.2, 5.0);
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
            when(技能冷却服务.是否冷却中(玩家标识, "1_1")).thenReturn(true);
            when(技能冷却服务.获取剩余冷却(玩家标识, "1_1")).thenReturn(2.3);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.冷却中, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.独立冷却中"), eq("一"), eq("2.3"));
            verify(消息服务, never()).发送消息(any(玩家快照.class), eq("技能日志.独立冷却中"), any(), any());
        }
    }

    @Nested
    @DisplayName("FP-C 释放过程日志归类为技能日志")
    class 释放过程日志路由 {

        @Test
        @DisplayName("瞬发释放过程应进入技能日志频道而非战斗日志频道")
        void 瞬发释放过程进入技能日志() {
            属性快照 属性 = 创建属性快照(1.2, 5.0);
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            准备释放环境(定义);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.瞬发释放"), eq("一"));
            verify(消息服务, never()).发送战斗日志(any(玩家快照.class), eq(20), eq("技能日志.瞬发释放"), eq("一"));
        }

        @Test
        @DisplayName("蓄力释放过程应进入技能日志频道而非战斗日志频道")
        void 蓄力释放过程进入技能日志() {
            技能定义 定义 = new 技能定义("1_1", "skill.test", 施法类型.蓄力,
                    1.0, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(1.0), eq(20.0))).thenReturn(1.0);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.蓄力释放"), eq("1.0"), eq("一"));
            verify(消息服务, never()).发送战斗日志(any(玩家快照.class), eq(20), eq("技能日志.蓄力释放"), any(), any());
        }

        @Test
        @DisplayName("引导释放过程应进入技能日志频道而非战斗日志频道")
        void 引导释放过程进入技能日志() {
            技能定义 定义 = new 技能定义("1_5", "skill.test", 施法类型.引导,
                    0.0, 3.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(3.0), eq(20.0))).thenReturn(2.5);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.引导释放"), eq("2.5"), eq("一"));
            verify(消息服务, never()).发送战斗日志(any(玩家快照.class), eq(20), eq("技能日志.引导释放"), any(), any());
        }

        @Test
        @DisplayName("持续施法释放过程应进入技能日志频道而非战斗日志频道")
        void 持续施法释放过程进入技能日志() {
            技能定义 定义 = new 技能定义("1_4", "skill.test", 施法类型.持续施法,
                    0.4, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(0.4), eq(20.0))).thenReturn(0.5);

            技能执行结果 结果 = 服务.释放(构造上下文(属性, 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.延迟释放"), eq("0.5"), eq("一"));
            verify(消息服务, never()).发送战斗日志(any(玩家快照.class), eq(20), eq("技能日志.延迟释放"), any(), any());
        }
    }

    @Nested
    @DisplayName("FP-05 施法类型基础持续时间选择（蓄力/引导/持续施法急速验证）")
    class 施法类型基础持续时间选择 {

        @Mock
        private Server mockServer;
        @Mock
        private BukkitScheduler mockScheduler;
        @Mock
        private BukkitTask mockTask;

        @BeforeEach
        void setUpScheduler() {
            when(插件.getServer()).thenReturn(mockServer);
            when(mockServer.getScheduler()).thenReturn(mockScheduler);
            when(mockScheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mockTask);
        }

        @Test
        @DisplayName("蓄力技能应使用定义.蓄力时间()作为基础持续时间并应用急速")
        void 蓄力技能应使用蓄力时间() {
            技能定义 定义 = new 技能定义("1_1", "skill.test", 施法类型.蓄力,
                    2.0, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, true, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(2.0), eq(20.0))).thenReturn(1.667);

            服务.释放(构造上下文(属性, 定义));

            verify(属性计算服务, org.mockito.Mockito.atLeastOnce()).计算实际蓄力时间(eq(2.0), eq(20.0));
        }

        @Test
        @DisplayName("引导技能应使用定义.引导时间()作为基础持续时间并应用急速")
        void 引导技能应使用引导时间() {
            技能定义 定义 = new 技能定义("1_5", "skill.test", 施法类型.引导,
                    0.0, 3.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(3.0), eq(20.0))).thenReturn(2.5);

            服务.释放(构造上下文(属性, 定义));

            verify(属性计算服务, org.mockito.Mockito.atLeastOnce()).计算实际蓄力时间(eq(3.0), eq(20.0));
        }

        @Test
        @DisplayName("持续施法技能应使用定义.蓄力时间()作为延迟基础时间并应用急速")
        void 持续施法技能应使用蓄力时间作为延迟() {
            技能定义 定义 = new 技能定义("1_4", "skill.test", 施法类型.持续施法,
                    0.4, 0.0, 0.0, 1.5, "", 0.0, 10.0, 1.0, 1, false, false, false);
            属性快照 属性 = 创建属性快照(1.2, 20.0);
            准备释放环境(定义);
            when(属性计算服务.计算实际蓄力时间(eq(0.4), eq(20.0))).thenReturn(0.333);

            服务.释放(构造上下文(属性, 定义));

            verify(属性计算服务, org.mockito.Mockito.atLeastOnce()).计算实际蓄力时间(eq(0.4), eq(20.0));
        }
    }

    @Nested
    @DisplayName("FP-04 资源消耗事务")
    class 资源消耗事务 {

        @Test
        @DisplayName("执行器不存在时不应消耗秘能")
        void 执行器不存在_不消耗秘能() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能注册服务.获取执行器("1_1")).thenReturn(Optional.empty());
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
            when(技能冷却服务.是否冷却中(玩家标识, "1_1")).thenReturn(false);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(4.0);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.失败, 结果);
            verify(资源变更服务, never()).消耗(any(), any(), anyDouble());
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.失败.其他"), eq("一"));
        }

        @Test
        @DisplayName("执行器查找返回null时应失败且不消耗秘能")
        void 执行器查找返回null_失败且不消耗秘能() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            when(技能注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
            when(技能注册服务.获取执行器("1_1")).thenReturn(null);
            when(技能冷却服务.是否公共冷却中(玩家标识)).thenReturn(false);
            when(技能冷却服务.是否冷却中(玩家标识, "1_1")).thenReturn(false);

            技能执行结果 结果 = assertDoesNotThrow(
                    () -> 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义)));

            assertEquals(技能执行结果.失败, 结果);
            verify(资源变更服务, never()).消耗(any(), any(), anyDouble());
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.失败.其他"), eq("一"));
        }

        @Test
        @DisplayName("资源不足时释放失败且不消耗秘能")
        void 资源不足_失败且不消耗秘能() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(0.9);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.资源不足, 结果);
            verify(资源变更服务, never()).消耗(any(), any(), anyDouble());
            verify(执行器, never()).执行(any());
        }

        @Test
        @DisplayName("瞬发执行异常时应恢复已预留秘能")
        void 瞬发执行异常_恢复秘能() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0);
            doThrow(new IllegalStateException("执行失败")).when(执行器).执行(any());

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.执行异常, 结果);
            var 顺序 = inOrder(资源变更服务, 执行器);
            顺序.verify(资源变更服务).消耗(玩家标识, "秘能", 1.0);
            顺序.verify(执行器).执行(any());
            顺序.verify(资源变更服务).增加(玩家标识, "秘能", 1.0);
        }

        @Test
        @DisplayName("秘能恰好等于消耗时成功且只扣除一次")
        void 秘能等于消耗_成功且只扣除一次() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0, 0.0);
            when(属性计算服务.计算实际公共冷却(anyDouble(), anyDouble())).thenReturn(1.2);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(资源变更服务).消耗(玩家标识, "秘能", 1.0);
            verify(资源变更服务, never()).增加(玩家标识, "秘能", 1.0);
        }

        @Test
        @DisplayName("资源扣除异常时释放失败且不执行技能")
        void 资源扣除异常_失败且不执行技能() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0);
            doThrow(new IllegalStateException("计分板同步失败"))
                    .when(资源变更服务).消耗(玩家标识, "秘能", 1.0);

            技能执行结果 结果 = assertDoesNotThrow(
                    () -> 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义)));

            assertEquals(技能执行结果.失败, 结果);
            verify(执行器, never()).执行(any());
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.失败.其他"), eq("一"));
        }

        @Test
        @DisplayName("null上下文应失败且不访问资源")
        void null上下文_失败且不访问资源() {
            技能执行结果 结果 = assertDoesNotThrow(() -> 服务.释放(null));

            assertEquals(技能执行结果.上下文为空, 结果);
            verify(资源变更服务, never()).获取当前值(any(), any());
            verify(资源变更服务, never()).消耗(any(), any(), anyDouble());
        }

        @Test
        @DisplayName("蓄力技能被打断时应回滚预扣秘能")
        void 蓄力被打断_回滚预扣秘能() {
            技能定义 定义 = new 技能定义(
                    "1_1", "skill.test", 施法类型.蓄力,
                    1.0, 0.0, 0.0, 1.5,
                    "秘能", 1.0, 10.0, 1.0, 1, false, false, false);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0);
            when(属性计算服务.计算实际蓄力时间(eq(1.0), anyDouble())).thenReturn(1.0);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));
            assertEquals(技能执行结果.成功, 结果);

            服务.打断蓄力(玩家标识, 技能释放服务实现.打断原因.手动取消);

            verify(资源变更服务).消耗(玩家标识, "秘能", 1.0);
            verify(资源变更服务).增加(玩家标识, "秘能", 1.0);
            verify(执行器, never()).执行(any());
        }

        @Test
        @DisplayName("同一蓄力技能重复请求不得重复扣除秘能")
        void 同一蓄力技能重复请求_不重复扣除秘能() {
            技能定义 定义 = new 技能定义(
                    "1_1", "skill.test", 施法类型.蓄力,
                    1.0, 0.0, 0.0, 1.5,
                    "秘能", 1.0, 10.0, 1.0, 1, false, false, false);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(2.0);
            when(属性计算服务.计算实际蓄力时间(eq(1.0), anyDouble())).thenReturn(1.0);

            技能上下文 上下文 = 构造上下文(创建属性快照(1.2, 0.0), 定义);
            assertEquals(技能执行结果.成功, 服务.释放(上下文));
            assertEquals(技能执行结果.成功, 服务.释放(上下文));

            verify(资源变更服务, org.mockito.Mockito.times(1))
                    .消耗(玩家标识, "秘能", 1.0);
        }

        @Test
        @DisplayName("蓄力执行异常时应回滚秘能且不开始独立冷却")
        void 蓄力执行异常_回滚秘能且不开始独立冷却() {
            技能定义 定义 = new 技能定义(
                    "1_1", "skill.test", 施法类型.蓄力,
                    1.0, 0.0, 0.0, 1.5,
                    "秘能", 1.0, 10.0, 2.0, 1, false, false, false);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0);
            when(属性计算服务.计算实际蓄力时间(eq(1.0), anyDouble())).thenReturn(1.0);
            doThrow(new IllegalStateException("异步执行失败")).when(执行器).执行(any());

            服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            ArgumentCaptor<Runnable> 任务捕获 = ArgumentCaptor.forClass(Runnable.class);
            verify(资源测试调度器).runTaskLater(eq(插件), 任务捕获.capture(), anyLong());
            任务捕获.getValue().run();

            verify(资源变更服务).增加(玩家标识, "秘能", 1.0);
            verify(技能冷却服务, never()).开始冷却(玩家标识, "1_1", 2.0);
        }
    }

    /**
     * FP-1修改3 验证：释放成功和释放失败消息必须有关键词颜色修饰。
     *
     * 关键词颜色修饰链路：技能释放服务 -> 消息服务.发送技能日志 -> 关键词解析器.解析 -> 日志合并管理器
     * 只要消息走 发送技能日志 路由（而非 发送消息/发送错误），翻译模板中的关键词
     * （[第N技能]/[蓄力]/[秒数:X]/[秘能] 等）会由关键词解析器自动着色，
     * 失败模板外层 <dark_red>...</dark_red> 包裹保持完整。
     */
    @Nested
    @DisplayName("FP-1修改3 释放日志关键词颜色修饰链路")
    class 释放日志关键词修饰链路 {

        @Test
        @DisplayName("资源不足失败应通过发送技能日志路由（确保关键词解析器着色+dark_red包裹）")
        void 资源不足失败_应走发送技能日志路由() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(0.9);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.资源不足, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.失败.资源不足"), eq("一"));
            verify(消息服务, never()).发送消息(any(玩家快照.class), eq("技能日志.失败.资源不足"), any());
            verify(消息服务, never()).发送错误(any(玩家快照.class), eq("技能日志.失败.资源不足"), any());
        }

        @Test
        @DisplayName("瞬发执行异常失败应通过发送技能日志路由（确保关键词解析器着色+dark_red包裹）")
        void 瞬发执行异常失败_应走发送技能日志路由() {
            技能定义 定义 = 构造秘能消耗技能定义(1.0);
            准备释放环境(定义);
            when(资源变更服务.获取当前值(玩家标识, "秘能")).thenReturn(1.0);
            doThrow(new IllegalStateException("执行失败")).when(执行器).执行(any());

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 0.0), 定义));

            assertEquals(技能执行结果.执行异常, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.失败.执行异常"), eq("一"));
            verify(消息服务, never()).发送消息(any(玩家快照.class), eq("技能日志.失败.执行异常"), any());
            verify(消息服务, never()).发送错误(any(玩家快照.class), eq("技能日志.失败.执行异常"), any());
        }

        @Test
        @DisplayName("瞬发释放成功应通过发送技能日志路由（确保[第N技能]关键词被着色）")
        void 瞬发释放成功_应走发送技能日志路由() {
            技能定义 定义 = 构造技能定义(1.5, 施法类型.瞬发);
            准备释放环境(定义);
            when(属性计算服务.计算实际公共冷却(anyDouble(), anyDouble())).thenReturn(1.142857);

            技能执行结果 结果 = 服务.释放(构造上下文(创建属性快照(1.2, 5.0), 定义));

            assertEquals(技能执行结果.成功, 结果);
            verify(消息服务).发送技能日志(any(玩家快照.class), eq("技能日志.瞬发释放"), eq("一"));
            verify(消息服务, never()).发送消息(any(玩家快照.class), eq("技能日志.瞬发释放"), any());
            verify(消息服务, never()).发送战斗日志(any(玩家快照.class), eq(20), eq("技能日志.瞬发释放"), any());
        }
    }
}
