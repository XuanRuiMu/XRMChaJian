package mljy.业务层;

import mljy.领域层.战斗.伤害管线阶段;
import mljy.领域层.战斗.伤害上下文;
import mljy.领域层.玩家.玩家快照;

import java.util.Optional;

public interface 伤害管线阶段处理器 {

    伤害管线阶段 阶段();

    double 计算增减伤百分比(伤害上下文 上下文, 属性计算服务 服务);

    record 全能增伤阶段处理器() implements 伤害管线阶段处理器 {
        @Override
        public 伤害管线阶段 阶段() {
            return 伤害管线阶段.全局伤害修饰;
        }

        @Override
        public double 计算增减伤百分比(伤害上下文 上下文, 属性计算服务 服务) {
            玩家快照 施法者 = 上下文.施法者();
            if (施法者 == null) {
                return 0;
            }
            return 服务.计算派生值("全能增伤比例", 服务.计算(施法者));
        }
    }

    record 全能减伤阶段处理器() implements 伤害管线阶段处理器 {
        @Override
        public 伤害管线阶段 阶段() {
            return 伤害管线阶段.目标减伤;
        }

        @Override
        public double 计算增减伤百分比(伤害上下文 上下文, 属性计算服务 服务) {
            Optional<玩家快照> 目标玩家Opt = 提取目标玩家(上下文.目标());
            if (目标玩家Opt.isEmpty()) {
                return 0;
            }
            return 服务.计算派生值("全能减伤比例", 服务.计算(目标玩家Opt.get()));
        }

        private Optional<玩家快照> 提取目标玩家(Object 目标) {
            if (目标 instanceof 玩家快照 玩家目标) {
                return Optional.of(玩家目标);
            }
            return Optional.empty();
        }
    }
}
