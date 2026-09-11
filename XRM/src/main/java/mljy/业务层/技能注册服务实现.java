package mljy.业务层;

import com.google.inject.Inject;
import mljy.技能执行器;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.内存技能服务;
import mljy.领域层.技能.技能定义;

import java.util.Optional;
import java.util.Set;

public class 技能注册服务实现 implements 技能注册服务 {
    private final 内存技能服务 技能服务;

    @Inject
    public 技能注册服务实现(内存技能服务 技能服务) {
        this.技能服务 = 技能服务;
    }

    @Override
    public void 注册(String 技能标识, 技能定义 定义, 技能执行器 执行器) {
        调试日志器.调试("技能注册", "注册技能：标识=%s 定义类=%s 执行器类=%s", 技能标识, 定义.getClass().getSimpleName(), 执行器.getClass().getSimpleName());
        技能服务.注册(技能标识, 定义, 执行器);
    }

    @Override
    public Optional<技能定义> 获取定义(String 技能标识) {
        return 技能服务.获取定义(技能标识);
    }

    @Override
    public Optional<技能执行器> 获取执行器(String 技能标识) {
        return 技能服务.获取执行器(技能标识);
    }

    @Override
    public Set<String> 获取所有技能标识() {
        return 技能服务.获取所有技能标识();
    }
}
