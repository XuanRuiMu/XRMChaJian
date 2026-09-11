package mljy;

import java.util.function.Consumer;

public interface 事件总线 {
    <T extends 领域事件> void 订阅(Class<T> 类型, Consumer<T> 处理器);

    void 发布(领域事件 事件);
}
