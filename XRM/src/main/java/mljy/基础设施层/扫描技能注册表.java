package mljy.基础设施层;

import com.google.inject.Inject;
import com.google.inject.Injector;
import mljy.技能定义;
import mljy.技能执行器;

import java.util.Set;

public class 扫描技能注册表 {
    private final 内存技能服务 技能服务;
    private final Injector 注入器;

    @Inject
    public 扫描技能注册表(内存技能服务 技能服务, Injector 注入器) {
        this.技能服务 = 技能服务;
        this.注入器 = 注入器;
    }

    public void 扫描并注册(Set<Class<?>> 技能类集合) {
        调试日志器.调试("扫描技能注册表", "开始扫描：技能类数量=%d", 技能类集合.size());
        for (Class<?> 类型 : 技能类集合) {
            技能定义 注解 = 类型.getAnnotation(技能定义.class);
            if (注解 == null || !技能执行器.class.isAssignableFrom(类型)) {
                调试日志器.调试("扫描技能注册表", "跳过：类=%s 无注解或非技能执行器", 类型.getSimpleName());
                continue;
            }
            技能执行器 执行器 = (技能执行器) 注入器.getInstance(类型);
            执行器.声明参数();
            mljy.领域层.技能.技能定义 定义 = new mljy.领域层.技能.技能定义(
                    注解.技能标识(),
                    注解.名称翻译键(),
                    注解.施法类型(),
                    注解.蓄力时间(),
                    注解.引导时间(),
                    注解.冷却时间(),
                    注解.公共冷却(),
                    注解.资源标识(),
                    注解.资源消耗(),
                    注解.射程(),
                    注解.宽度(),
                    注解.最大充能数(),
                    注解.移动可打断(),
                    注解.受伤害可打断(),
                    注解.必须保持移动()
            );
            技能服务.注册(注解.技能标识(), 定义, 执行器);
            调试日志器.调试("扫描技能注册表", "注册：技能=%s 名称=%s 施法类型=%s 冷却=%.1f 公CD=%.1f",
                    注解.技能标识(), 注解.名称翻译键(), 注解.施法类型(),
                    注解.冷却时间(), 注解.公共冷却());
        }
        调试日志器.调试("扫描技能注册表", "扫描完成");
    }
}
