package mljy.业务层;

import mljy.领域层.效果.效果定义;

import java.util.Collection;
import java.util.Optional;

public interface 效果注册服务 {
    void 注册(效果定义 定义);

    Optional<效果定义> 获取定义(String 效果标识);

    Collection<效果定义> 获取所有定义();

    boolean 存在(String 效果标识);
}
