package mljy.业务层.消息;

import mljy.基础设施层.事件日志上下文;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class 受击修正上下文测试 {
    private final UUID 玩家标识 = UUID.randomUUID();

    @AfterEach
    void 清理() {
        事件日志上下文.清除();
        受击修正上下文.获取并清除(玩家标识, UUID.randomUUID());
        受击修正上下文.获取并清除(玩家标识, null);
    }

    @Test
    void 修正按因果事件隔离() {
        UUID 第一事件 = UUID.randomUUID();
        UUID 第二事件 = UUID.randomUUID();
        受击修正上下文.记录吸收(玩家标识, 第一事件, 3.0, "skill.奥能法师.奥术护盾.name");
        受击修正上下文.记录吸收(玩家标识, 第二事件, 5.0, "skill.奥能法师.奥术护盾.name");

        List<受击修正上下文.修正信息> 第一列表 = 受击修正上下文.获取并清除(玩家标识, 第一事件);
        List<受击修正上下文.修正信息> 第二列表 = 受击修正上下文.获取并清除(玩家标识, 第二事件);

        assertEquals(1, 第一列表.size());
        assertEquals(3.0, 第一列表.get(0).修正量());
        assertEquals(1, 第二列表.size());
        assertEquals(5.0, 第二列表.get(0).修正量());
    }

    @Test
    void 获取后清除同一事件记录() {
        UUID 事件 = UUID.randomUUID();
        受击修正上下文.记录吸收(玩家标识, 事件, 2.0, "来源");

        受击修正上下文.获取并清除(玩家标识, 事件);

        assertTrue(受击修正上下文.获取并清除(玩家标识, 事件).isEmpty());
    }

    @Test
    void 无当前根事件_吸收记录不伪造随机事件() {
        事件日志上下文.清除();

        受击修正上下文.记录吸收(玩家标识, 2.0, "来源");

        List<受击修正上下文.修正信息> 列表 = 受击修正上下文.获取并清除(玩家标识, null);

        assertEquals(1, 列表.size());
        assertEquals(2.0, 列表.get(0).修正量());
    }
}
