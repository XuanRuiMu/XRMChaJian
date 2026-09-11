package mljy.全流程文本输出测试;

import mljy.玩家服务;
import mljy.业务层.属性计算服务;
import mljy.表现层.监听器.反伤监听器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.属性.属性元数据;
import mljy.领域层.属性.属性注册表;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-TEXT-07 反伤效果全流程文本输出测试。
 *
 * 验证玩家被攻击时反伤监听器将部分伤害反弹给攻击者的完整流程。
 * 使用 ArgumentCaptor 捕获 攻击者.damage(反弹伤害, 受击者) 的实际参数，
 * 验证反弹伤害数值符合公式：反弹伤害 = 原始伤害 × 反伤值 × 0.01（反伤比例系数）。
 *
 * 反伤监听器设计要点：
 * - 反伤作为扩展属性存储在 属性快照.扩展属性 Map 中
 * - 使用 ThreadLocal 标记防止递归
 * - 构造时自注册属性元数据
 */
@DisplayName("FP-TEXT-07 反伤效果全流程文本输出测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 反伤效果全流程测试 {

    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 属性计算服务 属性计算服务;
    @Mock
    private 属性注册表 属性注册表;
    @Mock
    private Player 受击者;
    @Mock
    private LivingEntity 攻击者;
    @Mock
    private EntityDamageByEntityEvent 事件;

    private 反伤监听器 监听器;

    private UUID 受击者标识;
    private UUID 攻击者标识;

    @BeforeEach
    void setUp() {
        监听器 = new 反伤监听器(玩家服务, 属性计算服务, 属性注册表);

        受击者标识 = UUID.randomUUID();
        攻击者标识 = UUID.randomUUID();
        when(受击者.getUniqueId()).thenReturn(受击者标识);
        when(攻击者.getUniqueId()).thenReturn(攻击者标识);
    }

    private 玩家快照 创建快照(double 反伤值) {
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0,
                50.0, 40.0, 30.0,
                50.0, 40.0, 30.0,
                15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        属性 = 属性.带扩展属性("反伤", 反伤值);
        return new 玩家快照(受击者标识, "受击者", 1, null, 属性, Collections.emptyMap(), 战斗状态.非战斗);
    }

    @Nested
    @DisplayName("反伤触发全流程：受击 → 反伤判定 → 攻击者受伤")
    class 反伤触发全流程 {

        @Test
        @DisplayName("反伤10点+原始伤害100：捕获damage调用应反弹10.0伤害（100×10×0.01）")
        void 反伤10点_原始伤害100_应反弹10点伤害() {
            玩家快照 快照 = 创建快照(10.0);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算(快照)).thenReturn(快照.属性());
            when(事件.getFinalDamage()).thenReturn(100.0);

            监听器.实体被攻击(事件);

            ArgumentCaptor<Double> 伤害捕获器 = ArgumentCaptor.forClass(Double.class);
            verify(攻击者).damage(伤害捕获器.capture(), eq(受击者));
            double 实际反弹 = 伤害捕获器.getValue();
            assertEquals(10.0, 实际反弹, 0.001,
                    "反伤10点+原始100伤害应反弹10.0伤害（100×10×0.01），实际：" + 实际反弹);
        }

        @Test
        @DisplayName("反伤50点+原始伤害200：捕获damage调用应反弹100伤害（200×50×0.01）")
        void 反伤50点_原始伤害200_应反弹100伤害() {
            玩家快照 快照 = 创建快照(50.0);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算(快照)).thenReturn(快照.属性());
            when(事件.getFinalDamage()).thenReturn(200.0);

            监听器.实体被攻击(事件);

            ArgumentCaptor<Double> 伤害捕获器 = ArgumentCaptor.forClass(Double.class);
            verify(攻击者).damage(伤害捕获器.capture(), eq(受击者));
            double 实际反弹 = 伤害捕获器.getValue();
            assertEquals(100.0, 实际反弹, 0.001,
                    "反伤50点+原始200伤害应反弹100.0伤害（200×50×0.01），实际：" + 实际反弹);
        }

        @Test
        @DisplayName("反伤触发：捕获damage调用应传入受击者作为伤害来源")
        void 反伤触发_应传入受击者作为伤害来源() {
            玩家快照 快照 = 创建快照(10.0);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算(快照)).thenReturn(快照.属性());
            when(事件.getFinalDamage()).thenReturn(100.0);

            监听器.实体被攻击(事件);

            verify(攻击者).damage(anyDouble(), eq(受击者));
        }
    }

    @Nested
    @DisplayName("边界值与异常场景：0反伤、0伤害、非玩家受击者、非生物攻击者")
    class 边界值与异常 {

        @Test
        @DisplayName("反伤0点：不应触发damage调用")
        void 反伤0点_不应触发damage调用() {
            玩家快照 快照 = 创建快照(0.0);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算(快照)).thenReturn(快照.属性());
            when(事件.getFinalDamage()).thenReturn(100.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("原始伤害0：不应触发damage调用")
        void 原始伤害0_不应触发damage调用() {
            玩家快照 快照 = 创建快照(10.0);
            when(事件.getEntity()).thenReturn(受击者);
            when(事件.getDamager()).thenReturn(攻击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.of(快照));
            when(属性计算服务.计算(快照)).thenReturn(快照.属性());
            when(事件.getFinalDamage()).thenReturn(0.0);

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }

        @Test
        @DisplayName("会话不存在：不应触发damage调用")
        void 会话不存在_不应触发damage调用() {
            when(事件.getEntity()).thenReturn(受击者);
            when(玩家服务.获取快照(受击者标识)).thenReturn(Optional.empty());

            监听器.实体被攻击(事件);

            verify(攻击者, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Nested
    @DisplayName("属性注册：构造时自注册反伤属性元数据")
    class 属性注册全流程 {

        @Test
        @DisplayName("构造监听器：应向属性注册表注册反伤属性元数据")
        void 构造监听器_应注册反伤属性元数据() {
            ArgumentCaptor<属性元数据> 捕获器 = ArgumentCaptor.forClass(属性元数据.class);
            verify(属性注册表).注册(捕获器.capture());

            属性元数据 元数据 = 捕获器.getValue();
            assertEquals("反伤", 元数据.命令名(),
                    "属性元数据命令名应为'反伤'，实际：" + 元数据.命令名());
            assertTrue(元数据.显示翻译键().contains("反伤"),
                    "显示翻译键应包含'反伤'，实际：" + 元数据.显示翻译键());
        }
    }
}
