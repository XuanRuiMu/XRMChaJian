package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.技能执行器;
import mljy.技能服务;
import mljy.领域层.技能.技能上下文;
import mljy.领域层.技能.技能定义;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class 内存技能服务 implements 技能服务 {
    private final Map<String, 技能定义> 技能定义表 = new ConcurrentHashMap<>();
    private final Map<String, 技能执行器> 技能执行器表 = new ConcurrentHashMap<>();

    @Inject
    public 内存技能服务() {
    }

    @Override
    public void 释放(技能上下文 上下文) {
        获取执行器(上下文.技能标识()).ifPresent(执行器 -> 执行器.执行(上下文));
    }

    @Override
    public Optional<技能定义> 获取定义(String 技能标识) {
        return Optional.ofNullable(技能定义表.get(技能标识));
    }

    @Override
    public Optional<技能执行器> 获取执行器(String 技能标识) {
        return Optional.ofNullable(技能执行器表.get(技能标识));
    }

    @Override
    public Set<String> 获取所有技能标识() {
        return Set.copyOf(技能定义表.keySet());
    }

    public void 注册(String 技能标识, 技能定义 定义, 技能执行器 执行器) {
        技能定义表.put(技能标识, 定义);
        技能执行器表.put(技能标识, 执行器);
    }
}
