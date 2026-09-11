package mljy.业务层;

import mljy.领域层.资源.资源定义;

import java.util.Collection;
import java.util.Optional;

public interface 资源注册服务 {
    Optional<资源定义> 获取资源定义(String 资源标识);

    Collection<资源定义> 获取所有资源定义();

    boolean 资源是否存在(String 资源标识);
}
