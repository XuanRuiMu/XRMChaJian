package 暮澜纪元.命令;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import 暮澜纪元.传送门区域;
import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传送管理命令处理器。
 * 处理传送门区域的选择、确认和管理操作。
 * 合并了命令执行器和Tab补全器。
 */
public class 传送管理命令 implements CommandExecutor, TabCompleter {

    private static final List<String> 服务器列表 = Arrays.asList("MMORPG", "粘液科技", "多元生存");
    private static final List<String> 子命令列表 = Arrays.asList("选点一", "选点二", "确认", "查看", "重置", "重载", "设重生点", "看重生点");

    private final 登录服插件 插件;
    private final Map<String, Map<String, Location>> 选择缓存 = new HashMap<>();

    public 传送管理命令(登录服插件 插件) {
        this.插件 = 插件;
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!(发送者 instanceof Player)) {
            发送者.sendMessage(插件.获取消息管理器().获取类组件(null, 传送管理命令.class, "仅玩家可用"));
            return true;
        }

        Player 玩家 = (Player) 发送者;
        消息管理器 消息 = 插件.获取消息管理器();

        if (!玩家.hasPermission("xrmdenglu.admin")) {
            消息.发送类翻译(玩家, 传送管理命令.class, "无权限");
            return true;
        }

        if (参数.length < 1) {
            发送帮助信息(玩家);
            return true;
        }

        String 子命令 = 参数[0].toLowerCase();

        // 处理不需要服务器参数的命令
        if (子命令.equals("重载")) {
            return 重载配置(玩家);
        }
        if (子命令.equals("设重生点")) {
            return 设置重生点(玩家);
        }
        if (子命令.equals("看重生点")) {
            return 查看重生点(玩家);
        }

        if (参数.length < 2) {
            发送帮助信息(玩家);
            return true;
        }

        String 服务器名 = 参数[1].toLowerCase();

