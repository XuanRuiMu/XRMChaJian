package mljy.表现层.菜单;

import com.google.inject.Inject;
import com.google.inject.Provider;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.天赋管理服务;
import mljy.业务层.天赋注册服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.天赋.天赋操作结果;
import mljy.领域层.天赋.天赋图;
import mljy.领域层.天赋.天赋节点;
import mljy.领域层.玩家.玩家会话;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 天赋菜单 extends 菜单框架 {
    private static final String 菜单标识 = "天赋菜单";
    private static final int 菜单大小 = 54;
    private static final int 关闭按钮槽位 = 49;
    private static final int 重置按钮槽位 = 45;
    private static final int 点数显示槽位 = 4;

    private static final String 标题键 = "天赋菜单.标题";
    private static final String 点数显示键 = "天赋菜单.点数显示";
    private static final String 剩余点数键 = "天赋菜单.剩余点数";
    private static final String 重置按钮名称键 = "天赋菜单.重置按钮.名称";
    private static final String 重置按钮描述键 = "天赋菜单.重置按钮.描述";
    private static final String 关闭文本键 = "菜单框架.通用.关闭";

    private static final String 物品已学习 = "ENCHANTED_GOLDEN_APPLE";
    private static final String 物品可学习 = "GOLDEN_APPLE";
    private static final String 物品锁定 = "IRON_NUGGET";
    private static final String 物品冲突 = "REDSTONE_BLOCK";
    private static final String 物品点数显示 = "NETHER_STAR";
    private static final String 物品重置 = "TNT";
    private static final String 物品关闭 = "BARRIER";

    private static final String 状态已学习键 = "天赋菜单.状态.已学习";
    private static final String 状态点击学习键 = "天赋菜单.状态.点击学习";
    private static final String 状态未解锁键 = "天赋菜单.状态.未解锁";
    private static final String 状态冲突键 = "天赋菜单.状态.冲突";
    private static final String 已学前缀键 = "天赋菜单.已学前缀";
    private static final String 冲突前缀键 = "天赋菜单.冲突前缀";
    private static final String 锁定前缀键 = "天赋菜单.锁定前缀";
    private static final String 影响技能键 = "天赋菜单.影响技能";
    private static final String 所需点数键 = "天赋菜单.所需点数";
    private static final String 前置天赋键 = "天赋菜单.前置天赋";
    private static final String 前置分隔符键 = "天赋菜单.前置分隔符";
    private static final String 前置包围符左键 = "天赋菜单.前置包围符左";
    private static final String 前置包围符右键 = "天赋菜单.前置包围符右";
    private static final String 选择组键 = "天赋菜单.选择组";
    private static final String 选择组警告键 = "天赋菜单.选择组警告";

    private static final String 默认专精 = "奥能法师";
    private static final String 点数格式 = "%d";
    private static final String 技能分隔符 = ", ";

    private final 玩家服务 玩家服务;
    private final 天赋管理服务 天赋管理服务;
    private final 天赋注册服务 天赋注册服务;
    private final JavaPlugin 插件;
    private final Map<UUID, Map<Integer, String>> 槽位天赋映射 = new ConcurrentHashMap<>();

    @Inject
    public 天赋菜单(翻译服务 翻译服务,
                   关键词解析器 关键词解析器,
                   菜单占位符 占位符,
                   菜单条件 条件,
                   Provider<菜单服务> 菜单服务提供器,
                   玩家服务 玩家服务,
                   天赋管理服务 天赋管理服务,
                   天赋注册服务 天赋注册服务,
                   JavaPlugin 插件) {
        super(翻译服务, 关键词解析器, 占位符, 条件, 菜单服务提供器);
        this.玩家服务 = 玩家服务;
        this.天赋管理服务 = 天赋管理服务;
        this.天赋注册服务 = 天赋注册服务;
        this.插件 = 插件;
    }

    @Override
    public String 获取菜单标识() {
        return 菜单标识;
    }

    @Override
    public 菜单配置 构建配置(Player 玩家) {
        String 专精 = 获取玩家专精(玩家);
        String 标题 = 翻译服务.获取(标题键, 专精);
        List<菜单按钮> 按钮 = new ArrayList<>();

        槽位天赋映射.remove(玩家.getUniqueId());
        Map<Integer, String> 映射 = new ConcurrentHashMap<>();

        按钮.add(构建点数显示按钮(玩家));

        Optional<天赋图> 图_opt = 天赋注册服务.获取天赋图(专精);
        if (图_opt.isPresent()) {
            天赋图 图 = 图_opt.get();
            Set<String> 已学天赋 = 天赋管理服务.获取已学天赋(玩家.getUniqueId());
            int 已用点数 = 已学天赋.size();

            for (天赋节点 节点 : 图.获取所有节点()) {
                菜单按钮 节点按钮 = 构建天赋节点按钮(节点, 图, 已学天赋, 已用点数);
                按钮.add(节点按钮);
                映射.put(节点.获取槽位(), 节点.天赋标识());
            }
        }

        按钮.add(构建重置按钮());
        按钮.add(构建关闭按钮());

        if (!映射.isEmpty()) {
            槽位天赋映射.put(玩家.getUniqueId(), 映射);
        }

        return 菜单配置.of(标题, 菜单大小).with按钮(按钮);
    }

    @Override
    public boolean 处理点击(Player 玩家, int 槽位, String 点击类型) {
        if (!是否打开(玩家.getUniqueId())) {
            return false;
        }

        Map<Integer, String> 映射 = 槽位天赋映射.get(玩家.getUniqueId());
        if (映射 != null && 映射.containsKey(槽位)) {
            String 天赋标识 = 映射.get(槽位);
            天赋操作结果 结果 = 天赋管理服务.学习天赋(玩家.getUniqueId(), 天赋标识);
            发送结果消息(玩家, 结果);
            刷新菜单(玩家);
            return true;
        }

        if (槽位 == 重置按钮槽位) {
            天赋操作结果 结果 = 天赋管理服务.重置天赋(玩家.getUniqueId());
            发送结果消息(玩家, 结果);
            刷新菜单(玩家);
            return true;
        }

        if (槽位 == 关闭按钮槽位) {
            玩家.closeInventory();
            return true;
        }

        return true;
    }

    private void 刷新菜单(Player 玩家) {
        玩家.closeInventory();
        Bukkit.getScheduler().runTask(插件, () -> 打开(玩家));
    }

    private void 发送结果消息(Player 玩家, 天赋操作结果 结果) {
        if (结果.有消息()) {
            String 文本 = 翻译服务.获取(结果.消息键(), 结果.参数());
            String 解析后 = 关键词解析器.解析(文本, 玩家.getUniqueId());
            玩家.sendMessage(迷你消息.deserialize(解析后));
        }
    }

    private 菜单按钮 构建点数显示按钮(Player 玩家) {
        int 已用点数 = 天赋管理服务.获取已用点数(玩家.getUniqueId());
        int 最大点数 = 天赋管理服务.获取最大点数(玩家.getUniqueId());
        if (最大点数 <= 0) {
            最大点数 = 10;
        }
        int 剩余点数 = 最大点数 - 已用点数;
        String 显示名 = 翻译服务.获取(点数显示键,
                String.format(点数格式, 已用点数),
                String.format(点数格式, 最大点数));
        List<String> 描述 = new ArrayList<>();
        描述.add(翻译服务.获取(剩余点数键, String.format(点数格式, 剩余点数)));
        return 菜单按钮.of(点数显示槽位, 物品点数显示, 显示名)
                .with描述(描述);
    }

    private 菜单按钮 构建天赋节点按钮(天赋节点 节点, 天赋图 图, Set<String> 已学天赋, int 已用点数) {
        boolean 已学习 = 已学天赋.contains(节点.天赋标识());
        boolean 前置满足 = 图.检查前置(节点.天赋标识(), 已学天赋);
        boolean 选择组无冲突 = 图.检查选择组(节点.天赋标识(), 已学天赋);
        boolean 点数足够 = 已用点数 < 图.获取最大点数();
        boolean 点数门控满足 = 已用点数 >= 节点.所需总点数();

        String 物品标识;
        String 状态文本;
        String 前缀 = "";

        if (已学习) {
            物品标识 = 物品已学习;
            状态文本 = 翻译服务.获取(状态已学习键);
            前缀 = 翻译服务.获取(已学前缀键);
        } else if (!选择组无冲突) {
            物品标识 = 物品冲突;
            状态文本 = 翻译服务.获取(状态冲突键);
            前缀 = 翻译服务.获取(冲突前缀键);
        } else if (前置满足 && 点数门控满足 && 点数足够) {
            物品标识 = 物品可学习;
            状态文本 = 翻译服务.获取(状态点击学习键);
        } else {
            物品标识 = 物品锁定;
            状态文本 = 翻译服务.获取(状态未解锁键);
            前缀 = 翻译服务.获取(锁定前缀键);
        }

        String 名称 = 前缀 + 翻译服务.获取(节点.名称翻译键());
        List<String> 描述 = 构建天赋描述(节点, 图, 状态文本, 已用点数);

        return 菜单按钮.of(节点.获取槽位(), 物品标识, 名称)
                .with描述(描述);
    }

    private List<String> 构建天赋描述(天赋节点 节点, 天赋图 图, String 状态文本, int 已用点数) {
        List<String> 描述 = new ArrayList<>();

        描述.add(翻译服务.获取(节点.描述翻译键()));

        描述.add(状态文本);

        描述.add(翻译服务.获取(所需点数键, String.format(点数格式, 节点.所需总点数())));

        if (节点.有前置()) {
            String 分隔符 = 翻译服务.获取(前置分隔符键);
            String 左 = 翻译服务.获取(前置包围符左键);
            String 右 = 翻译服务.获取(前置包围符右键);
            StringBuilder 前置文本 = new StringBuilder();
            for (int i = 0; i < 节点.前置天赋列表().size(); i++) {
                if (i > 0) {
                    前置文本.append(分隔符);
                }
                String 前置标识 = 节点.前置天赋列表().get(i);
                String 前置名称 = 图.获取节点(前置标识)
                        .map(天赋节点::名称翻译键)
                        .map(翻译服务::获取)
                        .orElse(前置标识);
                前置文本.append(左).append(前置名称).append(右);
            }
            描述.add(翻译服务.获取(前置天赋键, 前置文本.toString()));
        }

        if (节点.有选择组()) {
            描述.add(翻译服务.获取(选择组键, 节点.选择组()));
            描述.add(翻译服务.获取(选择组警告键));
        }

        if (节点.有影响技能()) {
            描述.add(翻译服务.获取(影响技能键, String.join(技能分隔符, 节点.影响技能())));
        }

        return 描述;
    }

    private 菜单按钮 构建重置按钮() {
        String 名称 = 翻译服务.获取(重置按钮名称键);
        List<String> 描述 = List.of(翻译服务.获取(重置按钮描述键));
        return 菜单按钮.of(重置按钮槽位, 物品重置, 名称)
                .with描述(描述);
    }

    private 菜单按钮 构建关闭按钮() {
        return 菜单按钮.of(关闭按钮槽位, 物品关闭, 翻译服务.获取(关闭文本键));
    }

    private String 获取玩家专精(Player 玩家) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家.getUniqueId());
        return 会话.map(玩家会话::获取专精).orElse(默认专精);
    }
}
