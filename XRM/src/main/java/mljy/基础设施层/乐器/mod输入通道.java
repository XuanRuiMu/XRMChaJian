package mljy.基础设施层.乐器;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.业务层.乐器服务;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.基础设施层.调试日志器;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * FP-12 mod 输入通道（Neoforge 客户端 mod 边界）。
 * <p>
 * mod 职责严格限定（需求文档 1615-1630 行）：
 * <ul>
 *   <li>仅负责输入采集（字母键、力度键、延音踏板、弯音键、调制轮键、表情键）与网络包传输</li>
 *   <li>可选：客户端 HUD 渲染（按键高亮、力度指示、当前音域/乐器状态显示、弯音/调制轮状态、表情实时值）</li>
 *   <li>禁止在客户端做任何演奏判定、力度计算、音域换算、弯音曲线计算——全部由服务端权威处理</li>
 * </ul>
 * <p>
 * 通信协议：使用 PluginMessage 通道 "xrm:music_input"。
 * 消息格式（需求文档 1624-1627 行）：
 * <ul>
 *   <li>JSON 文本：{type: "note_on"/"note_off"/"pitch_bend"/"modulation"/"expression", ...}</li>
 *   <li>type=note_on：{type, instrument, pitch, velocity, timestamp}</li>
 *   <li>type=note_off：{type, pitch, timestamp}</li>
 *   <li>type=pitch_bend：{type, value(0-127)}</li>
 *   <li>type=modulation：{type, value(0-127)}</li>
 *   <li>type=expression：{type, cc, value(0-127)}</li>
 * </ul>
 * <p>
 * 反作弊考量（需求文档 1629-1630 行）：
 * <ul>
 *   <li>服务端校验按键事件的合理性（频率上限、时间戳防重放、力度范围合法性、弯音/表情值范围）</li>
 *   <li>关键数值服务端算 [P0]：演奏判定、力度计算、音域换算、弯音曲线计算全部在服务端完成</li>
 * </ul>
 * <p>
 * 注意：mod 端代码本轮不实现，仅服务端预留通道入口。mod 端由独立的 Neoforge 客户端 mod 项目实现。
 */
@Singleton
public class mod输入通道 implements PluginMessageListener {
    /** PluginMessage 通道名（与需求文档 FP-12 一致）。 */
    public static final String 通道名 = "xrm:music_input";
    /** 消息类型：按键按下。 */
    public static final String 类型_按键按下 = "note_on";
    /** 消息类型：按键松开。 */
    public static final String 类型_按键松开 = "note_off";
    /** 消息类型：弯音轮。 */
    public static final String 类型_弯音轮 = "pitch_bend";
    /** 消息类型：调制轮。 */
    public static final String 类型_调制轮 = "modulation";
    /** 消息类型：表情控制。 */
    public static final String 类型_表情控制 = "expression";
    /** 通道是否已注册。 */
    private volatile boolean 通道已注册;
    /** 反作弊：每玩家每 tick 消息频率上限。 */
    public static final int 频率上限每tick = 1;
    /** 反作弊：消息体大小上限（字节），防止恶意 mod 发送超大消息。 */
    public static final int 消息大小上限字节 = 4096;

    private final JavaPlugin 插件;
    private final 乐器服务 乐器服务;
    private final 乐器注册表 乐器注册表;

    @Inject
    public mod输入通道(JavaPlugin 插件, 乐器服务 乐器服务, 乐器注册表 乐器注册表) {
        this.插件 = 插件;
        this.乐器服务 = 乐器服务;
        this.乐器注册表 = 乐器注册表;
    }

    /**
     * 注册 mod 输入通道。
     * 由 玄锐暮插件.onEnable 调用。
     *
     * @return true=注册成功；false=已注册或注册失败
     */
    public boolean 注册通道() {
        if (通道已注册) {
            return false;
        }
        try {
            插件.getServer().getMessenger().registerIncomingPluginChannel(插件, 通道名, this);
            通道已注册 = true;
            调试日志器.调试("mod输入通道", "通道已注册：%s", 通道名);
            return true;
        } catch (Exception e) {
            调试日志器.调试("mod输入通道", "注册通道失败：%s", e.getMessage());
            return false;
        }
    }

    /**
     * 查询通道是否已注册。
     */
    public boolean 是否已注册() {
        return 通道已注册;
    }

    /**
     * 注销通道（插件禁用时调用）。
     */
    public void 注销通道() {
        if (!通道已注册) {
            return;
        }
        try {
            插件.getServer().getMessenger().unregisterIncomingPluginChannel(插件, 通道名, this);
        } catch (Exception e) {
            调试日志器.调试("mod输入通道", "注销通道异常：%s", e.getMessage());
        }
        通道已注册 = false;
    }

    /**
     * 接收 mod 端消息入口。
     * <p>
     * 反作弊策略（需求文档 1629-1630 行）：
     * <ul>
     *   <li>消息大小校验（<= {@link #消息大小上限字节} 字节）</li>
     *   <li>消息格式校验（必须是 channel 长度前缀 + JSON 文本）</li>
     *   <li>消息内容校验（type 字段必填，参数范围 clamp）</li>
     *   <li>频率上限：每玩家每 tick 1 次（本轮简化为不实现频率限制，留作未来增强）</li>
     * </ul>
     */
    @Override
    public void onPluginMessageReceived(String 通道, Player 玩家, byte[] 消息) {
        if (!通道名.equals(通道) || 玩家 == null || 消息 == null) {
            return;
        }
        if (消息.length > 消息大小上限字节) {
            调试日志器.调试("mod输入通道", "拒绝超大消息：玩家=%s, 大小=%d", 玩家.getUniqueId(), 消息.length);
            return;
        }
        // 解析 channel 长度前缀 + JSON 文本（PluginMessage 标准格式）
        String json文本;
        try (DataInputStream 输入 = new DataInputStream(new ByteArrayInputStream(消息))) {
            int 长度 = 输入.readUnsignedShort();
            if (长度 <= 0 || 长度 > 消息.length - 2) {
                调试日志器.调试("mod输入通道", "消息长度字段无效：玩家=%s, 长度=%d", 玩家.getUniqueId(), 长度);
                return;
            }
            byte[] json字节 = new byte[长度];
            输入.readFully(json字节);
            json文本 = new String(json字节, StandardCharsets.UTF_8);
        } catch (IOException e) {
            调试日志器.调试("mod输入通道", "消息解析失败：玩家=%s, 异常=%s", 玩家.getUniqueId(), e.getMessage());
            return;
        }
        处理消息(玩家, json文本);
    }

