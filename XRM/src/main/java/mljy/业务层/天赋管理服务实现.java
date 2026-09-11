package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.玩家服务;
import mljy.数据服务;
import mljy.属性服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.领域层.天赋.天赋操作结果;
import mljy.领域层.天赋.天赋图;
import mljy.领域层.天赋.天赋节点;
import mljy.领域层.玩家.玩家会话;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class 天赋管理服务实现 implements 天赋管理服务 {

    private static final String 修饰器标签前缀 = "天赋_";
    private static final String 消息学习成功 = "天赋管理器.学习.成功";
    private static final String 消息取消成功 = "天赋管理器.取消.成功";
    private static final String 消息重置成功 = "天赋管理器.重置.成功";
    private static final String 消息未找到会话 = "天赋管理器.通用.未找到会话";
    private static final String 消息未找到专精 = "天赋管理器.学习.未找到专精";
    private static final String 消息未找到天赋 = "天赋管理器.学习.未找到天赋";
    private static final String 消息已经学习 = "天赋管理器.学习.已经学习";
    private static final String 消息点数用完 = "天赋管理器.学习.点数用完";
    private static final String 消息点数不足 = "天赋管理器.学习.点数不足";
    private static final String 消息前置未满足 = "天赋管理器.学习.前置未满足";
    private static final String 消息选择组冲突 = "天赋管理器.学习.选择组冲突";
    private static final String 消息未学习 = "天赋管理器.取消.未学习";
    private static final String 消息依赖冲突 = "天赋管理器.取消.依赖冲突";

    private final 天赋注册服务 天赋注册服务;
    private final 玩家服务 玩家服务;
    private final 数据服务 数据服务;
    private final 修饰器管理器 修饰器管理器;
    private final 属性服务 属性服务;

    @Inject
    public 天赋管理服务实现(天赋注册服务 天赋注册服务,
                            玩家服务 玩家服务,
                            数据服务 数据服务,
                            修饰器管理器 修饰器管理器,
                            属性服务 属性服务) {
        this.天赋注册服务 = 天赋注册服务;
        this.玩家服务 = 玩家服务;
        this.数据服务 = 数据服务;
        this.修饰器管理器 = 修饰器管理器;
        this.属性服务 = 属性服务;
    }

    @Override
    public 天赋操作结果 学习天赋(UUID 玩家标识, String 天赋标识) {
        Optional<玩家会话> 会话_opt = 玩家服务.获取会话(玩家标识);
        if (会话_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到会话);
        }
        玩家会话 会话 = 会话_opt.get();
        String 专精 = 会话.获取专精();

        Optional<天赋图> 图_opt = 天赋注册服务.获取天赋图(专精);
        if (图_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到专精, 专精);
        }
        天赋图 图 = 图_opt.get();

        Optional<天赋节点> 节点_opt = 图.获取节点(天赋标识);
        if (节点_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到天赋, 天赋标识);
        }
        天赋节点 节点 = 节点_opt.get();

        Set<String> 已学天赋 = 会话.获取已选天赋();

        if (已学天赋.contains(天赋标识)) {
            return 天赋操作结果.失败(消息已经学习);
        }

        int 已用点数 = 已学天赋.size();

        if (已用点数 >= 图.获取最大点数()) {
            return 天赋操作结果.失败(消息点数用完);
        }

        if (已用点数 < 节点.所需总点数()) {
            return 天赋操作结果.失败(消息点数不足, 节点.所需总点数(), 已用点数);
        }

        if (!图.检查前置(天赋标识, 已学天赋)) {
            return 天赋操作结果.失败(消息前置未满足);
        }

        if (!图.检查选择组(天赋标识, 已学天赋)) {
            String 冲突天赋 = 图.查找选择组冲突(天赋标识, 已学天赋);
            return 天赋操作结果.失败(消息选择组冲突, 冲突天赋);
        }

        会话.添加天赋(天赋标识);

        if (节点.有修饰器()) {
            for (mljy.领域层.属性.修饰器 修饰器 : 节点.修饰器列表()) {
                修饰器管理器.注册修饰器(玩家标识, 修饰器);
            }
            属性服务.刷新属性(玩家标识);
        }

        数据服务.保存(会话);

        return 天赋操作结果.成功(消息学习成功, 天赋标识);
    }

    @Override
    public 天赋操作结果 取消天赋(UUID 玩家标识, String 天赋标识) {
        Optional<玩家会话> 会话_opt = 玩家服务.获取会话(玩家标识);
        if (会话_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到会话);
        }
        玩家会话 会话 = 会话_opt.get();
        String 专精 = 会话.获取专精();

        Optional<天赋图> 图_opt = 天赋注册服务.获取天赋图(专精);
        if (图_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到专精, 专精);
        }
        天赋图 图 = 图_opt.get();

        Set<String> 已学天赋 = 会话.获取已选天赋();

        if (!已学天赋.contains(天赋标识)) {
            return 天赋操作结果.失败(消息未学习);
        }

        Set<String> 依赖者 = 图.获取依赖者(天赋标识, 已学天赋);
        if (!依赖者.isEmpty()) {
            return 天赋操作结果.失败(消息依赖冲突, 依赖者.iterator().next());
        }

        会话.移除天赋(天赋标识);

        修饰器管理器.注销来源修饰器(玩家标识, 修饰器标签前缀 + 天赋标识);
        属性服务.刷新属性(玩家标识);

        数据服务.保存(会话);

        return 天赋操作结果.成功(消息取消成功, 天赋标识);
    }

    @Override
    public 天赋操作结果 重置天赋(UUID 玩家标识) {
        Optional<玩家会话> 会话_opt = 玩家服务.获取会话(玩家标识);
        if (会话_opt.isEmpty()) {
            return 天赋操作结果.失败(消息未找到会话);
        }
        玩家会话 会话 = 会话_opt.get();

        Set<String> 已学天赋 = 会话.获取已选天赋();

        for (String 天赋标识 : 已学天赋) {
            修饰器管理器.注销来源修饰器(玩家标识, 修饰器标签前缀 + 天赋标识);
            会话.移除天赋(天赋标识);
        }
        属性服务.刷新属性(玩家标识);

        数据服务.保存(会话);

        return 天赋操作结果.成功(消息重置成功);
    }

    @Override
    public Set<String> 获取已学天赋(UUID 玩家标识) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家标识);
        return 会话.map(玩家会话::获取已选天赋).orElse(Collections.emptySet());
    }

    @Override
    public int 获取已用点数(UUID 玩家标识) {
        return 获取已学天赋(玩家标识).size();
    }

    @Override
    public int 获取最大点数(UUID 玩家标识) {
        Optional<玩家会话> 会话 = 玩家服务.获取会话(玩家标识);
        if (会话.isEmpty()) {
            return 0;
        }
        String 专精 = 会话.get().获取专精();
        Optional<天赋图> 图 = 天赋注册服务.获取天赋图(专精);
        return 图.map(天赋图::获取最大点数).orElse(0);
    }
}
