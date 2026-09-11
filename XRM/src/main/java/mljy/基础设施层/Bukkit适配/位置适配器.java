package mljy.基础设施层.Bukkit适配;

import mljy.领域层.玩家.位置;
import org.bukkit.Location;

public class 位置适配器 {
    public static 位置 转换(Location 位置) {
        if (位置 == null || 位置.getWorld() == null) {
            return new 位置("", 0, 0, 0, 0, 0);
        }
        return new 位置(
                位置.getWorld().getName(),
                位置.getX(),
                位置.getY(),
                位置.getZ(),
                位置.getYaw(),
                位置.getPitch()
        );
    }

    public static Location 转换(位置 位置) {
        return new Location(
                org.bukkit.Bukkit.getWorld(位置.世界()),
                位置.x(),
                位置.y(),
                位置.z(),
                位置.偏航角(),
                位置.俯仰角()
        );
    }
}
