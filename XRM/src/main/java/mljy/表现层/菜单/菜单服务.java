package mljy.表现层.菜单;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface 菜单服务 {
    void 注册菜单(菜单框架 菜单);

    default void 注册菜单() {
    }

    Optional<菜单框架> 获取菜单(String 菜单标识);

    boolean 打开菜单(Player 玩家, String 菜单标识);

    void 关闭菜单(Player 玩家);
}
