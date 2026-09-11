package mljy.基础设施层;

import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("FP-03 XRM技能名解析器测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class XRM技能名解析器测试 {

    @Mock
    private 翻译服务 翻译服务;

    @Mock
    private 玩家服务 玩家服务;

    @InjectMocks
    private XRM技能名解析器 解析器;

    @Nested
    @DisplayName("正常解析：玩家有会话+专精匹配+翻译键存在")
    class 正常解析 {

        @Test
        @DisplayName("槽位一+奥能法师 应解析为 奥术冲击")
        void 槽位一应解析为奥术冲击() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            when(翻译服务.获取(eq("skill.奥能法师.slot.一.name"))).thenReturn("奥术冲击");

            Optional<String> 结果 = 解析器.解析技能名("一", 玩家标识);

            assertTrue(结果.isPresent(), "应解析出技能名");
            assertEquals("奥术冲击", 结果.get(), "槽位一应解析为奥术冲击");
        }

        @Test
        @DisplayName("槽位五+奥能法师 应解析为 混沌束缚")
        void 槽位五应解析为混沌束缚() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            when(翻译服务.获取(eq("skill.奥能法师.slot.五.name"))).thenReturn("混沌束缚");

            Optional<String> 结果 = 解析器.解析技能名("五", 玩家标识);

            assertTrue(结果.isPresent(), "应解析出技能名");
            assertEquals("混沌束缚", 结果.get(), "槽位五应解析为混沌束缚");
        }

        @Test
        @DisplayName("槽位九 应解析为 精通魔法预兆")
        void 槽位九应解析为精通魔法预兆() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            when(翻译服务.获取(eq("skill.奥能法师.slot.九.name"))).thenReturn("精通魔法预兆");

            Optional<String> 结果 = 解析器.解析技能名("九", 玩家标识);

            assertTrue(结果.isPresent(), "应解析出技能名");
            assertEquals("精通魔法预兆", 结果.get(), "槽位九应解析为精通魔法预兆");
        }

        @Test
        @DisplayName("玩家自定义专精后应按新专精拼翻译键")
        void 自定义专精应按新专精拼键() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            会话.设置专精("冰霜法师");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            when(翻译服务.获取(eq("skill.冰霜法师.slot.一.name"))).thenReturn("冰锥术");

            Optional<String> 结果 = 解析器.解析技能名("一", 玩家标识);

            assertTrue(结果.isPresent(), "自定义专精应能解析");
            assertEquals("冰锥术", 结果.get(), "应按新专精解析技能名");
        }
    }

    @Nested
    @DisplayName("异常输入：null 或会话缺失")
    class 异常输入 {

        @Test
        @DisplayName("玩家标识为 null 应返回 empty")
        void 玩家标识为null应返回empty() {
            Optional<String> 结果 = 解析器.解析技能名("一", null);

            assertTrue(结果.isEmpty(), "玩家标识为 null 应返回 empty");
        }

        @Test
        @DisplayName("玩家会话不存在应返回 empty")
        void 玩家会话不存在应返回empty() {
            UUID 玩家标识 = UUID.randomUUID();
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.empty());

            Optional<String> 结果 = 解析器.解析技能名("一", 玩家标识);

            assertTrue(结果.isEmpty(), "玩家会话不存在应返回 empty");
        }
    }

    @Nested
    @DisplayName("翻译键缺失：翻译服务返回键本身")
    class 翻译键缺失 {

        @Test
        @DisplayName("翻译服务返回键本身应返回 empty（翻译缺失）")
        void 翻译服务返回键本身应返回empty() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            String 缺失键 = "skill.奥能法师.slot.一.name";
            when(翻译服务.获取(eq(缺失键))).thenReturn(缺失键);

            Optional<String> 结果 = 解析器.解析技能名("一", 玩家标识);

            assertTrue(结果.isEmpty(), "翻译键缺失（返回键本身）应返回 empty");
        }

        @Test
        @DisplayName("未定义槽位（如十）应返回 empty")
        void 未定义槽位应返回empty() {
            UUID 玩家标识 = UUID.randomUUID();
            玩家会话 会话 = new 玩家会话(玩家标识, "测试玩家");
            when(玩家服务.获取会话(玩家标识)).thenReturn(Optional.of(会话));
            String 缺失键 = "skill.奥能法师.slot.十.name";
            when(翻译服务.获取(eq(缺失键))).thenReturn(缺失键);

            Optional<String> 结果 = 解析器.解析技能名("十", 玩家标识);

            assertTrue(结果.isEmpty(), "未定义槽位应返回 empty");
        }
    }
}
