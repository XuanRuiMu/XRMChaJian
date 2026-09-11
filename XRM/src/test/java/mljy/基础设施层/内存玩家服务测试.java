package mljy.基础设施层;

import mljy.属性服务;
import mljy.数据服务;
import mljy.领域层.属性.属性快照;
import mljy.领域层.玩家.玩家快照;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-05 修复验证：内存玩家服务获取快照实时性。
 *
 * 根因：原实现 获取快照 直接返回登录时缓存的快照，其属性是基础属性（无修饰器），
 * 导致 bossbar（通过玩家服务.获取快照读属性）与 /sx（通过属性服务.计算属性读最终属性）显示不一致。
 *
 * 修复：获取快照 每次返回时用 属性服务.计算属性 替换快照属性字段，
 * 符合需求.md 第410行"通过玩家服务获取玩家快照"+ 第414行"实时查询，不缓存"。
 */
@DisplayName("FP-05 内存玩家服务获取快照实时性")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 内存玩家服务测试 {

    @Mock
    private 数据服务 数据服务;
    @Mock
    private 属性服务 属性服务;
    @Mock
    private JavaPlugin 插件;
    @Mock
    private FileConfiguration 配置;
    @Mock
    private Player 玩家实体;

    private 内存玩家服务 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        when(插件.getConfig()).thenReturn(配置);
        when(配置.getString(anyString(), anyString())).thenAnswer(调用 -> 调用.getArgument(1));
        when(配置.getBoolean(anyString(), anyBoolean())).thenAnswer(调用 -> 调用.getArgument(1));
        服务 = new 内存玩家服务(数据服务, 属性服务, 插件);
        玩家标识 = UUID.randomUUID();
    }

    private 属性快照 创建属性快照(double 公共冷却时间, double 急速) {
        return 属性快照.创建(
                100.0, 公共冷却时间, 急速,
                0.0, 0.0, 10.0,
                0.0, 0.0, 10.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private void 准备登录环境(属性快照 专精基础属性, 属性快照 计算属性返回) {
        玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
        会话.设置职业("魔法世界");
        会话.设置专精("奥能法师");
        when(数据服务.加载(玩家标识)).thenReturn(Optional.of(会话));
        when(属性服务.获取专精基础属性("奥能法师")).thenReturn(Optional.of(专精基础属性));
        when(属性服务.计算属性(玩家标识)).thenReturn(计算属性返回);
        when(玩家实体.isOnline()).thenReturn(true);
    }

    @Nested
    @DisplayName("获取快照实时性（FP-05核心修复）")
    class 获取快照实时性 {

        @Test
        @DisplayName("获取快照应返回属性服务计算属性的结果而非登录时缓存的基础属性")
        void 获取快照应返回最终属性而非基础属性() {
            属性快照 基础属性 = 创建属性快照(1.2, 5.0);
            属性快照 最终属性 = 创建属性快照(1.2, 15.0);
            准备登录环境(基础属性, 最终属性);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家实体);
                服务.玩家登录(玩家标识, "测试玩家");

                Optional<玩家快照> 快照可选 = 服务.获取快照(玩家标识);

                assertTrue(快照可选.isPresent());
                属性快照 属性 = 快照可选.get().属性();
                assertEquals(15.0, 属性.急速(), 0.0001, "应返回最终属性急速15.0，而非基础属性急速5.0");
                assertEquals(1.2, 属性.公共冷却时间(), 0.0001);
                verify(属性服务, atLeastOnce()).计算属性(玩家标识);
            }
        }

        @Test
        @DisplayName("修饰器变化后再次获取快照应反映新的最终属性（实时刷新不缓存）")
        void 修饰器变化后获取快照应反映新属性() {
            属性快照 基础属性 = 创建属性快照(1.2, 5.0);
            属性快照 第一次最终属性 = 创建属性快照(1.2, 15.0);
            属性快照 第二次最终属性 = 创建属性快照(1.2, 25.0);
            准备登录环境(基础属性, 第一次最终属性);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家实体);
                服务.玩家登录(玩家标识, "测试玩家");

                Optional<玩家快照> 第一次 = 服务.获取快照(玩家标识);
                assertTrue(第一次.isPresent());
                assertEquals(15.0, 第一次.get().属性().急速(), 0.0001);

                when(属性服务.计算属性(玩家标识)).thenReturn(第二次最终属性);
                Optional<玩家快照> 第二次 = 服务.获取快照(玩家标识);
                assertTrue(第二次.isPresent());
                assertEquals(25.0, 第二次.get().属性().急速(), 0.0001, "修饰器变化后应反映新急速25.0");
            }
        }

        @Test
        @DisplayName("玩家未登录获取快照应返回空")
        void 未登录获取快照应返回空() {
            Optional<玩家快照> 快照可选 = 服务.获取快照(玩家标识);
            assertTrue(快照可选.isEmpty());
            verify(属性服务, never()).计算属性(any());
        }
    }

    @Nested
    @DisplayName("三处数据源一致性（FP-05核心目标）")
    class 三处数据源一致性 {

        @Test
        @DisplayName("bossbar与技能执行读取的公共冷却时间应来自同一最终属性源")
        void bossbar与技能执行应读同一最终属性() {
            属性快照 基础属性 = 创建属性快照(1.2, 5.0);
            属性快照 最终属性 = 创建属性快照(1.2, 15.0);
            准备登录环境(基础属性, 最终属性);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(玩家标识)).thenReturn(玩家实体);
                服务.玩家登录(玩家标识, "测试玩家");

                Optional<玩家快照> 快照可选 = 服务.获取快照(玩家标识);
                assertTrue(快照可选.isPresent());
                属性快照 快照属性 = 快照可选.get().属性();

                属性快照 计算属性 = 属性服务.计算属性(玩家标识);

                assertEquals(计算属性.公共冷却时间(), 快照属性.公共冷却时间(), 0.0001,
                        "bossbar（通过快照）与/sx（通过计算属性）公共冷却时间必须一致");
                assertEquals(计算属性.急速(), 快照属性.急速(), 0.0001,
                        "急速必须一致，确保公CD计算结果一致");
            }
        }
    }
}