    /**
     * 解析 JSON 消息并分发到 乐器服务 对应方法。
     * 本轮使用简化 JSON 解析（无外部依赖，避免引入 Gson 等库）。
     */
    private void 处理消息(Player 玩家, String json文本) {
        if (json文本 == null || json文本.isBlank()) {
            return;
        }
        String 类型 = 提取字符串字段(json文本, "type");
        if (类型 == null) {
            调试日志器.调试("mod输入通道", "消息缺少 type 字段：玩家=%s", 玩家.getUniqueId());
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        switch (类型) {
            case 类型_按键按下 -> 处理按键按下(玩家, json文本);
            case 类型_按键松开 -> 处理按键松开(玩家标识, json文本);
            case 类型_弯音轮 -> 乐器服务.处理弯音轮(玩家, 提取整数字段(json文本, "value"));
            case 类型_调制轮 -> 乐器服务.处理调制轮(玩家, 提取整数字段(json文本, "value"));
            case 类型_表情控制 -> 乐器服务.处理表情控制(玩家,
                    提取整数字段(json文本, "cc"), 提取整数字段(json文本, "value"));
            default -> 调试日志器.调试("mod输入通道", "未知消息类型：%s, 玩家=%s", 类型, 玩家标识);
        }
    }

    /**
     * 处理 note_on 消息：调用 乐器服务.处理mod输入。
     */
    private void 处理按键按下(Player 玩家, String json文本) {
        UUID 玩家标识 = 玩家.getUniqueId();
        String 乐器名 = 提取字符串字段(json文本, "instrument");
        int 音高 = 提取整数字段(json文本, "pitch");
        int velocity = 提取整数字段(json文本, "velocity");
        long 时间戳 = 提取长整数字段(json文本, "timestamp");
        乐器定义 乐器 = 乐器注册表.按名称查找(乐器名).orElse(null);
        if (乐器 == null) {
            // 回退到玩家当前乐器
            乐器 = 乐器服务.获取玩家当前乐器(玩家标识).orElse(null);
            if (乐器 == null) {
                调试日志器.调试("mod输入通道", "note_on 消息未找到乐器：玩家=%s, 乐器名=%s", 玩家标识, 乐器名);
                return;
            }
        }
        // 简化实现：note_on 直接触发演奏（按下时间戳 = 当前时间戳）
        // mod 端未来可在 note_off 时再次发送消息，由服务端计算 velocity-by-duration
        乐器服务.处理mod输入(玩家标识, 乐器, 音高, velocity, 时间戳, 0L);
    }

    /**
     * 处理 note_off 消息：调用 乐器服务.记录按键松开。
     */
    private void 处理按键松开(UUID 玩家标识, String json文本) {
        int 音高 = 提取整数字段(json文本, "pitch");
        乐器服务.记录按键松开(玩家标识, 音高);
    }

    /**
     * 简化 JSON 字符串字段提取（不依赖外部 JSON 库）。
     * 匹配模式："字段名" : "值"
     */
    private static String 提取字符串字段(String json, String 字段名) {
        String 模式 = "\"" + 字段名 + "\"";
        int 索引 = json.indexOf(模式);
        if (索引 < 0) {
            return null;
        }
        int 冒号位置 = json.indexOf(':', 索引 + 模式.length());
        if (冒号位置 < 0) {
            return null;
        }
        int 引号开始 = json.indexOf('"', 冒号位置 + 1);
        if (引号开始 < 0) {
            return null;
        }
        int 引号结束 = json.indexOf('"', 引号开始 + 1);
        if (引号结束 < 0) {
            return null;
        }
        return json.substring(引号开始 + 1, 引号结束);
    }

    /**
     * 简化 JSON 整数字段提取（不依赖外部 JSON 库）。
     * 匹配模式："字段名" : 数值
     */
    private static int 提取整数字段(String json, String 字段名) {
        return (int) 提取长整数字段(json, 字段名);
    }

    /**
     * 简化 JSON 长整数字段提取（不依赖外部 JSON 库）。
     */
    private static long 提取长整数字段(String json, String 字段名) {
        String 模式 = "\"" + 字段名 + "\"";
        int 索引 = json.indexOf(模式);
        if (索引 < 0) {
            return 0L;
        }
        int 冒号位置 = json.indexOf(':', 索引 + 模式.length());
        if (冒号位置 < 0) {
            return 0L;
        }
        int 开始 = 冒号位置 + 1;
        while (开始 < json.length() && Character.isWhitespace(json.charAt(开始))) {
            开始++;
        }
        int 结束 = 开始;
        while (结束 < json.length()) {
            char 字符 = json.charAt(结束);
            if (字符 == '-' || Character.isDigit(字符)) {
                结束++;
            } else {
                break;
            }
        }
        if (开始 >= 结束) {
            return 0L;
        }
        try {
            return Long.parseLong(json.substring(开始, 结束));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
