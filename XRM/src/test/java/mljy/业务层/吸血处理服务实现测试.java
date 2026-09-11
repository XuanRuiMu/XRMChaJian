package mljy.业务层;

import mljy.业务层.消息.战斗日志上下文管理器;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("吸血处理服务实现")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 吸血处理服务实现测试 {

    @Mock
    private 属性计算服务 属性计算服务mock;
    @Mock
    private 战斗日志上下文管理器 战斗日志上下文管理器mock;

    private 吸血处理服务实现 服务;
    private UUID 玩家标识;
    private 玩家快照 快照;
    private 属性快照 属性带吸血;

    @BeforeEach
    void setUp() {
        玩家标识 = UUID.randomUUID();
        快照 = new 玩家快照(玩家标识, "测试玩家", 1, null, null, Map.of(), null);
        // 吸血=10（第14个参数），智力=10（第6个），用于派生吸血比例
        属性带吸血 = 属性快照.创建(20.0, 1.0, 1.0, 1.0, 1.0, 10.0, 1.0, 1.0, 10.0,
                1.0, 1.0, 1.0, 1.0, 10.0, 1.0, 1.0, 1.0);
        服务 = new 吸血处理服务实现(属性计算服务mock, 战斗日志上下文管理器mock);
        when(属性计算服务mock.计算(快照)).thenReturn(属性带吸血);
        when(属性计算服务mock.计算派生值(eq("吸血比例"), any())).thenReturn(0.5);
    }

    private Player 创建玩家(double 当前生命, double 最大生命) {
        Player 玩家 = mock(Player.class);
        when(玩家.isOnline()).thenReturn(true);
        when(玩家.isDead()).thenReturn(false);
        when(玩家.getHealth()).thenReturn(当前生命);
        AttributeInstance 最大 = mock(AttributeInstance.class);
        when(最大.getValue()).thenReturn(最大生命);
        when(玩家.getAttribute(Attribute.MAX_HEALTH)).thenReturn(最大);
        return 玩家;
    }

    @Nested
    @DisplayName("正常流程 - 伤害吸血")
    class 伤害吸血正常 {

        @Test
        @DisplayName("吸血属性>0 - 应治疗玩家并累加吸血量（数值×吸血比例）")
        void 处理伤害吸血_正常_应累加() {
            Player 玩家 = 创建玩家(10.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                // 吸血量 = 保留一位小数(100.0 × 0.5) = 50.0
                verify(玩家).setHealth(20.0);
                verify(战斗日志上下文管理器mock).累加吸血量(eq(玩家标识), any(), eq(50.0));
            }
        }

        @Test
        @DisplayName("不同数值 - 吸血量应等于 数值×吸血比例 保留一位小数")
        void 处理伤害吸血_不同数值_应正确计算() {
            Player 玩家 = 创建玩家(10.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 30.0);

                // 吸血量 = 保留一位小数(30.0 × 0.5) = 15.0
                verify(玩家).setHealth(20.0);
                verify(战斗日志上下文管理器mock).累加吸血量(eq(玩家标识), any(), eq(15.0));
            }
        }
    }

    @Nested
    @DisplayName("正常流程 - 治疗吸血")
    class 治疗吸血正常 {

        @Test
        @DisplayName("吸血属性>0 - 治疗也应触发吸血并累加")
        void 处理治疗吸血_正常_应累加() {
            Player 玩家 = 创建玩家(10.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理治疗吸血(快照, 100.0);

                verify(玩家).setHealth(20.0);
                verify(战斗日志上下文管理器mock).累加吸血量(eq(玩家标识), any(), eq(50.0));
            }
        }
    }

    @Nested
    @DisplayName("边界与异常 - 不应触发吸血")
    class 边界与异常 {

        @Test
        @DisplayName("施法者为null - 不应治疗也不应累加")
        void 施法者null_不应累加() {
            服务.处理伤害吸血(null, 100.0);

            verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
        }

        @Test
        @DisplayName("数值<=0 - 不应累加")
        void 数值非正_不应累加() {
            服务.处理伤害吸血(快照, 0.0);

            verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
        }

        @Test
        @DisplayName("吸血属性<=0 - 不应累加")
        void 吸血属性为0_不应累加() {
            属性快照 属性0 = 属性快照.创建(20.0, 1.0, 1.0, 1.0, 1.0, 10.0, 1.0, 1.0, 10.0,
                    1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0);
            when(属性计算服务mock.计算(快照)).thenReturn(属性0);
            Player 玩家 = 创建玩家(10.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }

        @Test
        @DisplayName("吸血比例<=0 - 不应累加")
        void 吸血比例为0_不应累加() {
            when(属性计算服务mock.计算派生值(eq("吸血比例"), any())).thenReturn(0.0);
            Player 玩家 = 创建玩家(10.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }

        @Test
        @DisplayName("玩家满血 - 提前return，不应累加")
        void 玩家满血_不应累加() {
            Player 玩家 = 创建玩家(20.0, 20.0);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }

        @Test
        @DisplayName("玩家离线 - 不应累加")
        void 玩家离线_不应累加() {
            Player 玩家 = 创建玩家(10.0, 20.0);
            when(玩家.isOnline()).thenReturn(false);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }

        @Test
        @DisplayName("玩家死亡 - 不应累加")
        void 玩家死亡_不应累加() {
            Player 玩家 = 创建玩家(10.0, 20.0);
            when(玩家.isDead()).thenReturn(true);
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }

        @Test
        @DisplayName("玩家不存在(Bukkit.getPlayer返回null) - 不应累加")
        void 玩家不存在_不应累加() {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

                服务.处理伤害吸血(快照, 100.0);

                verify(战斗日志上下文管理器mock, never()).累加吸血量(any(), any(), anyDouble());
            }
        }
    }
}
