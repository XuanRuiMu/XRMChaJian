package 暮澜纪元.测试工具;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.TestReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
public class 玩家输出捕获器 {

    private final Player mock玩家;
    private final List<String> 消息列表;

    private 玩家输出捕获器(Player mock玩家, List<String> 消息列表) {
        this.mock玩家 = mock玩家;
        this.消息列表 = 消息列表;
    }

    public static 玩家输出捕获器 创建() {
        Player mock玩家 = mock(Player.class);
        lenient().when(mock玩家.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(mock玩家.isOnline()).thenReturn(true);
        lenient().when(mock玩家.getName()).thenReturn("测试玩家");

        List<String> 消息列表 = new ArrayList<>();

        lenient().doAnswer(invocation -> {
            Component 组件 = invocation.getArgument(0);
            String 序列化 = MiniMessage.miniMessage().serialize(组件);
            消息列表.add(序列化);
            return null;
        }).when(mock玩家).sendMessage(any(Component.class));

        lenient().doAnswer(invocation -> {
            String 消息 = invocation.getArgument(0);
            消息列表.add(消息);
            return null;
        }).when(mock玩家).sendRichMessage(anyString());

        lenient().doAnswer(invocation -> {
            Component 组件 = invocation.getArgument(0);
            String 序列化 = MiniMessage.miniMessage().serialize(组件);
            消息列表.add("[ActionBar] " + 序列化);
            return null;
        }).when(mock玩家).sendActionBar(any(Component.class));

        return new 玩家输出捕获器(mock玩家, 消息列表);
    }

    public Player 获取玩家() { return mock玩家; }

    public void 设置语言(String 语言代码) {
        switch (语言代码) {
            case "en" -> lenient().when(mock玩家.locale()).thenReturn(Locale.US);
            case "ja" -> lenient().when(mock玩家.locale()).thenReturn(Locale.JAPAN);
            case "ko" -> lenient().when(mock玩家.locale()).thenReturn(Locale.KOREA);
            case "zh_tw" -> lenient().when(mock玩家.locale()).thenReturn(Locale.TRADITIONAL_CHINESE);
            case "de" -> lenient().when(mock玩家.locale()).thenReturn(Locale.GERMANY);
            case "fr" -> lenient().when(mock玩家.locale()).thenReturn(Locale.FRANCE);
            case "it" -> lenient().when(mock玩家.locale()).thenReturn(Locale.ITALY);
            case "es" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("es", "ES"));
            case "pt" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("pt", "BR"));
            case "ru" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("ru", "RU"));
            case "th" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("th", "TH"));
            case "tr" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("tr", "TR"));
            case "vi" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("vi", "VN"));
            case "id" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("id", "ID"));
            case "ms" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("ms", "MY"));
            case "nl" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("nl", "NL"));
            case "pl" -> lenient().when(mock玩家.locale()).thenReturn(Locale.of("pl", "PL"));
            default -> lenient().when(mock玩家.locale()).thenReturn(Locale.SIMPLIFIED_CHINESE);
        }
    }

    public void 设置OP(boolean 是否OP) { lenient().when(mock玩家.isOp()).thenReturn(是否OP); }

    public List<String> 获取所有消息() { return new ArrayList<>(消息列表); }
    public String 获取最后消息() { return 消息列表.isEmpty() ? null : 消息列表.get(消息列表.size() - 1); }
    public int 获取消息数量() { return 消息列表.size(); }

    public void 断言包含(String 期望内容, String 场景描述) { 断言最后消息包含(期望内容, 场景描述); }

    public void 断言最后消息包含(String 期望内容, String 场景描述) {
        String 最后消息 = 获取最后消息();
        assertNotNull(最后消息, 场景描述 + " → 玩家应收到消息，但未收到任何消息");
        String 去格式 = 去格式标签(最后消息);
        assertTrue(去格式.contains(期望内容), 场景描述 + " → 聊天栏应输出：" + 期望内容 + "，实际: " + 去格式);
    }

    public void 断言任意消息包含(String 期望内容, String 场景描述) {
        List<String> 所有消息 = 获取所有消息();
        assertFalse(所有消息.isEmpty(), 场景描述 + " → 玩家应收到消息，但未收到任何消息");
        boolean 找到 = 所有消息.stream().map(玩家输出捕获器::去格式标签).anyMatch(文本 -> 文本.contains(期望内容));
        assertTrue(找到, 场景描述 + " → 任意消息应包含'" + 期望内容 + "'，实际所有消息: " + 格式化所有消息(所有消息));
    }

    public void 断言消息数量(int 期望数量, String 场景描述) {
        assertEquals(期望数量, 消息列表.size(), 场景描述 + " → 消息数量不匹配，期望: " + 期望数量 + "，实际: " + 消息列表.size());
    }

    public void 断言无消息(String 场景描述) {
        assertTrue(消息列表.isEmpty(), 场景描述 + " → 玩家不应收到消息，但收到了: " + 消息列表);
    }

    public void 清除() { 消息列表.clear(); }

    public void 发布到报告(TestReporter 报告) {
        List<String> 所有消息 = 获取所有消息();
        if (所有消息.isEmpty()) {
            报告.publishEntry("玩家聊天栏实际输出", "（无输出）");
        } else {
            StringBuilder 汇总 = new StringBuilder();
            for (int i = 0; i < 所有消息.size(); i++) {
                String 去格式 = 去格式标签(所有消息.get(i));
                if (i > 0) 汇总.append(" | ");
                汇总.append(去格式);
                if (!去格式.isBlank()) 报告.publishEntry("消息[" + i + "]", 去格式);
            }
            String 汇总文本 = 汇总.toString();
            if (!汇总文本.isBlank()) 报告.publishEntry("玩家聊天栏实际输出", 汇总文本);
            else 报告.publishEntry("玩家聊天栏实际输出", "（格式标签内容）");
        }
    }

    private static String 格式化所有消息(List<String> 所有消息) {
        return 所有消息.stream().map(玩家输出捕获器::去格式标签).collect(Collectors.joining(" | "));
    }

    public static String 去格式标签(String 含格式文本) {
        if (含格式文本 == null) return "";
        try {
            Component 组件 = MiniMessage.miniMessage().deserialize(含格式文本);
            return PlainTextComponentSerializer.plainText().serialize(组件);
        } catch (Exception e) {
            return 含格式文本.replaceAll("<[^>]+>", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        }
    }
}
