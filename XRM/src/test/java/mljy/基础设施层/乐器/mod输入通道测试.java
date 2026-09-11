package mljy.基础设施层.乐器;

import mljy.业务层.乐器服务;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-12 mod 输入通道测试（mod 边界）。
 * 验证 PluginMessage 通道注册/注销、消息解析与分发到 乐器服务 的行为。
 * <p>
 * 注意：调试日志器在未初始化时为 no-op，无需 mock。
 */
@DisplayName("FP-12: mod 输入通道")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class mod输入通道测试 {

    @Mock
    private JavaPlugin 插件mock;
    @Mock
    private 乐器服务 乐器服务mock;
    @Mock
    private 乐器注册表 乐器注册表mock;
    @Mock
    private Player 玩家mock;
    @Mock
    private Server 服务器mock;
    @Mock
    private Messenger 信使mock;

    private mod输入通道 通道;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        玩家标识 = UUID.randomUUID();
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(插件mock.getServer()).thenReturn(服务器mock);
        when(服务器mock.getMessenger()).thenReturn(信使mock);
        通道 = new mod输入通道(插件mock, 乐器服务mock, 乐器注册表mock);
    }

    /**
     * 构造 PluginMessage 标准格式：2 字节长度前缀（big-endian）+ JSON 字节。
     */
    private byte[] 构造消息(String json) {
        byte[] json字节 = json.getBytes(StandardCharsets.UTF_8);
        byte[] 消息 = new byte[2 + json字节.length];
        消息[0] = (byte) ((json字节.length >> 8) & 0xFF);
        消息[1] = (byte) (json字节.length & 0xFF);
        System.arraycopy(json字节, 0, 消息, 2, json字节.length);
        return 消息;
    }

    private 乐器定义 创建测试乐器() {
        return new 乐器定义("钢琴", "钢琴", Sound.BLOCK_NOTE_BLOCK_HARP, "xrm.test.piano",
                Material.STICK, 9001, 2, 0, 24, 0,
                true, "测试乐器", null, 乐器定义.物理机制_瞬时触发);
    }

    @Nested
    @DisplayName("通道注册与注销")
    class 通道注册测试 {

        @Test
        @DisplayName("新通道应未注册")
        void 新通道未注册() {
            assertFalse(通道.是否已注册());
        }

        @Test
        @DisplayName("注册通道应返回 true 并调用 Messenger")
        void 注册成功() {
            boolean 结果 = 通道.注册通道();
            assertTrue(结果);
            assertTrue(通道.是否已注册());
            verify(信使mock).registerIncomingPluginChannel(插件mock, mod输入通道.通道名, 通道);
        }

        @Test
        @DisplayName("重复注册应返回 false 不再调用 Messenger")
        void 重复注册返回false() {
            assertTrue(通道.注册通道());
            boolean 结果 = 通道.注册通道();
            assertFalse(结果);
            verify(信使mock, times(1)).registerIncomingPluginChannel(any(), anyString(), any());
        }

        @Test
        @DisplayName("注销通道应调用 Messenger 并清除注册状态")
        void 注销成功() {
            通道.注册通道();
            通道.注销通道();
            assertFalse(通道.是否已注册());
            verify(信使mock).unregisterIncomingPluginChannel(插件mock, mod输入通道.通道名, 通道);
        }

        @Test
        @DisplayName("未注册时注销应无操作不抛异常")
        void 未注册时注销无异常() {
            assertDoesNotThrow(() -> 通道.注销通道());
            verify(信使mock, never()).unregisterIncomingPluginChannel(any(), anyString(), any());
        }

        @Test
        @DisplayName("注册后再注销再注册应成功")
        void 注册注销再注册() {
            assertTrue(通道.注册通道());
            通道.注销通道();
            assertFalse(通道.是否已注册());
            assertTrue(通道.注册通道());
            assertTrue(通道.是否已注册());
        }

        @Test
        @DisplayName("注册时 Messenger 抛异常应返回 false")
        void 注册异常返回false() {
            doThrow(new RuntimeException("测试异常"))
                    .when(信使mock).registerIncomingPluginChannel(any(), anyString(), any());
            boolean 结果 = 通道.注册通道();
            assertFalse(结果);
            assertFalse(通道.是否已注册());
        }
    }

    @Nested
    @DisplayName("消息接收校验")
    class 消息接收校验测试 {

        @Test
        @DisplayName("错误通道应被忽略")
        void 错误通道忽略() {
            byte[] 消息 = 构造消息("{\"type\":\"pitch_bend\",\"value\":64}");
            通道.onPluginMessageReceived("wrong:channel", 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("null 玩家应被忽略")
        void null玩家忽略() {
            byte[] 消息 = 构造消息("{\"type\":\"pitch_bend\",\"value\":64}");
            通道.onPluginMessageReceived(mod输入通道.通道名, (Player) null, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("null 消息应被忽略")
        void null消息忽略() {
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, null);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("超大消息应被忽略")
        void 超大消息忽略() {
            byte[] 大消息 = new byte[mod输入通道.消息大小上限字节 + 1];
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 大消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("长度字段为 0 应被忽略")
        void 长度0忽略() {
            byte[] 消息 = new byte[]{0, 0};
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("长度字段超过消息体应被忽略")
        void 长度超限忽略() {
            // 长度字段声明 100 但实际只有 2 字节
            byte[] 消息 = new byte[]{0, 100, 'a'};
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }
    }

    @Nested
    @DisplayName("消息分发")
    class 消息分发测试 {

        @Test
        @DisplayName("pitch_bend 消息应调用 乐器服务.处理弯音轮")
        void pitchBend分发() {
            byte[] 消息 = 构造消息("{\"type\":\"pitch_bend\",\"value\":64}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理弯音轮(玩家mock, 64);
        }

        @Test
        @DisplayName("modulation 消息应调用 乐器服务.处理调制轮")
        void modulation分发() {
            byte[] 消息 = 构造消息("{\"type\":\"modulation\",\"value\":100}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理调制轮(玩家mock, 100);
        }

        @Test
        @DisplayName("expression 消息应调用 乐器服务.处理表情控制")
        void expression分发() {
            byte[] 消息 = 构造消息("{\"type\":\"expression\",\"cc\":11,\"value\":80}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理表情控制(玩家mock, 11, 80);
        }

        @Test
        @DisplayName("note_off 消息应调用 乐器服务.记录按键松开")
        void noteOff分发() {
            byte[] 消息 = 构造消息("{\"type\":\"note_off\",\"pitch\":7}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).记录按键松开(玩家标识, 7);
        }

        @Test
        @DisplayName("note_on 消息已知乐器应调用 乐器服务.处理mod输入")
        void noteOn已知乐器分发() {
            乐器定义 乐器 = 创建测试乐器();
            when(乐器注册表mock.按名称查找("钢琴")).thenReturn(Optional.of(乐器));
            byte[] 消息 = 构造消息(
                    "{\"type\":\"note_on\",\"instrument\":\"钢琴\",\"pitch\":5,\"velocity\":80,\"timestamp\":123456}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理mod输入(eq(玩家标识), eq(乐器), eq(5), eq(80), eq(123456L), eq(0L));
        }

        @Test
        @DisplayName("note_on 消息未知乐器应回退到玩家当前乐器")
        void noteOn未知乐器回退() {
            乐器定义 乐器 = 创建测试乐器();
            when(乐器注册表mock.按名称查找("未知乐器")).thenReturn(Optional.empty());
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.of(乐器));
            byte[] 消息 = 构造消息(
                    "{\"type\":\"note_on\",\"instrument\":\"未知乐器\",\"pitch\":5,\"velocity\":80,\"timestamp\":123456}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理mod输入(eq(玩家标识), eq(乐器), eq(5), eq(80), eq(123456L), eq(0L));
        }

        @Test
        @DisplayName("note_on 消息乐器全找不到应不调用 处理mod输入")
        void noteOn无乐器不分发() {
            when(乐器注册表mock.按名称查找(anyString())).thenReturn(Optional.empty());
            when(乐器服务mock.获取玩家当前乐器(玩家标识)).thenReturn(Optional.empty());
            byte[] 消息 = 构造消息(
                    "{\"type\":\"note_on\",\"instrument\":\"不存在\",\"pitch\":5,\"velocity\":80,\"timestamp\":123456}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock, never()).处理mod输入(any(), any(), anyInt(), anyInt(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("未知 type 应被忽略")
        void 未知类型忽略() {
            byte[] 消息 = 构造消息("{\"type\":\"unknown_type\",\"value\":1}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("缺少 type 字段应被忽略")
        void 缺少类型忽略() {
            byte[] 消息 = 构造消息("{\"value\":1}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("空 JSON 应被忽略")
        void 空Json忽略() {
            byte[] 消息 = 构造消息("");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }

        @Test
        @DisplayName("空白 JSON 应被忽略")
        void 空白Json忽略() {
            byte[] 消息 = 构造消息("   ");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verifyNoInteractions(乐器服务mock);
        }
    }

    @Nested
    @DisplayName("JSON 字段提取")
    class Json字段提取测试 {

        @Test
        @DisplayName("pitch_bend 缺少 value 字段应传 0")
        void pitchBend缺value传0() {
            byte[] 消息 = 构造消息("{\"type\":\"pitch_bend\"}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理弯音轮(玩家mock, 0);
        }

        @Test
        @DisplayName("note_off 缺少 pitch 字段应传 0")
        void noteOff缺pitch传0() {
            byte[] 消息 = 构造消息("{\"type\":\"note_off\"}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).记录按键松开(玩家标识, 0);
        }

        @Test
        @DisplayName("expression 缺少 cc 和 value 应传 0")
        void expression缺字段传0() {
            byte[] 消息 = 构造消息("{\"type\":\"expression\"}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理表情控制(玩家mock, 0, 0);
        }

        @Test
        @DisplayName("带空格的 JSON 应正确解析")
        void 带空格Json解析() {
            byte[] 消息 = 构造消息("{ \"type\" : \"pitch_bend\" , \"value\" : 64 }");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理弯音轮(玩家mock, 64);
        }

        @Test
        @DisplayName("负数 value 应被正确解析")
        void 负数value解析() {
            byte[] 消息 = 构造消息("{\"type\":\"pitch_bend\",\"value\":-10}");
            通道.onPluginMessageReceived(mod输入通道.通道名, 玩家mock, 消息);
            verify(乐器服务mock).处理弯音轮(玩家mock, -10);
        }
    }

    @Nested
    @DisplayName("常量")
    class 常量测试 {

        @Test
        @DisplayName("通道名应为 xrm:music_input")
        void 通道名() {
            assertEquals("xrm:music_input", mod输入通道.通道名);
        }

        @Test
        @DisplayName("类型_按键按下 应为 note_on")
        void 类型按键按下() {
            assertEquals("note_on", mod输入通道.类型_按键按下);
        }

        @Test
        @DisplayName("类型_按键松开 应为 note_off")
        void 类型按键松开() {
            assertEquals("note_off", mod输入通道.类型_按键松开);
        }

        @Test
        @DisplayName("类型_弯音轮 应为 pitch_bend")
        void 类型弯音轮() {
            assertEquals("pitch_bend", mod输入通道.类型_弯音轮);
        }

        @Test
        @DisplayName("类型_调制轮 应为 modulation")
        void 类型调制轮() {
            assertEquals("modulation", mod输入通道.类型_调制轮);
        }

        @Test
        @DisplayName("类型_表情控制 应为 expression")
        void 类型表情控制() {
            assertEquals("expression", mod输入通道.类型_表情控制);
        }

        @Test
        @DisplayName("消息大小上限应为 4096 字节")
        void 消息大小上限() {
            assertEquals(4096, mod输入通道.消息大小上限字节);
        }

        @Test
        @DisplayName("频率上限每tick 应为 1")
        void 频率上限() {
            assertEquals(1, mod输入通道.频率上限每tick);
        }
    }
}
