package mljy.领域层.战斗;

import mljy.领域事件;

public final class 伤害管线上下文 implements 领域事件 {

    private final 伤害上下文 伤害上下文;
    private final 伤害管线阶段 当前阶段;
    private double 数值;

    public 伤害管线上下文(伤害上下文 伤害上下文, 伤害管线阶段 当前阶段, double 数值) {
        this.伤害上下文 = 伤害上下文;
        this.当前阶段 = 当前阶段;
        this.数值 = 数值;
    }

    public 伤害上下文 获取伤害上下文() {
        return 伤害上下文;
    }

    public 伤害管线阶段 获取当前阶段() {
        return 当前阶段;
    }

    public double 获取数值() {
        return 数值;
    }

    public void 设置数值(double 数值) {
        this.数值 = 数值;
    }
}
