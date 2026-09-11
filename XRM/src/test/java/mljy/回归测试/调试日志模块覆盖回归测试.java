package mljy.回归测试;

import mljy.业务层.属性计算服务实现;
import mljy.业务层.技能冷却服务;
import mljy.业务层.技能释放服务实现;
import mljy.业务层.技能注册服务;
import mljy.业务层.消息服务;
import mljy.业务层.资源变更服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.基础设施层.调试日志器;
import mljy.技能执行器;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.技能.施法类型;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.属性.属性快照;
import mljy.领域层.资源.资源;
import mljy.领域层.战斗.战斗状态;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：FP-DBG 相关模块按 debug 开关输出调试日志。
 *
 * FP-07 已将调试日志器改为文件日志架构。
 * 调试输出通过 java.util.logging.FileHandler 写入文件，
 * 不再经过注入的 java.util.logging.Logger。
 * 本测试验证服务方法安全执行且在开关关闭时不产生副作用。
 */
@DisplayName("回归测试 - FP-DBG：关键模块调试日志覆盖")
class 调试日志模块覆盖回归测试 {

    private Logger 日志器;

    @BeforeEach
    void 重置() {
        日志器 = mock(Logger.class);
        调试日志器.初始化(日志器, true);
    }

    @AfterEach
    void 清理() {
        调试日志器.初始化(null, false);
    }

    @Test
    @DisplayName("技能释放服务：瞬发技能释放应安全执行")
    void 技能释放服务_瞬发释放_应安全执行() {
        技能释放服务实现 服务 = 创建技能释放服务();
        技能上下文 上下文 = 创建瞬发技能上下文();

        assertDoesNotThrow(() -> 服务.释放(上下文));
    }

    @Test
    @DisplayName("技能释放服务：调试开关关闭时不应产生副作用")
    void 技能释放服务_开关关闭_不应产生副作用() {
        调试日志器.初始化(日志器, false);
        clearInvocations(日志器);

        技能释放服务实现 服务 = 创建技能释放服务();
        技能上下文 上下文 = 创建瞬发技能上下文();

        服务.释放(上下文);

        verify(日志器, never()).info(anyString());
    }

    @Test
    @DisplayName("属性计算服务：计算修饰后属性应安全执行")
    void 属性计算服务_计算修饰后属性_应安全执行() {
        属性计算服务实现 服务 = 创建属性计算服务();
        UUID 玩家标识 = UUID.randomUUID();
        属性快照 基础属性 = 属性快照.创建(
                100.0, 1.5, 10.0, 5.0, 5.0, 20.0,
                5.0, 5.0, 20.0, 15.0, 50.0, 10.0,
                5.0, 3.0, 8.0, 2.0, 1.0
        );

        assertDoesNotThrow(() -> 服务.计算修饰后属性(玩家标识, 基础属性));
    }

    @Test
    @DisplayName("属性计算服务：调试开关关闭时不应产生副作用")
    void 属性计算服务_开关关闭_不应产生副作用() {
        调试日志器.初始化(日志器, false);
        clearInvocations(日志器);

        属性计算服务实现 服务 = 创建属性计算服务();
        UUID 玩家标识 = UUID.randomUUID();
        属性快照 基础属性 = 属性快照.创建(
                100.0, 1.5, 10.0, 5.0, 5.0, 20.0,
                5.0, 5.0, 20.0, 15.0, 50.0, 10.0,
                5.0, 3.0, 8.0, 2.0, 1.0
        );

        服务.计算修饰后属性(玩家标识, 基础属性);

        verify(日志器, never()).info(anyString());
    }

    private 技能释放服务实现 创建技能释放服务() {
        技能注册服务 注册服务 = mock(技能注册服务.class);
        技能冷却服务 冷却服务 = mock(技能冷却服务.class);
        资源变更服务 资源服务 = mock(资源变更服务.class);
        mljy.业务层.属性计算服务 属性服务 = mock(mljy.业务层.属性计算服务.class);
        消息服务 消息服务 = mock(消息服务.class);
        翻译服务 翻译服务 = mock(翻译服务.class);
        JavaPlugin 插件 = mock(JavaPlugin.class);

        技能定义 定义 = new 技能定义(
                "1_1", "skill.test", 施法类型.瞬发,
                0.0, 0.0, 5.0, 1.5,
                "法力", 10.0, 20.0, 1.0, 1, true, false, false
        );
        when(注册服务.获取定义("1_1")).thenReturn(Optional.of(定义));
        when(注册服务.获取执行器("1_1")).thenReturn(Optional.of(mock(技能执行器.class)));
        when(冷却服务.是否冷却中(any(UUID.class), eq("1_1"))).thenReturn(false);
        when(冷却服务.是否公共冷却中(any(UUID.class))).thenReturn(false);
        when(资源服务.获取当前值(any(UUID.class), eq("法力"))).thenReturn(100.0);
        when(属性服务.计算实际公共冷却(anyDouble(), anyDouble())).thenReturn(1.5);
        when(插件.getServer()).thenReturn(mock(Server.class));

        return new 技能释放服务实现(
                注册服务, 冷却服务, 资源服务, 属性服务,
                消息服务, 翻译服务, mock(mljy.业务层.公共冷却显示服务.class),
                mock(mljy.玩家服务.class), 插件
        );
    }

    private 技能上下文 创建瞬发技能上下文() {
        UUID 玩家标识 = UUID.randomUUID();
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0, 5.0, 5.0, 20.0,
                5.0, 5.0, 20.0, 15.0, 50.0, 10.0,
                5.0, 3.0, 8.0, 2.0, 1.0
        );
        玩家快照 玩家 = new 玩家快照(
                玩家标识, "测试玩家", 1,
                new 位置("world", 0.0, 0.0, 0.0, 0.0f, 0.0f),
                属性, Map.of("法力", new 资源("法力", 100.0, 100.0)),
                战斗状态.非战斗
        );
        技能定义 定义 = new 技能定义(
                "1_1", "skill.test", 施法类型.瞬发,
                0.0, 0.0, 5.0, 1.5,
                "法力", 10.0, 20.0, 1.0, 1, true, false, false
        );
        return new 技能上下文(玩家, "1_1", 定义, null, System.currentTimeMillis());
    }

    private 属性计算服务实现 创建属性计算服务() {
        修饰器管理器 修饰器 = mock(修饰器管理器.class);
        when(修饰器.计算最终值(any(UUID.class), anyString(), anyDouble())).thenAnswer(调用 -> 调用.getArgument(2));
        return new 属性计算服务实现(修饰器);
    }
}
