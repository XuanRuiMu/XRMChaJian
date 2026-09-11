package mljy.基础设施层;

import mljy.事件总线;
import mljy.领域事件;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class 内存事件总线 implements 事件总线 {
    private final Map<Class<?>, List<Consumer<?>>> 订阅表 = new ConcurrentHashMap<>();

    @Override
    public <T extends 领域事件> void 订阅(Class<T> 类型, Consumer<T> 处理器) {
        订阅表.computeIfAbsent(类型, 键 -> new CopyOnWriteArrayList<>()).add(处理器);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void 发布(领域事件 事件) {
        Class<?> 类型 = 事件.getClass();
        List<Consumer<?>> 处理器列表 = 订阅表.get(类型);
        if (处理器列表 == null) {
            return;
        }
        for (Consumer<?> 处理器 : 处理器列表) {
            ((Consumer<领域事件>) 处理器).accept(事件);
        }
    }
}
