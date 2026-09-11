package mljy.领域层.属性;

import java.util.function.DoubleUnaryOperator;

/**
 * 派生属性计算器。
 *
 * 设计目标（需求.txt第32行"松散性"与"可插入性"）：
 * 解决 TD-2——属性计算服务接口硬编码属性特定方法的问题。
 *
 * 机制：
 * - 每个派生属性（如全能增伤比例、吸血比例等）封装为独立的计算器实现。
 * - 属性计算服务维护 派生名 -> 计算器 的注册表，通过 计算派生值(派生名, 属性) 通用方法访问。
 * - 新增派生属性只需：1) 实现本接口；2) 在属性计算服务注册。无需修改接口契约。
 *
 * 松散性验证：
 * - 删除任一计算器实现（如 全能增伤比例计算器），属性计算服务仍可正常运行，
 *   仅 计算派生值("全能增伤比例", ...) 返回 0，不会引起编译错误传播。
 *
 * 参数说明：
 * - 属性：属性快照，提供标准字段和扩展属性访问。
 * - 应用通用递减：递减函数，由属性计算服务传入（方法引用），避免循环依赖。
 */
public interface 派生属性计算器 {
    String 派生名();

    double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减);

    record 全能增伤比例计算器() implements 派生属性计算器 {
        private static final double 全能增伤每点系数 = 0.005;
        private static final String 派生名 = "全能增伤比例";

        @Override
        public String 派生名() {
            return 派生名;
        }

        @Override
        public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
            double 有效全能 = 应用通用递减.applyAsDouble(属性.全能());
            return 有效全能 * 全能增伤每点系数;
        }
    }

    record 全能减伤比例计算器() implements 派生属性计算器 {
        private static final double 全能减伤每点系数 = 0.005;
        private static final String 派生名 = "全能减伤比例";

        @Override
        public String 派生名() {
            return 派生名;
        }

        @Override
        public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
            double 有效全能 = 应用通用递减.applyAsDouble(属性.全能());
            return 有效全能 * 全能减伤每点系数;
        }
    }

    record 吸血比例计算器() implements 派生属性计算器 {
        private static final double 吸血每点系数 = 0.01;
        private static final String 派生名 = "吸血比例";

        @Override
        public String 派生名() {
            return 派生名;
        }

        @Override
        public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
            double 有效吸血 = 应用通用递减.applyAsDouble(属性.吸血());
            return 有效吸血 * 吸血每点系数;
        }
    }

    record 躲闪几率计算器() implements 派生属性计算器 {
        private static final double 躲闪每点系数 = 0.01;
        private static final String 派生名 = "躲闪几率";

        @Override
        public String 派生名() {
            return 派生名;
        }

        @Override
        public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
            double 有效躲闪 = 应用通用递减.applyAsDouble(属性.躲闪());
            return 有效躲闪 * 躲闪每点系数;
        }
    }

    record 暴击期望增伤计算器() implements 派生属性计算器 {
        private static final double 暴击百分比系数 = 0.01;
        private static final String 派生名 = "暴击期望增伤";

        @Override
        public String 派生名() {
            return 派生名;
        }

        @Override
        public double 计算(属性快照 属性, DoubleUnaryOperator 应用通用递减) {
            double 有效法术暴击几率 = 应用通用递减.applyAsDouble(属性.法术暴击几率());
            double 有效法术暴击伤害 = 应用通用递减.applyAsDouble(属性.法术暴击伤害());
            return 有效法术暴击几率 * 暴击百分比系数 * 有效法术暴击伤害 * 暴击百分比系数;
        }
    }
}
