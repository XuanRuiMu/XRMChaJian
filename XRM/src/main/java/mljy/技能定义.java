package mljy;

import mljy.领域层.技能.施法类型;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface 技能定义 {
    String 技能标识();

    String 名称翻译键();

    施法类型 施法类型() default 施法类型.瞬发;

    double 蓄力时间() default 0.0;

    double 引导时间() default 0.0;

    double 冷却时间() default 0.0;

    double 公共冷却() default 1.5;

    String 资源标识() default "";

    double 资源消耗() default 0.0;

    double 射程() default 0.0;

    double 宽度() default 0.0;

    int 最大充能数() default 1;

    boolean 移动可打断() default true;

    boolean 受伤害可打断() default false;

    boolean 必须保持移动() default false;
}
