package mljy.基础设施层;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("内存天赋服务")
class 内存天赋服务测试 {

    private 内存天赋服务 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        服务 = new 内存天赋服务();
        玩家标识 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("基本功能")
    class 基本功能 {

        @Test
        @DisplayName("选择天赋后应可查询到")
        void 选择天赋_应可查询() {
            服务.选择天赋(玩家标识, "天赋A");

            Set<String> 天赋集 = 服务.获取已选天赋(玩家标识);

            assertTrue(天赋集.contains("天赋A"));
        }

        @Test
        @DisplayName("取消已选天赋应移除")
        void 取消已选天赋_应移除() {
            服务.选择天赋(玩家标识, "天赋A");
            服务.选择天赋(玩家标识, "天赋B");

            服务.取消天赋(玩家标识, "天赋A");

            Set<String> 天赋集 = 服务.获取已选天赋(玩家标识);
            assertFalse(天赋集.contains("天赋A"));
            assertTrue(天赋集.contains("天赋B"));
        }

        @Test
        @DisplayName("未选择天赋的玩家应返回空集")
        void 未选择天赋_应返回空集() {
            Set<String> 天赋集 = 服务.获取已选天赋(玩家标识);

            assertNotNull(天赋集);
            assertTrue(天赋集.isEmpty());
        }
    }

    @Nested
    @DisplayName("ERR-24: 取消天赋不抛UnsupportedOperationException")
    class 取消天赋边界 {

        @Test
        @DisplayName("玩家不在天赋表中时取消天赋应静默返回不抛异常")
        void 玩家不在表中_取消天赋_不抛异常() {
            UUID 未知玩家 = UUID.randomUUID();

            assertDoesNotThrow(() -> 服务.取消天赋(未知玩家, "天赋A"));
        }

        @Test
        @DisplayName("玩家在选择后取消全部天赋应正常")
        void 取消全部天赋_应正常() {
            服务.选择天赋(玩家标识, "天赋A");
            服务.取消天赋(玩家标识, "天赋A");
            服务.取消天赋(玩家标识, "天赋A");

            assertTrue(服务.获取已选天赋(玩家标识).isEmpty());
        }

        @Test
        @DisplayName("取消不存在天赋标识应静默返回")
        void 取消不存在天赋_应静默返回() {
            服务.选择天赋(玩家标识, "天赋A");

            assertDoesNotThrow(() -> 服务.取消天赋(玩家标识, "不存在的天赋"));
        }
    }
}