        if (!验证服务器名(服务器名)) {
            String 服务器列表文本 = String.join(", ", 服务器列表);
            玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "无效服务器名", 服务器列表文本));
            return true;
        }

        switch (子命令) {
            case "选点一":
                return 选择第一点(玩家, 服务器名);
            case "选点二":
                return 选择第二点(玩家, 服务器名);
            case "确认":
                return 确认选择(玩家, 服务器名);
            case "重置":
                return 重置选择(玩家, 服务器名);
            case "查看":
                return 查看当前选择(玩家, 服务器名);
            default:
                发送帮助信息(玩家);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 别名, String[] 参数) {
        List<String> 补全列表 = new ArrayList<>();

        if (!(发送者 instanceof Player)) {
            return 补全列表;
        }

        Player 玩家 = (Player) 发送者;

        if (!玩家.hasPermission("xrmdenglu.admin")) {
            return 补全列表;
        }

        // 第一个参数：可以是子命令或服务器名
        if (参数.length == 1) {
            String 输入 = 参数[0].toLowerCase();

            // 添加匹配的子命令
            for (String 子命令 : 子命令列表) {
                if (子命令.startsWith(输入)) {
                    补全列表.add(子命令);
                }
            }

            // 添加匹配的服务器名
            for (String 服务器 : 服务器列表) {
                if (服务器.startsWith(输入)) {
                    补全列表.add(服务器);
                }
            }
        }
        // 第二个参数
        else if (参数.length == 2) {
            String 第一个参数 = 参数[0].toLowerCase();
            String 输入 = 参数[1].toLowerCase();

            // 如果第一个参数是服务器名
            if (服务器列表.contains(第一个参数)) {
                for (String 子命令 : 子命令列表) {
                    // 不需要服务器参数的命令不显示
                    if (!子命令.equals("重载") && !子命令.equals("设重生点") && !子命令.equals("看重生点")
                            && 子命令.startsWith(输入)) {
                        补全列表.add(子命令);
                    }
                }
            }
            // 如果第一个参数是子命令（且不需要服务器参数），第二个参数应该是服务器名
            else if (!第一个参数.equals("重载") && !第一个参数.equals("设重生点") && !第一个参数.equals("看重生点")) {
                for (String 服务器 : 服务器列表) {
                    if (服务器.startsWith(输入)) {
                        补全列表.add(服务器);
                    }
                }
            }
        }

        Collections.sort(补全列表);
        return 补全列表;
    }

    private boolean 验证服务器名(String 服务器名) {
        return 服务器名.equals("MMORPG") ||
               服务器名.equals("粘液科技") ||
               服务器名.equals("多元生存");
    }

    private String 获取服务器显示名(String 服务器名) {
        return switch (服务器名) {
            case "MMORPG" -> "MMORPG服";
            case "粘液科技" -> "粘液科技服";
            case "多元生存" -> "多元生存服";
            default -> 服务器名;
        };
    }

    private void 发送帮助信息(Player 玩家) {
        消息管理器 消息 = 插件.获取消息管理器();
        String 服务器列表文本 = String.join(", ", 服务器列表);
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助标题"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助选点一"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助选点二"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助确认"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助查看"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助重置"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助设重生点"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助看重生点"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助重载"));
        玩家.sendMessage(消息.获取类组件(玩家, 传送管理命令.class, "帮助服务器列表", 服务器列表文本));
    }

    private boolean 选择第一点(Player 玩家, String 服务器名) {
        Block 目标方块 = 玩家.getTargetBlockExact(100);
        if (目标方块 == null) {
            玩家.sendMessage(获取组件(玩家, "传送管理.请对准方块"));
            return true;
        }

        Location 位置 = 目标方块.getLocation();
        选择缓存.computeIfAbsent(服务器名, k -> new HashMap<>()).put("点一", 位置);

        玩家.sendMessage(获取组件(玩家, "传送管理.已选择第一点",
            获取服务器显示名(服务器名),
            String.valueOf(位置.getBlockX()),
            String.valueOf(位置.getBlockY()),
            String.valueOf(位置.getBlockZ())));

        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 选择了 " + 服务器名 + " 的第一点");
        return true;
    }

    private boolean 选择第二点(Player 玩家, String 服务器名) {
        Block 目标方块 = 玩家.getTargetBlockExact(100);
        if (目标方块 == null) {
            玩家.sendMessage(获取组件(玩家, "传送管理.请对准方块"));
            return true;
        }

        Location 位置 = 目标方块.getLocation();
        选择缓存.computeIfAbsent(服务器名, k -> new HashMap<>()).put("点二", 位置);

        玩家.sendMessage(获取组件(玩家, "传送管理.已选择第二点",
            获取服务器显示名(服务器名),
            String.valueOf(位置.getBlockX()),
            String.valueOf(位置.getBlockY()),
            String.valueOf(位置.getBlockZ())));

        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 选择了 " + 服务器名 + " 的第二点");
        return true;
    }

    private boolean 确认选择(Player 玩家, String 服务器名) {
        Map<String, Location> 选择 = 选择缓存.get(服务器名);

        if (选择 == null || !选择.containsKey("点一") || !选择.containsKey("点二")) {
            玩家.sendMessage(获取组件(玩家, "传送管理.请先选择两点", 服务器名));
            return true;
        }

        Location 点一 = 选择.get("点一");
        Location 点二 = 选择.get("点二");

        if (!点一.getWorld().equals(点二.getWorld())) {
            玩家.sendMessage(获取组件(玩家, "传送管理.两点必须同世界"));
            return true;
        }

        // 创建传送门区域
        传送门区域 区域 = new 传送门区域(服务器名, 点一, 点二);

        // 保存到配置
        插件.获取登录配置().设置传送门区域(服务器名, 区域);
        插件.获取登录配置().保存配置();

        玩家.sendMessage(获取组件(玩家, "传送管理.保存成功", 获取服务器显示名(服务器名)));
        玩家.sendMessage(获取组件(玩家, "传送管理.区域范围",
            String.valueOf(区域.获取最小X()),
            String.valueOf(区域.获取最小Y()),
            String.valueOf(区域.获取最小Z()),
            String.valueOf(区域.获取最大X()),
            String.valueOf(区域.获取最大Y()),
            String.valueOf(区域.获取最大Z())));

        // 清除缓存
        选择缓存.remove(服务器名);

        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 确认了 " + 服务器名 + " 的区域选择");
        return true;
    }

    private boolean 重置选择(Player 玩家, String 服务器名) {
        选择缓存.remove(服务器名);
        玩家.sendMessage(获取组件(玩家, "传送管理.重置成功", 获取服务器显示名(服务器名)));
        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 重置了 " + 服务器名 + " 的选择");
        return true;
    }

    private boolean 查看当前选择(Player 玩家, String 服务器名) {
        Map<String, Location> 选择 = 选择缓存.get(服务器名);

        玩家.sendMessage(获取组件(玩家, "传送管理.当前选择标题", 获取服务器显示名(服务器名)));

        if (选择 == null) {
            玩家.sendMessage(获取组件(玩家, "传送管理.点一未选择"));
            玩家.sendMessage(获取组件(玩家, "传送管理.点二未选择"));
        } else {
            Location 点一 = 选择.get("点一");
            Location 点二 = 选择.get("点二");

            if (点一 != null) {
                玩家.sendMessage(获取组件(玩家, "传送管理.点一已选择",
                    String.valueOf(点一.getBlockX()),
                    String.valueOf(点一.getBlockY()),
                    String.valueOf(点一.getBlockZ())));
            } else {
                玩家.sendMessage(获取组件(玩家, "传送管理.点一未选择"));
            }

            if (点二 != null) {
                玩家.sendMessage(获取组件(玩家, "传送管理.点二已选择",
                    String.valueOf(点二.getBlockX()),
                    String.valueOf(点二.getBlockY()),
                    String.valueOf(点二.getBlockZ())));
            } else {
                玩家.sendMessage(获取组件(玩家, "传送管理.点二未选择"));
            }
        }

        // 显示已保存的区域
        传送门区域 已保存区域 = 插件.获取登录配置().获取传送门区域(服务器名);
        if (已保存区域 != null) {
            玩家.sendMessage(获取组件(玩家, "传送管理.已保存区域",
                String.valueOf(已保存区域.获取最小X()),
                String.valueOf(已保存区域.获取最小Y()),
                String.valueOf(已保存区域.获取最小Z()),
                String.valueOf(已保存区域.获取最大X()),
                String.valueOf(已保存区域.获取最大Y()),
                String.valueOf(已保存区域.获取最大Z())));
        } else {
            玩家.sendMessage(获取组件(玩家, "传送管理.已保存区域无"));
        }

        return true;
    }

    private boolean 重载配置(Player 玩家) {
        插件.获取登录配置().重载();
        玩家.sendMessage(获取组件(玩家, "通用消息.重载成功"));
        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 重载了配置");
        return true;
    }

    private boolean 设置重生点(Player 玩家) {
        Location 位置 = 玩家.getLocation();
        if (位置 == null) {
            玩家.sendMessage(获取组件(玩家, "通用消息.错误"));
            return true;
        }
        org.bukkit.World 世界 = 位置.getWorld();
        if (世界 == null) {
            玩家.sendMessage(获取组件(玩家, "通用消息.错误"));
            return true;
        }
        String 世界名 = 世界.getName();
        double 坐标X = 位置.getX();
        double 坐标Y = 位置.getY();
        double 坐标Z = 位置.getZ();
        float 偏航角 = 位置.getYaw();
        float 俯仰角 = 位置.getPitch();

        // 设置重生点
        插件.获取登录配置().设置重生点(世界名, 坐标X, 坐标Y, 坐标Z, 偏航角, 俯仰角);
        插件.获取登录配置().保存配置();

        玩家.sendMessage(获取组件(玩家, "传送管理.重生点设置成功"));
        玩家.sendMessage(获取组件(玩家, "传送管理.重生点位置",
            世界名,
            String.format("%.2f", 坐标X),
            String.format("%.2f", 坐标Y),
            String.format("%.2f", 坐标Z)));
        玩家.sendMessage(获取组件(玩家, "传送管理.重生点朝向",
            String.format("%.2f", 偏航角),
            String.format("%.2f", 俯仰角)));

        插件.getLogger().info("[传送管理] 玩家 " + 玩家.getName() + " 设置了重生点: " + 世界名
                + " (" + 坐标X + ", " + 坐标Y + ", " + 坐标Z + ")");
        return true;
    }

    private boolean 查看重生点(Player 玩家) {
        var 重生点 = 插件.获取登录配置().获取重生点();

        玩家.sendMessage(获取组件(玩家, "传送管理.重生点查看标题"));

        if (重生点 == null) {
            玩家.sendMessage(获取组件(玩家, "传送管理.重生点未设置"));
        } else {
            玩家.sendMessage(获取组件(玩家, "传送管理.重生点世界", 重生点.获取世界名()));
            玩家.sendMessage(获取组件(玩家, "传送管理.重生点坐标",
                String.format("%.2f", 重生点.获取坐标X()),
                String.format("%.2f", 重生点.获取坐标Y()),
                String.format("%.2f", 重生点.获取坐标Z())));
            玩家.sendMessage(获取组件(玩家, "传送管理.重生点朝向",
                String.format("%.2f", 重生点.获取偏航角()),
                String.format("%.2f", 重生点.获取俯仰角())));
        }

        return true;
    }

    private Component 获取组件(Player 玩家, String 键, String... 参数) {
        String 子键 = 键;
        if (键.startsWith("传送管理.")) {
            子键 = 键.substring("传送管理.".length());
        } else if (键.startsWith("通用消息.")) {
            子键 = 键.substring("通用消息.".length());
        }
        return 插件.获取消息管理器().获取类组件(玩家, 传送管理命令.class, 子键, (Object[]) 参数);
    }
}
