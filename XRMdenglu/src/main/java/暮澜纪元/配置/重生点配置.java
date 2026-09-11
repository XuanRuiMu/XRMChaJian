package 暮澜纪元.配置;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/**
 * 重生点配置类。
 * 存储登录服玩家重生点位置信息。
 */
public class 重生点配置 {

    private final String 世界名;
    private final double 坐标X;
    private final double 坐标Y;
    private final double 坐标Z;
    private final float 偏航角;
    private final float 俯仰角;

    public 重生点配置(String 世界名, double 坐标X, double 坐标Y, double 坐标Z, float 偏航角, float 俯仰角) {
        this.世界名 = 世界名;
        this.坐标X = 坐标X;
        this.坐标Y = 坐标Y;
        this.坐标Z = 坐标Z;
        this.偏航角 = 偏航角;
        this.俯仰角 = 俯仰角;
    }

    public String 获取世界名() {
        return this.世界名;
    }

    public double 获取坐标X() {
        return this.坐标X;
    }

    public double 获取坐标Y() {
        return this.坐标Y;
    }

    public double 获取坐标Z() {
        return this.坐标Z;
    }

    public float 获取偏航角() {
        return this.偏航角;
    }

    public float 获取俯仰角() {
        return this.俯仰角;
    }

    /**
     * 根据配置构建Location对象。
     *
     * @return Location的Optional，若世界不存在则返回空
     */
    @SuppressWarnings("null")
    public Optional<Location> 构建位置() {
        World 世界 = Bukkit.getWorld(世界名);
        if (世界 == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(世界, 坐标X, 坐标Y, 坐标Z, 偏航角, 俯仰角));
    }
}
