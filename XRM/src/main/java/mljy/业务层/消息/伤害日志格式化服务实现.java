package mljy.业务层.消息;

import com.google.inject.Inject;
import mljy.翻译服务;
import mljy.基础设施层.Bukkit适配.实体适配器;
import mljy.基础设施层.颜色码工具;
import mljy.基础设施层.调试日志器;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

public class 伤害日志格式化服务实现 implements 伤害日志格式化服务 {
    private static final String 未知来源键 = "战斗日志.伤害来源.未知";

    private final 翻译服务 翻译服务;

    @Inject
    public 伤害日志格式化服务实现(翻译服务 翻译服务) {
        this.翻译服务 = 翻译服务;
    }

    @Override
    public String 获取实体来源名(Entity 来源实体) {
        if (来源实体 == null) {
            return 获取未知来源名();
        }
        if (来源实体 instanceof Projectile 投射物) {
            ProjectileSource 发射者 = 投射物.getShooter();
            if (发射者 instanceof Entity 发射实体) {
                String 发射者名 = 获取实体名称(发射实体);
                if (!发射者名.isBlank() && !发射者名.equals(获取未知来源名())) {
                    return 发射者名;
                }
            }
        }
        if (来源实体 instanceof Player || 来源实体 instanceof LivingEntity) {
            return 获取实体名称(来源实体);
        }
        String 实体类型名 = 来源实体.getType() == null ? "UNKNOWN" : 来源实体.getType().name();
        return 获取来源翻译("战斗日志.伤害来源." + 实体类型名);
    }

    @Override
    public String 获取实体名称(Entity 实体) {
        if (实体 == null) {
            return 获取未知来源名();
        }
        if (实体 instanceof Player 玩家) {
            String 玩家名 = 玩家.getName();
            return 玩家名 == null || 玩家名.isBlank()
                    ? 获取未知来源名() : 颜色码工具.转换颜色码(玩家名);
        }
        String 安全名称 = 实体适配器.获取安全名称(实体);
        if (!安全名称.isBlank()) {
            return 颜色码工具.转换颜色码(安全名称);
        }
        String 实体类型名 = 实体.getType() == null ? "UNKNOWN" : 实体.getType().name();
        return 获取来源翻译("战斗日志.伤害来源." + 实体类型名);
    }

    @Override
    public String 获取来源翻译(String 翻译键) {
        String 翻译文本 = 翻译服务.获取(翻译键);
        if (翻译文本 == null || 翻译文本.isBlank() || 翻译文本.equals(翻译键)) {
            return 获取未知来源名();
        }
        return 翻译文本;
    }

    @Override
    public String 获取未知来源名() {
        return 获取安全翻译(未知来源键, "");
    }

    @Override
    public String 获取安全翻译(String 翻译键, String 回退文本) {
        if (翻译键 == null || 翻译键.isBlank()) {
            return 回退文本 == null ? "" : 回退文本;
        }
        String 翻译文本 = 翻译服务.获取(翻译键);
        if (翻译文本 == null || 翻译文本.isBlank() || 翻译文本.equals(翻译键)) {
            return 回退文本 == null ? "" : 回退文本;
        }
        return 翻译文本;
    }
}
