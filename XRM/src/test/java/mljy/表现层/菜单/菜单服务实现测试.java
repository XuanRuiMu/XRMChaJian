package mljy.表现层.菜单;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FP-X3 菜单服务实现 - 注册菜单() 修复验证")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 菜单服务实现测试 {

    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 关键词解析器 关键词解析器;
    @Mock
    private 总菜单 总菜单实例;
    @Mock
    private 技能菜单 技能菜单实例;
    @Mock
    private 任务菜单 任务菜单实例;
    @Mock
    private 天赋菜单 天赋菜单实例;
    @Mock
    private 静态菜单 静态菜单实例;
    @Mock
    private Player 玩家;

    private 菜单服务实现 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        when(总菜单实例.获取菜单标识()).thenReturn("总菜单");
        when(技能菜单实例.获取菜单标识()).thenReturn("技能菜单");
        when(任务菜单实例.获取菜单标识()).thenReturn("任务菜单");
        when(天赋菜单实例.获取菜单标识()).thenReturn("天赋菜单");
        when(静态菜单实例.获取菜单标识()).thenReturn("主菜单");
        when(关键词解析器.解析(anyString(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        服务 = new 菜单服务实现(翻译服务, 关键词解析器,
                总菜单实例, 技能菜单实例, 任务菜单实例, 天赋菜单实例, 静态菜单实例);

        玩家标识 = UUID.randomUUID();
        when(玩家.getUniqueId()).thenReturn(玩家标识);
    }

    @Nested
    @DisplayName("注册菜单() - 修复空实现 bug")
    class 注册菜单修复 {

        @Test
        @DisplayName("注册前 - 总菜单不可获取（bug 复现基线）")
        void 注册前_总菜单不可获取() {
            Optional<菜单框架> 结果 = 服务.获取菜单("总菜单");
            assertTrue(结果.isEmpty(), "注册前总菜单不应可获取");
        }

        @Test
        @DisplayName("注册后 - 总菜单可获取")
        void 注册后_总菜单可获取() {
            服务.注册菜单();

            Optional<菜单框架> 结果 = 服务.获取菜单("总菜单");
            assertTrue(结果.isPresent(), "注册后总菜单应可获取");
            assertSame(总菜单实例, 结果.get(), "获取到的应为总菜单实例");
        }

        @Test
        @DisplayName("注册后 - 所有5个菜单均注册")
        void 注册后_所有菜单均注册() {
            服务.注册菜单();

            assertTrue(服务.获取菜单("总菜单").isPresent(), "总菜单应注册");
            assertTrue(服务.获取菜单("技能菜单").isPresent(), "技能菜单应注册");
            assertTrue(服务.获取菜单("任务菜单").isPresent(), "任务菜单应注册");
            assertTrue(服务.获取菜单("天赋菜单").isPresent(), "天赋菜单应注册");
            assertTrue(服务.获取菜单("主菜单").isPresent(), "主菜单(静态)应注册");
        }
    }

    @Nested
    @DisplayName("打开菜单 - /cd 命令核心路径")
    class 打开菜单路径 {

        @Test
        @DisplayName("注册后打开总菜单 - 应委托给总菜单.打开()")
        void 注册后打开总菜单_应委托打开() {
            服务.注册菜单();
            when(总菜单实例.打开(玩家)).thenReturn(true);

            boolean 结果 = 服务.打开菜单(玩家, "总菜单");

            assertTrue(结果, "打开总菜单应返回 true");
            verify(总菜单实例).打开(玩家);
        }

        @Test
        @DisplayName("未注册时打开总菜单 - 应返回 false 并发送菜单不存在消息")
        void 未注册时打开总菜单_应返回False并发送消息() {
            when(翻译服务.获取(eq("菜单框架.菜单.不存在"), any())).thenReturn("[错误]菜单 [警告]{0} [错误]不存在");

            boolean 结果 = 服务.打开菜单(玩家, "总菜单");

            assertFalse(结果, "未注册时打开总菜单应返回 false");
            verify(总菜单实例, never()).打开(any());
            verify(玩家, atLeastOnce()).sendMessage(any(Component.class));
        }

        @Test
        @DisplayName("注册后打开总菜单 - 不应发送菜单不存在消息")
        void 注册后打开总菜单_不应发送不存在消息() {
            服务.注册菜单();
            when(总菜单实例.打开(玩家)).thenReturn(true);

            服务.打开菜单(玩家, "总菜单");

            verify(翻译服务, never()).获取(eq("菜单框架.菜单.不存在"), any());
        }
    }

    @Nested
    @DisplayName("注册单个菜单 - 保留原注册接口")
    class 注册单个菜单 {

        @Test
        @DisplayName("注册单个菜单框架 - 应可获取")
        void 注册单个菜单_应可获取() {
            菜单框架 自定义菜单 = mock(菜单框架.class);
            when(自定义菜单.获取菜单标识()).thenReturn("自定义菜单");

            服务.注册菜单(自定义菜单);

            Optional<菜单框架> 结果 = 服务.获取菜单("自定义菜单");
            assertTrue(结果.isPresent());
            assertSame(自定义菜单, 结果.get());
        }
    }

    @Nested
    @DisplayName("关闭菜单 - 清理所有打开的菜单")
    class 关闭菜单 {

        @Test
        @DisplayName("关闭菜单 - 应调用所有菜单的关闭")
        void 关闭菜单_应调用所有菜单关闭() {
            服务.注册菜单();
            when(总菜单实例.是否打开(玩家标识)).thenReturn(true);
            when(技能菜单实例.是否打开(玩家标识)).thenReturn(false);

            服务.关闭菜单(玩家);

            verify(总菜单实例).关闭(玩家标识);
            verify(技能菜单实例, never()).关闭(any());
            verify(玩家).closeInventory();
        }
    }
}
