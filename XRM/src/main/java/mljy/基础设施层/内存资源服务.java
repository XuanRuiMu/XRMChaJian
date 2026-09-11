package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.业务层.资源注册服务;
import mljy.资源服务;
import mljy.领域层.资源.资源;
import mljy.领域层.资源.资源定义;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class 内存资源服务 implements 资源服务 {
    private final Map<UUID, Map<String, 资源>> 资源表 = new ConcurrentHashMap<>();
    private final 资源注册服务 资源注册服务;

    @Inject
    public 内存资源服务(资源注册服务 资源注册服务) {
        this.资源注册服务 = 资源注册服务;
    }

    @Override
    public Optional<资源> 获取资源(UUID 玩家标识, String 资源标识) {
        Map<String, 资源> 玩家资源 = 资源表.get(玩家标识);
        return 玩家资源 != null ? Optional.ofNullable(玩家资源.get(资源标识)) : Optional.empty();
    }

    @Override
    public void 变更资源(UUID 玩家标识, String 资源标识, double 变化量) {
        获取资源(玩家标识, 资源标识).ifPresent(资源 -> {
            double 新值 = Math.max(0, Math.min(资源.获取当前值() + 变化量, 资源.获取上限()));
            资源.设置当前值(新值);
        });
    }

    @Override
    public void 设置资源上限(UUID 玩家标识, String 资源标识, double 上限) {
        资源表.computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>())
                .computeIfAbsent(资源标识, 键 -> new 资源(资源标识, 0, 上限))
                .设置上限(上限);
    }

    @Override
    public void 初始化玩家资源(UUID 玩家标识) {
        Map<String, 资源> 玩家资源 = 资源表.computeIfAbsent(玩家标识, 键 -> new ConcurrentHashMap<>());
        for (资源定义 定义 : 资源注册服务.获取所有资源定义()) {
            玩家资源.computeIfAbsent(定义.获取资源标识(), 键 -> new 资源(定义.获取资源标识(), 0, 定义.获取默认上限()));
        }
    }
}
