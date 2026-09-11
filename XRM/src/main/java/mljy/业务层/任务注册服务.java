package mljy.业务层;

import mljy.领域层.任务.任务分类;
import mljy.领域层.任务.任务定义;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 任务注册服务接口。
 * 负责任务定义的注册、查询和重载。
 * 任务定义由 YAML 配置驱动，启动时加载。
 */
public interface 任务注册服务 {
    /**
     * 注册任务定义。
     */
    void 注册(任务定义 定义);

    /**
     * 根据任务标识获取任务定义。
     */
    Optional<任务定义> 获取定义(String 任务标识);

    /**
     * 获取所有任务定义。
     */
    Collection<任务定义> 获取所有定义();

    /**
     * 获取指定分类下的所有任务定义。
     */
    List<任务定义> 获取分类任务(任务分类 分类);

    /**
     * 判断任务是否存在。
     */
    boolean 存在(String 任务标识);

    /**
     * 获取接取NPC关联的所有任务。
     */
    List<任务定义> 获取NPC接取任务(String NPC名称);

    /**
     * 获取交付NPC关联的所有任务。
     */
    List<任务定义> 获取NPC交付任务(String NPC名称);

    /**
     * 重载任务配置。
     */
    void 重载();
}
