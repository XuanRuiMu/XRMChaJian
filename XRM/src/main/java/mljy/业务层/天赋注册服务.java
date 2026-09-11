package mljy.业务层;

import mljy.领域层.天赋.天赋图;
import mljy.领域层.天赋.天赋节点;

import java.util.Collection;
import java.util.Optional;

public interface 天赋注册服务 {

    Optional<天赋图> 获取天赋图(String 专精);

    Optional<天赋节点> 获取天赋定义(String 专精, String 天赋标识);

    Collection<天赋节点> 获取所有天赋定义(String 专精);

    boolean 存在天赋树(String 专精);

    Collection<String> 获取所有专精();

    void 重载();
}
