package mljy.基础设施层;

import mljy.业务层.伤害计算服务;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("内存战斗服务")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 内存战斗服务测试 {

    private static final String 插件名 = "XRM";
    private static final double 伤害数值 = 100.0;

    private 伤害计算服务 伤害计算服务;
    private Plugin 插件;
    private LivingEntity 目标生物;
    private PersistentDataContainer 持久数据容器;
    private Player 攻击者;

    private 内存战斗服务 战斗服务实例;
    private UUID 攻击者标识;

    @BeforeEach
    void setUp() {
        伤害计算服务 = mock(伤害计算服务.class);
        插件 = mock(Plugin.class);
        目标生物 = mock(LivingEntity.class);
        持久数据容器 = mock(PersistentDataContainer.class);
        攻击者 = mock(Player.class);
        when(插件.namespace()).thenReturn(插件名.toLowerCase(Locale.ROOT));
        战斗服务实例 = new 内存战斗服务(伤害计算服务, 插件);
        攻击者标识 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("双参数 damage 调用 + PDC 标记写入")
    class 双参数Damage调用 {

        @Test
        @DisplayName("应用伤害 - 应使用双参数 damage(数值, 攻击者) 触发 ByEntity 事件")
        void 应用伤害_应使用双参数Damage触发ByEntity事件() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            when(目标生物.getPersistentDataContainer()).thenReturn(持久数据容器);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 伤害数值, true, false, 1.0, List.of());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getEntity(攻击者标识)).thenReturn(攻击者);

                战斗服务实例.应用伤害(上下文);

                verify(目标生物).damage(伤害数值, 攻击者);
            }
        }

        @Test
        @DisplayName("应用伤害 - 应向目标 PDC 写入 XRM 技能伤害标记")
        void 应用伤害_应写入PDC标记() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            when(目标生物.getPersistentDataContainer()).thenReturn(持久数据容器);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 伤害数值, true, false, 1.0, List.of());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getEntity(攻击者标识)).thenReturn(攻击者);

                战斗服务实例.应用伤害(上下文);

                ArgumentCaptor<NamespacedKey> 键捕获 = ArgumentCaptor.forClass(NamespacedKey.class);
                @SuppressWarnings("unchecked")
                ArgumentCaptor<PersistentDataType<?, Double>> 类型捕获 = ArgumentCaptor.forClass(PersistentDataType.class);
                ArgumentCaptor<Double> 值捕获 = ArgumentCaptor.forClass(Double.class);
                verify(持久数据容器).set(键捕获.capture(), 类型捕获.capture(), 值捕获.capture());

                assertEquals(插件名.toLowerCase(), 键捕获.getValue().getNamespace());
                assertEquals(内存战斗服务.技能伤害标记键, 键捕获.getValue().getKey());
                assertEquals(伤害数值, 值捕获.getValue());
            }
        }

        @Test
        @DisplayName("应用伤害 - 应在 damage 调用前写入 PDC 标记")
        @SuppressWarnings("unchecked")
        void 应用伤害_应在Damage前写入PDC标记() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            when(目标生物.getPersistentDataContainer()).thenReturn(持久数据容器);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 伤害数值, true, false, 1.0, List.of());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getEntity(攻击者标识)).thenReturn(攻击者);

                战斗服务实例.应用伤害(上下文);

                InOrder 顺序 = inOrder(持久数据容器, 目标生物);
                顺序.verify(持久数据容器).set(any(NamespacedKey.class), any(PersistentDataType.class), eq(伤害数值));
                顺序.verify(目标生物).damage(伤害数值, 攻击者);
            }
        }

        @Test
        @DisplayName("应用伤害 - 应在 damage 调用后清理 PDC 标记")
        @SuppressWarnings("unchecked")
        void 应用伤害_应在Damage后清理PDC标记() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            when(目标生物.getPersistentDataContainer()).thenReturn(持久数据容器);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 伤害数值, true, false, 1.0, List.of());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getEntity(攻击者标识)).thenReturn(攻击者);

                战斗服务实例.应用伤害(上下文);

                ArgumentCaptor<NamespacedKey> 键捕获 = ArgumentCaptor.forClass(NamespacedKey.class);
                verify(持久数据容器).remove(键捕获.capture());
                assertEquals(插件名.toLowerCase(), 键捕获.getValue().getNamespace());
                assertEquals(内存战斗服务.技能伤害标记键, 键捕获.getValue().getKey());

                InOrder 顺序 = inOrder(持久数据容器, 目标生物);
                顺序.verify(持久数据容器).set(any(NamespacedKey.class), any(PersistentDataType.class), eq(伤害数值));
                顺序.verify(目标生物).damage(伤害数值, 攻击者);
                顺序.verify(持久数据容器).remove(any(NamespacedKey.class));
            }
        }
    }

    @Nested
    @DisplayName("边界值 - 攻击者不在线")
    class 攻击者不在线 {

        @Test
        @DisplayName("攻击者实体不存在 - 不应应用伤害且不写入 PDC 标记")
        void 攻击者实体不存在_不应应用伤害() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 伤害数值, true, false, 1.0, List.of());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(() -> Bukkit.getEntity(攻击者标识)).thenReturn(null);

                战斗服务实例.应用伤害(上下文);

                verify(目标生物, never()).damage(anyDouble(), any(Entity.class));
                verify(目标生物, never()).getPersistentDataContainer();
            }
        }

        @Test
        @DisplayName("施法者为 null - 不应应用伤害")
        void 施法者为Null_不应应用伤害() {
            伤害结果 结果 = new 伤害结果(伤害数值, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            伤害上下文 上下文 = new 伤害上下文(null, 适配实体, 伤害数值, true, false, 1.0, List.of());

            战斗服务实例.应用伤害(上下文);

            verify(目标生物, never()).damage(anyDouble(), any(Entity.class));
            verify(目标生物, never()).getPersistentDataContainer();
        }
    }

    @Nested
    @DisplayName("边界值 - 伤害数值")
    class 伤害数值边界 {

        @Test
        @DisplayName("最终数值为零 - 不应应用伤害")
        void 最终数值为零_不应应用伤害() {
            伤害结果 结果 = new 伤害结果(0.0, false, false);
            when(伤害计算服务.计算(any())).thenReturn(结果);
            实体适配器 适配实体 = new 实体适配器(目标生物);
            玩家快照 施法者 = 创建玩家快照(攻击者标识);
            伤害上下文 上下文 = new 伤害上下文(施法者, 适配实体, 0.0, true, false, 1.0, List.of());

            战斗服务实例.应用伤害(上下文);

            verify(目标生物, never()).damage(anyDouble(), any(Entity.class));
            verify(目标生物, never()).getPersistentDataContainer();
        }
    }

    private 玩家快照 创建玩家快照(UUID 标识) {
        return new 玩家快照(标识, "测试玩家", 1, null, null, Map.of(), null);
    }
}
