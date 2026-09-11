package mljy.基础设施层.Bukkit适配;

import mljy.领域层.实体;
import mljy.领域层.玩家.位置;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class 实体适配器 implements 实体 {
    private final Entity 实体;

    public 实体适配器(Entity 实体) {
        this.实体 = 实体;
    }

    @Override
    public UUID 获取唯一标识() {
        return 实体.getUniqueId();
    }

    @Override
    public String 获取名称() {
        return 获取安全名称(实体);
    }

    public static String 获取安全名称(Entity 实体) {
        if (实体 == null) {
            return "";
        }
        Component 自定义名称组件 = 实体.customName();
        String 自定义名称 = 自定义名称组件 == null ? null : LegacyComponentSerializer.legacySection().serialize(自定义名称组件);
        if (是可用名称(自定义名称)) {
            return 自定义名称;
        }
        if (实体 instanceof Player) {
            String 玩家名 = 实体.getName();
            return 是可用名称(玩家名) ? 玩家名 : "";
        }
        EntityType 实体类型 = 实体.getType();
        if (实体类型 != null) {
            String 翻译键 = 实体类型.translationKey();
            if (翻译键 != null && !翻译键.isBlank()) {
                return "<lang:" + 翻译键 + ">";
            }
        }
        String 原始名称 = 实体.getName();
        if (是可用名称(原始名称)) {
            return 原始名称;
        }
        return "";
    }

    public static boolean 是血条污染名称(String 名称) {
        if (名称 == null || 名称.isBlank()) {
            return false;
        }
        String 纯文本 = 名称.replaceAll("(?i)[§&][0-9A-FK-ORX]", "")
                .replaceAll("<[^>]*>", "")
                .replaceAll("[\\[\\]（）(){}\\s|:：/\\-]", "");
        if (纯文本.length() < 3) {
            return false;
        }
        int 血条字符数 = 0;
        for (int 索引 = 0; 索引 < 纯文本.length(); 索引++) {
            char 字符 = 纯文本.charAt(索引);
            if ("█▓▒░▌▐■▉▊▋▍▎▏".indexOf(字符) < 0) {
                return false;
            }
            血条字符数++;
        }
        return 血条字符数 >= 3;
    }

    private static boolean 是可用名称(String 名称) {
        return 名称 != null && !名称.isBlank() && !是血条污染名称(名称);
    }

    @Override
    public 位置 获取位置() {
        return 位置适配器.转换(实体.getLocation());
    }

    @Override
    public boolean 是否存活() {
        return 实体.isValid() && !实体.isDead();
    }

    public Entity 获取原始实体() {
        return 实体;
    }
}
