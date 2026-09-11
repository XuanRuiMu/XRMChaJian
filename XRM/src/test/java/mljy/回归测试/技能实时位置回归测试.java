package mljy.回归测试;

import mljy.业务层.伤害计算服务;
import mljy.业务层.效果注册服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.消息服务;
import mljy.业务层.效果调度服务;
import mljy.业务层.资源变更服务;
import mljy.战斗服务;
import mljy.技能实现.奥能法师.奥能法师技能基础;
import mljy.领域层.玩家.位置;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.战斗.战斗状态;
import mljy.领域层.属性.属性快照;
import mljy.基础设施层.Bukkit适配.位置适配器;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 回归测试：Bug 2 修复 - 技能特效缺失。
 *
 * Bug 2 根因：9个技能文件的 执行() 方法使用 玩家快照.位置()（旧位置）取起点，
 * 而非 Bukkit.getPlayer().getLocation()（实时位置），导致技能特效在错误位置生成。
 *
 * 修复：奥能法师技能基础 添加 protected 获取实时位置(玩家快照) helper，
 * 9个技能文件改用 获取实时位置(施法者) 取实时位置。
 *
 * 本测试验证 获取实时位置 方法调用 Bukkit.getPlayer 获取实时位置，
 * 而非使用快照中的位置。
 */
@DisplayName("回归测试 - Bug 2：技能特效缺失（获取实时位置调用 Bukkit.getPlayer）")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 技能实时位置回归测试 {

    @Mock
    private JavaPlugin 插件;
    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 伤害计算服务 伤害计算服务;
    @Mock
    private 目标选择服务 目标选择服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 资源变更服务 资源变更服务;
    @Mock
    private 效果注册服务 效果注册服务;

    private 测试技能基础 技能基础;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws Exception {
        技能基础 = new 测试技能基础();
        玩家标识 = UUID.randomUUID();

        // 注入父类 @Inject 字段
        注入字段(技能基础, "插件", 插件);
        注入字段(技能基础, "战斗服务", 战斗服务);
        注入字段(技能基础, "伤害计算服务", 伤害计算服务);
        注入字段(技能基础, "目标选择服务", 目标选择服务);
        注入字段(技能基础, "消息服务", 消息服务);
        注入字段(技能基础, "效果调度服务", 效果调度服务);
        注入字段(技能基础, "资源变更服务", 资源变更服务);
        注入字段(技能基础, "效果注册服务", 效果注册服务);
    }

    private void 注入字段(Object 实例, String 字段名, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(字段名);
        字段.setAccessible(true);
        字段.set(实例, 值);
    }

    private 位置 调用获取实时位置(玩家快照 施法者) throws Exception {
        Method 方法 = 奥能法师技能基础.class.getDeclaredMethod("获取实时位置", 玩家快照.class);
        方法.setAccessible(true);
        return (位置) 方法.invoke(技能基础, 施法者);
    }

    @Test
    @DisplayName("获取实时位置：应调用 Bukkit.getPlayer 获取在线玩家位置")
    void 获取实时位置_应调用BukkitGetPlayer() throws Exception {
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0, 50.0, 40.0, 30.0,
                50.0, 40.0, 30.0, 15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        // 快照中的旧位置
        位置 旧位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        玩家快照 快照 = new 玩家快照(玩家标识, "测试法师", 1, 旧位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);

        // 实时位置（不同于旧位置），需 mock World 避免 位置适配器.转换 返回 0,0,0
        World 世界 = mock(World.class);
        when(世界.getName()).thenReturn("world");
        Location 实时位置 = new Location(世界, 100, 70, 200, 90f, 45f);
        Player 在线玩家 = mock(Player.class);
        when(在线玩家.getLocation()).thenReturn(实时位置);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(在线玩家);

            位置 结果 = 调用获取实时位置(快照);

            assertNotNull(结果, "在线玩家应返回非null位置");
            // 验证返回的是实时位置，而非快照中的旧位置
            assertEquals(100.0, 结果.x(), 0.001, "应返回实时位置x=100，实际：" + 结果.x());
            assertEquals(70.0, 结果.y(), 0.001, "应返回实时位置y=70，实际：" + 结果.y());
            assertEquals(200.0, 结果.z(), 0.001, "应返回实时位置z=200，实际：" + 结果.z());
        }
    }

    @Test
    @DisplayName("获取实时位置：玩家不在线时应返回null")
    void 获取实时位置_玩家不在线时应返回Null() throws Exception {
        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 10.0, 50.0, 40.0, 30.0,
                50.0, 40.0, 30.0, 15.0, 100.0, 5.0,
                8.0, 12.0, 2.0, 1.0, 1.0);
        位置 旧位置 = new 位置("world", 0, 64, 0, 0f, 0f);
        玩家快照 快照 = new 玩家快照(玩家标识, "测试法师", 1, 旧位置, 属性, Collections.emptyMap(), 战斗状态.非战斗);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            bukkitMock.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(null);

            位置 结果 = 调用获取实时位置(快照);

            assertNull(结果, "玩家不在线时应返回null");
        }
    }

    /**
     * 测试用子类，用于访问 protected 方法。
     */
    private static class 测试技能基础 extends 奥能法师技能基础 {
    }
}
